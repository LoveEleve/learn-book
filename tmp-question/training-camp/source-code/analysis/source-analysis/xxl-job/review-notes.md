# XXL-Job 域规划 — 审查记录

## 审查范围
- 执行计划 6.4 XXL-Job 7 域
- 源码：`xxl-job-admin` + `xxl-job-core`
- 当前阶段：仅做域规划/Pass1/Pass2/全视角问题/大纲，不写正文

## 本轮深审发现

1. **路由策略清单过时**
   - 计划写 `LFH`
   - 源码实际是 `LFU`
   - 且还存在 `LRU`、`FAILOVER`
   - `SHARDING_BROADCAST` 不是普通路由，而是多次 trigger

2. **XJ-2 边界过窄**
   - 仅写 `XxlJobSpringExecutor -> JobThread` 不够
   - 必须吸收 `ExecutorBiz/ExecutorBizImpl/TriggerCallbackThread`
   - 否则执行端主闭环断裂

3. **XJ-5 至少两套失败处理**
   - block strategy：入队前决定丢弃/覆盖/串行
   - callback retry：回调失败写 `callbacklog` 文件，再由后台线程重试

4. **XJ-7 日志至少三条线**
   - executor 本地 job log 文件
   - callback 失败重试文件
   - admin DB 日志与报表聚合

5. **XJ-4 的真实主线不是 ShardingUtil**
   - `ShardingUtil.java` 当前是注释态残留
   - 现行分片主线是 Admin 侧 `XxlJobTrigger` 构造分片 trigger 参数，再由 `TriggerParam/XxlJobContext` 传播

6. **XJ-6 的真实主线不是单一 Groovy**
   - `GlueFactory` + `SpringGlueFactory` + `GlueJobHandler` + `ScriptJobHandler`
   - 需区分 JVM 内 handler 包装与外部脚本执行

## 当前状态
- 已完成：`pass1-notes.md`
- 已完成关键闭环：`pass2-q1.md` `q2.md` `q3.md` `q4.md` `q5.md` `q6.md` `q7.md`
- 已完成：`completeness-questions.md`、`outline.md`
- 未进入正文写作

## 规划收束判断
- 7 个域都已有对应闭环证据支撑。
- 执行顺序当前可稳定为：`XJ-1 → XJ-2 → XJ-3 → XJ-4 → XJ-5 → XJ-6 → XJ-7`。
- 若进入正文阶段，前置条件已经从“补边界”降为“逐篇按行号重验证”，不再需要重新拆域。

## 结论
- 7 域数量可保留
- 但写作前必须以本审查结果覆盖旧执行计划中的过时术语和边界错误
- 当前已达到“规划可收束、可进入正文前最终核对”的状态，而不是正文阶段
