# 闭环笔记 q6: Client-Side Caching 失效钩子 — 写命令 → evict 本地缓存

## 假设
async() 在写命令完成时若有 Client 侧缓存实例, 触发本地缓存失效 — 这是 RLocalCachedMap (RD-6) 的写侧一致性钩子。

## 验证过程
- CommandAsyncService.java:717-727:
  ```
  if (!readOnlyMode && getServiceManager().hasCachingInstances()) {
      Arrays.stream(params).filter(r -> r instanceof String).findFirst()
          .ifPresent(name -> mainPromise.thenAccept(r -> getServiceManager().evictClientSideCaching(name)));
  }
  ```
- 条件: **写命令 + 存在 caching 实例** → 取第一个 String 参数 (假定是 key name) → 命令成功后 evict
- serviceManager.hasCachingInstances (ServiceManager:774 `Set<RedissonClientSideCaching> cachingInstances`) — 全局注册的本地缓存实例集合
- evictClientSideCaching → 对应 RLocalCachedMap 实例的实际本地失效 (RD-6 详述)
- 设计: **客户端侧缓存失效 = 命令成功回调挂** — 不阻塞命令, 完成即失效 (最终一致)
- 这是"客户端本地缓存 + Redis 主数据"的一致捷径: 写本实例直接通知本地缓存, 跨实例靠订阅通道 (RD-6)

## 代码类型
Glue (一致性钩子) — 写路径自动失效本地缓存

## 跨域关联
- RD-6 (RLocalCachedMap) → evictClientSideCaching 消费点
- RD-1 (ServiceManager) → cachingInstances 注册
- 分布式一致性: 本实例写→本地立即失效; 他实例写→订阅通道广播 (RD-6 完整面)

## 结论
Client-Side Caching 失效钩子 = 写命令成功回调 → evictClientSideCaching(key)。本地缓存一致性的"就近失效"一半; 跨实例一半靠订阅。条件式触发 (有缓存实例才开), 零开销当无缓存。
源码位置: CommandAsyncService.java:717-727, ServiceManager.java:774