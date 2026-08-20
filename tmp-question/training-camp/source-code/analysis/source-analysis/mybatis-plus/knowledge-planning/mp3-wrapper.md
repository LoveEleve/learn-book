# MP-3 Lambda 条件构造器 — 方法引用反序列化取列名 + AbstractWrapper 条件段构建

> 项目: MyBatis-Plus | 🔴 Deep / 1 篇 | AbstractWrapper(712)+AbstractLambdaWrapper(127+)+LambdaQueryWrapper(145)+QueryWrapper(153)+SerializedLambda(73)+LambdaUtils(118+)+ColumnCache+MergeSegments+NormalSegmentList
> 基线: MP-PLAN MP-3 — 前置: **MP-2 (LambdaUtils.installCache 列缓存) + MP-1 (注入语句消费 Wrapper)** — 展开 方法引用→列名→条件段

---

## §0.8

- 🔴 Deep，1篇 — 列名提取(**AbstractLambdaWrapper.getColumnCache L127-133: LambdaUtils.extract(column)→LambdaMeta→`PropertyNamer.methodToProperty(implMethodName)`[getUserName→userName]→tryInitCache[首次实体类]→getColumnCache[formatKey 大写查 ColumnCache]**) → 三路元数据(**LambdaUtils.extract L50-62: ①IDEA 调试代理→IdeaProxyLambdaMeta ②反射 `writeReplace`→ReflectLambdaMeta ③序列化兜底→ShadowLambdaMeta[SerializedLambda.extract L42-60: 序列化后反序列化,**重写 resolveClass: java.lang.invoke.SerializedLambda→MP 镜像类 L47-52**]**) → 条件段构建(**AbstractWrapper: eq/ne/gt/... 全走 addCondition L467-470[maybeDo 条件守卫→appendSqlSegments(columnToSqlSegment, SqlKeyword, formatParam[**MPGENVAL 序列参数命名** 测试断言 `c=#{ew.paramNameValuePairs.MPGENVAL1}`])]; and/or/nested L218-234[addNestedCondition 嵌套 Consumer]; apply/last L243-248**; getSqlSegment L624-625[expression+lastSql]) → 段列表(**MergeSegments L33-71: normal/groupBy/having/orderBy 四段[各 SegmentList]+getSqlSegment 拼接; NormalSegmentList.transformList[**首段 and/or 不执行/相邻同类合并/not 处理** L30-60]**) → 查询字段(**LambdaQueryWrapper L50-145: select 重载族[列集合/谓词过滤 chooseSelect MP-2 衔接]; 链式构造器全参构造 SharedString/paramNameSeq/MergeSegments 共享**) → 链式(**LambdaQueryChainWrapper 109: baseMapper 持有, list()/one() 调 baseMapper.selectList[MP-9 衔接]**)
- 设计模式: [模式: 序列化反序列化取元数据+构建器+段列表]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SerializedLambda.java:42-60 | 反序列化 | **extract: 序列化→反序列化, 重写 resolveClass(L47-52): java.lang.invoke.SerializedLambda→MP 镜像类** — 拿方法引用元数据 | High |
| LambdaUtils.java:50-62 | 三路 | ①IDEA 调试 Proxy→IdeaProxyLambdaMeta ②反射 writeReplace→ReflectLambdaMeta ③序列化兜底→ShadowLambdaMeta — 兼容降级 | High |
| AbstractLambdaWrapper.java:127-133 | 列名 | getColumnCache: extract→methodToProperty(getUserName→userName)→tryInitCache(实体类)→formatKey 大写查 ColumnCache(MP-2 产物) | High |
| ReflectLambdaMeta.java | 元数据 | getImplMethodName(方法名)/getInstantiatedClass(从 instantiatedMethodType 解析实体类) | High |
| AbstractWrapper.java:467-470 | 条件 | addCondition: maybeDo(condition 守卫)→appendSqlSegments(columnToSqlSegment, SqlKeyword, formatParam) | High |
| AbstractWrapper.java:218-234 | 嵌套 | and/or/nested: addNestedCondition(Consumer 子包装→APPLY 段) — 括号嵌套 | High |
| AbstractWrapper.java:624-625 | 输出 | getSqlSegment: expression.getSqlSegment()+lastSql | High |
| MergeSegments.java:33-71 | 段合并 | normal/groupBy/having/orderBy 四段列表+拼接 | High |
| NormalSegmentList.java:30-60 | 去重 | **首段 and/or 不执行; 相邻同类 and-and/or-or 合并; not 语义** — transformList 校验 | High |
| LambdaQueryWrapper.java:50-145 | 查询字段 | select 重载族(列集合/TableFieldInfo 谓词→chooseSelect); 全参构造共享状态 | High |
| AbstractWrapperTest.java:18-28 | 参数命名 | 测试断言: `c=#{ew.paramNameValuePairs.MPGENVAL1}` — MPGENVAL 序列+javaType/jdbcType/typeHandler 后缀 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 条件构造器是单管线 — 1篇 (~70行) 按"方法引用元数据提取→列名解析→条件段构建→段列表合并→查询字段→链式"展开; ColumnCache 来源衔接 MP-2(引用), 消费衔接 MP-1(引用)/MP-9(引用)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SerializedLambda 反序列化取元数据 (resolveClass 重写) | 🔴 | **为什么🔴**: 核心黑魔法 |
| P1-2 | LambdaUtils 三路提取 (IDEA 代理/反射/序列化) | 🔴 | **为什么🔴**: 兼容设计 |
| P1-3 | 列名解析链 (methodToProperty→ColumnCache) | 🔴 | **为什么🔴**: 取列名 |
| P1-4 | AbstractWrapper 条件构建 (addCondition/MPGENVAL 参数) | 🔴 | **为什么🔴**: 条件面 |
| P1-5 | 段列表 (MergeSegments 四段+NormalSegmentList 去重) | 🔴 | **为什么🔴**: SQL 段语义 |
| P2-1 | 嵌套条件 (and/or/nested Consumer) | 🟡 | **为什么🟡**: 括号嵌套 |
| P2-2 | 查询字段 select 族 + 链式 | 🟡 | **为什么🟡**: 面 |
| P3-1 | 与 MP-2 ColumnCache/MP-1 语句消费协作 (引用) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **方法引用元数据** | 🔴 | 黑魔法核心 |
| B | **列名解析** | 🔴 | 取列 |
| C | **条件段构建** | 🔴 | SQL 面 |
| D | **嵌套/选择/链** | 🟡 | 增强面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 反序列化黑魔法 | SerializedLambda.extract: 序列化 lambda 再反序列化, **ObjectInputStream.resolveClass 重写: java.lang.invoke.SerializedLambda→MP 镜像类**(L47-52, JDK16+ 强封装下 java.base 类反序列化受限) — 把 JVM 序列化机制当反射工具用, 拿 implMethodName/instantiatedMethodType | SerializedLambda.java:42-60 |
| q2 | 三路提取 | LambdaUtils.extract 降级链: ①IDEA 调试下 lambda 是 Proxy→IdeaProxyLambdaMeta ②反射调用私有 writeReplace→ReflectLambdaMeta(主流) ③兜底序列化 | LambdaUtils.java:50-62 |
| q3 | 列名解析链 | getColumnCache: extract→`PropertyNamer.methodToProperty("getUserName")→"userName"`→tryInitCache(首次用实体类)→`formatKey(userName)` 全大写→COLUMN_CACHE_MAP(MP-2 installCache 产物) — 属性名→列名全链路无字符串硬编码 | AbstractLambdaWrapper.java:127-133; LambdaUtils.java:96-110 |
| q4 | 条件构建 | eq/ne/gt/... 全走 addCondition: `maybeDo(condition)` 条件守卫(可传 boolean 关闭)→`appendSqlSegments(columnToSqlSegment, SqlKeyword, formatParam)` — **参数以 MPGENVAL 序列命名入 ew.paramNameValuePairs**(测试断言 `c=#{ew.paramNameValuePairs.MPGENVAL1}`) | AbstractWrapper.java:467-470; AbstractWrapperTest.java:18 |
| q5 | 段列表语义 | MergeSegments 四段: normal(条件)/groupBy/having/orderBy 各自 SegmentList; **NormalSegmentList.transformList 首段 and/or 不执行、相邻同类合并** — 用户写 eq().or().eq() 时前导/冗余连接词被净化 | MergeSegments.java:33-71; NormalSegmentList.java:30-60 |
| q6 | 嵌套条件 | and/or/nested(Consumer<Children>): 子包装实例化+consumer 填充→作为 APPLY 段整体追加 — 括号嵌套语义 | AbstractWrapper.java:218-234 |
| q7 | 查询字段 | LambdaQueryWrapper.select 族: 列集合 select(SFunction...) 或 TableFieldInfo 谓词过滤(chooseSelect, MP-2 衔接); 全参构造共享 paramNameSeq/paramNameValuePairs/MergeSegments — 拷贝构造支持 | LambdaQueryWrapper.java:50-145 |
| q8 | 链式终点 | LambdaQueryChainWrapper: 持有 baseMapper, list()/one() → baseMapper.selectList(wrapper)(MP-9 衔接) — 链终点是注入语句 | LambdaQueryChainWrapper.java |

→ 引出 MP-4: 条件构造器产物(ew.paramNameValuePairs)在 SQL 里以 ew.xxx 引用 — 分页/乐观锁插件在 Executor 层改写 SQL 时与 Wrapper 联动。
