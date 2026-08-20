# 闭环笔记 q6: 关闭面与测试 — 有序 shutdown + 重启测试

## 假设
shutdown 逆序释放; 测试覆盖重启/慢任务。

## 验证过程
- **shutdown** (L1565-1640): 顺序释放 —
  - **brokerOuterAPI.shutdown** (namesrv 通信先断)
  - 各服务 shutdown: scheduleMessageService (RM-4) / consumerOffsetManager.persist 收尾 (L1472-1480: filter/order/schedule 持久化) / heartbeatExecutor / 定时器 (scheduledFutures 取消) / remoting 双服务 / messageStore.shutdown / topicConfigManager 等
- **shutdownHook** (BrokerStartup L224): JVM 钩子 → controller.shutdown — 优雅退出
- **配置持久化**: 关闭时 final persist (offset/filter/order/schedule) — 崩溃窗口最小化
- **测试** (BrokerControllerTest): **testBrokerRestart** (initialize→shutdown→重新 initialize 断言 — 重启路径) / testHeadSlowTimeMills (心跳队列慢任务监控)
- **BrokerStartupTest**: 启动参数面

## 代码类型
Interface (生命周期)

## 跨域关联
- 全部 RM 域: 生命周期收口

## 结论
关闭 = 逆序释放 (网络→定时→双 remoting→存储) + 收尾持久化 + JVM 钩子; 测试覆盖重启幂等。
源码位置: BrokerController.java:1565-1640; BrokerStartup.java:224; BrokerControllerTest.java:63-76
