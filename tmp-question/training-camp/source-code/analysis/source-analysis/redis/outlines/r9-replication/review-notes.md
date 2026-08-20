# R-9 复制 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: PSYNC 协议 ✅ / replBacklog ✅ / replid+offset ✅ / REPL_STATE 状态机 ✅ — 全验证 | 记录 |
| 2 | harness 实证 | **36 断言 ASan+LSan clean, 3 轮迭代抓真实语义**: (1) 块边界计算 (10KB 跨 16KB 块, 测试设计修正) (2) **backlog 自引用首块** (feedReplicationBuffer L393-397 — 源码初次 feed 引用, harness 初始漏实现) (3) **裁剪后新 head 引用转移** (L274-278 — 裁剪 refcount 守恒) | harness 迭代 |
| 3 | 常量确认 | repl-backlog-size 默认 **1MB** (config.c:3208, 1024*1024); REPL_BACKLOG_TRIM_BLOCKS_PER_CALL=64 (server.h:482); REPL_BACKLOG_INDEX_PER_BLOCKS=64 (server.h:486); CONFIG_REPL_BACKLOG_MIN_SIZE=16KB (server.h:116) | 记录 |
| 4 | 机制洞察 | **裁剪 refcount 守恒**: backlog 自引用 (L393-397) + 从库引用 (L382-387) + 裁后转移 (L274-278) — 首块 refcount==1 (仅 backlog) 才可裁, 保证部分重同步最大化接受 (L252-257 注释) | 大纲节 1 |
| 5 | 机制洞察 | **FULLRESYNC 延迟应答携带 RDB 时刻 offset** (L808-813 注释) — 全量+增量无缝衔接的协议设计 | 大纲节 3 |
| 6 | 行号验证 | 全函数 ~50 锚点 grep 穷举 (L102-113/242-295/315-413/598/718-814/915-1044/1184-1255/1385/1487/1853/2437-2604/2608-2877/3254/3292-3386/3521/3704; server.h:482-486; config.c:3208) | 记录 |

## 07 五维度

### 维度1 功能正确性
- PSYNC 双 ID + 范围双检逐条件验证
- 状态机每步转移 (auth/port/ip/capa 跳过逻辑)
- 部分重同步 offset+1 语义
- 裁剪三条件 (refcount/留 1 块/不超限)

### 维度2 性能
- 共享块链零复制扇出
- rax 索引 64 块/索引 O(log n) 定位
- 增量裁剪 64 块/次防冻结
- 无盘管道扇出

### 维度3 内存
- 引用计数共享 (backlog+从库)
- 块大小自适应 (size/16 上限)
- 慢从库输出缓冲限制
- cached_master 会话保持

### 维度4 一致性
- replid 代际演进 (PSYNC2)
- ACK 偏移级确认
- 空库交换原子性
- 首 ACK 门控

### 维度5 负面空间 (已写入大纲 6+6 条)
- 全量: 无增量 RDB/断点续传/独立 bgsave; 增量: 无无限保留/命令级确认/多代 ID

## 结论
R-9 全部锚点行号 ~50 处验证, 12 闭环完成 (6+6), **机制洞察 2 + 常量 3**。harness 36 断言 ASan clean (3 轮迭代)。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-8a/R-28/R-2/R-16 (序号 < 25) 均已讲 ✅; 引出 R-14/R-15 (未来域 OK); 对照 R-8b (AOF 同源) ✅; 两篇各自五结构元素齐备 ✅; 读者处境场景化 (断线重连/backlog 环形/ACK) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 两篇合计 ~80 锚点 (file:line) — 🔴A 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-14/R-15 仅作引出; R-9b 引用 R-9a 属同域拆篇) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (AOF 同源/逐条) / 通知 (module 事件) / 键空间 (从库过期不删) / 内存 (共享缓冲) / 阻塞 (WAIT) / 超时 (cron) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 两篇负面空间 6+6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲节 1 (R-9b) "裁剪后新 head 引用转移" — harness 实证: 缺此逻辑 refcount 不守恒, 从库引用语义破坏 — 已修复并断言 (refcount==2) ✅ | harness + 大纲 |
| 13 | **表述精确化** | 大纲节 1 (R-9b) "块大小自适应 max(backlog_size/16, 16KB)" — 源码 L349-350 `max(size/16, PROTO_REPLY_CHUNK_BYTES)` 上限 + `min(max(len, CHUNK), limit)` — 表述补全 | 大纲节 1 已含 |
| 14 | 通过项 | 其余 ~45 句机制描述逐句对源码一致 ✅ (状态机 9 步/双 ID 裁决/范围双检/延迟应答/四情形 bgsave/双模式传输/临时文件/空库交换/首 ACK 门控/共享块链/引用计数/rax 索引/ACK+GETACK+fack/PSYNC2 移位/cached_master 复活/WAIT) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证 refcount 守恒), 全部验证。反写测试结论: 两篇大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部推断/区间语义反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | **PSYNC 范围半开区间** | 源码 L756-758: 拒绝 `psync_offset < offset \|\| psync_offset > offset+histlen` — 允许 `[offset, offset+histlen]` = `[首字节, master_repl_offset+1]`; **从库完全追上 (offset+histlen) 也可 CONTINUE (续 0 字节)** — harness 原实现 `<= master_repl_offset` **窄了 1**, 已修正 + 新断言 (last_plus_1 可续) ✅ | 通过 (harness 修正) |
| V2 | REPL_STATE 全集 | server.h:430-444 = **13 状态** (握手子集 9 + TRANSFER/CONNECTED/NONE/CONNECT) — 大纲补全集 | 通过 |
| V3 | 从库 ACK 驱动 | replicationCron L3739-3744: 每秒 + **CLIENT_PRE_PSYNC 老协议不 ACK** (L3741-3742) — 大纲补条件 | 通过 |
| V4 | CONNECTED 超时 | cron L3727-3732: master->lastinteraction 超 repl_timeout → **freeClient(master)** (断线) — 大纲补具体 | 通过 |
| V5 | replica-serve-stale-data | config.c:3076 = 默认 1 (别名 slave-serve-stale-data) ✅ | 通过 |
| V6 | offset+histlen 数学 | offset = master_repl_offset - histlen + 1 (L293-294) → offset+histlen = master_repl_offset+1 — 与半开区间自洽 ✅ | 通过 |
| V7 | 块引用守恒再验证 | backlog 自引用 (L393-397) + 从库引用 (L382-387) + 裁后转移 (L274-278) + freeReplicaReferencedReplBuffer 释放 (L298-308) — refcount 全生命周期闭环 ✅ | 通过 |
| V8 | PSYNC2 offset 边界 | replid2 有效条件 `offset ≤ second_replid_offset` (L730-732) + +CONTINUE 时 second_replid_offset=当前+1 (L2553) — 一致 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **区间语义偏差** | harness psync_in_backlog_range 用 `<= master_repl_offset` — **源码允许到 master_repl_offset+1** (半开区间含追上态); 修正 + 新断言 | harness 修正 + 大纲节 3 补半开区间 |
| 16 | 覆盖缺口 | 大纲节 4 断线检测笼统 ("超时无 ACK → 断开") — 实测 cron 双超时: TRANSFER (L3720-3726) + **CONNECTED 靠 master->lastinteraction → freeClient(master)** (L3727-3732); 且从库 ACK 有 CLIENT_PRE_PSYNC 例外 (L3741-3742) | 大纲节 4 重写 |
| 17 | 覆盖缺口 | 大纲 R-9a 节 1 未提 REPL_STATE **全集** (13 状态含 TRANSFER/CONNECTED) — 补全集 + 三超时 | 大纲 R-9a 节 1 补 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **3 处** (**区间语义偏差 1 (harness 实证修正)** / 覆盖缺口 2), 全部修复。锚点逐句 re-grep 无行号偏移。两篇大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-13, 聚焦浅读区段精读)

> 动机: 追加 REVIEW — 精读前三轮未细读的区段 (addReplyReplicationBacklog/syncCommand 四情形/sendBulkToSlave/shiftReplicationId/WAIT/replicationCacheMaster), 反推验证。

## 精读验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V9 | addReplyReplicationBacklog 定位 | L598-663: skip=offset-首字节 → **rax 近似定位** (> 游标找 ≤offset 索引 L619-644) → 精确线性扫描 (L647-651) → **引用设置** (refcount++ + ref_repl_buf_node + ref_block_pos L654-660) → 返回 histlen-skip — 与 feedReplicationBuffer 共享缓冲完全一致 ✅ | 通过 |
| V10 | syncCommand 四情形 | CASE1 disk bgsave 进行中 → **能力/需求匹配 + copyReplicaOutputBuffer** (L1057-1072); CASE2 socket bgsave → 等下一个; CASE3 无 → diskless+EOF+delay 延迟 / startBgsaveForReplication (L1091-1108) ✅ | 通过 |
| V11 | sendBulkToSlave 前导 | L1392-1409: **"$<len>\r\n" RESP 批量前导** (replpreamble) 先发, 再流式数据 ✅ | 通过 |
| V12 | shiftReplicationId 权威注释 | L1702-1706: second_replid_offset = master_repl_offset+1 — **注释明示 "slave will ask for the first byte it has not yet received"** — PSYNC offset+1 语义权威依据 ✅ | 通过 |
| V13 | WAIT 流程 | L3521-3553: 主库专用 → 非阻塞尝试 → blockForReplication → **replicationRequestAckFromSlaves 主动拉 ACK** ✅ | 通过 |
| V14 | replicationCacheMaster | L3292-3320: unlinkClient + **清未处理 querybuf/reply** (L3304-3315) 但 **保留 replid/reploff** (续传前提) ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 18 | 覆盖缺口 | 大纲 R-9a 节 4 "多从库共享一次 bgsave" 只讲无盘管道扇出 — **磁盘模式共享靠 CASE 1 能力匹配 + copyReplicaOutputBuffer** (L1065-1072) | 大纲 R-9a 节 4 补 |
| 19 | 覆盖缺口 | 大纲 R-9b 节 3 未提 addReplyReplicationBacklog 的 **rax 近似定位 + 精确扫描 + 引用设置** (L598-663) | 大纲 R-9b 节 3 补 |
| 20 | 补充锚点 | sendBulkToSlave **$<len>\r\n 前导** (L1392-1409) — 大纲未提 | 大纲 R-9a 节 4 补 |
| 21 | 补充锚点 | WAIT 的 **blockForReplication + 主动拉 ACK** (L3551-3553) + shiftReplicationId 权威注释 (L1702-1706) | 大纲 R-9b 节 4/5 补 |

## 四次 REVIEW 汇总

精读验证 6 项全通过 (V9-V14); 新发现 **4 处** (覆盖缺口 2 / 补充锚点 2), 全部修复。两篇大纲现覆盖 addReplyReplicationBacklog 定位机制、磁盘共享 bgsave、RESP 前导、WAIT 阻塞拉取 — 可支撑写作。
