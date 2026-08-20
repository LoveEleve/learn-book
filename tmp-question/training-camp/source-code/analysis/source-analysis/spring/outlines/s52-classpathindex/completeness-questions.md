# C-10 ClassPathIndex 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | spring.components 文件哪来的？谁生成的？ | §1 (@Indexed + spring-indexer) |
| 2 | 组件扫描怎么利用索引加速？ | §3 (findCandidateComponents 分派) |
| 3 | 项目没生成 spring.components 会怎样？ | §3 (回退全扫描, 功能不变) |
| 4 | 怎么让项目用上组件索引？ | §1 (@Indexed/索引处理器) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么编译期生成而非运行期扫描？ | §1 关键设计 (时间换空间) |
| 6 | 为什么 getResources 合并多 jar 索引？ | §2 (跨模块组件索引) |
| 7 | 为什么自定义 includeFilter 时不能用索引？ | §3 (无法映射 stereotype) |
| 8 | 索引按 ClassLoader 缓存的理由？ | §2 (类加载器隔离) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 索引查询的维度(包, stereotype)是什么？ | §2 (getCandidateTypes) |
| 10 | 有索引和全扫描的差异在哪一步？ | §3 (读类元数据 vs 遍历所有 .class) |

## 覆盖: 10 问 / 3 身份 / 100%
