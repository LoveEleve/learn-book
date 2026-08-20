# S2-9 @Conditional 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @ConditionalOnClass 和 @ConditionalOnBean 为什么评估时机不同？ | §1 |
| 2 | 实现 ConfigurationCondition 时 getConfigurationPhase 返回 null 会发生什么？ | §1-2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 为什么 ConditionEvaluator 是 package-private 而非 public API？ | §2 |
| 4 | 如果 PARSE_CONFIGURATION 和 REGISTER_BEAN 合并为一个阶段会怎样？ | §3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | @Conditional 和 if 语句有什么区别？为什么不能用 if 代替？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
