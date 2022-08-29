package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class LoginInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
/*
        //1获取session
        //HttpSession session = request.getSession();
        //2.获取session中的用户
        //Object user = session.getAttribute("user");
        //3.判断用户是否存在
//        if(user == null){
//            //4不存在拦截
//            response.setStatus(401);
//            return false;
//        }

        //1获取请求头中的token
        String authorization = request.getHeader("authorization");
        if(StrUtil.isBlank(authorization)){
            //4不存在拦截
            response.setStatus(401);
            return false;
        }


        //2.基于TOKEN获取redis中的用户
        String key =LOGIN_USER_KEY + authorization;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if(userMap.isEmpty()){
            //4不存在拦截
            response.setStatus(401);
            return false;
       }
        //将查询到的Hash数据转为UserDto对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        //5存在保存到ThreadLocal
        UserHolder.saveUser(userDTO);
       // UserHolder.saveUser((UserDTO) user);

        //7刷新token有效期
         stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.放行
*/

        //1.判断是否需要拦截（TreadLocal中是否有用户）
        if(UserHolder.getUser()==null){
            //设置状态码
            response.setStatus(401);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
      //移除用户
       UserHolder.removeUser();
    }
}
