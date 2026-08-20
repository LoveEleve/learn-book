# Sentinel 源码分析 — 全域最终审查记录

> 日期: 2026-08-17
> 范围: S-1 至 S-10 全部分析域
> 方法: 产物完整性 → 跨域一致性 → 关键数字 → 关键锚点 → 绝对化措辞 → 遗留项诚实性

## 一、产物完整性

| 域 | 文件数 | 核心产物 |
|---|---:|---|
| S-1 | 19 | Pass1/Pass2/36问/大纲/时空/3正文/review |
| S-2 | 17 | Pass1/Pass2/36问/大纲/时空/3正文/review |
| S-3 | 17 | Pass1/Pass2/23问/大纲/时空/3正文/review |
| S-4 | 14 | Pass1/Pass2/22问/大纲/3正文/review |
| S-5 | 17 | Pass1/Pass2/20问/大纲/时空/3正文/review |
| S-6 | 15 | Pass1/Pass2/20问/大纲/3正文/review |
| S-7 | 12 | Pass1/Pass2/21问/大纲/3正文/review |
| S-8 | 12 | Pass1/Pass2/21问/大纲/3正文/review |
| S-9 | 12 | Pass1/Pass2/21问/大纲/3正文/review |
| S-10 | 11 | Pass1/Pass2/22问/大纲/3正文/review |

S-3 的 `completeness-questions.md` 在本轮最终审查中补齐。核心四件套（pass1、completeness、outline、review）现已覆盖全部 10 域。

## 二、拓扑一致性

执行拓扑：

```text
S-1 → S-2 → S-5 → S-3 → S-4 → S-6 → S-7 → S-9 → S-8 → S-10
```

关键移交一致：

- S-1 → S-2：ProcessorSlot 链进入 entry/context 生命周期
- S-2 → S-5：Entry exit / error / EntryType 进入统计节点与 StatisticSlot
- S-5 → S-3：StatisticNode/LeapArray 被 Flow controller 消费
- S-3 → S-4：Block/统计/规则路径与 circuit breaker 结合
- S-3 → S-6：普通 Flow 与 ParamFlow 的槽序、controller、cluster token 边界
- S-7 → S-8：RuleManager/property 被 DataSource 与适配器接入
- S-9 → S-10：cluster/transport/control plane 分层

## 三、关键数字复核

- S-1：NodeSelector/ClusterBuilder 原型槽；MAX_SLOT_CHAIN_SIZE=6000；ParamFlowSlot order=-3000
- S-2：20 个公开入口 = 12 entry + 6 asyncEntry + 2 entryWithPriority；MAX_CONTEXT_NAME_SIZE=2000
- S-3：4 个 TrafficShapingController；LeapArray occupy future bucket
- S-4：3 状态 CLOSED/OPEN/HALF_OPEN；3 个 Degrade grade
- S-5：DefaultNode/ClusterNode/EntranceNode 三视角；second/minute 双窗口
- S-6：ParamFlowSlot order=-3000；参数值 map LRU 限容
- S-7：RuleManager 简单/正则规则双路径；System 采样 1 秒
- S-8：Reactor operator/subscriber，不是同步 Filter 生命周期
- S-9：CLIENT=0/SERVER=1/NOT_STARTED=-1；TokenResult 核心 OK/SHOULD_WAIT/BLOCKED
- S-10：CommandHandler 17 个（以计划复核结果为准）；三种 command/heartbeat transport 实现

## 四、本轮跨域修正

1. S-2 旧“14 个 entry 重载”统一修正为 20 个公开入口。
2. S-2 AsyncEntry 旧 trueExit 锚点 `84-88` 修正为 `97-98`。
3. S-3 补齐缺失的 `completeness-questions.md`。
4. S-8 修正 `AbstractDataSource.loadConfig()`：它只读取/转换，具体 DataSource 才调用 property.updateValue。
5. S-8 补充 Reactor subscriber 的真实生命周期：订阅创建 entry，unary onNext 或 complete/error/cancel exit，AtomicBoolean 防重复。
6. S-8 补 Dubbo/OkHttp/Quarkus 源码锚点。
7. S-10 补 Dashboard machine/cluster/metric controller 锚点。
8. 统一 27 个 Pass2 文件的标题：`## 假设` → `## 初始假设`，明确区分猜想与结论。

## 五、已知边界与遗留

- S-1：EagleEye 批处理内部、SpiLoaderTest 细节未逐一展开。
- S-2：asyncContext 默认 context 边角语义、锁错位历史 diff、callback 细节未完全展开。
- S-3：WarmUp 数学推导、cluster transport、极端时钟回拨未深入。
- S-4：默认 `"*"` breaker 复用边界、HALF_OPEN 兜底 CAS 未检查是源码现状，未修改。
- S-5：LeapArray 极限算法、metrics 分钟拉取、EagleEye 独立附篇未展开。
- S-6：ParamFlowRuleUtil 解析细节、简化令牌桶数学、cluster 协议未展开。
- S-7：SystemStatusListener 具体系统采样、BBR 理论推导未展开。
- S-8：各 RPC 适配器、Quarkus 全部 BuildItem、各 DataSource 刷新机制未逐一展开。
- S-9：Netty 重连、ConcurrentClusterFlowChecker 细节、Envoy RLS 协议未展开。
- S-10：17 个 handler 逐一业务细节、SentinelApiClient 协议、Dashboard cluster controller 细节未展开。

## 六、最终结论

- 10 个域均已完成方法论要求的：Pass1、Pass2、全视角问题、大纲、正文、审查记录。
- 本轮未发现新的跨域高风险事实矛盾。
- 遗留项均已显式记录，没有把未验证内容包装成源码事实。
