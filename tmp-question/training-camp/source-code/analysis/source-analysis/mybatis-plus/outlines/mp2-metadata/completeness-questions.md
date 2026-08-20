# MP-2 表元数据解析 — completeness-questions

## 开发者视角

1. 实体类没有 @TableId 注解, 为什么 `selectById` 会报错而 `selectList` 正常? 哪个字段会被自动当成主键?
2. `@TableName("xxx")` 配了全局 `table-prefix: ttt_`, 表名到底是什么? `keepGlobalPrefix=true` 有什么影响?
3. 为什么 `@TableField("name")` 的字段 `userName` 生成的 SQL 里是 `name AS userName`? 什么条件下会加 AS?
4. `@TableField(fill = FieldFill.INSERT)` 的字段为什么 insert 时 null 也会拼进 SQL? 和 insertStrategy 是什么关系?
5. 类上有 `@TableLogic` 且全局配置了 `logic-delete-field`, 哪个生效? 为什么?
6. 继承 BaseEntity(id, createTime) 的实体, 字段是怎么被收集的? 子类重写同名属性会怎样?
7. 两个字段都标 `@Version` 会发生什么? 为什么 MP 选择启动时抛异常?
8. `@TableName(autoResultMap = true)` 什么时候需要开? 指定了 typeHandler 却不生效是什么原因?
9. 多数据源项目中同一个实体类为什么会被解析两次? 重解析时旧缓存条目会怎样?

## 架构师视角

10. TableInfoHelper 的两级缓存(Class→/tableName→)与 Configuration 比较机制, 解决的是什么架构问题? 为什么不用 Configuration 作缓存 key?
11. AnnotationHandler 设计成 SPI 接口有什么好处? 组合注解递归解析的防环是怎么做的?
12. PostInitTableInfoHandler 的三个钩子(creteTableInfo/postTableInfo/postFieldInfo)分别在管线的什么位置? 能扩展哪些场景?
13. FieldStrategy(NEVER/IGNORED/ALWAYS/NOT_NULL/NOT_EMPTY/DEFAULT, 注解 DEFAULT=跟随全局)在 SQL 生成期的语义是什么? 与 MyBatis 执行期 if 标签的关系?
14. checkRelated 判定与 resultMap 三态(显式/自动/无)如何协作? 为什么二者互斥?
15. 主键/逻辑删除/版本的 fail-fast 校验放在哪里? 为什么选择启动期失败而不是运行时?
16. TableInfo 聚合了 withLogicDelete/withVersion/withInsertFill 等布尔标记, 对 MP-6/7/8 插件体系意味着什么(导航)?

## 学生视角

17. 表名解析的完整管线(SimpleName→下划线→首字母小写→前缀→format→schema)每一步对应哪个配置?
18. getKeyInsertSqlProperty 中 IdType.AUTO 为什么单条用 convertIf、批量返回空?
19. LambdaUtils.installCache 缓存了什么结构? ColumnCache 的 sqlSelect 与 MP-3 取列名有什么关系(导航)?
20. 无主键时 `havePK()` 返回什么? MP-1 的 xxById 注入如何受它影响(导航)?
21. camelToUnderline 的算法规则是什么("userId"→"user_id")? 连续大写("userID")会怎样?
