# Pass 1 探索笔记: RD-6 RLocalCachedMap 本地缓存

> 方案 A (🔴) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 域规模: RedissonLocalCachedMap(1474)+cache/ 27 文件+LocalCacheListener → 拆 2-3 篇

## Pass 0 上下文吸收

- 核心: RedissonLocalCachedMap extends RedissonMap (L44) — **RMap 为远程载体 + LocalCacheView 本地层**
- LocalCachedMapOptions 四枚举: SyncStrategy (NONE/INVALIDATE/UPDATE) / ReconnectionStrategy (NONE/CLEAR/LOAD) / EvictionPolicy (NONE/LRU/LFU/SOFT/WEAK) / CacheProvider (REDISSON/CAFFEINE)
- cache/ 27 文件: 消息类 (Invalidate/Update/Clear/Disable/Enable/DisableAck/DisabledKey) + LocalCacheView + CacheMap 实现族
- LocalCacheListener (cache/LocalCacheListener) — 订阅处理: onMessage 按消息类型分发
- RD-4 已有写失效钩子 (evictClientSideCaching)

## 继承树/调用图

```
RLocalCachedMap 接口 → RedissonLocalCachedMap (1474) extends RedissonMap
  ├── LocalCacheView (L84) — 本地缓存视图
  │    ├── createCache (L312): CAFFEINE (Caffeine builder) / REDISSON (LRU/LFU/Soft/Weak/None)
  │    └── cacheKeyMap — 对象 key → CacheKey 哈希映射
  ├── LocalCacheListener (L87) — 订阅失效通道
  │    ├── onMessage (L220): Disable→清+ack / Enable / Clear→全清 / Invalidate→remove(hash) / Update→updateCache
  │    ├── excludedId 排除发送者
  │    └── ReconnectionStrategy: onSubscribe → CLEAR 全清 / LOAD loadAfterReconnection
  ├── 写路径 (L118-125,361-401): 写 Redis 成功 → 发布 Invalidate/Update 消息
  └── 读路径: 本地 hit → 返回; miss → 查 Redis → 回填本地

消息族 (cache/): LocalCachedMapInvalidate(instanceId, keyHash)
                  LocalCachedMapUpdate(instanceId, entries)
                  LocalCachedMapClear(instanceId, excludedId, releaseSemaphore)
                  LocalCachedMapDisable/Enable/DisableAck/DisabledKey
                  LocalCachedMessageCodec
```

## 基本元素分解

1. **RedissonLocalCachedMap 双层级** (L44) — RMap 远程 + LocalCacheView 本地
2. **LocalCacheView.createCache** (L312-350) — CAFFEINE (Caffeine builder TTL/idle/size) vs REDISSON (LRU/LFU/Soft/Weak/None)
3. **SyncStrategy 消息** (L118-125) — INVALIDATE→Invalidate(hash); UPDATE→Update(key,value)
4. **onMessage 分发** (LocalCacheListener L220-315) — Clear/Invalidate/Update/Disable/Enable; excludedId 排除
5. **ReconnectionStrategy** (L317-330) — onSubscribe: CLEAR 全清 / LOAD loadAfterReconnection
6. **消息族** (cache/ 9 类) — Invalidate/Update/Clear/Disable/Enable/DisableAck/DisabledKey/MessageCodec
7. **失效写路径** (L361-401) — 写 Redis 成功 → 发布失效消息 (带 excludedId 排除自己)
8. **RD-4 钩子联动** — async() 的 evictClientSideCaching (RD-4:717-727)

## 标记问题 (7 个)

1. **Q1 双层级读路径**: get 本地 hit?miss 怎么查 Redis 回填?write-through 语义?
2. **Q2 SyncStrategy 两消息**: INVALIDATE (hash 失效) vs UPDATE (值广播) 各适合什么?UPDATE 的带宽代价?
3. **Q3 excludedId 过滤**: 发送者怎么排除?多实例怎么协作?
4. **Q4 ReconnectionStrategy**: 断线重连后 CLEAR vs LOAD 的选择?loadAfterReconnection 增量补漏怎么实现 (更新日志 zset)?
5. **Q5 消息族完整性**: Disable/Enable (禁用窗口)、Clear (全清)、DisableAck 的用途?
6. **Q6 CacheProvider 双实现**: Caffeine vs Redisson 自研 (LRU/LFU/Soft/Weak) 差异?为什么支持两套?
7. **Q7 写路径发布链**: 每次 put 都发失效消息?RD-4 钩子与订阅失效的双重一致性?

## 已读测试 (2 个)

- RedissonLocalCachedMapTest (顶层): 本地缓存一致性验证
- (未深入 — RLocalCachedMap 相关测试族)

## 完成检查

- [x] 继承树/调用图已画出 (双层级+消息族)
- [x] 基本元素分解 8 项有源码位置
- [x] 7 个标记问题有源码位置
- [x] 已读测试 (RedissonLocalCachedMapTest)
- [x] 时空溯源: CHANGELOG 本地缓存失效演进 (4.x RClusteredLocalCachedMap)