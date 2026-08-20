# H-12 代理生成 — ProxyFactory → JavassistProxyFactory → Proxy 代理族

> 依赖 s24-s28 spring-aop (对照) + H-3/H-4 | 🔴 Deep | 6 KP | [模式: 字节码代理 + 门面工厂 + 委托]

**读者处境**: 用户从池拿到的连接其实是"代理" — 谁生成的?为什么用 Javassist 字节码而不是 JDK 动态代理?代理内部怎么转发到真实连接?

### 1. 代理族结构 — 6 个抽象代理类

场景: 连接/语句/结果集都要被包装 — 代理族怎么组织?

源码路径:
- `ProxyConnection.java:37` — **抽象代理**: `abstract class ProxyConnection implements Connection`(L37) — 代理 Connection 的抽象基类
- `ProxyConnection.java:51,53,54` — **内部**: `delegate`(L51, 真实连接) + `poolEntry`(L53, 池条目) + `leakTask`(L54, 泄漏任务)
- 同类: ProxyStatement/ProxyPreparedStatement/ProxyCallableStatement/ProxyResultSet/ProxyDatabaseMetaData — 共 6 个抽象代理类, 各 implements 对应 JDBC 接口

关键设计: **Why 抽象代理 + implements 接口？** 每个代理类实现 JDBC 接口(Connection/Statement/...), 用户无感; 持有 delegate(转发)+poolEntry(状态)+leakTask(追踪) — 代理层是"拦截 + 转发"的统一入口。[模式: 委托 + 接口实现]

数据流: 用户拿 ProxyConnection → 调方法 → 代理拦截(状态追踪/异常处理)→ delegate 转发真实连接 → 返回。

### 2. Javassist 字节码生成 — 具体代理类

场景: 抽象代理类怎么变成可实例化的具体类?为什么用 Javassist?

源码路径:
- `JavassistProxyFactory.java:42` — **生成器**: `JavassistProxyFactory`(L42) — **构建期代码生成器**(main 方法), 用 Javassist ClassPool 生成代理类到 target/classes
- `JavassistProxyFactory.java:64,65,66,67,71,72` — **生成 6 类**: `generateProxyClass(Connection/Statement/ResultSet/DatabaseMetaData/PreparedStatement/CallableStatement, ...)`(L64-72) — 为每个抽象代理生成具体子类
- `JavassistProxyFactory.java:114` — **generateProxyClass**: 基于 ClassPool 生成具体代理类(继承抽象代理 + 实现接口)
- 注入: 生成的类包含 ProxyFactory 静态工厂的方法体(见 §3)

关键设计: **Why Javassist 字节码而非 JDK 动态代理？** ①方法级拦截要覆盖 JDBC 全部方法(含异常处理/状态复位)— 字节码生成可精确控制每个方法体; ②性能 — 生成的类是普通类, 无反射调用开销(JDK 动态代理有 InvocationHandler 反射)。**Why 生成具体子类？** 抽象代理类实现公共逻辑(委托/状态), 生成类只需补方法体 — 一次生成复用于每个连接实例。[模式: 字节码生成]

数据流: JavassistProxyFactory 静态块 → ClassPool → generateProxyClass(L114) 生成 6 具体类(继承抽象代理) → 实例化时 new 具体代理类。

### 3. ProxyFactory 门面 + 池衔接

场景: 谁调 ProxyFactory 建代理?方法体哪来的?和 spring-aop 比怎样?

源码路径:
- `ProxyFactory.java:46` — **门面**: `static ProxyConnection getProxyConnection(...)`(L46) — 静态工厂方法
- `ProxyFactory.java:48` — **方法体注入**: 源码方法体只是 `throw new IllegalStateException(...)`(占位), 注释 "Body is replaced (injected) by JavassistProxyFactory"(L48) — **构建时** `JavassistProxyFactory.main`→`modifyProxyFactory`(L77) 改写 ProxyFactory.class 注入真实方法体
- 衔接: `PoolEntry.createProxyConnection(leakTask)`(H-3 L179 调用) → ProxyFactory.getProxyConnection → 返回代理
- 对照: spring-aop CGLIB 也用字节码生成代理, 但为 AOP 切面; Hikari 的 Javassist 专为 JDBC 包装拦截

关键设计: **Why 方法体注入？** ProxyFactory 静态方法源码是占位, Javassist 构建期(main→modifyProxyFactory)注入真实逻辑 — 这样池代码只调 ProxyFactory.getProxyConnection, 具体生成由 Javassist 透明完成。**Why 与 s24-s28 对照？** spring-aop 的 CGLIB 代理用于切面, Hikari 的 Javassist 代理专用于 JDBC 包装 — 都是字节码生成但目的不同。[模式: 门面工厂 + 字节码注入]

数据流: H-3 borrow → poolEntry.createProxyConnection(leakTask) → ProxyFactory.getProxyConnection(L46, 方法体已注入)→ new 具体代理类 → 返回用户。用户 close() → 代理 close(H-4)。

→ 引出 H-8: 泄漏检测 — 代理之后: ProxyLeakTask/leakDetectionThreshold 的借用超时泄漏跟踪(前置 H-12)。
