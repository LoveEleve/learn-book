# R-26 下 — 阻塞框架: 双向注册与就绪队列

> 前置: [[R-26-上]] (先试后阻) + [[R-21-db]] (blocking_keys/ready_keys) + [[R-20-server]] (beforeSleep) | 引出: [[R-10-stream]] (XREADGROUP 消费) + [[R-16-transaction]] (WATCH 对照) | 对照: [[R-2-events]] (事件循环面的唤醒)
> 🔴 A | 4 KP | [模式: 双向注册+就绪队列+防递归消费+命令重处理]
> Pass 2 闭环: q5(状态机) q6(双向注册) q7(就绪队列) q8(消费与超时)

**读者处境**: BLPOP 阻塞时客户端在干嘛?10 种阻塞类型怎么区分?为什么 LPUSH 只唤醒一次 (脚本里 push 三次)?唤醒后命令怎么"重新执行"?这篇拆阻塞框架: 状态机、双向注册、就绪队列、消费重处理。

### 1. 状态机 — CLIENT_BLOCKED 与 10 类型

场景: 一个阻塞客户端长什么样?
源码路径:
- **btype 10 种** (server.h:398-407): NONE/LIST/WAIT/WAITAOF/MODULE/STREAM/ZSET/POSTPONE/SHUTDOWN/LAZYFREE
- blockClient (blocked.c:67-80): CLIENT_BLOCKED (L75) + btype (L76) + **blocked_clients_by_type[btype]++** (L78, 就绪快检用) + **addClientToTimeoutTable** (L79, 超时表; 定义 timeout.c:95)
  - master 限制 (L69-74): 复制流不可阻 (除 MODULE/LAZYFREE/POSTPONE)
- unblockClient (L164-210): 清标志 + by_type 计数减 + queue_for_reprocessing → unblocked_clients (beforeSleep 消费)
- **超时** (L700-708): replyToBlockedClientTimedOut (null 回复) + PENDING_COMMAND 清 + unblock
关键设计 (q5): 阻塞 = 标志 + 类型 + **分类型计数** (O(1) 快检); 超时与数据两路径解阻。[模式: 状态机]
数据流: BLPOP → blockClient → 超时表 → cron 检查 → 超时 → null 回复。

### 2. blockForKeys — 双向注册

场景: "谁在等哪个键" 怎么记录?
源码路径:
- blockForKeys (L359-410): **client→key 侧** (L370-375: bstate.keys) + **db→client 侧** (L377-388: blocking_keys 的 list)
- **双向节点关联** (L389): bstate.keys 的 value = db 侧 list node — O(1) 解链
- **unblock_on_nokey 计数** (L393-401): blocking_keys_unblock_on_nokey 引用计数 (XREADGROUP)
- PENDING_COMMAND (L407-408) + blockClient (L409)
- releaseBlockedEntry (L507-540): 解链 → 空列表清键 → nokey 递减
关键设计 (q6): **两字典互链** (client 记"等哪些键", db 记"键被谁等") — 双向 O(1); 多键任一触发全醒。[模式: 双向映射]
数据流: BLPOP k1 k2 → bstate.keys{k1,k2} + blocking_keys{k1:[c], k2:[c]}。

### 3. 就绪队列 — 三级快检与防重

场景: LPUSH 怎么"通知"阻塞者?
源码路径:
- signalKeyAsReadyLogic (L447-494):
  - **快检 1: 类型可阻塞** (L451-455, OBJ→BLOCKED 映射)
  - **快检 2: 无该类型阻塞者** (L456-463, blocked_clients_by_type O(1))
  - **快检 3: 键无等待者** (L465-474, blocking_keys 查找)
  - **ready_keys dict 防重** (L476-486): 同键一次排队 (脚本/MULTI 多 push)
  - 入 server.ready_keys 列表 (L488-493)
- signalKeyAsReady (L542) / signalDeletedKeyAsReady (L546)
关键设计 (q7): **三级快检 + dict 防重** — 无阻塞者零开销 (每 push 只查计数); 多 push 只醒一次。[模式: 就绪队列]
数据流: push → 三级快检 → ready_keys dict → 列表 → beforeSleep 消费。

### 4. 消费与重处理 — 防递归 + 换列表

场景: 唤醒后命令怎么拿到数据?
源码路径:
- handleClientsBlockedOnKeys (L306-350): **防递归** (L310-313) + **新列表交换** (L322-348, BLMOVE 连环唤醒)
- handleClientsBlockedOnKey (L553-589): **FIFO** (L563) + 防无限循环 (L567) + **类型匹配** (L578-580: 键类型变了不误醒)
- unblockClientOnKey (L631-670): 释放 → unblock → **PENDING_COMMAND 重处理** (L648-668: processCommandAndResetClient 重新执行阻塞命令)
- 超时: null 回复 (L700-708)
关键设计 (q8): 消费 = **换列表支持连环唤醒** (BLMOVE 醒 BLMOVE) + 类型复核 + **命令重执行闭环** (阻塞命令从头跑, 现在有数据了)。[模式: 防递归消费]
数据流: beforeSleep → ready_keys → 类型匹配 → unblock → 重执行 BLPOP → 弹出成功。

### 负面空间 — 阻塞框架刻意不做的事

- **不做优先级阻塞**: FIFO 严格公平
- **不做多键部分唤醒**: 任一键触发全醒 (重执行时逐个试)
- **不做超时精确性**: cron 粒度 (hz 频率) 检查, 非精确定时器
- **不做跨 DB 阻塞**: blocking_keys 按 DB 隔离
- **不做阻塞嵌套**: 重执行时若仍无数据可再阻塞 (防无限循环计数)

→ 引出: stream 怎么复用阻塞框架 (XREADGROUP)?→ [[R-10-stream]]
