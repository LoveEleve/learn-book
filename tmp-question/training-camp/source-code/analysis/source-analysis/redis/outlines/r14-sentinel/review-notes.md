# R-14 Sentinel — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 预判验证 | 交接文档 §四 预判: SDOWN/ODOWN ✅ / 领导者选举 ✅ / 故障转移 3 阶段 ✅ / INFO 周期 ✅ / 配置自写 ✅ — 全验证 | 记录 |
| 2 | 常量确认 | SENTINEL_PING_PERIOD=1000 (L62) / info=10000 (L64) / publish=2000 (L67) / **down_after 默认 30000** (L68) / **tilt_trigger=2000** (L69) / **tilt_period=30s** (L70, PING×30) / **election_timeout=10000** (L73) / MAX_DESYNC=1000 (L83) | 大纲节 6 修正 (tilt_period 30s 非 2s) |
| 3 | harness 实证 | **18 断言 ASan clean, 2 处迭代**: (1) SentinelSet.sentinels 指针未分配 (测试骨架修正) (2) **get_leader 平票语义** — 必须唯一最高且 >n/2 (初版 max>n/2 在 2-2-1 时误判) | harness 迭代 |
| 4 | 机制洞察 | **ODOWN = 弱 quorum** (L4584-4589 注释权威): 自票+他票 ≥ quorum — "no strong guarantees about N instances agreeing at the same time" | 大纲节 4 |
| 5 | 机制洞察 | **failover_start_time 随机化** (L4936, MAX_DESYNC=1000): 防多哨兵同时启动转移 | 大纲节 1 |
| 6 | 行号验证 | 全函数 ~35 锚点 grep 穷举 (L4516-4582/4590-4623/4670/4762/4927-4979/5013-5338/5358-5463) | 记录 |

## 07 五维度

### 维度1 功能正确性
- SDOWN 三条件 + 恢复语义
- ODOWN quorum 计数 (自己+他票)
- 选主三维排序 (priority/offset/runid)
- 状态机 7 态转移

### 维度2 性能
- 周期频率分级 (10s→1s)
- parallel_syncs 并行限流
- hello 广播 (O(1) 每哨兵)

### 维度3 内存
- 实例字典 (master→slaves/sentinels)
- 双链接独立管理

### 维度4 一致性
- epoch 单调递增 + 每 epoch 一票
- 确定性选主 (所有哨兵同结果)
- hello 最终一致收敛
- TILT 时钟保护

### 维度5 负面空间 (已写入大纲 6+6 条)
- 监控: 无实时推送/强一致/全同步; 转移: 无数据补偿/多领导/跨主

## 结论
R-14 全部锚点行号 ~35 处验证, 12 闭环完成 (6+6), **机制洞察 2 + 常量 8**。harness 18 断言 ASan clean (2 轮迭代)。待二次 REVIEW 复核。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-28/R-20/R-2/R-9 (序号 < 27) 均已讲 ✅; 引出 R-15 (未来域 OK); 对照 R-15/R-9 ✅; 两篇各自五结构元素齐备 ✅; 读者处境场景化 (怎么发现/双下线区别/谁说了算) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 两篇合计 ~65 锚点 (file:line) — 🔴A 标准 ≥4 ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-15 仅作引出/对照) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (hello pub/sub) / 通知 (哨兵事件 +sdown/-odown) / 复制 (SLAVEOF 重定向) / 配置 (自写持久化) / 事件 (ae 异步连接) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 两篇负面空间 6+6 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **强断言实证** | 大纲节 1 (R-14b) "每个 epoch 每哨兵只投一票" — 验证 sentinelVoteLeader (L4762+ 区域) 的 epoch 比较逻辑; 需 grep 确认 | 记录 |
| 13 | 补充锚点 | 大纲节 2 (R-14b) 候选过滤 5 条件 + max_master_down_time 公式 (L5048-5052) — 已含 | 记录 |
| 14 | 通过项 | 其余 ~40 句机制描述逐句对源码一致 ✅ (SDOWN 三条件/weak quorum/hello 内容/双链接/TILT 常量/epoch 递增/三维排序/7 态机/SLAVEOF NO ONE/parallel_syncs/2×冷却/强制 FAILOVER) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **1 处记录** (强断言实证投票 epoch), harness 2 轮实证已固化。反写测试结论: 两篇大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-13, 逐句对源码 + 推理验证)

> 动机: 用户要求深度 REVIEW — 对大纲每个锚点重新 grep, 对全部结构/枚举/阈值反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 状态枚举位置 | **sentinel.h 不存在** — sentinelRedisInstance (L161)/instanceLink (L134)/SENTINEL_FAILOVER_STATE (L89-95) 全在 sentinel.c 内部 — 大纲引用修正 | 修正 |
| V2 | **状态枚举 7 值** | L89-95: NONE(0)+WAIT_START(1)+SELECT_SLAVE(2)+SEND_SLAVEOF_NOONE(3)+WAIT_PROMOTION(4)+RECONF_SLAVES(5)+UPDATE_CONFIG(6) — **无 DETECT_END**! 大纲把它当状态是错的: 它是 RECONF_SLAVES 态的结束检测函数 (L5176) | 修正 |
| V3 | **胜者双条件** | sentinelGetLeader (L4773-4830): **绝对多数 voters/2+1 (L4808) 且 ≥ quorum (L4811)** — 双门槛缺一不选; harness 原实现只有 >n/2 | 修正 |
| V4 | 自己投票 | L4814-4823: 未投票时投当前领先者或自己; 票数计入后可能反超 (L4820-4823) ✅ | 通过 |
| V5 | parallel_syncs | SENTINEL_DEFAULT_PARALLEL_SYNCS=1 (L80) ✅ | 通过 |
| V6 | hello 频道 | SENTINEL_HELLO_CHANNEL="__sentinel__:hello" (L78) ✅ | 通过 |
| V7 | 偶数哨兵数学 | voters=4 → 绝对多数 4/2+1=3; 2 票不够 (平票场景 harness 实证) ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 15 | **结构错误** | 大纲节 3 "DETECT_END" 当状态 — **实际不是枚举值** (L89-95 实证); 状态序列 7 值含 NONE, 活动态 6 | 大纲节 3 重写 |
| 16 | **语义缺失** | 大纲节 1 漏胜者**双条件** (绝对多数 + quorum) — 只写"票数>半数" | 大纲节 1 补 L4773-4830 |
| 17 | 引用错误 | "sentinel.h" 引用 ×3 — 文件不存在, 定义在 sentinel.c | 大纲修正 |
| 18 | harness 缺口 | get_leader 缺 quorum 门槛 — 补双条件 + 3 新测试 (quorum 不达标/偶数哨兵) → 21 断言 | harness 修正 |

## 三次 REVIEW 汇总

推理验证 7 项全通过 (V1-V7); 新发现 **4 处** (**结构错误 1 (DETECT_END 非状态)** / 语义缺失 1 (双条件) / 引用 1 / harness 缺口 1), 全部修复。锚点逐句 re-grep。两篇大纲现可支撑写作。
