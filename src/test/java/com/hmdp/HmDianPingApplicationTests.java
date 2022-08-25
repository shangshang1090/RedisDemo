package com.hmdp;

import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopService;
import com.hmdp.service.IShopTypeService;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IShopTypeService typeService;
    @Autowired
    private ShopServiceImpl shopService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    // 创建一个可重用固定线程数的线程池
    private ExecutorService es = Executors.newFixedThreadPool(500);
@Test
    public void test1(){
    Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisConstants.SHOP_TYPE_KEY + 1);
    System.out.println(entries);
}
@Test
    public void test2(){
    shopService.saveShopToRedis(1L,10L);
}
@Test
    public void test4() throws InterruptedException {
    //控制线程等待，它可以让某一个线程等待直到倒计数结束，再开始执行。
    CountDownLatch latch = new CountDownLatch(10);
//每一个线程来了都生成100个id
    Runnable task = () ->{
        for (int i = 0; i < 5; i++) {
            long id = redisIdWorker.nextId("order");
            System.out.println("id="+id);

        }

        latch.countDown();

    };
    long begin = System.currentTimeMillis();
    for (int i = 0; i < 10; i++) {
        es.submit(task);


    }
    latch.await();
   long end= System.currentTimeMillis();
    System.out.println("time="+(end-begin));
}
@Test
    public void test5(){
    Runnable task = () ->{
        System.out.println("我进来了");
    };
    //es.submit(task);
    task.run();
}
@Test
    public void test7(){
    try {
        int a =1;
        int b =1;
        int c=a/b;
        return;
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        System.out.println("我被执行了");
    }
}

}
