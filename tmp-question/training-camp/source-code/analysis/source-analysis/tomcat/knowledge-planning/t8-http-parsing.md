# T-8 HTTP 报文解析 — 知识规划 (09 审计新增域)

> 项目: Tomcat 10.1.x | 类型: Tomcat 自身设计 (HTTP/1.1 RFC 7230 实现) | 🟡B 域 / 2 篇大纲
> 核心文件: HttpParser 1049 + MimeHeaders + Parameters + Rfc6265CookieProcessor 292 | ~2200 行
> 基线: T-2 Connector — Http11Processor 解析报文的输入面; T-8 回答 "HTTP 字节流如何被安全、严格地解析成结构化报文"
> 09 审计: util/http 78 文件 ≥50 定量预检 → 设计决策承载 (字节分类表/严格解析/过滤) → 新增域

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 5 个 (HttpParser/MimeHeaders/Parameters/Rfc6265CookieProcessor/HeaderUtil)
- 5 ≤ 10 → 不触发拆分阈值 ✅

### 2. 淘汰清单
- parser/ 子包内 18 个 HTTP 头部专用解析器 (AcceptEncoding/AcceptLanguage/Host/Authorization 等) → 归入 T-8 §2 概述, 不逐一展开 (均为同模式: 结构化头解析)
- fileupload/ 子包 → 排除 (multipart 上传, 独立库 commons-fileupload, 设计决策承载低)
- ServerCookie/ServerCookies → 归 T-8 §2 (cookie 解析的字节层容器)

### 3. 规范缺口
T-8 需要映射的规范:
- RFC 7230 (HTTP/1.1 消息语法) → HttpParser 字节分类表 + 报文解析
- RFC 7231 (请求行/状态行) → HttpParser.parseRequestLine
- RFC 6265 (Cookie) → Rfc6265CookieProcessor
- RFC 3986 (URI 百分比编码) → Parameters.urlDecode + RequestUtil

### 4. 禁止过度加域
- Http11Processor 本体 (解析器的消费方) 属 T-2, 不展开 ✅
- ByteChunk 缓冲层属 T-4 支撑, 不展开 ✅

### 项目类型判定
| 维度 | 值 |
|---|---|
| 类型 | 协议解析器 (规范实现 + 自身设计决策) |
| 面试频度 | 中 (Cookie/参数编码/请求走私防护) |
| 生产 | 高 (严格解析 = 请求走私防线) |
| Hub | 中 (T-2/T-9 消费) |
| 方案 | 🟡 B (Pass 0-2, Pass 3 可选) |

---

## §一 逐源提取 (High 置信度, 源码实证)

| Source | Inferred Knowledge Point | Confidence |
|---|---|---|
| HttpParser.java:38-49 | 12 个 static boolean[] 字节分类表 (IS_CONTROL/IS_SEPARATOR/IS_TOKEN/IS_HEX/IS_HTTP_PROTOCOL/IS_ALPHA/IS_NUMERIC/IS_SCHEME/IS_UNRESERVED/IS_SUBDELIM/IS_USERINFO/IS_RELAXABLE) + 3 个实例级 relaxed 表 (IS_NOT_REQUEST_TARGET/IS_ABSOLUTEPATH_RELAXED/IS_QUERY_RELAXED, L123-125) — 128 槽查表替代逐字节 if | High |
| HttpParser.java:51-80 | 静态块初始化: 0-31/127=CONTROL; 22 个分隔符=SEPARATOR; 非控制非分隔=TOKEN; 0-9a-fA-F=HEX; "HTTP/" DIGIT "." DIGIT 例外=非协议 | High |
| HttpParser.java:128-163 | 实例构造 (relaxedPathChars/relaxedQueryChars) + isNotRequestTargetRelaxed — 每个请求实例化, 支持 RFC 7230 §5.3 宽松字符集 | High |
| HttpParser.java:196+ | unquote() — 带引号值解码 | High |
| MimeHeaders.java:103-133 | 数组+count 实现 (非 Map) — 头部保序 + 重复头支持 + recycle/clear 池化 | High |
| MimeHeaders.java:171-186 | filter(Set<String>) — 白名单过滤 (HTTP/2 头帧过滤用) + duplicate() 深拷贝 | High |
| MimeHeaders.java:200-232 | size()/findHeader — 线性扫描查找 (头数少, 线性优于哈希) | High |
| Parameters.java:37-234 | 参数解析: processParameters(bytes, start, len, charset) — 字节级解析避免 String 中间分配 | High |
| Parameters.java:374-381 | urlDecode 逐项解码 name/value — 保留原始字节顺序 | High |
| Parameters.java:465-481 | urlDecode(ByteChunk) + MessageBytes 重载 — 双入口 (字节/消息) | High |
| Rfc6265CookieProcessor.java:37-68 | RFC 6265 cookie 解析器 — 继承 CookieProcessorBase, parseCookieHeader(MimeHeaders, ServerCookies) | High |
| CookieProcessorBase | 抽象基类 — 自定义 CookieProcessor 的 SPI 点 (server.xml CookieProcessor 元素) | High |

---

## §二 深度分类 (🔴🟡🟢)

| 知识点 | 级别 | 理由 |
|:--|:--:|:--|
| 字节分类表查表设计 | 🔴 | T-8 核心设计决策 — 128 槽 boolean[] 替代逐字节 if 链, 严格/宽松双模式 |
| 严格解析 vs 容忍解析 | 🔴 | 请求走私防线 — 拒绝非 token 字符/错误版本号, 与 Nginx 宽容对比 |
| MimeHeaders 数组+count 复用 | 🟡 | 池化 + 保序 + 重复头, 与 Map 实现的取舍 |
| filter() 白名单 | 🟡 | HTTP/2 头过滤面, 头帧安全 |
| Parameters 字节级解析 | 🟡 | 免中间 String 分配, 编码处理 |
| cookie 双实现 | 🟡 | RFC6265 vs 旧版 (legacy), 兼容策略 |

---

## §三 聚类 (机制边界 + 依赖图 + 教学顺序)

### 机制边界
1. 字符分类基础设施 (HttpParser 静态表) — 一切解析的原子判断
2. 报文结构解析 (请求行/头/参数/cookie) — Http11Processor 消费面
3. 严格性策略 (relaxed 配置) — 请求走私防护

### 依赖图
```
HttpParser (分类表) ← MimeHeaders/Parameters (结构容器) ← Rfc6265CookieProcessor (cookie) 
                                    ↑ 消费
                          Http11Processor (T-2) / WsFrameBase (T-9)
```

### 教学顺序 (2 篇)
1. **T-8 §1 字节分类基础设施 + 严格解析**: HttpParser 查表设计 → 请求行/头的严格校验 → relaxed 配置 → 请求走私防护 (生产价值)
2. **T-8 §2 结构容器: MimeHeaders/Parameters/Cookie**: 头部数组实现 → 参数字节解析 → cookie 双实现 → 与 T-2 Processor 的衔接

---

## §四 大纲规划 (2 篇)

### T-8 §1: 字节分类基础设施与严格解析 (🟡B, 2 KP)
- 场景: 恶意请求 `GET /a/b%2f../c` — Tomcat 为什么拒绝而 Nginx 接受? 字节分类表的设计
- 机制 1: 13 表查表设计 (static boolean[128], 静态块初始化) — 为什么查表? 热路径性能
- 机制 2: 严格解析策略 (token 校验/版本号例外/relaxed 配置) — 为什么严格? 请求走私 (RFC 7230 攻击面)
- 引出: 分类器给了原子判断, 但报文结构 (头/参数) 怎么存?

### T-8 §2: 结构容器 — MimeHeaders/Parameters/Cookie (🟡B, 2 KP)
- 场景: 一个 8KB 的 header — Tomcat 怎么存? 为什么不是 HashMap?
- 机制 1: MimeHeaders 数组+count (保序/重复头/池化/filter 白名单) — 为什么不用 Map? 头数少线性扫描
- 机制 2: Parameters 字节级解析 (urlDecode 双入口) — 为什么字节级? 免中间 String
- 机制 3: cookie 双实现 (RFC6265 vs legacy) — 兼容策略
- 引出: HTTP 报文解析完了 → 上层 WebSocket (T-9) 的帧协议怎么复用这个基础设施?

---

## §五 跨域引用

| 域 | 关系 |
|:--|:--|
| T-2 Connector | Http11Processor 消费 MimeHeaders/Parameters (解析输入面) |
| T-4 线程模型 | ByteChunk 缓冲支撑 (解析的字节来源) |
| T-9 WebSocket | WsFrameBase 复用 HeaderUtil/MimeHeaders 握手解析 |
| ALI (Spring Cloud Alibaba) | 无直接依赖, 对照: 网关请求解析的严格性对照 |
| openjdk 对照 | HotSpot 的 UTF-8 解码器同样是查表设计 (utf8-json-decoder) — 同类查表模式 |

---

## §六 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

### A. 机制理解 (5)
1. 字节分类表为什么用 boolean[128] 查表而不是逐字节 if-else 链?
2. 严格解析与容忍解析的边界在哪? 为什么版本号校验有 "HTTP/" 例外?
3. relaxedPathChars/relaxedQueryChars 是全局的还是每请求的? 谁在构造 HttpParser 实例?
4. MimeHeaders 为什么用数组+count 而不是 Map? 查找复杂度是多少?
5. filter(Set<String>) 白名单在哪触发? 为什么需要它?

### B. 源码实证 (6)
6. HttpParser 有几个静态分类表? 各是什么? (grep boolean\[\])
7. IS_TOKEN 的判定条件? (grep 静态块)
8. MimeHeaders.recycle() 做了什么? (grep L125)
9. Parameters.processParameters 的字节级入口签名? (grep L230)
10. Rfc6265CookieProcessor 的 parseCookieHeader 签名? (grep L68)
11. urlDecode 的双入口 (ByteChunk/MessageBytes)? (grep L465/472)

### C. 推理深挖 (5)
12. 请求走私攻击怎么利用解析差异? Tomcat 的严格解析怎么封堵?
13. cookie 双实现 (RFC6265 vs legacy) 的切换机制? 为什么保留 legacy?
14. MimeHeaders 的 filter 在 HTTP/2 下怎么用? 头帧与 HTTP/1.1 头的差异?
15. 参数解析为什么在字节层做? 编码错误怎么处理 (decode 失败)?
16. 头部大小限制在哪实施? (MimeHeaders.setLimit 与 maxHttpHeaderSize 的关系)

### D. 跨域扩展 (4)
17. 本域 vs T-2 Http11Processor: 解析器的调用边界在哪?
18. 本域 vs T-9 WebSocket 握手: 复用哪些解析基础设施?
19. 本域 vs openjdk utf8 解码查表: 同构对照?
20. 本域 vs 网关 (Spring Cloud Gateway) 的请求解析: 严格性策略对比?

---

## §七 淘汰与排除

| 项 | 理由 |
|:--|:--|
| parser/ 18 个头部专用解析器 | 同模式 (结构化头解析), §2 概述即可 |
| fileupload/ | 独立库 commons-fileupload, 低承载 |
| FastHttpDateFormat/ConcurrentDateFormat | 日期工具, 低承载 |