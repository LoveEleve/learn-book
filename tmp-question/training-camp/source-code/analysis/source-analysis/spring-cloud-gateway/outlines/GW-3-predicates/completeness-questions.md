# GW-3 全视角提问验证 (completeness-questions)

> 🔴 A 域: 28 问 / 5 身份。每问标注大纲覆盖 (✅ / ⚠️ / ❌)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | SPI | 怎么自定义谓词? | ✅ §1 接口面 |
| 2 | 开发者 | SPI | applyAsync 默认怎么工作? | ✅ §1 同步包异步 |
| 3 | 开发者 | Path | 尾斜杠怎么处理? | ✅ §2 matchTrailingSlash |
| 4 | 开发者 | Path | 多 pattern 是 AND 还是 OR? | ✅ §2 任一 |
| 5 | 开发者 | Header | regexp 空会怎样? | ✅ §3 查存在性 |
| 6 | 开发者 | Weight | 权重谓词内部随机吗? | ✅ §3 预计算 |
| 7 | 开发者 | 组合 | 多个谓词怎么组合? | ✅ §3 AND |
| 8 | 架构师 | SPI | 为什么需要异步通道? | ✅ §1 ReadBody |
| 9 | 架构师 | Path | PathPattern vs 正则? | ✅ §2 |
| 10 | 架构师 | Weight | 预计算 vs 谓词内随机的差异? | ✅ §3 组内互斥 |
| 11 | 架构师 | 组合 | AND 组合 vs OR? | ✅ GW-1 已讲 |
| 12 | SRE | Path | context-path 下谓词失效吗? | ✅ §2 basePath |
| 13 | SRE | Weight | 权重怎么算? | ✅ §3 WebFilter |
| 14 | SRE | RemoteAddr | CIDR 支持吗? | ✅ §3 |
| 15 | SRE | Path | 路径解析性能? | ✅ §2 缓存 |
| 16 | 学生 | SPI | 谓词工厂是什么? | ✅ §1 |
| 17 | 学生 | Path | `**` 是什么? | ✅ §2 PathPattern |
| 18 | 学生 | Weight | 权重路由干嘛用? | ✅ §3 灰度 |
| 19 | 学生 | 时间 | After 谓词干嘛用? | ✅ §3 时间窗 |
| 20 | 研究者 | SPI | vs K8s Ingress 路径匹配? | ✅ 对照声明 |
| 21 | 研究者 | Path | PathPattern vs AntPathMatcher? | ⚠️ 未对比 (写作展开) |
| 22 | 研究者 | Weight | vs gRPC 权重路由 (G-7 WRR)? | ✅ 跨域隐含 (对照声明可加) |
| 23 | 研究者 | 组合 | AsyncPredicate AND 的性能? | ⚠️ 未评估 |
| 24 | 研究者 | SPI | 短路语法 vs 完整 Map 绑定? | ✅ v3 配置体系 |
| 25 | 研究者 | RemoteAddr | 可信代理链 (TrustedProxies)? | ✅ GW-5 关联声明 |
| 26 | 研究者 | ReadBody | 请求体谓词的缓存副作用? | ⚠️ 未明说 (body 缓存 GW-5) |
| 27 | 研究者 | Weight | WeightDefinedEvent 联动? | ⚠️ 未明说 (event/ 面) |
| 28 | 研究者 | 时间 | ZonedDateTime 时区处理? | ⚠️ 未明说 |

⚠️ 5 项 (#21/#23/#26/#27/#28) → 处理: #26/#27 补大纲一句 (ReadBody→body 缓存 GW-5 关联; WeightDefinedEvent); #21/#23/#28 写作时展开。
