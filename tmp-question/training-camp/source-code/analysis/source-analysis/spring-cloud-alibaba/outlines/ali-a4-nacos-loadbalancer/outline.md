# ALI-A4 NacosLoadBalancer 权重 — "按权重随机"的服务选择器

> 前置: [[ALI-A3-发现注册]] (selectInstances 数据源) + [[SCC-7-ReactorLoadBalancer]] (ReactorServiceInstanceLoadBalancer 契约) | 引出: [[SCC-6-Supplier]] (Supplier 链) | 对照: SCC-7 RoundRobin/Random / nacos-client Balancer
> 🟡 B | 方案 B (重要域) | 闭环: q1(算法插槽) q2(权重随机) q3(过滤链) q4(IPv6)

**读者处境**: 同一个服务 3 台机器权重 1:2:3, 请求怎么按比例分发? "权重"存在哪? 不同服务能用不同负载均衡算法吗? 集群隔离和 IPv6 双栈谁处理?

### 1. 算法插槽 — LoadBalancerAlgorithm SPI 与默认权重随机

场景: 每服务自定义负载均衡算法怎么接入?
源码路径:
- **LoadBalancerAlgorithm** (loadbalancer/LoadBalancerAlgorithm.java:28): 接口 + **DEFAULT_SERVICE_ID = "defaultServiceId"** — getServiceId 标识归属服务
- **DefaultLoadBalancerAlgorithm** (DefaultLoadBalancerAlgorithm.java:30): DEFAULT_SERVICE_ID + getOrder=LOWEST_PRECEDENCE + **getInstance → NacosBalancer.getHostByRandomWeight3** — 默认权重随机
- **算法注册** (NacosLoadBalancerClientConfiguration): `Map<serviceId, algorithm>` putIfAbsent (首个生效) + NacosLoadBalancer 按 serviceId 查 (NacosLoadBalancer.java:170-176: 有则用服务专属, 无则默认)
关键设计 (q1): **"算法 = 每服务可插拔 SPI"** — 用户自定义 LoadBalancerAlgorithm Bean 声明 serviceId, 装配期聚合成 Map, 选择期按 serviceId 路由; 默认权重随机兜底。 [模式: 可插拔策略]

### 2. 权重随机 — NacosBalancer 复用 nacos-client 内核

场景: getHostByRandomWeight3 怎么实现"权重越大越常被选中"?
源码路径:
- **NacosBalancer extends Balancer** (balancer/NacosBalancer.java:38) — 继承 **nacos-client 的 Balancer**, 直接复用 getHostByRandomWeight (L49-51)
- **元数据重建** (L58-73): ServiceInstance → nacos Instance 反向重建: **nacos.weight/nacos.healthy 从 metadata 读** (L69-70) — 注释指向 A3 的 hostToServiceInstance (L64-65) — 六键元数据的消费方实证
- **instanceMap 回映射** (L60/76): Instance → ServiceInstance (按对象 key) 再返回
- **IPv6 转换** (L77-81): NacosLoadBalancer.ipv6 非空 → convertIPv4ToIPv6 (L88-95: IPv4 主机 + metadata IPv6 → setHost 替换)
关键设计 (q2): **"继承复用 = 内核算法零重写"** — 权重随机的数学 (随机数 * 总权重区间定位) 在 nacos-client 内核, 集成层只做"双向转换" (ServiceInstance↔Instance) + 元数据桥接。 [模式: 继承复用内核]

### 3. 选择流水线 — cluster 隔离 → IP 类型过滤 → 用户过滤器 → 算法

场景: choose 时实例列表经过哪些关卡才到算法?
源码路径:
- **空列表短路** (NacosLoadBalancer.java:136-139): EmptyResponse + warn
- **cluster 隔离** (L142-155): clusterName 配置存在 → 按 **nacos.cluster 元数据** 过滤; 过滤结果非空才用 (L152-154); clusterName 为空时 warn 跨集群调用 (L156-160)
- **filterInstanceByIpType** (L86-112): ipv6 非空 → 优先 IPv6 实例 (IPv4 主机需 metadata 有 IPv6 键 L90-94), 无 IPv6 回退 IPv4 (L100-104); ipv6 空 → 纯 IPv4 过滤
- **ServiceInstanceFilter 链** (L163-166): 按序逐个 filterInstance (用户扩展点, serviceInstanceFilters 列表)
- 选择器: DefaultResponse (L178) / 异常 catch → null (L180-183)
关键设计 (q3): **"流水线 = 过滤先行, 算法收尾"** — 集群/IP/用户三层过滤逐级缩窄实例集, 最后一步才交给算法 — 与 SCC-6 Supplier 链的"加工流水线"同构, 只是方向 (先过滤后选择)。 [模式: 过滤流水线]

### 4. 装配面 — 子上下文与 Supplier 链

场景: NacosLoadBalancer 在哪个上下文装配? 实例列表从哪来?
源码路径:
- **NacosLoadBalancerClientConfiguration** (loadbalancer/NacosLoadBalancerClientConfiguration.java:56): @ConditionalOnLoadBalancerNacos (ConditionalOnLoadBalancerNacos.java:22) + @ConditionalOnDiscoveryEnabled + **每服务子上下文** (LoadBalancerClientFactory 触发, SCC-13)
- **nacosLoadBalancer Bean** (L38-53): getLazyProvider(name, ServiceInstanceListSupplier) — **懒加载 Provider** (SCC-13 ClientFactoryObjectProvider 语义)
- **Supplier 双分流** (L58-114): Reactive (ReactiveDiscoveryClient) / Blocking (DiscoveryClient) + configurations=default → withDiscoveryClient / zone-preference → withZonePreference — 消费 SCC-6 Builder 链
- @ConditionalOnLoadBalancerNacos (ConditionalOnLoadBalancerNacos.java:25-29): 装配开关 (`spring.cloud.loadbalancer.nacos.enabled`)
关键设计 (q4): **"子上下文隔离 + Supplier 复用"** — 每服务一个 LB 实例 (隔离算法/过滤器状态), 实例列表统一走 SCC-6 的 Supplier 链 (缓存/健康检查复用), 集成层只管"选择"不管"获取"。 [模式: 上下文隔离 + 能力复用]

### 5. 测试与行为锚

场景: 权重随机怎么测? IPv6 行为怎么固定?
源码路径:
- 测试: NacosLoadBalancerTests / NacosBalancerTests (test 目录) — 权重/过滤/空列表
- 注释锚: "see original RoundRobinLoadBalancer" (NacosLoadBalancer.java:45-46) — 设计出处
- @since 锚: NacosLoadBalancer `@since 2021.1` (L49) / NacosBalancer `@since 2021.1` (L36) — 同版本引入
关键设计 (q1): **"出处注释 + 版本锚"** — 明确声明借鉴 RoundRobinLoadBalancer, 便于对照学习; 2021.1 引入权重 LB 是 Nacos 集成层对 Spring Cloud LB 的第一版对接。 [模式: 出处锚]
