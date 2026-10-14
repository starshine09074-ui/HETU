package com.ruoyi.common.core.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author ruoyi
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * Optional process-local cache for development environments that do not run Redis.
     * This is disabled by default; cached sessions and tokens are lost when the process exits.
     */
    @Value("${app.cache.in-memory:false}")
    private boolean inMemory;

    private final ConcurrentMap<String, MemoryEntry> memoryCache = new ConcurrentHashMap<>();

    private static final class MemoryEntry
    {
        private final Object value;
        private volatile long expiresAtMillis;

        private MemoryEntry(Object value, long expiresAtMillis)
        {
            this.value = value;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private MemoryEntry getMemoryEntry(String key)
    {
        MemoryEntry entry = memoryCache.get(key);
        if (entry != null && entry.expiresAtMillis > 0 && entry.expiresAtMillis <= System.currentTimeMillis())
        {
            memoryCache.remove(key, entry);
            return null;
        }
        return entry;
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        if (inMemory)
        {
            memoryCache.put(key, new MemoryEntry(value, 0));
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
    {
        if (inMemory)
        {
            memoryCache.put(key, new MemoryEntry(value, System.currentTimeMillis() + timeUnit.toMillis(timeout)));
            return;
        }
        redisTemplate.opsForValue().set(key, value, Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        if (inMemory)
        {
            MemoryEntry entry = getMemoryEntry(key);
            if (entry == null)
            {
                return false;
            }
            entry.expiresAtMillis = System.currentTimeMillis() + unit.toMillis(timeout);
            return true;
        }
        return redisTemplate.expire(key, Duration.of(timeout, unit.toChronoUnit()));
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        if (inMemory)
        {
            MemoryEntry entry = getMemoryEntry(key);
            if (entry == null)
            {
                return -2;
            }
            return entry.expiresAtMillis == 0 ? -1 : Math.max(0, TimeUnit.MILLISECONDS.toSeconds(entry.expiresAtMillis - System.currentTimeMillis()));
        }
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        if (inMemory)
        {
            return getMemoryEntry(key) != null;
        }
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        if (inMemory)
        {
            MemoryEntry entry = getMemoryEntry(key);
            return entry == null ? null : (T) entry.value;
        }
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        if (inMemory)
        {
            return memoryCache.remove(key) != null;
        }
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        if (inMemory)
        {
            boolean deleted = false;
            for (Object key : collection)
            {
                deleted |= memoryCache.remove(String.valueOf(key)) != null;
            }
            return deleted;
        }
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        if (inMemory)
        {
            List<T> values = new java.util.ArrayList<>(dataList);
            setCacheObject(key, values);
            return values.size();
        }
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        if (inMemory)
        {
            List<T> values = getCacheObject(key);
            return values == null ? java.util.Collections.emptyList() : values;
        }
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        if (inMemory)
        {
            Set<T> values = new java.util.HashSet<>(dataSet);
            setCacheObject(key, values);
            throw new UnsupportedOperationException("Bound set operations are unavailable in process-local cache mode");
        }
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        if (inMemory)
        {
            Set<T> values = getCacheObject(key);
            return values == null ? java.util.Collections.emptySet() : values;
        }
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (inMemory)
        {
            setCacheObject(key, dataMap == null ? new java.util.HashMap<>() : new java.util.HashMap<>(dataMap));
            return;
        }
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        if (inMemory)
        {
            Map<String, T> values = getCacheObject(key);
            return values == null ? java.util.Collections.emptyMap() : values;
        }
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        if (inMemory)
        {
            Map<String, T> values = getCacheObject(key);
            if (values == null)
            {
                values = new java.util.HashMap<>();
            }
            values.put(hKey, value);
            setCacheObject(key, values);
            return;
        }
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        if (inMemory)
        {
            Map<String, T> values = getCacheObject(key);
            return values == null ? null : values.get(hKey);
        }
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        if (inMemory)
        {
            Map<String, T> values = getCacheObject(key);
            List<T> result = new java.util.ArrayList<>();
            for (Object hKey : hKeys)
            {
                result.add(values == null ? null : values.get(String.valueOf(hKey)));
            }
            return result;
        }
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        if (inMemory)
        {
            Map<String, Object> values = getCacheObject(key);
            return values != null && values.remove(hKey) != null;
        }
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        if (inMemory)
        {
            String regex = Pattern.quote(pattern).replace("\\Q*\\E", "\\E.*\\Q");
            Pattern matcher = Pattern.compile(regex);
            Set<String> keys = new java.util.HashSet<>();
            for (String key : memoryCache.keySet())
            {
                if (getMemoryEntry(key) != null && matcher.matcher(key).matches())
                {
                    keys.add(key);
                }
            }
            return keys;
        }
        return redisTemplate.keys(pattern);
    }
}
