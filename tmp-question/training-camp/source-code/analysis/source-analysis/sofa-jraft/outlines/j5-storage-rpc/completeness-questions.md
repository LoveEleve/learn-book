# J-5 存储与 RPC — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. RocksDBSegmentLogStorage 与 RocksDBLogStorage 什么关系? 继承还是组合?
2. 4K 阈值怎么配? 大日志走什么路径?
3. 16B 位置元数据里有什么? 怎么解码?
4. SegmentFile 的写入流程? wrotePos 和 committedPos 区别?
5. checkpoint 文件内容? 多久写一次?
6. abort 文件什么时候创建/删除? 残留意味着什么?
7. 段文件损坏怎么处理? .corrupted 什么时候出现?
8. AppendEntriesRequestProcessor 的 per-peer 单线程怎么实现?

## 架构师视角

9. RFC-0001 蓝图 vs 落地的差异? 为什么没完全文件化?
10. 双介质的读写放大权衡? 4K 阈值的取舍?
11. 预分配生产者-消费者的价值? mmap 缺页为什么是杀手?
12. 异步写 (占位+搬运) 的崩溃窗口? committedPos 怎么兜底?
13. 崩溃恢复三层 (checkpoint/abort/脏尾截断) 各解决什么?
14. 段内截断为什么要查 RocksDB 元数据? 混合架构代价?
15. SPI + priority 的可插拔设计? 加 jar 切换的机制?
16. pipeline 双端排序 (客户端 seq 消费 + 服务端 seq 投递) 的必要性?
17. gRPC 单方法复用怎么适配不同消息类型?

## SRE/运维视角

18. 段文件多大? 磁盘占用怎么预估? 1G 预分配会不会浪费?
19. 存储损坏怎么发现? 启动 fatal 的条件?
20. 换 RPC 框架的步骤? 灰度怎么搞?
21. checkpoint 周期和 fsync 成本的权衡?
22. 大日志场景 (业务 payload 大) 怎么调 4K 阈值?

## 研究者视角

23. vs Kafka 分段日志 (segment/offset index): 相似设计?
24. vs LevelDB/RocksDB LSM: 写放大问题的本质?
25. vs brpc: RPC 双实现 (Bolt/gRPC) 与 C++ 生态的对照?
26. mmap 预分配 vs 普通文件写: 页缓存语义差异?
27. RFC 的 logitLogStorage 若实现, 与当前混合式差异?

## 学生视角

28. 什么是日志存储? 和文件系统什么关系?
29. 什么是 mmap? 为什么快?
30. 什么是 SPI? 加 jar 换实现?
31. 什么是 checkpoint? 数据库里也有?
32. 什么是写放大? LSM 树的问题?
