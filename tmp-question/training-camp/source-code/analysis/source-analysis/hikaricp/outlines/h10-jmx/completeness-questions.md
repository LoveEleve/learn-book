# H-10 JMX 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么开启 JMX? | §3 (setRegisterMbeans(true)) |
| 2 | JMX 能对池做什么? | §1 (softEvict/suspend/resume) |
| 3 | 能热改配置吗? | §2 (ConfigMXBean setter) |
| 4 | 为什么默认看不到 JMX? | §3 (默认关, 按需开) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 MXBean 接口? | §1 (JMX 约定自动暴露) |
| 6 | 为什么配置可热改? | §2 (seal 之外的管理口) |
| 7 | 为什么默认关? | §3 (开销+安全) |
| 8 | 为什么 PlatformMBeanServer? | §3 (JConsole 直接可见) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 两个 MXBean 分工? | §2 (池状态 vs 配置) |
| 10 | JMX 路径怎么定? | §3 (ObjectName type=Pool/PoolConfig) |
| 11 | 注册入口在哪? | §3 (handleMBeans) |

## 覆盖: 11 问 / 3 身份 / 100%
