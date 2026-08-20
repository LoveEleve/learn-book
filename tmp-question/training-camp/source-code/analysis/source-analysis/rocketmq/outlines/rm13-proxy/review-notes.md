# RM-13 Proxy+安全 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | PLAN "双认证: SignAuthentication + ACL" — **SignAuthentication 类在 5.3.1 不存在**; 实为 **HmacSHA1 签名认证 (AclSigner, acl 模块) + 资源授权 (auth 模块) 两段式**; 认证是"签名验证"非独立类 | 大纲 §5 补注 |
| 2 | **表述精确化** | **"转发链"实为语义收敛**: 双协议 → 统一消息面 (MessagingProcessor) → 本地直写/远程转发; 非 proxy 内 processor 管线 (管线在 gRPC 入口) | 大纲 §4 补注 |
| 3 | **补充锚点** | **pipe 反转语义**: 声明序 Authorization→Authentication→ContextInit, 执行序反转为 ContextInit→Authn→Authz (RequestPipeline:28-33) — 易误读 | 大纲 §2 补注 |
| 4 | **补充锚点** | **authConfig 驱动**: authConfig==null 时管线跳过认证 — 认证默认不启用 | 大纲 §2/§5 补注 |
| 5 | 行号验证 | 全函数 36 锚点 + 跨文件 12 处 grep (ProxyStartup 180-213 / GrpcMessagingApplication 145-175 / RequestPipeline 28-33 / RemotingProtocolHandler 47-59 / ProtocolNegotiationHandler 46-58 / RemotingProtocolServer 191-217 / DefaultMessagingProcessor 110-136,158-230 / LocalMessageService 80-115 / AuthenticationPipeline 47-76 / DefaultAuthenticationHandler 64-66 / AclSigner 28-39) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双模式启动链
- 双协议协商/翻译
- 统一消息面收敛
- 认证链顺序

### 维度2 性能
- 四线程池分流 + 满则流控
- LOCAL 零网络直写
- 130MB 入站上限

### 维度3 内存
- grpc 队列 100000
- 内嵌客户端 6 个

### 维度4 一致性
- 语义统一 (双协议同结果)
- 认证透传 (AUTHORIZATION_AK)
- 授权资源表

### 维度5 负面空间 (已写入大纲 7 条)
- 不透明代理/不集群状态/不深度流控/不默认加密/不默认认证/不替代 namesrv

## 结论
RM-13 全部锚点 ~36 处验证, 6 闭环完成, **认知修正 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-1/5/11 (已交付) ✅; 对照 Kafka 直连 vs 网关 ✅; 读者处境场景化 ✅; 锚点 ~36 ✅; 负面空间 7 条 ✅; 横切 (并发/安全/协议/网关模式) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §5 未提 **认证透传链**: AuthenticationPipeline 结果 username 写入 **AUTHORIZATION_AK header** (L74-76) → 后续面/下游使用 — 认证与授权/业务解耦但需 header 传递 | 大纲 §5 补注 |
| 8 | 通过项 | 其余 ~32 句机制描述逐句对源码一致 ✅ (双模式/三端口/线程池/协商兜底/请求码分发/翻译面/统一方法集/本地直写/HmacSHA1/迁移/重试参数) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (认证透传 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | pipe 反转推导 | pipe(Authorization).pipe(Authentication).pipe(ContextInit) → ContextInit 最先执行 (最后 pipe 的 source 先跑) → Authn → Authz ✅ | 通过 |
| V2 | 双协议收敛 | gRPC (v2 服务) + remoting (activity) → 同一 MessagingProcessor 方法 → 同 broker 操作 ✅ | 通过 |
| V3 | LOCAL 直写正确性 | LocalMessageService 调 broker processor processRequest — 复用 broker 完整校验/存储链 ✅ | 通过 |
| V4 | 认证两段式 | Authn (签名, 无状态) → Authz (资源表, 有状态) — 先验证身份再检查权限 ✅ | 通过 |
| V5 | HmacSHA1 对称 | 客户端 calSignature(content, secret) == 服务端重算 → 通过; secret 不传输 ✅ | 通过 |
| V6 | 无状态网关推导 | 消费状态在 broker (offset/队列) — proxy 重启客户端重连即可 ✅ | 通过 |
| V7 | 重试参数 | attempts=3 + backoff ×2 → 第 n 次延迟 2^(n-1) 级 — 有界重试 ✅ | 通过 |
| V8 | 认证默认关 | authConfig null → 管线仅 ContextInit — 老集群零改动 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **LOCAL 模式生命周期耦合**: 内嵌 broker 启动失败 → proxy 启动失败 (同进程同生命周期) — 与"CLUSTER 独立高可用"对照 | 大纲 §1 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (LOCAL 生命周期耦合), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (match 恒真兜底/双面认证同构/会话过期/内嵌客户端凭据/授权失败面/流控语义), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | remoting match 恒真? | RemotingProtocolHandler.match 返回 true (L47-49) — 无 HTTP/2 特征即 remoting 兜底; http2proxy 面处理 gRPC 探测 | 发现 10 (补锚) |
| T2 | 双面认证同构? | broker 侧 Authorization/AuthenticationPipeline (RM-5 已见) 与 proxy 同款 — **proxy 认证后 broker 不再重认证?** — broker 侧仍挂链 (双面都有, 但 proxy 面为主) | 通过 (验证) |
| T3 | 会话过期? | channelExpiredInSeconds=60 / contextExpiredInSeconds=30 — proxy 侧会话/上下文清理 | 发现 11 (补锚) |
| T4 | 内嵌客户端凭据? | createForClusterMode 解析 authConfig.getInnerClientAuthenticationCredentials() → SessionCredentials (L126) — **CLUSTER 远程转发带认证** | 发现 12 (补锚) |
| T5 | 授权失败面? | AuthorizationPipeline.execute catch → LOGGER.error + 拒 (L45-55) — 授权失败即拒, 无降级 | 通过 (验证) |
| T6 | 流控语义? | flowLimitStatus TOO_MANY_REQUESTS (L161) — 线程池满 → 客户端可重试 (attempts=3) — **流控反馈闭环** | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 兜底协议安全性 | match 恒真 → 任何非 HTTP/2 首包走 remoting 解码 — 解码失败即断 (RM-1 协议错误即断) ✅ | 通过 |
| V2 | 双面认证链 | proxy 认证 + broker 再认证 (双面同链) — 纵深防御 ✅ | 通过 |
| V3 | 会话清理闭环 | 60s/30s 过期 → 清理 → 客户端重连重建 — 无泄漏 ✅ | 通过 |
| V4 | CLUSTER 凭据链 | 客户端→proxy (签名) + proxy→broker (内嵌凭据) — 双段认证 ✅ | 通过 |
| V5 | 流控-重试闭环 | 流控拒 (429) → 客户端退避重试 → 负载回落 — 背压语义 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **match 恒真 = remoting 兜底协议** (非 HTTP/2 特征即走 remoting 解码) | 大纲 §3 补注 |
| 11 | **补充锚点** | **会话/上下文过期**: channelExpiredInSeconds=60 / contextExpiredInSeconds=30 | 大纲 §1 补注 |
| 12 | **补充锚点** | **CLUSTER 双段认证**: 客户端→proxy 签名 + proxy→broker 内嵌 SessionCredentials (L126) | 大纲 §4 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 拓扑 (双模式/三端口/生命周期耦合) — 可写 ✅
- §2 gRPC 层 (服务面/管线反转/线程池/流控) — 可写 ✅
- §3 双协议 (协商/恒真兜底/请求码分发/翻译) — 可写 ✅
- §4 统一消息面 (方法集/本地直写/远程转发/双段认证) — 可写 ✅
- §5 认证一句话 (HmacSHA1/授权/透传/迁移) — 可写 ✅
- §6 客户端面 (重试/退避/无状态) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (恒真兜底/会话过期/双段认证)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-14, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 追查六个存疑点 + 反写测试 + 推理验证)

> 动机: 对 outline 全部锚点重新 grep 并追查网络拓扑六个存疑点 (多协议协商挂载端口/HTTP/2 转发目标/gRPC transport/TLS 双面/PRI 判定风险/线程语义)。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 多协议协商在哪个端口? | MultiProtocolRemotingServer.configChannel (L80-88): HandshakeHandler → IdleState → **ProtocolNegotiationHandler 挂 remoting server**; RemotingProtocolServer (L108-121): defaultRemotingServer **listenPort=getRemotingListenPort()=8080** — **协商在 8080, 非独立端口** | 发现 13 (表述精确化) |
| T2 | HTTP/2 转发到哪? | Http2ProtocolProxyHandler:114: `b.connect(LOCAL_HOST, config.getGrpcServerPort())` — **8080 收到 HTTP/2 (PRI magic) → 本地转发到 8081 gRPC server**; 配置 enableRemotingLocalProxyGrpc (默认?) — "LocalProxyGrpc" = 单端口暴露方案 | 发现 14 (补锚) |
| T3 | gRPC transport? | GrpcServerBuilder:49-53: **NettyServerBuilder.forPort(8081)** + protocolNegotiator(ProxyAndTlsProtocolNegotiator) — io.grpc netty 独立 server, 8081 自己 accept | 通过 (验证) |
| T4 | TLS 双面? | 8080: MultiProtocolTlsHelper (MultiProtocolRemotingServer.loadSslContext) + HandshakeHandler; 8081: ProxyAndTlsProtocolNegotiator (grpc 侧 TLS) — **双端口各自 TLS** | 通过 (验证) |
| T5 | PRI 判定风险? | Http2ProtocolProxyHandler:49-50 代码注释自认: "may be has potential risks if there is a new protocol which start with 'PRI '" — **4 字节 magic 前缀碰撞风险自认** | 发现 15 (语义标注) |
| T6 | LocalMessageService 线程语义? | processRequest 同步调用 (L115) — **broker processor 逻辑在 proxy 调用线程执行** (proxy 线程池), 非独立 broker 线程 — 内嵌模式线程模型 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | PRI magic 判定 | 0x50524920 = "PRI " — HTTP/2 连接序言前 4 字节; remoting 帧首 4 字节 = 总长 (非 PRI) → 无碰撞 (当前协议集) ✅ | 通过 |
| V2 | 本地转发闭环 | 8080 HTTP/2 → LOCAL_HOST:8081 → gRPC 服务 → 统一消息面 — 单端口暴露成立 ✅ | 通过 |
| V3 | 双 TLS 面独立 | 8080 HandshakeHandler (remoting 风格) + 8081 negotiator (grpc 风格) — 各端口独立协商 ✅ | 通过 |
| V4 | gRPC 独立性 | io.grpc NettyServerBuilder 自带 boss/worker loop (grpcBossLoopNum=1/workerLoopNum=2N) ✅ | 通过 |
| V5 | 线程复用 | LocalMessageService 直调 → proxy 线程执行 broker 逻辑 — 线程池参数共享 (sendMessageExecutor 等) ✅ | 通过 |
| V6 | 管线执行序再验证 | ContextInit (header 解析) → Authn (签名) → Authz (资源) — 上下文先行语义成立 ✅ | 通过 |
| V7 | 认证透传闭环 | AUTHORIZATION_AK → 授权面/broker 使用 — 无重复认证开销 (透传复用) ✅ | 通过 |
| V8 | 流控闭环 | 线程池满 → 429 → 客户端退避重试 (attempts=3) — 背压闭环 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **表述精确化** | outline §3 "单端口多协议" 位置不明 — **协商在 8080 remoting server** (MultiProtocolRemotingServer, enableRemotingLocalProxyGrpc 配置); gRPC 8081 是独立 io.grpc server | 大纲 §3 修正 |
| 14 | **补充锚点** | **HTTP/2 本地代理转发**: 8080 PRI 探测 → Http2ProxyFrontendHandler **connect LOCAL_HOST:8081** — "LocalProxyGrpc" 单端口暴露方案 (HAProxyMessageForwarder 转发头) | 大纲 §3 补注 |
| 15 | **语义标注** | **PRI 4 字节 magic 前缀碰撞风险** (Http2ProtocolProxyHandler:49-50 代码自认) — 新协议若以 "PRI " 开头会误判 | 大纲 §3 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 拓扑 (双模式/三端口/端口职责) — 可写 ✅
- §2 gRPC 层 (服务面/管线/线程池) — 可写 ✅
- §3 双协议 (8080 协商/本地转发/PRI 风险/请求码分发) — 可写 ✅
- §4 统一消息面 (方法集/直写/转发/线程语义) — 可写 ✅
- §5 认证一句话 (HmacSHA1/授权/透传) — 可写 ✅
- §6 客户端面 (重试/退避/无状态) — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 8 项全过 (V1-V8); **新发现 3 处全部修复** (协商位置/本地转发/PRI 风险)。核心认知: 多协议协商在 **8080** (enableRemotingLocalProxyGrpc 时), HTTP/2 分支本地转发到 8081 gRPC server — 单端口暴露方案; gRPC 8081 是独立 io.grpc server。大纲经修复后反写测试全过。
