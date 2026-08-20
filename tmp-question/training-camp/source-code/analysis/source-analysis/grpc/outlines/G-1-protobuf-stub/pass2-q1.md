# 闭环笔记 Q1 — AbstractStub 三形态: CRTP 链式不可变设计

假设: 三形态 stub (Async/Blocking/Future) 通过泛型自引用 CRTP 实现链式 with* 配置,每次调用都 build 新对象 — 不可变链式。

验证过程:
- grep `class AbstractStub` → `public abstract class AbstractStub<S extends AbstractStub<S>>` (AbstractStub.java:54) — CRTP 确认, 类型参数 S 约束为自身子类
- grep `build(` → `protected abstract S build(Channel channel, CallOptions callOptions)` (L105) — 每个子类实现 build 返回自身类型
- grep `withDeadlineAfter` → `return build(channel, callOptions.withDeadlineAfter(duration, unit))` (L151-152); withExecutor (L168-169)/withCompression (L180-181)/withCallCredentials (L224-225)/withWaitForReady (L238) — **全部 with* = 复制新实例**
- grep `newStub` → 静态工厂 `newStub(factory, channel, CallOptions.DEFAULT)` (L114-129) — factory 是 StubFactory 函数式接口
- golden 文件: `public static TestServiceStub newStub(io.grpc.Channel channel)` + `newStub(TestServiceGrpc::newStub, channel)` — 生成代码委托 StubFactory

代码类型: Interface (抽象契约) + Implementation

结论: 三形态是同一 CRTP 泛型族的三个叶子 — 每次 with* 都构建不可变副本 (getChannel/getCallOptions 不变式 L85-94), 链式安全; 代价是每链一个配置建一个对象 (微对象, 现代 JVM 可承受)。三形态的差异只在 build() 返回值类型 (Async/Blocking/Future 子类) 与生成代码调用 ClientCalls 的哪个分派面。 (AbstractStub.java:54,105,114,151)
