# C-6 Profile 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Profile("dev") 的 Bean 为什么只在 dev 加载？判定在哪？ | §1 (ConditionEvaluator) + §2 (ProfileCondition) |
| 2 | @Profile({"dev","test"}) 是 AND 还是 OR？ | §2 关键设计 (白名单并集) |
| 3 | 激活的 profile 从哪来？怎么配？ | §3 (显式 setActiveProfiles / spring.profiles.active 属性) |
| 4 | 没配任何 profile 时默认是啥？ | §3 (defaultProfiles=["default"]) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么条件判定分 PARSE_CONFIGURATION/REGISTER_BEAN 两阶段？ | §1 关键设计 (条件依赖注册状态) |
| 6 | @Profile 和 @ConditionalOnXxx 是什么关系？ | §1 (@Profile=@Conditional+ProfileCondition, 同一引擎) |
| 7 | 为什么 active profiles 要懒加载解析？ | §3 关键设计 (属性源就绪时机) |
| 8 | 无 @Conditional 的类为什么默认加载？ | §2 (Condition 只做拦截) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | shouldSkip 为什么返回"跳过"而非"匹配"？ | §1 关键设计 |
| 10 | !prod 表达式(否定 profile)怎么实现？ | §3 (acceptsProfiles L387) + §2 (Profiles 表达式) |

## 覆盖: 10 问 / 3 身份 / 100%
