# Ch7 Netty Pipeline — 知识规划

> 来源: 22 源文件 | ~6400 行 | transport/src/main/java/io/netty/channel/
> 基线: Ch6 Promise/Future 提供了异步结果 — Ch7 回答 "Handler 如何串成处理链"

---

## 01 提取 — 逐源映射

### 01.1 Pipeline 核心 (2 文件 — 31 KPs)

#### ChannelPipeline.java + DefaultChannelPipeline.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultChannelPipeline.java:63-64,91-101 | **HeadContext+TailContext 哨兵**: head.next=tail, tail.prev=head — 空链的闭环 | High |
| DefaultChannelPipeline.java:154-205 | **internalAdd() 统一入口 + AddStrategy 枚举**: FIRST/LAST/BEFORE/AFTER | High |
| DefaultChannelPipeline.java:544-553 | **checkMultiplicity: 非@Sharable 重复添加抛异常** | High |
| DefaultChannelPipeline.java:336-362 | **filterName→generateName: FastThreadLocal WeakHashMap 缓存类名→自动编号** | High |
| ChannelPipeline.java:89-99 | **入站(底→顶) vs 出站(顶→底) 传播方向** | High |
| DefaultChannelPipeline.java:800-856 | **destroy() 两阶段: destroyUp(头→尾)→destroyDown(尾→头)** | High |
| DefaultChannelPipeline.java:593-601 | **invokeHandlerAddedIfNeeded: 首次注册时批量执行 pending 回调** | High |

### 01.2 HandlerContext (2 文件 — 23 KPs)

#### AbstractChannelHandlerContext.java + ChannelHandlerContext.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractChannelHandlerContext.java:63-64 | **volatile next/prev 双向链表指针** | High |
| AbstractChannelHandlerContext.java:69-85 | **handlerState 4 态 FSM: INIT→ADD_PENDING→ADD_COMPLETE→REMOVE_COMPLETE** | High |
| AbstractChannelHandlerContext.java:90 | **executionMask 预计算位掩码 — 编码 Handler 实现的方法** | High |
| AbstractChannelHandlerContext.java:927-943 | **findContextInbound(next)/findContextOutbound(prev)** | High |
| AbstractChannelHandlerContext.java:945-953 | **skipContext: 掩码不匹配 && executor相同 → 跳过** | High |
| AbstractChannelHandlerContext.java:148-174 | **JDK-8180450 workaround: 三级 dispatch(HeadContext/Adapter/interface)** | High |
| AbstractChannelHandlerContext.java:780-841 | **write(msg,flush,promise): findContextOutbound→跨 executor 用 WriteTask Recycler** | High |
| AbstractChannelHandlerContext.java:1073-1153 | **WriteTask Recycler + Integer.MIN_VALUE 编码 flush 标志** | High |

### 01.3 Handler 体系 (7 文件 — 47 KPs)

#### ChannelHandler → Inbound/Outbound/Duplex + Adapters + Simple

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ChannelHandler.java:@Sharable | **@Sharable 注解: 标记 Handler 可被多个 Channel 共享** | High |
| ChannelHandlerAdapter.java:isSharable | **ThreadLocal+WeakHashMap 缓存 @Sharable 检查结果(#2289)** | High |
| ChannelInboundHandler.java:9 回调 | **入站生命周期: reg→active→read→readComplete→inactive→unreg** | High |
| ChannelOutboundHandler.java:write vs flush | **write 不 flush(攒批设计), 8 个出站操作** | High |
| ChannelDuplexHandler.java | **extends InboundAdapter + implements Outbound = 双向** | High |
| SimpleChannelInboundHandler.java | **TypeParameterMatcher 类型匹配 + autoRelease + channelRead0 模板** | High |
| CombinedChannelDuplexHandler.java | **DelegatingChannelHandlerContext: inboundCtx/outboundCtx 分离代理** | High |

### 01.4 Mask + Invoker (3 文件 — 29 KPs)

#### ChannelHandlerMask + ChannelInboundInvoker + ChannelOutboundInvoker

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ChannelHandlerMask.java:17 位掩码 | **17 位对应 17 个回调方法, mask0 先加(全量)再减(@Skip 削去)** | High |
| ChannelHandlerMask.java:FastThreadLocal | **FastThreadLocal 缓存掩码, 避 volatile 读取** | High |
| ChannelInboundInvoker.java:fire* | **9 个 fire* 方法 — 入站事件传播的驱动力** | High |
| ChannelOutboundInvoker.java:write/flush/connect | **9 大类出站操作 + voidPromise 零分配** | High |

### 01.5 写缓冲区与初始化 (5 文件 — 43 KPs)

#### ChannelOutboundBuffer + ChannelInitializer + FlushPromiseNotifier + Config

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ChannelOutboundBuffer.java:76-85 | **三指针链表: flushedEntry→unflushedEntry→tailEntry** | High |
| ChannelOutboundBuffer.java:826-857 | **Entry Recycler 对象池复用, pendingSize 含 96B 对象头** | High |
| ChannelOutboundBuffer.java:180-208 | **高低水位线: totalPending>high→setUnwritable; <low→setWritable** | High |
| ChannelOutboundBuffer.java:414-496 | **nioBuffers() 零拷贝聚集写: FastThreadLocal 数组复用** | High |
| ChannelInitializer.java:125-141 | **initChannel→finally pipeline.remove(this) 自移除模式** | High |
| ChannelFlushPromiseNotifier.java:29-53 | **writeCounter + ArrayDeque FlushCheckpoint 排队通知** | High |

---

## 01 聚合

N=22, P1=≥5文件, P2=3-4文件, P3=1-2文件

### P1 — 全系统共识

| Knowledge Point | 出现文件 |
|----------------|---------|
| **双向链表(next/prev) + HeadContext/TailContext** | DefaultChannelPipeline + AbstractChannelHandlerContext + 全context |
| **入站(底→顶) vs 出站(顶→底) 传播方向** | ChannelPipeline + AbstractChannelHandlerContext + ChannelInboundInvoker + ChannelOutboundInvoker |
| **fire* 事件传播** | AbstractChannelHandlerContext + ChannelInboundInvoker + DefaultChannelPipeline |
| **handlerState 4 态 FSM** | AbstractChannelHandlerContext |
| **executionMask 位掩码** | AbstractChannelHandlerContext + ChannelHandlerMask + DefaultChannelHandlerContext |
| **write/flush 出站链** | AbstractChannelHandlerContext + ChannelOutboundInvoker + ChannelOutboundBuffer |

### P2 — 局部重要

| Knowledge Point | 出现文件 |
|----------------|---------|
| **@Sharable + checkMultiplicity** | ChannelHandler + ChannelHandlerAdapter + DefaultChannelPipeline |
| **ChannelInitializer 自移除** | ChannelInitializer |
| **ChannelOutboundBuffer 水位线** | ChannelOutboundBuffer |
| **CombinedChannelDuplexHandler 代理分离** | CombinedChannelDuplexHandler |
| **WriteTask Recycler** | AbstractChannelHandlerContext |

### P3 — 独立

| Knowledge Point | 出现文件 |
|----------------|---------|
| **JDK-8180450 三级 dispatch** | AbstractChannelHandlerContext |
| **ChannelFlushPromiseNotifier checkpoint** | ChannelFlushPromiseNotifier |
| **DefaultChannelConfig 13 option** | DefaultChannelConfig |

---

## 02 深度分类

### 🔴 Deep

| KP | 为什么🔴 |
|----|---------|
| **HeadContext+TailContext 哨兵** | 双向链表的起点和终点 — 所有入站从 head.fire* 发起, 所有出站到 head.Unsafe 执行 |
| **handlerState 4 态 FSM** | Handler 生命周期保证 — INIT→ADD_PENDING→ADD_COMPLETE→REMOVE_COMPLETE, AtomicIntegerFieldUpdater |
| **executionMask + ChannelHandlerMask** | 17 位掩码预计算, skipContext 方法级零开销跳过 — Netty 最精巧的性能优化之一 |
| **入站(底→顶) vs 出站(顶→底)** | 责任链方向定义 — InboundHandler 沿 next 传播, OutboundHandler 沿 prev 传播 |

### 🟡 Working

| KP | 说明 |
|----|------|
| **ChannelInitializer 自移除** | initChannel→finally pipeline.remove(this) — 一次性初始化 |
| **ChannelOutboundBuffer 三指针+水位线** | flush→unflushed→tail 链表 + 高低水位流控 |
| **CombinedChannelDuplexHandler 代理分离** | inboundCtx/outboundCtx 伪装为一个 Handler |
| **SimpleChannelInboundHandler autoRelease** | TypeParameterMatcher + try-finally release |
| **JDK-8180450 三级 dispatch** | HeadContext/Adapter/interface 分派 workaround |

### 🟢 Surface

| KP | 放在哪 |
|----|-------|
| **DefaultChannelConfig 13 option** | 和 Pipeline 配置一起 |
| **ChannelFlushPromiseNotifier checkpoint** | 和 ChannelOutboundBuffer 一起 |
| **WriteTask Recycler** | 和出站传播一起 |

---

## 03 聚类

### Cluster A: 双向链表与事件传播 (12 KPs) — 零前置, 回答 "Pipeline 怎么串"

1. HeadContext+TailContext 哨兵节点
2. internalAdd + AddStrategy 四种插入
3. channel 注册时 invokeHandlerAddedIfNeeded
4. 入站(底→顶/fire*) vs 出站(顶→底/write-flush) 传播方向
5. findContextInbound(next方向)/findContextOutbound(prev方向)
6. skipContext(掩码不匹配+executor相同→跳过)
7. executionMask 预计算
8. handlerState 4 态 FSM
9. invokeHandler 三级 dispatch(JDK-8180450)
10. destroy 两阶段(update/downde)
11. checkMultiplicity + filterName→generateName 自动命名
12. PendingHandlerCallback 延迟回调队列

### Cluster B: Handler 类型体系 (10 KPs) — 依赖 A, 回答 "Handler 怎么分类"

1. @Sharable + ThreadLocal 缓存
2. ChannelInboundHandler: 9 个回调, lifecycle(reg→active→read→inactive)
3. ChannelOutboundHandler: 8 个出站, write 不 flush
4. ChannelDuplexHandler: Inbound+Outbound 双向
5. Adapter @Skip 默认转发
6. SimpleChannelInboundHandler: TypeParameterMatcher+autoRelease
7. CombinedChannelDuplexHandler: DelegatingContext 分离代理
8. ChannelHandlerMask: 17位+@Skip削去+FastThreadLocal缓存
9. ChannelInboundInvoker.fire* 传播驱动力
10. ChannelOutboundInvoker: write/flush/connect/bind + voidPromise

### Cluster C: 出站与写缓冲区 (8 KPs) — 依赖 A, 回答 "数据怎么写出"

1. ChannelOutboundBuffer 三指针链表(flushed→unflushed→tail)
2. Entry Recycler 对象池复用
3. addMessage→addFlush→remove/removeBytes 写路径
4. 高低水位线: totalPending>high→setUnwritable; <low→setWritable
5. nioBuffers() 零拷贝聚集写 + FastThreadLocal 数组
6. progress() ProgressivePromise 进度追踪
7. ChannelFlushPromiseNotifier: writeCounter+checkpoint 排队通知
8. ChannelOutboundBuffer.remove(Throwable) 失败路径

### Cluster D: 初始化与生命周期 (6 KPs) — 依赖 A, 回答 "Handler 怎么装配"

1. ChannelInitializer 自移除模式(pipeline.remove(this))
2. initMap ConcurrentHashMap 重入守卫
3. handlerAdded→initChannel→removeState 回调顺序
4. replace0: oldCtx.prev=oldCtx.next=newCtx(缓冲数据 forward)
5. PendingHandlerCallback 单链表 + callHandlerCallbackLater 尾部追加
6. callHandlerAdded0 异常保护: 抛异常→atomicRemove+callHandlerRemoved+fireExceptionCaught

### 教学顺序: A → B → C → D
A(链表+传播) → B(Handler体系) → C(出站写) → D(初始化装配)
