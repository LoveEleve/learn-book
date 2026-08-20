# 闭环笔记 q5: 输出双缓冲 — 静态 buf + reply 链表

## 假设
回复先写 16KB 静态 buf, 溢出后走 clientReplyBlock 链表; prepareClientToWrite 是总开关。

## 验证过程
- prepareClientToWrite (networking.c:278-310) **五拒**:
  1. CLIENT_SCRIPT|CLIENT_MODULE → C_OK 不装 handler (L281, 无 socket)
  2. CLIENT_CLOSE_ASAP → C_ERR (L284, 不写)
  3. REPLY_OFF|REPLY_SKIP && !PUSHING → C_ERR (L288-289, 静默)
  4. CLIENT_MASTER && !FORCE_REPLY → C_ERR (L293-294)
  5. !conn → C_ERR (L296, AOF 加载伪客户端)
  6. 无 pending 且 io IDLE → putClientInPendingWriteQueue (L305-306) — 挂写队列
- **双缓冲**:
  - 静态: _addReplyToBuffer (L323-337): **listLength(reply) > 0 时不再写 buf** (L328) — 一旦进链表全走链表; 剩余空间写 (L330-335)
  - 链表: _addReplyProtoToList (L341-375): **尾节点续写** (L349-359, 有剩余空间就 memcpy 续); 新节点 ≥ PROTO_REPLY_CHUNK_BYTES (16KB) (L364-365, zmalloc_usable 带内部碎片); reply_bytes 记账 (L371); **closeClientOnOutputBufferLimitReached** (L373, 超限断连)
- 高层封装 (L428+): addReply (L428, INT 编码转字符串 L433-439) / addReplySds / addReplyProto / addReplyError 家族 / addReplyArrayLen 族 (L973+) / deferred len (L726)
- _addReplyToBufferOrList (L387-420): CLOSE_AFTER_REPLY 早退 (L388); 从库误回复 → 断连 (L394-399); push 消息暂存 (L411-416)
- buf_peak 统计 (L334-335)

## 代码类型
Mechanism (输出缓冲)

## 跨域关联
- R-1 (共享响应串/INT 编码) / R-20 (beforeSleep 写) / R-2 (pending write 队列) / R-32 (错误消息)

## 结论
输出 = "静态优先, 链表兜底" 双结构: ≤16KB 的回复零分配 (栈上 buf), 大回复换链表 (writev 批量)。**链表出现后静态 buf 不再用** — 保证顺序一致性。超限断连是输出面安全 (防慢客户端吃内存)。
源码位置: networking.c:278-375,387-420
