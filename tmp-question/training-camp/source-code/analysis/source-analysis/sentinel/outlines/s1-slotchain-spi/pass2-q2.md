# Pass 2 闭环笔记 Q2: loadFirstInstance 与 loadFirstInstanceOrDefault — default 兜底语义

## 初始假设
- loadFirstInstance 返回第一个实现;loadFirstInstanceOrDefault 在无实现时返回默认实现。
- 实际更精确:两者差异在 **defaultClass 的参与方式**,且都基于**未排序**的 classList(文件声明序)。

## 验证过程
- 读 `SpiLoader.java:211-221` (loadFirstInstance): classList.get(0) 直接返回,**不看 defaultClass**;空列表返回 null。
- 读 `SpiLoader.java:228-238` (loadFirstInstanceOrDefault): 遍历 classList,`clazz != defaultClass` 的第一个返回;**全是 default → loadDefaultInstance()**。
- 读 `SpiLoader.java:246-254` (loadDefaultInstance): 返回 @Spi(isDefault=true) 的实现。
- 语义链: **firstOrDefault = 首个非默认实现,若无一非默认则退回默认实现** — 即"用户自定义优先、内置兜底"模式。
- 消费方实证: `SlotChainProvider.java:33-37` 用 `loadFirstInstanceOrDefault()` → 用户可覆写 SlotChainBuilder,无则 DefaultSlotChainBuilder (@Spi(isDefault=true), DefaultSlotChainBuilder.java:25)。

## 代码类型
- Glue(加载语义分发)

## 跨域关联
- S-1 → 全域: "用户自定义优先"是 Sentinel 扩展的通用模式(DataSource/CommandHandler 同理)

## 结论
loadFirstInstance = 文件首个(可与 default 相同);loadFirstInstanceOrDefault = "第一个非默认,否则默认" — 二选一时 Sentinel 约定用后者保证必有实例(SpiLoader.java:211-238 + SlotChainProvider.java:33-37)。注意:这里遍历的是**未排序** classList(文件声明序),与 loadInstanceListSorted 的 order 序是两套。