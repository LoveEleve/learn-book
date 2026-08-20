# C-5 注解元数据全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @RestController 没写 @Component 为什么能扫描到？ | §1 (元注解链合并视图) + §3 (isAnnotated) |
| 2 | @Service("userService") 的 value 怎么传给 @Component？ | §2 (@AliasFor 元注解映射) |
| 3 | 父类上的注解子类能用吗？ | §3 (find 系列 TYPE_HIERARCHY) |
| 4 | 注解写了不生效怎么排查？ | §2 (搜索策略/合并语义) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么不用 Java 反射 getAnnotation？ | §1 关键设计 (单层 vs 链) |
| 6 | @AliasFor 为什么是声明式而非硬编码？ | §2 关键设计 (可扩展组合注解) |
| 7 | 类级默认+方法级覆盖的属性合并怎么做？ | §3 (getMergedAnnotationAttributes) |
| 8 | 为什么会有新旧两套注解 API？ | §3 (5.3 演进) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 注解链的距离(distance)是什么？ | §1 (数据流: 0/1/2) + §2 (AnnotationTypeMapping) |
| 10 | 搜索策略 DIRECT 和 TYPE_HIERARCHY 差在哪？ | §1 (搜索策略) |

## 覆盖: 10 问 / 3 身份 / 100%
