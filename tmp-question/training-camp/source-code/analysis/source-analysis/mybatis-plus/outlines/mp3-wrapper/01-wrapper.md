# MP-3 Lambda 条件构造器 — 方法引用反序列化取列名 + AbstractWrapper 条件段构建

> 前置: [[MP-2-metadata]] (ColumnCache/LambdaUtils.installCache) | 复用: [[MP-1-injector]] (语句消费) | 对照: [[M-6-scripting]] (SQL 段生成 vs OGNL) | 引出: [[MP-4-plugin]]
> 🔴 Deep | 8 KP | [模式: 序列化反序列化取元数据+构建器+段列表]
> Pass 2 闭环: q1(反序列化黑魔法) q2(三路提取) q3(列名解析链) q4(条件构建) q5(段列表语义) q6(嵌套条件) q7(查询字段) q8(链式终点)

**读者处境**: `lambdaQuery().eq(User::getName, "张三").list()` — 一个方法引用 `User::getName` 怎么变成数据库列名 `name`?序列化机制怎么被"黑魔法"用来反射?为什么 eq().or().eq() 拼出的 SQL 没有多余 AND/OR?条件参数为什么叫 MPGENVAL1?这篇拆条件构造器的完整链路: 方法引用→列名→条件段→SQL 片段。

### 1. 方法引用黑魔法 — SerializedLambda 反序列化 + resolveClass 重写

场景: 一个方法引用怎么拿到它的类和方法名?序列化机制怎么当反射用?
源码路径:
- `SerializedLambda.java:42-60` — extract(q1): 序列化 lambda(ByteArrayOutputStream+ObjectOutputStream)→反序列化时 **ObjectInputStream.resolveClass 重写: `java.lang.invoke.SerializedLambda`→MP 镜像类**(L47-52) — JVM 序列化机制当反射工具用
- `SerializedLambda.java:31-40` — 镜像类字段: capturingClass/implClass/**implMethodName**/instantiatedMethodType 等(与 JDK SerializedLambda 同构)
- `ReflectLambdaMeta.java` — getImplMethodName(方法名 getUserName)/getInstantiatedClass(**从 instantiatedMethodType 解析实体类**, 如 Ljava/entity/User;→User)
关键设计: 黑魔法(q1): lambda 实现 Serializable 时 JVM 会写 writeReplace 产 SerializedLambda — MP 序列化走一遍拿到方法名+实体类; resolveClass 重写是**镜像替换**: JDK 的 SerializedLambda 属 java.base 模块(JDK16+ 强封装下反序列化受限), MP 用自己的同构类接收并直接读字段。[模式: 序列化反射]
数据流: User::getName → writeReplace → 序列化字节 → 反序列化(resolveClass→镜像) → implMethodName="getName" + instantiatedMethodType→User。

### 2. 三路提取 — LambdaUtils.extract 降级链

场景: 为什么有三种方式拿 LambdaMeta?IDEA 调试会怎样?
源码路径:
- `LambdaUtils.java:50-62` — extract(q2): ①**IDEA 调试模式下 lambda 是 Proxy**→IdeaProxyLambdaMeta ②反射调用私有 `writeReplace`→ReflectLambdaMeta(主流路径) ③反射失败→序列化兜底 ShadowLambdaMeta(SerializedLambda.extract)
- `LambdaMeta.java` — 统一接口: getInstantiatedMethodType/getImplMethodName/getInstantiatedClass
关键设计: 降级链(q2): 调试器会包装 lambda(代理), 反射 readObject 失败等环境差异都有对应路径 — 三种实现同一接口, 上层无感。[模式: 策略降级]
数据流: extract(func) → Proxy? Idea : writeReplace 反射成功? Reflect : Shadow(序列化)。

### 3. 列名解析链 — methodToProperty → ColumnCache

场景: getUserName 怎么变成 name 或 user_name?为什么不用字符串硬编码?
源码路径:
- `AbstractLambdaWrapper.java:127-133` — getColumnCache(q3): `LambdaUtils.extract`→**`PropertyNamer.methodToProperty("getUserName")→"userName"`**→tryInitCache(首次取实体类)→`getColumnCache(fieldName, instantiatedClass)`
- `LambdaUtils.java:96-110` — **formatKey(property) 全大写**→COLUMN_CACHE_MAP(MP-2 installCache 产物: ColumnCache[column/columnSelect/mapping])
- `ColumnCache.java` — column(裸列名)/columnSelect(带 AS 的查询列, MP-2 决策)/mapping
关键设计: 全链路无硬编码(q3): 方法引用→方法名→属性名→(大写键)→列缓存 — 列名唯一来源是 MP-2 的 TableInfo 解析; 属性名与列名解耦(userName→user_name 或自定义 @TableField)。[模式: 元数据驱动]
数据流: User::getName → implMethodName "getName" → methodToProperty "userName" → formatKey "USERNAME" → ColumnCache.column "user_name"。

### 4. 条件构建 — addCondition + MPGENVAL 参数命名

场景: eq 的条件参数怎么进 SQL?条件开关 boolean 是什么?
源码路径:
- `AbstractWrapper.java:467-470` — addCondition(q4): `maybeDo(condition)`(boolean 守卫, 条件为 false 整段跳过)→`appendSqlSegments(columnToSqlSegment, SqlKeyword, formatParam)`
- `AbstractWrapperTest.java:18-28` — 参数命名实证: **`c=#{ew.paramNameValuePairs.MPGENVAL1}`** — MPGENVAL 序列命名+javaType/jdbcType/typeHandler 后缀
- `AbstractWrapper.java:624-625` — getSqlSegment: `expression.getSqlSegment() + lastSql`
关键设计: 统一条件入口(q4): eq/ne/gt/ge/lt/le/like/in 等全走 addCondition — 条件守卫+段追加+参数命名三合一; **参数全部以 ew.paramNameValuePairs.MPGENVALn 引用**(MP-1 注入 SQL 里 #{ew.paramNameValuePairs.xxx} 的取参来源)。[模式: 统一入口+守卫]
数据流: eq(true, User::getName, "张") → addCondition → columnToSqlSegment(name) + EQ(=) + formatParam(MPGENVAL1→"张") → NormalSegmentList。

### 5. 段列表语义 — MergeSegments 四段 + NormalSegmentList 净化

场景: eq().or().eq() 的 SQL 里第一个 eq 前为什么没有多余 AND?连续 or 会怎样?
源码路径:
- `MergeSegments.java:33-71` — 四段(q5): normal(条件)/groupBy/having/orderBy 各自 SegmentList; getSqlSegment 拼接: `normal + groupBy + having + orderBy`(L68-71)
- `NormalSegmentList.java:30-60` — transformList 净化(q5): **首段 and/or 不执行**(list.size()==1 且首位连接词→isEmpty 时返回 false 丢弃); **相邻同类合并**(上次 AND 这次 AND→false 丢弃; AND 后 OR→保留); not 语义处理
关键设计: 段净化(q5): 用户任意书写 eq().or().eq()/and(...) 组合, 前导连接词与冗余连接词被 transformList 过滤 — 保证拼出的 SQL 永远合法; 四段分离支持 groupBy/orderBy 独立追加。[模式: 段列表+净化]
数据流: appendSqlSegments → 按类型入段(normal/groupBy/orderBy) → getSqlSegment → transformList 净化 → 拼接。

### 6. 嵌套与查询字段 — and/or Consumer 括号 + select 族

场景: (a OR b) AND c 怎么表达?select 怎么过滤列?
源码路径:
- `AbstractWrapper.java:218-234` — and/or/nested(q6): `addNestedCondition(condition, consumer)`: 子包装实例化→consumer.accept(填充条件)→作为 **APPLY 段整体追加**(括号语义)
- `LambdaQueryWrapper.java:50-145` — select 族(q7): select(SFunction... 列集合)/select(Class, Predicate<TableFieldInfo>)(**谓词过滤→chooseSelect, MP-2 衔接**); 全参构造共享 paramNameSeq/paramNameValuePairs/MergeSegments
关键设计: 嵌套(q6): Consumer 回调在子实例上执行 — 括号组内的条件独立成段; select 双模式(q7): 显式列集合或 TableFieldInfo 谓词(与 MP-2 chooseSelect 联动)。[模式: 回调嵌套+谓词选择]
数据流: and(c -> c.eq(a).or().eq(b)) → 子实例填充 → APPLY 段(整体括号) → MergeSegments。

### 7. 链式终点 — LambdaQueryChainWrapper 衔接

场景: lambdaQuery().eq().list() 最后调谁?
源码路径:
- `LambdaQueryChainWrapper.java` — 持有 baseMapper; list()/one()/page() → `baseMapper.selectList(this)`(q8, MP-9 衔接)
- `IService.lambdaQuery` → ChainWrappers.lambdaQueryChain(MP-9 已述)
关键设计: 链式终点(q8): wrapper 本身是查询参数, 链方法最终回调 baseMapper 的注入语句 — 条件构造器与执行层的唯一接口。[模式: 链式调用]
数据流: lambdaQuery() → .eq(...) → .list() → baseMapper.selectList(wrapper) → MP-1 注入语句 → M-2 执行。

### 负面空间 — 条件构造器刻意不做的事

- **不做任意字符串拼接**: 常规列名必须经方法引用+ColumnCache; **唯一例外 apply(condition, applySql, values)(L243-248) 提供受控片段 — 值须 {} 占位参数化(formatSqlMaybeWithParam), 结构部分仍由用户负责**
- **不做执行**: wrapper 只构建条件段, 不触发查询(链式终点在 baseMapper)
- **不做多表 join**: 条件构造器面向单实体, join 场景走自定义 SQL/XML

→ 引出: 条件构造器产物(ew.paramNameValuePairs)是注入 SQL 的取参来源; MP-4 插件在 Executor 层与 Wrapper 联动(分页改写) → [[MP-4-plugin]]
