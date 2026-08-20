# R-28 下 — 客户端 IO: 输出缓冲与生命周期

> 前置: [[R-28-上]] (解析) + [[R-2-events]] (写事件/扇出) | 引出: [[R-20-server]] (beforeSleep 写面) + [[R-16-transaction]] (watch) | 对照: [[R-2-下]] (写扇出扇入消费) + [[R-9-replication]] (replBufBlock 共享, 仅引用概念, 域详述在 R-9)
> 🔴 A | 4 KP | [模式: 双缓冲+writev 批量+共享引用+角色联动释放]
> Pass 2 闭环: q1(生命周期) q5(输出缓冲) q6(写路径) q7(释放链)

**读者处境**: 一条 GET 的回复怎么从 addReply 到客户端?为什么静态缓冲才 16KB?慢客户端怎么被断开?从库为什么"不持有"自己的回复缓冲?这篇拆客户端 IO: 双缓冲输出、writev 批量写、共享 repl 缓冲、释放链。

### 1. client 生命周期 — 创建即注册

场景: 新连接怎么变成可服务的客户端?
源码路径:
- createClient (networking.c:112-211): TCP_NODELAY (L120) + keepalive (L121) + **connSetReadHandler(readQueryFromClient)** (L123, 读事件注册 = R-2 aeCreateFileEvent)
- **双态结构** (L146-159 查询态 + L126-143 回复态): qb_pos/querybuf/multibulklen/bulklen/argv vs buf(16KB)/bufpos/reply 链表
- 回复链表 (L179-184): reply = listCreate + free/dup 方法
- 身份 (L128-136): next_client_id 原子递增 + resp=2 (RESP3 由 HELLO 切)
- 伪客户端 (L115-118): conn=NULL (Lua/AOF 加载)
- acceptCommonHandler (L1318): 上限检查 (maxclients) → createClient
关键设计 (q1): 查询态/回复态分离 = **解析与发送互不干扰**; 创建即挂读事件。[模式: 双态分离]
数据流: accept → createClient → 读事件 → 解析 → 执行 → 回复。

### 2. 输出双缓冲 — 16KB 静态 + 链表兜底

场景: 回复写哪?什么时候换链表?
源码路径:
- **prepareClientToWrite 五拒 + 挂队列** (L278-310): SCRIPT/MODULE (L281, 无 socket) / CLOSE_ASAP (L284) / REPLY_OFF|SKIP (L288-289) / MASTER (L293-294) / 无 conn (L296) → 全拒则不写; 否则挂写队列 (L305-306)
- **双缓冲切换** (L323-375): _addReplyToBuffer (L323): **reply 链表非空 → 不再写静态 buf** (L328); 静态写满 → _addReplyProtoToList (L341)
- **链表节点** (L349-375): 尾节点续写 (L349-359, 有剩余空间); 新节点 ≥16KB (PROTO_REPLY_CHUNK_BYTES, L364-365, zmalloc_usable 吃内部碎片); reply_bytes 记账 (L371); **超限断连** (L373, closeClientOnOutputBufferLimitReached)
- addReply 家族 (L428+): INT 编码转字符串 (L433-439) / deferred len (L726) / aggregate 族 (L973+)
- push 消息暂存 (L411-416): 订阅推送挂 pending_push_messages
关键设计 (q5): **≤16KB 零分配, 大回复换链表**; 链表一旦出现静态 buf 退休 (顺序一致)。[模式: 双缓冲]
数据流: addReply → 五拒检查 → 静态 buf? → 满 → 链表尾节点/新节点 → 超限断连。

### 3. 写路径 — writev 一次批量

场景: 一万个回复怎么高效发?
源码路径:
- writeToClient (L1978) → _writeToClient (L1917):
  - **从库分支** (L1919-1942): **replBufBlock 共享** (R-9: 从库直接引用复制缓冲块, ref_repl_buf_node + ref_block_pos); 块发完 refcount-- 移下块 (L1934-1939)
  - **普通分支** (L1944-1965): 链表非空 → **_writevToClient** (L1844-1910): iov = 静态 buf 剩余 + 链表节点 (L1851-1876); **iovmax = min(IOV_MAX, conn->iovcnt)** (L1846); **单轮 ≤64KB** (NET_MAX_WRITES_PER_EVENT, server.h:106, L1863)
    - 写后扣减 (L1883-1907): 静态先扣 (sentlen→bufpos 清零) → 链表逐节点 (sentlen 部分写 L1899-1901)
  - 链表空 + buf 有 → connWrite 直写 (L1954-1965)
- handler_installed (L1970-1977): io 线程调用 = 0 (不装写事件)
- 批量机会 (L2062): handleClientsWithPendingWrites (beforeSleep 前直写) + sendReplyToClient (L2053, 事件回调)
关键设计 (q6): **一次 writev 发静态+链表** (省系统调用); 从库零复制走共享块; 64KB 单轮上限防饿死事件循环。[模式: 批量+共享]
数据流: pending_write → 直写尝试 → 写不完 → 装写事件 → 事件循环 → writev → 未完继续。

### 4. 释放链 — 角色优先

场景: 客户端断开时都清理什么?
源码路径:
- freeClient (L1578-1740):
  - **PROTECTED → 转异步** (L1583-1586)
  - **master 缓存** (L1616-1623): replicationCacheMaster (R-9 部分重同步) — 协议错误/阻塞除外
  - **释放链** (L1631-1675): querybuf → 阻塞态 (L1635-1639) → watch (L1641-1643, R-16) → pubsub 全退订 (L1645-1652, R-29) → reply+buf (L1654-1656) → repl 引用 (L1657) → argv (L1658) → 内存记账移除 (L1668-1670) → unlinkClient (L1675)
  - **从库联动** (L1679-1697): WAIT_BGSAVE_END 无其他从库 → killRDBChild (L1687-1694); SEND_BULK 关 repldbfd
- freeClientAsync (L1742): 挂 clients_to_close 队列 (事件循环内安全释放)
- unlinkClient (L1451-1520): 摘事件 + 关 socket + 各链表摘除
关键设计 (q7): **角色先行** (master 缓存复用/从库资源释放), **再清数据面** (协议态→订阅面→内存面→IO 面); 异步队列防事件循环内自毁。[模式: 角色联动]
数据流: 断开 → PROTECTED? → master 缓存? → 释放链 → unlink → 从库杀 RDB。

### 负面空间 — 客户端 IO 刻意不做的事

- **不做输出压缩**: 回复原样发送 (客户端协议层无压缩)
- **不做背压调度**: 超限直接断连 (无降速/排队)
- **不做从库私有缓冲**: 从库复用 replBufBlock (省内存, R-9 管理)
- **不做零拷贝常态写**: writev 已批量, 无 sendfile 类优化
- **不做连接池**: 服务端无连接复用概念 (客户端侧的事)

→ 引出: 命令执行与回复的完整闭环 → [[R-20-server]]
