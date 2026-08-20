## §六 20 问 (A 5 / B 6 / C 5 / D 4)

### A. 机制理解 (5)
1. fin/rsv/opCode 为什么压在一字节里? 位运算的语义?
2. 控制帧为什么强制 fin=1 + 125 字节上限? 不校验会怎样?
3. 半包处理为什么 return false 而不是阻塞等待?
4. 掩码位为什么客户端必须置 1 (服务端帧不掩码)?
5. close 握手为什么区分本地/远端 1006?

### B. 源码实证 (6)
6. processInitialHeader 的位运算取字段? (grep L141-160)
7. 控制帧校验的 3 个条件? (grep L155-165)
8. 长度扩展的 126/127 分支? (grep processRemainingHeader)
9. controlBuffer 分配大小? (grep L55-56)
10. WsWebSocketContainer 实现的接口? (grep L79)
11. connectToServer 的重载形态? (grep L120-131)

### C. 推理深挖 (5)
12. 粘包时数据不足 return false — 谁负责再读? 调用方如何循环?
13. 分片 (continuation) 状态机怎么跨帧记忆? 中断怎么办?
14. 为什么 ping/pong 要后台周期发? 心跳的语义?
15. 写锁 vs 并发发送: 同步发送和异步发送怎么共存?
16. WsSession 与 HTTP Session 的生命周期差异?

### D. 跨域扩展 (4)
17. 本域 vs T-8: 帧协议与 HTTP 报文的解析范式差异?
18. 本域 vs T-4: NIO 通道如何承载帧流?
19. 本域 vs T-10: WebSocket 长连接能集群复制吗?
20. 本域 vs spring-websocket: JSR-356 原生 vs SockJS 的取舍?

---

