# 闭环笔记 q5: 服务端面 — registerProcessor + 请求码体系

## 假设
服务端注册 (code → processor) 表; 160 请求码分类; 心跳/拉取码位。

## 验证过程
- **registerProcessor** (NettyRemotingServer.java:339-351): `processorTable.put(requestCode, new Pair<>(processor, executor))` + defaultRequestProcessorPair 兜底注册 (L351)
- **请求码体系** (RequestCode.java): **160 个常量** (grep 实证) — 全协议命令面:
  - PULL_MESSAGE = 11 (拉取, RM-8 交叉)
  - HEART_BEAT = 34 (客户端心跳)
  - LITE_PULL_MESSAGE = 361 (LitePull 长轮询, 5.x — interruptPullRequests 的 code 361)
  - RAFT_BROKER_HEART_BEAT_EVENT_REQUEST = 1018 (DLedger/Controller, RM-12 交叉)
- **心跳面**: HEART_BEAT 34 — 客户端注册消费者/生产者信息到 broker (broker 侧 consumerTable)
- **双实现**: NettyRemotingServer (L817) 与 Proxy 内嵌 remoting 服务 (proxy 模块 rpc/ — 5.x)
- **EventExecutorGroup**: 服务端 worker 线程组 (Netty)
- **关闭面**: shutdown 清理 channel/线程池

## 代码类型
Interface (协议命令面)

## 跨域关联
- RM-7/8/9: 生产/消费走请求码 (SEND_MESSAGE/PULL_MESSAGE)
- RM-12: 1018 心跳 (HA)
- RM-11: 路由请求码

## 结论
服务端 = (code → processor+线程池) 注册表 + default 兜底; 160 请求码构成完整命令面 (拉取 11/361, 心跳 34, HA 1018); 5.x LitePull 长轮询独立码。
源码位置: NettyRemotingServer.java:339-351; RequestCode.java (160 常量)
