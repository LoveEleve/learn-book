# MP-7 自动填充 — completeness-questions

## 开发者视角

1. @TableField(fill = FieldFill.INSERT) 后, insertFill 到底被谁调用? 在 SQL 执行的哪个环节?
2. 为什么每次 SQL 执行都会触发填充? 会不会填两次?
3. strictInsertFill 和 setFieldValByName 有什么区别? 什么时候用哪个?
4. strictInsertFill 填错类型会怎样? 填一个实体里没有的字段会怎样?
5. update(wrapper) 更新时能填充吗? 为什么?
6. 批量插入 (saveBatch/list/数组) 填充怎么工作?
7. openInsertFill() 和 openInsertFill(MappedStatement) 的区别? 3.5.6 升级后要改吗?
8. 填充字段的 INSERT 语句为什么没有 if 非空守卫? 忘了配 handler 会怎样? insertStrategy=NEVER 时 fill 字段还会进 SQL 吗 (优先级)?

## 架构师视角

9. 为什么用 LanguageDriver 扩展点换 ParameterHandler, 而不是改 Executor 或加拦截器? 取舍是什么?
10. 填充 hook 放在 ParameterHandler 构造时的时序优势? 与拦截器 (beforeUpdate) 的层级关系?
11. strictFill 三条件匹配 (property+fieldType+fill 标记) 的设计动机? 为什么类型必须精确匹配?
12. "有值不覆盖"策略怎么保证幂等? 每次执行都 new ParameterHandler 的设计能成立的原因?
13. fill 字段 SQL 直拼 (必有值断言) vs version 字段 if 守卫 — 两条执行期注入路线的 SQL 侧差异?
14. 逻辑删除 (DELETE 转 UPDATE) 为什么会触发 updateFill? 这是设计意图还是副作用?
15. extractParameters 的 Map 值展开 + objectSet 去重解决了什么真实问题 (重入)?
16. 三重守门 (open 双签名 + isWithInsertFill) 的分层控制面 — 各控制什么粒度?

## 学生视角

17. insertFill 的完整调用链: 从 mapper.insert(et) 到实体被填充, 经历了哪些类?
18. populateKeys 主键生成和 insertFill 谁先执行? ASSIGN_ID/ASSIGN_UUID/AUTO/INPUT 各自怎么处理?
19. setFieldValByName / fillStrategy / strictFillStrategy 三个方法, 空值语义有什么区别?
20. 乐观锁 beforeUpdate 和 updateFill 谁先跑? version 字段能标 fill 吗? 会冲突吗?
