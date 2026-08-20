# Spring 生态源码分析 — 交接文档 v5

> **日期**: 2026-08-17
> **状态**: Stage 1 全部完成 — Netty 13 章 ✅ | Tomcat 10 域 (T-1~T-10) ✅ | Tomcat 深度 REVIEW 3 轮收敛完成
> **更新**: Tomcat 已从 5 域扩至 10 域并全部收官；harness 47/47、问数 200/200、锚点 244/244、时空溯源与 deep-review 全完成; next=Stage 2 Spring Framework

---

## §零 方法论（完整内联 — 无需读外部文件）

### 0.1 完整知识规划管线（每域必须全部走完）

```
01 逐源提取 → 01 聚合(P1/P2/P3) → 02 深度分类(🔴🟡🟢) → 03 聚类(教学顺序)
  → Per-Article Outline(v5 教学叙事) → completeness-questions(全视角验证) → 深审修复
```

**不可跳过任何步骤**。

**关键区分**: `knowledge-planning/` 是内部验证文件（含提取表+聚合表+分类表+聚类），`outlines/` 是交付物（TOC 大纲）。不能说 knowledge-planning 是"大纲"——混淆两者是 Netty Ch4-Ch9 最早复发的缺陷。

### 0.2 逐源提取格式

```markdown
| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| PoolChunk.java:364 | handle 64位编码(pageRun 15b+pageCount 15b+isUsed 1b+isSubpage 1b+bitmapIdx 32b) | High |
```

**置信度**: High(源码直接读取) / Medium(类名推断, 需验证) / Low(猜测)

### 0.3 聚合规则

| 出现文件数 | P-Level |
|----------|:--:|
| ≥5 文件引用 | P1 — 全系统共识 |
| 2-4 文件引用 | P2 — 局部重要 |
| 1 文件独有 | P3 — 独立知识点 |

### 0.4 深度分类

| 级别 | 含义 |
|:--:|------|
| 🔴 Deep | 承载核心设计决策 — 缺了它框架不成立 |
| 🟡 Working | 有设计决策但非核心 |
| 🟢 Surface | 机制性了解即可 |

### 0.5 Per-Article Outline 格式（v5 教学叙事 — **唯一正确格式**）

每机制必须写成 **四要素段落**，不是 bullet 列表：

```
### N. 机制名 — 一句话描述

场景: [一句真实场景，让读者知道"为什么要看这个"]

源码路径: file:line + 函数名 + 调用链。精确到具体行号。
  例如: `PoolChunk.java:memoryMap[]` — 数组 size = maxOrder << 1，`memoryMap[1]` 是根

关键设计: [为什么这样实现] + [按需标注: 模式 或 规范对应]
  解释设计意图，不是重复源码。必须有"为什么"。
  纯框架项目(Netty): 标注 [模式: Chain of Responsibility]
  规范参考实现(Tomcat): 标注 → 实现规范: ServletContext (6.0)

数据流: [具体的数据流转路径，从前一步到后一步]
  例如: `allocateRun(4 pages) → (depth<<15) | pageCount → handle → Arena → ThreadCache`
```

**严禁**:
- ❌ 技术清单式 bullet: `- read(buf): 返回值 n>0=字节数, n=0=无数据` (v1 格式)
- ❌ 叙事散文: `1. 问题引入 — 从 Ch5 过渡` (v2 格式)
- ❌ 裸行号: `(line 233)` — 必须 `(File.java:233)`
- ❌ 伪行号: `(File.java note)` 或 `(__LINE__)`
- ❌ 代码拼接错误: `srcHbdstHbdstOffset` → 应为 `src.hb, srcIdx, dst.hb, dstIdx`

**桥规则**: 每篇末必须 `→ 引出 ChX`，不能断裂。最后一个域(如 Ch14)不需要桥。

### 0.6 多维度分析（规范参考实现项目必须）

规范参考实现项目(Tomcat、Servlet 容器)除源码提取外，每域需含：

- **规范对应**: `→ 实现规范: ServletContext (1.0)` — 规范接口名 + 引入版本
- **设计模式**: `[模式: Chain of Responsibility]` — GoF 模式显式标注
- **架构意图**: 控制流方向(谁调谁)、分层职责、生命周期拓扑

### 0.7 深审清单（宣称完成前必须逐项检查）

- [ ] 每机制四要素（场景/源码路径/关键设计/数据流）全部存在且非空
- [ ] **逐行 Read 全部文章**: 检查语义矛盾(前后两句互相矛盾，如"占两个 slot"vs"一个 slot")、编辑残留(重复句)、内容省略("..."代替关键步骤)
- [ ] `grep -c '(line' *.md` → 0（零裸行号）
- [ ] `grep -c '→ 引出' *.md` → 每篇 1 个桥（末篇 0）
- [ ] KP 计数标题与正文条目数一致（`grep -cE '^\d+\.' 对比标题声明数`）
- [ ] grep 代码拼接: 无 `Hbdst`/`REPALY`/`SELETEOR` 等拼接错误
- [ ] **源锚行号跨文章冲突检查**: 同一域不同文章引用了同一源文件的重叠行号范围 → grep 源码验证 → 修正
- [ ] 规范参考实现: 每域含规范接口→实现类映射，版本号对照源码验证
- [ ] **尾篇防薄**: 域的最后一篇(src锚点数 ≥ 全域平均 70%) — 连续 3 域出现尾篇薄症

### 0.8 域审核前置（继承旧 AI 范围规划时）

1. 域过载检查: 单域核心类 > 10 且行数 > 5000 → 考虑拆分
2. 淘汰清单: 该机制在现代 Spring Boot 生态中还用吗？(如 Session → Redis 替代)
3. 规范缺口: 规范接口全映射到域了吗？
4. **禁止过度加域**: 子机制已在现有域中覆盖 → 不独立成域。用 `wc -l` 验证规模（<1000 行且域描述已有提及 → 非遗漏）

### 0.9 密度梯度控制

🔴A 域: 39-69 行/篇，3-6 机制/篇
🟡B 域: 35-49 行/篇，2-4 机制/篇

**不能跨域比行数** — 以 KP 讲透（四要素全）为目标，行数是结果不是追标。

### 0.10 全视角提问

每域完成 outline 后，从 ≥3 身份（开发者/架构师/学生）提 ≥10 问，每问标注大纲可定位节号。覆盖率必须 100%。

### 0.11 淘汰机制处理

**场景句(场景:)和数据流(数据流:)不能以淘汰的配置方式为主体**。必须以该框架在当代生态中的实际使用方式为主——淘汰机制仅作为"备选方案"附带提及。

| 场景 | 淘汰方式 | 当代方式 |
|------|------|------|
| Tomcat 配置 | `server.xml` `<Connector>` | Spring Boot `server.port` / `TomcatServletWebServerFactory` |
| Servlet/Filter 声明 | `web.xml` `<servlet>`/`<filter>` | `@WebServlet`/`@WebFilter` 注解 |
| Valve 添加 | `server.xml` `<Valve>` | `WebServerFactoryCustomizer` → `addValve()` |

**检测**: `grep -rn 'server\.xml\|web\.xml' outlines/` — 所有引用必须是为辅形式（"独立部署中..."），不能作为主场景句。

---

## §一 Netty — 全部 13 章 (36 篇)

| 章 | 域 | 级别 | 篇数 | 行数 | 提问 | 状态 |
|:--:|------|:--:|:--:|:--:|:--:|:--:|
| Ch1 | NIO ByteBuffer | 🔴 | 3 | 187 | 32 ✅ | ✅ |
| Ch2 | Channel | 🔴 | 3 | 149 | 16 ✅ | ✅ |
| Ch3 | Selector | 🔴 | 2 | 108 | 12 ✅ | ✅ |
| Ch4 | ByteBuf | 🔴 | 5 | 295 | 30 ✅ | ✅ |
| Ch5 | EventLoop | 🔴 | 4 | 184 | 25 ✅ | ✅ |
| Ch6 | Promise | 🔴 | 3 | 157 | 18 ✅ | ✅ |
| Ch7 | Pipeline | 🔴 | 4 | 186 | 20 ✅ | ✅ |
| Ch8 | MemoryPool | 🔴 | 4 | 196 | 13 ✅ | ✅ |
| Ch9 | Bootstrap | 🟡 | 2 | 78 | 10 ✅ | ✅ |
| Ch10 | Codec | 🟡 | 2 | 78 | 9 ✅ | ✅ |
| Ch11 | HTTP | 🟡 | 2 | 80 | 8 ✅ | ✅ |
| **Ch12** | **HTTP/2 Codec** | **🟡** | **1** | **64** | **11 ✅** | **✅ v5** |
| Ch13 | Epoll | — | — | — | — | 跳过 |
| Ch14 | HashedWheelTimer | 🟡 | 1 | 46 | 12 ✅ | ✅ v5 |

### Ch12+Ch14 — 全部 v5 源码验证完成

**Ch12 演进**: v1→v2(补 4 数据流+10 锚点) → v3→v4(9→10 帧+KP 补全+跨层一致性) → v4→v5(源码验证: readHeadersFrame:295→427 / readDataFrame:375→415 / readSettingsFrame:460→525 / verifyStreamId→verifyFrameState:207)

**Ch14**: v5 源码验证: tickPerWheel→ticksPerWheel / §1 行号优化(字段声明→区分构造器初始化)

### Ch12 跨框架依赖

gRPC-Java (`GrpcHttp2ConnectionHandler`) 和 Dubbo Triple 协议直接依赖 Netty HTTP/2。此域是 Stage 1 → Stage 5 的桥梁域。

---

## §二 Tomcat — 10 域收官 / 深度 REVIEW 完成

### 当前状态

| 产出 | 状态 |
|------|:--:|
| 范围规划 | ✅ 10 域 (09 审计 6→10；T-5 Session 砍除改 Mapper) |
| KP + outlines + completeness questions | ✅ 全部完成 (10 域 / 200 问 / A-B-C-D 四节齐全) |
| harness | ✅ 4 个红域全通过 (47/47) |
| deep-review + temporal-trace | ✅ 完成 |
| 深度 REVIEW | ✅ 3 轮收敛 (D1~D12 全修复) |

### 10 域（4🔴 + 6🟡）

| 域 | 级别 | 核心 | 篇数 | 提问 | 状态 |
|------|:--:|------|:--:|:--:|:--:|
| T-1 容器+Lifecycle | 🔴 | Server→Service→Engine→Host→Context→Wrapper + Lifecycle 11 态 | 4 | 20 ✅ | ✅ 收官+harness 15/15 |
| T-2 Connector+Adapter | 🔴 | Http11NioProtocol/NioEndpoint/CoyoteAdapter/Request/Response | 4 | 20 ✅ | ✅ 收官+harness 11/11 |
| T-3 Pipeline+双链 | 🔴 | StandardEngineValve→WrapperValve + ApplicationFilterChain | 4 | 20 ✅ | ✅ 收官+harness 10/10 |
| T-4 线程模型 | 🔴 | Acceptor/Poller/Worker 三线程 + maxConnections/maxThreads | 2 | 20 ✅ | ✅ 收官+harness 11/11 |
| T-5 Mapper路由 | 🟡 | Exact/Prefix/Extension/Welcome 四级匹配 + MapperListener | 2 | 20 ✅ | ✅ 收官 |
| T-6 ClassLoader | 🟡 | WebappClassLoaderBase 双亲委派打破 + filter + 并行加载 | 1 | 20 ✅ | ✅ 收官 |
| T-7 SpringBoot集成 | 🟡 | TomcatServletWebServerFactory/Customizer/GracefulShutdown | 2 | 20 ✅ | ✅ 收官 |
| T-8 HTTP报文解析 | 🟡 | HttpParser/MimeHeaders/Parameters/Cookie | 2 | 20 ✅ | ✅ 收官 |
| T-9 WebSocket | 🟡 | WsFrameBase/WsSession/WsWebSocketContainer | 2 | 20 ✅ | ✅ 收官 |
| T-10 集群通信+复制 | 🟡 | GroupChannel/McastServiceImpl/DeltaManager | 2 | 20 ✅ | ✅ 收官 |

### 启动命令

```bash
BASE=/data/workspace/source-code/code/spring/tomcat/java/org/apache

# 01 逐源提取 — 核心文件
ls $BASE/catalina/core/StandardServer.java $BASE/catalina/core/StandardService.java \
   $BASE/catalina/core/StandardEngine.java $BASE/catalina/core/StandardHost.java \
   $BASE/catalina/core/StandardContext.java $BASE/catalina/core/StandardWrapper.java \
   $BASE/catalina/core/ContainerBase.java $BASE/catalina/Lifecycle.java \
   $BASE/catalina/connector/CoyoteAdapter.java $BASE/catalina/connector/Request.java \
   $BASE/catalina/connector/Response.java $BASE/catalina/core/ApplicationFilterChain.java \
   $BASE/catalina/core/ApplicationDispatcher.java $BASE/catalina/startup/Tomcat.java \
   $BASE/coyote/http11/Http11NioProtocol.java $BASE/coyote/http11/Http11Processor.java

# 走完整管线: 01提取→聚合→分类→聚类→outlines v5(3-4篇)→全视角验证
# 每域含: 规范对应表 + [模式:] 标注 + 架构分层意图
```

### Tomcat vs Netty 差异

| 维度 | Netty | Tomcat |
|------|------|------|
| 类型 | 纯框架 | 规范参考实现(Servlet 6.0) |
| 控制流 | 框架调用户 | 规范调用户(反转) |
| 额外维度 | — | 规范映射 + 5 个 GoF 模式 + 架构分层 |

---

## §三 全局执行计划

| 阶段 | 主题 | 仓库数 | 状态 |
|:--:|------|:--:|:--:|
| 1 | 基础 I/O | Netty+Tomcat | ✅ **55 篇 v5** |
| 2 | 核心容器 | Spring Framework+Boot | 🔄 S1-1 ✅ / 83 域 ⏳ |
| 3 | 数据存储 | HikariCP→MyBatis→Redis→ES | ⏳ |
| 4 | 消息事务 | RocketMQ→Kafka→ZK→Seata | ⏳ |
| 5 | RPC治理 | Feign→Dubbo→gRPC→Nacos | ⏳ |
| 6 | 可观测 | Micrometer→SkyWalking→Arthas | ⏳ |

---

## §四 踩坑速查

| # | 坑 | 检测 |
|:--:|------|------|
| 1 | 裸行号 | `grep '(line' *.md` |
| 2 | 代码拼接错误 | `grep 'Hbdst\|REPALY' *.md` |
| 3 | KP 计数不一致 | 标题声明数 v.s 正文列举条目数 |
| 4 | 尾篇薄症 | 最后一篇 src 锚点 < 全域平均 70% |
| 5 | 收尾旧格式残留 | 后面章节还在 v1 bullet 格式 |
| 6 | 🟡B 域密度滑坡 | Ch9+ 从 49-69 行跌到 15-29 行 |
| 7 | 域过度膨胀 | 子机制已覆盖却提议独立成域 |
| 8 | 跨章桥断裂 | 某篇末无 `→ 引出 ChX` |
| 9 | 规范版本过时 | 规划写 Servlet 3.0 实际 6.0 — 对照 build.properties 验证 |
| 10 | 语义矛盾 | 同一段前后两句互相矛盾（逐行 Read 发现） |
| 11 | 源锚行号冲突 | 两篇文章引用同一源文件的重叠行号范围 |

---

## §五 文件路径

```
项目产出: /data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/
├── netty/
│   ├── knowledge-planning/ (13)    ✅
│   ├── outlines/           (13章 36篇) ✅
│   └── HANDOFF.md
├── tomcat/
│   ├── knowledge-planning/t1-container-lifecycle.md  ← 232行/119表行/5章
│   └── outlines/t1-container-lifecycle/              ← 4篇/15问/214行
└── issue/
    ├── HANDOVER.md              ← 本文档
    ├── 源码分析执行计划.md        ← 完整 6 阶段计划 (32 仓库/337 域)
    └── Tomcat源码学习范围规划.md   ← 早期 5 域范围规划(已过时，现以 10 域收官结果和 HANDOFF-TOMCAT.md 为准)
```
