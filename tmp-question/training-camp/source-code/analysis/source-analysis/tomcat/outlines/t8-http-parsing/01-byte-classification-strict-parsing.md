# T-8 §1 字节分类基础设施与严格解析 — HttpParser 查表设计

> 依赖 T-2 | 🟡B | 2 KP | 09 审计新增域

**读者处境**: T-2 讲完 Http11Processor 把字节流变成 coyote.Request — 但解析的原子判断 (这个字节是 token 吗? 是分隔符吗?) 没讲。HTTP 报文解析每秒执行数百万次 — 每一字节的判断都要快。Tomcat 的答案: **128 槽查表, 不做逐字节 if-else**。

### 1. 字节分类表 — boolean[128] 查表替代 if 链

场景: `GET /index.html HTTP/1.1` — 解析器要回答: 'G' 是 token 吗? '/' 是分隔符吗? 空格在哪结束? 如果逐字节写 `if (c=='(' || c==')' || c=='<' ...)` — 22 个分隔符要 22 次比较 — 每字节 22 次, 一个 1KB 请求头就是 2 万次比较。

源码路径: `HttpParser.java:38-49` (parser/HttpParser.java)。**12 个 static final boolean[128]**: `IS_CONTROL`/`IS_SEPARATOR`/`IS_TOKEN`/`IS_HEX`/`IS_HTTP_PROTOCOL`/`IS_ALPHA`/`IS_NUMERIC`/`IS_SCHEME`/`IS_UNRESERVED`/`IS_SUBDELIM`/`IS_USERINFO`/`IS_RELAXABLE` + **3 个实例级 relaxed 表** (IS_NOT_REQUEST_TARGET/IS_ABSOLUTEPATH_RELAXED/IS_QUERY_RELAXED, L123-125)。静态块 (L51-80) 一次性初始化: 0-31/127=CONTROL; 22 个分隔符=SEPARATOR; 非控制非分隔=TOKEN; 0-9a-fA-F=HEX; `HTTP/` DIGIT `.` DIGIT 例外 (这些字符虽非 token 但允许出现在协议版本)。

关键设计: **查表 vs 分支链 — 为什么是查表?** 布尔查表是 O(1) 单次内存读, 分支预测友好的连续访问; if-else 链是 O(n) 比较 + 分支预测失败惩罚。RFC 7230 的 token 定义 (控制符/分隔符/非 ASCII 之外全是 token) 恰好能用 128 槽表穷举 — 表是有限枚举的物理实现。静态块初始化保证零运行时开销。 [模式: Lookup Table]

数据流: `HttpParser` 静态块 → 13 表就绪 → `isNotRequestTargetRelaxed(c)` (L163) 查表 → 单次内存读返回 boolean → Http11Processor 解析请求行时逐字节查。

### 2. 严格解析 — 拒绝歧义, 封堵请求走私

场景: `GET /a%2f%2e%2e/b` — 有的服务器把 `%2f` 解码成 `/` — 客户端就能用双重编码绕过路径校验。Tomcat 的立场: **解析器不认识的就是非法的** — 不做智能猜测。

源码路径: `HttpParser.java:128-163` — 实例构造 (L128) 接收 `relaxedPathChars`/`relaxedQueryChars` 两个字符串, 生成实例级 relaxed 覆盖表; `isNotRequestTargetRelaxed` (L163)/`isAbsolutePathRelaxed` (L174)/`isQueryRelaxed` (L185) 三个查询方法 — 默认严格, 配置了才宽松。`unquote` (L196) 解码带引号值。

关键设计: **严格默认 + 显式放松 — 为什么不让解析器宽容?** 请求走私攻击 (RFC 7230 预警) 利用"两个服务器对同一请求解析结果不同" — 前端 Nginx 宽容解码, 后端 Tomcat 严格 — 攻击者构造前端接受/后端拒绝的请求就能走私。默认严格 = 拒绝一切不在表内的字符 — 攻击面最小; relaxed 配置给必须兼容的场景显式开后门 — 每次放宽都是配置可见的、管理员明知的。 [模式: Fail-Closed]

数据流: `server.xml <Connector relaxedQueryChars=...>` → 属性注入 Http11Processor → 构造 `new HttpParser(relaxedPathChars, relaxedQueryChars)` → 请求行/查询串解析查 relaxed 表。

→ 引出 §2 结构容器 — 原子判断解决了"这个字节是什么" — 但解析出来的头/参数存在哪? 为什么 MimeHeaders 不用 HashMap? 为什么参数解析在字节层做?