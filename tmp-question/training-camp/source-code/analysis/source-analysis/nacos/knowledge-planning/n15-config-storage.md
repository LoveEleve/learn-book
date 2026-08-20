# N-15 配置存储面 — 知识规划 (KP)

> 🔴 A | 模块: config/server/service (141 文件 21,000 行) | 版本: 3.0.3 | 3 篇大纲

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 持久化接口 | ConfigInfoPersistService:40 | 原子/CAS 族 |
| 2 | 双实现 | embedded (5) + extrnal (5) | 内嵌/外置 DB |
| 3 | 原子语义 | removeAtomic/updateCas | 并发控制 |
| 4 | 操作入口 | ConfigOperationService:64/88 | 灰度分流 |
| 5 | 查询链 | ConfigQueryHandlerChain | 可插拔 handler |
| 6 | 迁移 | ConfigMigrateService (1143 行) | 灰度收敛 |
| 7 | 缓存 | ConfigCacheService:53/66 | md5 门控 |
| 8 | dump 族 | DumpProcessor:40 + task/族 | 变更驱动转储 |
| 9 | 磁盘双实现 | ConfigDiskService:26 | Raw/RocksDb |
| 10 | 变更发布 | ConfigChangePublisher | distro 通知 |

## 02 高频坑
1. embedded (RocksDB+JRaft) vs external (DB) 双实现
2. updateCas 带 casMd5 — 不匹配拒绝
3. removeAtomic 条件删除
4. 发布灰度 (BETA/TAG) 分流
5. 查询走 handler 链 (可插拔)
6. dumpWithMd5 md5 相同跳过
7. 磁盘 Raw/RocksDb 双实现

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 持久化 | 接口族 / 双实现 / 原子/CAS |
| 操作 | publishConfig / 灰度分流 / 迁移 |
| 查询 | 链式 / builder / extractor |
| 缓存 | md5 门控 / CacheItem / 灰度缓存 |
| 转储 | DumpTask 族 / 处理器族 / 磁盘双实现 |

## 04 跨域桥接
- ← NC-2: publishConfigCas 两端乐观锁闭合
- ← NC-5: JRaft 落点 (embedded)
- → N-17: 长轮询消费缓存
- → 面试: "配置存储怎么组织" — 双实现 + 原子/CAS + 缓存转储
