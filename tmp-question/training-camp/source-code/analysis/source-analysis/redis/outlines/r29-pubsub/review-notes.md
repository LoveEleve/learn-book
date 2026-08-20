# R-29 pubsub+notify — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: 双订阅面镜像 ✅ / notifyKeyspaceEvent 消费链 ✅ (15 文件 grep) / 慢消费者处理 ✅ (config.c:152 三元组) / 从库语义 ✅ (publishCommand L605-606) | 记录 |
| 2 | 机制洞察 | **pubsubtype 抽象**: 7.0 shard pubsub 靠 4 函数指针参数化, 订阅/退订/分发逻辑零重复 (pubsub.c:14-22,54-75) — 对照 R-12 HLL 的 RAW 编码思路不同 (参数化 vs 特化) | 大纲节 1 |
| 3 | 机制洞察 | **PUBLISH 传播双语义**: 非 cluster → forceCommandPropagation(PROPAGATE_REPL) 复制到从库 (从库二次分发); cluster → gossip 不走复制 (L590-595,605-606) — 从库 PUBLISH 可被订阅者接收 | 大纲节 3 |
| 4 | 精确化 | **A (NOTIFY_ALL) 只含 10 类** — 不含 KEYSPACE/KEYEVENT/KEY_MISS/LOADED/NEW; "A" 判定 (flags&ALL)==ALL (notify.c:54) — 易误写 "A 含全部" | 大纲节 5 + q5 |
| 5 | 边界验证 | CLIENT_DENY_BLOCKING 拒订阅 (pubsub.c:522 全局含 MULTI 豁免 / L707 shard 无豁免); 空频道删除防滥用 (L294-299); 零订阅批量退订回 null (L407-408) | 大纲节 2/4/6 |
| 6 | 行号验证 | 全函数 35 锚点 + 常量 16 锚点 + 跨文件 6 处 grep 穷举全部命中 (pubsub.c 14-748 / notify.c 19-124 / server.h 341,385,641-656,1222-1224,1989-1994 / server.c 1932-1940,2081,4112-4125,4596,5649 / networking.c 188-190,1548-1550 / config.c 152; commands.def) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双向注册摘除顺序 (先 client 后 server, incrRefCount 保护)
- 空频道删除条件 (dictSize==0)
- stringmatchlen glob 分发逐行验证
- notify 双频道格式逐字符 (sdsnewlen("__keyspace@",11) 等)

### 维度2 性能
- 频道 O(1) 精确查表 vs 模式 O(模式数) 扫描
- dictFindPositionForInsert 预定位 (一次哈希)
- 共享协议串 (零分配)
- updateClientMemUsageAndBucket 每接收者记账

### 维度3 内存
- pubsubMemOverhead 三 dict 用量
- 输出缓冲 pubsub 类 {32MB,8MB,60s}
- 空频道即删 (防 millions of channels 滥用)

### 维度4 一致性
- 客户端 dict ↔ 服务器镜像双引用一致性 (incrRefCount×2)
- PUBLISH 从库复制语义 (主从订阅者都能收)
- shard 按 slot (cluster 分片一致性)
- RESP2/RESP3 双轨回复

### 维度5 负面空间 (已写入大纲 6 条)
- 不做持久化/积压/确认/消费组/订阅持久化/DB 命名空间

## 结论
R-29 全部锚点行号 ~60 处验证, 6 闭环完成, **机制洞察 2 (pubsubtype 抽象/传播双语义) + 精确化 1 (A 掩码 10 类)**。推断 3 处显式标注。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-28/R-21/R-3/R-1 (序号 < 29) 均已讲 ✅; 引出 R-15/R-16/R-10 (未来域 OK); 对照 R-12 (MAY_REPLICATE) ✅; 五结构元素齐备 ✅; 读者处境场景化 (订阅后能否 GET/消息到从库/字母串/内存) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 大纲 ~50 锚点 (file:line) — 🟡B 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-15/R-16/R-10 仅作引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (PUBLISH 复制/gossip) / 通知 (keyspace 双频道) / 协议 (RESP2/3) / 键空间 (无 db 语义) / 内存 (缓冲三元组+MemOverhead) / 生命周期 (freeClient 退订) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 负面空间 6 条 ✅ (对照 Stream 引出自然); 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **行号偏移** | 大纲节 4 pingCommand 锚点写 (server.c:4603-4612) — 那是 pubsub 分支区间; **函数定义 L4596**; 修正为 "L4596, 分支 L4603-4612" | 大纲节 4 修正 |
| 13 | 补充锚点 | CLIENT_DENY_BLOCKING 拒订阅: 全局 L522 (MULTI 豁免) vs shard L707 (无豁免) — 补入大纲节 4 | 大纲节 4 修正 |
| 14 | **强断言实证** | 大纲节 3 "从库收到后给它的订阅者分发" — 传播机制实证: forceCommandPropagation(PROPAGATE_REPL) 使 PUBLISH 进 AOF/从库命令流, 从库 processCommand 执行 publishCommand → 本地分发 (推断链: 从库执行被传播命令的机制属 R-9, 本域只到 forceCommandPropagation 为止 — 标注) | 大纲节 3 已含, 记录 |
| 15 | 通过项 | 其余 ~45 句机制描述逐句对源码一致 ✅ (双镜像/预定位/空频道删/安全迭代器/glob/CLIENT_PUSHING/共享协议串/白名单/A 掩码 10 类/双频道格式/module 旁路/5 子命令/缓冲三元组) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **2 处修正** (行号偏移 1 / 补充锚点 1), 全部修复。反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | NOTIFY_ALL 组成 | 位运算穷举: 含 g,$,l,s,h,z,x,e,t,d = **10 类**; 不含 K,E,m,LOADED,n — Python 位掩码验证 ✅ | 通过 |
| V2 | 字符映射数 | notify.c:25-41 grep -c "case '" = **15 个** (A,g,$,l,s,h,z,x,e,K,E,t,m,d,n) — LOADED 无字符 ✅ | 通过 |
| V3 | slot 三函数等价性 | calculateKeySlot = cluster?keyHashSlot:0 (db.c:205-206); getKeySlot = current_client 缓存优先 (L210-221); keyHashSlot = CRC16 (cluster.h:43) — **cluster 下等价, 非 cluster 恒 0** ✅ | 通过 |
| V4 | 白名单命令数 | server.c:4112-4125 穷举: ping/subscribe/ssubscribe/unsubscribe/sunsubscribe/psubscribe/punsubscribe/quit/reset = **9 个** ✅ | 通过 |
| V5 | kvstore 容量 | pubsub_channels bits=0 单 dict (server.c:2691, ALLOCATE_DICTS_ON_DEMAND); shard 按 slot_count_bits + FREE_EMPTY_DICTS (L2693) — NUMSUB slot 0 的根因 ✅ | 通过 |
| V6 | receivers 计数 | L472 (频道) + L500 (模式) 分计 — **同一客户端可计 2 次** (双订阅面) ✅ | 通过 |
| V7 | 模式匹配大小写 | stringmatchlen 第 5 参 nocase=0 (pubsub.c:489) — **区分大小写** ✅ | 通过 |
| V8 | CLIENT_PUSHING 一致性 | 6 个 addReplyPubsub* 函数全用 old_flags 保存/复位模式 (L87-88,96 等) ✅ | 通过 |

## 新发现问题 (5 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **语义不完整** | 节 5 "NOTIFY_ALL = 10 类组合不含 K/E/m" — **漏 n (NOTIFY_NEW) 和 LOADED**; 穷举实证不含 K/E/m/n/LOADED (L656 注释只解释了 m) | 大纲节 5 修正 |
| 17 | **数字错误** | pass2-q5 "14 字母映射" — 实测 **15 字符** (含 A); LOADED 无字符映射仅 module 内部用 | pass2-q5 + 大纲节 5 修正 |
| 18 | 表述精确化 | 节 3 "receivers = 收到消息的客户端数" — 实测频道/模式分计 (L472/L500), **同时订阅频道+匹配模式计 2 次** (投递数非客户端数) | 大纲节 3 修正 |
| 19 | 表述精确化 | 节 3 模式匹配漏大小写语义 — stringmatchlen nocase=0 (L489) 区分大小写 | 大纲节 3 补注 |
| 20 | 补充锚点 | 节 6 NUMSUB 固定 slot 0 的根因 (kvstore bits=0 单 dict, server.c:2691) + slot 三函数等价性 (getKeySlot 缓存/calculateKeySlot/keyHashSlot) | 大纲节 6 补注 |

## 三次 REVIEW 汇总

推理验证 8 项全通过 (V1-V8); 新发现 **5 处** (语义不完整 1 / 数字错误 1 / 表述精确化 2 / 补充锚点 1), 全部修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。
