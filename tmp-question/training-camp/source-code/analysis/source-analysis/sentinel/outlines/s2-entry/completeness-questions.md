# S-2 入口域 — 全视角提问 completeness (Pass 3 前置)

> 日期: 2026-08-17 | 源码: Sentinel 1.8.9 | 方法: 6 身份全视角提问

## 功能完整性 (Functional)

1. SphU 20 个公开入口(12 entry + 6 asyncEntry + 2 entryWithPriority), 全部汇聚到哪 2 个内部方法? — entryWithPriority / asyncEntryWithPriorityInternal (Q9 已证)
2. 用户显式 enter 后, 嵌套 entry 如何保持链? — parent/child 双向 (Q1/Q6)
3. SphO 与 SphU 行为差异? — 仅 API 形状 + Throwable→true (Q5)
4. 异步入口如何绕过 ThreadLocal? — asyncContext 对象携带 (Q3)
5. 业务异常如何进统计? — Tracer→entry.error (Q8)
6. NullContext 降级后 entry 是否正常返回? — 无链 CtEntry, 调用方无感知 (Q7)

## 性能 (Performance)

7. contextNameNodeMap 的读写代价? — volatile 读 + 写时 COW 全量复制 (ContextUtil.java:143-149)
8. 与 chainMap 一样为什么不用 ConcurrentHashMap? — 结构相同: 读多写极少 + 无锁读 (对 Q7 的假设)
9. MAX_CONTEXT_NAME_SIZE 2000 的依据? — 防 EntranceNode 树膨胀 (内存面, 与 6000 同哲学)
10. entry 的 OBJECTS0 空数组复用? — SphU/CtSph 静态共享, 零分配
11. Tracer 过滤是否每次都反射? — isAssignableFrom 循环, 无缓存(小清单可接受)
12. setNullContext 的 shouldWarn 无 volatile 是否安全? — 类注释 "Don't need to be thread-safe", 仅丢一次警告 (ContextUtil.java:161-173)

## 并发/线程安全 (Concurrency)

13. contextHolder ThreadLocal 的泄漏面? — exit 自动清 + 默认 context 联动退出 (CtEntry.java:123-125)
14. trueEnter 双检锁是否正确? — 先 volatile 读 → LOCK 内再查再 COW (ContextUtil.java:125-149)
15. 两个线程同时首个进入同一 context 名? — 锁内重查, 只建一个 EntranceNode
16. asyncContext 是否线程安全? — 仅归属异步线程, 无跨线程写
17. ContextUtil.exit 与 CtEntry.exit 的同步? — exit 只在调用线程, ThreadLocal 天然隔离
18. ENTRY_NODE 全局节点多线程计数? — 原子计数, 属 S-5

## 扩展性 (Extensibility)

19. 新 API 变体如何接入? — SphResourceTypeSupport 接口 + CtSph 覆写 (1.7.0 扩展点实证)
20. 自定义资源分类? — resourceType int 标签, equals 不含 (Q9)
21. 用户能否自定义 entry 行为? — CtEntry 非 final? 否: 扩展走 Sph 接口
22. exceptionPredicate 全局钩子? — Tracer 定制点 (Q8)
23. whenTerminate 钩子语义? — exit 时回调, 异步/同步通用 (CtEntry.java:132-138)
24. Sph 接口演进: 旧方法 @Deprecated? — trace(count) 已标 @Deprecated (Tracer.java:54)

## 边界/异常 (Boundary)

25. context 超限 2000 后行为? — NULL_CONTEXT 一次性 WARN, entry 全放行 (Q7)
26. 全局开关 ON=false 后? — 无链 CtEntry, 与超限同效 (CtSph.java:132-134)
27. 释放顺序错乱? — 先扯平全栈再抛 ErrorEntryFreeException (Q1)
28. entry 后不 exit? — 默认 context 自动退出; 显式 context 泄漏 ThreadLocal(用户责任)
29. BlockException 的 exit 配对? — CtSph catch 里 e.exit(count) (CtSph.java:150-153)
30. asyncEntryWithNoChain 的 BlockException? — 降级路径不抛 (Q3 已证三路径直接返回)

## 兼容性/演进 (Compatibility)

31. 0.1.0 → 1.8.9 入口面变化? — SphU.entry 签名稳定; 新增 resourceType/async/priority
32. ContextUtil.trueEnter 的 COW 何时引入? — 待 git 查证 (与 chainMap 同期?)
33. EntryType IN/OUT 变化? — 曾有过 IN/OUT 之外语义(注释提及 internal), 现仅二值
34. SphResourceTypeSupport 何时加入? — @since 1.7.0
35. asyncEntryWithType 何时加入? — @since 1.7.0
36. Entry.getLastNode 的语义变化? — 现实现 parent==null → null (CtEntry.java:147-149)

## 回补后进入 Pass 4 的判断

- [x] 全覆盖: 36 问中 14 个已有闭环证据, 其余 22 个将在大纲/正文阶段逐条落锚
- 待 git 查证: #32 (COW 引入版本), 与 S-1 的 1.8.1 SPI 重构同期对照
- 待 S-5 移交: #18 (ENTRY_NODE 计数), #33 (EntryType 语义考古)