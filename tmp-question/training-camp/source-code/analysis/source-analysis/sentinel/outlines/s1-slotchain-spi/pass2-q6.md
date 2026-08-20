# Pass 2 闭环笔记 Q6: LogSlot 位置语义 — 为什么在链第 3 位(-8000)?

## 初始假设
- LogSlot 记录所有 BlockException,放中间是为了"建完节点、检查之前"。

## 验证过程
- 读 `LogSlot.java:34-46`: entry 内 try-catch 包裹 **fireEntry** — 即 catch 的是**链下游所有槽**抛出的 BlockException;只 catch BlockException,其他 Throwable 走 RecordLog.warn。
- 位置推论: order -8000 位于 StatisticSlot(-7000) 之前 → 覆盖 Authority/System/ParamFlow/Flow/CircuitBreaker/Degrade 全部 6 个检查槽的 block 事件;NodeSelector/ClusterBuilder(前两位)不检查规则,不会抛 BlockException。
- 记录内容: `EagleEyeLogUtil.log(resourceName, 异常简单名, limitApp, origin, ruleId, count)` — LogSlot.java:40-41,含资源/类型/来源应用/调用方来源/规则 ID/计数。
- 链序测试实证: DefaultSlotChainBuilderTest.java:44-78 (NodeSelector → ClusterBuilder → Log → Statistic → Authority → System → Flow → DefaultCircuitBreaker → Degrade → null)。
- 设计语义: "一次捕获全部检查槽的 block" 是 LogSlot 放检查链最前而非中间的原因;NodeSelector/ClusterBuilder 是纯基础设施(建节点),必须最先执行。

## 代码类型
- Glue(异常汇流点)

## 跨域关联
- S-2 入口: 异常传播方向(检查槽抛 → 沿 fireEntry 反向冒泡 → LogSlot 捕获)
- S-3~S-6: 各检查槽 block 事件的日志面

## 结论
LogSlot 用 try-fireEntry-catch 包住整个检查链,处于所有规则检查槽之前 — 一个 catch 点覆盖全部 block 事件(LogSlot.java:34-46 + Constants.ORDER_LOG_SLOT=-8000 + DefaultSlotChainBuilderTest.java:44-78)。记录 6 字段:资源/类型/limitApp/origin/ruleId/count。