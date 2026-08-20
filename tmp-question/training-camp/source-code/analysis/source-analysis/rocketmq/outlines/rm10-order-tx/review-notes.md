# RM-10 顺序+事务消息 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | **MachineRoom 选择器是桩**: SelectMessageQueueByMachineRoom.select() 直接 return null (L29-31) — consumeridcs 字段无消费方; 执行计划"机房间选择器"表述需修正 | 大纲 §1 + 负面空间补注 |
| 2 | **认知修正** | **顺序消息起点无特殊分支 (5.3.1)**: computePullFromWhereWithException (RebalancePushImpl:166-248) 全 switch 无 isOrder 判断 — "顺序从 0 读"是旧版说法; 顺序 topic 与普通同起点逻辑 (默认尾部) | 大纲 §2 补注 |
| 3 | **机制缺口** | **免疫时间双实现不一致**: ServiceImpl.getImmunityTime (L356-366) 尊重用户自定义 (不钳制, 可 <6s) vs TransactionalMessageUtil.getImmunityTime (L78-92) **钳制 ≥ transactionTimeout** — 同一 CHECK_IMMUNITY_TIME_IN_SECONDS 在回查侧与 END 拒绝侧行为不同 | 大纲 §4/§5 补注 |
| 4 | **语义标注** | **escape 退避 2^cnt 是 XOR 非幂**: `100L * (2 ^ escapeFailCnt)` (L253) — Java 异或非指数; 序列 300,0,100,600,700,400,500,1000,1100,800ms — 非指数退避 | 大纲 §5 补注 (代码怪癖) |
| 5 | 行号验证 | 全函数 40 锚点 + 跨文件 12 处 grep (selector 3 / sendSelectImpl 1314-1353 / MQClientAPIImpl 778-782 / SendMessageProcessor 304-318 / parseHalf 219-233 / EndTransaction 64-68,199-273 / check 161-354 / Listener 51-70 / OpBatch 46-64 / EscapeBridge 95-116 / RebalanceImpl 521) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 选择器确定性 (hash 取模)
- 半消息改道 (REAL_* 备份)
- 三校验 + 免疫拒绝
- 回查三条件

### 维度2 性能
- SYNC 顺序单次发送 (不重试)
- OP 批量 (3s/4096B)
- 60s 单队列处理上限

### 维度3 内存
- deleteContext (per queueId 队列)
- checkExecutor 2000 上限
- opQueueMap 缓存

### 维度4 一致性
- 半+OP 双流对账
- TRAN_PREPARED_QUEUE_OFFSET 重写闭环
- at-least-once (无去重, 业务幂等)

### 维度5 负面空间 (已写入大纲 7 条)
- 不全局序/不选择器重试/不 2PC/不本地补偿/不回查去重/不幂等存储/桩选择器

## 结论
RM-10 全部锚点 ~40 处验证, 6 闭环完成, **认知修正 2 + 机制缺口 1 + 语义标注 1**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-7/8/9/3/5 (已交付) ✅; 引出 RM-12 ✅; 对照 Kafka (幂等/事务) ✅; 读者处境场景化 ✅; 锚点 ~40 ✅; 负面空间 7 条 ✅; 横切 (并发/一致性/状态机/模式) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §5 "checkTimes ≥ 15 → 丢弃" 表述不精确 — needDiscard (L108-121) 在**免疫检查之前**执行, checkTimes **免疫期也递增** (每 30s 循环遭遇一次); 且 15 判定是"下次遭遇" (checkTime=15 时已 >=15 → 本次即弃, 实际遭遇 16 次内弃置) | 大纲 §5 精确化 |
| 8 | 通过项 | 其余 ~40 句机制描述逐句对源码一致 ✅ (TRAN_MSG 判定/REAL_* 备份/三校验/还原重投/OP tag=d/免疫期 max/三条件/60s 上限/1 线程 checkExecutor/SPI/批量参数) | 记录 |

## 二次 REVIEW 汇总
共 **2 处修复** (免疫时间双实现 #3 已入深审; 精确化 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | hash 取模无溢出 | `arg.hashCode() % size` 结果 |result| < size ≤ Integer.MAX_VALUE → Math.abs 安全, 无 MIN_VALUE 特例 ✅ | 通过 |
| V2 | 免疫期 vs 弃置交互 | 默认 6s 免疫 + 30s 周期 → 免疫在首次检查前已过 ✅; 自定义 600s 免疫 → checkTimes 免疫期递增 → **8 分钟 (16 周期) 未查即弃** ⚠ 边缘 | 通过 (标注) |
| V3 | TRAN_PREPARED_QUEUE_OFFSET 闭环 | 免疫重写 (renewImmunityHalfMessageInner L259-268) 携带原 queueOffset → 下轮 removeMap 按原 offset 命中 → 决断不丢 ✅ | 通过 |
| V4 | 三校验幂等屏障 | group/queueOffset/commitLogOffset 逐项比对 — 防伪造; 但半消息删除前重复 END → 重复投递 (无端到端幂等, 业务 uniqKey 兜底) ✅ | 通过 (标注) |
| V5 | calculateOpOffset 数学 | doneOffset 排序后从 oldOffset 起连续递增推进 — 不跳洞 ✅ | 通过 |
| V6 | 回查终止条件 | 60s 上限 / getMessageNullCount>1 / NO_NEW_MSG 即退 / removeMap 命中推进 — 有限循环 ✅ | 通过 |
| V7 | checkTimes 计数数学 | 无属性→1; 递增; >=15 即弃 — 遭遇 15 次后第 16 次弃置 (与"查 15 次"等价) ✅ | 通过 |
| V8 | escape XOR 序列 | 2^cnt 异或: 300,0,100,600,700,400,500,1000,1100,800 — 非指数退避实证 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **推理验证发现** | **长自定义免疫期 + 弃置计数交互**: CHECK_IMMUNITY_TIME_IN_SECONDS > 16×30s=480s 时, 消息在免疫窗口内即被弃置 (从未真实回查) — needDiscard 先于免疫检查执行 (L263 vs L269+) | 大纲 §5 补注 (边缘) |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (免疫-弃置交互), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查五个存疑点 (顺序 SYNC 重试/ASYNC 重试 broker/免疫双实现/FLUSH 状态回滚/60s 上限), 并做反写测试。

## 追查过程 (五个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 顺序 SYNC 是否重试? | sendSelectImpl (L1314-1353): 选队列 → **单次 sendKernelImpl 无重试循环** (对照 sendDefaultImpl L751 timesTotal) — 失败即抛 | 发现 1 (补锚) |
| T2 | 顺序 ASYNC 重试换 broker? | sendKernelImpl ASYNC 传 topicPublishInfo=null (L1345) → onExceptionImpl (MQClientAPIImpl:778-782) `topicPublishInfo==null → retryBrokerName=brokerName` — **同 broker 同队列** | 发现 2 (补锚) |
| T3 | 免疫时间双实现? | ServiceImpl:356-366 不钳制 vs Util:78-92 钳制 ≥6s — **同一属性两路径不一致** 实证 | 发现 3 (机制缺口) |
| T4 | FLUSH_* 强制回滚? | sendMessageInTransaction L1475-1479: FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE → ROLLBACK_MESSAGE — **消息可能已落盘但回滚** (半消息弃置, 消费者不可见, 语义安全) | 发现 4 (语义标注) |
| T5 | 60s 上限语义? | MAX_PROCESS_TIME_LIMIT=60000 (L61) + 循环头检查 (L202-205) — 单队列单轮最长 60s, 防 check 线程饿死 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 顺序保序组合 | 同键同队列 (hash) + 消费单消费者串行 (锁) + 发送不换队列 (SYNC 不重试/ASYNC 同 broker) — 三端闭合 ✅ | 通过 |
| V2 | 半消息可见性 | HALF topic 系统级禁客户端订阅消费 (TopicValidator:62-64) + 真实 topic 藏在属性 — 消费者不可见 ✅ | 通过 |
| V3 | 对账不丢 | removeMap 按 halfOffset 精确匹配 + TRAN_PREPARED_QUEUE_OFFSET 重写闭环 — 决断不重复处理 ✅ | 通过 |
| V4 | 免疫-弃置交互 | 480s 内弃置 (16 周期) — 标注边缘 ✅ | 通过 |
| V5 | END 丢失兜底 | oneway 失败仅 log (L1484-1488) → 半消息滞留 → 30s 回查 → 客户端应答 (fromTransactionCheck=true 走通) ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 语义标注 | **FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE → 强制 ROLLBACK** (L1475-1479): 此时半消息可能已落盘; 回滚=弃置半消息, 消费者不可见 — 语义安全但"已写即丢" | 大纲 §4 补注 |
| 11 | 语义标注 | **END oneway 双超时**: 主路径 sendMsgTimeout (L1546-1547) vs 回查应答固定 3000 (L440-441) — 回查超时更短防线程堆积 | 大纲 §4/§5 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 顺序发送 (选择器/hash 数学/单次发送/ASYNC 同 broker) — 可写 ✅
- §2 顺序消费协同 (锁/起点无特殊分支) — 可写 ✅
- §3 半消息写路径 (TRAN_MSG/REAL_* 备份/1 队列) — 可写 ✅
- §4 本地事务+END (三校验/还原重投/OP 消息/604) — 可写 ✅
- §5 回查 (30s/免疫/三条件/15 次/60s/从库代主) — 可写 ✅
- §6 5.x 新面 (OP 批量/指标/SPI) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑点全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (FLUSH 强制回滚/END 双超时)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-14, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查事务对账闭环的六个存疑点 (重复 commit 幂等/重写链对账/END-回查竞态/双端 executor 策略/客户端应答丢弃/半队列膨胀)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 重复 END_COMMIT 是否幂等? | EndTransactionProcessor:130-164 commit 路径: commitMessage→三校验→sendFinalMessage→deletePrepareMessage — **全程无 op 查询** — 三校验只防伪造/串消息, **不能幂等化同 offset 重复 commit** | 发现 12 (表述修正) |
| T2 | 重写链是否放大重复? | putBackHalfMsgQueue (L135-159) **mutate msgExt** (queueOffset/commitLogOffset/msgId 全改新值) → resolveHalfMsg 构造 header 用**新 offset** → 回查应答 op 标记新 copy → 下轮 removeMap 命中跳过 — **单轮单条闭环, 不放大** ✅ | 通过 |
| T3 | END 与回查竞态? | 主路径 END (旧 offset) 与回查应答 (新 copy offset) 并发 → 两次 commitMessage 均过三校验 (旧记录/新 copy 各自一致) → **同业务消息双投递** — at-least-once 实证 | 发现 13 (语义标注) |
| T4 | 双端 executor 策略? | broker resolveHalfMsg: executor 2-5 线程/2000 队列/**CallerRunsPolicy 背压** (L99-104) vs 客户端 checkExecutor: 默认 ThreadPoolExecutor (L211-217, 默认 AbortPolicy) → 满则 RejectedExecutionException → **应答丢弃, 下轮再查** — 双端策略不对称 | 发现 14 (补锚) |
| T5 | 客户端应答丢弃面? | ClientRemotingProcessor:100-131: group 为 null / decode 失败 / selectProducer 无 → 静默 warn → 不回应答 → 半消息滞留 → broker 下轮再查 (安全) | 通过 (验证) |
| T6 | 半队列膨胀? | 重写链每条产生新 CQ 记录 + 弃置前最多 15 轮滞留 → HALF topic (1 队列) 容量被长滞留事务多副本消耗 — 弃置后才回收 | 发现 15 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 重写链闭环数学 | 轮1: 原始记录→重写 copy1 (新 offset) + 回查应答带新值 → op 标记 copy1 新 offset → 轮2: removeMap[新 offset] 命中跳过 ✅ 单条单轮 | 通过 |
| V2 | 竞态双 commit | END(旧) + 回查应答(新) → 各自三校验通过 (旧记录 vs 新 copy 字段自洽) → 双投递; 窗口 = 客户端单次 END 之外的回查应答 ✅ | 通过 |
| V3 | checkTimes 终止性 | 每次 check 遭遇 +1 (属性随重写复制) → 16 轮弃置 → 回查最多 15 次 → 重复投递有界 ✅ | 通过 |
| V4 | 弃置-重写一致性 | renewHalfMessageInner 复制全部 properties (L284) → checkTimes 跨重写保留 → 终止性不因重写重置 ✅ | 通过 |
| V5 | bornTimestamp 保留 | renewHalfMessageInner L286 setBornTimestamp(原值) → 重写 copy 免疫期立即失效 (born 未更新) → 重写即查即答 ✅ | 通过 |
| V6 | putBack 失败面 | 失败 → continue 不推进 i (L301-303) → 同 offset 重试直至 60s 上限 — 有限循环 ✅ | 通过 |
| V7 | 双端策略不对称 | broker CallerRuns 背压 (不丢) vs client AbortPolicy 丢弃 (丢后重查) — 方向合理: broker 必达, 客户端尽力 ✅ | 通过 |
| V8 | 三校验语义 | 防伪造 (group 不符)/防串 (offset 不符) — **不防重复** (T1) — 与 outline "幂等化" 表述冲突 → 修正 | 通过 (修正) |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **表述修正** | outline §4 "重复 COMMIT 由三校验+op 对账幂等化" 过度承诺 — EndTransactionProcessor 全程无 op 查询; 三校验防伪造/串, op 对账防回查跳过; **同 offset 重复 commit 不被拦截** | 大纲 §4 修正 |
| 13 | **语义标注** | **END 与回查竞态窗口**: 主路径 END (旧 offset) 与回查应答 (新 copy) 双 commit → 同业务消息重复投递一次 — at-least-once 实证 (防护=客户端单次 END+业务幂等) | 大纲 §4 补注 |
| 14 | **补充锚点** | **双端 executor 策略不对称**: broker resolveHalfMsg CallerRunsPolicy 背压 (2-5/2000) vs 客户端 checkExecutor AbortPolicy (1-1/2000) — 满时应答丢弃, 下轮再查 | 大纲 §5 补注 |
| 15 | **补充锚点** | **半队列膨胀**: 重写链多副本 + 弃置前 15 轮滞留 — 长滞留事务消耗 HALF topic (1 队列) 容量 | 大纲 §5 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 顺序发送 — 可写 ✅
- §2 顺序消费协同 — 可写 ✅
- §3 半消息写路径 — 可写 ✅
- §4 本地事务+END (三校验防串不防重/竞态窗口) — 可写 ✅
- §5 回查 (重写链闭环/双端策略/膨胀) — 可写 ✅
- §6 5.x 新面 — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 4 处全部修复** (三校验不幂等/END-回查竞态/双端 executor 策略/半队列膨胀)。核心认知升级: 回查闭环靠 **putBackHalfMsgQueue 的 msgExt mutate 语义** (应答带新 offset → op 标记新 copy), 重复投递窗口真实存在于 END-回查竞态。大纲经修复后反写测试全过。
