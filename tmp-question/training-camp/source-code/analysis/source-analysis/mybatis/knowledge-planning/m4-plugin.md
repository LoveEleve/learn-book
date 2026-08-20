# M-4 插件机制 — InterceptorChain 嵌套包装 + Plugin 代理 + 4 类目标白名单

> 项目: MyBatis | 🔴 Deep / 1 篇 | InterceptorChain(44)+Interceptor(35)+Plugin(102)+Signature(54)+Invocation(64)+Intercepts(59)
> 基线: M-PLAN M-4 — 前置: **M-1 (4 工厂 pluginAll 汇聚点) + M-2 (Executor 语义) + M-5 (Parameter/ResultSet 处理器语义)** — 展开 接口契约→链包装→代理分派→白名单校验

---

## §0.8

- 🔴 Deep，1篇 — 接口(**Interceptor L23-35: intercept 抽象[唯一必须实现]+plugin 默认[Plugin.wrap]+setProperties 默认 NOP**; Signature L25-54: type/method/args 精确签名; Intercepts 容器注解) → 链(**InterceptorChain L25-44: ArrayList+pluginAll 循环包装[后注册包外层 L34-39]+getInterceptors unmodifiable**) → 代理(**Plugin.wrap L44-52: getSignatureMap[@Intercepts 缺失抛 issue#251 L68-73]+getAllInterfaces[target 继承链收集 signatureMap 声明的接口 L89-100]**+无匹配接口直接返回原 target[不包装 L48-51]**; Proxy.newProxyInstance) → 分派(**Plugin.invoke L55-65: signatureMap.get(declaringClass) 命中且 contains→interceptor.intercept(new Invocation); 否则 method.invoke(target) 透传; ExceptionUtil.unwrapThrowable**) → 白名单(**Invocation 构造 L30-36: targetClasses=[Executor/ParameterHandler/ResultSetHandler/StatementHandler] — 拦截其他类的方法 invoke 时抛 "not supported as a plugin target"**[测试 shouldPluginNotInvokeArbitraryMethod]**) → 汇聚点(**Configuration L703-742: 4 工厂统一 interceptorChain.pluginAll[Executor L741/StatementHandler L721/ParameterHandler L707/ResultSetHandler L714] — M-1 已述**)
- 设计模式: [模式: 责任链(嵌套包装)+JDK 动态代理+白名单校验]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Interceptor.java:23-35 | 接口 | intercept 抽象; plugin 默认 Plugin.wrap; setProperties 默认 NOP — 实现只写拦截逻辑 | High |
| InterceptorChain.java:25-44 | 链 | ArrayList; pluginAll 循环 `target = interceptor.plugin(target)` — **后注册包外层**; unmodifiable 读取 | High |
| Plugin.java:44-52 | wrap | getSignatureMap→getAllInterfaces→**无匹配接口返回原 target**(L48-51 不包装) | High |
| Plugin.java:67-87 | 签名映射 | @Intercepts 缺失抛 PluginException(issue#251); Signature→Method 反射解析(getMethod(type,method,args)) | High |
| Plugin.java:89-100 | 接口收集 | getAllInterfaces: target 继承链(含父类)遍历, 收集 signatureMap 声明过的接口 | High |
| Plugin.java:55-65 | invoke | declaringClass 匹配+contains 双条件→intercept(new Invocation); 否则透传 method.invoke(target) | High |
| Invocation.java:30-36 | 白名单 | **targetClasses 4 类白名单**: Executor/ParameterHandler/ResultSetHandler/StatementHandler — 其他目标 invoke 时抛 | High |
| Invocation.java:52-56 | proceed | method.invoke(target, args) — 拦截器必须手动继续链 | High |
| Configuration.java:703-742 | 汇聚点 | 4 工厂统一 pluginAll(Executor L741/StatementHandler L721/ParameterHandler L707/ResultSetHandler L714) | High |
| PluginTest.java:75-100 | 用例 | 多租户 schema 切换(StatementHandler.prepare 拦截+ThreadLocal); 非法目标 Map.get 拦截抛 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 插件机制是小而精的单机制 — 1篇 (~60行) 按"接口契约→链包装顺序→代理分派→白名单→汇聚点"展开; 4 工厂汇聚点已在 M-1 展开(引用), 对照 spring-aop。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Interceptor 接口契约 + Signature 匹配 | 🔴 | **为什么🔴**: 扩展面定义 |
| P1-2 | pluginAll 嵌套顺序 (后注册包外层) | 🔴 | **为什么🔴**: 拦截链语义 |
| P1-3 | Plugin.wrap/invoke (无匹配不包装+双条件分派) | 🔴 | **为什么🔴**: 代理机制核心 |
| P1-4 | Invocation 4 类白名单 (运行期校验) | 🔴 | **为什么🔴**: 安全边界 |
| P2-1 | 4 工厂汇聚点 (导航 M-1) | 🟡 | **为什么🟡**: 拦截点面 |
| P2-2 | 用例: 多租户 schema 切换 | 🟡 | **为什么🟡**: 实战价值 |
| P3-1 | 与 spring-aop 对照 (s24-28) | 🟢 | **为什么🟢**: 方案对比 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **接口与匹配** | 🔴 | 扩展契约 |
| B | **链与代理** | 🔴 | 机制核心 |
| C | **白名单与汇聚** | 🔴 | 边界 |
| D | **对照与用例** | 🟡 | 应用面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 4 类白名单 | Invocation 构造校验 targetClasses=[Executor/ParameterHandler/ResultSetHandler/StatementHandler] — **拦截器只能拦这 4 类处理器**, 声明其他目标(M 类)时 wrap 成功但 invoke 时抛 "not supported as a plugin target"(测试 shouldPluginNotInvokeArbitraryMethod 拦截 Map.get 实证) | Invocation.java:30-36; 测试 PluginTest |
| q2 | 无匹配不包装 | Plugin.wrap: getAllInterfaces 收集 signatureMap 声明的接口(沿继承链), **无匹配接口直接返回原 target 不创建代理**(L48-51) — 拦截器对不相关对象零开销 | Plugin.java:44-52,89-100 |
| q3 | invoke 双条件 | 命中需 declaringClass 匹配+methods.contains(method) 双重条件; 未命中 method.invoke(target) 透传; 异常 unwrapThrowable | Plugin.java:55-65 |
| q4 | 嵌套顺序 | pluginAll 按注册顺序循环包装, **后注册的包在最外层**(先注册先被包) — 拦截执行顺序与注册顺序相反 | InterceptorChain.java:34-39 |
| q5 | 接口契约 | Interceptor 三方法: intercept 唯一抽象(拦截逻辑)+plugin 默认(Plugin.wrap, 可覆写)+setProperties 默认 NOP(配置注入) | Interceptor.java:23-35 |
| q6 | 汇聚点 | 4 工厂统一 `interceptorChain.pluginAll`(M-1 交付): newExecutor L741/newStatementHandler L721/newParameterHandler L707/newResultSetHandler L714 — 插件的全部挂载面 | Configuration.java:703-742 |
| q7 | 用例语义 | 多租户: StatementHandler.prepare 拦截改 Connection schema(ThreadLocal 租户名) — 插件在 prepare 时机改连接状态; proceed() 手动继续链(L52-56) | PluginTest.java:75-100; Invocation.java:52-56 |

→ 引出 MP 阶段回流: MyBatis 插件机制正是 MP 的 MybatisPlusInterceptor/InnerInterceptor 的宿主 (MP-4, 导航); 对照 spring-aop (s24-28, 配置式切面 vs 声明式拦截)。
