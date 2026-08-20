# 闭环笔记 q7: 写路径发布链 — 每次写都广播 + 与 RD-4 钩子双重一致性

## 假设
每次 put 成功 → 发布 Invalidate/Update 消息 (带 excludedId 排除自己); 同时 RD-4 的 evictClientSideCaching 钩子也会触发 — 两层一致机制?

## 验证过程
- 写路径发布 (RedissonLocalCachedMap.java:361-401): putOperation → Redis 命令成功 → `encode(msg) + publish` (LocalCachedMapUpdate/Invalidate)
- RD-4 钩子 (CommandAsyncService.java:717-727): async() 写命令成功 → `evictClientSideCaching(name)` — ServiceManager 的 cachingInstances 集合
- 两层关系:
  - **RD-4 钩子**: 命令级通用 — 任何写命令成功都尝试 evict 对应缓存实例的本地
  - **RLocalCachedMap 发布**: 结构级精确 — 写后广播 Invalidate/Update 给所有订阅实例 (含跨实例)
- 实际: RLocalCachedMap 的写 (putOperationAsync) 走 RedissonLocalCachedMap 覆盖的路径 → 发布消息; RD-4 钩子对普通命令 (非本 map) 触发
- 为什么需要发布链: 跨实例一致性 (其他 JVM 的本地缓存也要更新) — RD-4 钩子只解决本实例
- 双重一致性: 本实例 = 本地 cachePut (写路径) + 排除自己; 他实例 = 订阅消息

## 代码类型
Glue (一致性双通道) — 本实例写路径 vs 跨实例订阅

## 跨域关联
- RD-4 (evictClientSideCaching) → 命令级钩子
- Q2/Q3 (消息+excluded) → 跨实例广播
- 面试点: "写之后其他实例怎么知道?"

## 结论
一致性双通道: 本实例写路径直接 cachePut (排除自己); 跨实例靠订阅广播 (Invalidate/Update 带 excludedId)。RD-4 钩子提供命令级通用失效, RLocalCachedMap 提供结构级精确广播 — 各司其职。
源码位置: RedissonLocalCachedMap.java:361-401, CommandAsyncService.java:717-727