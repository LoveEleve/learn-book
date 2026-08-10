# MULTI加EXEC一定能保证转账正确 — 那你再想想, 出队出错Redis怎么处理的?

> Cluster C: 4 KPs | 依赖: 域4-01(单线程), 域4-06(Reactor事件) | 读者基线: 用过MULTI/EXEC, 看过PUB/SUB, 听说过客户端缓存

---

### 1. Redis事务不是ACID — 是"打包执行", 不是"回滚保护"
  你写MULTI→SET balanceA 50→SET balanceB 150→EXEC — 如果中间SET出错呢?
  - Redis事务流程: MULTI(开始入队)→命令排队(不执行, 只放入队列)→EXEC(一次全部执行) — 执行期间不服务其他客户端(单线程天然原子) (B1 Ch4 §4.1)
  - 两种错误: 入队错误(语法/WRONG TYPE→MULTI返回错误→EXEC不执行) vs 执行错误(对string LPOP→EXEC执行, 该条失败其他继续→不回滚) [工程: 入队错误=整体拒绝; 执行错误=单条失败→不回滚 — Redis的"不回滚"是设计选择, 不是bug — 保持简单和性能]
  - WATCH乐观锁: WATCH balanceA→MULTI→...→EXEC — 如果WATCH的key在MULTI和EXEC之间被其他客户端修改→EXEC返回nil(不执行) — 乐观锁不是悲观锁, 是"尝试+检查" (B1 Ch4 §4.1)
  - 关键设计: Redis事务不用两阶段提交 — 不需要和数据库一样做undo log — 承认"Redis事务做的是打包, 不做的是回滚" — 用WATCH乐观锁做条件执行 ← 这其实是CAS的变形

### 2. 发布订阅 — "我发了, 但谁在听? 没有人? 那我不管"
  你用PUBLISH发送消息 — 没人订阅 — 消息丢了。没有存储。
  - PubSub原理: pubsub_channels(字典: 频道→订阅客户端列表)/pubsub_patterns(列表: 模式→所有匹配频道的客户端, 每个模式扫描全部频道) — 发消息=遍历列表 (B1 Ch4 §4.4)
  - PubSub的致命限制: 不存消息(发了就没了)/不确认ACK(客户端收没收到不管)/客户端断线消息全丢 — 这不是Kafka, 是不可靠广播 [案例: 发布订阅做IM → 用户掉线→这段时间的消息全丢了 → 换Stream消费者组+ACK→消息持久有回溯]
  - 关键设计: Redis PubSub vs Redis Stream: PubSub=0可靠性, Stream=消息不丢; PubSub=发完即忘, Stream=ACK+PENDING; PubSub=无消费者组, Stream=消费者组均衡消费 — 这不是改进, 是两个不同的模型

### 3. 客户端缓存 — 让数据离客户端更近, 但更新要及时
  你做了本地缓存, 但数据变了要挨个通知 — Redis tracking帮了你一把。
  - tracking模式: CLIENT TRACKING on → Redis记录哪些客户端读了哪些key(invalidation table/radix tree)→写操作触发→invalidate消息发给对应客户端 [工程: invalidation table存储的是"哪个客户端读了哪个key" — 用完就删(消息发过即清), 不长期保留→内存开销有限]
  - 广播模式(BCAST): 前缀订阅→key前缀匹配→invalidate→广播给所有订阅者 — 准确但开销大(写一个key要通知所有人) — Tracking模式只在"有人用了才通知" (B1 Ch4 §4.5)
  - 重定向模式(REDIRECT): 客户端告诉Redis把invalidate消息发给另一个客户端(代理) — 代理可以统一管理多客户端的缓存 — 适合Redis上层有代理层(如Envoy) (B1 Ch4 §4.5)
  - 关键设计: 客户端缓存不是Redis独有的 — 这是Server-assisted client-side caching — Redis给出失效通知, 客户端决定是否重新拉取 — 解耦很清晰

### 4. 使用规范 — 不是所有Redis的O(1)都是真O(1)
  你发现接口突然超时 — `KEYS pattern*`扫了200万条key, 每条微秒, 加起来2秒。
  - 禁用名单: KEYS(全量扫, O(N))→SCAN(游标迭代, O(1)分批), SMEMBERS(全量取)→SSCAN, HGETALL→HSCAN — 全量操作在单线程上的代价就是阻塞所有人 (B1 Ch5 §5.2)
  - 连接池: maxTotal≠maxActive — 连接数×客户端数不能超过Redis maxclients — 连接池不是越大越好, 是够用就行(每个连接有内存开销) (B1 Ch5 §5.2)
  - pipeline: 打包多个命令一次发送→一次接收 — 减少RTT(不是减少命令执行时间) — 别把10000个GET打包成1个pipeline(拆包时间长) [案例: 循环SET 1000次→耗时=1000×RTT= ~200ms → pipeline SET 1000次→1次RTT + 1000次SET时间 = ~3ms]
  - 关键设计: Redis性能上限 = 网络带宽(请求/响应大小) + CPU(命令复杂度) + 内存(O(N)命令) — 三个维度任何一个瓶颈都会拖慢全局

### 5. 收束 — 事务/PubSub/客户端缓存/规范 — 四个"半场景"
  - 事务: 有点用(WATCH乐观锁)但不可靠(无回滚) — 金额交易用分布式事务别用Redis事务
  - PubSub: 适用于实时推送(进度通知/临时状态广播), 不适用关键消息(交易通知)
  - 客户端缓存: tracking搭配本地Caffeine效果好, 但需考虑客户端重启后invalidation丢失
  - 使用规范: 不是"Redis不好", 是"你用错了" —  改Pipelined替代循环, SHCAN替代KEYS

---

### 核心悬念
**"你学会了用, 但线上仍在报Cache Penetration——布隆过滤器防穿透, 那缓存的击穿和雪崩呢? 缓存和DB不一致又怎么办?"**

→ 引出 缓存穿透/击穿/雪崩 三剑客 + 缓存与数据库一致性方案 — 用Redis稳产 (08-cache-penetration-consistency)
