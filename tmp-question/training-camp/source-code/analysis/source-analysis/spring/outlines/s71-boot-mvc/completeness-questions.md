# S-7 MVC 自动装配全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 引 starter-web 后 DispatcherServlet 谁装配的？ | §2 (EnableWebMvcConfiguration) |
| 2 | @EnableWebMvc 为什么关闭自动装配？ | §1 (@ConditionalOnMissingBean 跳过) |
| 3 | 自定义消息转换器怎么覆盖默认？ | §3 (HttpMessageConverters 定制优先) |
| 4 | spring.mvc.* 属性怎么生效？ | §3 (WebMvcProperties → Adapter) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 @ConditionalOnMissingBean(WebMvcConfigurationSupport)？ | §1 (用户优先防冲突) |
| 6 | 为什么委托 DelegatingWebMvcConfiguration？ | §2 (机制与装配分离) |
| 7 | 为什么 HttpMessageConverters 用 ObjectProvider？ | §3 (可选依赖) |
| 8 | 本域与 W-1~W-5 的边界？ | §2 (机制复用, 只讲装配) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | WebMvcConfigurer 怎么被聚合？ | §2 (Delegating 收集) |
| 10 | @AutoConfiguration(after=...) 什么意思？ | §1 (依赖先行) |

## 覆盖: 10 问 / 3 身份 / 100%
