## §六 20 问 (A 5 / B 6 / C 5 / D 4)

### A. 机制理解 (5)
1. GroupChannel 为什么组合三大件 (Receiver/Sender/Membership) 而不是单类?
2. 组播成员发现的优势? 为什么不适合大集群?
3. 拦截器链能加什么? (心跳/分片/流量控制)
4. 会话复制为什么用事件而不是全量?
5. 主/备归属 (mapOwner) 决定什么?

### B. 源码实证 (6)
6. GroupChannel 实现的接口? (grep L67)
7. heartbeat() 做了什么? (grep L183)
8. McastServiceImpl 双线程字段? (grep L59-61)
9. sendFrequency/expireTime 语义? (grep L151-176)
10. replicate 的三条件? (grep AbstractReplicatedMap L441-453)
11. DeltaManager 事件类型有哪些? (grep EVT_)

### C. 推理深挖 (5)
12. 组播 UDP 丢包怎么办? 心跳的容错?
13. expireTime = 2×sendFrequency 的含义? 为什么 2 倍?
14. 脏复制 (isDirty) 的时序问题? 并发写怎么保证复制完整?
15. 会话复制失败 (节点宕机) 的恢复? 全量拉取?
16. 大集群为什么不用全组播? (广播风暴)

### D. 跨域扩展 (4)
17. 本域 vs Nacos 服务发现: 中心化 vs 去中心化?
18. 本域 vs T-9 WebSocket: 长连接为什么不能复制?
19. 本域 vs T-1 Lifecycle: 集群组件的生命周期怎么级联?
20. 本域 vs openjdk: 心跳机制 vs JVM Safepoint 同步的时序对照?

---

