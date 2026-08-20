# SCC-3 服务发现抽象 — 知识规划 (KP)

> 域: SCC-3 | 级别: 🔴 | 方案: A | 大纲: outlines/scc3-discovery/outline.md (6 节)

## §01 域定位

服务发现抽象 = 所有注册中心的统一插槽。DiscoveryClient 四方法契约 (description/getInstances/getServices/probe) + Composite 组合 (排序+短路) + Simple 属性驱动兜底 + @EnableDiscoveryClient ImportSelector 触发 + 健康/心跳面。

## §02 源文件清单

| 文件 | 职责 | 归属节 |
|:--|:--|:--:|
| discovery/DiscoveryClient.java | 接口 + probe default | 1 |
| discovery/composite/CompositeDiscoveryClient.java | 排序 + 短路 + 合并 | 2 |
| discovery/simple/SimpleDiscoveryClient.java + Properties | 属性驱动 + order | 3 |
| discovery/EnableDiscoveryClient + ImportSelector | 注解触发 + autoRegister 分支 | 4 |
| discovery/ReactiveDiscoveryClient + composite/reactive/ + simple/reactive/ | 响应式变体 | 5 |
| discovery/health/ (5 类) + discovery/event/ (5 类) | 健康聚合 + 心跳 | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 接口面
四方法 + DEFAULT_ORDER=0 + probe default (L66-68); 实现族: Eureka/Nacos/Consul 外部仓库。

### q2 Composite 组合
构造排序 (L41) + getInstances 短路 (L51-56) + getServices 合并去重 (L62-70)。

### q3 ImportSelector
autoRegister 双分支 (追加 AutoServiceRegistrationConfiguration L49-53 / 注入禁用属性 L56-61) + isEnabled (L68-69)。

### q4 健康/心跳
DiscoveryClientHealthIndicator (HIGHEST_PRECEDENCE) + HeartbeatMonitor AtomicReference 变更检测 (L29-35)。

## §06 负面空间 (6 条)

不做客户端内建 / 不做实例缓存 / 不做负载均衡 / 不做故障转移 / 不做推送订阅 / 不做实例排序

## §07 交叉引用

- ← SCC-13 NamedContextFactory (消费面)
- → SCC-4 服务注册 (autoRegister 联动) + SCC-6 LoadBalancer (实例消费)
- 另见: Eureka/Nacos/Consul 客户端 + Nacos 长轮询 (5.8)
