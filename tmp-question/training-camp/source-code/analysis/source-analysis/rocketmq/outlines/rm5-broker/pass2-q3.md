# 闭环笔记 q3: 处理器注册 — 26 请求码 × 双服务

## 假设
命令面注册到 remoting 双服务; 请求码覆盖全部 broker 面。

## 验证过程
- **双服务** (registerProcessor L1070-1151): remotingServer (主端口) + **fastRemotingServer (listenPort-2 专用发送端口, L478-486)** — **SEND_MESSAGE 族注册 2 次** (主+fast)
- **26 个请求码** (python 提取实证): SEND_MESSAGE(V2)/SEND_BATCH_MESSAGE/CONSUMER_SEND_MSG_BACK/ACK_MESSAGE(BATCH)/CHANGE_MESSAGE_INVISIBLETIME/SEND_REPLY_MESSAGE(V2)/QUERY_MESSAGE/VIEW_MESSAGE_BY_ID/HEART_BEAT/UNREGISTER_CLIENT/CHECK_CLIENT_CONFIG/GET_CONSUMER_LIST_BY_GROUP/... — 48 处注册 = 24 码×双服务
- **处理器分组**: sendMessageProcessor (发送族) / clientManageProcessor (心跳/注册, 独立 heartbeatExecutor) / pullMessageProcessor (消费) / queryMessageProcessor (查询) / adminProcessor (管理) — 各绑独立线程池 (RM-1 交叉)
- **REPLY 消息面** (SEND_REPLY_MESSAGE): 5.x 请求-响应模式 (客户端请求主题)

## 代码类型
Interface (命令面注册)

## 跨域关联
- RM-1 (remoting): processorTable 填充
- RM-8 (消费): pull 处理器
- RM-13 (Proxy): 双协议共用处理器

## 结论
命令面 = 26 请求码 × 双服务 (fast 发送专用端口); 处理器按功能分组绑独立线程池; 5.x REPLY 面新增。
源码位置: BrokerController.java:478-486,1070-1151
