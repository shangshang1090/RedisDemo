package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
//开始时间戳
    private static final long BEGIN_TIMESTAMP=1640995200L;
    //序列号的位数
    private static final int COUNT_BITS=32;

    public long nextId(String keyPrefix){
        //1.生成时间搓
        LocalDateTime now = LocalDateTime.now();
        long second = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = second - BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1获取当天日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyy:MM:dd"));
        //默认一次自增1,使用increment()方法进行增加后，该方法会返回增加后的值
        long count = stringRedisTemplate.opsForValue().increment("icr" + keyPrefix + ":" + date);
        //3.拼接返回,用或运算0|1=1 0|0=0

        return timestamp<<COUNT_BITS | count;
    }

}
