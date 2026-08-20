# 闭环笔记 q5: 消费进度 — OffsetStore 双实现

## 假设
集群: broker 存储 (RemoteBrokerOffsetStore); 广播: 本地文件 (LocalFileOffsetStore)。

## 验证过程
- **OffsetStore 接口** (store/): updateOffset/readOffset/persist/persistAll
- **RemoteBrokerOffsetStore** (集群): updateOffset (内存表) → **persist → updateConsumeOffsetToBroker (RM-1 请求, broker 侧 ConsumerOffsetManager — RM-5 定时持久化)**; readOffset 三类型 (内存/broker 拉取/内存优先)
- **LocalFileOffsetStore** (广播): **本地文件 (rocketmq.client.localOffsetStoreDir)** + OffsetSerializeWrapper JSON — 无 broker 交互
- **选择** (DefaultMQPushConsumerImpl): MessageModel.CLUSTERING → Remote / BROADCASTING → Local
- **提交时机**: 拉取回调后 persist (L416) + 定时 persistAll (L1403) + 关闭时
- **推进链**: 消费成功 → ProcessQueue.commit → offsetStore.updateOffset → persist → broker

## 代码类型
Implementation (进度持久化)

## 跨域关联
- RM-5 (Broker): ConsumerOffsetManager
- RM-1 (remoting): UPDATE_CONSUMER_OFFSET 请求码

## 结论
进度双实现: 集群 (broker 存储, 重平衡共享) vs 广播 (本地文件, 各客户端独立); 提交链 = 消费成功 → commit → update → persist。
源码位置: store/RemoteBrokerOffsetStore.java:59-163; LocalFileOffsetStore.java:78-104; DefaultMQPushConsumerImpl.java:416,1403
