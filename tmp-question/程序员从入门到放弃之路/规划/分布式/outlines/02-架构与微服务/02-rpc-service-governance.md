# 微服务调了下单, 库存扣了但订单显示失败—RPC的超时重试陷阱

> Cluster A: 12 KPs | 依赖: 01-communication-foundation | 读者基线: 了解HTTP和序列化

---

### 1. 从IPC到RPC — "像调本地方法一样调远程方法"为什么是个幻觉?
  你在本地调orderService.create(), 返回值立即拿到 — 但调远程OrderService时, 网络延迟+序列化+线程模型让这"10行代码"变成了4个C10K问题
  - B1 Ch3 §1: IPC演进 — 管道(pipe)→消息队列(POSIX mq)→共享内存(shm)→Socket, RPC=远程版的函数调用, 代价是7层透明性全部打破
  - RPC核心四元素: Stub(客户端代理/封装序列化+网络)+Skeleton(服务端骨架/反序列化+反射调用)+序列化协议+传输协议 (B1 Ch3 §2)
  - 关键设计: 为什么RPC不能真的像本地调用? — 网络不可靠(丢包/超时/乱序), 延迟不可预测(10ms~10s), 部分失败(对方宕机), 并发模型完全不同
  - gRPC vs Dubbo: gRPC基于HTTP/2+Protobuf(跨语言), Dubbo基于TCP+自定义协议(Java生态深度集成) (B4 Ch2 §7.8)

### 2. 服务注册与发现 — 微服务100个实例, 怎么知道该调哪个?
  你今天上线10个服务, 明天上线100个 — IP:Port硬编码第一个爆掉
  - B1 Ch3 §3: 注册中心三要素 — 服务注册(启动时注册IP:Port+元数据), 服务发现(定期拉取+长连接推送), 健康检查(心跳/探活, 剔除不健康节点)
  - ZK vs Etcd vs Nacos: ZK(CP, 临时节点watch, 会话断开自动删除), Etcd(Raft CP, Leased Key+Watch), Nacos(AP+CP可切换, 阿里出品) (B4 Ch2 §11)
  - ZK顺序节点价值: 临时顺序节点→分布式锁+选举, watch通知→配置热更新 (B1 Ch2 §3.2) [案例: Dubbo将provider URL注册为ZK临时节点, consumer watch /dubbo/com.xxx.OrderService/providers]
  - 关键设计: 为什么注册中心不能简单用DNS? — DNS有TTL缓存延迟(应用扩缩容不能瞬间生效), 需健康检查(宕机节点自动剔除), 需携带元数据(权重/分组/版本)

### 3. 负载均衡 — 5台机器, 请求该怎么分?
  注册中心返给你5个节点(权重/响应时间各有不同), 选哪个?
  - B3 Ch7 §2.3: 服务端LB(Nginx/HAProxy) vs 客户端LB(客户端本地缓存节点列表自己选, Dubbo/Ribbon) — 客户端LB消除LB单点但增加了客户端复杂度
  - 负载均衡算法: 轮询(RoundRobin)→随机(Random)→加权(Weighted)→最小活跃数(LeastActive, 最少并发)→一致性Hash(相同请求走相同节点, 缓存友好)  (B1 Ch3 §3)
  - 一致性Hash: 虚拟节点映射到Hash环, 增减节点只影响相邻节点, 缓存丢失最小化 [案例: Dubbo一致性Hash负载均衡用于有状态服务+本地缓存]
  - 关键设计: 最小活跃数=被调方弹性抗压 — 不是平均分配请求而是谁处理快就给谁, 慢节点自然少收请求, 实现去中心化反压

### 4. 服务治理全貌 — RPC框架的"八件套"
  Dubbo/Spring Cloud不只是在做RPC — 它们是一整套分布式治理工具
  - B4 Ch2 §8-10: Dubbo架构 — Provider(暴露服务)+Consumer(调用服务)+Registry(注册中心)+Monitor(监控); Spring Cloud: Eureka(注册)+Ribbon(LB)+Feign(声明式RPC)+Hystrix(熔断)+Zuul(网关)
  - B1 Ch3 §3: 治理八件套 — 注册发现→负载均衡→路由(灰度/分组)→限流→熔断降级→超时重试→链路追踪→配置中心
  - Feign声明式调用: @FeignClient(name="order")+接口方法→自动生成Proxy→Ribbon LB→HttpClient发送, 开发者感觉不出是远程调用 (B4 Ch2 §10)
  - 关键设计: 为什么Dubbo用私有协议而Spring Cloud用HTTP? — Dubbo追求极致性能(二进制+长连接), Spring Cloud拥抱HTTP生态

### 5. RPC重试的致命深渊 — 为什么超时重试可能扣两次钱?
  你设了3s超时, RPC框架自动重试 — 第一次调用实际上成功了(只是响应网络延迟), 重试→扣了两次库存
  - 超时设多少: 过长→请求堆积/线程池满, 过短→不必要的重试→雪崩, p99+50% buffer是经验值 (B4 Ch4 §3.5)
  - 重试策略: 固定间隔→指数退避(+jitter抖动用防止惊群)→限制最大重试次数(3次)→只重试幂等操作(GET/查询) (B4 Ch4 §3.5)
  - 幂等性: 拿重试前先检查操作唯一ID是否已执行 — 数据库UNIQUE("retry_429")+状态机("处理中"→"已完成") (B3 Ch10 §2-4)
  - 关键设计: 网络超时≠服务失败 — 客户端超时后服务端可能还在执行, 必须区分"没收到请求"和"收到了但响应超时" → 幂等+幂等号

### 6. 收束 — 回到下单RPC调用
  - RPC是分布式系统第一道分界线: 跨网络调用的延迟/失败/一致性全部不可控
  - 注册中心+负载均衡是RPC的左右手 — 一个告诉你"有哪些", 一个决定"调哪个"
  - 超时重试+幂等是RPC实用化的关键 — 没有幂等重试就是灾难

---

### 核心悬念
**"CAP理论说你不得不在一致性和可用性之间做选择, 但你的架构已经同时在用ZooKeeper(CP)和Eureka(AP)——这不是矛盾吗?"**

→ 引出 架构中的CAP: 选择一致性还是可用性不是理论问题, 是业务问题 (03-distributed-theory-architecture)
