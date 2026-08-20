# MP-8 逻辑删除 — completeness-questions

## 开发者视角

1. @TableLogic 不写 value/delval 参数时, 删除值和未删除值从哪来?
2. deleteById 后记录还在表里吗? 查询为什么查不到它?
3. deleted 字段能自己写 SQL 条件吗? 框架怎么防止条件重复?
4. updateById 能更新已逻辑删除的记录吗? 影响行数多少?
5. 批量删除 deleteByIds 传实体集合和 id 集合有什么区别?
6. 逻辑删除时"删除人/删除时间"怎么自动填充? 需要额外配置吗?
7. 表里有两个 @TableLogic 字段会怎样?
8. 逻辑删除字段的值可以用 "NULL" 字符串吗? 生成的 SQL 长什么样?

## 架构师视角

9. 方法类双路径 (isWithLogicDelete → UPDATE/DELETE) 的设计取舍? 为什么不做运行期切换?
10. getLogicDeleteSql 的 isWhere 参数为什么命名反直觉? 双语义片段的设计动机?
11. 查询三路注入 (尾缀/convertWhere/排除自身) — 为什么不能统一成一路?
12. SET 恒排除 deleted + WHERE 注入未删除条件 — "框架专属字段"设计哲学?
13. 逻辑删除触发 updateFill (MP-7 连接) — 这是设计意图还是副作用? 怎么保证 deleted 自身不被填充覆盖?
14. 与物理删除的共存面: 一个项目里能不能同时有逻辑删除表和物理删除表?
15. 手写 SQL/mapper.xml 为什么不转换? 这个边界是故意的吗?
16. 全表更新拦截 (BlockAttack) 与逻辑删除条件怎么协同?

## 学生视角

17. deleteById(1) 的完整 SQL 变换: 从 DELETE 到 UPDATE 经历了哪些代码?
18. 查询自动带 AND deleted=0 — selectById 和 selectList 的条件分别从哪进 SQL?
19. 逻辑删除字段的 charSequence 值为什么要加单引号? "NULL" 为什么特殊?
20. 逻辑删除、乐观锁、自动填充三件套的执行时序 (deleteById 场景)?
