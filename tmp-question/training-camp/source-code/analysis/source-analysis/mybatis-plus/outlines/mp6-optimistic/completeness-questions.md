# MP-6 乐观锁 — completeness-questions

## 开发者视角

1. 实体加 @Version 后 updateById 自动乐观锁 — 拦截发生在哪?为什么只拦 UPDATE?
2. 旧版本值怎么进 WHERE?MP_OPTLOCK_VERSION_ORIGINAL 是什么?
3. version 字段支持哪些类型?不支持的类型会怎样?
4. 版本字段为 null 时更新会发生什么?怎么自定义抛异常?
5. update(et, wrapper) 和 updateById(et) 的版本条件注入路径有什么不同?
6. 只用 LambdaUpdateWrapper 更新(无实体)能乐观锁吗?wrapperMode 是什么?
7. 并发冲突怎么识别?影响行数 0 意味着什么?
8. 冲突后重试需要注意什么?实体的版本值状态?

## 架构师视角

9. CAS 三步骤(捕获旧值/注入条件/回写新值)的时序 — 为什么先捕获再回写?
10. 双通道设计: wrapper 路径(apply 进 wrapper)与参数通道(MP_OPTLOCK_VERSION_ORIGINAL)的选择依据?
11. VERSION_FUNCTION_MAP 类型策略 — 数值+1 vs 时间取当前, 各自适用什么场景?
12. 乐观锁 vs 悲观锁的适用边界(对照 spring-tx): 什么场景该换悲观锁?
13. 与 MP-1 注入 SQL 的协作: getVersionOli 片段在注入时怎么生成?执行时怎么引用?
14. wrapperMode 的 setVersionByWrapper 从哪找版本条件?边界是什么?

## 学生视角

15. updateById(et) 的完整链路: 拦截→旧值→条件→回写→SQL 执行?
16. UPDATE SQL 里 SET version=#{新值} 和 WHERE version=#{MP_OPTLOCK_VERSION_ORIGINAL} 分别对应代码哪一步?
17. VERSION_FUNCTION_MAP 的 7 种类型映射表?
18. 两次并发 updateById 同一记录, 第二次为什么影响行数 0?
19. 与 M-2 update 返回行数语义的衔接?
20. 为什么旧值为 null 时默认不拦截(return)?
