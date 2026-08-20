# Pass 2 闭环笔记 Q7: HotParamSlotChainBuilder 的废弃原因

## 验证过程

- `HotParamSlotChainBuilder` 标 `@Deprecated`，注释明确写着 `since 1.7.2, we can use @Spi(order = -3000) to adjust the order of ParamFlowSlot` (`HotParamSlotChainBuilder.java:30-42`)。
- 它继承 `DefaultSlotChainBuilder`，没有任何额外代码。
- 历史背景：1.7.2 之前，槽链构建器是显式指定槽顺序的。热点参数限流需要一个独立的 builder 把 `ParamFlowSlot` 插到正确位置。
- 1.7.2 起，`ParamFlowSlot` 直接通过 `@Spi(order = -3000)` 声明顺序，`DefaultSlotChainBuilder` 会按 SPI order 排序加载，不再需要专门的 builder。

## 结论

`HotParamSlotChainBuilder` 是槽 SPI 化之前的兼容遗留物。现在是空类，只为老版本配置仍能编译/加载而保留。它佐证了 S-1 中“槽顺序权威是 @Spi order”的结论。