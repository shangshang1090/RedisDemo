package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Resource
      private StringRedisTemplate stringRedisTemplate;


    @Override
        public Result sendCode(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //不符合返回错误信息
            return Result.fail("手机号格式错误");
        }

        //符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到redis
//        session.setAttribute("code", code);
            stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送验证码成功，验证码：{}", code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号和验证码
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号码格式错误");
        }
        //校验验证码
        Object code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code1 = loginForm.getCode();
        if (code==null || !code.toString().equals(code1)) {
            //不一致报错
            return Result.fail("验证码出错误");
        }
        //一致根据手机号查询用户O，query是ServiceImpl提供的,one是查找一个，
        // list()是查找多个
        User user = query().eq("phone", phone).one();
        //判断用户是否存在
        if (user == null) {
            //不存在创建新用户
            user=createUserWithPhone(phone);
        }


        //7保存用户到redis
        //7.1生成Token
        String token = UUID.randomUUID().toString(true);
        //将user对象转成hash进行存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> usermap = BeanUtil.beanToMap(userDTO,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,usermap);
        String tokenString = LOGIN_USER_KEY+token;
        //设置有效期
        stringRedisTemplate.expire(tokenString,LOGIN_USER_TTL,TimeUnit.MINUTES);
        // session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //返回token
        return Result.ok(token);
    }
    public User createUserWithPhone(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        //保存用户
        save(user);
        return user;
    }
}
