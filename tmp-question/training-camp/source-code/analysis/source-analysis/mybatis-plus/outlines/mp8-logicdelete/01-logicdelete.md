# MP-8 逻辑删除 — @TableLogic: DELETE 转 UPDATE, 删除也能带审计

> 前置: [[MP-2-metadata]] (TableInfo/TableFieldInfo 元数据) + [[MP-7-fill]] (updateFill 触发/直拼连接) + [[MP-1-injector]] (方法类模板) | 对照: [[MP-6-optimistic]] (fail-fast 同款 Assert) | 复用: [[MP-7-fill]] (执行期注入族两翼) | 收束: [[MP-9-mapper-service]] (全方法面汇聚)
> 🟡 Working | 8 KP | [模式: 双路径注入+双值配置+框架专属字段管理]
> Pass 2 闭环: q1(双路径) q2(双值) q3(查询三路) q4(更新防护) q5(填充协同) q6(批量) q7(聚合) q8(边界)

**读者处境**: deleteById 之后记录还在表里, 查询也自动带过滤 — 这就是逻辑删除: 不真删, 只打标记。这篇拆 @TableLogic: 删除怎么变成 UPDATE?deleted 标记谁维护?为什么查询/更新会自动带条件, 而手写 SQL 不会?删除操作为什么能自动填充"删除人"?

### 1. 元数据与双值 — 一个注解, 两个值

场景: @TableLogic 没写参数也能用?删除值和未删除值从哪来?
源码路径:
- `TableFieldInfo.java:406-430` — initLogicDelete 双路径: 有 @TableLogic → `value()` 非空用注解未删除值, 空则取全局 `logicNotDeleteValue`; `delval()` 同理取删除值; **类中无注解时** → 全局 `dbConfig.getLogicDeleteField()` 属性名匹配兜底
- `GlobalConfig.java:180-186` — DbConfig: **logicDeleteValue="1" (默认已删除) / logicNotDeleteValue="0" (默认未删除)** / logicDeleteField (全局属性名, 默认 null)
- 组合注解穿透: @MyTableLogic(value="false", delval="true") → 值断言 (TableInfoHelperTest:104-141, MP-2 AnnotationUtils)
- `TableInfo.java:493-512` — setFieldList 聚合: logicDeleteFieldInfo 唯一 + **Assert<=1 fail-fast** ("@TableLogic not support more than one")
关键设计: 双值三级来源 (q2/q7): 注解 > 全局 > 默认 "0"/"1" — 注解不写参数也能用全局默认; 一个表只允许一个逻辑删除字段 (注入期抛错, 与 @Version 同款)。[模式: 三级配置 + 单字段特权]
数据流: @TableLogic / 全局配置 → initLogicDelete → logicDelete=true + 双值 → setFieldList 聚合 logicDeleteFieldInfo。

### 2. SQL 片段双语义 — 一个方法, 两副面孔

场景: 同一个 getLogicDeleteSql 怎么既生成删除 SET 又生成查询条件?
源码路径:
- `TableInfo.java:437-448` — getLogicDeleteSql(startWithAnd, isWhere): withLogicDelete 才产出; startWithAnd → 前缀 " AND "
- `TableInfo.java:450-470` — formatLogicDeleteSql(isWhere): **isWhere=true → 取 logicNotDeleteValue (未删除值)** — 查询条件 (`deleted=0` / `deleted IS NULL`); **isWhere=false → 取 logicDeleteValue (删除值)** — 删除 SET (`deleted=1`)
- **"NULL" 字符串特例**: value 忽略大小写等于 "NULL" → 查询 `column IS NULL` / 删除 `column=NULL` (CHANGELOG 466; H2 测试 logicNotDeleteValue="NULL" 实证)
- charSequence → `'%s'` 单引号包裹; 其他 (数字/函数) → 裸值 — H2 测试 logicDeleteValue="NOW()" → `SET deleted=NOW()`
- **源码 javadoc 自证矛盾**: formatLogicDeleteSql 的 javadoc 写 "isWhere true: logicDeleteValue" — **与实现 (true→logicNotDeleteValue) 正好相反**, 命名反直觉到连作者注释都写反了 (TableInfo.java:450-454)
关键设计: isWhere 反直觉命名 (q3): "isWhere" = 是否作为查询条件 — 查询条件取的是**未删除值** (WHERE deleted=0), 删除 SET 取的是**删除值**。字符串 "NULL" 渲染成 SQL NULL 语义, 支持"未删除=NULL 空值"模型。[模式: 双语义片段 + 特殊值渲染]
数据流: 调用方 (startWithAnd, isWhere) → getLogicDeleteSql → formatLogicDeleteSql → 未删除/删除值 SQL 片段。

### 3. 方法类双路径 — 一个 delete, 两种删除

场景: deleteById 怎么变成 UPDATE?物理删除和逻辑删除怎么切换?
源码路径:
- `Delete.java:47-62` — `if (tableInfo.isWithLogicDelete())` → SqlMethod.LOGIC_DELETE ("UPDATE %s %s %s %s") + sqlLogicSet + sqlWhereEntityWrapper → **addUpdateMappedStatement (UPDATE 命令)**; else → SqlMethod.DELETE ("DELETE FROM...") → **addDeleteMappedStatement (DELETE 命令)**
- `DeleteById.java:57-79` / `DeleteByMap.java:46-63` — 同构双路径
- `AbstractMethod.java:106-108` — sqlLogicSet: `"SET " + tableInfo.getLogicDeleteSql(false, false)` — SET deleted=1
- 命令类型切换的连锁: MappedStatement.sqlCommandType=UPDATE → 触发 MybatisParameterHandler.updateFill (MP-7 q8)
关键设计: 注入期动态二选一 (q1): 同一方法类按表的 isWithLogicDelete 生成 UPDATE 或 DELETE, **命令类型同步切换** — 物理/逻辑删除在 SQL 层共存, 切换点是一个布尔值; UPDATE 命令是填充触发的根源。[模式: 注入期双路径]
数据流: isWithLogicDelete=true → LOGIC_DELETE 模板 → SET deleted=1 + WHERE 条件 → addUpdateMappedStatement (UPDATE)。

### 4. 查询三路 — 未删除条件怎么进每一条查询

场景: selectById/selectList/selectCount 都自动过滤已删除记录, 条件从哪进 SQL?
源码路径:
- 路 1 主键尾缀: `SelectById.java:50` / `SelectBatchByIds:51` — `getLogicDeleteSql(true, true)` 拼在 WHERE 尾部 → `WHERE id=#{id} AND deleted=0`
- 路 2 wrapper 路径: `AbstractMethod.java:243-246` — 逻辑删除分支: `convertWhere(getLogicDeleteSql(false, true) + NEWLINE + sqlScript)` — 未删除条件包在 `<where>` 最前 (SelectList/SelectCount/Update/Delete 共用)
- 路 3 字段排除: `TableInfo.java:395-402` — getAllSqlWhere(ignoreLogicDelFiled=true) → 过滤 deleted 字段自身, 不生成普通 `<if>` 条件 — **条件由 getLogicDeleteSql 统一注入, 避免双写**
关键设计: 三路归一 (q3): 主键查询尾缀 / wrapper 路径 convertWhere / 普通 where 排除自身 — deleted 字段对用户 SQL 面不可见, 未删除条件框架独占管理。[模式: 统一注入 + 字段隐身]
数据流: 注入方法 → SelectById (尾缀) / sqlWhereEntityWrapper (convertWhere) / getAllSqlWhere (排除) → 每条查询含 AND deleted=0。

### 5. 更新防护 — 更新已删记录不生效

场景: updateById 会改到已删除的记录吗?deleted 字段会被用户更新覆盖吗?
源码路径:
- `UpdateById.java:48-51` — `additional = optlockVersion(tableInfo) + tableInfo.getLogicDeleteSql(true, true)` → WHERE id=#{id} **AND version=#{...} AND deleted=0** — 乐观锁条件在前, 逻辑删除条件在后 (组合场景顺序); 更新已删记录影响行数 0
- `AbstractMethod.java:120-130` — sqlSet(logic): `getAllSqlSet(logic=true)` → **SET 排除 deleted 字段** (表配逻辑删除时); Update.java:45 sqlSet(true, true) 恒排除
- `TableInfo.java:416-430` — getAllSqlSet(ignoreLogicDelFiled): filter 排除逻辑删除字段
- 全表更新协同: BlockAttackInnerInterceptor (MP-5) 拦"无 where 更新" — 逻辑删除表的 update 仍会拼 AND deleted=0 (d694f104b: 逻辑删除字段默认 null 时守卫不失效)
关键设计: 双防护 (q4): WHERE 注入未删除条件 (更新已删不生效) + SET 恒排除 deleted (防用户实体覆盖删除标记) — 删除标记由框架独占, 与查询 (节 4) 同一设计哲学。[模式: 专属字段双路防护]
数据流: updateById(et) → WHERE id=#{id} AND deleted=0 + SET(无 deleted) → 已删记录行数 0。

### 6. 填充协同与批量 — 删除带审计, 批量双形态

场景: 逻辑删除为什么能自动填"删除人"?批量删除怎么同时支持 id 集合和实体集合?
源码路径:
- `DeleteById.java:57-67` — withUpdateFill 字段 `getSqlSet(EMPTY)` 直拼进 SET (**filter 排除 logicDelete 自身** — deleted 即使标 fill 也不进) + **`!isSimpleType(_parameter)` 运行时守卫** (L64 convertIf) — **参数是简单类型 (deleteById(1L)) 时填充 SET 不生成, 只有 SET deleted=1**; 实体参数才生成填充 SET
- 触发面: UPDATE 命令 → MybatisParameterHandler.updateFill (MP-7) — 简单类型参数在 processParameter 最外层已被过滤 (MP-7 q1), 双重一致: **deleteById(id) 不填充 / deleteById(et) 填充**
- `LogicDelTest.java:83-94,127` — 实证: deleteById(et) 后 deleteBy = "聂秋秋" (L94 断言, L127 strictUpdateFill 配置); deleted (@TableLogic+@TableField(fill=UPDATE) 双注解) 不进 SET
- `DeleteByIds.java:84-100` — 批量: foreach **运行时双形态判定**: `SimpleTypeRegistry.isSimpleType(item)` → `#{item}` 直接用值; 实体 → `#{item.id}` 取主键; 实体集合+withUpdateFill → 实体删除转换 (4d5e4e45a: 主键类型必须匹配)
关键设计: 隐藏触发面 + 双形态 (q5/q6): DELETE 转 UPDATE 让删除操作自动携带审计字段 (MP-7 两翼); 但**只有实体参数形态才有填充** (简单类型 id 删除无实体可填 — SQL 守卫 + 填充过滤双重一致); 批量 foreach 一次注入服务两种参数形态。[模式: 触发面复用 + 运行时形态判定]
数据流: deleteById(et) → UPDATE 命令 → updateFill 填 deleteBy → SQL: SET deleteBy=#{...}, deleted=1 WHERE id=#{id} AND deleted=0。

### 负面空间 — 逻辑删除刻意不做的事

- **不转换手写 SQL**: mapper.xml/自定义 SQL 不做逻辑删除改造 (CHANGELOG 682) — 保护面只在注入方法内
- **不做物理删除共存**: 一个表要么逻辑要么物理 (isWithLogicDelete 布尔切换) — 节 3 的"共存"指不同表可分别选择, 同表绝不混合
- **不处理 INSERT**: 插入不涉及删除标记 (DB 默认值/NULL 由建表语句保证)
- **不填充 id 参数删除**: deleteById(1L) 无实体可填 (简单类型), 只有实体参数删除带审计字段
- **值不参数化**: formatLogicDeleteSql 用 String.format 直拼值 — 无 #{} 参数化, SQL 注入面由"值只来自注解/全局配置 (信任配置方)"封口
- **不自动迁移数据**: 已删除数据永久保留, 无清理机制 (对照真实物理删除)
- **不做多表级联**: 逻辑删除只作用于本表, 关联表数据不受影响

→ 收束: 逻辑删除与乐观锁 (MP-6)、自动填充 (MP-7) 构成 MP 执行期注入三件套 — 全部汇聚到 BaseMapper 19 方法面 (MP-9)。MP 阶段 9/9 完成。
