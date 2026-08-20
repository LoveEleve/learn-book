# D-9 Boot3 Starter 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 不写 spring.datasource.type 会用 Druid 吗? | §1 (matchIfMissing=true L44) |
| 2 | druid 前缀和原生前缀都能配, 怎么选? | §2 (druid 优先, null 时回退 determineXxx L38) |
| 3 | 怎么开启监控页? | §3 (stat-view-servlet.enabled: true L29) |
| 4 | Web 统计 Filter 拦截什么? | §3 (/* + exclusions 静态资源 L38-39) |
| 5 | Filter 怎么进链? | §2 (autoAddFilters @Autowired 汇入 L52-55) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么必须 AutoConfigureBefore Boot 的 DataSourceAutoConfiguration? | §1 (抢占 DataSource bean 名额, 否则 Hikari 先注册) |
| 7 | 为什么 Wrapper 继承 DruidDataSource? | §2 (配置绑定+生命周期落同一对象, 免代理) |
| 8 | 为什么组件全部条件注册? | §3 (可选组件 enabled 才装, 不污染应用) |
| 9 | 与 S-2 自动装配管线什么关系? | §1 (Boot 条件装配机制复用, 本域是具体装配实例) |
| 10 | Boot2 starter 为什么淘汰? | §3/header (项目 Boot3, jakarta API) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 11 | starter 装配分几步? | §1 (条件判定→配置绑定→@Import→主 Bean) |
| 12 | afterPropertiesSet 干什么? | §2 (回退填充属性→init 启动池) |
| 13 | 8 个 Filter 注册有什么共同模式? | §3 (Property enabled+ConfigurationProperties+MissingBean) |
| 14 | 监控页数据从哪来? | §3 (StatViewServlet 消费 D-3 统计) |
| 15 | AOP 统计怎么装配? | §3 (aop-patterns 正则 + RegexpMethodPointcutAdvisor + DruidStatInterceptor) |
| 16 | spring.aop.auto=false 会怎样? | §3 (手动 DefaultAdvisorAutoProxyCreator 兜底) |

## 覆盖: 14 问 / 3 身份 / 100%
