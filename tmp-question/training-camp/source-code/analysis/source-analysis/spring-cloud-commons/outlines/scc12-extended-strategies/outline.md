# SCC-12 LoadBalancer 扩展策略 — 实例列表的"六种滤镜": 加权/区域/hint/粘性/子集/同实例

> 前置: [[SCC-6-Supplier]] (链扩展) + [[SCC-7-ReactorLoadBalancer]] (策略消费) | 对照: Ribbon 的 WeightedResponseTimeRule + ZoneAwareLoadBalancer
> 🟡 B | 方案 B (标准) | 闭环: q1(加权) q2(区域) q3(hint/粘性) q4(子集/同实例)
> Pass 2 闭环: q1(weight 元数据) q2(zone 回退) q3(cookie 匹配) q4(分桶算法)

**读者处境**: 实例列表到手后还能怎么"加工"? 加权怎么算? 同区域怎么优先? 粘性会话怎么实现? 大集群怎么降采样?

### 1. WeightedServiceInstanceListSupplier — 权重展开与元数据 weight

场景: 实例怎么按权重被选择?
源码路径:
- WeightedServiceInstanceListSupplier (core/WeightedServiceInstanceListSupplier.java:37): extends Delegating
- **METADATA_WEIGHT_KEY = "weight"** (L41) + **默认 weightFunction = metadataWeightFunction** (L50, 从实例元数据取 weight)
- **核心 buildWeightedList** (L85-113): ① 权重 **≤ 0 → DEFAULT_WEIGHT** (L91-95) ② **异常 → DEFAULT_WEIGHT** (L99-103) ③ 返回 **LazyWeightedServiceInstanceList** (L106) — 非排序/展开
- **LazyWeighted 懒展开** (LazyWeightedServiceInstanceList.java:33-75): **GCD 归一化** (L46-51, weights 求最大公约数 + total/GCD 展开数组 — 防大权重爆炸) + **get(index) 才 selector.next() 逐步展开** (L57-66, synchronized expandingLock) + WeightedServiceInstanceSelector 选择器
- metadataWeightFunction 默认 (L115-125): 无 weight 元数据 → **DEFAULT_WEIGHT**
- WeightFunction 接口 (core/WeightFunction.java): 自定义权重函数
关键设计 (q1): **"权重 = 实例元数据的 weight 字段"是默认约定** — 注册中心可注入权重元数据; WeightFunction 是可插拔的 (用户自定义算法); 权重影响选择分布 (SCC-7 消费展开后的列表)。 [模式: 元数据权重 + SPI]

### 2. ZonePreferenceServiceInstanceListSupplier — 区域优先 + 回退

场景: 同区域实例怎么优先?
源码路径:
- ZonePreferenceServiceInstanceListSupplier (core/ZonePreferenceServiceInstanceListSupplier.java:40): extends Delegating
- **ZONE 元数据键 = "zone"** (L42) + **spring.cloud.loadbalancer.zone 属性** (Javadoc L33)
- **过滤语义** (Javadoc L31-34): 按 zone 过滤 + **无匹配 → 回退全部** (Javadoc 明示 "If the zone is not set or no instances are found for the requested zone, all instances retrieved by the delegate are")
- LoadBalancerZoneConfig (L44): zone 配置持有
关键设计 (q2): **"区域优先但绝不空手"** — zone 过滤后若无实例, 回退全部 (避免区域故障导致全失败); zone 来源: 属性配置; 实例 zone 在元数据。 [模式: 优先 + 回退]

### 3. HintBasedServiceInstanceListSupplier — hint 过滤

场景: 按提示词 (hint) 过滤实例?
源码路径:
- HintBasedServiceInstanceListSupplier (core/HintBasedServiceInstanceListSupplier.java:40): extends Delegating
- **getHint 双来源** (L63-74): RequestDataContext 头 (L66, hintHeaderName 配置 L79-81) / **HintRequestContext.getHint()** (L68-69)
- **filteredByHint** (L87-93): 按 hint 过滤实例列表 — **实例元数据 "hint" 键匹配** (L93); 无 hint → 返回全部 (L88-90)
关键设计 (q3): **hint 是"请求级提示"** — 不同请求可带不同 hint (如版本/分组); hint 来源: 请求头或上下文; 无 hint 不过滤。 [模式: 请求级过滤]

### 4. RequestBasedStickySessionServiceInstanceListSupplier — cookie 粘性

场景: 同一会话怎么固定到同一实例?
源码路径:
- RequestBasedStickySessionServiceInstanceListSupplier (core/...:40): extends Delegating
- **instanceIdCookieName** (L60): properties.getStickySession().getInstanceIdCookieName() — cookie 名配置
- **cookie 匹配** (L63-68): RequestDataContext.getClientRequest().getCookies().getFirst(instanceIdCookieName) → 匹配实例
- **cookie 未找到 → 返回全部** (L73, LOG.debug "Cookie not found. Returning all instances")
关键设计 (q4): **"cookie 粘性 + 无 cookie 回退"** — 会话第一次无 cookie 返回全部 (让 LoadBalancer 选), 之后 cookie 固定实例; 粘性依赖客户端带 cookie。 [模式: cookie 会话保持]

### 5. SubsetServiceInstanceListSupplier — 分桶降采样

场景: 大集群怎么只选部分实例?
源码路径:
- SubsetServiceInstanceListSupplier (core/SubsetServiceInstanceListSupplier.java:42): extends Delegating
- **size 配置** (L46/L53): properties.getSubset().getSize() — 子集大小
- **分桶算法** (L64-78): 实例 ≤ size → 全部 (L64); 否则 **count = instances.size() / size** (L71) + **bucket * size 起点** (L78) — 从当前位置均匀取 size 个
- Javadoc (L37): Netflix subsetting 算法引用
关键设计 (q4): **"分桶均匀降采样"** — 大集群只暴露 size 个实例给 LoadBalancer (减少选择集); count = 总数/size 分桶 + bucket 位移实现均匀; Netflix 算法承袭。 [模式: 分桶降采样]

### 6. SameInstancePreferenceServiceInstanceListSupplier — 同实例偏好

场景: 选中过的实例怎么优先再选?
源码路径:
- SameInstancePreferenceServiceInstanceListSupplier (core/...:38): extends Delegating + **SelectedInstanceCallback** (L39)
- **selectedServiceInstance 覆写** (L94-95): super + **preferredInstance 记录** — 选中后记住实例
- **get 时偏好** : preferredInstance 在列表中 → 排前/过滤
关键设计 (q4): **"选中记忆 + 下次偏好"** — 与 SCC-7 的 SelectedInstanceCallback 联动 (LoadBalancer 选中后回调); 偏好让"热实例"持续被选 (减少抖动)。 [模式: 选择记忆]

## 代码类型
Architecture (策略扩展) + Algorithms (分桶/权重)

## 负面空间 — 扩展策略刻意不做的事

- **不做动态权重调整**: 权重静态 (元数据), 无运行时响应时间反馈 (对比 Ribbon WeightedResponseTimeRule)
- **不做多区域感知算法**: 只有 zone 过滤, 无跨区域故障转移策略
- **不做粘性失效清理**: cookie 匹配不到实例时静默回退, 无显式失效处理
- **不做子集动态重算**: size 配置固定, 无自适应
- **不做同实例负载上限**: 偏好无"过载保护" (一直选同一实例可能压垮它)
- **不做组合策略编排**: 各 Supplier 独立, 组合靠 SCC-6 Builder

→ 引出: 全部策略汇总 → 阶段 5.1 Feign/5.5 Gateway 消费 (收束)
