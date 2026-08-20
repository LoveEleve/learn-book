# M-4 插件机制 — InterceptorChain 嵌套包装 + Plugin 代理 + 4 类目标白名单

> 前置: [[M-1-configuration]] (4 工厂汇聚点) | 复用: [[M-2-executor]] [[M-5-mapping]] (被拦截对象语义) | 对照: [[s24-aop-proxy]] (Spring AOP 切面 vs MyBatis 声明式拦截) | 引出: [[MP-4-plugin]] (MybatisPlusInterceptor 宿主)
> 🔴 Deep | 7 KP | [模式: 责任链(嵌套包装)+JDK 动态代理+白名单校验]
> Pass 2 闭环: q1(4 类白名单) q2(无匹配不包装) q3(invoke 双条件) q4(嵌套顺序) q5(接口契约) q6(汇聚点) q7(用例语义)

**读者处境**: `@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))` 一行注解, 插件就能在每次 SQL prepare 前介入 — 为什么能拦 Map.get 却会抛 "not supported as a plugin target"?为什么 PageInterceptor 能改 SQL?多个插件谁先执行?这篇拆 MyBatis 插件机制: 接口契约、链式包装、代理分派、目标白名单。

### 1. 接口契约 — Interceptor 三方法 + Signature 精确匹配

场景: 实现一个插件最少写什么?@Signature 的参数必须和真实方法完全一致吗?
源码路径:
- `Interceptor.java:23-35` — 三方法(q5): `intercept(Invocation)` 唯一抽象(拦截逻辑); `plugin(Object)` 默认 `Plugin.wrap`(可覆写自定义包装); `setProperties` 默认 NOP(配置注入)
- `Signature.java:25-54` — type/method/args 三要素: **args 必须与真实方法参数类型完全一致**(Plugin.getSignatureMap 用 `sig.type().getMethod(sig.method(), sig.args())` 反射解析, 找不到抛 PluginException L79-83)
- `Intercepts.java:50-52` — @Intercepts 容器注解(**@Target(TYPE) 放类上**): 一个插件可声明多个 @Signature
关键设计: 最小契约(q5): 实现者只写 intercept + 注解声明, 包装/解析/校验全框架化; Signature 精确匹配保证方法级粒度 — 参数类型错一个就解析失败(构建期暴露, getSignatureMap 在 wrap 时执行)。[模式: 声明式扩展]
数据流: 声明 @Intercepts → 注册 addInterceptor → wrap 时 getSignatureMap 反射解析 → intercept 调用时匹配。

### 2. 链式包装 — InterceptorChain.pluginAll 嵌套顺序

场景: 多个插件注册后, 谁的拦截先执行?包装层次怎么嵌套?
源码路径:
- `InterceptorChain.java:25-44` — ArrayList 存储; `pluginAll(target)`(L29-34): 循环 `target = interceptor.plugin(target)` — **后注册的包在最外层**(q4)
- `Configuration.java:703-742` — 汇聚点(q6): 4 工厂统一 pluginAll — newExecutor L741/newStatementHandler L721/newParameterHandler L707/newResultSetHandler L714(M-1 交付)
关键设计: 洋葱模型(q4): 先注册的先被包(内层), 后注册的包住前面(外层) — 调用时外层先拦截; 拦截器执行顺序与注册顺序相反; 所有挂载点集中在 4 工厂, 插件无需知道执行链细节。[模式: 责任链嵌套包装]
数据流: newExecutor → pluginAll: interceptor1.plugin → interceptor2.plugin(包住) → 最终代理 → invoke 从最外层开始。

### 3. 代理分派 — Plugin.wrap 三步骤 + invoke 双条件

场景: 插件对不相关的对象会不会产生代理开销?未拦截的方法调用会怎样?
源码路径:
- `Plugin.java:44-52` — wrap 三步骤(q2): ①getSignatureMap(解析注解, 缺失 @Intercepts 抛 issue#251 L68-73) ②getAllInterfaces(target 继承链收集 signatureMap 声明的接口 L89-100) ③**无匹配接口直接返回原 target 不创建代理**(L48-51)
- `Plugin.java:55-65` — invoke(q3): `signatureMap.get(method.getDeclaringClass())` 命中且 `contains(method)` → `interceptor.intercept(new Invocation(target, method, args))`; 否则 `method.invoke(target, args)` 透传; ExceptionUtil.unwrapThrowable 解包
关键设计: 零开销防护(q2): getAllInterfaces 沿继承链(含父类)找匹配接口, 一个都不匹配就不代理 — 无关对象原样返回; invoke 双条件(q3): 声明类+方法双重匹配才拦截, 未拦方法透传 — 代理对非拦截行为完全透明。[模式: JDK 动态代理+条件分派]
数据流: wrap → 代理(或原对象) → 调用任意方法 → declaringClass 命中? intercept : 透传。

### 4. 目标白名单 — Invocation 4 类校验 + proceed 语义

场景: 为什么不能随便拦截任意类?拦截器里怎么继续原调用?
源码路径:
- `Invocation.java:30-36` — **4 类白名单**(q1): `targetClasses = [Executor, ParameterHandler, ResultSetHandler, StatementHandler]` — Invocation 构造校验, 其他目标抛 "is not supported as a plugin target"(测试 shouldPluginNotInvokeArbitraryMethod 拦截 Map.get 实证)
- `Invocation.java:52-56` — `proceed()`: `method.invoke(target, args)` — **拦截器必须手动继续链**(不调 proceed 就短路)
- `Invocation.java:39-50` — getTarget/getMethod/getArgs 访问器(拦截器可改参数/拿目标)
关键设计: 运行期白名单(q1): wrap 不校验(注解声明什么都能解析), **invoke 时 Invocation 构造校验** — 设计上拦截器只面向 4 类执行处理器, 防任意类劫持; proceed 显式语义: 拦截=前置/后置/短路三种模式都由是否调用 proceed 决定。[模式: 白名单校验+显式继续]
数据流: invoke 命中 → new Invocation(校验通过?) → interceptor.intercept → 逻辑 → proceed() 继续原方法(或短路返回)。

### 5. 实战用例 — 多租户 schema 切换 + 对照 spring-aop

场景: 一个真实插件长什么样?和 Spring AOP 的区别?
源码路径:
- `PluginTest.java:75-100` — 多租户(q7): `@Intercepts(@Signature(type=StatementHandler.class, method="prepare", args={Connection.class, Integer.class}))` → intercept 里 `con.setSchema(ThreadLocal 租户名)` → `invocation.proceed()` — prepare 时机改连接 schema, 同一数据源多租户隔离
- `PluginTest.java:86-100` — 非法目标: 声明拦截 Map.get → wrap 成功 → **调用时抛 IllegalArgumentException**(白名单实证)
关键设计: 用例语义(q7): 插件选择 4 处理器的方法级时机(prepare/query/parameterize/handleResultSets)介入; 对照 spring-aop(s24-28): AOP 是配置式切面(任意 bean/任意方法, CGLIB/JDK 代理+切点表达式), MyBatis 是声明式拦截(4 类白名单+注解精确签名) — 更窄更可控; 这也是 MP 的 MybatisPlusInterceptor 宿主(MP-4 导航)。[模式: 方法级时机注入]
数据流: 查询 → newStatementHandler(pluginAll 包装) → prepare 调用 → 代理分派 → SwitchCatalogInterceptor.intercept → setSchema → proceed → 原 prepare。

### 负面空间 — 插件层刻意不做的事

- **不做任意对象拦截**: 白名单限 4 类执行处理器, 不开放任意类代理(防误用+性能)
- **不做自动排序/依赖**: 插件执行顺序=注册顺序, 无优先级注解, 依赖关系的插件需自行保证注册序
- **不做执行期重载**: 拦截器在工厂创建时包装一次, 运行期增删插件不影响已创建的处理器

→ 引出: MP 阶段回流 — MybatisPlusInterceptor 正是本机制的宿主(MP-4, 导航); Spring AOP 对照见 s24-28 → [[MP-4-plugin]] [[s24-aop-proxy]]
