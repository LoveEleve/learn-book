# S-18 日志 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 想用 Log4J2 怎么切? | §1 (排除 logback 加 log4j2 依赖, classpath 探测) |
| 2 | logback-spring.xml 和 logback.xml 差别? | §3 (-spring 由 Boot 加载, 支持 Spring 标签) |
| 3 | 怎么指定任意日志配置文件? | §3 (logging.config 属性) |
| 4 | 为什么启动早期日志被压住? | §2 (SUPPRESS_ALL_FILTER) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么用 SPI + classpath 探测选日志系统? | §1 (不硬编码, 依赖驱动) |
| 6 | 为什么分两阶段初始化? | §2 (环境就绪前静默, 就绪后加载) |
| 7 | -spring.xml 变体解决了什么? | §3 (让配置感知 Spring 环境) |
| 8 | 与 S-17 的触发方式对比? | §2 (同用 SpringApplication 事件) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | 日志系统抽象长什么样? | §1 (beforeInitialize/initialize 模板) |
| 10 | <springProfile> 怎么按 profile 生效? | §3 (SpringBootJoranConfigurator 解析) |
| 11 | 无配置文件会怎样? | §3 (loadDefaults 内建默认) |

## 覆盖: 11 问 / 3 身份 / 100%
