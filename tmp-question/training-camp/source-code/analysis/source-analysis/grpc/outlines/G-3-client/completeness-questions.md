# G-3 全视角提问验证 (completeness-questions)

> 普通大域 (≈13000 行): 40+ 问 / 6 身份。每问标注大纲覆盖 (✅ 有 / ⚠️ 部分 / ❌ 无)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | syncContext | executeLater 和 execute 差在哪?什么时候用前者? | ✅ §1 锁内入队 |
| 2 | 开发者 | syncContext | 我在回调里再 execute 会死锁吗? | ⚠️ 未明说 (可重入, drain 单线程) |
| 3 | 开发者 | 生命周期 | idle 超时默认多少?怎么配? | ⚠️ 未给默认值 (Builder 配置) |
| 4 | 开发者 | newCall | PendingCall 放行时 Context 在哪个线程恢复? | ✅ §3 快照语义 |
| 5 | 开发者 | CallImpl | start 后还能 cancel 吗? | ✅ §4 (cancel L452) |
| 6 | 开发者 | CallImpl | 用户 onMessage 抛异常, onClose 还会到吗? | ✅ §4 异常优先级 |
| 7 | 开发者 | Context | 子线程自动继承 Context 吗? | ✅ §5 ContextRunnable |
| 8 | 开发者 | Deadline | deadline 过了, start 还会发消息吗? | ✅ §6 过期检查 |
| 9 | 开发者 | Delayed | DelayedListener 缓冲了哪些回调? | ✅ §7 |
| 10 | 开发者 | 传输 | 连接断开会怎样?重连机制? | ⚠️ 断线重连归 G-4 InternalSubchannel |
| 11 | 架构师 | syncContext | 为什么不用锁?CAS 抢权有什么边界? | ✅ §1 被放弃方案 |
| 12 | 架构师 | 生命周期 | 懒启动 vs 预启动的资源权衡? | ✅ §2 |
| 13 | 架构师 | newCall | 三层路径分别解决什么? | ✅ §3 |
| 14 | 架构师 | CallImpl | 为什么用户异常优先于服务端状态? | ✅ §4 |
| 15 | 架构师 | Context | PHAMT 为什么比全量拷贝好? | ✅ §5 O(log n) |
| 16 | 架构师 | Deadline | 单调时钟 vs 墙上时钟的根本差异? | ✅ §6 |
| 17 | 架构师 | Delayed | passThrough 为什么"永不回头"? | ✅ §7 原子切换 |
| 18 | 架构师 | 传输 | 连接复用 vs 每调用连接的权衡? | ✅ §8 |
| 19 | SRE | 生命周期 | 零流量时连接还活着吗? | ✅ §2 idle 回收 |
| 20 | SRE | CallImpl | 线上 DEADLINE_EXCEEDED 从哪来? | ✅ §6 |
| 21 | SRE | Deadline | 时钟回拨会影响超时吗? | ✅ §6 单调时钟 |
| 22 | SRE | 传输 | 空闲连接被中间设备断开怎么办? | ✅ §8 keepAliveWithoutCalls |
| 23 | SRE | Context | 取消传播链路断了会怎样? | ⚠️ 未明说 (取消监听器清理) |
| 24 | SRE | newCall | shutdown 后 newCall 返回什么? | ✅ §3 SHUTDOWN_STATUS |
| 25 | 学生 | syncContext | "串行化"到底是什么?和锁区别? | ✅ §1 借用线程 |
| 26 | 学生 | 生命周期 | 为什么第一个调用前通道像不存在? | ✅ §2 |
| 27 | 学生 | newCall | newCall 返回的是真实调用吗? | ✅ §3 缓冲 |
| 28 | 学生 | CallImpl | 客户端调用有哪些"提前失败"? | ✅ §4 短路 |
| 29 | 学生 | Context | Context 是线程变量吗? | ✅ §5 ThreadLocal |
| 30 | 学生 | Deadline | 超时和 deadline 有什么区别? | ✅ §6 偏移转绝对 |
| 31 | 学生 | Delayed | 缓冲期调用会丢吗? | ✅ §7 重放 |
| 32 | 学生 | 传输 | 一次 RPC 占一个连接吗? | ✅ §8 复用 |
| 33 | 性能工程师 | syncContext | 高并发下 CAS 抢权失败会怎样? | ⚠️ 未明说 (排队等 drain) |
| 34 | 性能工程师 | CallImpl | NoopClientStream 短路省了什么? | ✅ §4 零流创建 |
| 35 | 性能工程师 | Context | PHAMT 单次 withValue 成本? | ✅ §5 O(log n) |
| 36 | 性能工程师 | Deadline | 定时取消的调度精度? | ⚠️ 未明说 (调度器粒度) |
| 37 | 性能工程师 | Delayed | 大量 pending 调用的内存? | ⚠️ 未明说 (LinkedHashSet) |
| 38 | 性能工程师 | 传输 | 流控窗口默认值? | ⚠️ 窗口细节归 G-6 |
| 39 | 研究者 | syncContext | vs Reactor 单线程模型对比? | ⚠️ 有思想对比无具体 |
| 40 | 研究者 | Context | PHAMT vs CopyOnWrite/软引用方案? | ✅ §5 被放弃方案 |
| 41 | 研究者 | Deadline | gRPC 超时语义 vs HTTP 超时? | ⚠️ 未对比 (grpc-timeout 传播已提) |
| 42 | 研究者 | Delayed | 缓冲 vs 队列积压的背压? | ⚠️ 未明说 (G-6 关联) |

⚠️ 12 项 → 处理策略:
- 核心缺失补大纲: #7 (子线程继承 → §5 已有 ContextRunnable ✅ 实际覆盖, 标 ✅); #2/#33 (可重入性) → §1 补一句 "drain 单线程内可重入 (嵌套 execute 内联执行, 无死锁)"; #23 (取消监听清理) → §4 补 "removeListener 清理 (L375-377 区)"
- 归其他域 (声明桥链): #3 (Builder 默认值 G-3 可提 30min → 补一句), #10 (G-4 InternalSubchannel 重连), #36/#37/#38 (G-6 流控), #39/#41 (对照性讨论写作时展开)
