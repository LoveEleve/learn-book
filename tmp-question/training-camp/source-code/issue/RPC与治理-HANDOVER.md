# RPC 与治理主题交接文档

> 交接目标：把当前“RPC 与治理”主题的规划状态、已完成产物、下一步应做什么、不能做什么，完整交给下一个 AI，确保它能无缝继续，而不会偏回单仓乱钻或切去别的阶段。

---

## 一、当前总目标

当前正在推进的是总执行计划中的：

- **阶段 5：RPC 与治理**
- 主题范围：`Feign → Dubbo → gRPC → Spring Cloud Commons / OpenFeign / Gateway / Alibaba → Nacos → Sentinel`
- 参考来源：
  - `源码分析执行计划.md`
  - `源码范围规划复盘方法论.md`

当前**不是**在做：
- 消息与事务（Kafka / RocketMQ / ZooKeeper / Seata / Curator / SofaJRaft）
- Netty 新正文补写
- 其他阶段的任何仓库正文

如果后续 AI 看到别的规划建议，比如“先做 Kafka”，那是**另一条阶段线**，不能插入当前这条 `RPC 与治理` 主线。

---

## 二、详细路径（绝对路径）

### 主题与方法论文档

1. 交接文档（当前文件）  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RPC与治理-HANDOVER.md`

2. 主题总规划  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RPC与治理主题总规划.md`

3. gRPC-Java 在该主题中的新规划  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/gRPC-Java在RPC与治理主题中的新规划.md`

4. 旧的 gRPC-Java 单仓规划  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/gRPC-Java源码学习范围规划.md`

5. 执行计划  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码分析执行计划.md`

6. 范围规划复盘方法论  
`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码范围规划复盘方法论.md`

### 复用 Netty 前置（已完成）

7. HTTP/2 API 层  
`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/ch12-http2/02-framecodec-and-multiplex.md`

8. HTTP/2 连接主链  
`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/ch12-http2/03-connection-encoder-decoder.md`

9. gRPC / Triple over HTTP/2 桥接  
`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/ch12-http2/04-grpc-and-triple-on-http2.md`

10. HTTP/2 额度分配器  
`/data/workspace/source-code/openjdk-book/docs/openjdk/vol-netty/ch12-http2/05-weighted-fair-queue-distributor.md`

---

## 三、当前状态表

| 状态 | 内容 | 说明 |
|---|---|---|
| 已完成 | RPC 与治理主题总规划 | 已按机制轴心重组，不再按仓库平铺 |
| 已完成 | gRPC-Java 主题内重新规划 | 已确定它在本组里承担“RPC 运行时基线篇”角色 |
| 已完成 | 第一篇正文入口选择 | 已确定从 gRPC-Java 客户端调用主线开始 |
| 进行中 | 无 | 当前不是“继续规划”，而是准备切入第一篇正文前的最后核验 |
| 下一步 | 起第一篇 `rewrite-plan` | 主题：`gRPC-Java：Stub、Channel 与 ClientCall 调用主线` |
| 暂缓 | Dubbo / Feign / Commons / Nacos / Sentinel 正文 | 必须等 gRPC 第一篇基线篇先落地 |
| 禁止切换 | Kafka / RocketMQ / 消息与事务 | 属于阶段 4，当前不应插入 |

---

## 四、已经完成的产物

### 1. 主题级总规划

已完成文件：
- `RPC与治理主题总规划.md`

这份文档已经完成了三件事：

1. **否定了按仓库平铺写法**
   - 不能直接按：Feign → Dubbo → gRPC → Commons → OpenFeign → Gateway → Alibaba → Nacos → Sentinel 顺序开正文

2. **按机制轴心重组了整组主题**
   - 轴心 1：调用入口与代理抽象
   - 轴心 2：传输与协议主线
   - 轴心 3：服务发现、注册与命名
   - 轴心 4：实例选择、负载均衡与集群容错
   - 轴心 5：元数据、请求上下文与跨层透传
   - 轴心 6：配置中心、动态刷新与命名上下文
   - 轴心 7：限流、熔断、降级与治理规则
   - 轴心 8：完整集成层与生态桥接

3. **给出了这一组的分阶段写作顺序**
   - 阶段 A：RPC 运行时内核
   - 阶段 B：服务发现与实例选择
   - 阶段 C：Spring 集成桥接层
   - 阶段 D：治理与规则引擎

### 2. gRPC-Java 在该主题中的重新规划

已完成文件：
- `gRPC-Java在RPC与治理主题中的新规划.md`

这份文档已经完成了两件事：

1. **把 gRPC-Java 从旧单仓 6 域顺序里解耦出来**
   - 不再按旧顺序平均推进
   - 重新组织为 4 条机制线：
     - G-RPC-1：调用入口与客户端运行时
     - G-RPC-2：服务端运行时与流式交互
     - G-RPC-3：Interceptor、Context 与 Deadline
     - G-RPC-4：传输与实例选择桥接层

2. **明确了本组第一篇正文入口**
   - 第一篇不该写服务发现
   - 不该先写 Context/Deadline
   - 不该直接铺服务端全链路
   - **应先写：客户端调用主线**

---

## 五、当前已经达成的关键判断

### 判断 1：这一组不能直接进多仓正文

必须先按机制轴心拆分，不然后续会重蹈 Netty 旧规划的问题：
- 同一机制被拆散到多个仓库
- 集成层和内核层顺序错乱
- 最后形成“很多仓库串讲”，不是“完整卷”

### 判断 2：这一组的第一篇应优先从 gRPC-Java 开始

原因已经确认：

1. **它最适合复用已经写完的 Netty HTTP/2 卷成果**
2. **它最适合作为 RPC 运行时基线篇**
3. **它的信息密度刚好合适**
   - 比 Feign 更底层
   - 比 Dubbo 更集中
   - 比 Spring 集成层更少装配噪音

注意：这里用词是“**应优先**”，不是“绝对不可改变”。如果下一个 AI 在重新核验本地源码后，发现篇章边界必须微调，可以在**不打断主题主线**的前提下修正，但默认顺序不应改变。

### 判断 3：当前已经过了主题总规划阶段

下一步不该继续扩：
- RPC 与治理总规划
- Dubbo / Feign / Nacos / Sentinel 子规划
- 其他阶段内容

当前最合理的动作是：
- **准备第一篇正文的 rewrite-plan**

---

## 六、硬规则（必须遵守）

### 规则 1：不能直接写正文

顺序必须是：

1. 先做本地源码核验
2. 再写第一篇正文的 `rewrite-plan`
3. 再写正文
4. 再写 review notes

不能跳过 `rewrite-plan` 直接写正文。

### 规则 2：不能只凭现有规划文档落笔

虽然已经有：
- 主题总规划
- gRPC 新规划
- 旧单仓规划

但正式写第一篇 `rewrite-plan` 前，仍然必须**回到本地仓库源码重新核验**：
- 关键入口类
- 真实调用链
- 当前版本的类名与方法名
- 关键测试

不能把旧规划里的类名或叙述直接当成已验证事实。

### 规则 3：不能切去别的阶段

下一个 AI 不应：
- 讨论 Kafka / RocketMQ / ZooKeeper / Seata
- 回头继续写 Netty
- 开 Dubbo / Feign / Commons / Nacos / Sentinel 正文

除非用户明确改任务，否则必须沿当前主线推进。

### 规则 4：第一篇必须严格控边界

第一篇的任务是：
- 建立 RPC 运行时基线
- 解释本地 Stub 调用如何变成远程调用

第一篇**不要**过度吞下：
- 服务端全链路
- 四种流式调用全矩阵
- Context / Deadline
- NameResolver / LoadBalancer
- Netty transport 细节全展开
- Dubbo / Feign 横向对照

这些内容后移。

---

## 七、下一个 AI 默认应做的事情

### 默认下一步

开始写：
- **`gRPC-Java：Stub、Channel 与 ClientCall 调用主线` 的 rewrite-plan**

这是默认下一步，不是机械命令。前提是先完成本地源码核验。

### 这一篇的使命

它不是整本 gRPC-Java 卷的总序篇，也不是 transport 篇，而是：

- 先建立 **RPC 调用运行时基线**
- 先回答：
  - Stub 是什么
  - `stub.method(request)` 怎么变成一次远程调用
  - `ManagedChannel`、`ClientCall`、`ClientCalls` 各自处在哪个位置
  - Unary 调用的最短闭环是什么

### 建议标题

- `gRPC-Java：Stub、Channel 与 ClientCall 调用主线`

### 建议 rewrite-plan 结构

#### 篇章定位
- 核心困惑：一个本地 Stub 方法调用，为什么最终会变成一次远程 RPC？
- 一句话顿悟：Stub 不是“远程代理”的泛泛概念，而是把方法描述、调用选项和请求对象收束成 `ClientCall` 的一层客户端运行时入口。
- 文章边界：只讲客户端调用主线，重点是 `AbstractStub / ClientCalls / ManagedChannel / ClientCall`，transport 只做桥接，不重写 Netty HTTP/2 主线。

#### 依赖
- 硬前置：Netty HTTP/2 API 层与连接主链相关已完成篇章、Promise/Future 基础
- 软前置：不要求读者先懂服务发现、LoadBalancer、Context

#### 素材卡片建议
至少补足：
- `AbstractStub`
- `ClientCalls`
- `ClientCall`
- `ManagedChannel`
- unary 最短闭环
- `CallOptions`
- transport 只作为出口桥接

#### 理解路径建议
1. 为什么 RPC 不能只是“接口代理”四个字带过
2. Stub 到底做了什么最小包装
3. `ClientCalls` 怎样把调用模式标准化
4. `ManagedChannel` 处在哪一层
5. `ClientCall` 才是 transport 前的最后统一抽象
6. 篇末桥到服务端主线与流式调用

#### 失败方案必须写
至少要推演：
- 只讲动态代理，不讲 `ClientCall`
- 只讲 Netty transport，不讲 `Stub -> ClientCalls`
- 把 unary 和 streaming 混成一套

---

## 八、下一个 AI 不该做的事

1. **不要重新讨论“要不要先做 Kafka / RocketMQ”**
   - 那是阶段 4，不是当前主线。

2. **不要重新做一份 RPC 与治理总规划**
   - 已有：`RPC与治理主题总规划.md`

3. **不要继续扩 gRPC-Java 单仓总规划**
   - 已有：`gRPC-Java在RPC与治理主题中的新规划.md`

4. **不要直接开正文**
   - 必须先本地源码核验，再 `rewrite-plan`，再正文。

5. **不要第一篇就同时写客户端 + 服务端 + transport + loadbalancer**
   - 必须控制边界，否则第一篇又会退化成“仓库总览式堆叠”。

---

## 九、交接结论

当前状态不是“继续规划”，而是：

- 主题级总规划已完成
- gRPC-Java 在该主题中的重排已完成
- 第一篇正文入口已确定
- 下一步默认动作已明确

**因此，下一个 AI 应先回到本地 `grpc-java` 仓库做一轮源码核验，然后直接进入：`gRPC-Java：Stub、Channel 与 ClientCall 调用主线` 的 `rewrite-plan`。**