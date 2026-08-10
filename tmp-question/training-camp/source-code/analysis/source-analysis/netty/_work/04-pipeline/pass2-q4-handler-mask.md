## Loop Note: Q4 — ChannelHandlerMask 检测

**Hypothesis**: ChannelHandlerMask 用反射检测 handler 类的 @Skip 注解方法，在 AbstractChannelHandlerContext 构造时一次性计算掩码——之后所有传播使用位运算跳过。

**Verification** (`ChannelHandlerMask.java`):
- `line 39-55` — 17 个位定义: MASK_CHANNEL_READ=1<<5, MASK_WRITE=1<<15, MASK_FLUSH=1<<16
- `line 57-59` — MASK_ONLY_INBOUND = 9 个 inbound 位的 OR
- `line 61-62` — MASK_ONLY_OUTBOUND = 8 个 outbound 位的 OR
- `line 65` — `FastThreadLocal<Map<Class, Integer>> MASKS` — 缓存每类的掩码（反射只做一次）
- `mask(handlerClass)`: 检查 MASKS 缓存 → 未缓存 → 反射检测每个 @Skip 注解方法 → 计算掩码 → 存入缓存
- `isSkippable()`: 检查 handlerClass 或其父类是否声明了该方法且带有 @Skip 注解
- `AbstractChannelHandlerContext.java:113`: `executionMask = mask(handlerClass)` — 在构造函数中调用
- mask=0 → ChannelHandlerAdapter (所有方法空实现带 @Skip 注解) — 传播中完全跳过

**Code type**: Implementation (compile-time reflection)

**设计权衡**: 反射一次 — 每类只做一次，后续所有实例共享缓存。位运算检查 — O(1) 位掩码 `&` 操作替代 O(N) 反射 `method.invoke()`。FastThreadLocal — 缓存 thread-local 避免冲突。

**Conclusion**: ChannelHandlerMask = 反射一次 + 位运算 17 次的"编译期"检测。mask=0 → handler 被完全跳过 (Adapter)。source: ChannelHandlerMask.java:38-65
