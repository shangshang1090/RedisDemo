package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.injector.methods.SelectList;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private ShopTypeMapper shopTypeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public List<ShopType> queryByZset() {
     //1查询redis判断店铺是否存在
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(RedisConstants.SHOP_TYPE_KEY + 1);
            if(map.isEmpty()){
                //3.不存在，按照升序查询mysql数据库
                QueryWrapper<ShopType> query = new QueryWrapper<>();
                query.orderByAsc("sort");
                List<ShopType> typeList = shopTypeMapper.selectList(query);

                if (typeList.isEmpty()){
                    //4.不存在，返回错误信息
                    new RuntimeException("数据库出错");
                }

                //5.存在，储存到redis,返回
                for (ShopType shopType : typeList) {
                    Map<String, Object> mm = BeanUtil.beanToMap(shopType,new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
                    stringRedisTemplate.opsForHash().putAll(RedisConstants.SHOP_TYPE_KEY+shopType.getSort(),mm);
                }
                return typeList;
            }
        //2，存在返回店铺
        List<ShopType> shopTypes = new ArrayList<>();
        for (int sort = 1; sort <= 10; sort++) {
            Map<Object, Object> m = stringRedisTemplate.opsForHash().entries(RedisConstants.SHOP_TYPE_KEY + sort);
            ShopType shopType = BeanUtil.fillBeanWithMap(m, new ShopType(), false);
            shopTypes.add(shopType);
        }
        return shopTypes;
    }
}
