# 《HikariCP 数据库连接池实战》—— 完整目录

> 作者：朱政科 | 14章、4篇 | 机械工业出版社 | 从准备→基础→原理→实战

---

## 第一篇 准备篇（第1~3章）

### 第1章 阿里中间件实战，第一个案例
1.1 物联网 MQTT 单机压测 130 万参数调优
1.2 阿里中间件 TCP 四次挥手性能调优实战
1.2.1 亿级消息网关 Rowan 架构
1.2.2 人脸识别服务：异曲同工的架构
1.2.3 "双十一大促"全链路压测发现 TCP 问题
1.2.4 Linux 内核网络参数调优
1.2.5 Linux TCP 参数调优
1.2.6 一行代码大幅提升 QPS
1.3 技术驱动业务，结果为导向
1.4 本章小结

### 第2章 数据库连接池江湖
2.1 为什么使用数据库连接池
2.2 数据库连接池原理
2.3 数据库连接池百晓生《兵器谱》
2.3.1 c3p0
2.3.2 Proxool
2.3.3 XAPool
2.3.4 DBCP
2.3.5 Tomcat JDBC Pool
2.3.6 BoneCP
2.3.7 Druid
2.4 主流数据库连接池对比（性能/代码复杂度/功能/数据库中断）
2.5 本章小结

### 第3章 初识 HikariCP
3.1 Hikari 背景、特色及前景
3.2 SpringBoot 数据库连接池加载顺序剖析
3.3 SpringBoot 整合 HikariCP 实战（Spring Initializr/依赖/JdbcTemplate/初始化/启动）
3.4 本章小结

---

## 第二篇 基础篇（第4~5章）

### 第4章 HikariCP 参数配置
4.1 校时
4.2 HikariCP 配置手册（必需配置/非必需配置）
4.3 HikariCP 连接池配置多大合适
4.4 Fixed Pool Design 思想
4.5 MySQL 高性能配置
4.6 Hibernate 配置
4.7 JNDI 配置
4.8 本章小结

### 第5章 HikariCP 与 JDBC
5.1 HikariCP JDBC Logging
5.2 JDBC（定义/实战案例/剖析/PreparedStatement和Statement）
5.3 JDBC 与 SPI（简介/实战案例/JDBC的SPI机制/Dubbo分布式日志链路TraceID追踪）
5.4 拓展：线程池技术（MySQL线程池简介/技术内幕/实战）
5.5 本章小结

---

## 第三篇 原理篇（第6~9章）

### 第6章 HikariCP 性能揭秘
6.1 华山论剑
6.2 第三方测评（获取关闭连接测试/查询语句测试/psCache测试/结论）
6.3 HikariCP 为什么这么快（精简字节码/FastList/ConcurrentBag）
6.4 本章小结

### 第7章 HikariCP 连接原理
7.1 获取连接
7.2 归还连接
7.3 关闭连接
7.4 生成连接
7.5 扩展阅读：DCL 为什么要加 volatile
7.6 扩展阅读：Log4j2 为何性能优秀
7.7 本章小结

### 第8章 HikariCP 参数源码解析
8.1 SpringBoot 2.x HikariCP 参数加载原理
8.2 allowPoolSuspension
8.3 validationTimeout
8.4 leakDetectionThreshold
8.5 本章小结

### 第9章 HikariCP 动态代理与字节码技术
9.1 HikariCP 字节码工程（字节码技术/代理技术原理/JIT方法内联优化）
9.2 JMH 基准测试（常用注解/实战案例Orika）
9.3 本章小结

---

## 第四篇 实战篇（第10~14章）

### 第10章 HikariCP 监控实战
10.1 监控体系层次
10.2 为什么需要数据库连接池监控
10.3 HikariCP 监控指标（7个核心指标）
10.4 HikariCP 监控指标实战（连接风暴/慢SQL/监控指标与参数配置）
10.5 SpringBoot 2.0 暴露 HikariCP Metrics
10.6 SpringBoot 2.0 监控 HikariCP JMX
10.7 微服务架构下的监控平台选型
10.8 本章小结

### 第11章 从 HikariCP Metrics 谈微服务监控架构实战
11.1 HikariCP Metrics
11.2 Micrometer
11.3 SpringBoot 2.x 自定义埋点实战
11.4 SpringBoot 2.x 集成 Micrometer 源码解析
11.5 SpringBoot 1.5.x 自定义埋点实战
11.6 监控架构重点（Prometheus/Grafana）
11.7 本章小结

### 第12章 HikariCP 扩展技术
12.1 Flexy-Pool
12.2 Apache ShardingSphere
12.3 自研集成 HikariCP 和 Sharding-JDBC 数据库中间件
12.4 时钟回拨
12.5 本章小结

### 第13章 HikariCP 常见问题
13.1 HikariCP 故障分析技巧
13.2 leakDetectionThreshold 参数解决 Spark/Scala 连接池泄露
13.3 详解 JDBC 超时
13.4 快速恢复
13.5 Oracle Connection Reset 问题
13.6 HikariCP 关闭连接的 5 种情况
13.7 如何获取 HikariDataSource 的 active connection
13.8 如何对 HikariCP 配置文件中的服务器名、用户名、密码加密
13.9 HikariCP 神奇的配置 dataSourceProperties
13.10 如何获取 HikariCP 连接池中的原始连接
13.11 HikariCP 并不是万能工具
13.12 本章小结

### 第14章 HikariCP 诡案实录
14.1 问题描述
14.2 Brett 经典回答
14.3 另一个类似的案例
14.4 分析问题
14.5 解决问题
14.6 本章小结
