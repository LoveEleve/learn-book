# Z-7 Client API — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | 门面骨架: ZooKeeper 同步/异步双 API + ClientCnxn (SendThread/EventThread) + XID 匹配 (L120-127 注释锚) + ping 机制 |
| 3.5.x | **SASL 认证** (startConnect L1147-1163); hostProvider 多地址; read-only 模式 (CONNECTEDREADONLY) |
| 3.6.x | **持久 watch**: setWatches2 (SetWatches vs SetWatches2 兼容 L1066-1075) + AddWatchRegistration (4.0 协议 addWatch) |
| 3.9.x | DISABLE_AUTO_WATCH_RESET 配置 (L1015); WatchRemoved 事件 (deregistration L730-750) |

## 痕迹证据

- ClientCnxn.java:120-127: XID 预定义注释 (3.4 锚)
- ClientCnxn.java:1015: DISABLE_AUTO_WATCH_RESET (3.6+ 配置面)
- ClientCnxn.java:1066-1075: SetWatches vs SetWatches2 兼容注释 (3.6 锚)
- ClientCnxn.java:1147-1163: SASL (3.5 锚)
- ClientCnxn.java:1278-1284: read-only 模式 (3.4+)

## 推断标注

- "3.4.x 骨架" — 公知版本线 (客户端协议 3.4 定型) (标注)
- "3.5.x SASL/read-only" — 特性年代推断 (标注)
- "3.6.x setWatches2" — 持久 watch 3.6 推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
