# MyBatis-Plus — 知识网络化规划 (MP-1~MP-9)

> **日期**: 2026-08-13 | **依据**: issue/源码分析执行计划.md 阶段3.3 (9域) + 00 域发现 REVIEW (入口展开+旁路扫描+对照验证)
> **源码**: `/data/workspace/source-code/code/spring/mybatis-plus` (403 文件; core 模块核心, annotation/extension/generator/spring-boot-starter 4 个外围模块)
> **定位**: 阶段3.3 — ORM 增强层. 核心 = **SQL 自动注入**(DefaultSqlInjector→AbstractMethod 模板) + **Interceptor 链**(MybatisPlusInterceptor→InnerInterceptor 7 回调) — MP 让 MyBatis 从"手写 XML"升级为"方法即 SQL"
> **知识网络**: 本文含 前置/复用/引出 双链, 与阶段2 Spring 层 + 阶段3.1/3.2 连接池双向互联; MyBatis 内核(阶段3.4)未分析 — 本阶段用导航指针

---

## 一、入口点与主线

`BaseMapper<T>`(接口, 565 行: 19 个抽象 CRUD 方法 — 实测修正, 原 17 遗漏 selectMaps×4) → 方法调用经 `MybatisMapperProxy` → MappedStatement 由 **SQL 自动注入**生成(DefaultSqlInjector.getMethodList L39 → AbstractMethod 模板 → 18 个方法类) → 元数据来自 **TableInfoHelper.initTableInfo**(L163 同步+缓存) → 执行时过 **MybatisPlusInterceptor**(L53 interceptors 列表: willDoQuery L75/beforeQuery L78/willDoUpdate L84/beforePrepare L102) → 分页/乐观锁/填充/逻辑删除各司其职. 旁路: mybatis-plus-generator(代码生成器), spring-boot-starter(MybatisPlusAutoConfiguration), annotation(注解定义).

---

## 二、入口展开追踪 (00 §2) + 旁路扫描 (00 §2.5)

### 入口展开 (Level-1/2)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `BaseMapper` 19 抽象 CRUD 方法 | BaseMapper.java | 方法集即 MP 能力面 — 有决策(无 XML 的 CRUD) | **MP-9** |
| `DefaultSqlInjector.getMethodList` | DefaultSqlInjector.java:39-60 | **无主键降级**: havePK 才注入 xxById 族(L49-57) — 方法可用性取决于元数据 | **MP-1** |
| `AbstractMethod` 模板 | AbstractMethod.java(450 行) | SQL 生成模板(方法→MappedStatement) | **MP-1** |
| `TableInfoHelper.initTableInfo` | TableInfoHelper.java:163-185 | 同步+缓存(targetTableInfo L170) — 每 mapper 只解析一次 | **MP-2** |
| `TableInfo` 注解解析 | TableInfo.java:161,439-456 | @TableName/@TableId/@TableField→表模型; logicDeleteSql 生成 | **MP-2** |
| `MybatisPlusInterceptor` | plugins/MybatisPlusInterceptor.java:53,75-102 | interceptors 链 + 4 主回调 | **MP-4** |
| `InnerInterceptor` 接口 | plugins/inner/InnerInterceptor.java:53-123 | **7 方法**(6 回调+setProperties) — 插件 SPI | **MP-4** |
| `PaginationInnerInterceptor` | plugins/inner/PaginationInnerInterceptor.java:116,149 | willDoQuery 判定+beforeQuery 物理分页改写 | **MP-5** |
| `OptimisticLockerInnerInterceptor` | plugins/inner/OptimisticLockerInnerInterceptor.java(319) | @Version CAS 更新 | **MP-6** |
| `MetaObjectHandler` | handlers/MetaObjectHandler.java:85-146 | insertFill/updateFill+strictInsertFill 泛型守卫 | **MP-7** |
| `AbstractWrapper/LambdaQueryWrapper` | conditions/AbstractWrapper.java(712)+LambdaQueryWrapper | 条件 SQL 段构建 | **MP-3** |
| `SerializedLambda.extract` | toolkit/support/SerializedLambda.java:42 | **方法引用反序列化**: 重写 resolveClass(L49-50) 拿字段名 | **MP-3** |
| `IService/ServiceImpl` | extension/service/(IService+impl/ServiceImpl) | 链式调用服务层 | **MP-9** |
| `MybatisPlusAutoConfiguration` | spring-boot-starter/autoconfigure | 自动装配(复用 S-2) | 并入 MP-4/各域一句话 |

### 旁路扫描

| 包/模块 | 文件数 | 设计决策测试 | 结论 |
|---|---|---|---|
| `mybatis-plus-generator` | ~90 | 代码生成器: 元数据→模板→代码 — **承载设计决策(模板引擎/覆盖策略)但独立工具, 面试低频, 非 MP 运行时内核** | **排除**(按需查阅) |
| `mybatis-plus-annotation` | ~20 | @TableName/@Version/@TableLogic 注解定义 — 薄(载体) | 并入 MP-2/6/7/8 |
| `spring-boot-starter` | ~15 | MybatisPlusAutoConfiguration+MybatisSqlSessionFactoryBean — 装配胶水 | 并入各域(导航 S-2) |
| `core/batch` (MybatisBatch/BatchSqlSession) | ~6 | JDBC 批量执行 — 面试低频, 单薄 | 排除(MP-9 一句话) |
| `core/incrementer` | 少量 | 主键生成器(DB2/Postgres 等) — 薄 | 并入 MP-2 |

---

## 三、域清单 (9 域 / 5🔴 + 4🟡, 含信号置信度 + 方案预告)

### 核心域 (🔴 ×5)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| MP-1 | SQL 自动注入 | core/injector | **DefaultSqlInjector(61行) getMethodList 模板法** → AbstractMethod(450行) 方法→MappedStatement 生成器; **无主键降级**(L49-57 warn 不注入 xxById); 18 方法类(12 默认注入+SelectPage 等扩展) | MP-2(元数据), MyBatis 内核(导航) | 面试高频/生产主流/Hub ✅ | 高 | A |
| MP-2 | 表元数据解析 | core/metadata | **TableInfoHelper.initTableInfo(L163) 同步+缓存** — @TableName/@TableId/@TableField/@TableLogic 注解→TableInfo 模型; TableInfo 字段(L161 logicDeleteFieldInfo)与 SQL 片段生成(L439-456) | annotation 模块(薄), MyBatis MapperBuilderAssistant(导航) | 面试中频/生产主流/Hub ✅ | 高 | A |
| MP-3 | Lambda 条件构造器 | core/conditions | **SerializedLambda.extract(L42) 重写 resolveClass 反序列化方法引用** → SFunction 取列名; AbstractWrapper(712行) 条件 SQL 段构建(嵌套/AND-OR/空值守卫) | MyBatis BoundSql(导航) | 面试中频/生产主流/依赖弱 | 中 | A |
| MP-4 | 插件体系 | extension/plugins | **MybatisPlusInterceptor(155行) 主流程 4 回调**(willDoQuery L75/beforeQuery L78/willDoUpdate L84/beforePrepare L102) + **InnerInterceptor 接口 7 方法**(6 回调+setProperties, 全部 default) | MyBatis Executor/StatementHandler(导航) | 面试高频/生产主流/Hub ✅ | 高 | A |
| MP-5 | 分页插件 | extension/plugins/inner | **PaginationInnerInterceptor(478行)**: willDoQuery(L116) 判定分页(IPage 参数存在)→beforeQuery(L149) **物理分页 SQL 改写**(count+limit/方言); 12+ 方言(MySql/Oracle/PG...) | MP-4(链), MyBatis BoundSql(导航) | 面试高频/生产主流/依赖弱 | 高 | A |

### 支撑域 (🟡 ×4)

| # | 域 | 包 | 核心主题 (设计决策) | 依赖 | 信号/置信度 | 方案 |
|:--:|---|---|---|---|:--:|:--:|
| MP-6 | 乐观锁 | extension/plugins/inner | OptimisticLockerInnerInterceptor(319行): @Version 字段 → UPDATE 时 version+1 且 WHERE version=旧值 — CAS 语义 | MP-4 | 面试中频/生产常见/依赖弱 | 中 | B |
| MP-7 | 自动填充 | core/handlers | MetaObjectHandler(接口+strictInsertFill L136 泛型守卫): @TableField(fill=INSERT/UPDATE) → insertFill/updateFill 审计字段 | MP-2 | 面试中频/生产常见/依赖弱 | 中 | B |
| MP-8 | 逻辑删除 | core/metadata+extension | @TableLogic → TableInfo logicDeleteSql(L439-456): DELETE 转 UPDATE(删除值/未删除值双配置); 查询自动加 AND 条件 | MP-2 | 面试中频/生产主流/依赖弱 | 中 | B |
| MP-9 | BaseMapper+IService | core/mapper+extension/service | BaseMapper(565行) **19 个抽象 CRUD 方法**(实测修正, 原 17 遗漏 selectMaps×4); IService/ServiceImpl 链式调用(保存/更新/查询链) | MP-1~8(全部汇聚) | 面试低频/生产主流/叶子 | 中 | B |

---

## 四、已排除 (00 §3 — 防"存在=域")

| 类/包 | 原因 |
|-------|------|
| mybatis-plus-generator (~90) | 代码生成器 — 独立工具承载模板设计决策, 但非 MP 运行时内核, 面试低频, 执行计划未列 |
| core/batch (MybatisBatch/BatchSqlSession) | JDBC 批量封装 — 单薄, MP-9 一句话 |
| core/incrementer | 主键生成器族 — 薄, 并入 MP-2 |
| core/override (MybatisMapperProxy) | Mapper 代理 — MyBatis 内核机制(阶段3.4 导航), 非 MP 独有设计 |
| annotation 模块 | 注解定义 — 载体, 并入 MP-2/6/7/8 |
| spring-boot-starter | 装配胶水 — 复用 S-2, 各域一句话 |

---

## 五、知识网络图 (Obsidian 双链)

```
← 复用/内核来源:
   MyBatis 内核 (SqlSession/Executor/MappedStatement/BoundSql, 阶段3.4 未分析) ──→ MP-1~5 (导航指针, 正文机制展开本层)
   S-2 自动装配管线 (已分析) ──→ MP-4/装配 (MybatisPlusAutoConfiguration)
   HikariCP/Druid (阶段3.1/3.2, 已分析) ──→ MP-9 (连接池是 SQL 执行的下游)
   spring-tx (s29-33, 已分析) ──→ MP-6 乐观锁对照 (并发控制两种方案)

→ 引出/消费者:
   MP-1~9 ──→ 阶段3.4 MyBatis (5 域) — 内核深入, 本阶段是 MyBatis 之上的增强层
   MP-3 Lambda ──→ MyBatis-Plus 生成器 (已排除) / Boot 集成

   📌 双链格式 (每篇大纲 header 写):
   前置: [[MyBatis 内核(导航)]] ...
   复用: [[S-2]] [[D-1-druid]] ...
   引出: [[MyBatis(3.4)]] ...
```

> **⚠️ 前向引用原则 (06 §2)**: MyBatis 内核未分析 — 本阶段正文禁止展开 MyBatis 内部机制, 对 SqlSession/Executor 用导航指针, 机制必展开 MP 本层增量. 阶段3.4 将呼应.

---

## 六、执行顺序 (拓扑: 叶子先)

**MP-2 → MP-1 → MP-9 → MP-3 → MP-4 → MP-5 → MP-6 → MP-7 → MP-8**

> 依赖说明: MP-1 注入依赖 MP-2 元数据(方法生成需 TableInfo) → 先元数据后注入; MP-9 汇聚全部方法面 → 依赖 MP-1/2; MP-3 独立(条件构造)可先; MP-4 链是 MP-5/6 的宿主 → 先链后插件; MP-7/8 依赖 MP-2. 每域走 v5 全管线 (KP→大纲→questions→六层深审→更新 HANDOFF).

---

## 七、深度分类复核 (00 §3.5)

- **5🔴 / 4🟡** (56% 🔴, 未犯 80% 反模式) — 与执行计划一致
- 🔴 = 定义性机制 (注入/元数据/Lambda/插件链/分页 — MP 之所以是 MP)
- 🟡 = 支撑 (乐观锁/填充/逻辑删除/Service 层)
- **REVIEW 修复** (相对执行计划): ①MP-1 方法数 17→18 方法类/12 默认注入(执行计划"17 种内置方法"不精确) ②MP-4 回调数 5→7(InnerInterceptor 实际 7 方法: willDoQuery/beforeQuery/willDoUpdate/beforeUpdate/beforePrepare/beforeGetBoundSql/setProperties; 主流程用 4 个) ③显式排除清单(generator/batch/incrementer) ④知识网络双链(MyBatis 导航+阶段3.1/3.2 对照)

---

## 八、与原始执行计划 (issue/源码分析执行计划.md 阶段3.3) 的差异

| 原始 | 本规划 | 理由 |
|:--:|---|---|
| MP-1 "17种内置方法" | 18 方法类/12 默认注入(DefaultSqlInjector L42-54) | 实测: injector/methods/ 18 类, 默认注入 12 种(Insert/Delete/Update/SelectCount/Maps/Objs/List + 5 ById); SelectPage/SelectMapsPage 等由扩展注入器 |
| MP-4 "5个回调" | InnerInterceptor 7 方法(6 回调+setProperties) | 实测接口 L53-123: willDoQuery/beforeQuery/willDoUpdate/beforeUpdate/beforePrepare/beforeGetBoundSql |
| — | + 排除清单 (4 项) | 00 要求显式排除 |
| — | + 知识网络双链 | MyBatis 导航 + 池对照 |

**覆盖率报告 (00 §第九步)**: 方法论域发现 9 域 = 执行计划 9 域, 覆盖率 **100%**. 差距分析: 无遗漏. 方法论额外发现: generator(排除 — 独立工具非运行时内核), batch/incrementer(排除 — 单薄).

---

## 九、完成检查单 (00 §8)

- [x] 入口点: BaseMapper → DefaultSqlInjector → TableInfoHelper → MybatisPlusInterceptor 主线
- [x] 层级展开: §二 Level-1/2 候选 14 项 + 旁路 6 项
- [x] 设计决策测试: 每候选含测试与结论
- [x] 依赖图: §三 每域依赖列 + §六 拓扑验证
- [x] 环形依赖: 无 (MP-9 汇聚为叶子消费方, 无环)
- [x] 排除清单: §四 4 项含原因
- [x] 对照验证: §八 覆盖率 100% + 2 处执行计划数字修正
- [x] Hub 检查: 无 ≥10 依赖域; 叶子 MP-9 最低 B
