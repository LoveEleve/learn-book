# MP-8 逻辑删除 — 知识规划 (knowledge-planning)

> 项目: MyBatis-Plus | 🟡 Working / 1 篇 | TableInfo.logicDeleteSql(437-470)+TableFieldInfo.initLogicDelete(406-430)+Delete 方法类族+AbstractMethod.sqlLogicSet(106-108)+GlobalConfig.DbConfig(180-186)
> 基线: MP-PLAN MP-8 — 前置: **MP-2 (TableInfo/TableFieldInfo 元数据) + MP-7 (updateFill 触发/直拼连接) + MP-1 (注入器)** — 展开 元数据标记→SQL 片段→方法类双路径→查询三路→更新防护→填充协同→批量→边界

---

## §0.8

- 🟡 Working，1篇 — 元数据(**@TableLogic(value/delval) + 全局 DbConfig 兜底 L180-186 (默认 0/1) → TableFieldInfo.initLogicDelete L406-430 双路径: 注解优先/全局 logicDeleteField 属性名兜底 → TableInfo.setFieldList 聚合 logicDeleteFieldInfo+Assert<=1 L493-512**) → SQL 片段(**getLogicDeleteSql(startWithAnd,isWhere) L437-448: isWhere=true→未删除值(查询)/false→删除值(SET); formatLogicDeleteSql L450-470: "NULL"→IS NULL/=NULL 特例 + charSequence 引号**) → 方法类双路径(**Delete/DeleteById/DeleteByMap/DeleteByIds: isWithLogicDelete→LOGIC_DELETE(UPDATE)+addUpdateMappedStatement / 否则物理 DELETE+addDeleteMappedStatement — 命令类型切换**) → 查询三路(**SelectById/SelectBatchByIds 尾缀 AND deleted=0 L50; sqlWhereEntityWrapper convertWhere 包裹 L243-246; getAllSqlWhere ignoreLogicDelFiled 排除 deleted 自身 L395-402**) → 更新防护(**UpdateById additional=optlock+getLogicDeleteSql(true,true) L48-51 → WHERE AND deleted=0; sqlSet(logic) 恒排除 deleted L120-130**) → 填充协同(**DeleteById L57-67 withUpdateFill 直拼 SET (filter 排除 logicDelete 自身) + UPDATE 命令触发 updateFill — 删除人/时间自动填充, LogicDelTest deleteBy 实证**) → 批量(**DeleteByIds foreach 双形态: SimpleType→#{item}/实体→#{item.id} L84-100**) → 边界(**手写 SQL 不转换 CHANGELOG 682; 全表更新拦截协同 d694f104b; 实体删除主键类型匹配 4d5e4e45a**)
- 设计模式: [模式: 双路径注入(物理/逻辑切换)+双值配置+框架专属字段管理(排除自身)]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TableFieldInfo.java:406-430; GlobalConfig.java:180-186 | 元数据 | 双值三级来源: 注解 value/delval > 全局 DbConfig > 默认 "0"/"1"; 全局属性名兜底仅类中无注解 | High |
| TableInfo.java:437-470 | SQL 片段 | getLogicDeleteSql 双语义: isWhere=true→未删除值(查询)/false→删除值(SET); "NULL"→IS NULL/=NULL; charSequence 引号 | High |
| Delete.java:47-62; DeleteById.java:57-79; DeleteByMap.java:46-63 | 双路径 | isWithLogicDelete → UPDATE(逻辑)/DELETE(物理) + 命令类型切换 (addUpdate/DeleteMappedStatement) | High |
| SelectById.java:50; AbstractMethod.java:243-246; TableInfo.java:395-402 | 查询三路 | 主键尾缀 AND deleted=0 / wrapper convertWhere / 排除 deleted 自身 (不生成普通条件) | High |
| UpdateById.java:48-51; AbstractMethod.java:120-130 | 更新防护 | WHERE AND deleted=0 (更新已删不生效) + SET 恒排除 deleted (防覆盖删除标记) | High |
| DeleteById.java:57-67; DeleteByIds.java:80-100 | 填充协同 | withUpdateFill 直拼 SET (filter 排除 logicDelete) + UPDATE 命令触发 updateFill | High |
| DeleteByIds.java:84-100 | 批量 | foreach 双形态: SimpleType→#{item} / 实体→#{item.id}; 实体集合删除转换 | High |
| TableInfo.java:493-512 | 聚合 | logicDeleteFieldInfo 唯一化 + Assert<=1 fail-fast (同 @Version 模式) | High |
| CHANGELOG:682,264; 4d5e4e45a | 边界 | 手写 SQL 不转换; 全表更新拦截协同; 实体删除主键类型匹配 | Medium |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 逻辑删除是单机制闭环 (元数据→SQL 片段→注入双路径→查询/更新注入→填充协同), 1篇 (~60行) 按"元数据与双值→SQL 片段语义→方法类双路径→查询三路→更新防护→填充协同与批量→边界"展开; 与 MP-7 是执行期注入族的两翼 (引用+桥)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 方法类双路径 (isWithLogicDelete → UPDATE/DELETE + 命令切换) | 🔴 | **为什么🔴**: 核心机制 |
| P1-2 | getLogicDeleteSql 双语义 + 值渲染 (NULL 特例/引号) | 🔴 | **为什么🔴**: SQL 生成 |
| P1-3 | 查询三路注入 + deleted 排除自身 | 🔴 | **为什么🔴**: 查询面 |
| P2-1 | 更新防护 (WHERE deleted=0 + SET 排除) | 🟡 | **为什么🟡**: 一致性 |
| P2-2 | 填充协同 (withUpdateFill 直拼 + updateFill 触发) | 🟡 | **为什么🟡**: MP-7 连接 |
| P2-3 | 批量双形态 (SimpleType/实体) | 🟡 | **为什么🟡**: 批量面 |
| P3-1 | 双值三级来源 + fail-fast | 🟢 | **为什么🟢**: 配置面 |
| P3-2 | 边界: 手写 SQL/全表更新/类型匹配 | 🟢 | **为什么🟢**: 负面空间 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **双路径注入** | 🔴 | 机制核心 |
| B | **SQL 片段与查询注入** | 🔴 | SQL 设计 |
| C | **更新/填充/批量** | 🟡 | 协同面 |
| D | **配置与边界** | 🟡 | 控制面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双路径 | 同一方法类 isWithLogicDelete 动态二选一: LOGIC_DELETE (UPDATE)+addUpdateMappedStatement vs 物理 DELETE+addDeleteMappedStatement — 命令类型切换是填充触发的根源 | Delete.java:47-62; DeleteById.java:57-79 |
| q2 | 双值渲染 | value/delval 三级来源 (注解>全局>默认 0/1); "NULL" 字符串→IS NULL (查)/=NULL (删); charSequence→引号; 全局可注入函数 NOW() | TableFieldInfo.java:406-430; GlobalConfig.java:180-186; TableInfo.java:450-470 |
| q3 | 查询三路 | isWhere=true 取未删除值 (WHERE deleted=0); 三路: SelectById 尾缀 / sqlWhereEntityWrapper convertWhere / getAllSqlWhere 排除 deleted 自身 — 避免条件双写 | TableInfo.java:437-470; AbstractMethod.java:243-246; SelectById.java:50 |
| q4 | 更新防护 | updateById: additional 含 AND deleted=0 (更新已删行数 0); SET 恒排除 deleted (logic 参数路由) — 删除标记框架独占 | UpdateById.java:48-51; AbstractMethod.java:120-130 |
| q5 | 填充协同 | 逻辑删除 = 填充隐藏触发面: UPDATE 命令→updateFill + withUpdateFill 直拼 SET (filter 排除 logicDelete) — 但仅实体参数: `!isSimpleType(_parameter)` SQL 守卫 (L64) + 填充过滤双重一致, deleteById(id) 不填充/deleteById(et) 填充 (LogicDelTest deleteBy 实证) | DeleteById.java:57-67; LogicDelTest.java:83-94,127 |
| q6 | 批量 | DeleteByIds foreach 双形态: SimpleType→#{item} / 实体→#{item.id} (运行时判定); 实体集合+withUpdateFill → 实体删除转换填充 | DeleteByIds.java:84-100 |
| q7 | 聚合 | logicDeleteFieldInfo 唯一化 (Assert<=1 fail-fast, 同 @Version); 全局 logicDeleteField 兜底仅类中无注解 | TableInfo.java:493-512; TableFieldInfo.java:406-430 |
| q8 | 边界 | 保护面仅注入方法: 手写 SQL 不转换 (682); 全表更新拦截协同 (d694f104b); 实体删除主键类型匹配约束 (4d5e4e45a) | CHANGELOG.md:682,264 |

→ 引出: MP-9 已交付 (BaseMapper 汇聚), MP 阶段收官; 逻辑删除与乐观锁/填充构成 MP 执行期注入三件套。

（说明: MP-8 是 MP 最后一个域, 无引出 — 收束回 MP-9 全方法面 + 阶段3.5 Redis 域重审。REVIEW 注: formatLogicDeleteSql javadoc 与实现矛盾 (写反), 大纲节 2 已记）
