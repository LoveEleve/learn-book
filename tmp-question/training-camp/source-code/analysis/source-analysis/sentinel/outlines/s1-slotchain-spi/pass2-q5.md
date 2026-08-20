# Pass 2 闭环笔记 Q5: InitExecutor 触发链 — 谁在什么时候初始化,顺序如何定?

## 初始假设
- InitFunc 由某个启动类显式调用;顺序仅由 @InitOrder 决定。
- 实际:触发点是**类加载时**而非应用主动调用,且排序是**双层**的。

## 验证过程
- 触发链: `Env.java:36` 静态块 → `InitExecutor.doInit()`(Env.sph = new CtSph() 同在静态块,L34)。另有 `ClusterStateManager.java:51` 也调 doInit — CAS(initialized)保证**只执行一次**,先到先得。
- `InitExecutor.java:36-40`: doInit CAS 后,`SpiLoader.of(InitFunc.class).loadInstanceListSorted()` — **第一层 @Spi order**。
- `InitExecutor.java:46-70`: insertSorted 按 resolveOrder(**@InitOrder.value()**,缺省 InitOrder.LOWEST_PRECEDENCE)→ **第二层 @InitOrder**。
- 失败语义: 单 InitFunc 异常 → 中断 + `ex.printStackTrace()`(不退出进程,注释 L38 原文 "the application will exit" 与实际 printStackTrace 有出入 — 注解说退出,代码实际不退出,以代码为准)。
- 内置注册 8 个: core 1 (MetricCallbackInit) / extension 3 (ParamFlowStatisticSlotCallbackInit, MetricExporterInit, PromExporterInit) / transport 2 (CommandCenterInitFunc @InitOrder(-1), HeartbeatSenderInitFunc @InitOrder(-1)) / cluster 2 (DefaultClusterClientInitFunc @InitOrder(0), DefaultClusterServerInitFunc)。

## 代码类型
- Glue(启动生命周期编排)

## 跨域关联
- S-9 集群: ClusterStateManager 触发 + cluster 两个 InitFunc
- S-10 传输: CommandCenterInitFunc/HeartbeatSenderInitFunc (@InitOrder(-1) 优先启动)

## 结论
首次触碰 Env 或 ClusterStateManager 时触发,CAS 单次执行;两层排序(@Spi order 先、@InitOrder 后);单点异常中断初始化但不退出进程(InitExecutor.java:36-70 + Env.java:34-36)。注释 "process will exit" 与实现不符 — 记录为文档偏差。