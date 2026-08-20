# S2-14 AOT/Native Image 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | AOT 编译后 @Configuration 的 CGLIB 代理是怎么被替代的？ | §1 |
| 2 | RuntimeHints 为什么需要精确到方法而非类？ | §2 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 为什么 Registration 和 Initialization 要分成两个 AOT 接口？ | §1 |
| 4 | GraalVM Native Image 中 `@ComponentScan` 怎么工作的？ | §1-2 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | GraalVM Native Image 是什么？为什么不支持反射？ | §2 |

## 覆盖: 5 问 / 3 身份 / 100%
