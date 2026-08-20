# RM-13 Proxy+安全 — completeness-questions (全视角提问验证)

## 开发者视角

1. proxy 两种部署模式? (LOCAL 内嵌 broker / CLUSTER 独立网关)
2. gRPC 和 remoting 各在哪个端口? (8081 / 8080)
3. 老 remoting 客户端还能连 proxy 吗? (能 — 多协议协商 + 翻译)
4. gRPC 请求怎么进管线? (ContextInit → Authentication → Authorization)
5. 签名认证用什么算法? (HmacSHA1, AclSigner)
6. 认证失败返回什么? (AuthenticationException → 拒; 授权失败同理)
7. LOCAL 模式消息怎么落盘? (直接调 broker processor — 同进程)
8. gRPC 线程池多大? (16+2×N / 队列 100000 / 入站 130MB)

## 架构师视角

9. 为什么单端口多协议? (协议协商: 首包探测, remoting 兜底)
10. 双协议为什么收敛到统一消息面? (语义统一 — 新逻辑只写一遍)
11. proxy 为什么无状态? (消费状态在 broker; 水平扩展)
12. LOCAL vs CLUSTER 取舍? (便捷 vs 隔离; LOCAL 挂一起挂)
13. 认证链为什么 authConfig 驱动? (无认证配置时零开销)
14. 授权为什么需要资源表? (有状态元数据 vs 无状态签名)
15. acl 怎么迁移? (AuthMigrator 工具)
16. 对照 Kafka? (Kafka 直连 broker 无网关; RocketMQ 5.x 网关化 — 云原生接入)
17. proxy 高可用? (无状态 + 多实例 + 客户端重试 3 次/退避 2)

## 学生视角

18. proxy 是干什么的? (统一接入网关 — 客户端只连 proxy)
19. 什么是双协议? (新 gRPC + 老 remoting 都能进)
20. 什么是签名认证? (密钥+内容算签名, 服务端重算比对)
