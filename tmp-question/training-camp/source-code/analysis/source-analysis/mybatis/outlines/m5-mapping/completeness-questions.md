# M-5 参数/结果映射 — completeness-questions

## 开发者视角

1. #{id} 的参数值怎么取的?foreach 的 __frch_ 参数和普通参数谁优先?为什么?
2. #{id} 传 null 时预编译怎么绑定?jdbcTypeForNull 是什么?
3. 数据库列 user_name 怎么自动映射到 userName?开关在哪?autoMappingBehavior 三种取值区别?
4. 实体类没有默认构造函数时怎么实例化?@AutomapConstructor 是干什么的?
5. resultMap 里没声明的列会自动映射吗?嵌套 resultMap 场景自动映射有什么区别?
6. callSettersOnNulls 控制什么?为什么默认 false?
7. 数据库返回的列名在结果集里不存在(如 SQL 别名写错)会怎样?AutoMappingUnknownColumnBehavior 三态?
8. 嵌套查询(association select=)什么时候执行?lazy 是什么意思?
9. 一个存储过程返回多个 ResultSet 怎么映射?resultSets 属性怎么用?

## 架构师视角

10. DefaultParameterHandler 取参四路的设计: 为什么 additionalParameter 最优先?与 M-2 createCacheKey 取参逻辑的一致性意味着什么?
11. handleRowValues 双路径分流 + ensureNoRowBounds/checkResultHandler 安全守卫 — 为什么嵌套映射禁止 RowBounds/自定义 ResultHandler?safeXxxEnabled 的语义?
12. createResultObject 的四分支降级链 (原始值→构造器→默认构造→自动构造器) 设计意图是什么?全失败时抛什么?
13. 嵌套映射的 combinedKey 聚合 + partialObject 复用 + ancestorObjects 循环引用保护 — 一父多子/自引用分别怎么处理?
14. 自动映射缓存 (autoMappingsCache 按 resultMapId:columnPrefix) 解决什么问题?列名→属性推断为什么只做一次?
15. TypeHandlerRegistry 在 M-1 注册、在 M-5 消费 — 注册表设计如何支撑"列值→Java 类型"的双向转换?
16. ResultSetWrapper 的 mapped/unmapped 列名缓存与 ResultMap 的关系?useColumnLabel 的作用?

## 学生视角

17. 一次 selectList 从 SQL 结果到 List<User> 的完整数据流 (handleResultSets→handleRowValues→getRowValue→createResultObject→双映射→storeObject)?
18. skipRows 在 FORWARD_ONLY 和非 FORWARD_ONLY 结果集上分别怎么实现?为什么?
19. discriminated resultMap 是什么?resolveDiscriminatedResultMap 什么时候调用?
20. 嵌套结果 (nested result maps) 和嵌套查询 (nested query) 的区别?延迟加载只对哪种生效?
21. foundValues 语义: 一行全 null 时返回什么?returnInstanceForEmptyRow 控制什么?
