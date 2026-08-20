# gRPC-Java 源码分析卷交接文档

> 交接时间：2026-08-20  
> 交接目标：把 gRPC-Java 源码分析卷的完整状态交给下一个 AI，确保它能无缝继续推进剩余主题。

---

## 一、项目全貌

### 1.1 这是什么项目

这是一个**培训营教材**项目中的源码分析卷。整个项目为一本多卷册的技术书，其中一卷叫 `vol-rpc-governance`（RPC与治理卷），当前正在写 gRPC-Java 部分。

### 1.2 关键路径

| 用途 | 绝对路径 |
|------|---------|
| **grpc-java 源码** | `/data/workspace/source-code/code/spring/grpc-java/` (v1.83.1) |
| **产出目录（正文在此）** | `/data/workspace/source-code/openjdk-book/docs/openjdk/vol-rpc-governance/` |
| **规划/方法论/交接文档** | `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/` |
| **写作指南** | `/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-GUIDELINES.md` |
| **范围规划复盘方法论** | `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码范围规划复盘方法论.md` |
| **gRPC-Java 完整卷总规划** | `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/gRPC-Java完整卷重新规划.md` |

### 1.3 前置 Netty HTTP/2 文章（grpc-java 文章引用这些作为前置）

- `vol-netty/ch12-http2/02-framecodec-and-multiplex.md`
- `vol-netty/ch12-http2/03-connection-encoder-decoder.md`
- `vol-netty/ch12-http2/04-grpc-and-triple-on-http2.md`
- `vol-netty/ch12-http2/05-weighted-fair-queue-distributor.md`

---

## 二、已完成产物清单

### 2.1 产出目录结构

```
vol-rpc-governance/
├── ch01-grpc-runtime/           ← 主干运行时卷（4篇，全部完成）
│   ├── 01-stub-channel-clientcall.*
│   ├── 02-servercall-and-streaming-model.*
│   ├── 03-interceptors-context-deadline.*
│   └── 04-nameresolver-loadbalancer-netty-transport.*
├── ch02-codegen-builders/       ← codegen与装配卷（4篇，全部完成）
│   ├── 01-protoc-grpc-skeleton.*
│   ├── 02-channel-server-builders.*
│   ├── 03-marshaller-protoutils-message-bridge.*
│   └── 04-inprocess-testing-semantics.*
└── ch03-runtime-deepening/      ← 机制补深卷（2篇完成，第3篇待写）
    ├── 01-service-config-retry-hedging.*
    ├── 02-callcredentials-auth-boundary.*
    └── (03-health-reflection-channelz.* ← 下一步要写)
```

### 2.2 每篇文章的标准三件套

每篇文章都有三个文件：
- `*.rewrite-plan.md` — 规划文档（理解路径、失败方案、素材卡片）
- `*.md` — 正文
- `*.review-notes.md` — 审查笔记（事实审、因果审、结构审、读者审）

### 2.3 完成状态表

| 章 | 篇 | rewrite-plan | 正文 | review-notes | 状态 |
|----|-----|:---:|:---:|:---:|------|
| ch01 | 01-stub-channel-clientcall | ✅ | ✅ | ✅ | 完成 |
| ch01 | 02-servercall-and-streaming-model | ✅ | ✅ | ✅ | 完成 |
| ch01 | 03-interceptors-context-deadline | ✅ | ✅ | ✅ | 完成 |
| ch01 | 04-nameresolver-loadbalancer-netty-transport | ✅ | ✅ | ✅ | 完成 |
| ch02 | 01-protoc-grpc-skeleton | ✅ | ✅ | ✅ | 完成 |
| ch02 | 02-channel-server-builders | ✅ | ✅ | ✅ | 完成 |
| ch02 | 03-marshaller-protoutils-message-bridge | ✅ | ✅ | ✅ | 完成 |
| ch02 | 04-inprocess-testing-semantics | ✅ | ✅ | ✅ | 完成 |
| ch03 | 01-service-config-retry-hedging | ✅ | ✅ | ✅ | 完成 |
| ch03 | 02-callcredentials-auth-boundary | ✅ | ✅ | ✅ | 完成 |
| ch03 | 03-health-reflection-channelz | ❌ | ❌ | ❌ | **下一步** |

---

## 三、下一步要做什么

### 3.1 当前任务：Health / Reflection / Channelz

**这是下一个 AI 应该直接开始的任务。**

- 章节：`ch03-runtime-deepening`
- 篇名：`03-health-reflection-channelz`
- 对应总规划中的主题：`G-DEEP-5 Health / Reflection / Channelz`
- 产出：三个文件（rewrite-plan.md、.md、review-notes.md）

### 3.2 已收集的源码证据

上一轮已经读过以下关键文件，可直接使用（但仍建议回到源码复核）：

**Health 检查：**
- `services/src/main/java/io/grpc/protobuf/services/HealthStatusManager.java` — 管理健康检查服务，维护 service name -> serving status 映射
- `services/src/main/java/io/grpc/protobuf/services/HealthServiceImpl.java` — Health gRPC 服务实现（Watch 流式推送状态变更）
- `services/src/test/java/io/grpc/protobuf/services/HealthStatusManagerTest.java` — 测试证据

**Channelz 诊断：**
- `services/src/main/java/io/grpc/protobuf/services/ChannelzService.java` — channelz gRPC 服务，包装 `InternalChannelz`
- `core/src/main/java/io/grpc/internal/InternalChannelz.java` — channelz 内部注册表
- `services/src/test/java/io/grpc/protobuf/services/ChannelzServiceTest.java` — 测试证据

**Server Reflection：**
- `services/src/main/java/io/grpc/protobuf/services/ProtoReflectionServiceV1.java` — 服务器反射服务，基于 `ServerReflectionIndex`
- `services/src/test/java/io/grpc/protobuf/services/ProtoReflectionServiceTest.java` — 测试证据

### 3.3 篇章定位（建议）

- 核心困惑：Health、Reflection、Channelz 为什么不是"辅助服务杂项"，而是 grpc-java 的诊断与生产可见性层？它们如何与 server、metadata、status、transport 产生关系？
- 一句话顿悟：这三个服务本质上是把 grpc-java 运行时内部状态（服务健康、服务描述、通道/套接字/服务器统计）通过 gRPC 协议本身暴露出来——它们不是外部监控插件，而是 gRPC 生态的自描述能力。
- 文章边界：重点讲 HealthStatusManager、ChannelzService、ProtoReflectionServiceV1 与运行时的对接；不展开到 OpenTelemetry/gcp-observability 等外部监控体系。

### 3.4 工作流（三步走）

**第一步：rewrite-plan.md**
- 参考已有 plan 的结构（篇章定位、前置依赖、一句话困惑/顿悟、读者理解路径、失败方案推演、素材卡片、预估字数）
- 参考文件：`ch03-runtime-deepening/01-service-config-retry-hedging.rewrite-plan.md`

**第二步：正文.md**
- 严格遵循 WRITING-GUIDELINES.md
- 结构：困惑→失败方案→最小总图→分层正文→收网
- 禁用词：显然、不再展开、同理、依此类推、篇幅所限、容易看出 等
- 代码块用文字描述（text-only code blocks），正文必须删代码后仍成立
- 目标字数：~9000-12000 字叙述性正文
- 每个源码引用需标注 `file:line`

**第三步：review-notes.md**
- 四轮审查：事实审（源码引用核对）→ 因果审 → 结构审 → 读者审（删码测试）
- 参考文件：`ch03-runtime-deepening/02-callcredentials-auth-boundary.review-notes.md`

---

## 四、完整卷剩余任务

按总规划 `gRPC-Java完整卷重新规划.md`，以下任务尚未开始（按优先级排序）：

### 优先级 A — ch03 剩余
- `03-health-reflection-channelz` ← **立即做这个**
- `04-compression-codec-message-framing`（Compression / Codec / Message Framing）

### 优先级 B — 协议语义卷（ch04，尚未建立）
- `01-method-type-contracts`（四种调用模式与方法契约总图）
- `02-metadata-status-trailers`（Metadata、Status 与 Trailers 语义）
- `03-cancel-halfclose-completion`（取消、half-close 与完成边界）

### 优先级 C — 生产诊断卷（ch05，尚未建立）
- `01-deadline-cancel-retry-troubleshooting`（Deadline、Cancel、Retry 的线上排障）
- `02-channel-subchannel-picker-diagnosis`（Channel、Subchannel、Picker 与 Transport 状态诊断）
- `03-keepalive-flowcontrol-connection`（Keepalive、流控与连接问题分析）

### 暂缓但已建档
- xDS（机制很重，当前不急于展开）
- okhttp/cronet/servlet/android/binder 等平台变体
- opentelemetry/gcp-observability 适配

---

## 五、硬规则（必须遵守）

### 规则 1：三步走，不能跳步

每个主题必须：`rewrite-plan.md → .md → review-notes.md`。不能直接写正文。

### 规则 2：回到源码核验

不能只凭现有规划文档或上一轮的笔记落笔。正式写每个 rewrite-plan 前，必须回到 `/data/workspace/source-code/code/spring/grpc-java/` 重新核验关键入口类、调用链、类名与方法名。

### 规则 3：遵循写作指南

所有正文必须遵循 `/data/workspace/source-code/openjdk-book/docs/openjdk/WRITING-GUIDELINES.md` 的全部规则。核心：
- 困惑→失败方案→最小总图→分层正文→收网
- 主语是角色不是变量
- 代码只能当证据不能当骨架
- 删掉代码后文章必须仍成立
- 禁用词清单见指南

### 规则 4：控边界

每篇只讲自己的主题，不过度吞下下一篇的内容。例如 Health/Reflection/Channelz 篇不要把生产排障全吞进来。

### 规则 5：不要切去别的阶段

当前是"RPC与治理"阶段的 gRPC-Java 部分。不要切去 Kafka/RocketMQ/ZooKeeper/Seata（那些是"消息与事务"阶段），也不要切去 Dubbo/Feign/Nacos/Sentinel（那些在 gRPC-Java 基线篇完成后再做）。

### 规则 6：review-notes 必须做删码测试

review-notes 的第四轮"读者审"必须验证：删除所有代码块后，正文是否仍能复述核心结论。

---

## 六、方法论背景

### 6.1 为什么 gRPC-Java 不只 4 篇

最初只规划了 4 篇主干运行时文章。但按《源码范围规划复盘方法论》审视后发现，4 篇只覆盖了"主干层"，还缺：
- 规范层（方法契约、Metadata/Status 语义）
- 集成层（codegen、builder、InProcess/testing）
- 机制补深层（Service Config/Retry/Hedging、CallCredentials、Health/Reflection/Channelz、Compression）
- 生产层（排障、诊断、连接问题）

因此重新规划了完整卷（见 `gRPC-Java完整卷重新规划.md`），结构为 6 章约 26 篇。当前已完成 10 篇。

### 6.2 方法论核心要点

《源码范围规划复盘方法论》的核心教训：
1. 不能把"类名出现"误当成"机制闭环完成"
2. 不能只按目录/包切域，必须按机制重组知识域
3. 不能低估运行时诊断能力（Health/Reflection/Channelz）
4. 不能只做主干闭环，必须补完整卷（规范层/集成层/机制补深/生产层）
5. 每个知识域都要围绕读者真实困惑建立
6. 必须记录排除理由和待复核边界

---

## 七、当前状态总结

```
已完成：ch01 (4篇) + ch02 (4篇) + ch03前2篇 = 10篇
进行中：ch03/03-health-reflection-channelz（源码证据已收集，三件套未写）
下一步：写 ch03/03-health-reflection-channelz 的 rewrite-plan.md
后续：ch03剩余2篇 → ch04协议语义卷(3篇) → ch05生产诊断卷(3篇) → ...
```

**下一个 AI 的默认动作：开始写 `ch03-runtime-deepening/03-health-reflection-channelz.rewrite-plan.md`。**

---

## 八、关键文件快速索引

| 文件 | 用途 |
|------|------|
| `gRPC-Java完整卷重新规划.md` | 完整卷 6 章 26 篇规划，当前工作的上位结构 |
| `源码范围规划复盘方法论.md` | 所有规划必须遵循的方法论 |
| `WRITING-GUIDELINES.md` | 所有正文必须遵循的写作标准 |
| `RPC与治理-HANDOVER.md` | RPC与治理主题级交接（更上层的交接） |
| `RPC与治理主题总规划.md` | RPC与治理主题的机制轴心重组 |
| `gRPC-Java在RPC与治理主题中的新规划.md` | gRPC-Java 在 RPC 主题中的 4 线定位 |
