# 闭环笔记 q4: 上下文存储 SPI — ContextCore 双实现

## 假设
线程上下文存储可插拔 (ThreadLocal/FastThreadLocal); 传播依赖的底层载体。

## 验证过程
- **ContextCore SPI** (ContextCoreLoader:27-41): **EnhancedServiceLoader.load(ContextCore.class)** — Optional 加载, 无实现 → null
- **双实现**: **ThreadLocalContextCore** (普通 ThreadLocal, ThreadLocalContextCore:29-31) / **FastThreadLocalContextCore** (Netty FastThreadLocal, L32) — **FastThreadLocal 面向 Netty 事件循环场景**
- **RootContext 键** (L90-116): KEY_XID / KEY_TIMEOUT / KEY_GLOBAL_LOCK_FLAG + MDC 同步
- **全局锁标志** (L150-156): bindGlobalLockFlag — @GlobalLock 场景 (S-4 交叉)
- **传播承载**: 挂起/恢复全部通过 CONTEXT_HOLDER (get/put/remove) — 与具体实现解耦
- **可替换面**: 响应式/异步场景可换 ContextCore 实现 (S-9 Spring 集成可能替换)

## 代码类型
Architecture (SPI 存储)

## 跨域关联
- S-9: Spring 集成 (上下文生命周期管理)
- S-4: 全局锁标志 (context 键)
- S-13: MDC 日志关联

## 结论
上下文存储 = ContextCore SPI (ThreadLocal/FastThreadLocal 双实现); RootContext 统一门面 + MDC 同步。
源码位置: ContextCoreLoader.java:27-41; ThreadLocalContextCore.java:29-31; FastThreadLocalContextCore.java:32; RootContext.java:90-176
