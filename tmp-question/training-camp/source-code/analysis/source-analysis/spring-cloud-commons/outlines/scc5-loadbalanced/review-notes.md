# SCC-5 @LoadBalanced 客户端 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 5 | **语义重大** | BlockingLoadBalancerClient.execute 完整链: **getHint + LoadBalancerRequestAdapter/buildRequestContext (RequestDataContext/DefaultRequestContext 二选一) + LoadBalancerLifecycle.onStart + choose + null 实例 → onComplete(DISCARD) + IllegalStateException + DefaultResponse 包装** — 大纲原只说 "choose block" | 已修 §5 |
| 6 | 语义补强 | DeferringLoadBalancerInterceptor 实现机制: **ObjectProvider.getIfAvailable 首次 intercept 时解析** (L40-44, 无则抛 "LoadBalancer interceptor not available") | 已修 §6 |
| 7 | 语义精确化 | **transformRequest(HttpRequest, ServiceInstance) 签名** — 变换器在实例选定后执行 (可基于实例改请求) + @Order | 已修 §3 |
| 8 | 验证通过 | 负面空间"不做实例缓存" (包内零缓存) / 跨域 SCC-5→SCC-7 (ReactiveLoadBalancer.Factory L61) / reconstructURI Javadoc 契约 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | LoadBalanced 类 **L34** (写 L22-28 — 注解区) + @Qualifier **L38** (写 L27) + @Target L34 | 已修 |
| 2 | 锚点漂移 | LoadBalancerInterceptor intercept **L50-55** + getHost **L53** (写 L48) + Assert **L54** + execute **L55** (写 L51) | 已修 |
| 3 | 锚点漂移 | LoadBalancerRequestFactory createRequest **L52-55** (写 L41-44) + BlockingLoadBalancerRequest **L54-55** | 已修 |
| 4 | 验证通过 | LoadBalancerClient L29/42/56/67 / BlockingLoadBalancerClient L59/148-149/163 (Mono.from.block) / AutoConfiguration L55/60/68/86 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniLoadBalanced 7/7 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 验证通过 | 轮询选择实证: URL host 提取服务名 + reconstructURI 换 host:port + 轮询三次回环 | 7/7 |
| H2 | 验证通过 | transformers 请求变换 / execute 指定实例双形态 / 无 host 抛错 — 全部实证 | 7/7 |

## 锚点密度统计

- file:line 锚点数: **20+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 4 项检查 3 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (Ribbon 兼容/实例缓存/重试内建/异步支持/协议校验/重放保护)
- [x] 每条有对照物 (Ribbon ServerList/RetryLoadBalancerInterceptor/WebClient 响应式)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (URL 变真实地址/服务名提取/延迟拦截器/阻塞包装响应式)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (@LoadBalanced 收集 → 拦截 → 服务名 → execute → choose → reconstructURI)
- [x] 边界交代: 双形态 execute/DeferringLoadBalancerInterceptor 时序/Assert 校验

## 方法论教训

- **"URL host 即服务名"是核心约定** — @LoadBalanced 的全部魔力建立在这个约定上, 大纲必须凸显
- **BlockingLoadBalancerClient 是"响应式内核 + 阻塞门面"** — Mono.from(...).block() 一句话说清阻塞包装的本质
- **@since 注释是时空溯源金矿** — 4.1.2 DeferringLoadBalancerInterceptor/4.2.0 Builder 后处理器标记了时序问题的解决演进
