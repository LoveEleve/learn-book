# Tomcat 源码分析 — 规划 (TOMCAT-PLAN v1, 09 审计产物)

> **日期**: 2026-08-17 | **版本**: Apache Tomcat 10.1.34 (git tag acf7da1, 浅克隆单提交实证)
> **源码**: `/data/workspace/source-code/code/spring/tomcat/` (java/org/apache/: catalina 618 + tomcat 570 + coyote 94 + jasper 115 + el 80 + juli 12 + naming 34 = **1523 主源**)
> **方法论文档**: `talk-method/source-code-analysis/methodology/zh/` (01-09, 09 必读)
> **前置状态**: 既有产出 = 7 域 KP+大纲 (2026-08-09~10, v5 时代), 无 PLAN/HANDOFF/harness/时空溯源, 悬空未收官
> **本 PLAN 任务**: 09 怀疑审计 → 新增 3 域 (7→10) → 补 20 问/域 → 🔴 harness+时空溯源 → 深审 → HANDOFF-TOMCAT.md 收官

---

## §零 09 怀疑审计表 (2026-08-17)

### 1. 域清单完整性 (四类怀疑对象 #1)

**顶层包扫描 (java/org/apache/)**:

| 包 | 主源文件 | 既有覆盖 | 判定 |
|:--|:--:|:--|:--|
| catalina/ | 618 | T-1~T-3/T-5 | ✅ 覆盖 |
| tomcat/ (util 354 + websocket 77 + dbcp 106 + buildutil 12 + jni 10) | 570 | T-4/T-6 | ⚠️ 部分覆盖 |
| coyote/ | 94 | T-2/T-4 | ✅ 覆盖 |
| jasper/ | 115 | — | ❌ 排除 (JSP 编译器, 生态淘汰, 设计决策低) |
| el/ | 80 | — | ❌ 排除 (JSP EL, 生态淘汰) |
| juli/ | 12 | — | ❌ 排除 (日志 SPI, 无设计决策) |
| naming/ | 34 | — | ❌ 排除 (JNDI 实现, 规范跟随) |

**catalina/ 子包 (≥10 文件)**:

| 子包 | 文件 | 既有覆盖 | 判定 |
|:--|:--:|:--|:--|
| core/ | 46 | T-1 | ✅ |
| ant/ | 32 | — | ❌ 排除 (构建工具) |
| authenticator/ | 20 | — | ❌ 排除 (认证, 规范跟随) |
| connector/ | 14 | T-2 | ✅ |
| filters/ | 20 | — | ❌ 排除 (Servlet 过滤器, 规范跟随) |
| ha/ | 38 | — | ⚠️ **新增 T-10 集群会话复制** (DeltaManager 1358 行 — 设计决策承载) |
| manager/ | 12 | — | ❌ 排除 (管理界面) |
| mbeans/ | 20 | — | ❌ 排除 (JMX 工具) |
| realm/ | 20 | — | ❌ 排除 (认证域, 规范跟随) |
| session/ | 11 | — | ❌ 排除 (StandardManager 422 行 — 设计决策承载低, 且 T-5 Session 已按 09 淘汰: Spring Boot 用 spring-session 替代) |
| ssi/ | 23 | — | ❌ 排除 (Server Side Include, 边缘) |
| startup/ | 34 | T-1 (Catalina.start 已覆盖) | ✅ |
| storeconfig/ | 39 | — | ❌ 排除 (server.xml 序列化工具) |
| tribes/ | 112 | — | ⚠️ **新增 T-10 集群通信** (GroupChannel 734 + McastServiceImpl 734 + AbstractReplicatedMap 1728 — 组播成员/心跳/复制设计决策承载) |
| users/ | 15 | — | ❌ 排除 (用户库) |
| util/ | 29 | — | ❌ 排除 (工具) |
| valves/ | 32 | T-3 | ✅ |
| webresources/ | 31 | — | ❌ 排除 (静态资源, 设计决策低) |

**tomcat/util/ 子包 (≥10 文件)**:

| 子包 | 文件 | 既有覆盖 | 判定 |
|:--|:--:|:--|:--|
| bcel/ | 24 | — | ❌ 排除 (字节码工具, 编译期) |
| buf/ | 22 | T-4 (ByteChunk 引用) | ✅ 部分 |
| descriptor/ | 52 | — | ❌ 排除 (部署描述符解析工具) |
| digester/ | 18 | — | ❌ 排除 (XML 解析工具) |
| http/ | 78 | — | ⚠️ **新增 T-8 HTTP 报文解析** (HttpParser 1049 + MimeHeaders + Cookies + Parameters — HTTP/1.1 协议解析设计决策承载) |
| modeler/ | 16 | — | ❌ 排除 (JMX 工具) |
| net/ | 64 | T-4 (NioEndpoint) | ✅ 部分 |
| openssl/ | 10 | — | ❌ 排除 (OpenSSL 绑定, 边缘) |
| scan/ | 10 | — | ❌ 排除 (Jar 扫描工具) |
| threads/ | 11 | T-4 | ✅ |
| **websocket/** | **77** | — | ⚠️ **新增 T-9 WebSocket 协议实现** (WsFrameBase 982 帧协议 + WsSession 1091 + WsWebSocketContainer 1112 — JSR-356 实现设计决策承载) |

### 2. 数字断言验证 (四类怀疑对象 #2)

| 断言 | 验证 | 结论 |
|:--|:--|:--|
| T-1 "8 文件 11581 行" | StandardServer 1031 + Service 597 + Engine 430 + Host 812 + Context 5858 + Wrapper 1340 + ContainerBase 1225 + Lifecycle 288 = **11581** | ✅ 准确 |
| T-2 "7 文件 ~7700 行" | AbstractProtocol 1240 + Http11NioProtocol 76 + Processor 117 + Adapter 100 + CoyoteAdapter 1315 + catalina.Request 3244 + catalina.Response 1674 = **7766** | ✅ 准确 (~) |
| T-4 NioEndpoint 1787 / AbstractEndpoint 1590 / Acceptor 231 / SocketWrapperBase 1521 | wc -l 全对 | ✅ 准确 |
| T-5 Mapper 1655 / MappingData 62 / MapperListener 512 | wc -l 全对 | ✅ 准确 |
| T-6 WebappClassLoaderBase 2660 / ParallelWebappClassLoader 61 | wc -l 全对 | ✅ 准确 |
| T-7 TomcatServletWebServerFactory 1032 | wc -l (Spring Boot 仓库) | ✅ 准确 |
| maxConnections 8192 | AbstractEndpoint.java:548 `8*1024` | ✅ 准确 |
| Lifecycle 11 态 | Lifecycle.java:26-73 Javadoc ASCII 图实证 | ✅ 准确 |

### 3. 依赖方向与执行序 (四类怀疑对象 #3/#4)

**依赖方向实证** (import 证据):

```
T-8 http 解析: tomcat/util/http ← coyote (Http11Processor 消费 MimeHeaders/Parameters)
T-9 websocket: tomcat/websocket ← tomcat/util (WsFrameBase 消费 ByteChunk/HeaderUtil)
T-10 tribes: catalina/tribes ← catalina/ha (SimpleTcpCluster 组合 GroupChannel)
```

**执行序 (拓扑)**: T-1 容器 → T-2 Connector → T-3 Pipeline → T-4 线程 → T-5 Mapper → T-6 ClassLoader → T-7 Boot → T-8 HTTP 解析 (T-2 的报文层, 依赖 T-2 Processor) → T-9 WebSocket (依赖 T-8 Header 解析/T-2) → T-10 集群 (独立, 依赖 T-1 容器)

**注意**: 既有 7 域无 PLAN 执行序记录, 大纲内"依赖"标注: T-1 无 / T-2 依赖 T-1 / T-3 依赖 T-1 / T-4 依赖 T-2 / T-5 依赖 T-3 / T-6 依赖 T-1 / T-7 依赖 T-6 — 拓扑自洽 ✅

### 4. 覆盖率

| 来源 | 域数 | 覆盖率 |
|:--|:--:|:--|
| 执行计划 (issue/源码分析执行计划.md) | 6 | — |
| 既有产出 (v5 时代) | 7 | 116% |
| **09 审计后** | **10** | **167%** (+3: http 解析/WebSocket/集群 — 均为 ≥50 文件设计决策承载包) |

### 5. 既有产出质量抽查结论

- 大纲五要素 (场景/源码路径/关键设计/数据流/引出) 完整, 行号实证 0 越界
- 文字锚缺陷 #7: 裸 L 引用均有上下文类名 ✅ (v5 时代质量反而好)
- **缺口**: 问数不足 (5-15/域, 当前标准 20) · 无 harness · 无时空溯源 · 无深审记录 · 无 HANDOFF

---

## §一 域清单 (10 域, 09 审计后)

| 域 | 目录 | 级别 | 方案 | 大纲篇 | 状态 |
|:--:|:--|:--:|:--:|:--:|:--|
| T-1 容器层次+Lifecycle | outlines/t1-container-lifecycle | 🔴 | A | 4 | ✅ 既有, 补 20 问 |
| T-2 Connector 适配 | outlines/t2-connector-adapter | 🔴 | A | 4 | ✅ 既有, 补 20 问 |
| T-3 Pipeline+Filter 链 | outlines/t3-pipeline-filterchain | 🔴 | A | 4 | ✅ 既有, 补 20 问 |
| T-4 线程模型 | outlines/t4-thread-model | 🔴 | A | 2 | ✅ 既有, 补 20 问 |
| T-5 Mapper 路由 | outlines/t5-mapper-routing | 🟡 | B | 2 | ✅ 既有, 补 20 问 |
| T-6 ClassLoader | outlines/t6-classloader | 🟡 | B | 1 | ✅ 既有, 补 20 问 |
| T-7 Boot 集成 | outlines/t7-springboot-integration | 🟡 | B | 2 | ✅ 既有, 补 20 问 |
| **T-8 HTTP 报文解析** | outlines/t8-http-parsing | 🟡 | B | 2 | 🆕 新建 |
| **T-9 WebSocket 实现** | outlines/t9-websocket | 🟡 | B | 2 | 🆕 新建 |
| **T-10 集群通信+复制** | outlines/t10-cluster | 🟡 | B | 2 | 🆕 新建 |

---

## §二 任务清单 (本 PLAN 收官)

1. [x] 09 怀疑审计 (域清单/数字/依赖/顺序)
2. [x] 新增 3 域: T-8/T-9/T-10 KP + 大纲 + 20 问 (源码实证, 行号 re-grep) — 2026-08-17
3. [x] 既有 7 域: 补 20 问格式 (旧多视角 → 新 A/B/C/D 四节) — 2026-08-17
4. [x] 🔴 域 (T-1~T-4) harness 极简复现 + 运行验证 (47/47 全过) — 2026-08-17
5. [x] 🔴 域时空溯源 (git 浅克隆无历史 → 用 CHANGELOG/RELEASE-NOTES + @since) → temporal-trace.md — 2026-08-17
6. [x] 深审: 缺陷档案 #1~#15 逐域检查 → deep-review.md (2 缺陷已修复) — 2026-08-17
7. [x] 全量回归 (锚点 241/241 + harness 47/47 + 问数 200/200) — 2026-08-17
8. [x] HANDOFF-TOMCAT.md 收官 — 2026-08-17

---

## 附: 新增域源码锚点速查 (写作时仍须 re-grep)

| 锚点 | 位置 |
|:--|:--|
| HttpParser 类声明 (1049 行) | tomcat/util/http/**parser/**HttpParser.java (注意: 在 parser/ 子目录) |
| MimeHeaders 类声明 | tomcat/util/http/MimeHeaders.java |
| Parameters 类声明 | tomcat/util/http/Parameters.java |
| Rfc6265CookieProcessor | tomcat/util/http/Rfc6265CookieProcessor.java |
| WsFrameBase 帧解析 (982 行) | tomcat/websocket/WsFrameBase.java |
| WsWebSocketContainer (1112) | tomcat/websocket/WsWebSocketContainer.java |
| GroupChannel (734) | catalina/tribes/group/GroupChannel.java |
| McastServiceImpl 组播 (734) | catalina/tribes/membership/McastServiceImpl.java |
| AbstractReplicatedMap (1728) | catalina/tribes/tipis/AbstractReplicatedMap.java |
| DeltaManager (1358) | catalina/ha/session/DeltaManager.java |
| SimpleTcpCluster (762) | catalina/ha/tcp/SimpleTcpCluster.java |