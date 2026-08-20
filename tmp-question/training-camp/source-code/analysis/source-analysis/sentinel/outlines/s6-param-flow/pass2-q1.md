# Pass 2 闭环笔记 Q1: ParamFlowSlot 的 SPI 位置

## 验证过程

- `ParamFlowSlot` 标 `@Spi(order = -3000)`，位于 `sentinel-extension/sentinel-parameter-flow-control`，不是 `sentinel-core` 内置槽 (`ParamFlowSlot.java:39`)。
- 对照 S-1 已实证的槽序：Authority(-6000) → System(-5000) → **ParamFlow(-3000)** → Flow(-2000)。所以热点参数限流插在系统保护之后、普通流控之前。
- `ParamFlowSlot.entry` 先 `checkFlow` 再 `fireEntry`，与 FlowSlot 结构一致。

## 结论

热点参数限流是 order=-3000 的 SPI 扩展槽，天然位于 System 与 Flow 之间；它不是 core 内置，而是扩展模块通过槽 SPI 织入主链。这个位置保证了它比普通 FlowSlot 更早拦截热点参数流量。