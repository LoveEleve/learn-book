# R-26 上 — list 命令面: 双编码与推送弹出

> 前置: [[R-5-quicklist]] (唯一编码) + [[R-19-listpack]] (小编码) + [[R-21-db]] (键空间) | 引出: [[R-26-下]] (阻塞框架) | 对照: [[R-25-hash]] (编码单向升级同哲学)
> 🔴 A | 4 KP | [模式: 双编码+空键不变量+先试后阻+迭代器命令]
> Pass 2 闭环: q1(编码) q2(push) q3(pop) q4(范围/lmove)

**读者处境**: LPUSH 一个不存在键会发生什么?为什么 BLPOP 永远等不到"非空 list 被 push"?LPOP COUNT 和 LPOP 的回复格式差在哪?LMOVE 为什么是原子的?这篇拆 list 命令面: 双编码、推送与唤醒、弹出与空键、范围命令。

### 1. 双编码 — listpack 起步, quicklist 兜底

场景: list 内存怎么随规模变化?
源码路径:
- 编码 (t_list.c:110-127): listpack (小) / quicklist (大) — **GROWING 只影响 listpack** (L116-117), SHRINKING 只影响 quicklist (L122-123)
- 阈值 (config.c:3152): list-max-listpack-size 默认 -2 (quicklist 节点 8KB, R-5)
- 转换: listTypeTryConvertListpack (L21, 超限转 quicklist) / listTypeTryConvertQuicklist (L65, 缩容 + beforeConvertCB 回调)
- 创建: createListListpackObject (L477) — listpack 起步
关键设计 (q1): 双编码 = **小 listpack 零指针, 大 quicklist 分页**; 转换带回调钩子 (lazyfree 场景)。[模式: 双编码]
数据流: LPUSH → 创建 listpack → 增长 → 超节点限 → quicklist。

### 2. pushGenericCommand — 推送与唤醒

场景: LPUSH 怎么唤醒 BLPOP?
源码路径:
- pushGenericCommand (L464-492): lookup + **XX 不存在返回 0** (L470-471) → 创建+dbAdd (L475-476) → 转换预判 (L477) → listTypePush 批量 (L478-482) → notify "lpush"/"rpush" (L486-491)
- **唤醒只需 dbAdd 路径** (db.c:192 signalKeyAsReady): 空键不存在是 Redis 不变量 (pop 空即删键) → 阻塞者等待的键必然不存在 → push 必然走 dbAddInternal 唤醒
- stream 例外 (t_stream.c:2083): 键不因消费删 → 需显式 signalKeyAsReady
关键设计 (q2): **空键不存在不变量** = 唤醒点单一化 (dbAdd 即唤醒), list 命令面零唤醒代码。[模式: 空键不变量]
数据流: LPUSH 空键 → dbAdd → signalKeyAsReady → 就绪队列。

### 3. popGenericCommand — 弹出与空键

场景: 弹到空列表会发生什么?
源码路径:
- popGenericCommand (L757-809): COUNT 解析 (L762-769) → 不存在: hascount ? 空数组 : null (L771) → COUNT=0 快速路径 (L775-778)
- 单元素 (L780-789) / COUNT 范围 (L791-807: rangelen=min(count,llen), 首/尾段)
- **listElementsRemoved** (L736-755): **空 list → dbDelete** (L748-750, 维持"空键不存在") + signalDeletedKeyAsReady (L751, XREADGROUP) + notify "del"
- mpopGenericCommand (L811-845): 多键顺序尝试 + 传播重写 [LR]POP COUNT
- **BLPOP 先试后阻**: 先非阻塞尝试, 失败才 blockForKeys
关键设计 (q3): **pop 空即删键** (维持不变量) + 删除信号 (nokey 解阻); 阻塞 = "现在拿不到" 而非"将来一定等"。[模式: 先试后阻]
数据流: LPOP → 弹 → 空 → 删键 → 下个 push 走 dbAdd 唤醒。

### 4. 范围与移动 — 迭代器命令

场景: LRANGE/LTRIM/LSET/LMOVE 怎么实现?
源码路径:
- lrange (L856): 负索引归一 (R-24 同模式) → addListRangeReply (L704: quicklist 段 L657 / listpack 段 L678)
- lset (L601-635): listTypeReplaceAtIndex (L362, quicklist 节点直改) + 共享保护 (L618-621)
- linsert (L513-563): 找 pivot + listTypeInsert (L318)
- **lmoveCommand** (L1151): pop src → push dst (L1087) — 单线程原子; BLMOVE 加阻塞面 (**blmoveGenericCommand L1265-1280 是"先试后阻"样板**: key==NULL → blockForKeys L1274 / 否则常规 lmove L1279)
关键设计 (q4): 范围/移动 = quicklist API 封装 + 负索引归一; LMOVE = pop+push 原子组合。[模式: 迭代器命令]
数据流: LMOVE src dst → pop src → push dst → 唤醒 dst 阻塞者。

### 负面空间 — list 命令刻意不做的事

- **不做双向索引**: LINDEX O(n) 线性 (对照 R-6 zset 跳表)
- **不做消息确认**: 弹走即消费 (无 PEL, 对照 R-10 stream)
- **不做持久化队列**: 重启丢失 (非 RDB/AOF 语义, 属上层)
- **不做阻塞优先级**: FIFO 严格公平 (无优先队列)
- **不做空 list 保留**: 空即删 (内存不浪费)

→ 引出: BLPOP 等不到数据时谁在等?→ [[R-26-下]]
