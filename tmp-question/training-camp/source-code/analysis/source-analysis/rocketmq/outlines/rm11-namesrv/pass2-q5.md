# 闭环笔记 q5: 心跳与清理 — 超时判定 + 双通道

## 假设
心跳轻量 (仅时间戳); 超时 = 定时扫描 + 通道事件双通道。

## 验证过程
- **心跳三来源**:
  1. BROKER_HEARTBEAT=904 → updateBrokerInfoUpdateTimestamp (DefaultRequestProcessor:384-395) — 轻量, 不带配置
  2. QUERY_DATA_VERSION=322 → 比对后**同样更新**时间戳 (L355) — broker DataVersion 轮询兼作心跳 (RM-5 交叉)
  3. 注册本身 → brokerLiveTable.put 带新时间戳 (RouteInfoManager:366-373)
- **超时判定** (RouteInfoManager:803-818): `lastUpdate + heartbeatTimeoutMillis < now` → **closeChannel (主动断连!) + onChannelDestroy** → 进批量注销队列
- **参数**: scanNotActiveBrokerInterval=**5s** (NamesrvController:117, 初始 5ms); DEFAULT_BROKER_CHANNEL_EXPIRED_TIME=**2min** (L70) — broker 注册时可用请求头 heartbeatTimeoutMillis 覆盖 (DefaultRequestProcessor:256)
- **双通道**: 定时扫描 (主动) + BrokerHousekeepingService 三事件 (close/exception/idle, 被动) — 任一先到即清理
- **broker 侧协同**: 10s 初注册 + [10s,60s] 周期钳制 (RM-5 已交付) — 心跳频率 ≤ 超时, 正常不误踢

## 代码类型
Implementation (心跳保活 + 超时清理)

## 跨域关联
- RM-5 (Broker): registerBrokerAll 周期 [10s,60s] / unregisterBrokerAll
- RM-1 (协议): 904/322 请求码
- RM-12 (HA): 心跳超时 → 主从切换触发链 (交叉)

## 结论
心跳 = 三来源轻量时间戳更新; 超时 = 5s 扫描 + 主动 closeChannel + 双通道事件; 2min 默认超时可被注册请求覆盖。
源码位置: DefaultRequestProcessor.java:384-395; RouteInfoManager.java:803-818; NamesrvController.java:116-118
