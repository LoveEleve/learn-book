# 闭环笔记 q3: 连接与会话 — primeConnection + 超时面

## 假设
重连 = 会话恢复 + watch 重注册; 超时双面。

## 验证过程
- **primeConnection** (L1005-1095): **ConnectRequest(0, lastZxid, sessionTimeout, sessId, sessionPasswd, readOnly)** — **seenRwServerBefore ? sessionId : 0** (L1011: 重连恢复 vs 新会话)
- **watch 重注册** (L1015-1081): DISABLE_AUTO_WATCH_RESET 配置 + 五类 watch 列表 (data/exist/child/persistent/persistentRecursive) + **分批 SET_WATCHES_MAX_LENGTH** + **SetWatches vs SetWatches2** (持久 watch 兼容 L1066-1075) + outgoingQueue.addFirst (倒序)
- **auth 重放** (L1083-1091): authInfo → AUTHPACKET_XID 包
- **conReq 最后 addFirst** (L1092): **连接包在最前** (先连接 → auth → watch)
- **startConnect** (L1132-1167): 重连 **sleep 1000 随机** (防风暴) + CONNECTING + SASL + connect
- **超时面** (L1244-1258): **会话超时 = expirationTimeout - idleRecv → SessionTimeoutException** / 连接超时 = connectTimeout → ConnectionTimeoutException
- **readConnectResult** (ClientCnxnSocket:131-150): ConnectResponse → **onConnected (sessionId/passwd/timeout 更新)**
- **read-only** (L1278-1284): CONNECTEDREADONLY → 找 RW server (pingRwTimeout 指数 100→60000)

## 代码类型
Implementation (连接会话)

## 跨域关联
- Z-5: 会话协商 (服务端 processConnectRequest 钳制)
- Z-6: watch 重注册 (setWatches)

## 结论
连接 = ConnectRequest (重连带 sessionId) + watch 重注册 (分批) + auth 重放; 超时双面; read-only 退避找 RW。
源码位置: ClientCnxn.java:1005-1095,1132-1167,1244-1284; ClientCnxnSocket.java:131-150
