# 闭环笔记 q2: 主从同步 — 从库主动拉取 + 双线程流水线

## 假设
"主推从拉" — 从库控制连接与节奏, 主库按请求推。

## 验证过程
- **从库侧** (DefaultHAClient, 411 行):
  - 状态机 (L310-329): **SHUTDOWN / READY (连主) / TRANSFER (同步)** — READY 连接失败 waitForRunning(5s) 重试 (L317)
  - connectMaster (L251-270): 主动 connect haMasterAddress → 注册 OP_READ → TRANSFER
  - transferFromMaster (L347-365): isTimeToReportOffset → reportSlaveMaxOffset (报自己 maxPhyOffset) → selector 1000ms → processReadEvent
  - **offset 强校验** (dispatchReadRequest L195-201): 主推 masterPhyOffset != 自身 maxPhyOffset (非 0) → 断连 — 落后/错乱检测
  - 落盘: appendToCommitLog(masterPhyOffset, body, start, size) — 物理 offset 直接对齐 (L207-208)
  - 回报: reportSlaveMaxOffsetPlus 每批 (L231-244); 失败 → closeMaster
  - **housekeeping**: 20s 无读 → closeMaster (L330-336); 断连 5s 重试
- **主库侧** (DefaultHAConnection, 476 行):
  - AcceptSocketService (DefaultHAService:259-379): NIO Selector 接受 haListenPort=10912
  - ReadSocketService (L211-253): 收 REPORT (8B offset) → slaveAckOffset → notifyTransferSome; 首次 → slaveRequestOffset
  - WriteSocketService (L274-384): 首连 0 → **从最近 1GB 段起点推** (L287-299); 心跳 5s 空 header (L308-326); 推 [offset(8)+size(4)+body] 12B 头, 限 32KB + canTransferMaxBytes 流控 (L337-350); 无新数据 waitNotifyObject 100ms (L368)
- **HAConnectionStateNotificationService** (150 行): 连接状态通知 (断连事件 → broker 感知)

## 代码类型
Implementation (拉取式复制流水线)

## 跨域关联
- RM-1 (协议): HA 通道独立于 remoting (NIO 原生, 非 RemotingCommand)
- RM-12 q1: 双水位推进链

## 结论
从库主动连主 + 报 offset 驱动主推; 首连从 1GB 段起点补全; offset 强校验 + 32KB/流控限速 + 5s 心跳/20s housekeeping。
源码位置: DefaultHAClient.java:251-370; DefaultHAConnection.java:211-384; DefaultHAService.java:259-379
