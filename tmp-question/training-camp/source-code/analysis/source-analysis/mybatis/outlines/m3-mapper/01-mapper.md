# M-3 Mapper 代理 — Registry→ProxyFactory→Proxy→Method 四层 + 参数命名

> 前置: [[M-1-configuration]] (StrictMap/Configuration) | 复用: [[M-2-executor]] (SqlSession 消费) | 对照: [[s24-aop-proxy]] (Spring AOP 代理 vs MyBatis 代理) | 引出: [[M-5-mapping]] [[MP-1-sql-injector]]
> 🔴 Deep | 8 KP | [模式: 注册表+工厂+JDK 动态代理+命令分发+缓存]
> Pass 2 闭环: q1(注册时序) q2(代理分层) q3(default 方法) q4(execute 分发) q5(SqlCommand 绑定) q6(参数命名三态) q7(ParamMap 严格) q8(特殊参数) q9(注解装配)

**读者处境**: `userMapper.selectById(1)` — 一个接口方法怎么变成 SQL 执行?为什么每个 SqlSession 拿到的 mapper 都是新代理但方法解析只做一次?为什么 mapper 接口方法没绑定 XML/注解时抛 "Invalid bound statement (not found)"?多参数时 #{id} 里的 id 从哪来?这篇拆注册→代理→分发→参数命名的完整链路。

### 1. 注册 — MapperRegistry 先 put 后 parse + 失败回滚

场景: addMapper 为什么先把接口放进 knownMappers 再解析注解?解析失败会怎样?
源码路径:
- `MapperRegistry.java:37,44-54` — knownMappers ConcurrentHashMap; getMapper: 未注册抛 "not known"; mapperProxyFactory.newInstance(sqlSession)
- `MapperRegistry.java:60-80` — addMapper(q1): 接口校验+重复抛 "already known"; **先 knownMappers.put 再 MapperAnnotationBuilder.parse**(L67-72 注释: "added before the parser is run" 防解析期间自触发绑定); finally loadCompleted 失败回滚移除
- `MapperRegistry.java:103-122` — addMappers 包扫描: ResolverUtil.IsA(superType)+**VFS.list 单层**(M-1 负面空间已述)
关键设计: 先 put 后 parse(q1): 注解解析过程中若内部触发 getMapper 绑定(循环引用), 已注册的接口不会再次解析; 失败回滚保证注册表无半成品 — 与 M-1 StrictMap 的"无半成品"哲学一致。[模式: 注册表+时序控制]
数据流: Configuration.addMapper → registry.addMapper: 校验→put→parse(注解/XML)→成功标记; 失败→remove。

### 2. 工厂与代理 — MapperProxyFactory + MapperProxy 三层分派

场景: 为什么每个会话的 mapper 是新实例但方法解析只一次?toString() 会被拦截吗?接口 default 方法怎么执行?
源码路径:
- `MapperProxyFactory.java:35-56` — **methodCache 跨会话共享**(构造传入); newInstance(proxy): `Proxy.newProxyInstance(loader, {mapperInterface}, mapperProxy)`(L45-48); newInstance(sqlSession): 每会话新代理绑定 sqlSession(L51-54)
- `MapperProxy.java:81-90` — invoke: **Object 方法(toString/equals/hashCode)直接 method.invoke(this)**(L83-84, 不拦截); 其余 cachedInvoker(method).invoke
- `MapperProxy.java:92-112` — cachedInvoker: methodCache computeIfAbsent — **非 default→PlainMethodInvoker(new MapperMethod); default→DefaultMethodInvoker(MethodHandle)**
- `MapperProxy.java:54-78,114-126` — default 方法 JDK 双兼容(q3): JDK9 `MethodHandles.privateLookupIn`(L57,L114-120); JDK8 `Lookup(Class,int)` 反射构造+unreflectSpecial(L67,L122-126)
关键设计: 三层(q2): 工厂(创建)+代理(分派)+invoker(执行策略); Object 方法直通防递归(toString 不触发 SQL); default 方法走 MethodHandle(q3) — 默认方法有 Java 实现, 不是 SQL 语句, 直接执行; methodCache 跨会话共享 = 方法→MapperMethod 映射全局一份。[模式: 工厂+JDK 动态代理+策略]
数据流: getMapper → factory.newInstance(sqlSession) → Proxy.newProxyInstance → invoke(method) → Object 方法? 直通 : cachedInvoker → Plain/DefaultMethodInvoker。

### 3. 命令解析 — SqlCommand: statementId 规则与父接口递归

场景: "Invalid bound statement (not found)" 什么时候抛?继承的 mapper 接口怎么绑定?
源码路径:
- `MapperMethod.java:222-240` — SqlCommand 构造: `resolveMappedStatement(mapperInterface, methodName, declaringClass, config)`; ms==null 且无 @Flush → **抛 "Invalid bound statement (not found)"**(L226-230); @Flush → FLUSH 类型; UNKNOWN → 抛
- `MapperMethod.java:250-268` — statementId=`mapperInterface.getName()+"."+methodName`(L252); `configuration.hasStatement`(M-1 StrictMap); **父接口递归**: mapperInterface.getInterfaces() 中 declaringClass 可赋值的 superInterface 递归 resolveMappedStatement(L259-266)
关键设计: 绑定规则(q5): 接口全名.方法名 = statementId — MP 注入器正是在这个 id 空间注册(导航 MP-1); 父接口递归让继承的 mapper 方法共享 statement; 未绑定报错在**第一次方法调用**时抛(懒绑定)。[模式: 命名约定+递归查找]
数据流: execute → command.getName() → resolveMappedStatement: 自身 hasStatement→父接口递归→null → @Flush? FLUSH : 抛经典报错。

### 4. 方法分发 — MapperMethod.execute 六分支 + rowCountResult

场景: mapper 方法返回 int/boolean/List/Optional 时分别怎么处理?返回 null 会怎样?
源码路径:
- `MapperMethod.java:57-104` — execute(q4): INSERT/UPDATE/DELETE→`convertArgsToSqlCommandParam`→sqlSession.insert/update/delete→rowCountResult; SELECT 五分支: ①returnsVoid+hasResultHandler→executeWithResultHandler ②returnsMany→executeForMany ③returnsMap→executeForMap ④returnsCursor→executeForCursor ⑤单值 selectOne+**returnsOptional→Optional.ofNullable**(L88-90); FLUSH→flushStatements
- `MapperMethod.java:99-102` — **primitive 返回类型 + null → 抛 BindingException**
- `MapperMethod.java:106-121` — rowCountResult: void→null; Integer/int→rowCount; Long/long→(long); Boolean/boolean→`rowCount > 0`; 其他→抛 "unsupported return type"
- `MapperMethod.java:140-157,171-189` — executeForMany: List 结果 → 返回类型数组 convertToArray/声明集合 convertToDeclaredCollection(issue#510)
关键设计: 返回类型驱动的分发(q4): 方法签名在 MethodSignature 一次性解析, execute 按签名分支 — void+Handler 组合/List/Map(@MapKey)/Cursor/Optional 全语义覆盖; rowCount 的 int→布尔转换是 MyBatis 的特色便利(update 返回 boolean=影响行数>0)。[模式: 命令分发+签名驱动]
数据流: execute → switch type → SELECT: returnsVoid+Handler? executeWithResultHandler : returnsMany? executeForMany : returnsMap? executeForMap : returnsCursor? executeForCursor : selectOne(+Optional 包装)。

### 5. 签名解析 — MethodSignature 泛型返回 + 特殊参数唯一索引

场景: List<User> 的泛型怎么解析?RowBounds 和 ResultHandler 可以出现多次吗?
源码路径:
- `MapperMethod.java:284-302` — MethodSignature 构造(q8): `TypeParameterResolver.resolveReturnType(method, mapperInterface)`(L285 — 泛型实参解析); returnsMany=isCollection||isArray(L294); returnsCursor/returnsOptional; mapKey=@MapKey(L297,L374-383); rowBoundsIndex/resultHandlerIndex=`getUniqueParamIndex`(L299-300)
- `MapperMethod.java:355-368` — getUniqueParamIndex: 同类型特殊参数多个 → 抛 "cannot have multiple ... parameters"
- `MapperMethod.java:123-138` — executeWithResultHandler: 非 CALLABLE 且结果类型 void → 抛 "needs either a @ResultMap ... or resultType"(ResultHandler 无法知道目标类型)
关键设计: 签名面(q8): 泛型返回经 TypeParameterResolver 解析到实参(子类泛型场景); RowBounds/ResultHandler 是"框架参数" — 不参与 SQL 参数命名且每方法最多一个; ResultHandler 模式强制要求结果类型可推断。[模式: 签名预解析]
数据流: MapperMethod 构造 → SqlCommand + MethodSignature(泛型解析/索引/命名器) → execute 全程用签名判定。

### 6. 参数命名 — ParamNameResolver 三态与 param1..N

场景: 多参数时 #{id} 的 id 从哪来?为什么还能用 #{param1}?单个 List 参数怎么进 foreach?
源码路径:
- `ParamNameResolver.java:40-101` — 构造: sorted TreeMap; **@Param 值 > 实际参数名(useActualParamName, -parameters 编译)> 索引名 "0","1"**(gcode#71, L63-95); RowBounds/ResultHandler 特殊参跳过(L97-101)
- `ParamNameResolver.java:110-146` — getNamedParams 三态(q6): ①args null 或 0 参→null ②**单非特殊参且无 @Param→裸值**+`wrapToMapIfCollection`(集合→collection/list 键, 数组→array) ③多参→ParamMap: 具名+**param1..N 通用名**(L132-135 不覆盖 @Param 名)
- `MapperMethod.java:203-215` — **ParamMap.get 缺失抛 "Parameter 'x' not found. Available parameters are ..."**(q7)
关键设计: 命名优先级(q6): @Param > 编译参数名 > 索引 — 生产强制 @Param(索引名在无 -parameters 时易错); 单参裸值简化(#{value} 或直接属性访问); param1..N 兜底让无 @Param 的多参也能用 #{param1}; foreach collection="list" 正是单 List 参数裸值包装的结果。[模式: 命名优先级链+严格 Map]
数据流: getNamedParams(args) → 0 参? null : 单参无注解? 裸值(集合包装) : ParamMap(具名+paramN) → 传给 sqlSession.selectList/update → 参数化(#{id}→ParamMap.get)。

### 7. 注解装配 — MapperAnnotationBuilder 双源路径

场景: 接口上既有注解又有同名 XML, 哪个生效?
源码路径:
- `MapperAnnotationBuilder.java:114-143` — parse: isResourceLoaded 幂等→**loadXmlResource(同名 XML 优先, "namespace:"+类型名标志防重复加载 L145-148 + 双路径资源查找 #1347)**→addLoadedResource→parseCache/parseCacheRef→逐方法: **canHaveStatement 过滤(!isBridge && !isDefault, L140-143 issue#237)**→@Select 且无 @ResultMap→parseResultMap→parseStatement(**IncompleteElementException→addIncompleteMethod 延迟**, M-1 机制)
关键设计: 双源合一(q9): 同名 XML 优先(loadXmlResource 先解析), 注解补充 — 混合使用; 语句解析失败入 incomplete 延迟重试, 与 M-1 buildAllStatements 联动。[模式: 双源装配+延迟解析]
数据流: addMapper → parse → loadXmlResource → parseCache/Ref → 逐方法 parseStatement → addMappedStatement 或 addIncompleteMethod。

### 负面空间 — 代理层刻意不做的事

- **不做 AOP 增强**: MyBatis 代理只做"接口→SqlSession"转发, 无切面能力 — 插件拦截在 Executor 层(M-4), 与 Spring AOP 职责分离(对照 s24)
- **不做多接口代理**: 代理实现单接口(mapperInterface), 不支持多接口合并
- **不做运行时方法级校验**: default/static/Object 方法在 canHaveStatement 过滤, 但错误绑定只在使用时报错(懒绑定)

→ 引出: 参数命名产物(paramMap/裸值)是 M-5 DefaultParameterHandler 的取参输入; M-1 StrictMap 的 hasStatement 是 SqlCommand 查找基础; MP 注入器在 statementId 空间注册(导航 MP-1) → [[M-5-mapping]] [[MP-1-sql-injector]]
