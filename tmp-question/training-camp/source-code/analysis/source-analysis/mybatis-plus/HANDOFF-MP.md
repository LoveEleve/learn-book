# MyBatis-Plus 源码分析 — 交接文档 (阶段3.3)

> **日期**: 2026-08-13 | 阶段3.3 MyBatis-Plus
> **⚠️ 总入口**: 阶段3 总交接见 `../HANDOFF-STAGE3.md`(M+MP 全部状态/方法论/下一步) — 本文为 MP 分域细节。
> **⚠️ 执行顺序变更 (2026-08-13)**: 经 09 怀疑重审 — 实测 **MP→M 单向依赖** (MP 源码 import org.apache.ibatis 474 处; MyBatis 反向引用 com.baomidou 零命中)。原执行计划 3.3 MP 先于 3.4 M 反拓扑 → **M 阶段提前执行, MP 暂停**。M-PLAN 见 `../mybatis/M-PLAN.md` (7 域)。MP 剩余域 (MP-1~MP-8) 在 M 完成后回流, 届时 MyBatis 导航指针全部升级为真实引用。
> **给新 AI**: 本文是 MP 分析唯一入口。规划权威 = `MP-PLAN.md` (9 域 v1, 已按 M 提前调整)。方法论权威 = `talk-method/source-code-analysis/methodology/zh/` (01-09, **09 对既有规划保持怀疑必读**)。Druid 阶段见 `../druid/HANDOFF-DRUID-v2.md`。
> **任务**: MP 剩余 8 域在 M 阶段完成后执行, 顺序 MP-1 → MP-9 → MP-3 → MP-4 → MP-5 → MP-6 → MP-7 → MP-8 (MP-2 已完成)。严格一个域一个域, 问题驱动, 禁止批量写。

---

## §零 当前状态速查

| 域 | 目录 | 类型 | 行数 | 锚点 | 闭环 | questions | 状态 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|
| MP-2 表元数据解析 | outlines/mp2-metadata | 🔴 | 66 | 23 | 10 | 21 | ✅ 完成 (六层深审+二次 REVIEW 通过) |
| MP-1 SQL 自动注入 | outlines/mp1-injector | 🔴 | 62 | 8 | 8 | 20 | ✅ 完成 (深审+时空溯源+harness 8/8) |
| MP-9 BaseMapper+IService | outlines/mp9-mapper-service | 🟡 | 56 | 12 | 8 | 20 | ✅ 完成 (深审+时空溯源) |
| MP-3 Lambda 条件构造器 | outlines/mp3-wrapper | 🔴 | 81 | 12 | 8 | 20 | ✅ 完成 (深审+时空溯源+harness 7/7) |
| MP-4 插件体系 | outlines/mp4-plugin | 🔴 | 62 | 10 | 7 | 20 | ✅ 完成 (深审+时空溯源+harness 5/5) |
| MP-5 分页插件 | outlines/mp5-pagination | 🔴 | 64 | 10 | 8 | 20 | ✅ 完成 (深审+时空溯源+harness 8/8) |
| MP-6 乐观锁 | outlines/mp6-optimistic | 🟡 | 53 | 9 | 6 | 20 | ✅ 完成 (深审+时空溯源) |
| MP-7 自动填充 | outlines/mp7-fill | 🟡 | 82 | 20 | 8 | 20 | ✅ 完成 (深审 5 处真实问题+时空溯源) |
| MP-8 逻辑删除 | outlines/mp8-logicdelete | 🟡 | 82 | 17 | 8 | 20 | ✅ 完成 (深审 2 处修正) |

**执行序**: M 阶段 7/7 ✅ (../mybatis/HANDOFF-M.md §五) → MP-2 ✅ → MP-1 ✅ → MP-9 ✅ → MP-3 ✅ → MP-4 ✅ → MP-5 ✅ → MP-6 ✅ → MP-7 ✅ → MP-8 ✅ — **MP 9/9 全量完成**

---

## §一 MP-6 已交付内容速查 (乐观锁)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| beforeUpdate 入口 (仅 UPDATE+Map 参数) | OptimisticLockerInnerInterceptor.java:106-116 |
| 旧值捕获: et=ENTITY→versionFieldInfo→versionField.get; null→exception/return | OptimisticLockerInnerInterceptor.java:118-136 |
| 双路径: update→wrapper.apply({0} 旧值) / updateById→MP_OPTLOCK_VERSION_ORIGINAL | OptimisticLockerInnerInterceptor.java:143-154 |
| 新值回写: versionField.set(et, 新值) | OptimisticLockerInnerInterceptor.java:156 |
| VERSION_FUNCTION_MAP: 数值+1/时间 now/不支持原值 | OptimisticLockerInnerInterceptor.java:290-305 |
| SQL 联动: getVersionOli `AND version=#{MP_OPTLOCK_VERSION_ORIGINAL}` | TableFieldInfo.java:578-586 (MP-2) |
| wrapperMode: 无实体仅 wrapper→setVersionByWrapper | OptimisticLockerInnerInterceptor.java:158-163,198-203 |

### MP-6 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 40eb27cdd | 2020-06 | OptimisticLockerInnerInterceptor 引入 (3.4.x 插件体系) |
| 9a70683c3 | 2022-01 | **wrapper 模式支持** (pull/3664) |

### MP-6 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | CAS 三步骤/双路径/VERSION_FUNCTION_MAP 7 类型/getVersionOli 联动 逐字对照; 负面空间 4 条 | 记录 |

### MP-6 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | **数字错误** | VERSION_FUNCTION_MAP 写 "7 类型" — 实测 **8 类**(L290-297 穷举: long.class/Long/int/Integer/Date/Timestamp/LocalDateTime/Instant) — 漏基本类型 long | 大纲 §3+KP §0.8/q3/P1-3 四处修正 |
| 3 | 机制补充 | **getUpdatedVersionVal protected 覆写点**(L308-311) 未写 — 用户可自定义版本策略扩展 | 大纲 §3 关键设计+KP q3 补全 |
| 4 | 通过项 | 双路径自洽性验证 (update→wrapper.apply 条件进 ew.sqlSegment / updateById→MP_OPTLOCK_VERSION_ORIGINAL 进 getVersionOli, 两条注入 SQL 路径不同但殊途同归) ✅; 维度1 桥 OUT→MP-7 ✅; 维度3 对照 spring-tx 已分析 ✅ | 记录备查 |

### MP-6 负面空间 (07 维度5)

不做重试 (冲突行数 0 归业务) / 不做悲观锁 (对照 spring-tx) / 不做版本回滚 (拦截后实体已改)。

---

## §二 MP-5 已交付内容速查 (分页插件)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| willDoQuery count 预检 (4 条件不接管 + count 查询 + setTotal) | PaginationInnerInterceptor.java:116-145 |
| continuePage 短路 (总数 0/页码越界未开 overflow) | PaginationInnerInterceptor.java:432-450 |
| buildAutoCountMappedStatement: _mpCount 缓存+复用 sqlSource+Long resultMap | PaginationInnerInterceptor.java:230-253 |
| autoCountSql 优化 (去 orderBy/简化列, 测试断言) | PaginationInnerInterceptor.java:260-275 |
| beforeQuery 改写: concatOrderBy→size<0 特例→handlerLimit→buildPaginationSql | PaginationInnerInterceptor.java:149-195,454-456 |
| 方言: MySql LIMIT ?/? , ? (setConsumerChain) | MySqlDialect.java:25-40 |
| DialectFactory EnumMap + JdbcUtils.getDbType 自动识别 | DialectFactory.java:32-75; L196-202 |
| DialectModel setConsumer 占位标记 + consumers 注入 | DialectModel.java:100-145 |
| findPage 定位 (IPage 参数/Map "page" 键) | ParameterUtils |

### MP-5 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 39cc2ac29 | 2018-02 | 3.0 分包 init — MySqlDialect 等方言框架已有 |
| 40eb27cdd | 2020-06 | PaginationInnerInterceptor 引入 (3.4.x 插件体系) |
| a18ee98e9 | 2020-06 | autoCountSql 引入 |
| be09055ac | 2020-07 | **"新分页插件 优化"** — continuePage 短路等 |

### MP-5 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 一致性 | header "8 KP" 与 KP P 条目 7 不符 | header 改 7 KP |
| 2 | harness 迭代 | 双参数用例用 current=1 (offset=0 实为单参数) — 测试用例设计错误 | 改 current=2, 8/8 PASS |
| 3 | 通过项 | count 流程/改写步骤/方言分支 逐字对照; 负面空间 4 条 | 记录 |

### MP-5 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **算法错误** | "offset()=current*size" — 实测 **IPage.offset(): current<=1→0, 否则 (current-1)*size**(IPage.java:71-77) — 第一页 offset=0 语义 | 大纲两处+KP 修正 |
| 5 | 位置错误 | ParameterUtils 写 extension/plugins/ — 实为 **core/toolkit/ParameterUtils**(findPage L42) | 大纲+KP 修正 |
| 6 | 机制补充 | beforeQuery 改写后 **mpBoundSql.sql(model.getDialectSql()) 写回 boundSql**(L190-191) 未写 | 大纲 §5 数据流补全 |
| 7 | 通过项 | harness offset 实现与真实一致 (current-1)*size 无污染; maxLimit null 时 handlerLimit 跳过(L454) ✅; 维度1 桥 OUT→MP-6 ✅; 维度3 对照 M-5 RowBounds 已分析 ✅ | 记录备查 |

### MP-5 负面空间 (07 维度5)

不做逻辑分页 (物理改写, 对照 M-5 RowBounds) / 不缓存 count 结果 / 复杂 SQL count 优化降级包裹。

---

## §三 MP-4 已交付内容速查 (插件体系)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 5 @Signature 挂载 (StatementHandler×2 + Executor×3) | MybatisPlusInterceptor.java:41-48 |
| plugin 过滤: 只包装 Executor/StatementHandler | MybatisPlusInterceptor.java:110-115 |
| intercept 三路分发 (query/update/StatementHandler) | MybatisPlusInterceptor.java:56-107 |
| willDoQuery 短路 emptyList + beforeQuery 改写 + createCacheKey 重发 | MybatisPlusInterceptor.java:64-81 |
| willDoUpdate 短路 -1 | MybatisPlusInterceptor.java:82-89 |
| StatementHandler 两路 (args null→getBoundSql/否则 prepare) | MybatisPlusInterceptor.java:90-104 |
| InnerInterceptor 6 回调+setProperties 全 default | InnerInterceptor.java:53-126 |
| 配置驱动: PropertyMapper @分组→newInstance+属性注入 | MybatisPlusInterceptor.java:138-146; 测试 L20-38 |

### MP-4 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 56dc59a9b | 2020-06 | 3.4.0 MybatisPlusInterceptor 引入 (InnerInterceptor 体系) |
| 40eb27cdd | 2020-06 | InnerInterceptor 定型 |
| cb5b12f8d | 2020-06-30 | **setProperties 实现** (配置驱动装配) |

### MP-4 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | 5 签名/三路分发/短路语义/boundSql 来源 逐字对照; harness 5/5 (1 次编译迭代); 负面空间 3 条 | 记录 |

### MP-4 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | **用例错误** | willDoQuery false 写"如 SQL 审查拒绝→空结果" — 实为 **分页插件的 continuePage 短路**(count 后总数 0 或页码越界且未开 overflow, PaginationInnerInterceptor.java:432-450); 且分页插件在 **willDoQuery 内部就执行 count 查询**(page.setTotal, L124-145) 再决定短路 | 大纲 §2+KP q2 双修正 (含真实用例细节) |
| 3 | 机制补充 | @InterceptorIgnore 配套缺失 — 各插件内部自查(InterceptorIgnoreHelper.willIgnoreXxx(ms.id), BlockAttack L57/DataPermission L69), 宿主不感知 | 大纲 §3 补 "忽略注解" |
| 4 | 通过项 | 重发语义: target 非代理时无递归; 改写前后 CacheKey 不同→分页/不分页缓存条目分离 ✅; 维度1 桥 OUT→MP-5 ✅; 维度3 对照 M-4 已分析 ✅ | 记录备查 |

### MP-4 负面空间 (07 维度5)

不拦 Parameter/ResultSet 处理器 / 不做插件间依赖管理 / 不做异常重试。

---

## §四 MP-3 已交付内容速查 (Lambda 条件构造器)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| SerializedLambda 反序列化 + resolveClass 重写 (JDK→MP 镜像) | SerializedLambda.java:42-60,47-52 |
| LambdaUtils 三路提取 (IDEA 代理/反射 writeReplace/序列化兜底) | LambdaUtils.java:50-62 |
| 列名解析链: methodToProperty→formatKey→ColumnCache (MP-2 产物) | AbstractLambdaWrapper.java:127-133 |
| addCondition 统一入口 + MPGENVAL 参数命名 | AbstractWrapper.java:467-470; AbstractWrapperTest.java:18 |
| MergeSegments 四段 + NormalSegmentList 净化 (首段/同类合并) | MergeSegments.java:33-71; NormalSegmentList.java:30-60 |
| 嵌套 and/or/nested (Consumer→APPLY 段) | AbstractWrapper.java:218-234 |
| select 族 (列集合/TableFieldInfo 谓词→chooseSelect) | LambdaQueryWrapper.java:50-145 |
| 链式终点: LambdaQueryChainWrapper→baseMapper.selectList | LambdaQueryChainWrapper.java |

### MP-3 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| dd84378fe | 2018-05 | **"完成简单 lambda 解析代码"** — SerializedLambda 初始版 |
| 03e35f1cb | 2018-05 | 支持 String lambda 模式 |
| 11fbaf824 | 2018-10 | **重构: 增加反序列化安全性 (resolveClass 重写), 优化命名** |
| 162fc4358 | 2021-07 | **解决 lambda 构造器 JDK16 无法运行问题** (强封装兼容) |

### MP-3 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | 行号全部 grep (extract 三路/writeReplace 反射/MPGENVAL 断言/transformList); harness 7/7 一次通过; 负面空间 4 条 | 记录 |

### MP-3 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | **内部矛盾** | 负面空间 "不做字符串拼接: 无 SQL 注入面" — 与 **apply(condition, applySql, values)(L243-248) 受控片段注入** 矛盾 (值 {} 占位参数化, 结构部分用户负责) | 负面空间改 "不做任意字符串拼接, 唯一例外 apply 受控片段" |
| 3 | **表述不准确** | "JDK 的 SerializedLambda 非 public 构造受限" — 实为 **java.base 模块类, JDK16+ 强封装下反序列化受限**(时空溯源 162fc4358 JDK16 修复印证) | 大纲 §1+KP q1 改模块/强封装表述 |
| 4 | 通过项 | maybeDo L544/columnToSqlSegment L671(ColumnSegment 函数式)/columnToString 双实现(String vs SFunction) 逐字对照; 维度1 桥 OUT→MP-4 ✅; 维度3 对照 M-6 已分析 ✅ | 记录备查 |

### MP-3 负面空间 (07 维度5)

不做字符串拼接 (防注入) / 不做执行 (链终点在 baseMapper) / 不做多表 join (单实体)。

---

## §五 MP-9 已交付内容速查 (BaseMapper+IService)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 17 抽象 CRUD 方法面 + default 便捷族 | BaseMapper.java:108-565 |
| default 直连: MybatisUtils.getMybatisMapperProxy→sqlSession 直调 | BaseMapper.java:136-138,205-216 |
| selectOne 双态 + exists | BaseMapper.java:305-329 |
| ServiceImpl: baseMapper 注入/工厂从代理提取(volatile) | ServiceImpl.java:58-93 |
| getSqlStatement: 复用 MP-1 注入 statementId | ServiceImpl.java:166-170 |
| saveBatch: @Transactional+executeBatch 分批提交 | ServiceImpl.java:180-190 |
| retBool: 影响行数→boolean | SqlHelper.java:113-123 |
| 链式入口: queryChain/lambdaQuery → LambdaQueryChainWrapper(MP-3) | IService.java:596-655 |

### MP-9 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 39cc2ac29 | 2018-02 | 3.0 分包 init — BaseMapper/ServiceImpl 初始版 |
| 5d25a09a7 | 2019-11 | lambdaQueryChain rename |
| 8953ad271 | 2024-04 | **增强 BaseMapper 代理类获取** (getMybatisMapperProxy) |

### MP-9 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 数字验证 | 17 抽象方法精确穷举 (python 正则计数, 与 MP-PLAN 基线一致); default 直连/selectOne 双态/批量路径逐字对照 | 记录 |

### MP-9 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | **数字错误 (含基线)** | 大纲/KP "17 抽象方法 + selectMaps×3" — 实测 **19 个** (L108-423 逐行 grep: selectMaps **×4** 非 ×3, L377/386/395/405); **MP-PLAN 基线 "17 个 CRUD 方法" 同步错误** | 大纲 §1+KP q1/P1-1 + **MP-PLAN 基线** 三处修正 (09 精神: 既有规划数字穷举) |
| 3 | 通过项 | 维度1 桥 OUT→MP-3 ✅; 维度3 引用 MP-1/MP-2 就绪+MP-3/MP-5 导航 ✅; 维度5 负面空间 4 条 ✅; ServiceImpl 工厂提取/批量路径逐字对照 ✅ | 记录备查 |

### MP-9 负面空间 (07 维度5)

不做事务注解 (边界在 ServiceImpl/外部切面) / 不做 SQL 拼接 (全走 Wrapper MP-3) / 不做实体校验 (归填充与业务层)。

---

## §六 MP-1 已交付内容 (SQL 自动注入)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 12 默认方法面: 7 无条件+5 havePK 条件+无主键 warn | DefaultSqlInjector.java:39-60 |
| inspectInject 装配循环 + mapperRegistryCache 防重复 | AbstractSqlInjector.java:44-65 |
| 用户自定义优先: hasMappedStatement 跳过+warn | AbstractMethod.java:252-273 |
| 注册参数: PREPARED/flushCache=!isSelect/useCache=isSelect | AbstractMethod.java:261-267 |
| 列片段: 自定义 resultMap→*/否则 getAllSqlSelect(MP-2) | AbstractMethod.java:162-173 |
| 方法类模板: SelectById 5 占位 String.format | methods/SelectById.java |
| keyGenerator 三分支: NoKey/AUTO→Jdbc3+去转义符/Sequence→genKeyGenerator | methods/Insert.java |
| SqlMethod 枚举 25 值 (method+描述+SQL 模板) | core/enums/SqlMethod.java |

### MP-1 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 0bbe9c31e | 2018-04 | **注入器重构抽象方法位置移动** — AbstractMethod/方法类体系初始定型 |
| 后续 | — | insertIgnoreAutoIncrementColumn(3.5.4)/用户优先 warn 等迭代 |

### MP-1 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | 构造器参数遮蔽 final 字段; SELECT_BY_ID 模板少 1 个 %s (实为 5 占位: 列/表/keyColumn/keyProperty/逻辑删除) | harness 修正 8/8 PASS |
| 2 | 通过项 | 行号全部 grep (L39-60/L44-65/L252-273/L446); 负面空间 4 条; 五要素 5=5=5=5=5 | 记录 |

### MP-1 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **机制不精确** | 大纲 §2 注入时机写"与 mapper 解析同步" — 实为 **MybatisMapperAnnotationBuilder.parse 注解解析后 + isSupperMapperChildren 条件**(GlobalConfigUtils L122-124: superMapperClass 默认 Mapper.class 可赋值才注入, 普通接口跳过) + **InjectorResolver 延迟重试**(M-1 incomplete 机制) | 大纲 §2+KP q2 双修正 |
| 4 | 机制补充 | Insert keyGenerator 外层条件 (keyProperty 非空) / DefaultSqlInjector L42 Insert 带 insertIgnoreAutoIncrementColumn 配置 / hasMappedStatement(statementName, false) 防注入期 pending 解析 | 大纲 §4/§1 + KP q3 补全 |
| 5 | 通过项 | 维度1 桥 OUT→MP-9 ✅; 维度3 引用 M-1/MP-2 均就绪 ✅; 维度5 负面空间 4 条 ✅; 时空溯源 2018 初始版 ✅ | 记录备查 |

### MP-1 负面空间 (07 维度5)

不做 SQL 校验 (首执行才暴露) / 不做运行时重注入 / 不做方法删除 (用户优先=跳过非移除)。

---

## §七 MP-2 已交付内容 (表元数据解析)

### 产出物

- `knowledge-planning/mp2-metadata.md` — KP (§0.8+01 提取 14 行+P1P2P3 8 条+§05 闭环 10 条)
- `outlines/mp2-metadata/01-table-metadata.md` — 大纲 (66 行, 5 节+结尾桥)
- `outlines/mp2-metadata/completeness-questions.md` — 3 身份 21 问

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| 入口双缓存 + Configuration 重初始化 (多数据源/热重载重建, TABLE_NAME_INFO_CACHE 只 put 不清理边界) | TableInfoHelper.java:163,164,168,170,77,82,203-204 |
| 读取路径: primitive/SimpleType/interface 过滤 + getUserClass 去代理 + 父类缓存移动 | TableInfoHelper.java:97-118 |
| 表名五步管线: camelToUnderline→firstToLowerCase→prefix→tableFormat→schema; 显式表名豁免前缀 (keepGlobalPrefix) | TableInfoHelper.java:221,225,233-238,255-270,288-302; MybatisConfiguration.java:92 |
| 主键三路径: @TableId (多个抛) → 属性名 id 猜 → warn+havePK=false; type 注解>全局 ASSIGN_ID | TableInfoHelper.java:321,335-347,472-517,529-558,374-377; TableInfo.java:220; GlobalConfig.java:108 |
| 字段发现: 组合注解递归 SPI (防环) + 继承合并 (子类同名优先, 去 static/transient) | AnnotationHandler.java:38,62; AnnotationUtils.java:29-46,62-70; ReflectionKit.java:127-155; TableInfoHelper.java:607-615 |
| 列名推导 + checkRelated AS 兜底 (有 resultMap 互斥) | TableFieldInfo.java:265-300,354-380; TableInfoHelper.java:570-582 |
| FieldStrategy convertIf (NEVER→null/直拼/NOT_EMPTY/非空) + fill 豁免 + chooseFieldStrategy 注解>全局 | TableFieldInfo.java:596-608,302-304,318-320,455-462; GlobalConfig.java:194,200,217 |
| 逻辑删除双路径: @TableLogic 注解值优先, 全局 logicDeleteField 仅类中无注解时兜底 | TableFieldInfo.java:406-430; GlobalConfig.java:184-188 |
| fail-fast: 多 @TableId 解析期抛 / 多 @TableLogic·@Version setFieldList Assert | TableInfoHelper.java:338-340; TableInfo.java:493-518 |
| resultMap 三态: 显式/autoResultMap 自动构建 (ResultFlag.ID+IJsonTypeHandler 每实例化)/AS 兜底 | TableInfoHelper.java:246-250,475-491; TableFieldInfo.java:556-576 |
| Lambda 列缓存联动 (MP-3 衔接) | TableInfoHelper.java:207; LambdaUtils.java:86-110 |

### 本域 REVIEW 真实问题 (六层深审抓到)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 密度 | 首版大纲 145 行 (🔴 上限 69) — 4 节太多/空行膨胀 | 重构 5 节紧凑结构 (场景/源码路径无空行), 66 行 |
| 2 | 一致性 | header "10 KP" 与 KP P1P2P3 实际 8 条不符 | header 改 8 KP (对照 Druid d02: KP 数=P 条目数) |
| 3 | 语义 | 大纲 "excludeProperty 落表" — 实为 initTableName 返回值 | 改 "schema/resultMap/autoResultMap 落表, excludeProperty 返回" |
| 4 | 编造 | 结尾桥 "8 种标记" — TableInfo 标记无此数字 | 改 "逻辑删除·版本·填充标记" |
| 5 | 语义 | KP §0.8 旧稿 "无注解猜主键按 IdType 策略" — 实为按属性名 "id" 猜 | 双修正 (KP+保持大纲一致) |

### 二次深度 REVIEW (2026-08-13, 5 处真实问题)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 行号 | AnnotationUtils 递归穿透元注解调用点写 L70 — 实为 **L65** (sed 59-72 逐行核对) | 大纲 L35 + KP q2 区间精确化 (33-49, 61-72; HashSet 防环 L61, 上溯 L44-48) |
| 7 | 错别字 | 大纲 "注读取 SPI" | 改 "注解读取 SPI" |
| 8 | 语义 | LambdaUtils 列缓存 key 写 "property→ColumnCache" — 实为 **formatKey(property)=property 全大写** (L77-79,101,107), 这是 MP-3 匹配列名的关键约定 | 大纲补 formatKey 大写语义 |
| 9 | 数字 | questions#13 FieldStrategy 枚举列 5 个 — 实为 **6 个** (漏 DEFAULT, 且 DEFAULT=注解跟随全局是关键语义) | 补全并注明 DEFAULT 语义 |
| 10 | 验证通过项 | PostInitTableInfoHandler 三钩子 L37/47/57 (非 40/52/59) — 产出物未引用, 无污染; ReflectionKit 过滤 static/transient 区间 L147-153; GlobalConfig 策略默认 L194/200/217; getWhereStrategy 有 selectStrategy 兼容覆写 (L235-237, 未展开可接受) | 记录备查 |

### 六层深审验证情况

- 层1 行号: 全部 grep 原文件验证 (含 L70 递归调用点/TableInfo.java:477,480-482,516-517 等)
- 层4 技术声明: SqlCondition.EQUAL="%s=#{%s}" 确认 where 默认; MybatisConfiguration.java:92 确认驼峰默认开
- 层5 算法: 主键三路径条件 (existTableId/else if !isReadPK)、keepGlobalPrefix 豁免、checkRelated 去下划线比较、formatLogicDeleteSql 双值分支 — 逐字对照
- 层6 归属: 全部方法/字段归属类核对通过

---

## §八 方法论速查 (从 HANDOFF-DRUID-v2 继承, 每域必走)

```
Pass 0 读代码前上下文(README/测试地图)
  → Pass 1 扫轮廓(≥5 真标记问题/读 2 测试)
  → Pass 2 闭环(假设→grep 验证→结论; 内化 KP §05; ≥3 闭环)
  → Pass 3 大纲(四要素: 场景/源码路径/关键设计/数据流)
  → 六层深审(必须真找问题, 零发现=不合格)
  → 全量回归 → 更新本文
```

**大纲格式**: header 必含 `前置/复用/对照/引出` + `类型 | KP 数(P 条目数) | 模式` + `Pass 2 闭环: qN(...)` (与 KP §05 完全同步) + 读者处境; 每节四要素; 结尾桥独立段落 (不占四要素计数); 🔴 39-69 行 / 🟡 35-49 行; 锚点 🔴≥8 / 🟡≥4。

**产出物**: KP (§0.8+01提取+P1P2P3+§05闭环) + outlines/{域}/01-*.md + completeness-questions.md (≥3 身份 ≥5 问)。

**检查命令**: 五要素 grep 计数全等 / wc -l 密度 / tail -1 含 → 引出 / KP P1P2P3 条数=色=为什么 / header qN 列表=KP §05 条数 / 锚点密度。

---

## §九 高频坑 (HANDOFF-DRUID-v2 §八 继承)

1. **批量操作 = 被质疑** — 一个域一个域, 汇报确认后再下一个
2. header 闭环列表与 KP §05 不同步 (Druid 8 次被抓)
3. 条件写反 (updateCount/直方图教训) — 逐字对照源码
4. 默认值编造 (testWhileIdle 教训) — 默认值 grep DEFAULT_*
5. 数字穷举 (spi 14→16 教训) — 目录/文件数 ls 数全
6. 重写后残留旧错误 — 修正后全文件扫
7. 行号必须 grep 原文件 (禁止数 cat 输出)
8. 前向引用: 正文禁展开未分析域机制 (MyBatis 内核/后续 MP 域用导航指针)
9. **MP 特有**: MyBatis 内核 (SqlSession/Executor/MappedStatement/BoundSql) 未分析 — 阶段3.4 才展开, 本阶段一律导航指针

---

## §十 文件路径

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/mybatis-plus/
├── MP-PLAN.md                     ← 规划权威 (9 域 v1: 域清单/依赖/排除/拓扑)
├── HANDOFF-MP.md (本文)           ← MP 阶段权威
├── knowledge-planning/mp2-metadata.md   ← MP-2 KP ✅
└── outlines/mp2-metadata/               ← MP-2 大纲+questions ✅

源码:
/data/workspace/source-code/code/spring/mybatis-plus/  (403 文件)
├── mybatis-plus-core/    ← metadata/(TableInfoHelper 646/TableInfo 580/TableFieldInfo 609/IPage)
├── mybatis-plus-annotation/  ← 注解定义 (TableName 76/TableId 41/TableField 182/TableLogic 40/Version 40)
├── mybatis-plus-extension/   ← plugins/conditions/service (MP-3/4/5/6/9)
├── spring-boot-starter/      ← 装配 (并入各域一句话)
└── mybatis-plus-generator/   ← 已排除 (独立工具)
```

---

## §十一 MP-7 已交付内容速查 (自动填充)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **执行入口**: LanguageDriver 覆写 createParameterHandler → MybatisParameterHandler 构造 (super 存参后 processParameter) — 每次 SQL 执行 new ParameterHandler 必触发 | MybatisXMLLanguageDriver.java:45-46; BaseStatementHandler.java:70; SimpleExecutor.java:48,62,75; MybatisParameterHandler.java:64-70 |
| 命令过滤: 非 null + 非 SimpleType + INSERT/UPDATE | MybatisParameterHandler.java:72-79 |
| **提取链四分支**: Collection/数组/Map 值展开+objectSet 去重 (重入修复 ae5592621)/实体单例; Map 找 et 键 | MybatisParameterHandler.java:188-221,85-94 |
| **三重守门**: openInsertFill() 旧签名 AND openInsertFill(ms) (3.5.6 按 MappedStatement.id 跳过, cd0238821) AND tableInfo.isWithInsertFill (表级聚合) | MybatisParameterHandler.java:128-142; MetaObjectHandler.java:43-78; TableInfo.java:493-508 |
| **strictFill 三条件**: property 同名 + fieldType 精确 (Class.equals) + fill 标记, findFirst; 泛型 <T,E extends T> 子类值 (OjbkXx) | MetaObjectHandler.java:195-207; StrictFill.java:31-51 |
| **策略族**: setFieldValByName (值非 null 强写)/fillStrategy (空才填)/strictFillStrategy (空+新值非 null) — 有值不覆盖 = 幂等基石 | MetaObjectHandler.java:101-106,218-223,234-242 |
| **SQL 直拼**: withInsertFill/withUpdateFill 字段列+值无 if 标签 (必有值断言, 对照 version if 守卫) | TableFieldInfo.java:455-462,486-493,512-530; TableInfo.java:343-382 |
| 主键先行: populateKeys (getKey>=3 即 ASSIGN_ID=3/ASSIGN_UUID=4) 先, insertFill 后; AUTO/INPUT 不管 | MybatisParameterHandler.java:101-126 |
| 时序: 拦截器 beforeUpdate (乐观锁) 外层 → ParameterHandler updateFill 内层 | MybatisPlusInterceptor.java:84-89 |
| **逻辑删除触发面**: LOGIC_DELETE_BY_ID = UPDATE 命令 + withUpdateFill 字段 getSqlSet 直拼 SET (排除 logicDelete 自身) → 删除也填充 (MP-8 连接) | DeleteById.java:57-67; SqlMethod.java:51 |
| 边界: update(wrapper) 无实体不填充 (官方声明); SELECT/DELETE 不处理 | CHANGELOG.md:56; MybatisParameterHandler.java:72-79 |

### MP-7 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 2017-06 | FieldFill 枚举 (hubin, @TableField fill 声明) | |
| 2016-08 | MetaObjectHandler 接口初始版 (since 2016-08-28) | |
| 2017-06-25 | H2MetaObjectHandler 集成测试 (hubin) | |
| 44c421836 | 2020-06 | **strictFill 族引入 + 重载修复 (参数位置对调) + 泛型子类值支持** (3.3.0) |
| ae5592621 | 2023-09 | 修复参数填充多次重入 (I8506T/I82VLI, objectSet 去重) |
| cd0238821 | 2024-03 | **3.5.6 openInsertFill(MappedStatement)** — 按 ms.id 跳过特定方法填充 |

### MP-7 深审记录 (六层深审抓到 5 处真实问题)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **机制错误** | 构造器顺序写反 — "先 processParameter 再 super" 实为 **super 先存参后 processParameter** (L65→L69) | pass1/q1/KP/大纲 4 处修正 |
| 2 | **负面空间错误** | "不做分布式/全局 ID" — populateKeys 的 IdentifierGenerator 雪花就是分布式 ID 方案 (ASSIGN_ID) | 改为 "不管 AUTO/INPUT 主键" |
| 3 | 行号 | TableInfo.setFieldList 聚合写 503-508, 实为 **493-508** | 大纲+pass1 修正 |
| 4 | 双链 | M-2 目录名 m2-sqlsession-executor 实为 **m2-executor** / M-6 实为 m6-scripting | 大纲 header 修正 |
| 5 | 精确化 | SimpleExecutor 三处 newStatementHandler 方法归属: doUpdate/doQuery/queryCursor | 大纲修正 |
| 6 | 通过项 | MybatisUtils 与填充无直接关系 (JSON handler/代理提取, 属 MP-2/MP-9) — HANDOFF-STAGE3 §七 提示误导 | 记录澄清 (q9) |

### MP-7 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **缺失深度** | FieldFill vs FieldStrategy 优先级未写 — FieldFill javadoc "判断优先级比 FieldStrategy 高"; convertIf (L596-608) NEVER→null 整列不进 SQL, fill 字段绕过整个 FieldStrategy | 大纲节 5 + KP 补全 |
| 8 | **注册路径缺失** | MybatisXMLLanguageDriver 成为默认驱动的前提未写 — MybatisConfiguration 构造 L94 setDefaultDriverClass (MP 覆写 Configuration) | 大纲节 1 补全 |
| 9 | **表述不精确** | "version 字段不参与 fill" — 无代码互斥 (TableFieldInfo L215/L220 独立赋值), 实为乐观锁先写+有值不覆盖保证不冲突 | 大纲节 6 措辞修正 |
| 10 | **边界缺口** | 基本类型 int 字段 strictFill 不匹配 (int.class vs Integer.class) → 静默不填充; MP 测试均用包装类型 | 大纲节 3 补边界句 |
| 11 | 措辞 | "updateById 的 Map 参数" — updateById 是单实体参数; Map 场景是 update(et, wrapper) 等 | 大纲节 2 修正 |
| 12 | 通过项 | MybatisConfiguration.setDefaultScriptingLanguage L208-210 兜底 MybatisXMLLanguageDriver; HANDOFF-MP/STAGE3 一致性 ✅ | 记录 |

### MP-7 负面空间 (07 维度5)

不填充 update(wrapper) (官方声明) / 不处理 SELECT/DELETE / 不管 AUTO/INPUT 主键 / 不校验填充结果 (DB NOT NULL 兜底) / 不做并发控制 (有值不覆盖幂等)。

---

## §十二 MP-8 已交付内容速查 (逻辑删除)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **双值三级来源**: @TableLogic value/delval > 全局 DbConfig (logicDeleteValue="1"/logicNotDeleteValue="0") > 默认; 全局 logicDeleteField 属性名兜底 (仅类中无注解) | TableFieldInfo.java:406-430; GlobalConfig.java:180-186 |
| **fail-fast**: 多个 @TableLogic 注入期 Assert 抛错 (同 @Version 模式); logicDeleteFieldInfo 唯一聚合 | TableInfo.java:493-512 |
| **getLogicDeleteSql 双语义**: isWhere=true→未删除值 (查询条件) / false→删除值 (SET); "NULL" 字符串→IS NULL/=NULL 特例; charSequence→引号, 函数 (NOW())→裸值 | TableInfo.java:437-470 |
| **方法类双路径**: isWithLogicDelete → LOGIC_DELETE (UPDATE)+addUpdateMappedStatement / 物理 DELETE+addDeleteMappedStatement — 命令类型切换是填充触发根源 | Delete.java:47-62; DeleteById.java:57-79; DeleteByMap.java:46-63 |
| sqlLogicSet: "SET " + getLogicDeleteSql(false,false) → SET deleted=1 | AbstractMethod.java:106-108 |
| **查询三路**: SelectById/SelectBatchByIds 尾缀 AND deleted=0; wrapper 路径 convertWhere 包裹; getAllSqlWhere 排除 deleted 自身 (防条件双写) | SelectById.java:50; AbstractMethod.java:243-246; TableInfo.java:395-402 |
| **更新双防护**: updateById additional 含 AND deleted=0 (更新已删行数 0); sqlSet(logic) SET 恒排除 deleted; 全表更新拦截协同 (d694f104b) | UpdateById.java:48-51; AbstractMethod.java:120-130 |
| **填充协同 (MP-7)**: withUpdateFill 字段 getSqlSet 直拼 SET (filter 排除 logicDelete 自身) + UPDATE 命令触发 updateFill — 删除人/时间自动填充 | DeleteById.java:57-67; LogicDelTest.java:83-94,127 |
| **批量双形态**: foreach 运行时判定 SimpleType→#{item} / 实体→#{item.id}; 实体集合+withUpdateFill → 实体删除转换 (主键类型匹配 4d5e4e45a) | DeleteByIds.java:84-100 |

### MP-8 时空溯源

| 提交 | 日期 | 机制演变 |
|:--|:--|:--|
| 早期 | 2017 | @TableLogic 逻辑删除引入 (annotation) |
| — | — | 全局逻辑删除字段支持 (CHANGELOG 484) |
| — | — | 删除/未删除值支持字符串 "null" (CHANGELOG 466) |
| 5bf9f6290 | — | 逻辑删除 deleteById 支持自动填充 |
| 4d5e4e45a | 2021-12 | 实体删除转换 (主键类型必须匹配) |
| be2f0c3da | — | BaseMapper 逻辑删除默认支持填充 (3.5.6) |

### MP-8 深审记录 (六层深审抓到 2 处真实问题)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 行号 | LogicDelTest deleteBy 断言行号 58-64 实为 **83-94,127** (L94 断言/L127 strictUpdateFill) | 大纲节 6 修正 |
| 2 | 覆盖缺口 | Q16 全表更新拦截 (BlockAttack) 与逻辑删除协同 — 大纲未覆盖 (闭环 q8 有) | 大纲节 5 补全 |
| 3 | 通过项 | 全部锚点 grep 验证: getLogicDeleteSql L437/formatLogicDeleteSql L455/H2 全局配置 L87-88 等 | 记录 |

### MP-8 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **源码 javadoc 矛盾 (实证)** | formatLogicDeleteSql javadoc 写 "isWhere true: logicDeleteValue" — 与实现 (true→logicNotDeleteValue) **正好相反** — 命名反直觉到注释都写反 (TableInfo.java:450-454) | 大纲节 2 加注 |
| 5 | **机制缺失** | DeleteById 填充 SET 的 **`!isSimpleType(_parameter)` 运行时守卫** (L64) — deleteById(1L) 不生成填充 SET/不触发 updateFill, deleteById(et) 才填充 (与 MP-7 过滤双重一致) | 大纲节 6+闭环 q5+KP 补全 |
| 6 | **缺失负面空间** | 值直拼不参数化 (formatLogicDeleteSql String.format) — SQL 注入面由"值只来自注解/全局配置"封口 | 大纲负面空间补 2 条 |
| 7 | 一致性 | 节 3 "物理/逻辑共存" vs 负面空间 "不做共存" 措辞易混 — 澄清: 共存=不同表可选, 同表不混合 | 负面空间加限定 |
| 8 | 深度 | additional 条件顺序: optlock 前 deleted 后 (组合场景) | 大纲节 5 补全 |

### MP-8 负面空间 (07 维度5)

不转换手写 SQL (CHANGELOG 682) / 不做物理删除共存 (布尔切换) / 不处理 INSERT / 不自动迁移清理已删数据 / 不做多表级联。

---

## §十三 阶段3.3 收官 — MP 9/9 全量完成

- **MP 阶段**: 9 域全部完成 (MP-2 历史 + MP-1/9/3/4/5/6/7/8 本阶段), 每域 KP+大纲+questions+六层深审
- **执行期注入三件套**: 乐观锁 (MP-6) + 自动填充 (MP-7) + 逻辑删除 (MP-8) — 汇聚 BaseMapper 19 方法面 (MP-9)
- **阶段3.3 汇总**: 交给 HANDOFF-STAGE3 §零/§四/§五 更新 (另行)
- **下一步**: 阶段3.5 Redis (18 域) — 开工前必须 09 域重审 (见 HANDOFF-STAGE3 §八)
