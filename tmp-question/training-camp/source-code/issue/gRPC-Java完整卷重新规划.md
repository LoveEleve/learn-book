# gRPC-Java 完整卷重新规划

> 目标：不再把当前已经完成的 4 篇正文误判为“grpc-java 整体源码分析已完成”，而是严格按照《源码范围规划复盘方法论》重新审视 grpc-java 仓库，把它从“RPC 与治理主题中的第一轮主干卷”提升为一份更接近“完整卷”的规划。  
> 分析对象：`grpc-java v1.83.1`  
> 仓库路径：`/data/workspace/source-code/code/spring/grpc-java/`

---

## 一、先给结论：现在完成的，不是 grpc-java 整卷，而是第一轮主干卷

当前已经完成的 4 篇：

- `01-stub-channel-clientcall.md`
- `02-servercall-and-streaming-model.md`
- `03-interceptors-context-deadline.md`
- `04-nameresolver-loadbalancer-netty-transport.md`

它们解决的是：

- 客户端调用主线
- 服务端调用主线
- 横切面协议主线
- 发现、选址与 transport 桥接主线

也就是说，当前已经建立起了 grpc-java 在“RPC 与治理”主题中的**第一轮运行时主干基线**。

这一步是对的，而且很重要，因为它直接回答了：

- 一个本地方法调用怎样变成远程调用
- 一次远端调用怎样在服务端落地
- 横切面语义怎样挂进调用链
- 调用发出去之前怎样解析目标、选择实例并桥到 transport

但如果按《源码范围规划复盘方法论》的标准来判断，当前状态还不能叫：

- grpc-java 整卷规划完成

原因不是前 4 篇写得不对，而是它们解决的是：

- **主干层**

却还没有补出一卷完整源码书所需要的：

- 规范层
- 集成层
- 机制补深层
- 生产层

所以最准确的判断应该是：

**当前 grpc-java 已完成的是“主题卷中的第一轮主干篇”，不是“grpc-java 整体完整卷”。**

---

## 二、为什么之前会产生“4 篇是不是就完了”的错觉

这正是方法论里第 10 条明确点名的问题：

- 只做主干闭环，没有做完整卷补层

grpc-java 当前这 4 篇之所以会给人一种“看起来已经挺完整”的错觉，是因为它们确实已经把最重要的运行时骨架讲清了：

- Stub / Channel / ClientCall
- ServerCall / ServerCalls / StreamObserver
- Interceptor / Context / Deadline
- NameResolver / LoadBalancer / Netty transport bridge

但这套骨架的完整，只能说明：

- 运行时主线已经立住

不能说明：

- 这就是 grpc-java 整卷应有的全部内容

按方法论文档的说法，如果一卷源码书最后缺：

- 规范层
- 集成层
- 机制补深层
- 生产层

那结果就会是：

- 主线很清楚
- 但整卷明显不完整

grpc-java 现在正处在这个状态。

---

## 三、重新按方法论审计 grpc-java：当前主干覆盖了什么

### 1. 当前已覆盖的主干知识域

#### G-MAIN-1 客户端调用主线
回答：
- Stub 是什么
- `stub.method(request)` 怎么变成远程调用
- `ClientCalls` / `ManagedChannel` / `ClientCallImpl` 各自处在什么位置

对应已完成：
- `01-stub-channel-clientcall.md`

#### G-MAIN-2 服务端调用主线
回答：
- transport stream 怎样进入服务端运行时
- `ServerImpl`、`ServerCallImpl`、`ServerCalls` 如何接力
- 为什么四种调用模式不是 unary 的简单放大

对应已完成：
- `02-servercall-and-streaming-model.md`

#### G-MAIN-3 横切面协议
回答：
- 客户端 / 服务端拦截器怎样挂进调用边界
- `Context` 怎样传播值、作用域与取消
- `Deadline` 怎样成为调用链的截止约束

对应已完成：
- `03-interceptors-context-deadline.md`

#### G-MAIN-4 发现、选址与 transport 桥接
回答：
- 逻辑 target 怎样持续解析成地址与配置
- `LoadBalancer` 怎样通过 subchannel/picker 做选址状态机
- `ManagedChannelImpl` 怎样把结果压到 delayed transport、`ClientTransport` 与 Netty handler

对应已完成：
- `04-nameresolver-loadbalancer-netty-transport.md`

### 2. 当前主干覆盖的价值

这 4 个域组合起来，已经足以支撑 grpc-java 在“RPC 与治理”主题中的基准角色：

- 它比 Feign 更底层
- 比 Spring Cloud 集成层更少装配噪音
- 比 Dubbo 的整套服务导出 / 注册 / 集群更集中
- 最适合作为“RPC 运行时最低心智图”的地基

所以它们不是多余的，也不是误写的；问题只在于：

- **它们还只是主干**

---

## 四、重新按方法论审计 grpc-java：当前明显缺失的层

下面开始进入真正的重规划部分。

### A. 规范层：明显缺失，且不能继续省略

方法论要求必须回答：

- 这套实现是在兑现哪些外部契约
- 哪些行为是规范要求
- 哪些行为是框架自己的实现取舍

对 grpc-java 来说，当前 4 篇虽然已经零散讲到：

- unary / streaming 语义
- metadata / headers
- deadline / cancel
- HTTP/2 transport bridge

但这些都还是“实现内的解释”，还不是“规范层的集中收束”。

完整卷至少应补出以下规范层主题：

#### G-SPEC-1 gRPC 方法类型契约总篇
回答：
- Unary / ServerStreaming / ClientStreaming / BidiStreaming 的正式语义差异
- 请求条数、响应条数、half-close、completion 的契约边界
- 哪些错误是违反调用契约而非普通业务异常

为什么要补：
- 当前四种模式差异已经讲了，但还是散在服务端主线里
- 缺一篇统一规范层，读者仍不容易建立“这是协议要求还是实现习惯”的区分

#### G-SPEC-2 Metadata / Status / Trailers 语义专题
回答：
- headers、metadata、status、trailers 各自在哪一层承担什么语义
- 哪些是 transport 头，哪些是 RPC 结果语义
- 为什么取消 / deadline 最终要回到统一 status 语义

为什么要补：
- 这是 grpc-java 最基础的“协议对象层”
- 不补，后续排障、拦截器、网关适配都会悬空

#### G-SPEC-3 Deadline / Cancel / Completion 契约边界
回答：
- 调用何时算“正常完成”
- 何时算取消
- deadline exceeded 和业务错误、transport error 的区别

为什么要补：
- 现在只是在实现主线里点到
- 还没有形成独立的“契约边界篇” 

### B. 集成层：当前几乎没做，这是最大缺口之一

方法论文档专门强调：

- 嵌入式/集成型框架必须检查上层装配桥

grpc-java 明显属于这类框架。因为真实使用者接触到的，往往不是：

- 裸 `ClientCallImpl`
- 裸 `ServerCallImpl`

而是：

- `.proto` + codegen 生成出来的 `*Grpc`
- `ManagedChannelBuilder` / `ServerBuilder`
- `InProcessChannelBuilder` / `InProcessServerBuilder`
- 测试规则与样板

完整卷至少应补出以下集成层主题：

#### G-INT-1 protoc 代码生成与 `*Grpc` 骨架
涉及模块：
- `compiler/`
- 生成产物中的 `*Grpc.java`

回答：
- `.proto` 服务定义怎样变成 `*Grpc`
- `Stub`、`ImplBase`、`bindService()` 怎样被生成出来
- 为什么运行时主线必须和 codegen 生成骨架一起看，才算真正闭环

为什么要补：
- 这是 grpc-java 最核心的“用户 API -> 运行时主线”装配桥
- 不补这一层，读者虽然知道主干怎么跑，但不知道入口是怎么被真正造出来的

#### G-INT-2 `ManagedChannelBuilder` / `ServerBuilder` 装配层
回答：
- 用户侧配置怎样映射进 resolver、lb、transport、deadline、executor 等内部结构
- 哪些 builder 配置决定了前面四篇的运行时边界

为什么要补：
- 这是 grpc-java 真实落地的配置入口
- 不补，当前正文依然偏“内部实现视角”

#### G-INT-3 InProcess / testing 装配桥
涉及模块：
- `inprocess/`
- `testing/`
- `examples/src/test`

回答：
- 为什么 grpc-java 官方明确不鼓励 mock stub
- `InProcessTransport` 为什么能更真实地复用运行时语义
- `GrpcCleanupRule` 一类测试基础设施怎样接入

为什么要补：
- `examples/README.md:134` 已经明确把这件事当成使用/测试哲学的一部分
- 这不是纯测试杂项，而是 grpc-java 的集成层与开发者体验层

### C. 机制补深层：当前还缺很多高价值专题

这是按方法论看，当前主干卷之外最应该继续补的部分。

#### G-DEEP-1 Service Config / Retry / Hedging
涉及模块：
- `core/`

回答：
- service config 怎样进入 channel 运行时
- retry / hedging 怎样改变 delayed transport 与 stream 策略
- 什么时候它是配置，什么时候它已经变成 transport 行为

为什么要补：
- 当前第四篇只点到了 service config 和 retry/hedging
- 但这条线信息密度非常高，完全值得独立成卷内重点篇

#### G-DEEP-2 `CallCredentials` 与认证边界
涉及模块：
- `api/`
- `auth/`
- `alts/`
- `authz/`

回答：
- 为什么 `ClientInterceptor` 不是最理想的认证边界
- `CallCredentials` 在调用链中挂在哪
- TLS / ALTS / authz 分别解决什么问题

为什么要补：
- README 和 API 已经明确区分 interceptor 与 credentials 职责
- 这是 grpc-java 的关键安全边界，不能完全空着

#### G-DEEP-3 Marshaller / ProtoUtils / 消息编解码桥
涉及模块：
- `protobuf/`
- `protobuf-lite/`
- `api/`

回答：
- 请求对象怎样被 marshaller 压成消息体
- `ProtoUtils` 在整个调用链中的桥接作用
- lite / full protobuf 的差异

为什么要补：
- 当前主干里反复提到 message/stream，但没有把“对象 -> 消息”这层独立打透

#### G-DEEP-4 Compression / Codec / Message Framing
回答：
- message encoding / accept encoding 怎样协商
- 压缩开启后到底影响哪一层
- 失败路径怎样回到 cancel / status

为什么要补：
- 当前前几篇只点到 compressor / metadata
- 还没形成独立机制域

#### G-DEEP-5 Health / Reflection / Channelz
涉及模块：
- `services/`

回答：
- 这些为什么不是“辅助服务杂项”，而是 grpc-java 的诊断与生产可见性层
- 它们如何与 server、metadata、status、transport 产生关系

为什么要补：
- 方法论明确要求不能低估运行时诊断能力
- 这一块正是 grpc-java 生产可见性的核心材料

#### G-DEEP-6 xDS
涉及模块：
- `xds/`

回答：
- xDS 怎样重写 NameResolver / LB / routing / server wrapper 的角色分工
- 它是“发现策略扩展”，还是“上层控制面总集成”

为什么要补：
- 从仓库体量、机制桥接作用、上层复用度看，xDS 绝对不应该被一句“按需扩展”轻轻跳过
- 它很可能是 grpc-java 完整卷里的高阶终章之一

### D. 生产层：现在几乎空白，但按方法论必须补

grpc-java 不是玩具库，它是高频生产基础设施。

所以完整卷至少要有一组生产层主题：

#### G-PROD-1 调用超时 / 取消 / 重试排障
回答：
- deadline、cancel、retry、hedging 在生产里最容易出现什么错觉
- 错误怎样从 `Context`、status、stream、listener 之间扩散

#### G-PROD-2 Channel / Subchannel / Picker / Transport 状态诊断
回答：
- READY / CONNECTING / IDLE / TRANSIENT_FAILURE 怎么看
- 一次调用为什么卡在 delayed transport
- picker 为什么一直给 `withNoResult()` 或 error

#### G-PROD-3 keepalive / 流控 / 连接耗尽问题
回答：
- keepalive 的生产副作用
- stream id exhaustion / backoff / connection churn 怎么定位

#### G-PROD-4 Channelz / Health / Reflection 的生产使用
回答：
- 这些诊断能力怎样真正帮你定位线上问题

### E. 平台 / 变体层：不一定先写，但必须记录

按方法论要求，不能只写“按需”，必须写清楚：

- 为什么暂缓
- 什么条件下重新纳入

grpc-java 当前应明确标成“暂缓但已建档”的模块至少包括：

- `okhttp/`
- `cronet/`
- `servlet/`
- `android/`
- `binder/`
- `gae-interop-testing/`
- `gcp-observability/`
- `opentelemetry/`

它们现在不必先写，但必须明确：
- 当前主线为什么不先吃
- 后续若补“平台实现卷”或“集成层卷”，它们怎样纳入

---

## 五、重新规划后，grpc-java 更合理的整卷结构

下面给出一版新的整卷结构建议。

### 第一组：主干运行时卷（已完成第一轮）
1. Stub、Channel 与 ClientCall 调用主线
2. ServerCall、ServerCalls 与流式调用模型
3. 拦截器、上下文传播与 Deadline
4. NameResolver、LoadBalancer 与 Netty Transport

### 第二组：协议语义卷
5. grpc-java：四种调用模式与方法契约总图
6. grpc-java：Metadata、Status 与 Trailers 语义
7. grpc-java：取消、half-close 与完成边界

### 第三组：生成代码与装配卷
8. grpc-java：protoc 代码生成与 `*Grpc` 骨架
9. grpc-java：Stub、`ImplBase` 与 `bindService()` 装配桥
10. grpc-java：`ManagedChannelBuilder`、`ServerBuilder` 与运行时配置注入
11. grpc-java：InProcess Transport、Testing 与真实测试语义

### 第四组：机制补深卷
12. grpc-java：Service Config、Retry 与 Hedging
13. grpc-java：`CallCredentials`、认证与调用凭证边界
14. grpc-java：Marshaller、ProtoUtils 与消息对象桥
15. grpc-java：Compression、Codec 与消息压缩路径
16. grpc-java：Health、Reflection 与 Channelz
17. grpc-java：xDS 如何重写发现、路由与服务端包装

### 第五组：生产与诊断卷
18. grpc-java：Deadline、Cancel、Retry 的线上排障
19. grpc-java：Channel、Subchannel、Picker 与 Transport 状态诊断
20. grpc-java：Keepalive、流控与连接问题分析
21. grpc-java：生产诊断能力的组合使用（Channelz / Health / Reflection）

### 第六组：平台与生态变体卷（按需）
22. okhttp transport
23. cronet transport
24. servlet 接入
25. android / binder 差异
26. opentelemetry / gcp-observability 适配

---

## 六、当前最合理的下一步

如果继续严格按方法论推进，当前最合理的动作**不是马上补第 5 篇正文**，而是：

1. 先承认当前 4 篇只是**主干层**
2. 把 grpc-java 完整卷规划显式建立出来
3. 在完整卷规划里排出下一批正文优先级

按优先级，我建议这样进入第二轮正文：

### 优先级 A：必须尽快补
1. `grpc-java：protoc 代码生成与 *Grpc 骨架`
2. `grpc-java：Service Config、Retry 与 Hedging`
3. `grpc-java：Metadata、Status 与 Trailers 语义`

### 优先级 B：主干已立后最值得补深
4. `grpc-java：InProcess Transport、Testing 与真实测试语义`
5. `grpc-java：CallCredentials、认证与调用凭证边界`
6. `grpc-java：Health、Reflection 与 Channelz`

### 优先级 C：完整卷高阶层
7. `grpc-java：xDS 如何重写发现、路由与服务端包装`
8. `grpc-java：生产诊断与故障排查`

---

## 七、最终结论

重新按《源码范围规划复盘方法论》审视之后，可以明确得出三个结论：

1. 当前已经完成的 4 篇正文是对的，但它们只构成 **grpc-java 主干运行时卷**。
2. 如果把这 4 篇误当成“grpc-java 整卷完成”，就会重蹈方法论文档里批评的“主干闭环有了，但完整卷缺层严重”的问题。
3. 因此，grpc-java 当前最合理的后续动作，不是立刻切离它去别的仓库，而是先承认它还缺：
   - 规范层
   - 集成层
   - 机制补深层
   - 生产层

**所以，后续如果继续深挖 grpc-java，应以这份《gRPC-Java 完整卷重新规划》为新的上位结构，而不再把“RPC 与治理主题里的 4 篇主干”当成整卷终点。**