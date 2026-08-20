# 闭环笔记 q5: 5.x 新面 — 双 remoting/ReplicasManager/插件

## 假设
5.x 装配差异: fast 发送端口 / controller 模式 / 插件体系 / TimerWheel。

## 验证过程
- **双 remoting** (L478-486): fastRemotingServer = **listenPort-2** (专用发送端口, SEND 族走 fast); 生产可拆分发送流量 (5.x)
- **ReplicasManager** (L855-858, enableControllerMode): **fenced=true 初始** (5.0 自动故障转移 — 初始隔离直到控制器确认); RM-14 交叉
- **RocksDBMessageStore** (L787-794): enableRocksDBStore 切换 + **rocksdbCQDoubleWrite** (CQ 双写); RM-16 交叉
- **存储插件** (MessageStoreFactory.build L809-812): MessageStorePluginContext — 存储层插件链
- **brokerAttachedPlugins** (L867-870): 附件插件 load (5.x 插件体系)
- **TimerWheel** (L813-819): TimerMessageStore + TimerCheckpoint (5.x Timer 消息, RM-4 对照)
- **BrokerIdentity** (5.x): 身份贯穿线程命名/日志/注册
- **slave-act-master 隔离** (L1709-1711): isIsolated (从库代主, 隔离注册)

## 代码类型
Implementation (5.x 装配差异)

## 跨域关联
- RM-14 (Controller) / RM-16 (TieredStore/RocksDB) / RM-4 (TimerWheel)

## 结论
5.x 装配新增: fast 发送端口 / controller 模式 (fenced 初始) / RocksDB 存储+双写 / 插件体系 / TimerWheel / 从库代主隔离。
源码位置: BrokerController.java:478-486,787-819,855-870,1709-1711
