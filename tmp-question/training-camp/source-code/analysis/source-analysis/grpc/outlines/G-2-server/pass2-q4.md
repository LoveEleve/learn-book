# 闭环笔记 Q4 — 拦截器链: handler 洋葱包装 + call 转发

假设: 拦截器在方法查找后、回调前用 InterceptCallHandler 洋葱式包装; 包装顺序 = 数组迭代顺序 (最后添加的在最外层)。

验证过程:
- grep `wrapMethod` (ServerImpl.java:660-675): `handler = methodDef.getServerCallHandler()` → **for (ServerInterceptor interceptor : interceptors) handler = InternalServerInterceptors.interceptCallHandlerCreate(interceptor, handler)`** (L669-670) — 顺序包装
- InterceptCallHandler (ServerInterceptors.java:250-269): `startCall → interceptor.interceptCall(call, headers, callHandler)` (L266-269) — **拦截器拿到原始 ServerCall + 下一层 handler**, 自己决定是否包 call
- 包装次序推导: [i0,i1,i2] → H2(i2, H1(i1, H0(i0, orig))) — **最后添加的 i2 最外层先执行**, 先添加的 i0 最内层最后执行
- **包装时机**: 每请求一次 (MethodLookup 里 wrapMethod, ServerImpl.java:561), 不在注册时 — 可结合 statsTraceCtx 每请求状态
- startWrappedCall (L692-699): `callHandler.startCall(call, headers)` → listener → `params.call.newServerStreamListener(callListener)` → **ServerStreamListenerImpl** (ServerCallImpl.java:238-240) — listener 再包一层 (call + listener + context)
- binlog 钩子 (L674): binlog.wrapMethodDefinition (可观测面)

代码类型: Glue (装饰器链)

结论: 服务端拦截器是 **handler 级洋葱** (不是 call 级): 每请求在 serializing executor 上重新包装; 拦截器接口 interceptCall(call, headers, next) 让拦截器既可拦 handler 链也可包装 call 对象 (PartialForwardingServerCall); **被放弃的方案: 注册时包装一次** — 请求时包装能携带每请求的 statsTraceCtx/context, 且避免不可变注册表持有可变拦截器实例。包装次序: 最后添加的最外层 (与直觉相反, 写书要点名)。 (ServerImpl.java:660-675,692-699; ServerInterceptors.java:44-46; ServerCallImpl.java:238-240)
