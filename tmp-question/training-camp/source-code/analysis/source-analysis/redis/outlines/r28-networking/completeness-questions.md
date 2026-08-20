# R-28 networking 协议 — completeness-questions

## 开发者视角

1. readQueryFromClient 的 readlen 有哪几种决策?
2. querybuf 什么时候用 NonGreedy 什么时候 Greedy?
3. processInputBuffer 的四个提前退出是什么?
4. RESP 多行解析的三行状态机是什么?
5. 大参数零拷贝的前提条件 (四个)?
6. 输出双缓冲的切换条件?
7. writev 的 iov 数组怎么构建?
8. freeClient 的释放顺序?

## 架构师视角

9. "多读省系统调用 vs 精确读省复制" 的权衡怎么落地?
10. 大参数零拷贝为什么要求 qb_pos==0 且恰好整包?
11. 未认证三级限 (10/16KB/1MB) 的设计依据?
12. 从库为什么走 replBufBlock 共享而非 reply 链表?
13. NET_MAX_WRITES_PER_EVENT=64KB 单轮上限的意义?
14. master 客户端 querybuf 为什么按 repl_applied trim 而非 qb_pos?
15. 静态 buf 16KB 的选择依据 (与 reply 节点同尺寸)?
16. 协议错误为什么不重试直接断连?

## 学生视角

17. redis-cli 发 "GET key\r\n" 的完整解析路径?
18. 一条 100KB 的回复: 从 addReply 到客户端经历了什么?
19. 慢客户端 (客户端缓冲超限) 会发生什么?
20. 从库断开时服务器除了释放还做什么?
21. 未认证客户端发一条大命令会发生什么?
