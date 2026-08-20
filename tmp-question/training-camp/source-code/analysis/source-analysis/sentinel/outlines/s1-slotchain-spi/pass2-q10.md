# Pass 2 闭环笔记 Q10: 链遍历机制 — fireEntry→transformEntry 的双向不对称

## 初始假设
- 链遍历是循环;entry 与 exit 对称。
- 实际: **递归遍历**,且 entry 带泛型转换(transformEntry)、exit 直传 — 不对称源于参数签名差异。

## 验证过程
- 读 `AbstractLinkedProcessorSlot.java:16-38`: `fireEntry` → `next.transformEntry` → `transformEntry` 内 `T t = (T)o; entry(...)`(L22-26)。`fireExit` → `next.exit` **无转换直传**(L31-36)。
- 原因: entry 的 obj 参数是**泛型 T**(每槽 T 不同: NodeSelectorSlot<DefaultNode> 等),Object 沿链传递需 cast;exit 无泛型参数,无需转换 — **不对称是类型系统强制的,不是设计随意**。
- 读 `DefaultProcessorSlotChain.java:11-19`: 匿名头节点 first(entry → super.fireEntry,即从 next 开始);`addLast` 维护 end 尾指针(L29-33);链入口 `entry → first.transformEntry`(L52-56)。
- 遍历形态: 递归调用链(深度 = 槽数 10,无栈风险);exit 时从当前槽 fireExit 沿链继续 — **entry 与 exit 都是自传播**(slot 自己不"循环"后续槽)。
- 类型形态: 实际所有内置槽 T=DefaultNode(entry/exit 传 DefaultNode);但链上挂的 Object 起点是 `null`(CtSph.java:139 `chain.entry(context, resourceWrapper, null, ...)`)。

## 代码类型
- Implementation(链式递归 + 泛型桥)

## 跨域关联
- S-1 全域: 每槽 entry 先做自己的工作再 fireEntry(后置语义,StatisticSlot 统计后置的载体)
- S-2 入口: CtSph 以 null 启动链遍历

## 结论
链遍历 = 头节点→transformEntry 递归;entry 泛型 cast 在 transformEntry 内完成,exit 无需 cast — 双向不对称是泛型签名差异的必然结果(AbstractLinkedProcessorSlot.java:16-38 + DefaultProcessorSlotChain.java:11-56)。链起点 obj=null(CtSph.java:139)。