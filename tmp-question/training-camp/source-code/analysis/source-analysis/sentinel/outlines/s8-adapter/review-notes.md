# S-8 适配器与扩展域 — 审查记录

## Pass 1 / Pass 2

- 确认适配器主线共通骨架：resource/origin 提取 → SphU/AsyncEntry → block/error bridge → 正确生命周期 exit。
- 确认 WebMVC 用 request attribute + reference count，WebFlux 用 Transformer/Operator/Subscriber。
- 确认 AspectJ 的处理顺序：BlockException → blockHandler/fallback；业务异常 → ignore/trace/fallback。
- 确认 DataSource 只生产 `SentinelProperty`，不直接操作 RuleManager。
- 确认 JMX/Prometheus 是 core 指标的消费/暴露层，不参与规则判定。
- 确认 Reactor 适配器不是 Filter 变体，而是 Publisher/operator/subscriber 生命周期桥。

## 深审修正

1. 独立复核发现：`AbstractDataSource.loadConfig()` 只负责 `readSource → Converter`，实际 `property.updateValue` 由具体 Nacos/Apollo/Consul 等 DataSource 调用；已修正中篇正文。
2. 独立复核发现：Reactor entry 在 subscriber 订阅阶段创建，exit 由 unary onNext 或 complete/error/cancel 驱动，并由 AtomicBoolean 防重复；已补充上篇正文。
3. WebMVC/WebFlux/Reactor 入口与 exit 锚点逐一核对。
2. AspectJ fallback/blockHandler 行为对照 `AbstractSentinelAspectSupport`，确认 ignore 优先于 trace，blockHandler 优先于 fallback。
3. DataSource 生命周期锚点对照 `AbstractDataSource`，确认 converter/property 解耦。
4. JMX/Prometheus 行号核对：JMX start/export/shutdown 在 60-70，Prometheus init 在 34-46。
5. 明确 Prometheus 初始化失败只记录 warning，并不传播到 core entry 主链。

## 三篇正文

- `01-web-reactive.md`：WebMVC/WebFlux/Reactor 入口与生命周期
- `02-aspect-datasource.md`：注解切面、异常/fallback、DataSource 推送
- `03-exporter-boundary.md`：JMX/Prometheus、Quarkus、适配器边界

## 遗留

1. RPC/HTTP 各适配器没有逐个展开，仅抽取共通桥接骨架。
2. Quarkus deployment processor/runtime recorder 未深入到具体 BuildItem/Recorder 方法。
3. WebFlux Reactor subscriber 的 onError/cancel 具体 exit 实现未逐行展开。
4. 各 DataSource 的具体刷新线程/监听机制未逐一比较。
