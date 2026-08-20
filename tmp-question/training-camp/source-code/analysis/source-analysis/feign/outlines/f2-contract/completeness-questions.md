# F-2 契约解析 — completeness-questions (全视角提问验证, 5 身份)

## 开发者视角

1. parseAndValidateMetadata 的入口签名? BaseContract 过滤哪些方法?
2. 参数角色怎么分配? 未消费参数为什么推断为 body?
3. @Param 没写 value 名字从哪来? 空名报错长什么样?
4. formParams 怎么推断? 什么条件加入?
5. @Body 含 { 和不含 { 分别走什么路径?
6. DeclarativeContract 怎么注册自定义注解处理器?
7. MethodInfoResolver 返回什么? CompletableFuture 怎么剥离?

## 架构师视角

8. BaseContract 为什么把 body 推断留在基类? 各 Contract 一致性?
9. 13.x 注册表 vs 旧版 if/else: 为什么是架构升级?
10. 未识别注解 warning 的动机? 静默忽略的问题?
11. processAnnotationsOnParameter 固定返回 false 的语义变化?
12. configKey 去重 + 协变合并解决什么 (泛型桥接)?
13. MethodInfo 抽象让同步/异步/协程共用元数据的设计?
14. Types.resolve 的消解循环怎么处理 TypeVariable/ParameterizedType?
15. 类级 @Headers 合并 (先父后自身) 的继承语义?

## SRE/运维视角

16. 注解没生效怎么排查? warning 日志特征?
17. -parameters 编译参数对 @Param 的影响?
18. "didn't start with an HTTP verb" 什么时候触发?
19. 契约解析失败对运行时的表现?

## 研究者视角

20. vs Retrofit: 注解解析架构对比 (ServiceMethod vs MethodMetadata)?
21. vs JAX-RS: ClientBuilder 的注解处理方式?
22. DeclarativeContract 与 SPI/插件模式的异同?
23. 泛型消解移植自 Retrofit 的演化?
24. 13.x 异步抽象与 Kotlin 协程的关系?

## 学生视角

25. 什么是契约? 为什么叫 contract?
26. 什么是注解处理? @RequestLine 怎么被"读"?
27. 什么是泛型消解? List<String> 为什么需要解析?
28. 什么是 body? 参数怎么变成请求体?
29. 什么是 form 参数? 和 query 参数区别?
