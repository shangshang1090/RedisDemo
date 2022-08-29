package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自建一个对象，防止修改原来对象，
 * Object data 用于接收需要添加expireTime属性的对象
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
