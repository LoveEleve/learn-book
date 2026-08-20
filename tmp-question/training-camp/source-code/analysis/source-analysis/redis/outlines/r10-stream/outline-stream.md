# R-10b Stream — rax+listpack 双层消息队列

> 前置: [[R-10a-rax]] (rax 载体) + [[R-19-listpack]] (叶子存储) + [[R-26-list]] (阻塞消费) + [[R-29-pubsub]] (对照: 无持久化) | 引出: [[R-17-client-caching]] | 对照: [[R-29-pubsub]] (fire-and-forget vs PEL)
> 🔴 A (拆篇 2/2) | 6 KP | [模式: 双层存储 + 单调 ID + 主条目压缩 + 消费组 + 确认机制]
> Pass 2 闭环: q1(双层结构) q2(ID 语义) q3(XADD) q4(消费组) q5(读取面) q6(命令面)

**读者处境**: XADD 的消息 ID 怎么生成? 百万消息怎么存? 消费组 PEL 是什么? 这篇拆 Stream: rax+listpack 双层、ms-seq ID、主条目压缩、消费组确认、阻塞读取。

### 1. 双层结构 — rax + listpack

场景: Stream 怎么组织百万条消息?
源码路径:
- **stream 结构** (stream.h:16-24): rax 树 (ID→listpack) + length + last_id/first_id/max_deleted_entry_id + entries_added + cgroups
- **rax 键 = 128bit BE ID** (t_stream.c:520-524, streamAppendItem): ms-seq 大端拼 16B — **字典序 = ID 序** (harness 实证)
- **listpack 叶子**: 每 rax 节点一个 listpack, 存多个条目 (**stream-node-max-entries=100 默认**, config.c:3207; **STREAM_LISTPACK_MAX_SIZE=1<<30**, t_stream.c:33,457-459)
- **master entry 主条目** (t_stream.c:475-520): listpack 头的字段引用表 — 条目字段压缩 (重复字段只存偏移)
- 裁剪: streamTrim (L740+, 墓碑标记 deleted flag)
关键设计 (q1): **双层 = 索引 + 批量存储**: rax 按 ID 索引 listpack, listpack 内连续条目共享主条目字段 — 空间与查询平衡。[模式: 双层存储]
数据流: ID → rax 定位 listpack → 条目追加。

### 2. 消息 ID — ms-seq 128bit

场景: XADD * 的 ID 怎么生成? 能指定吗?
源码路径:
- **streamID** (stream.h:11-14): ms (毫秒时间戳) + seq (序列号)
- streamIncrID/DecrID (t_stream.c:78-117): seq 溢出 → ms+1, seq 归零; 回绕 (max-max → 0-0)
- **streamNextID** (L119-129): `ms > last.ms → 新 ms+seq0; 否则 last+1` — **时钟回退用 last 递增** (注释 L122-123 "never go backward")
- 指定 ID (streamAppendItem L417-437): seq_given (显式 seq) / 自动 (last.ms==ms → seq+1, 溢出 EDOM); **严格递增检查** (L439-441, ≤ last → EDOM)
- commandTimeSnapshot (L122): 命令快照时间 (脚本内一致)
关键设计 (q2): **单调递增 + 时钟回退保护**: seq 补位保证同 ms 内单调; 回退时沿用旧 ms 递增。[模式: 单调 ID]
数据流: XADD * → 当前 ms vs last → 新 ID 或 last+1。

### 3. XADD — 追加与裁剪

场景: XADD 的完整流程?
源码路径:
- xaddCommand (L1996): 参数解析 (MAXLEN/MINID/近似 ~/NOMKSTREAM) → streamAppendItem
- **streamAppendItem** (L408+): ID 生成 → 严格递增 → 大小检查 (totelelen > STREAM_LISTPACK_MAX_SIZE → ERANGE L457-459) → rax 尾 listpack 追加 / 新 listpack+主条目
- **条目编码** (L520+): master entry 引用字段 + 逐字段编码
- **裁剪联动**: streamTrimByLength/ByID (L862-873, TRIM_STRATEGY_*) — **近似裁剪** limit=100×stream_node_max_entries (L866-867)
- 传播: XADD 命令原样 (R-9 交叉)
关键设计 (q3): **追加 + 裁剪一体**: XADD 时可选 MAXLEN/MINID 即裁即写; 近似模式批量删减。[模式: 追加裁剪]
数据流: XADD → ID → 尾 listpack 追加 → MAXLEN? → 裁剪。

### 4. 消费组 — CG/PEL/NACK

场景: 多个消费者怎么协调?
源码路径:
- **streamCG** (stream.h:55-73): last_id (组游标) + entries_read + pel (全局待确认 rax) + consumers rax
- **PEL 双层**: 全局 pel (ID→NACK) + 消费者 pel (ID→同一 NACK 指针, stream.h:82-88) — **共享 streamNACK** (L92-97: delivery_time/delivery_count/consumer)
- **streamConsumer** (L76-89): seen_time/active_time + name + pel
- XGROUP CREATE (L2588): 初始 last_id + entries_read; SCG_INVALID_ENTRIES_READ=-1 (L114)
- **streamEstimateDistanceFromFirstEverEntry** (L114-116 注释): entries_read 精确/估算语义
关键设计 (q4): **PEL = 交付但未确认的消息集合**: 全局+消费者双 rax 共享 NACK — 确认/过期/转移的枢纽。[模式: 待确认队列]
数据流: 交付 → 双 PEL 入 → XACK → 双 PEL 出。

### 5. 读取面 — XREADGROUP/XACK/XCLAIM

场景: 消费/确认/转移的流程?
源码路径:
- **XREADGROUP/XREAD 同函数**: **xreadCommand (L2173) 统一处理** (GROUP 参数分流; commands.def:11201 实证 xreadgroup 也指向 xreadCommand); > last_id 读新消息 → 入 PEL; NOACK 选项跳过
- **XACK** (L2834): 双 PEL 摘除 + NACK 释放 — 确认语义
- **XCLAIM** (L3134): 过期未确认消息转移 (IDLE 阈值) → delivery_count++ / delivery_time 更新 — 幂等重投; XAUTOCLAIM (L3355)
- **streamReplyWithRange** (L1670): 迭代器遍历 + 墓碑跳过; **消费者 PEL 读走 streamReplyWithRangeFromConsumerPEL** (L37,1687)
- 阻塞: BLOCK 选项 → blocked.c 框架 (R-26 交叉, t_stream.c:2083 signalKeyAsReady 显式唤醒)
关键设计 (q5): **确认闭环**: 交付→PEL→确认/超时转移 — Stream 与 pubsub 的本质差异 (R-29 对照)。[模式: 确认闭环]
数据流: XREADGROUP → PEL 入 → 处理 → XACK → PEL 出。

### 6. 命令面 — XTRIM/XSETID/XDEL/XINFO

场景: 管理命令的语义?
源码路径:
- **XTRIM** (L3616): MAXLEN/MINID + 近似/精确 — streamTrimByLength/ByID
- **XSETID** (L2755): 手工设 last_id (灾难恢复) + entries_added
- **XDEL** (L3534): **墓碑删除** (streamDeleteItem) — max_deleted_entry_id 更新; 不物理移除 (跳过逻辑)
- **XINFO** (L3862): STREAM/GROUPS/CONSUMERS 子命令 (内省)
- XLEN (L2157) / XRANGE/XREVRANGE (L2147) / XREAD (L2173, $ 语义)
关键设计 (q6): **删除 = 墓碑**: XDEL 只标记, rax/listpack 结构不动 — 与 Redis 其他类型 (物理删除) 不同。[模式: 墓碑]
数据流: XDEL → 标记 deleted → 迭代跳过 → 裁剪时物理清除。

### 负面空间 — Stream 刻意不做的事

- **不做消息超时自动清理**: PEL 永久保留 (XCLAIM 才转移)
- **不做消费者自动注册删除**: 消费者永不自动移除 (XINFO 可见)
- **不做多组广播差异**: 各组独立 last_id, 无共享投递状态
- **不做 ID 复用**: 删除后 ID 不回收 (单调)
- **不做跨实例消费协调**: 无分片/无多主 (Cluster 单 key 单主)
- **不做离线重放**: XREADGROUP > 只读新消息

→ 引出: 客户端缓存怎么实现? → [[R-17-client-caching]]
