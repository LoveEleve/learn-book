# SCC-7 ReactorLoadBalancer 策略 — 一个原子计数器的轮询: 响应式负载均衡策略

> 前置: [[SCC-6-Supplier]] (实例列表) + [[SCC-13-NamedContextFactory]] (每服务上下文) | 引出: [[SCC-11-BlockingLoadBalancer]] (阻塞消费) + [[SCC-12-扩展策略]] | 对照: Netflix ocelli + Ribbon RoundRobin
> 🔴 A | 方案 A (全深度) | 闭环: q1(接口面) q2(轮询数学) q3(响应处理) q4(每服务工厂)
> Pass 2 闭环: q1(ReactorLoadBalancer) q2(incrementAndGet & MAX) q3(SelectedInstanceCallback) q4(LoadBalancerClientFactory)

**读者处境**: 轮询怎么"均匀"分配? 为什么 position 要 `& Integer.MAX_VALUE`? 单实例时为什么不转位置? 随机种子哪来的? 每个服务的 LoadBalancer 怎么隔离?

### 1. ReactorLoadBalancer 接口 — 响应式选择的契约

场景: 负载均衡策略的统一接口?
源码路径:
- ReactorLoadBalancer\<T\> (core/ReactorLoadBalancer.java:31): **extends ReactiveLoadBalancer\<T\>** + **choose(Request) → Mono\<Response\<T\>\>** (L39) + **choose() default** (L41-43, 用 REQUEST 常量)
- ReactiveLoadBalancer.Factory: 每服务工厂接口 (SCC-5 BlockingLoadBalancerClient 消费 L61)
- 实现: RoundRobinLoadBalancer / RandomLoadBalancer (core/)
关键设计 (q1): **choose 返回 Mono\<Response\>** — 响应式选择 (异步实例获取); choose() 无参默认 REQUEST; Factory 是"每服务创建 LoadBalancer"的工厂 (LoadBalancerClientFactory 实现)。 [模式: 响应式策略接口]

### 2. RoundRobinLoadBalancer — 构造与惰性 Supplier

场景: 轮询器怎么初始化?
源码路径:
- RoundRobinLoadBalancer (core/RoundRobinLoadBalancer.java:43): implements ReactorServiceInstanceLoadBalancer
- **position = AtomicInteger** (L47) + **seedPosition 随机种子** (L60: `new Random().nextInt(1000)` — 启动随机化防惊群!)
- **SingletonSupplier.of(provider.getIfAvailable(NoopSupplier::new))** (L64-66) — 惰性解析 Supplier, 无则 Noop 兜底
- serviceId 字段 (L48)
关键设计 (q2): **随机种子防"多实例同时从 0 开始"** — 每个实例启动随机偏移, 避免冷启动惊群; SingletonSupplier 惰性 (首次 choose 才解析); Noop 兜底防 NPE。 [模式: 随机种子 + 惰性单例]

### 3. choose 主流程 — supplier.get → next → process

场景: 选择过程的数据流?
源码路径:
- **choose** (L82-86): `supplier.get(request).next().map(processInstanceResponse)` — **取列表第一个 Flux 元素 (完整列表) → 处理**
- processInstanceResponse (L89-96): getInstanceResponse → **supplier instanceof SelectedInstanceCallback && hasServer → selectedServiceInstance 回调** (L93-95, SCC-6 联动); **测试实证 "Supplier 或其 Delegate"** (RoundRobinLoadBalancerTests:75-85: RetryAware 包装 delegate, 回调经 Delegating 传递到 delegate)
- getInstanceResponse (L72-85): 三态
关键设计 (q1): **supplier.get(request) 返回 Flux\<List\>, .next() 取第一个列表** — 响应式链: 列表获取 (SCC-6) → 选择 → 回调; SelectedInstanceCallback 让链上各层知道选中了谁。 [模式: 响应式链]

### 4. getInstanceResponse — 轮询数学三态

场景: 轮询怎么处理空/单/多实例?
源码路径:
- **三态** (L98-116): ① **空列表 → EmptyResponse + warn** (L100-103, "No servers available") ② **单实例 → DefaultResponse(直接返回)** (L106-109, 注释 "Do not move position when there is only 1 instance, especially some suppliers have already filtered") ③ **多实例 → position.incrementAndGet() & Integer.MAX_VALUE** (L113-116)
- **& Integer.MAX_VALUE 语义** (L114): **忽略符号位** — position 从 0 循环到 MAX_VALUE 再回 0 (防负数)
- 取模: `instances.get(pos % instances.size())` (L116)
关键设计 (q2): **"& MAX_VALUE 忽略符号位"是防溢出循环** — AtomicInteger 递增到 MAX 后 incrementAndGet 会溢出为负, & MAX_VALUE 保证 0~MAX 循环; 单实例不转位置 (过滤型 supplier 已处理); 空列表 EmptyResponse (warn 不抛)。 [模式: 位运算循环 + 三态]

### 5. RandomLoadBalancer — 随机选择的同构实现

场景: 随机策略与轮询差多少?
源码路径:
- RandomLoadBalancer (core/RandomLoadBalancer.java:41): implements ReactorServiceInstanceLoadBalancer (L42)
- **同构**: SingletonSupplier (L49-51) + choose (L55-61) + processInstanceResponse (L63-70)
- **getInstanceResponse 差异** (L78-87): 空 → EmptyResponse + warn (L80-83); **多实例 → ThreadLocalRandom.current().nextInt(size)** (L86)
- 无单实例特判 (随机天然无需)
关键设计 (q1): **策略差异只在 getInstanceResponse** — choose/process/回调完全同构 (模板方法); ThreadLocalRandom 线程本地随机 (无锁); Random 无 seed (无需防惊群)。 [模式: 同构策略 + 局部差异]

### 6. LoadBalancerClientFactory — 每服务的 LoadBalancer 工厂

场景: 每个服务的 LoadBalancer 怎么创建和隔离?
源码路径:
- LoadBalancerClientFactory (support/LoadBalancerClientFactory.java:46-47): **extends NamedContextFactory + implements ReactiveLoadBalancer.Factory\<ServiceInstance\>** — SCC-13 消费实证
- **NAMESPACE="loadbalancer" + PROPERTY_NAME="loadbalancer.client.name"** (L55-58) — 子上下文属性源命名空间 (SCC-13 buildContext 注入)
- **构造传 LoadBalancerClientConfiguration.class 为 defaultConfigType** (L63) — 每个子上下文默认装配
- **getInstance(serviceId)** (L79-80): `getInstance(serviceId, ReactorServiceInstanceLoadBalancer.class)` — **从子上下文取 LoadBalancer**
- **默认策略: RoundRobinLoadBalancer** (LoadBalancerClientConfiguration.java:74-75, getLazyProvider 拿 Supplier) + **默认 Supplier 链 = withDiscoveryClient().withCaching()** (L89, SCC-6 Builder 消费)
- 装配: LoadBalancerClientConfiguration 默认链 (SCC-6 Builder 消费) + @LoadBalancerClient 自定义策略 (annotation 面)
- 每服务上下文: 子上下文隔离策略配置 (SCC-13 default. 前缀机制)
关键设计 (q4): **"每服务一个子上下文 = 每服务一个 LoadBalancer"** — LoadBalancerClientFactory 用 NamedContextFactory 为每个 serviceId 建独立上下文; getInstance 从对应上下文取策略 Bean; 配置隔离 (SCC-13 全机制复用)。 [模式: 每服务上下文工厂]

## 代码类型
Architecture (策略核心) + Concurrency (原子计数器)

## 负面空间 — ReactorLoadBalancer 策略刻意不做的事

- **不做加权轮询内建**: WeightedSupplier 是 SCC-12 的职责 (SCC-6 Builder withWeighted)
- **不做粘性会话**: StickySession Supplier (SCC-12), LoadBalancer 本身无状态
- **不做重试**: RetryAware Supplier + RetryLoadBalancerInterceptor (SCC-11)
- **不做实例排序**: 依赖 Supplier 链的顺序 (SCC-6)
- **不做健康检查**: 过滤在 Supplier 链 (SCC-6 HealthCheck), LoadBalancer 只选
- **不做多策略切换**: 每服务固定一个策略 Bean, 无运行时切换

→ 引出: 阻塞式怎么消费这些响应式策略? → SCC-11 BlockingLoadBalancer 重试
