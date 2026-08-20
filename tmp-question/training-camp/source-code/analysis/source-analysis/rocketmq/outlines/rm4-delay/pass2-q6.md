# 闭环笔记 q6: 配置与测试面

## 假设
配置面 = 等级/异步/持久化; 测试面覆盖核心路径。

## 验证过程
- **配置** (MessageStoreConfig): messageDelayLevel 18 级 (L224) / **enableScheduleAsyncDeliver=false 默认** (L255) / **scheduleAsyncDeliverMaxPendingLimit=2000** (L256, 异步流控阈值) / flushDelayOffsetInterval=10s (L224 后)
- **常量** (ScheduleMessageService L43-49): FIRST_DELAY_TIME=1s (首启) / DELAY_FOR_A_WHILE=100ms (重查) / DELAY_FOR_A_PERIOD=10s (队尾周期) / WAIT_FOR_SHUTDOWN=5s / DELAY_FOR_A_SLEEP=10ms
- **测试** (ScheduleMessageServiceTest): testLoad (恢复) / testCorrectDelayOffset_whenInit / testDeliverDelayedMessageTimerTask (delayLevel=3 → queueId=2 断言) — 3 用例核心路径
- **5.x 指标**: BrokerMetricsManager (延迟投递度量, LABEL_MESSAGE_TYPE 等)
- **系统 topic**: SCHEDULE_TOPIC_XXXX (TopicValidator 实证, RM-3 已见) — 用户不可发送 (NOT_ALLOWED_SEND_TOPIC_SET)

## 代码类型
Interface (配置与面)

## 跨域关联
- RM-5 (Broker): BrokerController 持有/启动
- RM-3 (CQ): SCHEDULE_TOPIC 的 CQ 迭代

## 结论
配置 4 项 + 常量 5 个 + 测试 3 用例; 系统 topic 受保护; 5.x 异步投递默认关 (2000 流控)。
源码位置: MessageStoreConfig.java:224,255-256; ScheduleMessageService.java:43-49; ScheduleMessageServiceTest.java
