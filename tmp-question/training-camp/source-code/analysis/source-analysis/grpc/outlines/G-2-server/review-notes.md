# G-2 review-notes — 六层深审 + 07 全量维度审查记录

> 2026-08-16 | 锚点回源全部重新 grep, 零发现=不合格原则执行

## 第一轮: 六层深审 (交付时)

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 全部 file:line 重新 grep | 19 个唯一锚点 (≥8 ✅), 抽查命中; **1 处修正**: ServerInterceptors.java:44-46 → 250/266-269 |
| 2. 数字穷举 | GOAWAY 两阶段/TOO_MANY_RESPONSES/MISSING_RESPONSE | ✅ |
| 3. 代码块逐字 | "services are added/replaced atomically"/"so jumpListener..." /"Too many pings" | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-1 / → G-3 + G-6 | ✅ |
| 6. 五维检查 | 场景/源码/关键设计/跨层/悬念 | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16 用户要求按方法论再深审)

### R1 锚点回源 + 声明准确性

**19/19 锚点全部命中** (逐行 sed 验证: NettyServerBuilder:176 委托构造 ✅ / ServerImplBuilder:109 回调接口 ✅ / :257-263 new ServerImpl ✅ / ServerImpl:99,187,490,545,548-549,561,669-670 ✅ / ServerCallImpl:104 sendHeaders ✅ / ServerInterceptors:250 InterceptCallHandler ✅ (上轮已修) / NettyServerHandler:1071 GracefulShutdown ✅ / ProtocolNegotiators:424 ServerTlsHandler ✅ / 测试锚点 262/419 ✅)。无新修正。

### R2 内容深度 (03 §深度不足检测)

| 发现 | 严重度 | 修复 |
|---|---|---|
| 闭环字数 1643-2093 字符, 全 >200 字 ✅ | — | 无需 |
| **跨域关联仅 2/9** (q5/q9) — 03 标准: "没有任何跨域关联提及 → 深度不够" | 高 | **9/9 闭环补跨域标注**: q1 (inprocess/binder 复用)、q2 (fullMethodName 来自 G-1)、q3 (客户端对称双执行器)、q4 (G-3 withInterceptors 对照)、q6 (G-3 消费 GOAWAY)、q7 (客户端协商对称 tlsClientFactory)、q8 (SerializingExecutor 同源) |
| §5 "开发期暴露" 表述不准确 (checkState 是运行时抛) | 中 | 改为 "立即失败 (运行时即时暴露)" — outline + pass2-q5 同步 |

### R3 结构完整性 (07 §维度1/3/5)

锚点 19→**22** (补 maxConcurrentCalls) | 桥: IN ← G-1 ✅ / OUT → G-3+G-6 ✅ | 前向引用: 依赖只指向前位域 ✅ | 负面空间 6 条含理由 ✅ | 开篇: 具体场景 (三行代码 start()) ✅ — **无修复**

### R4 横切关注点 (07 §维度4)

| 横切面 | 发现 | 修复 |
|---|---|---|
| **服务端背压链** | **maxConcurrentCallsPerConnection 未覆盖** — 每连接并发流上限 (NettyServerBuilder.java:105 默认 Integer.MAX_VALUE, 经 SETTINGS_MAX_CONCURRENT_STREAMS 通告 NettyServerHandler.java:283) | **§6 补"每连接并发流上限"** — 服务端背压第一道闸 |
| 线程 (传输/串行/应用) | §3+§8 已覆盖 | ✅ |
| 错误路径 (UNIMPLEMENTED/TOO_MANY_RESPONSES/MISSING_RESPONSE) | §2+§5 已覆盖 | ✅ |
| 压缩协商 (headers 时刻) | §5 已覆盖 | ✅ |
| 安全 (TLS/ALPN/SecurityLevel) | §7 已覆盖 | ✅ |

### 反写测试 (07 §反模式 5)

每节有场景+源码路径+关键设计 (含被放弃方案)+跨层+边界 (UNIMPLEMENTED/双响应/ALPN 失败/ping 限频) → 可写 ✅

## 结论

两轮审查累计修复: **跨域标注 2/9→9/9**、**§5 表述修正**、**maxConcurrentCalls 补入 §6** (锚点 19→22)。机制性错误 0 残留。**达到合格标准**。
