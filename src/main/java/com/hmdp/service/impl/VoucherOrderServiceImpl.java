package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;

    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher serviceById = iSeckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if (serviceById.getBeginTime().isAfter(LocalDateTime.now())) {
            //活动尚未开始
            return Result.fail("活动尚未开始");
        }
        //3.判断秒杀是否结束
        if (serviceById.getEndTime().isBefore(LocalDateTime.now())) {
            //秒杀结束
            return Result.fail("秒杀活动已经结束");
        }
        //4.判断库存是否充足
        if (serviceById.getStock() < 1) {
            return Result.fail("库存不足");
        }
        Long id = UserHolder.getUser().getId();
//        分布式锁版本一
        //创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order:" + id, stringRedisTemplate);
        //获取锁
        boolean tryLock = lock.tryLock(1200);
        //判断是否获取锁成功
        if (!tryLock) {
            return Result.fail("不允许重复下单");
        }
        // synchronized (id.toString().intern()) {
        //拿到代理对象，没有代理对象，则调用是无法进行事务管理的
        //默认是用this.creatVoucherOrder来执行的
        //解决办法
        //获取代理对象
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.creatVoucherOrder(voucherId);
        } finally {
            //释放锁
            lock.unlock();
        }
//    }
    }

    @Transactional
    public  Result creatVoucherOrder(Long voucherId) {
        //一人一单
        Long id = UserHolder.getUser().getId();


            //查询订单
            Integer count = query().eq("user_id", id).eq("voucher_id", voucherId).count();

            if (count > 0) {
                return Result.fail("用户已经购买过一张优惠卷");
            }
            //5.减库存
            boolean success = iSeckillVoucherService.update()
                    .setSql("stock=stock-1")
                    .eq("voucher_id", voucherId).gt("stock", 0).update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //6.创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            //6.1订单id
            long order = redisIdWorker.nextId("order");
            voucherOrder.setVoucherId(order);
            //6用户id
            voucherOrder.setUserId(id);
            //6.3设置代金券id存如数据库
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
//        返回订单id
            return Result.ok(order);
        }


}
