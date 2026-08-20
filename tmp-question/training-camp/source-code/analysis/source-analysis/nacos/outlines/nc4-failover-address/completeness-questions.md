# NC-4 本地缓存+故障转移+地址管理 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. FailoverSwitchRefresher 的 5 秒轮询与三态分支?
2. failover 与正常缓存的差异事件为何走同一通道 (InstancesChangeEvent)?
3. ServerListProvider 的 order 排序 + match 首个命中的语义?
4. Endpoint 与 Properties 两种地址源的差异与取舍?
5. 磁盘缓存写/读的时机?

## B. 源码实证 (6)

6. FailoverReactor 的 SPI 加载逻辑? (grep L72-77)
7. 5 秒定时的实现? (grep L87-89)
8. isFailoverSwitch(serviceName) 的条件? (grep L159-161)
9. ServerListProvider 的加载排序? (grep AbstractServerListManager:50-58)
10. Endpoint 刷新间隔? (grep L63)
11. DiskCache.write 用什么写文件? (grep L73)

## C. 推理深挖 (5)

12. 从 failover 切回正常时, 差异事件对比谁? (L129-143)
13. 为什么 FailoverDataSource 只取第一个 SPI 实现?
14. failover 开关开启但数据为空时 serviceMap 不替换 (L120-123) — 为什么?
15. ServerListChangeEvent 发布后, NC-3 的 onServerListChange 怎么联动?
16. 配置的 failover 文件是用户创建的, 命名服务 failover 也是吗?

## D. 跨域扩展 (4)

17. FailoverReactor vs ALI-A5 的 ServiceCache 写穿: 两种容灾层次?
18. ServerListProvider 族 vs 1.x ServerListManager: 重构动机?
19. DiskCache vs NC-2 的 snapshot 文件: 命名/配置双面平行?
20. 本域容灾 vs SCC-6 CacheFlux: 客户端 vs 集成层缓存的边界?
