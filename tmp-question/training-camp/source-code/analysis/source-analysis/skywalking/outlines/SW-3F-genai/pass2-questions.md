# SW-3F GenAI Analyzer — Pass 2 问题收敛

> 模块: `oap-server/analyzer/gen-ai-analyzer`
> 日期: 2026-08-18

## Q1: Zipkin 分支是否与 SW 分支保持同等 provider fallback 语义
已确认基本一致，但 Zipkin 多一个 legacy fallback：
- 先读 `gen_ai.provider.name`
- 为空时再读 `gen_ai.system`
- 仍为空时才根据 model prefix 推断 provider

结论：Zipkin 比 SW 多兼容一层 legacy tag，不是分支不一致。

## Q2: `transferToSources(...)` 的 4 个 source 是否完整、顺序是否稳定
已测试确认固定为 4 个：
1. `ServiceMeta`
2. `ServiceInstance`
3. `GenAIProviderAccess`
4. `GenAIModelAccess`

当前实现使用固定 `add(...)` 顺序，因此顺序稳定。

## Q3: cost double -> long rounding 是否是设计问题
`GenAIMetrics.totalEstimatedCost` 用 `double` 保留中间精度，但在 `toProviderAccess(...)` 与 `toModelAccess(...)` 里通过 `Math.round(...)` 落成 `long`。

结论：
- 这是当前设计，不是偶发误差
- 语义是“以微货币单位近似取整后入 source”
- 若后续 query 要展示更细精度，应该从模型设计层调整，而不是在 analyzer 内局部修补

## Q4: provider/model 为空、unknown model、legacy `gen_ai.system` 的行为
已核实：
- model 空：直接返回 `null`，整条 GenAI 分析跳过
- provider 空：尽量用 matcher 推断
- unknown model：`modelConfig == null`，cost=0，但 source 仍生成
- Zipkin legacy `gen_ai.system`：可作为 provider fallback

## Q5: `SegmentObject` 参数在 SW 分支是否实际参与计算
没有。`extractMetricsFromSWSpan(SpanObject span, SegmentObject segment)` 当前只消费 `span` tag 与时间，不读取 `segment`。

结论：
- `segment` 只是接口对齐/预留参数
- 当前域的计算输入本质上只依赖 span 自身

## Q6: NamingControl 是否会影响虚拟 GenAI service/model 名
会。`transferToSources(...)` 中：
- provider name 走 `formatServiceName(...)`
- model name 走 `formatInstanceName(...)`

结论：source 中看到的 provider/model 名不一定等于原始 tag 字符串，长度可能被裁剪。
