# S-22 测试自动配置 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @WebMvcTest 只加载 web 层怎么做到的? | §2 (关全量 + 类型过滤) |
| 2 | 想测全量用哪个注解? | §1/§2 对照 (@SpringBootTest) |
| 3 | 切片怎么配 MockMvc? | §3 (@ImportAutoConfiguration + C-16) |
| 4 | 为什么启动飞快? | §2 (enableautoconfiguration=false) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么复合注解? | §1 (一个注解打包多个关注点) |
| 6 | 为什么关全量 + 选择性导入平衡? | §3 (隔离 + 按需补装) |
| 7 | 为什么用 TypeExcludeFilter 而非别的方式? | §2 (按注解类型过滤纯化切片) |
| 8 | 切片与 @SpringBootTest 差异? | §1/§2 (全量 vs 子集) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | @WebMvcTest 背后有哪些元注解? | §1 (L101-108) |
| 10 | enableautoconfiguration=false 在哪设? | §2 (DisableAutoConfigurationContextCustomizer) |
| 11 | 切片保留哪些 Bean? | §2 (Controller/ControllerAdvice/Filter) |

## 覆盖: 11 问 / 3 身份 / 100%
