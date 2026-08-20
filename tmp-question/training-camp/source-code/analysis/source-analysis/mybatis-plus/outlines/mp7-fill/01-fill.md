# MP-7 自动填充 — @TableField(fill) 审计字段自动写入: 执行期 hook + 严格匹配 + SQL 直拼

> 前置: [[MP-2-metadata]] (FieldFill→withInsertFill/withUpdateFill) + [[M-2-executor]] (ParameterHandler 时序) + [[M-6-scripting]] (LanguageDriver 扩展点) | 复用: [[MP-4-plugin]] (拦截器外层时序) | 对照: [[MP-6-optimistic]] (执行期注入族) | 引出: [[MP-8-logic-delete]]
> 🟡 Working | 8 KP | [模式: 扩展点 hook+严格匹配守卫+策略]
> Pass 2 闭环: q1(执行时机) q2(严格匹配) q3(SQL直拼) q4(提取链) q5(开关注门) q6(策略族) q7(时序) q8(边界)

**读者处境**: 每张表都有 create_time/update_time/creator/operator 审计字段 — 手写 set 又啰嗦又漏; 给字段加个 `@TableField(fill = FieldFill.INSERT)` 注解就自动填充。这篇拆自动填充: 填充在 SQL 执行链的哪一环介入? 为什么填错类型会被拒绝? 为什么填充字段的 SQL 没有 if 守卫? 逻辑删除为什么会触发填充?

### 1. 执行入口 — 藏在 ParameterHandler 构造里的 hook

场景: insertFill 到底被谁调用? 为什么每次 SQL 执行都会触发?
源码路径:
- `MybatisConfiguration.java:94` — MP 覆写 Configuration, 构造时 `languageRegistry.setDefaultDriverClass(MybatisXMLLanguageDriver.class)` — **默认语言驱动被换掉的注册点** (装配前提)
- `MybatisXMLLanguageDriver.java:45-46` — 覆写 `createParameterHandler` → 返回 `new MybatisParameterHandler(...)` (LanguageDriver 扩展点, M-6)
- `BaseStatementHandler.java:70` (MyBatis) — StatementHandler 构造时 newParameterHandler; `SimpleExecutor.java:48,62,75` — **doUpdate/doQuery/queryCursor 每次执行都 new StatementHandler** → 每次执行必 new ParameterHandler
- `MybatisParameterHandler.java:64-70` — 构造器: super 先 (DefaultParameterHandler 存参) → `processParameter(parameter)` L69 — 填充发生在参数绑定 (setParameters) 之前
- `MybatisParameterHandler.java:72-79` — 过滤: 非 null + 非 SimpleType + INSERT/UPDATE 命令
关键设计: 扩展点 hook (q1): MP 不改造 MyBatis 内核, 用 LanguageDriver 扩展点换掉 ParameterHandler 实现 — 构造时改实体, 稍后 setParameters 读到填充后的值; 装配靠 MybatisConfiguration 把默认驱动指到 MP 实现。[模式: 可插拔扩展点 + 构造时注入]
数据流: SQL 执行 → newStatementHandler → newParameterHandler → MybatisParameterHandler 构造 → processParameter → 实体被填充 → setParameters 绑定新值。

### 2. 提取与守门 — 实体从哪来, 三层开关怎么关

场景: 批量插入/updateById 的 Map 参数怎么提取实体? 填充什么时候被跳过?
源码路径:
- `MybatisParameterHandler.java:188-221` — extractParameters 四分支: Collection / 数组 / **Map 所有值展开** + objectSet 去重 (q4, 重入修复 ae5592621) / 实体单例
- `MybatisParameterHandler.java:81-109` — process: 值若为 Map 找 `Constants.ENTITY` ("et") 键提实体 (update(et, wrapper) 场景); 普通实体/集合元素直接当实体; getTableInfo 定位 (TableInfoHelper 双缓存, MP-2)
- `MybatisParameterHandler.java:128-142` — 三重守门: `openInsertFill()` (旧) AND `openInsertFill(mappedStatement)` (**3.5.6 按 MappedStatement.id 精确跳过**, q5) AND `tableInfo.isWithInsertFill()` (表级聚合 TableInfo.java:493-508)
- `GlobalConfigUtils.java:110-111` — handler 未配置 → Optional 空 → 整链静默跳过
关键设计: 宽提取+严守门 (q4/q5): Map 值全展开兼容各种参数形态 (et/批量/多参), 守门三层 (全局/按 mapper/按表) 控制填充面。[模式: 提取适配 + 分层开关]
数据流: parameter (实体/Collection/数组/Map) → extractParameters → process → 实体 → 三层守门 → insertFill/updateFill。

### 3. 严格匹配 — 三条件过滤 + 泛型子类值

场景: strictInsertFill 和 setFieldValByName 有什么不同? 为什么填错类型会静默失败?
源码路径:
- `MetaObjectHandler.java:195-207` — strictFill 核心: 表级守门 → fieldList 流式过滤 **property 同名 + fieldType 精确相等 + fill 标记匹配** → findFirst → strictFillStrategy
- `StrictFill.java:31-51` — `<T, E extends T>`: fieldVal 是 fieldType 的子类实例 (OjbkXx extends Ojbk, 测试实证); 两种 of() 工厂 (直值/Supplier)
- H2 测试实证: strictInsertFill("testType1"...) 实体没有该字段 → 匹配不到 → **不 set**
- **基本类型边界**: 实体字段是 `int` (propertyType=int.class) 时, strictInsertFill 传 Integer.class → Class.equals 不匹配 → **静默不填充** (包装类型字段才匹配; MP 测试均用包装类型)
关键设计: 白名单匹配 (q2): 严格模式只填"表模型里确实声明 fill 的字段", 类型必须精确一致 (Class.equals, 基本类型与包装类不混) — 防错填/类型转换错误; 值可为子类 (泛型 E extends T)。[模式: 白名单 + 泛型守卫]
数据流: strictInsertFill(name, String.class, "值") → findTableInfo → strictFill → 三条件过滤 → 匹配则 strictFillStrategy 填充。

### 4. 填充策略族与幂等 — 有值不覆盖

场景: setFieldValByName / fillStrategy / strictFillStrategy 三个方法有什么区别?
源码路径:
- `MetaObjectHandler.java:101-106` — setFieldValByName: **值非 null + hasSetter 才 set** (强制覆盖)
- `MetaObjectHandler.java:218-223` — fillStrategy: **当前值 null 才填** (有值不覆盖 — 历史 BUG 修复, CHANGELOG 1017)
- `MetaObjectHandler.java:234-242` — strictFillStrategy: 当前值 null **且 supplier 产出非 null** 才 set (双保险)
关键设计: 策略梯度 + 幂等 (q6): 每次执行都 new ParameterHandler (q1) → 同实体二次执行时字段已有值 → "有值不覆盖"保证填充幂等, 不会把已有数据冲掉。[模式: 策略梯度]
数据流: 策略选择 → 空值判断 → (值非 null) → metaObject.setValue。

### 5. SQL 侧直拼 — 必有值断言

场景: 为什么填充字段的 INSERT 语句没有 `<if>` 非空守卫?
源码路径:
- `TableFieldInfo.java:455-462` — getInsertSqlPropertyMaybeIf: `if (withInsertFill) return sqlScript;` — **无 if 直拼**
- `TableFieldInfo.java:486-493` — getInsertSqlColumnMaybeIf: 列片段同样直拼
- `TableFieldInfo.java:512-530` — getSqlSet: `if (withUpdateFill) return sqlSet;` — SET 无 if 包裹
- **FieldStrategy 优先级**: FieldFill javadoc 声明"判断优先级比 FieldStrategy 高" — 非 fill 字段走 `convertIf` (L596-608): **NEVER→null (整列不进 SQL)** / IGNORED·ALWAYS→直拼 / NOT_EMPTY+CharSequence→`!=null and !=''` / 默认→`!=null`; fill 字段直接 return sqlScript, **绕过整个 FieldStrategy** (即使 insertStrategy=NEVER 也进 SQL)
- 注入侧: `TableInfo.java:343-382` — getAllInsertSqlColumnMaybeIf/PropertyMaybeIf (MP-1 的 Insert/Update 方法类调用), 直拼列靠 `.filter(Objects::nonNull)` 排除 NEVER 返回的 null 片段
关键设计: 直拼 = 必有值断言 (q3): fill 字段列+值无条件进 SQL (保证审计列一定被写入, 优先级压过 FieldStrategy.NEVER), 值非空由执行期填充保证; 对比 version 字段 (MP-6 getVersionOli 有 if 守卫) — 两条执行期注入路线的 SQL 侧差异。[模式: 声明式必填]
数据流: withInsertFill=true → 注入 SQL 列/值直拼 → 执行期填充保证值 → DB 得到填充值。

### 6. 时序与边界 — 主键先行 / 乐观锁外层 / 逻辑删除触发

场景: 主键生成和填充谁先? 乐观锁和填充谁先? 逻辑删除为什么也会填充?
源码路径:
- `MybatisParameterHandler.java:101-106` — INSERT: `populateKeys(...)` 先 → `insertFill(...)` 后 (q7)
- `MybatisParameterHandler.java:111-126` — populateKeys: **idType.getKey() >= 3** (ASSIGN_ID=3/ASSIGN_UUID=4, 仅这两种自动生成) + assignId 已有值跳过; AUTO 交数据库, INPUT 用户自设
- 整体时序 (q7): MybatisPlusInterceptor.intercept (MP-4 L84-89: willDoUpdate→beforeUpdate) → executor.update → doUpdate → newStatementHandler → ParameterHandler 构造 → updateFill — **乐观锁先, 填充后**; version 与 fill 无代码互斥 (TableFieldInfo L215/L220 独立赋值), 实践中不标 fill — 乐观锁先写新版本值 + 填充"有值不覆盖"→ 不冲突
- `DeleteById.java:57-67` (MP-8 连接): 逻辑删除 → SqlMethod.LOGIC_DELETE_BY_ID (**UPDATE 命令**) + withUpdateFill 字段 getSqlSet(EMPTY) 直拼进 SET (排除 logicDelete 字段自身) → **逻辑删除也触发 updateFill** (CHANGELOG 93: 删除人/删除时间自动填充)
- 边界 (q8): update(wrapper) 无实体 → **官方声明不填充** (CHANGELOG 56); SELECT/DELETE 命令不处理
关键设计: 时序编排 + 隐藏触发面 (q7/q8): 填充是最内层 (ParameterHandler), 拦截器在更外层; 逻辑删除的 UPDATE 命令让删除操作也能带审计字段。[模式: 分层时序 + 命令判定]
数据流: INSERT: 拦截器(无) → Executor → populateKeys → insertFill; UPDATE: 拦截器 beforeUpdate → Executor → updateFill; 逻辑删除: UPDATE 命令 → updateFill 也触发。

### 负面空间 — 自动填充刻意不做的事

- **不填充 update(wrapper)**: 无实体可定位 (wrapper 不是实体), 官方文档标注
- **不处理 SELECT/DELETE**: 命令过滤, 查询不碰实体
- **不管 AUTO/INPUT 主键**: populateKeys 只认 ASSIGN_ID/ASSIGN_UUID (雪花/UUID 生成器), 数据库自增 (AUTO) 与手动输入 (INPUT) 由外部保证
- **不校验填充结果**: 直拼列若 handler 没给值 → DB NOT NULL 约束兜底 (无二次校验)
- **不做并发控制**: 填充是"有值不覆盖"的幂等写入, 不做锁 (对照 spring-tx)

→ 引出: 逻辑删除是另一个"执行期改 SQL"的机制 — DELETE 转 UPDATE, 与填充共享 UPDATE 命令触发面 → [[MP-8-logic-delete]]
