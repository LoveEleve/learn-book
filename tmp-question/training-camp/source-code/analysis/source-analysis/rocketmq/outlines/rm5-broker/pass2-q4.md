# 闭环笔记 q4: 定时任务 — 8 核心 + 周期注册/持久化

## 假设
定时面 = 统计/持久化/保护/积压/主从/namesrv 六大类。

## 验证过程
- **initializeBrokerScheduledTasks** (L608-731) 8 个核心定时 (python 提取实证):
  1. **brokerStats.record** — 每日零点 (computeNextMorningTimeMillis, L609-613)
  2. **consumerOffsetManager.persist** — 周期 = flushConsumerOffsetInterval (消费进度)
  3. **consumerFilterManager/consumerOrderInfoManager.persist** — 过滤/顺序状态
  4. **protectBroker** — 资源保护 (连接/流量面)
  5. **printWaterMark** — 水位日志
  6. **dispatchBehindBytes 积压监控** — 分发滞后 (store 面)
  7. **syncAll** (主从全量同步: topic/offset/订阅/过滤 — getSlaveSynchronize) + **syncTimerCheckPoint** (TimerWheel)
  8. **printMasterAndSlaveDiff** (复制滞后打印)
- **initializeScheduledTasks** (L732-770): 条件任务 — fetchNameServerAddr (DNS/地址服务器, 按 FetchNamesrvAddrInterval) / updateNamesrvAddr (UpdateNameServerAddrPeriod) / enableControllerMode 条件 (5.x)
- **namesrv 周期注册** (start L1724-1746): 10s 后每 **10-60s** (registerNameServerPeriod 钳制) registerBrokerAll
- **5.x 附加**: syncBrokerMemberGroup / refreshMetadata / topicQueueMappingClean (独立服务)

## 代码类型
Implementation (周期调度)

## 跨域关联
- RM-12 (HA): syncAll 主从
- RM-4 (延迟): scheduleMessageService.persist 定时 (L1480)

## 结论
定时面 = 统计每日 + 持久化 (offset/filter/order) + 保护/水位 + 积压监控 + 主从同步 + namesrv 周期注册; 条件任务 (5.x controller/TimerWheel)。
源码位置: BrokerController.java:608-770,1724-1746
