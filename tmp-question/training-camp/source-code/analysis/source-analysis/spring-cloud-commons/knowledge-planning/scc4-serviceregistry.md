# SCC-4 服务注册抽象 — 知识规划 (KP)

> 域: SCC-4 | 级别: 🔴 | 方案: A | 大纲: outlines/scc4-serviceregistry/outline.md (6 节)

## §01 域定位

服务注册抽象 = 启动即注册的完整仪式。ServiceRegistry 5 方法契约 + AbstractAutoServiceRegistration 双角色 (事件监听+生命周期) + start/stop 对称链 + RegistrationLifecycle 4 钩子 + failFast。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| serviceregistry/ServiceRegistry.java | 64 | 5 方法契约 | 1 |
| serviceregistry/AbstractAutoServiceRegistration.java | 315 | 双角色 + start/stop 仪式 | 2,3,4 |
| serviceregistry/RegistrationLifecycle.java | 66 | 4 钩子 + Ordered | 5 |
| serviceregistry/RegistrationManagementLifecycle.java | 55 | 管理注册变体 | 5 |
| serviceregistry/AutoServiceRegistrationAutoConfiguration.java | 47 | 条件装配 + failFast | 6 |
| serviceregistry/AutoServiceRegistrationProperties.java | 67 | failFast 配置 | 6 |
| serviceregistry/Registration.java + AutoServiceRegistration | — | 元数据 + 标记 | 1 |

## §05 闭环要点 (Pass 2 内化)

### q1 双角色
ApplicationListener\<WebServerInitializedEvent\> + AutoServiceRegistration; onApplicationEvent: management 跳过 (L114-117) + port CAS (L118) + start (L119)。

### q2 触发链
start(): isEnabled 守卫 → running 双检 → Pre 事件 → 前钩子 → register → 后钩子 → 管理注册 → Registered 事件 → running CAS (L142-170)。

### q3 生命周期钩子
RegistrationLifecycle 4 钩子 (Ordered) + RegistrationManagementLifecycle 变体; stop() 镜像对称 (L294-311)。

### q4 装配条件
@ConditionalOnProperty auto-registration.enabled (L30, SCC-3 联动) + failFast (L42)。

## §06 负面空间 (6 条)

不做客户端内建 / 不做心跳续约 / 不做重试注册 / 不做失败降级 / 不做多实例注册 / 不做注销保护

## §07 交叉引用

- ← SCC-3 服务发现 (autoRegister 联动)
- → SCC-5 @LoadBalanced (注册后消费)
- 另见: Nacos NacosAutoServiceRegistration (5.8) / Eureka
