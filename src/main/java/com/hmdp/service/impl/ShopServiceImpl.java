package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Shop queryById(Long id){
        //缓存穿透
        //Shop shop = queryWithPassThrough(id);
        //缓存穿透 id2->getById(id2)==this::getById
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);
        //逻辑缓存击穿
       /* Shop shop= cacheClient.queryWithLogicalExpire
                (RedisConstants.CACHE_SHOP_KEY,id,Shop.class,this::getById,RedisConstants.CACHE_SHOP_TTL,TimeUnit.MINUTES);*/
        //返回

        // 7.返回
        return shop;
    }





  /*  //互斥锁解决缓存击穿
    public Shop queryWithMutex(Long id){
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3存在返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断的是否存在空值
        if(shopJson!=null){
            return null;
        }

        //4.实现缓存重建
        //4.1获取互斥锁
        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        Shop shop = null;
        try {
            boolean b = tryLock(lockKey);
            //4.2判断是否成功
            if(!b){
                //4.3失败，休眠重试
                Thread.sleep(50);


            }

            //4.4成功，根据id查询数据库
            shop = getById(id);
            //5.不存在返回错误
            if(shop==null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,"",8L,TimeUnit.MINUTES);
                //不存在返回错误信息
                return null;
            }
            //6.存在，写入redis
            stringRedisTemplate.opsForValue()
                    .set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            throw  new RuntimeException();
        }finally {
            //7释放互斥锁
            unlock(lockKey);
        }
        //8返回
        return shop;
    }
*/



   /* //逻辑过期解决缓存击穿
    public Shop queryWithLogicalExpire(Long id){
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(StrUtil.isBlank(shopJson)){
            //2.1不存在返回
            return null;
        }
        //3命中，需要把json反序列化
        RedisData data = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) data.getData(), Shop.class);
        //3.0判断缓存是否过期
        LocalDateTime expireTime = data.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return  shop;
        }
        //3.1过期,获取互斥锁
        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
        Boolean isLock=tryLock(lockKey);
        if(!isLock){
            return shop;
        }
        //3.1.1获取成功，判断缓存是否过期
        String shopJson1 = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        RedisData data1 = JSONUtil.toBean(shopJson1, RedisData.class);
        //JSONObject dataData1 = (JSONObject) data.getData();
        Shop shop1 = JSONUtil.toBean((JSONObject) data.getData(), Shop.class);
        LocalDateTime expireTime1 = data1.getExpireTime();
        if(expireTime1.isAfter(LocalDateTime.now())){
            return shop1;
        }
        //3.1.2过期，开启独立线程缓存重建
        CACHE_REBUILD_EXECUTOR.submit(()->{
            //重建缓存
            try {
                saveShopToRedis(id,RedisConstants.CACHE_SHOP_TTL);
            } catch (Exception e) {
                throw  new RuntimeException();
            }finally {
                //释放锁
                unlock(lockKey);
            }

        });
        return shop1;
    }


    */

    public Shop queryWithPassThrough(Long id){
        //1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3存在返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断的是否存在空值
        if(shopJson!=null){
            return null;
        }
        //4.不存在，根据id的查询数据库
        Shop shop = getById(id);
        //5.不存在返回错误
        if(shop==null){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,"",8L,TimeUnit.MINUTES);
            //不存在返回错误信息
            return null;
        }
        //6.存在，写入redis,再返回
        stringRedisTemplate.opsForValue()
                .set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }

    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null){
            return Result.fail("店铺id不能为空");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);
        return Result.ok();
    }
/*
//获取锁
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);//值为0或1，spring帮我们转为boolean;jok
         return BooleanUtil.isTrue(flag);
    }
    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
*/

    //数据预热
    public void saveShopToRedis(Long id,Long expireTime){
        Shop shop = getById(id);
        RedisData data = new RedisData();
        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        //写入redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(data));
    }
   /*//线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);*/

}
