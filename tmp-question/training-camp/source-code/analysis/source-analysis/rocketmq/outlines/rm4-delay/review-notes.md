# RM-4 延迟消息 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **线程模型**: deliverExecutorService = **maxDelayLevel 线程池** (每级一线程, L138) + 每级 schedule TimerTask (L153-156); async 时 + handleExecutorService (每级 HandlePutResultTask — SUCCESS→updateOffset/RUNNING→重排, L550-570) | 大纲 §3/§5 补注 |
| 2 | **补充锚点** | persist 定时 = scheduleAtFixedRate(**初始 10s** + flushDelayOffsetInterval) (L158-165) | 大纲 §5 补注 |
| 3 | **表述精确化** | correctDeliverTimestamp 语义: deliverTimestamp > now+levelDelay → now — **防永久等待** (到期时间异常超远, 如等级配置变更后), 非仅"时钟校正" | 大纲 §3 补注 |
| 4 | 验证 | start 的 load 前置 (L136); 每级 offset 从 offsetTable 续扫 (L146-148); delayLevel>0 才标记 (CommitLog L544-545); 测试 3 用例 (较 RM-3 7 专项少 — 标注) | 记录 |
| 5 | 行号验证 | 全函数 22 锚点 + 跨文件 8 处 grep (ScheduleMessageService 43-570 / MessageStoreConfig 224,255-256 / CommitLog 536-552 / MessageExtEncoder 相关) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 等级表解析 + 越界钳制
- tagsCode 到期标记/读取对称
- 投递循环三级节奏
- 还原字段完整 (REAL_*)

### 维度2 性能
- 每级独立线程 (无跨级争用)
- 轮询节奏 (100ms/10s)
- async 投递解耦等待

### 维度3 内存
- offsetTable 小 (18 级)
- deliverPendingTable (流控 2000/级)

### 维度4 一致性
- offset 持久化续扫
- TRANS_HALF 校验
- 版本计数 (状态机)
- 崩溃恢复 (load)

### 维度5 负面空间 (已写入大纲 5 条)
- 不秒级以下/不精度保证/不堆积告警/不等级热增/不事务混合

## 结论
RM-4 全部锚点 ~35 处验证, 6 闭环完成, **补充锚点 2 + 表述精确化 1**。怀疑审计全接受+补充。推断 3 处显式标注。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-3/RM-2 (已交付) ✅; 引出 RM-8/10 ✅; 对照 Kafka ✅; 读者处境场景化 ✅; 锚点 ~35 (≥4 ⏫) ✅; 负面空间 5 条 ✅; 横切 (并发/持久化/一致性/配置) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §2 "真实 topic/queue 存属性 REAL_TOPIC/REAL_QUEUE_ID" — **写前改道的具体代码位置未锚定** (在 broker 发送处理面 SendMessageProcessor, 非 store); 标注 | 大纲 §2 补注 (SendMessageProcessor 交叉, RM-5) |
| 8 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (18 级字符串/单位表/等级↔队列/钳制/tagsCode 到期/兜底 1s/每级任务/1s-100ms-10s 节奏/Ext 重算/TRANS_HALF 校验/还原字段/清属性/waitStoreMsgOK/async 流控 Blocked/JSON 持久化/load 双路径/版本计数/线程模型/配置 4 项) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (写前改道锚点归位 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 等级数 | "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h" 空格分割 = 18 级 ✅ | 通过 |
| V2 | 单位换算 | s=1000/m=60000/h=3600000/d=86400000; 2h=7200000ms ✅ | 通过 |
| V3 | 线程数 | deliverExecutorService = maxDelayLevel 线程 = 18 ✅ | 通过 |
| V4 | 到期时间链 | 写: storeTimestamp+delay (L548) → 读: correctDeliverTimestamp (L443) → 未到期重排/到期投递 — 闭环 ✅ | 通过 |
| V5 | 钳制数学 | 客户端 level=30 > max 18 → 钳到 18 (L541-544) — 超配延迟被限制 ✅ | 通过 |
| V6 | 还原清属性 | DELAY_TIME_LEVEL/TIMER_DELIVER_MS/TIMER_DELAY_SEC 三清 (L371-374) — 防二次延迟 ✅ | 通过 |
| V7 | 投递失败重排 | syncDeliver false → scheduleNextTimerTask(nextOffset, 100ms) — offset 续扫 ✅ | 通过 |
| V8 | 异步完成回调 | HandlePutResultTask: SUCCESS→updateOffset/RUNNING→重排 — 每级轮询 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **投递消息的流控/背压**: syncDeliver 依赖 putMessage 返回; async 时 putMessage 失败 (磁盘满等) 在 PutResultProcess 的 FAILED 状态处理 (L570+ 区域) — 失败面未穷举 | 大纲 §5 补注 (FAILED 状态面) |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (投递失败状态面), 修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查七个存疑点 (写前改道真实位置/REAL 残留/batch 断言/时钟回拨/重复投递/重试延迟/持久化竞态), 并做反写测试。

## 追查过程 (七个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 写前改道真实位置? | **HookUtils.transformDelayLevelMessage (L226-236)**: 钳制 maxDelayLevel → 备份 REAL_TOPIC/REAL_QUEUE_ID (putProperty) → topic=SCHEDULE_TOPIC_XXXX + queueId=level-1 — 在发送钩子链, 非 SendMessageProcessor 主链 | 发现 1 (精确化: 二次 REVIEW "SendMessageProcessor 交叉"归位 HookUtils) |
| T2 | 消费重试的延迟? | **AbstractSendMessageProcessor L209-212**: 重试消息 delayLevel = **3 + reconsumeTimes** (重试延迟递增!) — 延迟机制另一消费方 | 发现 2 (补充锚点) |
| T3 | REAL_* 属性残留? | messageTimeUp 只清 3 属性 (L371-374); REAL_TOPIC/REAL_QUEUE_ID **残留** — 无害 (消费者忽略) 但可见 | 发现 3 (标注) |
| T4 | batch 断言? | `assert cqUnit.getBatchNum() == 1` (L446) — 延迟队列不支持批量消息 | 发现 4 (补充锚点) |
| T5 | 时钟回拨推导 | correctDeliverTimestamp: now 回拨 → deliverTimestamp > now+levelDelay 恒真 → **立即投递 (提前)** — 防永久等待的代价 | 发现 5 (精确化) |
| T6 | 重复投递窗口 | 投递成功 (putMessage OK) → updateOffset 之间崩溃 → 重启 load 旧 offset → 续扫重投 — **at-least-once, 无去重** | 发现 6 (负面空间补充) |
| T7 | 持久化竞态 | updateOffset (投递线程) vs persist (定时线程) — ConcurrentHashMap 安全; 版本计数缓解 | 通过 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 重试延迟递增 | 3+reconsumeTimes: 第 1 次重试 level=4 (30s? 等级 4=30s... 3+1=4 → 30s) — 递增延迟 ✅ | 通过 |
| V2 | 钳制双路径 | HookUtils (发送前) + CommitLog (分发期) 双钳制 — 防御纵深 ✅ | 通过 |
| V3 | 提前投递推导 | 回拨 ΔT: deliverTimestamp(storeTs+d) > now(storeTs-ΔT)+d ⟺ storeTs+d > storeTs-ΔT+d ⟺ 0 > -ΔT 恒真 → now ✅ | 通过 |
| V4 | 重复投递窗口 | updateOffset 在投递成功后 (L500/565) — 窗口 = 投递到更新之间 (毫秒级) ✅ | 通过 |
| V5 | batch 断言语义 | 批量消息进延迟队列 → 断言失败 (开发期); 生产应被前置拦截 ✅ | 通过 |
| V6 | REAL 残留影响 | 还原后消息属性含 REAL_* — 消费侧仅当二次发送才相关 (无路径) ✅ | 通过 |
| V7 | 重试与延迟等级耦合 | 重试走 SCHEDULE 队列 (delayLevel=3+n) — 与客户端延迟同机制 ✅ | 通过 |
| V8 | 版本计数缓解 | updateOffset 每 step 递增 version — DLedger 状态机同步面 (RM-12) ✅ | 通过 |

## 新发现问题 (6 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **精确化** | 写前改道归位 **HookUtils.transformDelayLevelMessage** (L226-236) — 含钳制+REAL 备份+topic 改写三步; 二次 REVIEW "SendMessageProcessor 交叉"不精确 | 大纲 §2 重写 |
| 11 | **补充锚点** | **消费重试延迟**: 重试消息 delayLevel = 3+reconsumeTimes (AbstractSendMessageProcessor L209-212) — 延迟机制的双消费方 | 大纲 §2 补注 |
| 12 | 补充锚点 | batch 断言 (L446): 延迟队列不支持批量 | 大纲 §3 补注 |
| 13 | 精确化 | 时钟回拨 → 提前投递 (推导 V3) | 大纲 §3 补注 |
| 14 | 机制缺口 | 投递 at-least-once (updateOffset 窗口重复) — 无去重 | 大纲负面空间补 |
| 15 | 标注 | REAL_* 属性残留 (无害) | 大纲 §4 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 等级表: 配置/解析/钳制 — 可写 ✅
- §2 写路径: HookUtils 三步 (修复后)/双钳制/重试延迟 (修复后) — 可写 ✅
- §3 投递: 线程模型/节奏/correctDeliverTimestamp+时钟回拨 (修复后)/batch 断言 (修复后) — 可写 ✅
- §4 还原: 三清/REAL 残留标注 (修复后) — 可写 ✅
- §5 投递+持久化: sync/async/JSON/版本 — 可写 ✅
- §6 配置测试 — 可写 ✅
- 负面空间 7 条 (修复后含 at-least-once) — 完整 ✅

## 四次 REVIEW 汇总

七存疑点全实证 (T1-T7); 推理验证 8 项全过 (V1-V8); **新发现 6 处全部修复** — #10 写前改道归位最有价值 (HookUtils 三步); #14 重复投递窗口为机制缺口 (负面空间补)。大纲经修复后反写测试全过。
