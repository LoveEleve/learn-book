# 闭环笔记 q3: MapLoader 读通 — 缓存未命中从数据源加载

## 假设
MapLoader.load(K) 是"读通" (read-through): RMap 读未命中 (Redis 无 key) → 调 load(K) 从外部数据源加载 → 可选回填 Redis。loadAll 批量预热。

## 验证过程
- MapLoader 接口 (api/map/MapLoader.java:28): `V load(K key)` (L36) + loadAll 族
- RedissonMap 构造器 (L77): options.getLoader() 存在时启用
- 触发: 读操作 (get/entrySet 等) Redis 未命中 → 检查 loader
- loadAll (MapLoader:?): 批量加载 (启动预热/显式调)
- 与 MapWriter 对称: writer 写通/写后 (Redis→外部), loader 读通 (外部→Redis 回填)
- 幂等/并发: loadAll 并行; load(K) 单 key (并发 miss 可能重复 load — 无去重)
- RetryableMapLoader? (未确认)

## 代码类型
Interface (读通回调) — 缓存未命中的外部来源

## 跨域关联
- Q1/Q2 (MapWriter) → 读写对称
- Redis R-21 (db 键空间) → 未命中语义
- 面试点: "MapLoader 什么时候被调?"

## 结论
MapLoader = 读通: Redis miss → load(K) 外部源 → 回填。与 MapWriter (写通/后) 构成"缓存 + 外部数据源"对称通道。并发 miss 无去重是已知边界 (调用方保障幂等)。
源码位置: api/map/MapLoader.java:28-36