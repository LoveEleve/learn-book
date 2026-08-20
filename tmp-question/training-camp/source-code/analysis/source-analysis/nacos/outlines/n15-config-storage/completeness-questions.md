# N-15 配置存储面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. PersistService 接口族的场景分型依据?
2. embedded vs external 的切换机制?
3. 原子/CAS 操作语义的并发保证?
4. 查询 handler 链的可插拔设计?
5. dump 变更驱动与全量的双路径?

## B. 源码实证 (6)

6. ConfigInfoPersistService 的原子方法? (grep L194-238)
7. 双实现族类名? (grep embedded/extrnal)
8. ConfigOperationService 的灰度分派? (grep L109-120)
9. 查询链的构建? (grep DefaultConfigQueryHandlerChainBuilder)
10. ConfigCacheService 的 CACHE? (grep L66)
11. 磁盘双实现? (grep disk/)

## C. 推理深挖 (5)

12. 灰度配置发布 vs 普通发布的差异?
13. ConfigMigrateService 1143 行的迁移语义?
14. dump 写锁失败的并发场景?
15. RocksDB 与文件实现的取舍?
16. 缓存与 DB 的一致性保证?

## D. 跨域扩展 (4)

17. CAS 操作 vs NC-2 publishConfigCas: 两端乐观锁闭合?
18. 查询链 vs N-12 推送 executor: 链式模式对照?
19. 缓存 vs NC-1 ServiceInfoHolder: 缓存设计对照?
20. 本域 vs openjdk 的 ClassFile 存储: 存储面规划对照?
