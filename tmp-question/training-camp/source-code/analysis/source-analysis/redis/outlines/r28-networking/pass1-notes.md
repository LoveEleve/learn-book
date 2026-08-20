# R-28 networking 协议 — Pass 1 探索笔记

> 域: R-28 networking 协议 (networking.c 4653) | 🔴 A 方案 | 2026-08-13
> 源码: src/networking.c (4653) | Redis 7.4.2

## 调用图

```
读路径:
acceptCommonHandler (L1318) → createClient (L112) → connSetReadHandler(readQueryFromClient) (L123)
readQueryFromClient (L2655): postpone (io threads, L2662) → readlen 决策 (L2667-2688)
  → querybuf 分配 (NonGreedy BIG_ARG / Greedy 多读 L2691-2707) → connRead (L2708)
  → sdsIncrLen (L2727) → querybuf 上限检查 (L2739-2755, 未认证 1MB) → processInputBuffer (L2759)
processInputBuffer (L2559): while qb_pos < len → reqtype 判定 (*→MULTIBULK 否则 INLINE, L2583-2589)
  → processMultibulkBuffer (L2292) / processInlineBuffer (L2174)
  → io 线程 → CLIENT_PENDING_COMMAND (L2606-2610) | 否则 processCommandAndResetClient (L2613)
  → querybuf trim (L2635-2644: master repl_applied / 普通 qb_pos) 
processMultibulkBuffer (L2292): * 行 → multibulklen (L2317-2333) → argv 分配 (min 1024, L2336-2338)
  → $ 行 → bulklen (L2361-2407, 上限 proto_max_bulk_len / 未认证 16384)
  → bulk 读取 (L2411-2444): 大参数零拷贝 (querybuf 借用 L2424-2435) / 普通 createStringObject
  → argv 增长 (L2416-2419, 2× 上限 INT_MAX)

输出路径:
addReply 家族 (L428+) → prepareClientToWrite (L278, 五拒条件) → _addReplyToBufferOrList (L387)
  → _addReplyToBuffer (L323, 静态 buf ≤16KB) → 溢出 → _addReplyProtoToList (L341, clientReplyBlock 链表)
  → 新节点 ≥16KB (zmalloc_usable) + closeClientOnOutputBufferLimitReached (L373)
写路径:
writeToClient (L1978) → _writeToClient (L1917): 从库 replBufBlock 共享 (L1919-1942)
  → 普通: 链表非空 writev (L1946-1953, _writevToClient L1844) / 单 buf 直写 (L1954-1965)
  → sentlen 部分写跟踪 → handleClientsWithPendingWrites (L2062) 批量尝试直写

生命周期:
freeClient (L1578): PROTECTED → async (L1583-1586); master → replicationCacheMaster (L1616-1623)
  → 释放链 (querybuf/reply/buf/watch/pubsub L1631-1664) → unlinkClient (L1675)
  → 从库联动 (RDB 子进程 kill L1687-1694)
```

## 基本元素分解

1. **client 结构**: 查询态 (qb_pos/querybuf/multibulklen/bulklen/argv) + 回复态 (buf/bufpos/reply 链表)
2. **读路径**: readlen 决策 (16KB 基准/BIG_ARG 精确读) + 分配策略 (NonGreedy/Greedy) + 上限防护
3. **RESP 解析**: * 多行计数 / $ bulk 长度 / argv 构建 / 大参数零拷贝 / 协议错误处理
4. **INLINE 解析**: 旧式空格分隔命令 (processInlineBuffer L2174)
5. **输出双缓冲**: 静态 buf (≤16KB) + clientReplyBlock 链表 (writev 批量)
6. **写路径**: writev/单写/从库共享 replBufBlock/sentlen 部分写
7. **回复控制**: prepareClientToWrite 五拒 (SCRIPT/CLOSE_ASAP/REPLY_OFF/MASTER/无 conn)
8. **释放链**: freeClient 全量清理 + async 队列 + master 缓存
9. **安全面**: 未认证限制 (1MB querybuf/16384 bulk) + proto_max_bulk_len + 协议错误集

## 标记问题 (10 个)

1. readlen 的 4 种决策路径 (16KB 基准/BIG_ARG 剩余/MASTER 扩大/avail)?
2. 大参数零拷贝怎么借用 querybuf?为什么 qb_pos==0 才借?
3. argv 数组怎么增长 (1024 起步/2×/INT_MAX)?
4. 输出双缓冲的切换条件 (静态 buf 满才链表)?
5. clientReplyBlock 节点怎么复用尾节点?
6. writev 的 iov 构建与 sentlen 部分写?
7. 从库为什么走 replBufBlock 而非 reply 链表?
8. freeClient 的释放顺序 (为什么先 querybuf 后 reply)?
9. 未认证限制的 1MB/16384 依据?
10. prepareClientToWrite 五拒条件?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): networking.c 初版 — RESP 解析/输出缓冲 (版权 2009-Present)
- 演进: INLINE (telnet 兼容) → RESP 2 → RESP 3 (HELLO, L3593)
- 4.0: writev 批量写 (_writevToClient); clientReplyBlock 16KB 节点
- 6.0: 共享回复缓冲 (replBufBlock, R-9 连接)
- 7.x: 大参数零拷贝 (PROTO_MBULK_BIG_ARG); 未认证限制 (1MB/16384); reqres (RESP3 请求响应日志)
- 演进: 输出缓冲从"单 buf"→"buf+链表双结构" (大回复换链表)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (协议解析): q2 (读路径) + q3 (解析循环) + q4 (多行解析) + q8 (安全面)
- 篇 2 (客户端 IO): q1 (生命周期) + q5 (输出缓冲) + q6 (写路径) + q7 (释放链)
