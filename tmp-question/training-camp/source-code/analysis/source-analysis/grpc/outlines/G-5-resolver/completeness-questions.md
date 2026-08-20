# G-5 全视角提问验证 (completeness-questions)

> 🟡 B 域: 28 问 / 5 身份。每问标注大纲覆盖 (✅ / ⚠️ / ❌)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 生命周期 | start 能被调用两次吗? | ✅ §1 checkState 一次性 |
| 2 | 开发者 | 生命周期 | refresh 与 start 区别? | ✅ §1 重解析 vs 首轮 |
| 3 | 开发者 | Uri | 非法 target 抛什么? | ✅ §2 URISyntaxException |
| 4 | 开发者 | Uri | 组件怎么存?编码会丢吗? | ✅ §2 percent-encoded 往返 |
| 5 | 开发者 | DNS | 缓存 TTL 怎么配? | ✅ §3 networkaddress.cache.ttl |
| 6 | 开发者 | DNS | 每个 IP 是独立 EAG 吗? | ✅ §3 每地址一个 |
| 7 | 开发者 | Retrying | 重试成功后退避会重置吗? | ✅ §4 reset |
| 8 | 开发者 | Provider | 自定义 scheme 怎么接入? | ✅ §4 SPI |
| 9 | 架构师 | 生命周期 | 为什么错误重试由 Listener 负责? | ✅ §1 职责单一 |
| 10 | 架构师 | Uri | 为什么不用 java.net.URI? | ✅ §2 宽松 authority |
| 11 | 架构师 | DNS | 30s 缓存 vs 每次查询的权衡? | ✅ §3 |
| 12 | 架构师 | DNS | TXT 配置下发的意义? | ✅ §3 配置与地址同节奏 |
| 13 | 架构师 | Retrying | 装饰器 vs 内置重试? | ✅ §4 |
| 14 | SRE | DNS | DNS 挂了会怎样? | ✅ §4 退避重试 |
| 15 | SRE | DNS | 地址变更多久生效? | ✅ §3 TTL 30s |
| 16 | SRE | Uri | 线上 "Missing required scheme" 排查? | ✅ §2 |
| 17 | SRE | 生命周期 | 解析器泄漏 (忘 shutdown)? | ⚠️ 未明说 (通道 shutdown 时 shutdown) |
| 18 | 学生 | 生命周期 | 解析器什么时候启动? | ✅ §1 start 即解析 |
| 19 | 学生 | Uri | target 字符串长什么样? | ✅ §4 dns:/// 格式 |
| 20 | 学生 | DNS | 为什么缓存? | ✅ §3 |
| 21 | 学生 | Retrying | 解析失败会无限重试吗? | ✅ §4 退避收敛 |
| 22 | 研究者 | 生命周期 | vs 注册中心推送模型 (Dubbo/Nacos)? | ✅ 对照声明 |
| 23 | 研究者 | Uri | 手写解析 vs Guava/第三方? | ✅ §2 |
| 24 | 研究者 | DNS | TXT 配置 vs 独立配置中心? | ✅ §3 A2 提案 |
| 25 | 研究者 | Retrying | 与 G-6 RetriableStream 退避的异同? | ✅ §4 同源 |
| 26 | 研究者 | Provider | UdsNameResolver 的适用场景? | ✅ 负面空间 #3 声明 |
| 27 | 研究者 | 生命周期 | onResult2 的 syncContext 约束意义? | ✅ §1 |
| 28 | 研究者 | DNS | JdkAddressResolver 与 NetworkAddressResolver 选择? | ⚠️ 未展开 (实现选择细节) |

⚠️ 2 项 (#17/#28) → 处理: #17 归 G-3 (通道 shutdown 链, 已提); #28 写作时展开 (JdkAddressResolver L595 是默认, NetworkAddressResolver 备选)。无需补大纲。
