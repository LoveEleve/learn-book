# Pass 2 闭环笔记 Q2: SystemRuleManager 的系统保护判定

## 验证过程

- `SystemRuleManager` 用一组 volatile 阈值字段保存系统规则，并有一组 `*IsSet` 标志标记用户是否设置过 (`SystemRuleManager.java:44-60`)。
- 静态块启动一个 1 秒周期的 `SystemStatusListener` 采样任务，并注册 property listener (`SystemRuleManager.java:70-76`)。
- `loadSystemConf` 对每条规则取各指标的最小值，任一指标被设置就打开 `checkSystemStatus` 开关 (`SystemRuleManager.java:190-230`)。
- `checkSystem` 的判定顺序：
  1. 开关关闭 → 放行
  2. 非 IN 流量 → 放行（系统保护只针对入站）
  3. 总 QPS 超限 → 拒绝
  4. 总线程超限 → 拒绝
  5. 平均 RT 超限 → 拒绝
  6. 系统负载超限 → 用 BBR 算法二次判断
  7. CPU 使用率超限 → 拒绝 (`SystemRuleManager.java:240-285`)
- `checkBbr` 用 `currentThread > maxSuccessQps * minRt / 1000` 判断是否真的过载，避免负载高但吞吐未饱和时误拒 (`SystemRuleManager.java:287-293`)。

## 结论

系统保护是“全局入站流量”维度的保护，只针对 IN 流量。它用 `Constants.ENTRY_NODE` 的全局统计做 QPS/线程/RT 判定，负载用 BBR 算法二次确认，CPU 直接比较。阈值取多规则最小值，任一设置即开启检查。