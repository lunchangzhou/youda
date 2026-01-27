package com.lcz.usercenter.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lcz.usercenter.mapper.UserMapper;
import com.lcz.usercenter.model.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 1onetw
 * @version 1.0
 * @Description: 定时任务 - 缓存预热
 */
@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RedissonClient redissonClient;
    // 重点用户
    private final List<Long> mainUserList = Collections.singletonList(1L);
    @Scheduled(cron = "0 12 1 * * *")
    public void doCacheRecommendUsers(){
        RLock lock = redissonClient.getLock("youda:precachejob:docache:lock");
        // 只有一个线程能获取到锁
        try {
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                String redisKey = String.format("youda:user:recommendUsers:%s", mainUserList);
                // 查数据库
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                Page<User> userPage = userMapper.selectPage(new Page<>(1, 20), queryWrapper);
                // 写缓存，10s 过期
                try {
                    ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                    valueOperations.set(redisKey, userPage,86400, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.error("推荐用户写缓存失败...");
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }


}
