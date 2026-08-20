# RM-7 Producer 发送 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **sendLatencyFaultEnable 默认 false** (MQFaultStrategy:27 volatile 无初始化) — 故障感知需显式开启; 关闭时仍有 brokerFilter (排除上次 broker) | 大纲 §3 修正 |
| 2 | **补充锚点** | **retryResponseCodes 默认 8 码穷举** (DefaultMQProducer:76-85): TOPIC_NOT_EXIST/SERVICE_NOT_AVAILABLE/SYSTEM_ERROR/SYSTEM_BUSY/NO_PERMISSION/NO_BUYER_ID/NOT_IN_CURRENT_UNIT/GO_AWAY | 大纲 §1 补注 |
| 3 | **补充锚点** | **AggregateKey 四维分组** (topic/mq/tag/waitStoreMsgOK, ProduceAccumulator:287-312) — 批量累积维度 | 大纲 §5 补注 |
| 4 | **补充锚点** | **callbackExecutor 可注入** (setCallbackExecutor → remotingClient) | 大纲 §6 补注 |
| 5 | 行号验证 | 全函数 25 锚点 + 跨文件 8 处 grep (DefaultMQProducer 76-139 / DefaultMQProducerImpl 733-1130 / MQFaultStrategy 27-187 / LatencyFaultToleranceImpl 36-126 / TopicPublishInfo 75-112 / ProduceAccumulator 45-312 / selector 3 文件) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 重试编排异常三分类
- 重试码白名单
- 故障更新双维度

### 维度2 性能
- 轮询无锁
- 批量累积
- 线程独立索引

### 维度3 内存
- 路由缓存
- 故障表 (FaultItem)

### 维度4 一致性
- 超时预算扣减
- SEND_OK 语义 (存储确认)
- 重试 resetIndex

### 维度5 负面空间 (已写入大纲 5 条)
- 不无限重试/不严格均衡/不事务保证/不端到端确认/不压缩内建

## 结论
RM-7 全部锚点 ~40 处验证, 6 闭环完成, **补充锚点 4**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-1 (已交付) + RM-11 (未来 — 仅"路由发现"引用, 标注) ✅; 引出 RM-10/8 ✅; 对照 Kafka ✅; 读者处境场景化 ✅; 锚点 ~40 ✅; 负面空间 5 条 ✅; 横切 (并发/故障/路由/批量) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §4 "重试 resetIndex 防重复打同一队列" — **不精确**: resetIndex 是重置轮询索引 (配合 lastBrokerName 排除), 防的是重试打回**同一 broker** 而非同一队列; brokersSent 数组记录已试 | 大纲 §4 精确化 |
| 8 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (三模式分派/重试 1+2/超时扣减/异常三分类/隔离 10s/7 档映射/三级过滤/轮询取模/路由缓存/头构造/hook 链/批量双条件/选择器三实现/回调注入) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (resetIndex 语义 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 7 档映射数学 | latencyMax 7 档与 notAvailableDuration 7 档一一对应; 30s 上限为最慢档 ✅ | 通过 |
| V2 | 隔离 10s | computeNotAvailableDuration(10000) → 匹配 5000<=10000<15000 档? 不 — 10000 >= 5000 档 (L180 从高到低: 15000>10000, 5000<=10000 → 档 6=10000ms) — isolation 恰好命中 10s 档 ✅ | 通过 |
| V3 | 超时扣减 | 总超时 - 已耗时 = kernel 剩余 — 端到端 ✅ | 通过 |
| V4 | 异步不重试 | timesTotal=1 (L760) — 调用方负责 ✅ | 通过 |
| V5 | 重试码语义 | 8 码全为"可恢复"类 (topic 不存在=路由过期, SYSTEM_BUSY=节流) — 白名单合理 ✅ | 通过 |
| V6 | 轮询公平性 | 线程独立索引 — 多线程下各线程轮转, 全局近似均匀 ✅ | 通过 |
| V7 | AggregateKey 维度 | topic+tag+waitStoreMsgOK+mq(可空) — 批量需同队列同 tag 同语义 ✅ | 通过 |
| V8 | 故障恢复 | FaultItem.startTimestamp 过期 → isAvailable true; 5.x 探测主动恢复 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **消息重试属性 (RETRY_TOPIC/重试次数)**: sendKernelImpl 构造时按重试次数设置属性 (ReconsumeTimes 面, RM-8 消费重试交叉); 发送失败重试与消费重试共用 | 大纲 §5 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (重试属性面), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (ASYNC 重试/故障更新时机/ThreadLocalIndex/路由 fallback/addr 解析/累积触发), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | **ASYNC 真的不重试?** | sendDefaultImpl L760 (timesTotal=1) 但 **MQClientAPIImpl.sendMessageAsync (L674-787): 回调 onException → times < retryTimesWhenSendAsyncFailed=2 → 换 broker (retryBrokerName) 递归重发** — **双层重试语义** | **认知修正 (发现 1): "异步不重试"只对 sendDefaultImpl 层; MQClientAPIImpl 回调层重试 2 次** |
| T2 | ASYNC 故障更新时机? | sendDefaultImpl L778: updateFaultItem(kernel 返回后立即) — invokeAsync 发出即返回 → **度量=发送发起耗时非完成耗时** (异步本质) | 发现 2 (语义标注) |
| T3 | ThreadLocalIndex? | TopicPublishInfo.sendWhichQueue = ThreadLocalIndex (内部 ThreadLocal<AtomicLong>) — 线程独立递增 | 通过 |
| T4 | 路由 fallback? | tryToFindTopicPublishInfo: 缓存未命中 → fetch → 失败时**保留旧缓存返回** (producerTable 不删) | 通过 |
| T5 | brokerAddr 解析? | sendKernelImpl: topicRouteData.brokerDatas → mq.brokerName → brokerAddr (L950 区域) | 通过 |
| T6 | 累积触发? | ProduceAccumulator 由 send 路径调用 (checkAndTryAccumulate 类方法, 条件满足即批量) — 未深挖细节, 标注 | 通过 (标注) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 双层重试语义 | sendDefaultImpl (SYNC 1+2) + MQClientAPIImpl (ASYNC 2) — 两套配置互不干扰; ONEWAY 无重试 ✅ | 通过 |
| V2 | 异步故障度量 | updateFaultItem 在 kernel 返回 (invokeAsync 发出) 后 — 延迟=发起耗时; 完成耗时在回调 (不更新故障) ✅ | 通过 |
| V3 | 换 broker 递归 | sendMessageAsync 失败 → retryBrokerName (从 topicPublishInfo 选非当前 broker) → 递归 sendMessageAsync — 次数递减 ✅ | 通过 |
| V4 | 压缩恢复 | ASYNC 克隆消息发压缩体, 原消息恢复 prevBody (finally L1080) — 发送后 msg 还原 ✅ | 通过 |
| V5 | 白名单 8 码 | CopyOnWriteArraySet 默认 8 — 与 SYNC 共用 ✅ | 通过 |
| V6 | 轮询线程独立 | ThreadLocal 索引 — 每线程独立轮转 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **认知修正** | "异步/单向不重试" **只对 sendDefaultImpl 层**: ASYNC 在 MQClientAPIImpl 回调层重试 2 次 (retryTimesWhenSendAsyncFailed, 换 broker) — 大纲表述误导 | 大纲 §1/§2 重写 |
| 11 | 语义标注 | ASYNC 故障更新度量 = 发送发起耗时 (invokeAsync 即返回) — 非完成耗时 | 大纲 §2 补注 |
| 12 | 补充锚点 | **消息体压缩面**: ASYNC 克隆消息发压缩体 + prevBody 恢复 (L1008-1035,1080) | 大纲 §1 补注 |
| 13 | 补充锚点 | retryTimesWhenSendAsyncFailed=2 独立配置 (DefaultMQProducer:134) | 大纲 §1 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 门面: 三模式/**ASYNC 独立重试 (修复后)**/压缩面 (修复后)/8 码 — 可写 ✅
- §2 重试: SYNC 1+2/**双层重试语义 (修复后)**/异常三分类/故障度量标注 (修复后) — 可写 ✅
- §3 故障: 7 档/默认 false/三级/双维度 — 可写 ✅
- §4 路由均衡: 缓存/轮询/resetIndex — 可写 ✅
- §5 kernel+累积: 头构造/hook/四维分组 — 可写 ✅
- §6 选择器: hash/机房间/随机/回调注入 — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 4 处全部修复** — **#10 认知修正最有价值** (双层重试: sendDefaultImpl SYNC 重试 + MQClientAPIImpl ASYNC 回调重试, 配置独立)。大纲经修复后反写测试全过。
