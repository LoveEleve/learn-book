# 闭环笔记 q1: 启动链 — main → createBrokerController → initialize → start

## 假设
启动 = 参数装配 → 三阶段初始化 → 服务启动; 失败即退出。

## 验证过程
- **main** (BrokerStartup:51-52): `start(createBrokerController(args))`
- **createBrokerController** (L245-248): buildBrokerController (L84: 命令行 -c 配置/-n namesrv/环境变量/系统属性 → BrokerConfig+MessageStoreConfig+NettyServerConfig 三配置) → `controller.initialize()` — 失败 → 日志 + exit(-1) (L250-253 区域)
- **initialize 三阶段** (BrokerController:824-831): initializeMetadata → initializeMessageStore → recoverAndInitService — **逐步短路** (任一 false → 启动失败)
- **start** (L1705+): shouldStartTime (disappearTimeAfterStart 延迟生效) → brokerOuterAPI.start → startBasicService → 非隔离/非 DLedger/非双写 → registerBrokerAll + **10s 后每 10-60s 重注册** (registerNameServerPeriod, L1724-1746)
- **BrokerIdentity** (5.x): 定时任务线程命名/日志身份

## 代码类型
Implementation (启动管线)

## 跨域关联
- 全部 RM 域: 装配面

## 结论
启动 = 参数装配 → 三阶段初始化 (短路) → 服务启动 + namesrv 周期注册; 失败 exit; 5.x 身份与隔离 (slave-act-master)。
源码位置: BrokerStartup.java:51-248; BrokerController.java:824-831,1705-1746
