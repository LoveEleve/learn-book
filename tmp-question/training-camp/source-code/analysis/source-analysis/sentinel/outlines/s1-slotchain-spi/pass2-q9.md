# Pass 2 闭环笔记 Q9: addFirst 消费方 — HotParamSlotChainBuilder 是死代码?

## 初始假设
- extension 有独立链构建器(如 HotParamSlotChainBuilder)会用 addFirst 插入 ParamFlowSlot。
- 实际: **HotParamSlotChainBuilder 是 @Deprecated 空类**,addFirst 无任何生产消费方。

## 验证过程
- 读 `HotParamSlotChainBuilder.java`: `@Deprecated public class HotParamSlotChainBuilder extends DefaultSlotChainBuilder {}` — 纯占位,无方法覆写。
- 全仓库 addFirst 检索: 仅 ProcessorSlotChain.java:30 抽象定义,无实现调用 — **addFirst 是历史 API,生产代码零消费**。
- ParamFlowSlot 的实际接入: 不经自定义 builder — 直接经 `sentinel-parameter-flow-control` 模块的 SPI 文件 `META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot` 注册,由 DefaultSlotChainBuilder.loadInstanceListSorted 按 @Spi(order=-3000) 自动插入 System 与 Flow 之间。
- 自定义 builder 官方范式: `DemoSlotChainBuilder.java`(demo 模块)— 实现 SlotChainBuilder,可从 loadInstanceListSorted 列表 removeIf 过滤槽(示例: 移除 DegradeSlot),再用 addLast 逐个挂链。

## 代码类型
- Glue(扩展点范式)/ Dead code(HotParamSlotChainBuilder)

## 跨域关联
- S-1 → S-6 热点: ParamFlowSlot 接入路径 = SPI 文件(非 builder 覆写)
- S-2 入口: 自定义 builder 被 SlotChainProvider.loadFirstInstanceOrDefault 优先选中

## 结论
addFirst 是死 API;HotParamSlotChainBuilder 是 @Deprecated 空类。ParamFlowSlot 经 SPI 文件 + @Spi order 自动入链 — 这正是"SPI 优先于继承覆写"的设计取向(HotParamSlotChainBuilder.java + DemoSlotChainBuilder.java + 参数流控模块 SPI 文件)。