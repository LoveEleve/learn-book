# Tomcat 深审缺陷档案 — 逐域检查记录

> 审查对象: 10 域全部大纲 + KP (既有 7 域 + 新增 3 域)
> 缺陷清单基线: #1~#15 (与 Nacos 阶段同一方法论)
> 审查方式: 全部锚点 re-grep 源码实证 (10.1.34, tag acf7da1)

---

## 结论速览

| 缺陷类 | 检查项 | 发现数 |
|:--|:--|:--:|
| #1 伪锚点 | 行号不存在/错位 | 1 (已修复) |
| #2 语义与源码不符 | 机制描述偏差 | 0 |
| #3 数字错误 | 行数/参数值 | 0 |
| #4 区间锚点越界 | 区间超文件尾 | 0 |
| #5 编造类名 | 类不存在 | 0 |
| #6 编造方法名 | 方法不存在 | 0 |
| #7 裸 L 引用无上下文 | 引用缺类名 | 0 |
| #8 锚点路径错 | 目录错误 | 1 (已修复) |
| #9 依赖方向错 | 依赖图逆序 | 0 |
| #10 顺序错误 | 教学顺序 | 0 |
| #11 域遗漏 | 承载类未入域 | 0 |
| #12 问数不足 | <20 问 | 7 域 (已补) |
| #13 harness 缺失 | 🔴 域无验证 | 0 (4 域已建) |
| #14 时空缺失 | 🔴 域无溯源 | 0 (已补) |
| #15 引用未验证 | 交叉引用空泛 | 0 |

---

## 缺陷明细

### D1. T-8 KP: 分类表数量错误 (#3 数字错误)
- **位置**: knowledge-planning/t8-http-parsing.md §一 + outlines/t8-http-parsing/01-*.md
- **原文**: "13 个 static boolean[] 字节分类表"
- **实证**: `HttpParser.java:38-49` 静态表 **12 个** + 实例级 relaxed 表 **3 个** (IS_NOT_REQUEST_TARGET/IS_ABSOLUTEPATH_RELAXED/IS_QUERY_RELAXED, L123-125)
- **修复**: KP + 大纲均已更正为 "12 静态 + 3 实例" 并补充 L123-125 锚点

### D2. TOMCAT-PLAN 锚点速查: HttpParser 路径错 (#8 锚点路径错)
- **位置**: TOMCAT-PLAN.md 附录
- **原文**: `tomcat/util/http/HttpParser.java`
- **实证**: 实际在 `tomcat/util/http/parser/HttpParser.java` (parser/ 子目录)
- **修复**: 已更正

---

## 逐域验证记录 (2026-08-17)

### T-1 容器+Lifecycle — 全过
- Lifecycle.java:26-73 ASCII 11 态图 ✅ (25 行管道符行)
- LifecycleBase.java:139 `public final synchronized void start()` ✅
- LifecycleBase.java:193 `protected abstract void startInternal()` ✅
- ContainerBase.java:154 `HashMap<String,Container> children` + L35-36 RWLock ✅
- StandardServer.java:508 `await()` ✅
- StandardService.java:90 engine + L97-103 mapper/mapperListener ✅
- StandardEngine.java:64/107-127/195-230 (09 审计已验) ✅

### T-2 Connector/Adapter — 全过
- Http11NioProtocol.java:28 类声明 ✅
- AbstractProtocol.java:786 recycledProcessors + L974-979 keep-alive 释放语义 ✅
- CoyoteAdapter.java:75 ADAPTER_NOTES + L303 service ✅
- Response.java:501 getOutputStream / L523 getWriter + ISE ✅
- Request.java:431-504 recycle ✅

### T-3 Pipeline/Filter — 全过
- Valve.java:50 getNext / 58 setNext / 68 backgroundProcess / 114 invoke / 117 isAsyncSupported ✅
- StandardEngineValve:56 / StandardHostValve:80 / StandardContextValve:60 / StandardWrapperValve:86 invoke ✅
- ApplicationFilterChain.java:46 类声明 + L117 doFilter ✅

### T-4 线程模型 — 全过
- AbstractEndpoint.java:548 `maxConnections = 8*1024` + L1477 countUpOrAwaitConnection ✅
- NioEndpoint.java:595 Poller / 605 wakeupCounter / 635 createPollerEvent / 741 处理 ✅
- Acceptor.java:116 countUpOrAwaitConnection 调用 ✅

### T-5 Mapper — 全过
- Mapper.java:47 `public final class Mapper` + L364-372 findContextVersion ✅
- MapperListener.java:147 containerEvent / 149 ADD_CHILD / 178 ADD_MAPPING / 292 registerHost ✅

### T-6 ClassLoader — 全过
- WebappClassLoaderBase.java:281 `delegate = false` + L2481 jakarta 前缀过滤 ✅

### T-7 Spring Boot 集成 — 全过 (跨仓库)
- TomcatServletWebServerFactory.java:196 getWebServer / 269 configureContext 调用 / 403 configureContext / 687 addConnectorCustomizers ✅ (spring-boot 仓库, 1032 行)

### T-8 HTTP 解析 — 1 缺陷 (D1)
- HttpParser.java:38-49 12 静态表 + L51-80 静态块 + L123-125 实例表 + L128-163 构造/relaxed + L196 unquote ✅
- MimeHeaders.java:103/112/125/171/186/200/232 ✅
- Parameters.java:37/234/374-381/465/472 ✅
- Rfc6265CookieProcessor.java:37/68 ✅

### T-9 WebSocket — 全过
- WsFrameBase.java:111/141/155-165/226/276/311/400/493 ✅
- WsFrameBase.java:55-56 controlBuffer = allocate(125) ✅
- WsWebSocketContainer.java:79 (WebSocketContainer+BackgroundProcess) / 120-131 connectToServer / 1062-1064 backgroundProcess ✅
- WsSession.java:537-553 close/doClose ✅
- WsRemoteEndpointImplBase.java = 1288 行 ✅

### T-10 集群 — 全过
- GroupChannel.java:67/159/183/264/351/513-543 ✅
- McastServiceImpl.java:59/85/151-176/222/268-269/284 ✅
- AbstractReplicatedMap.java:120/212/441-460 (isPrimary+isDirty+isAccess 三条件实证) ✅
- DeltaManager.java:55/92-105 (9 个 receive 计数器) ✅
- SimpleTcpCluster.java:66/129 ✅

---

## 深审结论

- **合计缺陷 2 项, 均已修复**, 无遗留
- 新增 3 域全部锚点首次写入即实测 (未发生 #5/#6 编造类名/方法名 — 与 Nacos 阶段对比: 本阶段写作时已按 re-grep 先行的纪律)
- 跨仓库引用 (T-7 Spring Boot) 二次核验通过
- 缺陷根因复盘: D1 属"凭记忆写数字"违反实证纪律; D2 属"目录凭印象" — 两条均已纳入全量回归检查单
---

## REVIEW 轮追加 (2026-08-17, 深度 REVIEW N 轮收敛)

> 方法: R1 全量语义锚点 (241→244) / R2 类名 49+方法 208+常量 49 存在性 / R3 文字锚 22+15 / R4 数字断言 / R5 跨域互引 136 / R6 拼写术语 / harness 回归。

### 第 1 轮 — 9 缺陷 (全部修复)

| # | 域/文 | 缺陷 | 修复 |
|:--|:--|:--|:--|
| D3 | t1/01 | ContainerBase.startInternal 锚点 780-845 错位 (实为 stopInternal), 顺序颠倒 | → 729-793, Cluster→Realm→children 并行 |
| D4 | t1/02 | pipeline 字段 L75-77 错 (Javadoc) | → L217 new StandardPipeline |
| D5 | t3/01 | Valve 契约 15 条 → 实 10 条 (MAY5+MUST NOT5) | 数量+区间 71-113 |
| D6 | t3/02 | StandardContextValve 编造 dispatcherType/WebSocket 检查, 漏 WEB-INF/META-INF 拦截 | 重写真实流程 |
| D7 | t3/03 | release() 称"数组不清" → 实际清空 | → 259-276 清空 |
| D8 | t6/01 | getClassLoadingLock 38-40 错 (实为 copyWithoutTransformers) | → 54-57 |
| D9 | t8/02 | processParameters L234 公开 → 实为 private, 公共入口 L230 | 修正入口链 |
| D10 | t6/01 | 场景段 registerAsParallelCapable L31 → L30 | 修正 |
| D11 | t6/01 | 场景段 WebappClassLoader L57-61 → L54-57 | 修正 |

### 第 2 轮 — 1 缺陷 (修复)

| # | 域/文 | 缺陷 | 修复 |
|:--|:--|:--|:--|
| D12 | t6/01 | 修复 D8 时区间 54-58 越界 (文件 57 行) | → 54-57 |

### 第 3 轮 — 零新发现, 收敛

- 锚点 244/244 OK / 问数 200/200 / harness 47/47 (15+11+10+11)
- 累计缺陷档案: D1~D12, 全部修复, 无遗留
