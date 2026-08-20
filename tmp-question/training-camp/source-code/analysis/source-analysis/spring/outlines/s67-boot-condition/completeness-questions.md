# S-3 条件注解全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @ConditionalOnClass 怎么判断类存在？ | §2 (OnClassCondition 元数据粗筛) |
| 2 | @ConditionalOnMissingBean 为什么能防止覆盖用户配置？ | §2 (REGISTER_BEAN 阶段 + getBeanNamesForType) |
| 3 | @ConditionalOnProperty 的 havingValue/matchIfMissing 怎么用？ | §3 (三种开关形态) |
| 4 | Boot 条件与 Spring @Conditional 什么关系？ | §1 (同一引擎, Boot 是实现) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | matches 为什么设 final？ | §1 (统一流程 + 子类只写判定) |
| 6 | 类条件为什么不加载类？ | §2 (元数据驱动性能) |
| 7 | Bean 条件为什么在 REGISTER_BEAN 阶段？ | §2 (用户 Bean 注册完才能判断) |
| 8 | 复用 C-6 引擎 vs 重复实现？ | §1 (06 §2.5: 引擎复用, 实现展开) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | getMatchOutcome 返回什么？ | §1 (ConditionOutcome: 匹配+原因) |
| 10 | 注解怎么挂接到条件类？ | §1 (@Conditional(OnXxxCondition.class)) |

## 覆盖: 10 问 / 3 身份 / 100%
