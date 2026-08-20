# GW-5 全视角提问验证 (completeness-questions)

> 🔴 A 域: 30 问 / 5 身份。每问标注大纲覆盖 (✅ / ⚠️ / ❌)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 转发 | GATEWAY_REQUEST_URL 谁设置? | ✅ §1 RouteToRequestUrlFilter |
| 2 | 开发者 | 转发 | 响应头什么时候写? | ✅ §1 延迟提交 |
| 3 | 开发者 | HttpClient | 连接池怎么配? | ✅ §2 ConnectionProvider |
| 4 | 开发者 | body | 缓存 body 的过滤器怎么声明? | ✅ §3 EnableBodyCachingEvent |
| 5 | 开发者 | 头 | X-Forwarded 追加 vs 覆盖? | ✅ §4 开关 |
| 6 | 开发者 | 回写 | 流式响应怎么回写? | ✅ §6 writeAndFlushWith |
| 7 | 架构师 | 转发 | 为什么延迟提交? | ✅ §1 后置过滤器改头 |
| 8 | 架构师 | HttpClient | 集中装配的价值? | ✅ §2 连接池共享 |
| 9 | 架构师 | body | 按需缓存 vs 全缓存? | ✅ §3 内存 |
| 10 | 架构师 | 头 | TrustedProxies 防什么? | ✅ §4 IP 伪造 |
| 11 | 架构师 | 变体 | 为什么 scheme 驱动? | ✅ §5 模型不同 |
| 12 | 架构师 | 回写 | 读写分离两过滤器? | ✅ §6 |
| 13 | SRE | 转发 | 后端超时怎么配? | ✅ §2 超时属性 |
| 14 | SRE | 头 | X-Forwarded-For 伪造风险? | ✅ §4 可信代理 |
| 15 | SRE | 回写 | SSE 流式性能? | ✅ §6 逐块 |
| 16 | SRE | body | 缓存 body 的内存占用? | ✅ §3 按路由 |
| 17 | 学生 | 转发 | 网关怎么转发? | ✅ §1 |
| 18 | 学生 | 头 | X-Forwarded-For 是什么? | ✅ §4 |
| 19 | 学生 | 变体 | WebSocket 能转发吗? | ✅ §5 |
| 20 | 学生 | 回写 | 响应怎么回来? | ✅ §6 |
| 21 | 研究者 | 转发 | vs Envoy 转发模型? | ✅ 对照声明 |
| 22 | 研究者 | HttpClient | Reactor Netty vs Apache/OkHttp? | ⚠️ 未对比 |
| 23 | 研究者 | body | vs gRPC 消息缓冲 (G-1)? | ⚠️ 未对比 |
| 24 | 研究者 | 头 | RFC 7239 vs X-Forwarded? | ⚠️ 未对比 (ForwardedHeadersFilter 存在) |
| 25 | 研究者 | 变体 | WebClient vs Netty 转发差异? | ⚠️ 未展开 |
| 26 | 研究者 | 回写 | 零拷贝? | ⚠️ 未展开 |
| 27 | 研究者 | 头 | hop-by-hop 移除必要性? | ✅ §4 |
| 28 | 研究者 | 转发 | 响应缓存面 (v4) 关联? | ✅ 负面空间 #4 声明 |
| 29 | 研究者 | body | 缓存键生成 (keygenerator)? | ⚠️ 未展开 (v4 面) |
| 30 | 研究者 | 回写 | 连接泄漏防护? | ✅ §6 cleanup |

⚠️ 6 项 (#22/#23/#24/#25/#26/#29) → 对照/性能类写作展开; #24 已有 ForwardedHeadersFilter 家族可对照。
