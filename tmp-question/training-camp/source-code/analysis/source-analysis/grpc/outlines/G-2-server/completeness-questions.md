# G-2 全视角提问验证 (completeness-questions)

> 普通域: 30+ 问 / 5 身份。每问标注大纲覆盖 (✅ 有 / ⚠️ 部分 / ❌ 无)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 装配 | 自定义传输 (inprocess/binder) 怎么接入 ServerImpl? | ✅ §1 回调注入 |
| 2 | 开发者 | 注册表 | 同名 service 重复 addService 会怎样? | ✅ §2 原子覆盖 |
| 3 | 开发者 | 链路 | 回调真的不并发吗?SerializingExecutor 怎么保证? | ✅ §3+§8 排队 |
| 4 | 开发者 | 拦截器 | 拦截器里能改 call 对象吗? | ✅ §4 PartialForwarding |
| 5 | 开发者 | 状态机 | 忘记调 close 会怎样? | ⚠️ 未明说 (流泄漏→超时回收, G-6 关联) |
| 6 | 开发者 | 状态机 | setCompression 为什么必须在 sendHeaders 前? | ✅ §5 checkState |
| 7 | 开发者 | 执行器 | executorSupplier 每个请求都会调用吗? | ✅ §8 maySwitchExecutor |
| 8 | 开发者 | 协商 | 怎么判断当前连接是加密的? | ✅ §7 SecurityLevel 属性 |
| 9 | 架构师 | 装配 | 为什么 ServerImpl 是主体而传输是参数? | ✅ §1 依赖注入理由 |
| 10 | 架构师 | 注册表 | 不可变快照 vs 动态注册的取舍? | ✅ §2 无锁读 |
| 11 | 架构师 | 链路 | 为什么 setup 必须在 serializing executor 排队? | ✅ §3 顺序保证 |
| 12 | 架构师 | 拦截器 | 为什么请求时包装而非注册时包装? | ✅ §4 请求级状态 |
| 13 | 架构师 | 状态机 | 编程错误和协议错误为什么用不同失败方式? | ✅ §5 IllegalState vs INTERNAL |
| 14 | 架构师 | 连接 | 为什么双 GOAWAY 而不是一个? | ✅ §6 拒新+等旧 |
| 15 | 架构师 | 协商 | ALPN 失败会怎样? | ✅ §7 不兼容协议报错 |
| 16 | 架构师 | 停机 | 优雅下线的分布式意义? | ✅ §9 UNAVAILABLE→重试 |
| 17 | SRE | 连接 | 客户端 ping 洪泛怎么防? | ✅ §6 ENHANCE_YOUR_CALM |
| 18 | SRE | 停机 | 慢调用拖住下线怎么办? | ✅ §9 shutdownNow + 幂等 |
| 19 | SRE | 协商 | "Unable to find compatible protocol" 排查? | ✅ §7 ALPN 检查 |
| 20 | SRE | 注册表 | "Method not found" 从哪来? | ✅ §2 UNIMPLEMENTED |
| 21 | SRE | 状态机 | 线上 "TOO_MANY_RESPONSES" 说明什么? | ✅ §5 协议违规 |
| 22 | 学生 | 装配 | 三行代码 start() 背后建了几个对象? | ✅ §1 链 |
| 23 | 学生 | 注册表 | fullMethodName 长什么样? | ✅ §2 (G-1 已提) |
| 24 | 学生 | 链路 | 我的 serviceImpl 方法在哪个线程执行? | ✅ §3+§8 应用执行器 |
| 25 | 学生 | 拦截器 | 拦截器顺序和直觉相反? | ✅ §4 最后添加最外层 |
| 26 | 学生 | 状态机 | 服务端什么时候发响应头? | ✅ §5 sendHeaders 时机 |
| 27 | 学生 | 连接 | GOAWAY 是什么? | ✅ §6 告别信 |
| 28 | 学生 | 协商 | TLS 和 h2c 怎么共存? | ✅ §7 pipeline 替换 |
| 29 | 研究者 | 链路 | 传输线程 vs 应用线程的切换代价? | ✅ §3 双跳转 |
| 30 | 研究者 | 连接 | 双 GOAWAY vs Envoy/k8s 优雅下线对比? | ✅ §6 两阶段 |
| 31 | 研究者 | 执行器 | SerializingExecutor vs 锁/队列方案? | ✅ §8 串行化 |
| 32 | 研究者 | 停机 | shutdownNow 的中断传播路径? | ⚠️ 已讲 transports 遍历, 未提线程中断细节 |

⚠️ 2 项 (#5/#32) → 处理:
- #5 忘调 close → 桥链补: 流保持打开, 连接超时/客户端取消回收 (G-6/传输层) — 大纲 §5 补一句
- #32 shutdownNow 线程中断 → 传输层流取消 (G-3 对称), 不展开 — 声明归 G-3
