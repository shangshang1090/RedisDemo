package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.internal.Function;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(value),time,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    //缓存穿透
    /*public <R,ID> R queryWithPassThrough(String keyHead, ID id, Class<R> type, Function<ID,R> dbFallBack,Long time, TimeUnit unit){
        String key =keyHead+id;
        //1.从redis查询商铺缓存
        String Json = stringRedisTemplate.opsForValue().get(key + id);
        //2.判断是否存在
        if(StrUtil.isNotBlank(Json)){
            //3存在返回
            return JSONUtil.toBean(Json, type);
        }
        //判断的是否存在空值
        if(Json!=null){
            return null;
        }
        //4.不存在，根据id的查询数据库
        R r = dbFallBack.apply(id);
        //5.不存在返回错误
        if(r==null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
            //不存在返回错误信息
            return null;
        }
        //6.存在，写入redis,再返回
        this.set(key,r,time,unit);
        return r;

    }*/
    public <R,ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断命中的是否是空值
        if (json != null) {
            // 返回一个错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.不存在，返回错误
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }
        // 6.存在，写入redis
        this.set(key, r, time, unit);
        return r;
    }

    //逻辑缓存击穿
    public <R,ID>R queryWithLogicalExpire(String keyHead,ID id,Class<R>type,Function<ID,R>dbFallBack,Long time, TimeUnit unit){
        String key=RedisConstants.CACHE_SHOP_KEY + id;
        //1.从redis查询商铺缓存
        String Json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(StrUtil.isBlank(Json)){
            //2.1存在返回
            return null;
        }
        //3命中，需要把json反序列化
        RedisData data = JSONUtil.toBean(Json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) data.getData(), type);
        //3.0判断缓存是否过期
        LocalDateTime expireTime = data.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return  r;
        }
        //3.1过期,获取互斥锁
        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        Boolean isLock=tryLock(lockKey);
        if(!isLock){
            return r;
        }
        //3.1.1获取成功，判断缓存是否过期
        String Json1 = stringRedisTemplate.opsForValue().get(key);
        RedisData data1 = JSONUtil.toBean(Json1, RedisData.class);
        //JSONObject dataData1 = (JSONObject) data.getData();
        R r1 = JSONUtil.toBean((JSONObject) data.getData(), type);
        LocalDateTime expireTime1 = data1.getExpireTime();
        if(expireTime1.isAfter(LocalDateTime.now())){
            return r1;
        }
        //3.1.2过期，开启独立线程缓存重建
        CACHE_REBUILD_EXECUTOR.submit(()->{
            //重建缓存
            try {
                //查数据库
                R r2 =dbFallBack.apply(id);
                //写入缓存
                this.setWithLogicalExpire(key,r2,time,unit);
                return r2;
            } catch (Exception e) {
                throw  new RuntimeException();
            }finally {
                //释放锁
                unlock(lockKey);
            }

        });
        return r1;
    }
    //获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);//值为0或1，spring帮我们转为boolean;jok
        return BooleanUtil.isTrue(flag);
    }
    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
    //线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);

}
