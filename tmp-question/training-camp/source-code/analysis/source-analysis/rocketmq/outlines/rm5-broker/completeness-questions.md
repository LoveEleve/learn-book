# RM-5 Broker 启动 — completeness-questions (全视角提问验证)

## 开发者视角

1. 启动失败会怎样? (initialize false → exit(-1))
2. 7 个 configManager 是什么? (topic/queueMapping/offset/group/filter/order)
3. Default vs RocksDB 存储怎么选? (enableRocksDBStore)
4. 48 个处理器注册怎么分布? (26 码 × 双服务)
5. fastRemotingServer 是什么? (listenPort-2 发送专用)
6. 定时任务保什么? (持久化/主从/积压/保护)
7. ReplicasManager fenced 是什么? (5.x controller 初始隔离)
8. shutdown 顺序? (逆序 + 收尾持久化)

## 架构师视角

9. 三阶段装配的顺序依据? (元数据→存储→恢复注册 — 依赖序)
10. 双端口 (fast) 的设计动机? (发送流量分离)
11. 处理器分组线程池隔离? (心跳不被拉取拖垮)
12. 恢复序为什么 store→schedule→插件? (依赖链)
13. 插件化装配 (存储/附件) 的扩展面? (5.x 即插即用)
14. namesrv 周期注册的钳制? (10-60s 防风暴)
15. 从库代主隔离? (slave-act-master 5.x 高可用演进)
16. 关闭收尾持久化的意义? (崩溃窗口最小化)

## 学生视角

17. Broker 启动和 Redis 启动像吗? (都是装配+命令注册+周期任务)
18. 为什么叫"枢纽"? (所有域的服务都在这注册)
19. 双服务什么区别? (主端口全命令 vs fast 发送专用)
20. 测试为什么测重启? (重启幂等 = 生产核心场景)
