package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
  /*  @Resource
    private StringRedisTemplate stringRedisTemplate;
    这个类不是由spring创建的所以不能用@注入
    */
  private StringRedisTemplate stringRedisTemplate;
    private String name;

    private static final String KEY_PREFIX="lock";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";



    //构造方法
   public SimpleRedisLock(String name,StringRedisTemplate stringRedisTemplate){
       this.name=name;
       this.stringRedisTemplate=stringRedisTemplate;
   }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取现成的标识
       String id = ID_PREFIX+Thread.currentThread().getId();
        //获取锁
        Boolean success=stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name,id,timeoutSec, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
       //获取线程标识
        String id = ID_PREFIX+Thread.currentThread().getId();
        //获取锁中的标识
        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        if (id.equals(s)){
            //释放锁
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }

    }
}
