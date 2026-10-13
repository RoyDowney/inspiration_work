package Utils;

import org.springframework.data.redis.serializer.RedisSerializer;

import org.springframework.data.redis.core.script.RedisScript;

import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author RoyDowney
 * @date 2025/7/4
 */
@Component
public class RedisUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 用于管理可重入锁的引用计数
    private final ConcurrentHashMap<String, LockInfo> lockInfoMap = new ConcurrentHashMap<>();
    // 用于延迟释放锁的线程池
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(10);

    /**
     * 原子执行字符串协议的 Lua 脚本（队列领取、确认等）。
     * 参数和返回字符串使用 UTF-8，不改变现有缓存方法的序列化方式；执行异常向调用方传播。
     */
    @SuppressWarnings("unchecked")
    public <T> T executeStringScript(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, RedisSerializer.string(),
                (RedisSerializer<T>) RedisSerializer.string(), keys, args);
    }

    public Set<String> keys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // 锁信息类，用于跟踪锁的引用计数和释放任务
    private static class LockInfo {
        final RLock lock;
        final AtomicInteger referenceCount;
        volatile ScheduledFuture<?> releaseTask;

        LockInfo(RLock lock) {
            this.lock = lock;
            this.referenceCount = new AtomicInteger(1);
        }
    }

    // =============== common ==========================
    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     * @return boolean
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效 失效时间为负数，说明该主键未设置失效时间（失效时间默认为-1）
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false 不存在
     */
    public boolean exists(String key){
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除对应的value
     * @param key string key
     */
    public void delete(String key) {
        if (exists(key)) {
            redisTemplate.delete(key);
        }
    }

    // =============== string ==========================
    /**
     * 写入缓存
     * @param key string key
     * @param value string value
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 写入缓存带有效期(默认时间单位:秒)
     * @param key string key
     * @param value string value
     * @param expireTime Long 过期时间
     * @return boolean
     */
    public boolean set(String key, Object value, Long expireTime) {
        try {
            if (expireTime > 0) {
                redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 写入缓存带有效期
     * @param key string key
     * @param value string value
     * @param expireTime Long 过期时间
     * @param timeUnit TimeUnit 时间格式
     * @return boolean
     */
    public boolean set(String key, Object value, Long expireTime, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 读取缓存
     * @param key string key
     * @return T
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 哈希添加
     * @param key string key
     * @param hashKey Object hashKey
     * @param value Object value
     */

    public void hmSet(String key, Object hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 哈希删除
     * @param key string key
     * @param hashKey Object hashKey
     */

    public void hmDelete(String key, Object hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }

    /**
     * 哈希获取数据
     * @param key string key
     * @param hashKey Object hashKey
     */
    public Object hmGet(String key, Object hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 哈希数量
     * @param key string key
     */
    public Long getHashSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /**
     * 列表添加左边添加
     * @param k string key
     * @param v Object v
     * @author Mr.Zhang
     * @since 2020-04-13
     */
    public void lPush(String k, Object v) {
        ListOperations<String, Object> list = redisTemplate.opsForList();
        list.leftPush(k, v);
    }

    /**
     * 列表添加右边添加
     * @param k string key
     * @param v Object v
     * @author Mr.Zhang
     * @since 2020-04-13
     */
    public void rPush(String k, Object v) {
        ListOperations<String, Object> list = redisTemplate.opsForList();
        list.rightPush(k, v);
    }

    /**
     * 从右边拿出来一个
     * @param k string key
     * @param t Long 超时秒数
     */
    public Object getRightPop(String k, Long t){
        return redisTemplate.opsForList().rightPop(k, t, TimeUnit.SECONDS);
    }

    /**
     * 列表获取数量
     * @param k string key
     * @return Long
     */
    public Long getListSize(String k) {
        return redisTemplate.opsForList().size(k);
    }

    /**
     * 列表获取
     * @param k string key
     * @param l long l
     * @param l1 long l1
     * @return List<Object>
     */
    public List<Object> lRange(String k, long l, long l1) {
        ListOperations<String, Object> list = redisTemplate.opsForList();
        return list.range(k, l, l1);
    }

    /**
     * 集合添加
     * @param key string key
     * @param value Object value
     */
    public void add(String key, Object value) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        set.add(key, value);
    }

    /**
     * 集合获取
     * @param key string key
     * @return Set<Object>
     */
    public Set<Object> setMembers(String key) {
        SetOperations<String, Object> set = redisTemplate.opsForSet();
        return set.members(key);
    }

    /**
     * 有序集合添加    排行榜使用
     * @param key string key
     * @param value Object value
     * @param score double scoure
     */
    public void zAdd(String key, Object value, double score) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        zset.add(key, value, score);
    }


    /**
     * 有序集合获取    排行榜使用
     * @param key string key
     * @param score double scoure
     * @return Set<Object>
     */
    public Set<Object> rangeByScore(String key, double score, double score1) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rangeByScore(key, score, score1);
    }

    /**
     * 有序集合获取所有元素（按score排序）
     * @param key string key
     * @return Set<Object>
     */
    public Set<Object> zRangeAll(String key) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.range(key, 0, -1);
    }

    /**
     * 有序集合获取所有元素及score（按score排序）
     * @param key string key
     * @return Set<ZSetOperations.TypedTuple<Object>>
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScoresAll(String key) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rangeWithScores(key, 0, -1);
    }

    /**
     * 有序集合删除元素
     * @param key string key
     * @param value Object value
     * @return Long 删除的元素数量
     */
    public Long zRemove(String key, Object value) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.remove(key, value);
    }

    /**
     * 获取有序集合大小
     * @param key string key
     * @return Long 集合大小
     */
    public Long getZSetSize(String key) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.size(key);
    }

    /**
     * 按score范围获取元素（带偏移量和数量限制）
     * @param key string key
     * @param min 最小score
     * @param max 最大score
     * @param offset 偏移量
     * @param count 数量
     * @return Set<Object>
     */
    public Set<Object> rangeByScore(String key, double min, double max, long offset, long count) {
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        return zset.rangeByScore(key, min, max, offset, count);
    }

    /**
     * 从左边取出一个元素
     * @param key 列表key
     * @param timeout 超时时间（秒）
     * @return 元素值
     */
    public Object getLeftPop(String key, Long timeout) {
        return redisTemplate.opsForList().leftPop(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 递增
     * @param key 键
     * @return long
     */
    public long incrAndCreate(String key){
        if (!exists(key)) {
            set(key, 1);
            return 1;
        }
        return incr(key, 1L);
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     * @return long
     */
    public long incr(String key, long delta){
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     * */
    public long decr(String key, long delta){
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 尝试获取分布式锁 (使用RedisTemplate实现)
     * @param key 锁的key
     * @return boolean
     */
    public boolean tryLock(String key) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1"));
    }

    /**
     * 释放分布式锁 (使用RedisTemplate实现)
     * @param key 锁的key
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 尝试获取可重入分布式锁 (使用Redisson实现)
     * 支持同一个key的多次请求继承锁，并在没有新请求时自动释放
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 锁的持有时间
     * @param timeUnit 时间单位
     * @return LockToken 用于释放锁的令牌
     */
    public LockToken tryReentrantLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        String fullKey = "reentrant_lock:" + lockKey;
        LockInfo lockInfo = lockInfoMap.get(fullKey);

        // 如果已经存在锁信息，增加引用计数
        if (lockInfo != null) {
            // 取消之前的释放任务（如果存在）
            if (lockInfo.releaseTask != null && !lockInfo.releaseTask.isDone()) {
                lockInfo.releaseTask.cancel(false);
            }
            lockInfo.referenceCount.incrementAndGet();
            return new LockToken(fullKey, lockInfo.lock);
        }

        // 获取新的锁
        RLock lock = redissonClient.getLock(fullKey);
        try {
            if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
                LockInfo newLockInfo = new LockInfo(lock);
                lockInfoMap.put(fullKey, newLockInfo);
                return new LockToken(fullKey, lock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 尝试获取可重入分布式锁 (使用Redisson实现)，默认参数
     * @param lockKey 锁的key
     * @return LockToken 用于释放锁的令牌
     */
    public LockToken tryReentrantLock(String lockKey) {
        return tryReentrantLock(lockKey, 10, 30, TimeUnit.SECONDS);
    }

    /**
     * 释放可重入分布式锁
     * 只有当引用计数为0时才会真正释放锁
     * @param lockToken 锁令牌
     * @param delay 延迟释放时间
     * @param timeUnit 时间单位
     */
    public void releaseReentrantLock(LockToken lockToken, long delay, TimeUnit timeUnit) {
        if (lockToken == null) return;

        LockInfo lockInfo = lockInfoMap.get(lockToken.lockKey);
        if (lockInfo == null || lockInfo.lock != lockToken.lock) {
            return;
        }

        // 减少引用计数
        int count = lockInfo.referenceCount.decrementAndGet();

        // 如果引用计数为0，安排延迟释放锁
        if (count <= 0) {
            lockInfo.releaseTask = scheduledExecutor.schedule(() -> {
                try {
                    if (lockInfo.referenceCount.get() <= 0) {
                        lockInfo.lock.unlock();
                        lockInfoMap.remove(lockToken.lockKey);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, delay, timeUnit);
        }
    }

    /**
     * 立即释放可重入分布式锁
     * @param lockToken 锁令牌
     */
    public void releaseReentrantLockImmediately(LockToken lockToken) {
        if (lockToken == null) return;

        LockInfo lockInfo = lockInfoMap.get(lockToken.lockKey);
        if (lockInfo == null || lockInfo.lock != lockToken.lock) {
            return;
        }

        // 强制释放锁
        try {
            if (lockInfo.releaseTask != null && !lockInfo.releaseTask.isDone()) {
                lockInfo.releaseTask.cancel(false);
            }
            lockInfo.lock.unlock();
            lockInfoMap.remove(lockToken.lockKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放可重入分布式锁，默认延迟1秒
     * @param lockToken 锁令牌
     */
    public void releaseReentrantLock(LockToken lockToken) {
        releaseReentrantLock(lockToken, 1, TimeUnit.SECONDS);
    }

    /**
     * 锁令牌类，用于标识和管理获取到的锁
     */
    public static class LockToken {
        private final String lockKey;
        private final RLock lock;

        public LockToken(String lockKey, RLock lock) {
            this.lockKey = lockKey;
            this.lock = lock;
        }

        public String getLockKey() {
            return lockKey;
        }

        public RLock getLock() {
            return lock;
        }
    }

    /**
     * 尝试获取分布式锁 (使用Redisson实现)
     * @param lockKey 锁的key
     * @param expireTime 过期时间(秒)
     * @return RLock
     */
    public RLock tryLockWithRedisson(String lockKey, long expireTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean isLocked = lock.tryLock(0, expireTime, TimeUnit.SECONDS);
            return isLocked ? lock : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 尝试获取分布式锁 (使用Redisson实现)，默认过期时间30秒
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock tryLockWithRedisson(String lockKey) {
        return tryLockWithRedisson(lockKey, 30);
    }

    /**
     * 尝试获取分布式锁 (使用Redisson实现)并启用看门狗机制
     * 注意：此方法不指定leaseTime参数，Redisson会启用看门狗自动续期机制
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param timeUnit 时间单位
     * @return RLock
     */
    public RLock tryLockWithRedissonAndWatchdog(String lockKey, long waitTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 不指定leaseTime参数，启用Redisson的看门狗机制
            boolean isLocked = lock.tryLock(waitTime, timeUnit);
            return isLocked ? lock : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 尝试获取分布式锁 (使用Redisson实现)并启用看门狗机制，默认等待时间10秒
     * 注意：此方法不指定leaseTime参数，Redisson会启用看门狗自动续期机制
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock tryLockWithRedissonAndWatchdog(String lockKey) {
        return tryLockWithRedissonAndWatchdog(lockKey, 10, TimeUnit.SECONDS);
    }

    /**
     * 获取分布式锁 (使用Redisson实现)并启用看门狗机制
     * 注意：此方法不指定leaseTime参数，Redisson会启用看门狗自动续期机制
     * @param lockKey 锁的key
     * @return RLock
     */
    public RLock lockWithRedissonAndWatchdog(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        // 不指定leaseTime参数，启用Redisson的看门狗机制
        lock.lock();
        return lock;
    }

    /**
     * 释放分布式锁 (使用Redisson实现)
     * @param lock RLock对象
     */
    public void unlockWithRedisson(RLock lock) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 获取Redisson客户端实例
     * @return RedissonClient
     */
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * 向Set添加元素
     * @param key 键
     * @param values 值
     * @return 成功添加的元素数量
     */
    public Long sAdd(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 从Set移除元素
     * @param key 键
     * @param values 值
     * @return 成功移除的元素数量
     */
    public Long sRem(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }

    /**
     * 判断元素是否在Set中
     * @param key 键
     * @param value 值
     * @return 是否存在
     */
    public Boolean sIsMember(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置键的过期时间
     * @param key 键
     * @param timeout 时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取Set中的所有元素
     * @param key 键
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    /**
     * 获取Set的大小
     * @param key 键
     * @return 元素数量
     */
    public Long sCard(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }
}