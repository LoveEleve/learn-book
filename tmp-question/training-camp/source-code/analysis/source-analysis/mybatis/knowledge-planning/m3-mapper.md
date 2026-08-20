# M-3 Mapper 代理 — Registry→ProxyFactory→Proxy→Method 四层 + 参数命名

> 项目: MyBatis | 🔴 Deep / 1 篇 | MapperRegistry(124)+MapperProxyFactory(56)+MapperProxy(157)+MapperMethod(386)+ParamNameResolver(177)+MapperAnnotationBuilder(706)
> 基线: M-PLAN M-3 — 前置: **M-1 (StrictMap/Configuration) + M-2 (SqlSession 消费)** — 展开 注册→代理→方法分发→参数命名

---

## §0.8

- 🔴 Deep，1篇 — 注册(**MapperRegistry: knownMappers ConcurrentHashMap L37; getMapper L44-54[未注册抛 "not known"; 每会话 newInstance]; addMapper L60-80[接口校验+重复抛+**先 put 再 parse** L67-72[防 mapper 解析器自触发绑定]+finally 失败回滚移除]; addMappers 包扫描 L103-122[ResolverUtil.IsA]**) → 工厂(**MapperProxyFactory L35-56: methodCache ConcurrentHashMap **跨会话共享**; newInstance(proxy) Proxy.newProxyInstance L45-48; newInstance(sqlSession) 每会话新代理绑定 L51-54**) → 代理(**MapperProxy L37-157: invoke L81-90[Object 方法直通 L83-84]; cachedInvoker L92-112[methodCache computeIfAbsent; 非 default→PlainMethodInvoker[MapperMethod]; **default→DefaultMethodInvoker[MethodHandle: JDK9 privateLookupIn L57/L114-120, JDK8 Lookup 反射构造 L67/L122-126 双兼容]**]**) → 方法分发(**MapperMethod.execute L57-104: SqlCommandType 分发[INSERT/UPDATE/DELETE→rowCountResult[void/Integer/Long/Boolean 转换, 其他抛 L106-121]; SELECT 五分支[ResultHandler+void/List/Map/流/单值+Optional L75-92]; FLUSH]; primitive null 检查 L99-102; executeForMany List→数组/声明集合转换 L140-157,171-189 issue#510; executeForMap L191-201; executeWithResultHandler resultMap/resultType 校验 L123-138**) → 命令解析(**SqlCommand L217-269: statementId=接口名.方法名 L252; hasStatement 查找[StrictMap]; 未找且无 @Flush→"Invalid bound statement (not found)" L226-230[MP 经典报错]; **父接口递归 resolveMappedStatement L250-268**; UNKNOWN 类型抛**) → 签名解析(**MethodSignature L271-384: TypeParameterResolver 泛型返回 L285-292; returnsMany/returnsMap[@MapKey L374-383]/returnsVoid/returnsCursor/returnsOptional; rowBounds/resultHandler 唯一索引[多个抛 L355-368]**) → 参数命名(**ParamNameResolver L40-160: 构造 sorted map[@Param 值>实际参数名(useActualParamName)>索引名 "0","1" gcode#71 L63-95; RowBounds/ResultHandler 特殊参跳过 L97-101]; getNamedParams 三态 L110-146[无参→null; 单非特殊参无 @Param→裸值+wrapToMapIfCollection; 多参→ParamMap 具名+param1..N 通用名[不覆盖 @Param L132-135]]; ParamMap.get 缺失抛 "Parameter not found. Available parameters" L203-215**) → 注解装配(**MapperAnnotationBuilder.parse L114-143: isResourceLoaded 幂等→loadXmlResource[同名 XML 优先]→parseCache/parseCacheRef→逐方法[canHaveStatement 过滤(!isBridge && !isDefault L140-143 issue#237); @Select 且无 @ResultMap→parseResultMap; parseStatement 失败→addIncompleteMethod 延迟]→parsePendingMethods**)
- 设计模式: [模式: 注册表+工厂+JDK 动态代理+命令分发+缓存]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MapperRegistry.java:44-54 | getMapper | 未注册抛 "not known"; mapperProxyFactory.newInstance(sqlSession) — 每会话新代理 | High |
| MapperRegistry.java:60-80 | addMapper | 接口校验+重复抛; **先 put 后 parse**(L67-72, 防 mapper 解析器自触发); finally 失败回滚移除(loadCompleted) | High |
| MapperProxyFactory.java:35-56 | 工厂 | methodCache **跨会话共享**(构造传入); newInstance: Proxy.newProxyInstance 单接口代理 | High |
| MapperProxy.java:81-90 | invoke | Object 方法(toString/equals/hashCode)直通 method.invoke(this); 其余 cachedInvoker | High |
| MapperProxy.java:92-112 | invoker 缓存 | methodCache computeIfAbsent: 非 default→PlainMethodInvoker(MapperMethod); default→DefaultMethodInvoker(MethodHandle) | High |
| MapperProxy.java:54-78,114-126 | JDK 兼容 | JDK9 privateLookupIn 反射检测; JDK8 Lookup(Class,int) 构造+unreflectSpecial — 双版本兼容 | High |
| MapperMethod.java:57-104 | execute | SqlCommandType 分发; SELECT 五分支(Handler/List/Map/Cursor/单值+Optional); primitive null 抛(L99-102) | High |
| MapperMethod.java:106-121 | rowCount | rowCountResult: void null/Integer/int/Long/long/Boolean/boolean(rowCount>0)/其他抛 | High |
| MapperMethod.java:250-268 | SqlCommand | statementId=接口名.方法名; 未找且无 @Flush→"Invalid bound statement (not found)"; **父接口递归解析** | High |
| MapperMethod.java:284-302 | 签名 | TypeParameterResolver 泛型返回; returnsMany/Map/Cursor/Optional; rowBounds/resultHandler 唯一索引(多个抛) | High |
| ParamNameResolver.java:40-101 | 命名 | sorted map: @Param>实际参数名>索引名(gcode#71); RowBounds/ResultHandler 特殊参跳过 | High |
| ParamNameResolver.java:110-146 | 三态 | 无参→null; 单非特殊无 @Param→裸值+wrapToMapIfCollection; 多参→ParamMap(具名+param1..N, 不覆盖 @Param) | High |
| MapperMethod.java:203-215 | ParamMap | get 缺失抛 "Parameter 'x' not found. Available parameters are ..." | High |
| MapperAnnotationBuilder.java:114-143 | 注解装配 | 幂等→loadXmlResource(XML 优先)→parseCache/CacheRef→逐方法 parseStatement(失败→addIncompleteMethod) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Mapper 代理是单链路 — 1篇 (~70行) 按"注册→工厂→代理→分发→命令→签名→参数命名→注解装配"展开; M-1 StrictMap 关联/M-2 SqlSession 消费导航。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Registry 注册时序 (先 put 后 parse+失败回滚) | 🔴 | **为什么🔴**: 幂等与并发 |
| P1-2 | 代理三层 (invoke 分派/invoker 缓存/default 方法 MethodHandle) | 🔴 | **为什么🔴**: 代理机制核心 |
| P1-3 | MapperMethod 分发 (五分支+rowCount+primitive 检查) | 🔴 | **为什么🔴**: 方法语义面 |
| P1-4 | SqlCommand 解析 (id 规则+父接口递归+经典报错) | 🔴 | **为什么🔴**: 绑定关系 |
| P1-5 | ParamNameResolver 命名三态+param1..N | 🔴 | **为什么🔴**: 参数绑定基础 |
| P2-1 | MethodSignature (泛型返回/特殊参唯一索引) | 🟡 | **为什么🟡**: 签名面 |
| P2-2 | MapperAnnotationBuilder 注解装配 (XML 优先) | 🟡 | **为什么🟡**: 注解路径 |
| P3-1 | 与 M-1 StrictMap/M-2 SqlSession 协作 (导航) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **注册与代理** | 🔴 | 机制核心 |
| B | **方法与命令** | 🔴 | 语义面 |
| C | **参数命名** | 🔴 | 绑定基础 |
| D | **签名与注解** | 🟡 | 支撑面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 注册时序 | addMapper **先 knownMappers.put 再 MapperAnnotationBuilder.parse**(L67-72 注释 "added before the parser is run") — 防注解解析期间的自触发绑定(parse 里 getMapper 可能回查); finally 失败回滚移除 | MapperRegistry.java:60-80 |
| q2 | 代理分层 | 三层: MapperProxyFactory(创建)+MapperProxy(InvocationHandler 分派)+MapperMethodInvoker(执行策略); Object 方法直通不拦; methodCache 跨会话共享(每会话仅新代理, 方法解析一次) | MapperProxyFactory.java:35-56; MapperProxy.java:81-112 |
| q3 | default 方法 | 接口 default 方法走 **MethodHandle**(JDK9 privateLookupIn L57 / JDK8 Lookup(Class,int) L67 双兼容)而非 MapperMethod — 默认方法不是 SQL 语句, 直接执行 Java 逻辑 | MapperProxy.java:54-78,92-126 |
| q4 | execute 分发 | SqlCommandType 六分支: INSERT/UPDATE/DELETE→rowCountResult(int 语义转换); SELECT→五分支(ResultHandler+void/List/Map/Cursor/单值+Optional); FLUSH; primitive 返回 null 抛 BindingException | MapperMethod.java:57-104 |
| q5 | SqlCommand 绑定 | statementId=接口全名.方法名; hasStatement 经 M-1 StrictMap; 未绑定且无 @Flush → **"Invalid bound statement (not found)"**(MP 集成最常见的报错根源); **父接口方法递归查找**(继承 mapper 接口共享 statement) | MapperMethod.java:222-268 |
| q6 | 参数命名三态 | ①无参→null ②单非特殊参且无 @Param→裸值(集合/数组 wrapToMapIfCollection: collection/list/array 键) ③多参→ParamMap: @Param 名/实际参数名/索引名("0","1" gcode#71) + **param1..N 通用名**(不覆盖 @Param 名) | ParamNameResolver.java:63-95,110-146 |
| q7 | ParamMap 严格 | 多参模式用 ParamMap(HashMap 子类): get 缺失 key 抛 "Parameter 'x' not found. Available parameters are ..." — 拼错参数名立刻暴露 | MapperMethod.java:203-215 |
| q8 | 特殊参数 | RowBounds/ResultHandler 在 ParamNameResolver 跳过命名(L97-101), 在 MethodSignature 唯一索引(多个抛 "cannot have multiple") — 不入 SQL 参数 | ParamNameResolver.java:97-101; MapperMethod.java:355-368 |
| q9 | 注解装配 | MapperAnnotationBuilder.parse: 幂等→**loadXmlResource(同名 XML 优先于注解)**→parseCache/CacheRef→逐方法 parseStatement(IncompleteElementException→addIncompleteMethod 延迟 M-1 机制) | MapperAnnotationBuilder.java:114-143 |

→ 引出 M-5: 参数命名产物(paramMap/裸值)是 DefaultParameterHandler 取参输入; M-1 的 StrictMap hasStatement 是 SqlCommand 查找基础。
