# HANDOFF — Tomcat 源码分析交接文档 (10/10 全量收官)

> **日期**: 2026-08-17 | **版本**: Apache Tomcat 10.1.34 (git tag acf7da1 实证, 浅克隆) | 模块: java/org/apache/ = **1523 主源** (catalina 618 + tomcat 570 + coyote 94 + jasper 115 + el 80 + juli 12 + naming 34)
> **给新 AI**: 本文是 Tomcat 阶段的**唯一入口**。**10 域全量交付** (v5 遗留 7 域 + 09 审计新增 3 域; 20 篇大纲 + 20 问/域 ×10 = 200 问 + **4 个 harness 47/47** + 时空溯源 + 深审档案)。
> **源码**: `/data/workspace/source-code/code/spring/tomcat` (Apache Tomcat 10.1.34, git tag acf7da1, 浅克隆无历史 → 时空溯源用 changelog.xml)
> **跨仓库锚点**: T-7 引用 spring-boot 仓库 `TomcatServletWebServerFactory.java` (1032 行, 锚点已验)
> **规划**: TOMCAT-PLAN.md (09 审计 + 任务清单)
> **分工确认**: Tomcat 无人占用, 本会话开工 10/10 收官 ✅

---

## §零 状态速查 (2026-08-17, 10/10 收官)

| 域 | 模块 | 级别 | 方案 | 大纲节 | questions | harness | 时空溯源 | 深审 |
|:--:|:--|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| T-1 容器+Lifecycle | catalina/core + util | 🔴 | A | 4 | 20 | **15/15** | ✅ temporal-trace | ✅ |
| T-2 Connector+Adapter | coyote + catalina/connector | 🔴 | A | 4 | 20 | **11/11** | ✅ | ✅ |
| T-3 Pipeline+Filter 双链 | catalina/core | 🔴 | A | 4 | 20 | **10/10** | ✅ | ✅ |
| T-4 线程模型 | tomcat/util/net | 🔴 | A | 2 | 20 | **11/11** | ✅ (Poller 可靠性演化) | ✅ |
| T-5 Mapper 路由 | catalina/mapper | 🟡 | B | 2 | 20 | — | — | ✅ |
| T-6 ClassLoader | catalina/loader | 🟡 | B | 1 | 20 | — | — | ✅ |
| T-7 Spring Boot 集成 | spring-boot (跨仓库) | 🟡 | B | 2 | 20 | — | — | ✅ |
| **T-8 HTTP 报文解析** 🆕 | tomcat/util/http | 🟡 | B | 2 | 20 | — | — | ✅ (1 缺陷已修) |
| **T-9 WebSocket 实现** 🆕 | tomcat/websocket | 🟡 | B | 2 | 20 | — | — | ✅ |
| **T-10 集群通信+复制** 🆕 | catalina/tribes + ha | 🟡 | B | 2 | 20 | — | — | ✅ |

**统计**: **10 域全交付** (v5 遗留 7 + 09 审计新增 3) · 20 篇大纲 · **200 问 (10×20, 全 A/B/C/D 四节格式)** · **4 个 harness 47/47 断言全 PASS** · 锚点回归 **241/241 全验** · 深审缺陷 2 (已修复) · 时空溯源 1 份

**执行序**: T-1 → T-2 → T-3 → T-4 → T-5 → T-6 → T-7 → T-8 (依赖 T-2) → T-9 (依赖 T-8/T-2) → T-10 (依赖 T-1)

---

## §一 10 域核心知识速查 (全量固化)

### T-1 容器+Lifecycle (🔴, catalina/core)

**核心机制**: 11 态状态机 + Template Method + 递归级联
- **状态机** (LifecycleState.java:23): 11 态 (NEW→INITIALIZING→INITIALIZED→STARTING_PREP→STARTING→STARTED→STOPPING_PREP→STOPPING→STOPPED→DESTROYING→DESTROYED) + FAILED/MUST_STOP
- **模板方法** (LifecycleBase.java:139/193): `final synchronized start()` 驱动 `abstract startInternal()` — 状态转换集中管控
- **异常语义**: startInternal 抛异常 → 状态 FAILED (不回滚 INITIALIZED); child.start() 异常经 addChild 上抛 (harness 实证)
- **children 存储** (ContainerBase.java:154): HashMap + ReadWriteLock (非 CHM) — 写锁内两步原子 (注册+parent 引用), child.start() 在写锁外
- **await()** (StandardServer.java:508): port=-1 模式不用 ServerSocket, 用锁等待退出

### T-2 Connector+Adapter (🔴, coyote + catalina/connector)

**核心机制**: 双层 Request/Response + Processor 池化 + keep-alive 复用
- **双层设计**: coyote (协议) vs catalina (servlet) — ADAPTER_NOTES (CoyoteAdapter.java:75) 承载转换
- **池化** (AbstractProtocol.java:786): recycledProcessors 双端队列, processorCache=200 (L166), 满则弃
- **keep-alive 释放** (AbstractProtocol.java:974-979): OPEN 状态 → release(processor) → registerReadInterest 等下一请求
- **互斥** (Response.java:501/523): getOutputStream/getWriter 二选一, 否则 ISE
- **recycle()** (Request.java:431): 逐字段清空 (43 行), 池化复用不 new

### T-3 Pipeline+Filter 双链 (🔴, catalina/core)

**核心机制**: Valve 链 (容器级) + Filter 链 (应用级) 双 CoR
- **单向链** (Valve.java:50/58): getNext/setNext 显式传递
- **契约 15 条** (Valve.java:64-118): MAY/MUST NOT 清单 (短路/不消费流/返回后不改头)
- **4 阀级联** (StandardEngineValve:56 / StandardHostValve:80 / StandardContextValve:60 / StandardWrapperValve:86): 逐级下探 getPipeline().getFirst().invoke()
- **Filter 链** (ApplicationFilterChain.java:46/117): pos 游标循环 (非递归) — servlet 恰好一次
- **终止语义**: filter 不调 doFilter → 链终止 (harness 实证)

### T-4 线程模型 (🔴, tomcat/util/net)

**核心机制**: Acceptor/Poller/Worker 三线程模型
- **连接限流** (AbstractEndpoint.java:548/1477): maxConnections = 8*1024, countUpOrAwaitConnection 信号量式阻塞
- **Poller** (NioEndpoint.java:595): wakeupCounter CAS (L605, L630) — 只在 counter==0 才 selector.wakeup(); select 循环 getAndSet(-1) 判定 selectNow/select (L750-757)
- **事件复用** (NioEndpoint.java:635): createPollerEvent 从池取 (L659/731)
- **keep-alive**: 同一 socket 多次 registerReadInterest → N 请求 (harness 实证)
- **时空**: 7.0 引入 NIO 后模型冻结; 10.1.x 仅可靠性修正 (Poller 通知丢失 10.1.11, 重复注册 M17)

### T-5 Mapper 路由 (🟡, catalina/mapper)

**核心机制**: 数组+二分 vs HashMap + 事件驱动路由表
- **三数组** (Mapper.java:449-452): exact/wildcard/extensionWrappers
- **匹配规则** (Mapper.java:871-890): 精确 > 前缀 > 扩展, 顺序判定
- **findContextVersion** (L372): hostName→contextPath→version 三级
- **事件构建** (MapperListener.java:147-178): ADD_CHILD_EVENT / ADD_MAPPING_EVENT 区分
- **线程安全**: synchronized addHost (L101)

### T-6 ClassLoader (🟡, catalina/loader)

**核心机制**: 本地优先 + 双亲委派打破 + 安全过滤
- **delegate** (WebappClassLoaderBase.java:281): 默认 false — 本地优先, 父仅兜底
- **jakarta 过滤** (L2481-2497): jakarta.* 永不从应用加载 — 规范类保护
- **缓存** (L807/919): notFoundClassResources + resourceEntries — 负缓存

### T-7 Spring Boot 集成 (🟡, spring-boot 跨仓库)

**核心机制**: 工厂编程式装配 vs server.xml
- **getWebServer** (TomcatServletWebServerFactory.java:196): 一行创建 Tomcat 实例
- **configureContext** (L403): TomcatStarter 绕过 SCI @HandlesTypes 扫描
- **Customizer 三层** (L687/742): properties → Customizer → subclass
- **锚点全部实证** (L196/208/269/403/687/742, 文件 1032 行)

### T-8 HTTP 报文解析 (🟡 新增, tomcat/util/http)

**核心机制**: 字节分类查表 + 严格解析 + 结构容器
- **12 静态 + 3 实例查表** (HttpParser.java:38-49 + L123-125): boolean[128] 替代逐字节 if — O(1) 单次内存读
- **严格默认 + 显式放松** (L128-163): relaxedPathChars/QueryChars 配置才宽松 — 请求走私防线
- **MimeHeaders 数组+count** (L103/112/125/171/186/200/232): 保序/重复头/池化/filter 白名单 — 非 Map
- **Parameters 字节级** (L234/374-381/465/472): 免中间 String 分配
- **Cookie 双实现** (Rfc6265CookieProcessor.java:37/68): RFC6265 vs legacy 策略模式
- **深审修正 1**: 分类表 13→12 静态 + 3 实例 (凭记忆写数字, 实证纠正)

### T-9 WebSocket 实现 (🟡 新增, tomcat/websocket)

**核心机制**: 位运算帧解析 + 半包状态机 + 会话管理
- **帧头位运算** (WsFrameBase.java:141-160): fin(b&0x80)/rsv(b&0x70>>>4)/opCode(b&0x0F) 一字节三字段
- **控制帧严格校验** (L155-165): fin=1 + 白名单 + 125 上限 (L55-56 预分配)
- **半包状态机** (L111/226): 不足 return false, 实例字段记忆断点 — 非阻塞增量解析
- **会话双语义 close** (WsSession.java:537-553): 本地/远端 1006 区分 — doClose 双参数
- **容器双角色** (WsWebSocketContainer.java:79/1062-1064): Container + BackgroundProcess — 10 秒周期批量扫描会话超时
- **发送写锁** (WsRemoteEndpointImplBase 1288 行): 连接级互斥 — 帧原子性

### T-10 集群通信+复制 (🟡 新增, catalina/tribes + ha)

**核心机制**: 组播成员管理 + 拦截器链 + 脏复制
- **门面三组件** (GroupChannel.java:67/513-543): Receiver/Sender/MembershipService 可插拔 — 组播发现 + TCP 收发
- **组播心跳** (McastServiceImpl.java:59/85/151-176/222/268-284): 双线程 + mcastSoTimeout=sendFrequency + expireTime + 2 倍启动等待
- **拦截器链** (GroupChannel.java:159/183): 洋葱结构 — 心跳/分片/统计正交切面
- **脏复制三条件** (AbstractReplicatedMap.java:441-460): complete || isDirty || isAccessReplicate, 前置 isPrimary && backupNodes
- **事件协议** (DeltaManager.java:92-105): EVT_SESSION_CREATED/DELTA/ACCESSED/EXPIRED + 双向计数器
- **集成** (SimpleTcpCluster.java:66/129): LifecycleMBeanBase + managers Map (context→ClusterManager)

---

## §二 09 审计结论 (TOMCAT-PLAN.md 全表)

- **新增 3 域** (设计决策测试): T-8 (util/http 78 文件, HttpParser 1049 行) / T-9 (websocket 77 文件, WsFrameBase 982) / T-10 (tribes 112 + ha 38, AbstractReplicatedMap 1728)
- **排除及理由**: dbcp (阶段 3 已分析连接池) / jasper+el (JSP 生态淘汰) / session (StandardManager 422 行低承载, T-5 旧审计已砍) / juli+naming+ant+authenticator+filters+manager+mbeans+realm+ssi+storeconfig+users+util+webresources+bcel+descriptor+digester+modeler+openssl+scan (规范跟随/工具/边缘)
- **数字断言全验**: T-1 8 文件 11581 行 ✅ / T-2 7 文件 7766≈7700 ✅ / maxConnections 8*1024 ✅ / Lifecycle 11 态 ✅

---

## §三 深审档案 (deep-review.md)

- 缺陷清单 #1~#15 逐域检查 → **2 缺陷, 均已修复**
- D1: T-8 分类表数量错误 (13→12 静态+3 实例) — KP+大纲双修
- D2: TOMCAT-PLAN 附录 HttpParser 路径错 (parser/ 子目录)
- 与 Nacos 阶段对比: 写作时 re-grep 先行纪律生效 — 无编造类名/伪锚点

---

## §四 时空溯源 (temporal-trace.md)

- **数据源**: changelog.xml (10.1.x 全量 993 条) — 浅克隆无 git 历史
- **T-4**: 三线程模型 7.0 定型; 10.1.x 仅可靠性修正 (Poller 通知丢失 10.1.11 / 重复注册 M17 / threadsMaxIdleTime 10.1.20)
- **T-1**: 状态机 5.5 定型冻结; 10.1.34 演化在监听器生态 (AprLifecycleListener 引用计数)
- **T-2**: coyote/catalina 双层三代连续 (8.5→10.1); 10.1.x 只修协议边缘
- **T-3**: 双链 CoR 自 3.0 定型, 10.1.x 仅 Valve 细节修正

---

## §五 文件索引

| 文件 | 说明 |
|:--|:--|
| TOMCAT-PLAN.md | 09 审计 + 任务清单 (8/8 全勾) |
| knowledge-planning/t1~t10-*.md | 10 域 KP (逐源提取/深度分类/聚类/大纲规划/20 问/淘汰) |
| outlines/t1~t10-*/NN-*.md | 20 篇大纲 (五要素) |
| outlines/t*-*/completeness-questions.md | 10 域 20 问 (A 机制 5/B 实证 6/C 推理 5/D 跨域 4) |
| harness/t1-lifecycle/MiniLifecycle.java | 15 断言 |
| harness/t2-connector/MiniProcessor.java | 11 断言 |
| harness/t3-pipeline/MiniPipeline.java | 10 断言 |
| harness/t4-threadmodel/MiniThreadModel.java | 11 断言 |
| temporal-trace.md | 🔴 域时空溯源 |
| deep-review.md | 缺陷档案 #1~#15 |

---

## §六 深度 REVIEW 档案 (N 轮收敛)

**方法**: 对标 Nacos 六轮 REVIEW 格式 — R1 全量语义锚点核验 → R2 编造类名/方法名/变量名 → R3 文字锚 → R4 数字与问数 → R5 前置/引出引用 → R6 拼写/术语 → harness 回归 → 收敛判定 (连续一轮零新发现)。

**第 1 轮**: 241 锚点逐条语义核验 (T-1~T-10 全域) + 49 类名/208 方法/49 常量存在性 + 22 文字锚 + 数字断言 + 136 跨域互引 → **9 缺陷, 全部修复**:
| # | 位置 | 缺陷 | 修复 |
|:--|:--|:--|:--|
| 1 | t1/01 §3 | ContainerBase.startInternal 锚点错 (780-845 实为 stopInternal) + 顺序颠倒 (实为 Cluster→Realm→children 并行) | 修正 729-793 + 顺序 + 数据流 |
| 2 | t1/02 §3 | pipeline 字段锚点错 (L75-77 实为 Javadoc) | 修正 L217 new StandardPipeline |
| 3 | t3/01 §1 | Valve invoke 契约 15 条 → 实为 10 条 (MAY 5 + MUST NOT 5) | 修正数量 + 区间 71-113 |
| 4 | t3/02 §4 | StandardContextValve 编造 dispatcherType/WebSocket 检查; 漏 WEB-INF/META-INF 拦截 + sendAcknowledgement | 修正为真实流程 |
| 5 | t3/03 §1 | release() 声称 filters 数组不清 → 实际循环清空 | 修正 259-276 |
| 6 | t6/01 §3 | WebappClassLoader getClassLoadingLock 锚点 38-40 错 (实为 copyWithoutTransformers) | 修正 54-57 |
| 7 | t8/02 §2 | processParameters 签名/行号错 (L234 是 private 4 参, 公共入口 L230) | 修正入口链 |
| 8 | t6/01 §3 | registerAsParallelCapable 场景段 L31 → 实为 L30 | 修正 |
| 9 | t6/01 §3 | WebappClassLoader 引区 L57-61 → 实为 L54-57 | 修正 (含场景段残留) |

**第 2 轮回归**: 9 修复点逐一复核 + harness 47/47 + 锚点范围复验 → **发现 1 缺陷** (WebappClassLoader L54-58 区间越界, 文件 57 行) → 修正 L54-57。
**第 3 轮复验**: 锚点 **244/244 OK** / 问数 **200/200** / harness **47/47** → **零新发现, 收敛达成**。

**与深审 (D1/D2) 累计**: 缺陷档案共 11 条 (深审 2 + REVIEW 9 + 越界 1), 全部修复。REVIEW 期间核心教训: ① 区间锚点必须闭合文件行数; ② 编造控制流 (dispatcherType/WebSocket 检查) 比编造类名更难自查 — 依赖逐条语义核验; ③ 场景段文字锚与源码路径锚需同步修 (一处修复两处残留)。

---

## §七 遗留与建议

1. **T-5~T-10 🟡 域可选增强**: 方案 B 允许 Pass 3 (harness) — 如需加码可补 (方法论 04 决策树)
2. **8.5/9.0 演化对照**: 本仓库 changelog 仅 10.1.x — 想查更早版本演化需拉全历史或官方 changelog
3. **T-7 跨仓库**: 锚点依赖 spring-boot 仓库文件 — 该文件改动需同步复核
4. **下一步**: 更新 HANDOFF-STAGE5.md (本仓库不在阶段 5 — 属阶段 1.2) + issue/源码分析执行计划.md 1.2 状态