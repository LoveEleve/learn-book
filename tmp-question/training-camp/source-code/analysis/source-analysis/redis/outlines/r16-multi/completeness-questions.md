# R-16 事务 — completeness-questions

## 开发者视角

1. MULTI 后命令怎么入队? QUEUED 回复?
2. 入队期错误怎么处理 (DIRTY_EXEC)?
3. EXEC 的双失败路径 (nullarray vs EXECABORT)?
4. WATCH 怎么注册? 修改键怎么失效?
5. touchWatchedKey 为什么立即 unwatch?
6. DISCARD 的清理链?
7. WATCH inside MULTI 为什么禁止?
8. CMD_NO_MULTI 哪 4 个命令?

## 架构师视角

9. watchedKey 嵌入式 listNode 的设计收益?
10. EXEC 为什么不能有阻塞命令 (DENY_BLOCKING)?
11. ACL 复查的时机设计 (入队 vs 执行)?
12. signalModifiedKey 双失效 (WATCH+tracking) 的统一性?
13. FLUSHDB/SWAPDB 全量失效为什么不能迭代中退订 (UAF)?
14. isWatchedKeyExpired 的 expired 位语义?
15. 传播: EXEC 在 AOF/从库怎么包裹?
16. mstate 回写 (命令改 argv) 的意义?

## 学生视角

17. MULTI 原子性的本质是什么? (单线程顺序执行)
18. WATCH 一个不存在的键有意义吗? (键被创建也失效)
19. DIRTY_CAS 后还能 UNWATCH 恢复吗?
20. 事务内执行错误命令 (如 INCR 字符串) 会中断事务吗?
