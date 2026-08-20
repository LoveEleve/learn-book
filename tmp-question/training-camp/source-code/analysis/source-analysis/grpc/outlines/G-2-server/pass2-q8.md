# 闭环笔记 Q8 — 执行器三源: 池 + 串行桥 + 动态切换

假设: 服务端执行器有三级: 默认 executor 来自池 (ObjectPool), 所有回调经 SerializingExecutor 串行化, executorSupplier 可在请求时动态切换。

验证过程:
- **池化管理**: `ObjectPool<? extends Executor> executorPool` (ServerImpl.java:99,145) — start 时 `executorPool.getObject()` (L188), 停机时 `executorPool.returnObject(executor)` (L361-362) — 与传输共享线程资源
- **串行桥**: JumpToApplicationThreadServerStreamListener 持有 wrappedExecutor (q3), MethodLookup/回调全在 serializing executor 排队 (L515-517 注释: 保证顺序)
- **动态切换**: maySwitchExecutor (q8 代码): `executorSupplier.getExecutor(call, headers)` → **`((SerializingExecutor) wrappedExecutor).setExecutor(switchingExecutor)`** — 请求按 method/headers 换执行器, 且切换发生在 MethodLookup 里 (排队阶段), 后续回调自动用新执行器
- ServerCallExecutorSupplier 接口: `Executor getExecutor(ServerCall, Metadata)` (api/ServerCallExecutorSupplier.java:33) — 请求级自定义 (如按 method 分线程池/事务执行器)

代码类型: Glue (执行器桥)

结论: 执行器模型 = **池 (资源复用) + SerializingExecutor (顺序保证) + Supplier (请求级切换)** 三层; 动态切换的巧妙点: setExecutor 发生在回调排队之前 (MethodLookup 阶段), 无竞态; **被放弃的方案: 每请求 new 执行器** — 池化避免线程风暴; SerializingExecutor 是 gRPC 回调不并发的根本保证 (单个调用内回调严格串行)。 [跨域: SerializingExecutor 与 G-3 客户端回调串行同源] (ServerImpl.java:99,188,361-362,475; ServerCallExecutorSupplier.java:33)
