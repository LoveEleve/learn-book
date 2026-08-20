# G-1 全视角提问验证 (completeness-questions)

> 普通域 (<10000 行): 30 问 / 5 身份。每问标注大纲覆盖 (✅ 有 / ⚠️ 部分 / ❌ 无)。

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 代码生成 | MethodDescriptor 为什么用 volatile + DCL, 直接静态初始化不行吗? | ✅ §1 DCL |
| 2 | 开发者 | 代码生成 | MethodHandlers 的 switch(methodId) 怎么保证与方法的对应关系不错位? | ✅ §1 稳定排序 |
| 3 | 开发者 | 三形态 | 链式 with* 每次 new 对象, 高并发下创建开销可接受吗? | ✅ §2 不可变+引用复用 |
| 4 | 开发者 | 客户端分派 | ThreadlessExecutor 里 waitAndDrain 是忙等还是挂起? | ⚠️ §3 有机制未展开 drain 细节 (G-3) |
| 5 | 开发者 | 适配器 | disableAutoRequestWithInitial 的语义与 request() 的关系? | ✅ §4 自定义流控 |
| 6 | 开发者 | 服务端分派 | 业务方法抛异常时 onClose 怎么处理? | ⚠️ §5 未提异常路径 (支撑面 Status) |
| 7 | 开发者 | 序列化 | ThreadLocal 缓冲复用为什么不直接用池? | ✅ §6 弱引用防泄漏 |
| 8 | 开发者 | 帧格式 | maxMessageSize 校验在哪一层做?解压前后各一次吗? | ✅ §7 双重校验 |
| 9 | 架构师 | 代码生成 | 为什么生成 switch 分派而不是每个方法独立 handler 类? | ✅ §1 switch 表 O(1) |
| 10 | 架构师 | 三形态 | CRTP 泛型自引用解决了什么?不用它会怎样? | ✅ §2 精确子类型 |
| 11 | 架构师 | 客户端分派 | V2 BlockingClientCall 相比 V1 Iterator 的本质改进? | ✅ §3 三问题修复 |
| 12 | 架构师 | 适配器 | 冻结窗口 (freeze) 为什么是必要的? | ✅ §4 配置一致性 |
| 13 | 架构师 | 服务端分派 | 为什么延迟到 onHalfClose 才 invoke? | ✅ §5 客户端语义完整性 |
| 14 | 架构师 | 序列化 | 为什么 setSizeLimit 设成 Integer.MAX_VALUE, 谁管尺寸? | ✅ §6 责任上移 |
| 15 | 架构师 | 帧格式 | 压缩标志为什么在每帧头而不是协商一次? | ✅ §7 逐帧可切换 |
| 16 | 架构师 | 工具面 | 头注入为什么不直接改 CallOptions 而要拦截器? | ✅ §8 不改 call 语义 |
| 17 | SRE | 客户端分派 | 阻塞调用被中断会发生什么?连接会泄漏吗? | ✅ §3 等 onClose 优雅取消 |
| 18 | SRE | 帧格式 | 收到超大消息/压缩炸弹线上怎么防护? | ✅ §7 双重校验 |
| 19 | SRE | 序列化 | "Invalid protobuf byte sequence" 错误从哪来? | ✅ §6 错误映射 |
| 20 | SRE | 服务端分派 | 客户端发两个请求 (unary) 服务端会怎样? | ✅ §5 TOO_MANY_REQUESTS |
| 21 | SRE | 工具面 | 线上怎么查 RPC 错误详情 (rich error)? | ✅ §8 grpc-status-details-bin |
| 22 | 学生 | 代码生成 | 生成的 TestServiceGrpc 为什么是 final 且私有构造? | ⚠️ 未明说 (静态工具类惯例) |
| 23 | 学生 | 三形态 | Blocking/Async/Future 三种 stub 分别什么时候用? | ⚠️ 大纲有区分无选择指南 |
| 24 | 学生 | 客户端分派 | "异步调用"为什么还有 blocking 版? | ✅ §3 三通道 |
| 25 | 学生 | 适配器 | onNext/onCompleted 和 onMessage/onClose 是一回事吗? | ✅ §4 双向桥 |
| 26 | 学生 | 服务端分派 | 我写的 serviceImpl 方法是什么时候被调用的? | ✅ §5 halfClose 时 |
| 27 | 学生 | 序列化 | marshaller 是干嘛的?为什么需要 defaultInstance? | ✅ §6 解析器来源 |
| 28 | 学生 | 帧格式 | 5 字节头里到底是什么? | ✅ §7 标志+长度 |
| 29 | 研究者 | 序列化 | 相比直接传 byte[], marshaller 设计好在哪? | ✅ §6 零拷贝+防护 |
| 30 | 研究者 | 客户端分派 | ThreadlessExecutor vs 传统阻塞队列方案优劣? | ✅ §3 零线程占用 |
| 31 | 研究者 | 帧格式 | gRPC 帧 vs HTTP/2 DATA 帧的关系? | ✅ §7 HTTP/2 标注 |
| 32 | 研究者 | 工具面 | Rich Error Model 与普通 Status 的取舍? | ✅ §8 结构化 details |

⚠️ 5 项 (4/6/22/23) → 大纲补强:
- #4 ThreadlessExecutor drain 细节 → 归 G-3 桥链已声明 (客户端域展开)
- #6 业务异常路径 → §5 补一句: 异常经 StreamObserver.onError → Status (桥链 G-2)
- #22 final+私有构造 → §1 补一句: 静态门面惯例
- #23 选择指南 → §2 补一句: 阻塞=同步编程/异步=回调/Future=异步+可取消
