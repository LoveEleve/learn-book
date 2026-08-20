# J-3 快照压缩 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. snapshotIntervalSecs 和 snapshotLogIndexMargin 分别控制什么?
2. doSnapshot 的 EBUSY 什么时候触发? 保存和安装能并行吗?
3. SnapshotMeta 从哪来? lastAppliedIndex 怎么取?
4. writer.close 做了什么? 原子 rename 失败会怎样?
5. copier 的 filter 阶段干什么? 硬链接复用条件?
6. InstallSnapshot RPC 什么时候响应? 阻塞多久?
7. setSnapshot 的截断三分支分别什么时候触发?

## 架构师视角

8. "冻结"的实现为什么是队列串行而不是加锁暂停?
9. temp+原子改名的崩溃安全: 半成品快照哪去了?
10. 硬链接复用的前提是什么? checksum 比对成本?
11. 同步阻塞安装 vs 流式安装的取舍?
12. 为什么 setSnapshot 不推进 diskId? braft PR#224 的教训?
13. 截断"只到上次快照点+1"的保守理由? follower 可能还在拉?
14. 快照里的配置对 ConfigurationManager 的意义? 截断后怎么恢复成员历史?
15. 安装期间拒绝日志的代价? EBUSY 会触发 leader 做什么?

## SRE/运维视角

16. 新节点加入要多久? 快照大小/带宽怎么估?
17. 快照下载限流参数? 线上怎么调?
18. 快照损坏怎么发现? meta checksum?
19. 安装快照的节点对外表现? 业务会中断吗?
20. 磁盘空间: 快照+日志双占用怎么管理?

## 研究者视角

21. vs Kafka log compaction: 键值级 vs 全量快照, 场景差异?
22. vs MySQL: 全量备份+binlog vs 快照+日志, 同构吗?
23. vs Raft 论文: 快照章节的工程细节 (论文没有的)?
24. 文件级硬链接 vs 块级去重, 各适用什么?
25. vs braft: 同步安装模型是 C++ 原版语义吗?

## 学生视角

26. 什么是快照? 为什么叫"拍照"?
27. 什么是日志截断? 删日志安全吗?
28. 什么是断点续传? 分块下载怎么实现?
29. 什么是引用计数? 快照为什么不立即删?
30. 什么是元数据? 快照的 meta 文件里有什么?
