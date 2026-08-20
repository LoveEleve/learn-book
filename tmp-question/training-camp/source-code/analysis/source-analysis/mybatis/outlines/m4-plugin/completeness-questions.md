# M-4 插件机制 — completeness-questions

## 开发者视角

1. 实现一个 MyBatis 插件最少要做什么?@Intercepts 和 @Signature 分别干什么?
2. @Signature 的 args 写错(参数类型不匹配)会怎样?什么时候报错?
3. 插件能拦截任意类的任意方法吗?为什么?
4. intercept 里不调用 invocation.proceed() 会怎样?前置/后置/短路三种模式怎么写?
5. 多个插件注册后执行顺序是什么?为什么后注册的先执行?
6. 插件如何修改 SQL?以分页插件为例, 应该在哪个方法拦截?
7. 拦截器里怎么拿到/修改参数和返回值?
8. 没有匹配接口时 Plugin.wrap 返回什么?会不会产生不必要的代理?

## 架构师视角

9. Invocation 的 4 类白名单(Executor/ParameterHandler/ResultSetHandler/StatementHandler)设计意图是什么?为什么 wrap 不校验而 invoke 时才校验?
10. InterceptorChain.pluginAll 的洋葱嵌套(后注册包外层)对拦截器执行顺序意味着什么?和 Druid Filter 链(递归下推)的差异(对照 D-2)?
11. Plugin.invoke 的双条件分派(declaringClass+contains)为什么需要声明类匹配?接口继承场景会怎样?
12. 4 工厂统一 pluginAll(Configuration L707/714/721/741)的汇聚点设计 — 为什么插件的挂载面能如此集中?
13. 拦截器的代理链和被代理对象的 setExecutorWrapper(M-2)如何协作?插件看到的 Executor 是什么?
14. 与 Spring AOP(切点表达式+环绕通知)对比, MyBatis 声明式拦截的优缺点?

## 学生视角

15. 一次带插件的 SQL 执行: 从 newStatementHandler 到 prepare 被拦截的完整调用链?
16. Signature 的反射解析(getMethod(type, method, args))失败抛什么?在哪一步?
17. proceed() 的语义是什么?和 Druid Filter 的 chain.xxx 调用有什么异同(对照 D-2)?
18. 多租户 schema 切换插件为什么选 prepare 时机?还有其他可选时机吗?
19. getSignatureMap/getAllInterfaces 在 wrap 时各做什么?为什么 getAllInterfaces 要遍历继承链?
20. ExceptionUtil.unwrapThrowable 解决什么问题?插件抛的异常怎么传播?
