# MP-7 自动填充 — 知识规划 (knowledge-planning)

> 项目: MyBatis-Plus | 🟡 Working / 1 篇 | MetaObjectHandler(243)+StrictFill(52)+MybatisParameterHandler(223)+MybatisXMLLanguageDriver
> 基线: MP-PLAN MP-7 — 前置: **MP-2 (withInsertFill/withUpdateFill) + M-2 (ParameterHandler 时序) + M-6 (LanguageDriver 扩展点)** — 展开 执行入口→提取守门→严格匹配→策略族→SQL直拼→时序边界

---

## §0.8

- 🟡 Working，1篇 — 入口(**MybatisConfiguration L94 setDefaultDriverClass 注册默认驱动 → MybatisXMLLanguageDriver L45-46 覆写 createParameterHandler → MybatisParameterHandler 构造 L65 super 存参后 L69 processParameter**; MyBatis 每次执行 new StatementHandler→newParameterHandler BaseStatementHandler.java:70/SimpleExecutor.java:48,62,75) → 过滤(**非 null+非 SimpleType+INSERT/UPDATE L72-79**) → 提取(**extractParameters 四分支: Collection/数组/Map 值展开+objectSet 去重/实体 L188-221; 值若为 Map 找 et 键 L85-94**) → 守门(**openInsertFill×2+isWithInsertFill 三重 L128-142; 3.5.6 MappedStatement 签名 cd0238821**) → 严格匹配(**strictFill 三条件: property+fieldType 精确+fill 标记 L195-207; 泛型 <T,E extends T> 子类值; 基本类型 int 不匹配**) → 策略族(**setFieldValByName 值非 null L101-106 / fillStrategy 空才填 L218-223 / strictFillStrategy 双保险 L234-242 — 幂等**) → SQL 直拼(**withInsertFill/UpdateFill 无 if 标签 TableFieldInfo L455-462/486-493/512-530; FieldStrategy 优先级: fill 绕过 convertIf L596-608 (NEVER→null 整列不进), FieldFill javadoc 声明优先级更高; TableInfo L343-382**) → 时序边界(**INSERT: populateKeys(ASSIGN_ID/UUID getKey>=3 L111-126) 先 fill 后 L101-106; 拦截器 beforeUpdate 外层 MP-4 L84-89; version/fill 无互斥靠有值不覆盖; 逻辑删除 UPDATE 命令触发 updateFill DeleteById L57-67; update(wrapper) 不填充 CHANGELOG 56**)
- 设计模式: [模式: 扩展点 hook(语言驱动覆写)+白名单严格匹配+策略梯度+声明式必填(SQL 直拼)]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MybatisXMLLanguageDriver.java:45-46; BaseStatementHandler.java:70 | 入口 | LanguageDriver 覆写 createParameterHandler → 构造时 processParameter — 每次 SQL 执行 new ParameterHandler | High |
| MybatisParameterHandler.java:72-79 | 过滤 | 非 null + 非 SimpleType + INSERT/UPDATE 命令才处理 | High |
| MybatisParameterHandler.java:188-221 | 提取 | extractParameters 四分支; Map 值展开 + objectSet 去重 (重入修复 ae5592621) | High |
| MybatisParameterHandler.java:128-142 | 守门 | openInsertFill 双签名 + isWithInsertFill 三重守门; ms 参数 3.5.6 按 id 跳过 | High |
| MetaObjectHandler.java:195-207 | 严格匹配 | strictFill 三条件 (property+fieldType.equals+fill 标记) findFirst | High |
| StrictFill.java:31-51 | 泛型 | <T, E extends T> 子类值; of() 直值/Supplier 双工厂 | High |
| MetaObjectHandler.java:101-106,218-223,234-242 | 策略族 | setFieldValByName/fillStrategy/strictFillStrategy 梯度; 有值不覆盖=幂等 | High |
| TableFieldInfo.java:455-462,486-493,512-530 | SQL 直拼 | withInsertFill/withUpdateFill 无 if 标签 — 必有值断言 (对照 version if 守卫) | High |
| MybatisParameterHandler.java:101-126 | 主键 | populateKeys: idType.getKey()>=3 (ASSIGN_ID/UUID) 才生成; AUTO/INPUT 不管 | High |
| DeleteById.java:57-67; CHANGELOG:56,93 | 边界 | 逻辑删除 UPDATE 命令触发 updateFill; update(wrapper) 不填充; SELECT 不处理 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 自动填充是单机制闭环 (hook→提取→守门→匹配→策略→SQL), 1篇 (~70行) 按"入口→提取守门→严格匹配→策略族→SQL直拼→时序边界"展开; withInsertFill 衔接 MP-2 (引用), LanguageDriver 衔接 M-6 (引用), 逻辑删除衔接 MP-8 (桥)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | LanguageDriver 覆写 + ParameterHandler 构造时 hook | 🔴 | **为什么🔴**: 执行入口 |
| P1-2 | strictFill 三条件严格匹配 + 泛型守卫 | 🔴 | **为什么🔴**: 填充正确性 |
| P1-3 | fill SQL 直拼 (必有值断言, 对照 version if 守卫) | 🔴 | **为什么🔴**: SQL 侧设计 |
| P2-1 | extractParameters 提取链 + objectSet 重入 | 🟡 | **为什么🟡**: 参数形态适配 |
| P2-2 | 三重守门 (open 双签名 + isWithInsertFill) | 🟡 | **为什么🟡**: 控制面 |
| P2-3 | 策略族梯度 + 幂等 (有值不覆盖) | 🟡 | **为什么🟡**: 填充语义 |
| P3-1 | 时序 (主键先行/乐观锁外层) + 逻辑删除触发 (MP-8 桥) | 🟢 | **为什么🟢**: 编排与边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **执行入口** | 🔴 | hook 机制 |
| B | **严格匹配与 SQL 直拼** | 🔴 | 正确性+SQL 设计 |
| C | **提取/守门/策略** | 🟡 | 适配与控制 |
| D | **时序与边界** | 🟡 | 编排 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 执行时机 | LanguageDriver 覆写 createParameterHandler → MybatisParameterHandler 构造: super 存参后 processParameter (先于 setParameters); 每次 SQL 执行 new ParameterHandler 必触发 | MybatisXMLLanguageDriver.java:45-46; BaseStatementHandler.java:70; MybatisParameterHandler.java:64-70 |
| q2 | 严格匹配 | strictFill 三条件: property 同名 + fieldType 精确 (Class.equals) + fill 标记, findFirst; 泛型 <T,E extends T> 值可为子类; 表模型外的字段 (testType1) 静默不填 | MetaObjectHandler.java:195-207; StrictFill.java:31-51 |
| q3 | SQL 直拼 | withInsertFill/withUpdateFill 字段列+值无 if 标签直拼进 INSERT/SET — 必有值断言; **优先级压过 FieldStrategy** (FieldFill javadoc 声明; convertIf NEVER→null 整列不进, fill 字段绕过); 对照 version 字段 if 守卫 | TableFieldInfo.java:455-462,486-493,512-530,596-608; TableInfo.java:343-382 |
| q4 | 提取链 | extractParameters 四分支 (Collection/数组/Map 值展开/实体); Map 值全展开 + objectSet 去重防重入 (I8506T/I82VLI) | MybatisParameterHandler.java:188-221 |
| q5 | 守门 | 三重: openInsertFill() 旧签名 AND openInsertFill(ms) (3.5.6 按 MappedStatement.id 跳过) AND tableInfo.isWithInsertFill (表级聚合) | MybatisParameterHandler.java:128-142; MetaObjectHandler.java:43-78 |
| q6 | 策略族 | setFieldValByName (值非 null 强写) / fillStrategy (空才填) / strictFillStrategy (空+新值非 null); 有值不覆盖 = 幂等基石 (每次执行都 new 也无副作用) | MetaObjectHandler.java:101-106,218-223,234-242 |
| q7 | 时序 | INSERT: populateKeys (getKey>=3 即 ASSIGN_ID/UUID) 先, insertFill 后; 整体: 拦截器 beforeUpdate (乐观锁) 外层 → ParameterHandler updateFill 内层; version 不参与 fill | MybatisParameterHandler.java:101-126; MybatisPlusInterceptor.java:84-89 |
| q8 | 边界 | 逻辑删除 (UPDATE 命令) 触发 updateFill + withUpdateFill 字段直拼 SET (DeleteById); update(wrapper) 无实体不填充 (官方声明); SELECT/DELETE 不处理 | DeleteById.java:57-67; MybatisParameterHandler.java:72-79 |
| q9 | MybatisUtils 澄清 | MybatisUtils 与填充无关 (JSON handler/代理提取, 属 MP-2/MP-9) — HANDOFF 提示误导, 已修正认知 | MybatisUtils.java:38-109 |

→ 引出 MP-8: 逻辑删除与填充共享 UPDATE 命令触发面 — deleteById 自动带审计字段 → [[MP-8-logic-delete]]
