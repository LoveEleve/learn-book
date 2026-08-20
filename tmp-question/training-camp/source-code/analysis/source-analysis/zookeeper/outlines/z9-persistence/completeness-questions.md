# Z-9 持久化 — completeness-questions (全视角提问验证)

## 开发者视角

1. 事务记录怎么落盘? (CRC 8B + len 4B + payload + 0x42)
2. CRC 覆盖什么? (只 payload — Javadoc 声称含 len+0x42 不符)
3. 什么时候 fsync? (commit → force(false), forceSync 默认 yes)
4. 日志文件多大切? (预分配 64MB + txnLogSizeLimit 可选)
5. 快照怎么校验? (三段 Adler32 seal + 尾部 "/" 标记)
6. 恢复怎么走? (快照 + read(zxid+1) 重放)
7. 无快照怎么办? (默认 throw; trustEmptySnapshot 放行; 空库建空快照)
8. 日志怎么清理? (PurgeTxnLog num≥3 + getSnapshotLogs 守卫)

## 架构师视角

9. 为什么快照+WAL 双文件? (快照定位 + WAL 增量 — 避免全量重放)
10. 为什么尾部残缺容忍/中部损坏致命? (append-only: 尾部=崩溃现场, 中部=不可解释)
11. 为什么从 zxid+1 重放? (fuzzy 快照可能已含到 X 的事务 — 边界对称)
12. 为什么 TRUNC 是 exclusive? (learner 领先 → 截到 maxCommittedLog 重新同步)
13. 为什么快照回退 100 个? (容忍连续损坏, 磁盘冗余换可用性)
14. 为什么 PurgeTxnLog 保跨界日志? (log.(X-a) 含 >X 事务 — 可恢复性守卫)
15. 对照 ES Translog? (WAL 同构; ZK 无压缩/无 group commit)
16. 对照 Redis AOF? (snapCount 随机快照 ≈ AOF rewrite; fsync 二态 vs 三档)

## 学生视角

17. 什么是 WAL? (先写日志再改内存 — 崩溃可重放)
18. 什么是 checksum? (Adler32 校验和 — 检测损坏)
19. 什么是快照? (全量状态拷贝 — 恢复起点)
20. 什么是 zxid? (事务编号 — 恢复边界)
