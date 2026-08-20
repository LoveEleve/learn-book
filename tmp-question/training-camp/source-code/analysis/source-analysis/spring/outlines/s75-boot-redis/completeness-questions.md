# S-11 Redis 自动装配全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 引 starter-data-redis 后 RedisTemplate 哪来的? | §1 (条件激活+模板装配) |
| 2 | Lettuce/Jedis 怎么选? | §2 (依赖驱动) |
| 3 | 多连接工厂会怎样? | §1 (@ConditionalOnSingleCandidate) |
| 4 | 自定义 RedisTemplate 怎么覆盖? | §1 (@ConditionalOnMissingBean) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 @ConditionalOnSingleCandidate? | §1 (多实例歧义) |
| 6 | 为什么依赖驱动选客户端? | §2 (与 S-8 同模式) |
| 7 | 为什么只讲接线? | §3 (阶段3 深挖) |
| 8 | 配置怎么到连接工厂? | §3 (ConnectionDetails) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 默认客户端是什么? | §2 (Lettuce) |
| 10 | spring.data.redis.* 谁绑定? | §3 (S-5) |

## 覆盖: 10 问 / 3 身份 / 100%
