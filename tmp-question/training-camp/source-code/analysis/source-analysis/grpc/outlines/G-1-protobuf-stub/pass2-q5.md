# 闭环笔记 Q5 — compiler 生成面: 一个 service 生成 7 个类型

假设: java_generator.cpp 为一个 .proto service 生成完整客户端/服务端代码面, 且 1.83 已支持 BlockingV2。

验证过程:
- grep `StubType` → 枚举含 **BLOCKING_V2_CLIENT_IMPL** (java_generator.cpp:585-594, "BlockingV2") — 4 种客户端 stub: Async/Future/Blocking/BlockingV2
- grep `ASYNC_INTERFACE` → 生成 AsyncService 接口 (L608-611) — 服务端面
- golden 实证: TestServiceGrpc (final, L14) 含 4 类 stub 工厂 (newStub/newFutureStub/newBlockingStub/**newBlockingV2Stub** L288) + **AsyncService 接口** (L335) + **TestServiceImplBase** (L433, implements BindableService+AsyncService) + TestServiceStub (L447)
- grep `PrintMethodHandlerClass` (L937) → 生成 **MethodHandlers** 类 implements 全部 4 种 ServerCalls.Method 接口, invoke() 用 **switch (methodId)** 分派 (L966-985); 方法 id 按 client_streaming **稳定排序使 switch 表紧凑** (L941-948)
- grep `StubFactory` (L566) → 匿名内部类 newStub 委托构造 (L581-585)
- 静态 MethodDescriptor: volatile + **双重检查锁** DCL (golden L31-35) + @RpcMethod 注解 + ProtoUtils.marshaller + MethodDescriptorSupplier
- @GrpcGenerated (java_generator.cpp:1366) — 标记生成代码 (lint/工具识别)

代码类型: Implementation (代码生成器)

结论: 生成面 = 1 个 Grpc 门面类 (静态 MethodDescriptor DCL + 4 stub 工厂 + AsyncService + ImplBase + MethodHandlers) + 4 个 stub 类。**MethodHandlers 的 switch 分派是服务端路径的核心**: BindService 注册 MethodHandlers, 运行时 invoke() 按 methodId switch 到 serviceImpl 方法 — 用 switch 表而非虚方法表, 避免每个方法一个处理器类。BlockingV2 已在 1.83 生成 (issue 10918 演进落地)。 (java_generator.cpp:566-650,937-1000,1366)
