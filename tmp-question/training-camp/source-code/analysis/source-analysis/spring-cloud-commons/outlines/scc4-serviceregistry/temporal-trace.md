# SCC-4 服务注册抽象 — 时空溯源 (代码注释锚, git shallow)

> git shallow (单提交) 无法考古; 溯源以 @Deprecated + management 演进 + 装配条件为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 1.x | **ServiceRegistry 诞生**: register/deregister/close/setStatus/getStatus 5 方法 | ServiceRegistry.java:26-62 |
| 1.x | **AbstractAutoServiceRegistration**: WebServerInitializedEvent 触发 + start/stop 对称链 | AbstractAutoServiceRegistration.java:49-50 |
| 2.x | **RegistrationLifecycle 钩子引入**: 前后注册/前后注销 4 钩子 | RegistrationLifecycle.java:27-60 |
| 中期 | **management 注册面**: 管理服务单独注册 + RegistrationManagementLifecycle 继承 | RegistrationManagementLifecycle.java:25 + L114 (management namespace 跳过) |
| 当前 | **failFast**: 无 AutoServiceRegistration bean 且 failFast → 启动失败 | AutoServiceRegistrationAutoConfiguration.java:42 |
| 当前 | getEnvironment/getPort @Deprecated — 环境/端口访问内部化 | AbstractAutoServiceRegistration.java:128/133 |

## 版本相关性结论

- **5 方法契约自 1.x 稳定**: register/deregister 核心 + close/setStatus/getStatus 管理面, 从未变 (所有注册中心实现依赖)
- **触发时机设计稳定**: WebServerInitializedEvent (端口就绪) 而非应用启动 — 从诞生至今
- **钩子体系是 2.x 扩展**: RegistrationLifecycle 让注册流程可插拔 (元数据设置/审计)
- **management 是渐进演化**: 从"单注册"到"主+管理双注册" (management namespace 跳过 + 独立注册面)
