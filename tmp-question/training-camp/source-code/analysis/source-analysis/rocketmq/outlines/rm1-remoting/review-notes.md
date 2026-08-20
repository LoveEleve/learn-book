# RM-1 remoting 协议层 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **观察记录** | **suspended 仅 JSON 序列化路径**: 二进制头 (RocketMQSerializable L57-80) 只含 code/language/version/opaque/flag/remark/map — 无 suspended 字段; 用途 = 服务端内存标记 (PullRequestHoldService L55 / PopLongPollingService L298 设置) + writeResponse metrics (LABEL_IS_LONG_POLLING) — 不跨网络, 功能不受影响 | 大纲 §1 标注 |
| 2 | **补充锚点** | **二进制头字段序** (RocketMQSerializable L57-80): code short(~32767) → language byte → version short → opaque int → flag int → remark(int 长+UTF8) → map(int 长+FastCodesHeader.encode 免反射+extFields short 键/int 值); 字符串长度双精度 (short 键/int 值) | 大纲 §1 补注 |
| 3 | 验证 | 160 请求码 = public static final 精确计数; flag 仅 RPC_TYPE/RPC_ONEWAY 2 位 (L50-51); MIN_CLOSE_TIMEOUT_MILLIS=100 (L95); RemotingServerTest 三调用模式覆盖 (testInvokeSync/Oneway/Async); 帧头长 24 位 (16MB) 与 FRAME_MAX_LENGTH 16MB 上限闭环 | 记录 |
| 4 | 行号验证 | 全函数 25 锚点 + 跨文件 8 处 grep (RemotingCommand 245-473 / RocketMQSerializable 57-80 / NettyDecoder 34-48 / NettyRemotingAbstract 96-400 / NettyRemotingClient 540-632 / NettyRemotingServer 339-351 / RequestCode 160 常量 / TlsSystemConfig 24-30) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 帧格式 encode/decode 对称 (总长/头长/类型)
- 双序列化选择一致性 (serializeType 全局配置)
- opaque 匹配与清理 (responseTable 移除)
- 超时分级关闭逻辑

### 维度2 性能
- fastEncodeHeader 零拷贝 + FastCodesHeader 免反射
- 每处理器独立线程池 (隔离)
- 长连接复用
- IO 线程零阻塞 (RequestTask)

### 维度3 内存
- 16MB 帧上限 (防巨型帧)
- responseTable 定时清理
- channelTables 扫描

### 维度4 一致性
- 协议错误即断连
- 类型高字节编码 (零开销协商)
- 5.x 新码向后兼容 (枚举增量)
- 二进制/JSON 双路径差异 (suspended 观察)

### 维度5 负面空间 (已写入大纲 5 条)
- 不协议协商/不背压/不多路复用/不压缩/不追踪内建

## 结论
RM-1 全部锚点 ~40 处验证, 6 闭环完成, **观察 1 + 补锚 1**。怀疑审计 1 项修正 (markProtocolType 高 8 位类型, pass1 猜测反了, q1 已修正)。推断 3 处显式标注。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | 通过项 | 前置 Netty (阶段1) + RM-2 (未来域前置声明 — 拓扑 OK) ✅; 引出 RM-7/8/11/12/13 (未来 OK); 对照 R-28 ✅; 读者处境场景化 ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 大纲 ~40 锚点 (file:line) ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 RM-2 未执行 (拓扑 RM-1 先行) — 但 RM-1 仅"协议消费者"一句话, 非实质依赖, 可接受 ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 线程 (分发/IO 分离) / 网络 (Netty) / 协议 (双序列化) / 认证 (RPCHook) / TLS / 度量 (metrics) / 连接生命周期 (ChannelEventListener) — 七横切 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 负面空间 5 条 ✅; 开篇场景化 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **反写测试发现** | 大纲 §4 "Oneway 免注册" 表述 — 需精确: oneway 客户端发送后**不创建 ResponseFuture 也不等待**, 服务端**不响应** (writeResponse 里 isOnewayRPC 直接返回 L225-228); "免注册"只说了一半 | 大纲 §4 补注 |
| 11 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (帧布局/markProtocolType/双序列化/flag 2 位/suspended 独立字段/零拷贝/LengthField/16MB/断连/processorTable/default 兜底/rejectRequest/opaque 匹配/超时扣减/分级关闭/中断拉取/160 码/心跳/双实现/RPCHook/TLS/rpc 桥) | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处修复** (oneway 双端语义 #10), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

> 动机: 大纲每个锚点重新 grep, 对全部推断/计数反推验证。

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 头长位宽与帧上限闭环 | 低 24 位头长 = 16MB-1; FRAME_MAX_LENGTH 默认 16MB — 一致 ✅ | 通过 |
| V2 | 类型高字节编码 | type.getCode() << 24 — JSON=0 → 头长原样; ROCKETMQ=1 → 高字节 0x01; decode 反解 (>> 24) ✅ | 通过 |
| V3 | flag 语义 | bit0=响应标志, bit1=oneway; 无其他位 (仅 2 常量) — 5.x 未扩展 ✅ | 通过 |
| V4 | 超时扣减正确性 | left = timeout - (取通道耗时) — 端到端超时; 分级关闭: left>100ms 或 >timeout/4 才关 — 防误杀 ✅ | 通过 |
| V5 | oneway 全链 | 客户端不注册 Future + 服务端 writeResponse 直接返回 (L225-228) — 双向无响应闭环 ✅ | 通过 |
| V6 | 二进制头对称 | encode L57-80 ↔ decode (rocketMQProtocolDecode) — 字段序对称; 长度上限 readStr limit 检查 ✅ | 通过 |
| V7 | suspended 不跨网络 | 二进制头无字段 + JSON 路径 Jackson @JSONField 带 — 双路径差异; 功能面靠服务端内存持有 (PullRequestHoldService) ✅ | 通过 |
| V8 | 160 码分类 | public static final 精确 160; 含 5.x 码 (361 LITE_PULL / 1018 RAFT) — 增量兼容 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | 大纲 §5 未提 **rejectRequest 的实现方**: NettyRequestProcessor.rejectRequest() — 服务端可基于负载/连接数拒绝 (broker 侧实现, 限流面; RM-5 交叉) | 大纲 §5 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (rejectRequest 实现方锚点), 修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查八个存疑点 (proxy 内嵌 remoting/rpc 层用途/线程默认值/namesrv 切换/Encoder 实现/rejectRequest 接口), 并做反写测试。

## 追查过程 (八个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | proxy 内嵌 remoting 的角色? | proxy/remoting/protocol/remoting/RemotingProtocolHandler.java — **双协议翻译层**: 老客户端 (remoting) 经 proxy 转 grpc (RM-13 交叉) | 发现 2 (大纲 §5 "双实现"精确化) |
| T2 | rpc/ 层 (remoting 模块内) 用途? | **RpcClientImpl 包装 RemotingClient** (非 gRPC!); 消费方 BrokerOuterAPI (broker/src/.../BrokerOuterAPI.java:171 `new RpcClientImpl(this.clientMetadata, this.remotingClient)`) — remoting 高层抽象; gRPC 在 proxy 模块 grpc/ 目录 | **认知修正 (发现 1): q6/大纲 §6 原表述 "RpcClient gRPC 桥" 错误** |
| T3 | 线程默认值? | NettyServerConfig: serverWorkerThreads=8 / serverSelectorThreads=3 / serverChannelMaxIdleTimeSeconds=120 / listenPort=0 (需配置) | 发现 4 (大纲 §5 补) |
| T4 | namesrv 故障切换? | updateNameServerAddressList (L499-530): 列表变化 → **Collections.shuffle 随机化** + chosen 失效 → 关 channel+置空; getAndCreateNameserverChannelAsync (L633-660): **轮询切换** (namesrvIndex 递增 + tryLock LOCK_TIMEOUT 防并发) | 发现 3 (大纲 §4 补) |
| T5 | NettyEncoder 实现? | fastEncodeHeader + body 直写 (L37-41); 异常 → closeChannel | 通过 |
| T6 | rejectRequest 接口? | NettyRequestProcessor.java:29 `boolean rejectRequest();` — 接口方法非默认 | 通过 |
| T7 | ProxyProtocolTest 场景? | HAProxy 协议编解码 (netty HAProxyMessageDecoder) — broker 在 LB 后获取真实客户端地址场景 | 通过 (q6 方向正确) |
| T8 | namesrv 通道 null 路径? | getAndCreateChannelAsync 失败 → channelFuture null → 返回 null (L636-638) — 调用方 parseChannelRemoteAddr(null) 前有检查链 (invokeSync 的 connect 异常路径) | 通过 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 头长与帧上限闭环 | 低 24 位头长 16MB-1 = FRAME_MAX_LENGTH 默认 16MB ✅ | 通过 |
| V2 | namesrv 轮询公平性 | namesrvIndex 递增取模 — 多地址轮询; shuffle 后顺序随机 (防热点) ✅ | 通过 |
| V3 | update 时 chosen 失效处理 | 新列表不含 chosen → compareAndSet(null) + 关闭 channel 缓存 (L523-530) — 防陈旧连接 ✅ | 通过 |
| V4 | rpc/ 与 grpc 边界 | remoting 模块 rpc/ (RemotingClient 包装) vs proxy 模块 grpc/ (RpcServer) — 两套不同抽象 ✅ | 通过 |
| V5 | worker 线程与业务线程 | selector 3 (IO) + worker 8 (处理) + 每处理器 executor (业务) — 三层线程 ✅ | 通过 |
| V6 | maxIdle 120s 与心跳 | 服务端 120s 空闲断连; 客户端心跳周期 < 120s (MQClientInstance 30s) — 不误杀 ✅ | 通过 |
| V7 | rejectRequest 时机 | processRequestCommand L286: 处理器拒绝 → 直接错误响应 (不提交线程池) ✅ | 通过 |
| V8 | Encoder 异常即断连 | encode 异常 → closeChannel — 与 Decoder 对称 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 18 | **认知修正** | 大纲 §6 "rpc/ 层 (RpcClient gRPC 桥 — proxy 协议桥)" **错误**: RpcClientImpl 包装的是 RemotingClient (BrokerOuterAPI 消费), 是 remoting 高层抽象; gRPC 在 proxy 模块 grpc/ 目录 (RpcServer) | 大纲 §6 重写 (rpc/=高层抽象 + proxy RemotingProtocolHandler=双协议翻译层) |
| 19 | **补充锚点** | proxy 双协议翻译层 (RemotingProtocolHandler) — 老客户端 remoting 经 proxy 转 grpc; 大纲 §5 "双实现"仅说"proxy 内嵌"未及翻译语义 | 大纲 §5/§6 补注 |
| 20 | 补充锚点 | namesrv 轮询切换 (shuffle+index 递增+tryLock) + 线程默认值 (8/3/120s) | 大纲 §4/§5 补注 |

## 反写测试 (只读大纲能否写文章)

- §1 帧格式: 布局/类型高字节/双序列化/二进制头序/suspended 差异/零拷贝 — 可写 ✅
- §2 编解码: LengthField/16MB/断连 — 可写 ✅
- §3 分发: 双分派/线程池/rejectRequest/opaque — 可写 ✅
- §4 客户端: 三模式/超时扣减/分级关闭/namesrv 切换 (修复后) — 可写 ✅
- §5 服务端: 注册/160 码/默认值 (修复后)/双实现 — 可写 ✅
- §6 扩展: RPCHook/rpc 抽象 (修正后)/TLS/metrics/翻译层 (补充后) — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

八存疑点全实证 (T1-T8); 推理验证 8 项全过 (V1-V8); **新发现 3 处全部修复** — **#18 认知修正最有价值** (rpc/ 层定位: RemotingClient 高层抽象非 gRPC 桥; 双协议翻译在 proxy RemotingProtocolHandler)。大纲经修复后反写测试全过。
