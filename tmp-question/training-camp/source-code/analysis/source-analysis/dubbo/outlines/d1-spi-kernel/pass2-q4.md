# 闭环笔记 q4: 激活面 — @Activate 条件激活

## 假设
@Activate 扩展按 group/条件/顺序激活; 过滤面 (Filter/Listener) 的装配机制。

## 验证过程
- **getActivateExtension(url, group)** (L344-440): 按 group 筛选 + **TreeMap (activateComparator) 排序** + isActive 条件判定
- **@Activate 参数** (Activate.java:45-93): **group() / value() (支持 "key:value" 条件对!) / order() / before() / after()** — 排序三机制
- **isActive** (L471): value 条件对 → URL 参数匹配判定
- **cachedActivateExtensions**: 激活扩展缓存 (组内)
- **历史修复注释** (L346): "@SPI's wrapper method null pointer" bug — 先加载类再判断
- **消费面**: ProtocolFilter 链/Listener/注册通知 全经激活面装配

## 代码类型
Implementation (激活面)

## 跨域关联
- D-4: Filter 链 (激活面装配)
- D-5: 注册中心 listener (激活)
- 对照: Spring @Conditional — 条件装配对照

## 结论
激活 = group 筛选 + value 条件对 + order/before/after 排序 — 过滤链装配机制。
源码位置: ExtensionLoader.java:344-471; Activate.java:45-93
