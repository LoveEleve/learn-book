# T-8 §2 结构容器 — MimeHeaders/Parameters/Cookie 的存储设计

> 依赖 T-8 §1 + T-2 | 🟡B | 3 KP

**读者处境**: T-8 §1 解决了"这个字节是什么" — 现在的问题是"解析结果放哪": 一个 8KB 的请求头 — 30 个字段 — Tomcat 怎么存? 为什么不是 HashMap? 查询参数怎么解析才不产生垃圾对象?

### 1. MimeHeaders — 数组+count 而非 Map

场景: HTTP/1.1 允许重复头 (两个 `Cookie:` 头合法) — 如果存 Map, 重复键直接覆盖 — 信息丢失。且请求头查找 (findHeader) 每请求只调几次 — 用哈希不值得。

源码路径: `MimeHeaders.java:103-133`。构造器 `new MimeHeaders()` (L103) 创建 `MimeHeaderField[] headers` 数组 + `int count` 计数; `setLimit(int)` (L112) 限制头数量上限 — **超限收缩数组** (System.arraycopy 到新 limit 数组); `recycle()` (L125) 遍历 count 个字段逐个 recycle 然后 count=0 — **池化复用, 不重建对象**。

关键设计: **数组 + 线性扫描 vs Map — 为什么?** ① 保序: 请求头顺序对代理/调试有意义, 数组天然保序, LinkedHashMap 也行但更重; ② 重复头: 同键多值数组可容纳, Map 需 List<value> 包装; ③ 复用: recycle 后数组原地复用, 避免 Map 扩容; ④ 头数少 (RFC 限制 ~100), 线性扫描 O(n) 的常数极小, HashMap 的哈希开销反而更高。`setLimit` 收缩而非扩容 — 头过多直接拒绝, 防 DoS (头部轰炸)。 [模式: Object Pool + Array over Map]

数据流: `Http11Processor` 解析头部 → `MimeHeaders.setLimit(maxHttpHeaderSize)` → `mimeHeaders.addValue(name)` 逐字段追加 → 请求处理完 `recycle()` → 数组复用给 keep-alive 下一请求。

### 2. Parameters — 字节级解析免中间 String

场景: `?name=张三&age=25` — URL 编码的参数 — 如果先 `new String(bytes)` 再 split — 每个参数产生 2 个 String 对象 — 一次带 100 参数的请求就是 200 个垃圾对象。且编码错误 (非法 %xx) 在 String 层无法逐字节定位。

源码路径: `Parameters.java:37-234`。`processParameters(byte[] bytes, int start, int len)` (L230 公共入口) — 委托私有 `processParameters(byte[], int, int, Charset)` (L234) — **直接在字节数组上解析**: 定位 `&` 分隔 → 定位 `=` → 逐段 `urlDecode` (L374-381 name, L381 value); 双入口: `processParameters(MessageBytes, Charset)` (L472) 从消息字节重载, `urlDecode(ByteChunk)` (L465) 字节级解码。

关键设计: **字节级解析 — 为什么?** ① 零中间分配: 不创建中间 String, 只在最终落库/取值时才按需转换 (lazy); ② 精确错误处理: %zz 非法序列在字节层精确拦截, String 层已丢失原始字节; ③ 编码灵活: charset 参数化, 同一字节流可解析为不同编码 — 与 `Connector URIEncoding` 配置衔接。 [模式: Zero-Copy Parsing]

数据流: `coyote.Request.getParameters()` → `Parameters.processParameters(MessageBytes queryString, charset)` → 字节级扫 `&`/`=` → `urlDecode` 逐段 → `getParameter(name)` 返回 String (此时才解码) → 请求结束 `recycle()` 清空。

### 3. Cookie 解析 — RFC 6265 双实现

场景: `Cookie: SID=31d4d96e407aad42; lang=en-US` — cookie 的解析规则曾经混乱 — 旧版 Tomcat 用 RFC 2109 规则, 新版标准是 RFC 6265。两种客户端共存 — 兼容怎么办?

源码路径: `Rfc6265CookieProcessor.java:37-68`。`parseCookieHeader(MimeHeaders headers, ServerCookies serverCookies)` (L68) — 遍历 MimeHeaders 的 Cookie 头 → 按 RFC 6265 规则解析到 ServerCookies 容器; 继承 `CookieProcessorBase` (抽象基类, SPI 点) — `server.xml` 的 `<CookieProcessor>` 元素可替换实现。

关键设计: **接口抽象 + 双实现 — 为什么?** 默认 RFC 6265 (现代标准), legacy 实现保留给老客户端 — 切换点通过 CookieProcessor 抽象隔离, 容器代码 (StandardContext) 只依赖接口。这与 MimeHeaders 的"数组复用"不同 — 这是**策略模式的兼容面**: 协议标准演进时代码演进, 容器不感知。 [模式: Strategy + SPI]

数据流: `server.xml <Context><CookieProcessor className=...>` → StandardContext 持有 CookieProcessor 实例 → Http11Processor 解析到 Cookie 头 → `cookieProcessor.parseCookieHeader(mimeHeaders, serverCookies)` → `request.getCookies()` 消费。

→ 引出 T-9 WebSocket — HTTP 报文解析 (请求行/头/参数/cookie) 是 WebSocket 握手的输入面 — 升级请求 (Upgrade: websocket) 也是这些容器解析的 — 握手成功后字节流切换成帧协议 — T-9 的 WsFrameBase 怎么解析帧?