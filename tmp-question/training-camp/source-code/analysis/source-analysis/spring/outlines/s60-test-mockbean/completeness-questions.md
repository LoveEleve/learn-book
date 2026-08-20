# C-18 @MockBean 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @MockBean 怎么替换真实 Bean 的？ | §2 (启动期覆盖 BeanDefinition) |
| 2 | 接口多实现时 @MockBean 替换哪个？ | §2 (primary 或 name 指定) |
| 3 | mock 未 stub 的方法返回什么？ | §3 (RETURNS_DEFAULTS) |
| 4 | 要保留部分真实逻辑用什么？ | §3 (@SpyBean) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 BeanDefinitionRegistryPostProcessor？ | §1 关键设计 (实例化前改定义) |
| 6 | 多实现歧义怎么处理？ | §2 (唯一/primary/报错) |
| 7 | 为什么不同 mock 组合会重建容器？ | §3 (缓存键含 mock 定义) |
| 8 | mock vs spy 的适用场景？ | §3 (全替身 vs 部分真实) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | register 时 beanName 怎么定位？ | §2 (五规则) |
| 10 | Mockito.mock 和 Mockito.spy 区别？ | §3 (替身 vs 半真) |

## 覆盖: 10 问 / 3 身份 / 100%
