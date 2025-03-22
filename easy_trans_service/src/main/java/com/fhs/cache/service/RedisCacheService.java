package com.fhs.cache.service;

import java.util.List;
import java.util.Set;

/**
 * reidis 缓存服务
 *
 * @author wanglei
 * @version [版本号, 2015年8月4日]
 * @see [相关类/方法]
 * @since [产品/模块版本]
 */
public interface RedisCacheService<E> {
    /**
     * 为缓存添加一个obejct
     *
     * @param key 一个主键，如sys:user:admin
     * @param obj 主键对应的数据
     */
    void put(String key, E obj);

    /**
     * 根据key获取object
     *
     * @param key
     * @return key对应的对象
     */
    E get(String key);

    /**
     * 删除一个key
     *
     * @param key key
     * @return 影响的结果数
     */
    Long remove(String key);

    /**
     * 检查key是否已经存在
     *
     * @param key
     * @return
     */
    boolean exists(String key);

    /**
     * 添加数组到缓存中
     *
     * @param key  key
     * @param objs 数组
     */
    void addSet(String key, E[] objs);

    /**
     * 添加一个set到数组中
     *
     * @param key    key
     * @param objSet objset
     */
    void addSet(String key, Set<E> objSet);

    /**
     * 添加value 到set中
     *
     * @param key
     * @param value
     */
    void addSet(String key, E value);

    /**
     * 添加一个list到集合中
     *
     * @param key  key
     * @param list 需要添加的集合数据
     */
    void addSet(String key, List<E> list);

    /**
     * 判断set中有无value 有true
     *
     * @param key
     * @param value
     * @return
     */
    boolean contains(String key, Object value);

    /**
     * 删除set集合中指定的value
     *
     * @param key
     * @param value
     */
    void removeSetValue(String key, Object value);

    /**
     * 获取一个set
     *
     * @param key key
     * @return key对应的set
     */
    Set<E> getSet(String key);

    /**
     * 放入set可以直接取出list
     *
     * @param key key
     * @return 对应的set转为的list
     */
    List<E> getList(String key);

    /**
     * 添加一个字符串
     *
     * @param key   key
     * @param value value
     */
    boolean addStr(String key, String value);

    /**
     * 修改字符串
     *
     * @param key   键
     * @param value 值
     * @return 是否更新成功
     */
    boolean updateStr(String key, String value);

    /**
     * 根据key获取一个字符串
     *
     * @param key key
     * @return key对应的字符串
     */
    String getStr(String key);

    /**
     * 模糊删除一个key
     *
     * @param key key
     * @return 影响的结果数
     */
    Long removeFuzzy(String key);

    /**
     * 设置一个key在 timeout 秒后超时
     *
     * @param key     key
     * @param timeout 超时时间  秒
     */
    boolean expire(String key, int timeout);

    /**
     * 获取key超时时间
     *
     * @param key key
     * @return key的超时时间
     */
    Long getExpire(String key);

    /**
     * 从队列头插入值
     *
     * @param key
     * @param value
     */
    void leftPush(String key, E value);

    /**
     * 从队列尾部插入值
     *
     * @param key
     * @param value
     */
    void rightPush(String key, E value);

    /**
     * 从头开始取值
     *
     * @param key
     * @return
     */
    E getBLPop(String key);

    /**
     * 从后开始取值
     *
     * @param key
     * @return
     */
    E getBRPop(String key);

    /**
     * 获取list总数
     *
     * @param key
     * @return
     */
    Long getForListSize(String key);

    /**
     * 递增值
     *
     * @param key
     */
    public long incrAdd(String key);

    /**
     * 增加固定值
     *
     * @param key
     * @param value
     */
    public long incrAdd(String key, int value);

    /**
     * 递减值
     *
     * @param key
     */
    public long incrSub(String key);

    /**
     * 给redis 的channel 发送 message
     *
     * @param channel
     * @param message
     */
    void convertAndSend(String channel, String message);
}
