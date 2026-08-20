# S-24 Elasticsearch 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么配 ES 地址? | §2/§3 (spring.elasticsearch.uris) |
| 2 | 注入 RestClient 从哪来? | §3 (builder.build()) |
| 3 | 想定制 builder 怎么做? | §2/§3 (注入 RestClientBuilder/customizers) |
| 4 | 没 ES 依赖会怎样? | §1 (@ConditionalOnClass 跳过) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 @ConditionalOnClass 装配? | §1 (有类才装) |
| 6 | 为什么抽 ConnectionDetails 抽象? | §2 (地址来源解耦) |
| 7 | 为什么降级只讲接线? | §3 (与阶段3 ES 重叠) |
| 8 | 为什么分 builder/client 两 Bean? | §3 (定制 vs 直接用) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | uris 默认值是什么? | §2 (http://localhost:9200) |
| 10 | RestClient 怎么产出? | §3 (restClientBuilder.build()) |
| 11 | 本域边界在哪? | §3 (只接线, 深入在阶段3) |

## 覆盖: 11 问 / 3 身份 / 100%
