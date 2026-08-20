# Pass 2 闭环笔记 Q1: @Spi order 排序 — 相同 order 时谁先执行?

## 初始假设
- 排序比较器只有 order 一个维度,order 相同则顺序不确定或报错。
- 实际上:比较器 `Integer.compare(order1, order2)` 无二次 key,但 `Collections.sort` 是**稳定排序** — order 相同时保持 SPI 配置文件中的声明顺序(SpiLoader.java:414-426)。

## 验证过程
- 读 `SpiLoader.java:415-425`: compare 只取两类的 @Spi.order(),无 order 注解默认 0;返回 Integer.compare。
- 读 `Constants.java:75-86`: 9 个内置槽 order 全部唯一 — -10000(NodeSelector) / -9000(ClusterBuilder) / -8000(Log) / -7000(Statistic) / -6000(Authority) / -5000(System) / **-2000(Flow)** / -1500(DefaultCircuitBreaker) / **-1000(Degrade)**。
- 读 `ParamFlowSlot.java:34`: extension 槽 @Spi(order = -3000),插在 System(-5000) 与 Flow(-2000) 之间 — 与 PLAN 断言一致。
- 链序完整实证(10 槽): NodeSelector → ClusterBuilder → Log → Statistic → Authority → System → **ParamFlow(扩展)** → Flow → DefaultCircuitBreaker → Degrade。

## 代码类型
- Implementation(排序逻辑)/ Algorithmic(稳定排序的次序语义)

## 跨域关联
- S-3 流控 / S-4 熔断 / S-6 热点:各槽执行时机的全局位置由本结论决定
- S-2 入口: CtSph 链构建消费本排序结果

## 结论
order 升序决定执行序,小值先执行。9 内置槽 order 全部唯一,唯一"插入点"是 extension 的 ParamFlowSlot(-3000);若未来出现 order 相同,稳定排序保证 = SPI 文件声明顺序(源码 SpiLoader.java:414-426 + Constants.java:75-86)。测试实证链序: DefaultSlotChainBuilderTest.java:41-78(9 槽断言 + 末槽 next==null)。