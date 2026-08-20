# S-2 入口域 — 大纲 (三篇)

> 日期: 2026-08-17 | 素材: pass2-q1~q9 + temporal-trace + completeness 36 问(全 ✅ 回补)
> 拆篇逻辑: 按"进入 → 生命周期 → 异步/统计"三段递进, 与 S-1 同规模

## 上篇: 入门与闸门(触达面) — 01-entry-gateway.md

核心悬念: 业务代码一行 `SphU.entry("resource")` 之后, 究竟走了多少层才到规则?

1. **两条触达 API** — SphU(14 entry 变体 + entryWithPriority + asyncEntry)与 SphO(boolean 版, **Throwable→true 放行哲学**, SphO.java:180-194)
2. **汇聚点** — 所有变体 → CtSph 两个私有方法 (entryWithPriority/asyncEntryWithPriorityInternal); resourceType 标签穿透, **equals 仅按 name**(ResourceWrapper.java:82-94)
3. **默认 context** — InternalContextUtil 静默注入 "sentinel_default_context"(CtSph.java:128 + ContextUtil.java:224-228)
4. **三道放行闸门 + 双上限** — NullContext(2000)/ON 开关/链空(6000)(CtSph.java:120-141 + Constants.java:36-37)

→ 悬念回收: 一行 entry 的完整路程图(含降级短路)

## 中篇: entry 的生命周期(调用栈的起落) — 02-entry-lifecycle.md

核心悬念: entry 与 exit 如何保证配对?释放错了会怎样?

1. **Context 隐式栈** — 单 curEntry 指针 + CtEntry.parent 链(Context.java:62-79);节点访问 getCurNode/getLastNode
2. **嵌套构造** — parent=curEntry, child 回指(CtEntry.java:61-63);树/栈关系
3. **exit 三态** — NullContext 免清理 / 错序"先扯平全栈再抛 ErrorEntryFreeException" / 正常退栈(CtEntry.java:90-127)
4. **自动退出联动** — 默认 context 的 ThreadLocal 自动清理; 演进: 0.1.0 无条件退出 → cbaacfda 收窄为仅默认 context(temporal-trace)

→ 悬念回收: "配对性"由 curEntry 校验强制 + 自愈式错误处理

## 下篇: 异步与统计入口(不占线程的调用) — 03-async-tracer-type.md

核心悬念: 异步线程没有 ThreadLocal, 生命周期怎么挂?业务异常怎么进统计?

1. **AsyncEntry 两阶段** — initAsyncContext(独立 async Context)+ cleanCurrentEntryInLocal(摘除当前线程)(CtSph.java:64-110)
2. **异步退出** — trueExit 在 asyncContext 上跑完整槽链 exit(AsyncEntry.java:84-88); Block 分支不建 asyncContext
3. **Tracer: 异常入统计** — shouldTrace 过滤(BlockException 永不 trace)+ entry.error(CtEntry/Tracer.java:201-225); 定制点 exceptionPredicate/ignore/trace 清单
4. **EntryType 分流** — 仅 IN 记全局入口节点(StatisticSlot:71,140)+ 仅 IN 可被系统规则拦(SystemRuleManager.java:300)

→ 悬念回收: 异步的生命周期 = 对象携带(Entry 即 context); 异常统计 = error 字段 + exit 侧消费(S-5 移交点)

## 悬念总表

| 篇 | 悬念 | 回收位置 |
|---|---|---|
| 上 | 一行 entry 走几层 | 1.4 路程图 |
| 中 | entry/exit 配对靠什么 | 3.3 自愈语义 |
| 下 | 异步生命周期怎么挂 | 1-2 对象携带 |
| 下 | 异常怎么进统计 | 3.3 error 字段 |

## 跨域移交点

- S-5: ENTRY_NODE 计数细节 / entry.error 统计消费 / RT 记录(exit 侧)
- S-3: SystemRule 只拦 IN 的规则细节
- S-1: 链与 equals(name)契约(已在 S-1 文档确认, 无勘误)
- S-8: WebFlux 适配器对 AsyncEntry 的使用