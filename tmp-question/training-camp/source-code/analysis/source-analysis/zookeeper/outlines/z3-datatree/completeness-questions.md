# Z-3 DataTree — completeness-questions (全视角提问验证)

## 开发者视角

1. znode 树怎么存? (NodeHashMap 扁平路径 → 节点, 非真实树)
2. 创建节点校验什么? (父节点存在 + 不重复)
3. 删除节点怎么处理子节点? (必须有空? 还是父 removeChild)
4. setData 版本怎么校验? (乐观锁 BadVersionException)
5. ephemeral 节点怎么记? (sessionId → paths 集合)
6. ACL 怎么缓存? (ReferenceCountedACLCache 引用计数)
7. 快照怎么序列化? (DFS 路径+节点, "/" 结束)
8. digest 是什么? (tree digest — pre/postChange 钩子)

## 架构师视角

9. 为什么扁平 HashMap? (O(1) 查找 vs 树遍历; 一致性快照简单)
10. 为什么锁父节点? (兄弟创建互斥; 子操作不影响他子树)
11. cversion/pzxid 单调保护? (replay 模糊窗口防回退)
12. digest 校验的意义? (快照/日志重放一致性 — 跨节点对账)
13. committedLog 缓存? (Z-2 DIFF 数据源 — 广播与存储桥接)
14. ACL 引用计数? (共享 ACL 省内存 — 快照恢复 addUsage)
15. 对照 Redis? (无压缩/无指针 — ZK 为一致性强于性能)
16. multi 原子性? (子事务循环 + 失败回滚面 Z-4)

## 学生视角

17. znode 是什么? (数据节点 — 路径+数据+stat)
18. 什么是 stat? (版本号/时间戳/子版本等元数据)
19. 什么是快照? (全树序列化到磁盘)
20. 什么是版本号? (乐观锁 — 修改需匹配版本)
