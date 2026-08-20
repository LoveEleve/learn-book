# 闭环笔记 q6: 版本矩阵 — 18 个 spring-data 子模块的编译期隔离

## 假设
spring-data 按 Spring Data Redis 版本拆分 18 个子模块 (data-16~41), 每版本独立编译适配其 API (编译期隔离, 避免运行时反射/版本冲突)。

## 验证过程
- 结构: redisson-spring-data/ 下 18 子模块 (data-16,17,18,20,21,22,23,24,25,26,27,30,31,32,33,34,35,40,41) — 每版 14-56 文件
- 每版含: RedissonConnectionFactory/RedissonConnection/RedissonSubscription 等核心适配类
- 为什么分版: Spring Data Redis API 随版本演进 (方法签名/枚举变化), 编译期隔离 = 每版编译针对其 API, 无运行时版本判断
- 用户选版: 依赖 redisson-spring-data-{version} 匹配自己 Spring Data Redis 版本
- 版本对应: 16~27 → Spring Data Redis 2.x; 30~35 → 3.x; 40/41 → 4.x

## 代码类型
Interface (版本隔离) — 多版本编译期适配

## 跨域关联
- Q4 (ConnectionFactory) → 各版本核心类
- s75-boot-redis (Boot Redis) → Boot 版本决定 data 版本
- 运维: 版本错配 = 类找不到/NoSuchMethodError

## 结论
版本矩阵 = 18 子模块编译期隔离: 每 Spring Data Redis 版本独立适配类集, 用户按版本选依赖。避免运行时反射, 但增加维护面 (每新版本复制适配)。这是"版本对齐"的编译期方案。
源码位置: redisson-spring-data/redisson-spring-data-{16..41}/ 结构