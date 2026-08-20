# S-1 时空溯源:slotchain + SPI 演进(0.1.0 → 1.8.9)

> 日期: 2026-08-17 | 🔴 域强制 (01 §时空溯源)
> 方法: git show <tag>:<file> 逐版本对比(未 checkout,工作区不变)
> 覆盖: 链构建方式 / SPI 机制 / 槽序 / chainMap 并发

## 演进时间线(全部源码实证)

| 版本/时点 | 链构建 | SPI 机制 | 槽序关键点 |
|---|---|---|---|
| 0.1.0 (2018-07-23, 首 commit c92fea5d) | **硬编码** DefaultSlotsChainBuilder 手动 new 8 槽 | 无任何 SPI | NS→CB→Log→Stat→**System→Authority**→Flow→Degrade |
| 2018-09-12 (commit ca2f4d9f, #145) | SlotChainBuilder 接口 + **SlotChainProvider** 出现 | **JDK ServiceLoader**(仅 Builder 层面) | 同 0.1.0(硬编码链序) |
| 1.4.0 ~ 1.6.x | 同 #145 | 同 #145 | 同 0.1.0 |
| **1.7.0** | 同 #145 | 同 #145 | NS→CB→Log→Stat→**Authority→System**→Flow→Degrade(**对调!**) |
| **1.8.1** (2021-01-27, commit 62efb78d, #1383 "Refactor SpiLoader and enhance SPI mechanism") | DefaultSlotChainBuilder 改为 **@Spi(isDefault=true) + loadInstanceListSorted** | **自有 SpiLoader + @Spi order** 取代 JDK | 9 内置槽全部 SPI 化,order 常量控制 |
| 1.8.9 (2025, 本分析版本) | 同 1.8.1 + warn 过滤非 AbstractLinkedProcessorSlot | 自有 SPI(类缓存/别名/单例/双检锁) | +DefaultCircuitBreakerSlot(order -1500) |

## 三阶段画像

### 阶段一:硬编码时代(0.1.0)
- `DefaultSlotsChainBuilder.build()` 手写 8 行 addLast: 加槽 = 改源码 + 重新编译
- 链容器已定型: 匿名头节点 + end 尾指针 + addFirst/addLast(与 1.8.9 逐字一致)
- chainMap 已用 **COW 重建**(new HashMap + putAll)但**非 volatile**(1.8.9 补 volatile)
- MAX_SLOT_CHAIN_SIZE + 超限返回 null 的降级语义 **第一天就在**

### 阶段二:Builder SPI 化(#145 → 1.7.0)
- SlotChainProvider.newSlotChain() 出现: volatile builder 懒加载 + JDK ServiceLoader
- resolveSlotChainBuilder: 遍历 LOADER,非默认 builder 优先;无则兜底 new DefaultSlotChainBuilder
- **槽仍硬编码** — 用户只能换整条链,不能插单个槽
- 1.7.0 链序变化: Authority 提到 System 之前(黑名单/白名单快速拒绝前置);对调 commit 未检索到,动机推测为"白名单放行前先拦截,减少 System 检查代价"[待查 commit]

### 阶段三:自有 SPI 时代(1.8.1+ #1383)
- 动机(类头注释实证): "A simple SPI loading facility (refactored since 1.8.1)"
- JDK ServiceLoader 三缺(无 order/无别名/无默认语义)→ 自有 SpiLoader(单例缓存/双缓存/双检锁)
- 槽从"硬编码"到"SPI 注册 + @Spi(order) 声明位置" — 加槽不再碰核心代码
- 1.8.9 增量: DefaultCircuitBreakerSlot(熔断重构 1.8.0 引入,order -1500 插在 Flow 与 Degrade 之间)

## 不变与变(对照 1.8.9 锚点)

**五不变**:
1. 链容器结构(头节点/end/addFirst/addLast/setNext)— DefaultProcessorSlotChain.java
2. LogSlot 恒居链第 3 位(order -8000 承接 0.1.0 硬编码位)
3. MAX_SLOT_CHAIN_SIZE + 超限返回 null 降级 — CtSph.java:201-203
4. COW 重建模式 — CtSph.java:206-210
5. NodeSelector 恒第一/ClusterBuilder 恒第二(建节点必须先于一切检查)

**五变化**:
1. 链构建: 硬编码 → Builder SPI(JDK)→ 槽 SPI(自有)三连跳
2. 槽序: System→Authority 对调为 Authority→System(1.7.0)
3. SPI: 无 → JDK ServiceLoader → 自有 SpiLoader(1.8.1)
4. chainMap: 非 volatile COW → volatile COW(1.8.9)
5. 槽数量: 8(0.1.0)→ 9(1.8.1, 无 ParamFlow, extension 在核心外)→ 9 内置 + 1 扩展(1.8.9)

## 对本域写作的启示

1. 上篇"链序"叙事可引 1.7.0 对调事实: 槽序是演进产物,不是一次性设计
2. 中篇"为什么自有 SPI"可引 #145→#1383 两步走: 先换 Builder 后换槽,每次只解决一个需求层次
3. 下篇"降级"语义是第一天设计,持久未变 — 强化"静默放行是有意为之"
4. 时空溯源确认: 1.8.9 的槽 SPI 化是**扩展开放性**的最终形态,加槽零核心改动(对比 0.1.0 改源码)

## 验证记录

- 0.1.0 CtSph 注释已含 "Note that total ProcessorSlot count must not exceed MAX_SLOT_CHAIN_SIZE" — 上限语义与 1.8.9 同源
- 1.8.1 DefaultSlotChainBuilder 与 1.8.9 差异仅: 1.8.9 增加了 warn 分支(1.8.1 已有)与 CircuitBreakerSlot 注册 — 核心机制同构
- [待查] 1.7.0 Authority/System 对调的 commit 说明(行为变化已实证,动机注释待补)
