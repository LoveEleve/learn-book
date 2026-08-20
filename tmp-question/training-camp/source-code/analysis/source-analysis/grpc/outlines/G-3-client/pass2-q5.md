# 闭环笔记 Q5 — Context: 不可变快照 + 跨线程传播

假设: Context 是链式不可变上下文 (PHAMT 持久化结构), ThreadLocal 存储, ContextRunnable 实现跨线程 attach/detach — gRPC 取消传播的载体。

验证过程:
- **不可变快照**: `final Node<Key<?>, Object> keyValueEntries` (Context.java:180) — PersistentHashArrayMappedTrie 节点; withValue 生成新 Context (`PersistentHashArrayMappedTrie.put(keyValueEntries, k, v)`, L344-355) — **持久化结构: 新节点共享旧路径, O(log n) 空间**
- **ThreadLocal 存储**: ThreadLocalContextStorage (ThreadLocalContextStorage.java:25-47) `ThreadLocal<Context> localContext` (L32) — attach 设置, detach 恢复; detach 不匹配 → SEVERE 日志 (L44-47)
- **Context.current()** (Context.java:171-176): storage().current() null → ROOT 兜底
- **跨线程恢复**: ContextRunnable (core, L25-44) — 构造保存 Context → `run(): previous = context.attach(); runInContext(); finally context.detach(previous)` (L34-40) — 注释: "performs the same function as Context.wrap(Runnable) without requiring the construction of an additional object"
- **取消链**: CancellableContext (Context.java:224+ 示例); 客户端 createContext → cancellable → 取消 → listener (ClientCallImpl.cancelled L382-391, q4)
- 测试: ContextTest/ThreadLocalContextStorageTest 存在

代码类型: Algorithmic (持久化数据结构 + 传播协议)

结论: Context = **调用级线程本地状态**: PHAMT 让 withValue 成本 = O(log n) 且天然线程安全 (不可变共享); ThreadLocal 让 current() 零锁; ContextRunnable 让任务在目标线程恢复调用上下文 — 取消语义 (CancellableContext) 沿调用链传播到流 (cancel → ClientCallImpl.cancelled → stream.cancel)。**被放弃的方案: ① 可变 Map 拷贝** — 每次 withValue 全量复制 O(n); ② 继承 ThreadLocal (InheritableThreadLocal)** — 跨线程池传播失效且线程池复用污染。 [跨域: G-2 createContext→CancellableContext 服务端对称; Deadline (q6) 是 Context 的值] [算法: HAMT 持久化] (Context.java:180,171-176,344-367; ThreadLocalContextStorage.java:25-47; ContextRunnable.java:25-44)
