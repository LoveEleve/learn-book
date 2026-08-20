# S2-6 BFPP 全景 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | BDRPP 的三阶段 while 循环为什么要不断循环？如果只执行一次会出什么问题？ | §1 |
| 2 | PropertySourcesPlaceholderConfigurer 的 appliedPropertySources 从哪里来？为什么不是硬编码的属性文件？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | BFPP 和 BPP 的本质区别是什么？为什么 BFPP 在 Step 5 / BPP 在 Step 6？ | §1 |
| 4 | PropertyPlaceholderConfigurer → PropertySourcesPlaceholderConfigurer 的演进反映什么问题？为什么 Spring Boot 不能仅用前者？ | §2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | `${server.port:8080}` 中的 `:8080` 是怎么被识别的？如果没有默认值会发生什么？ | §3 |

## 覆盖: 5 问 / 3 身份 / 100%
