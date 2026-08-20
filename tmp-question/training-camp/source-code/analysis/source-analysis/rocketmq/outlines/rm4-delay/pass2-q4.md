# 闭环笔记 q4: 消息还原 — messageTimeUp

## 假设
到期后重建消息: 还原真实 topic/queue + 清除延迟属性。

## 验证过程
- **还原** (messageTimeUp L340-380): 复制 body/flag/properties/tagsCode (按真实 tag 重算)/sysFlag/born/存储信息 → **清 DELAY_TIME_LEVEL/TIMER_DELIVER_MS/TIMER_DELAY_SEC 属性** (L371-374) → **topic = REAL_TOPIC 属性** (L376) → **queueId = REAL_QUEUE_ID 属性** (L378-380)
- **waitStoreMsgOK=false** (L369): 投递消息不要求同步刷盘 (延迟投递容忍异步)
- **tagsCode 重算** (L346-348): 按还原后的 tag 字符串重算 (投递后 CQ 是真实 topic 的 tag 哈希)
- **属性往返**: propertiesString 由 msgExt 原样携带 (L350) — REAL_* 属性在投递时读取后保留? (需确认是否清理 — 标注)
- **消费者可见**: 还原后消息在真实 topic 正常消费 (延迟对消费者透明)

## 代码类型
Implementation (消息重建)

## 跨域关联
- RM-3 (编码): MessageExtBrokerInner 对称构造
- RM-8 (消费): 还原后走普通消费

## 结论
到期 = 消息重建: 还原 topic/queue + 清延迟属性 + tagsCode 重算; waitStoreMsgOK=false (投递异步容忍)。
源码位置: ScheduleMessageService.java:340-380
