# F-6 模板引擎 — 一张请求的"乐高图纸": 从 {var} 到完整 HTTP 请求

> 前置: 无 (叶子域) | 引出: [[F-2-契约解析]] (注解产出模板) + [[F-3-代理与调用链]] (模板执行) | 对照: Retrofit @Path/@Query + RFC 6570
> 🔴 A | 9 KP | [模式: 模板 + 表达式 + 编码策略]
> Pass 2 闭环: q1(模板结构) q2(resolve) q3(表达式) q4(编码) q5(策略矩阵)

**读者处境**: 接口里写 `@RequestLine("GET /users/{id}")`, 调用时 `id` 怎么变成 URL 里的值? 中文参数会不会乱码? header 里的 `{token}` 和 URL 里的 `{id}` 展开规则一样吗?

### 1. 模板结构 — 一张请求的"乐高图纸"

场景: 请求模板里到底存了什么?
源码路径:
- queries LinkedHashMap 保序 (RequestTemplate.java:56) / headers TreeMap **大小写不敏感** (L57) / uriTemplate + bodyTemplate + resolved 标志 (L58-69)
- **模板探测正则** (L55): `(?<!\\)\?` — 识别"不在 {…} 表达式内"的问号, 用于从 uri 切出 query string
- from() 拷贝工厂 (L123-146): 共享不可变模板, 深拷贝两个 Map
关键设计 (q1): **模板是"未展开的图纸", 每次调用从图纸拷贝再填充** — 原模板永不修改; headers 用大小写不敏感 TreeMap 天然去重排序。 [模式: 原型拷贝]

### 2. resolve — 一次调用一次展开

场景: resolve() 做了什么? 展开后还能再展开吗?
源码路径:
- **副本隔离** from(this) (L187) → uriTemplate.expand (L194) → query 展开后**烧进 uri 字符串** (L199-233, 已有 query 追加 `&` 否则 `?`) → header 以**字面量**追加 (L238-253, 防二次展开) → bodyTemplate.expand (L255-257) → resolved=true (L260)
- **request() 强制校验** (L287-292): 未 resolve 抛 "template has not been resolved."
- url() = path() + queryLine() + fragment (L543-555)
关键设计 (q2): **展开即固化** — resolve 后 query 不再是独立结构而是 uri 的一部分; resolved 标志防二次展开; 副本隔离保证多线程并发调用安全。 [模式: 一次性展开]

### 3. 表达式解析 — {var} 的切分艺术

场景: `{id}` 和 `{id:regex}` 和嵌套的 `{a{b}}` 分别怎么解析?
源码路径:
- **parse 管线** (Template.java:202-330): parseTemplate → parseFragment → ChunkTokenizer 逐字符扫描 `{`/`}`
- **嵌套花括号当字面量** (L289-300, 层级计数, 灵感 Apache CXF): `foo{bar{baz}}` → `foo` + `{bar{baz}}` 整块
- chunks = Literal ∪ Expression (L36-42); 展开遍历 chunks (L98-130)
关键设计 (q3): **切块而非逐字符替换** — 解析一次切出块列表, 展开时逐块处理; 嵌套花括号保护 JSON body 不被误拆。 [模式: 分词器]

### 4. 表达式求值 — RFC 6570 的 Feign 方言

场景: 表达式支持哪些语法? `;name` 是什么?
源码路径:
- **语法正则** (Expressions.java:57): `^(\{([+#./;?&=,!@|]?)(.+)\})$` — RFC 6570 Level 2/3 运算符前缀; **Feign 偏离 RFC: 允许 `$` 和正则值修饰符**
- 工厂 create (L76-141): **长度上限 10000** (L89-94) → 冒号切变量名/正则 (L107-112) → `;` 走 PathStyleExpression (L129-137) → **正则非法降级字面量** (L138-140, 防 header-map 动态值带 `{`/`:` 炸)
- 求值 (L179-209): Iterable/Map/Optional 分派; **展开结果必须匹配 :regex 否则抛** (L204-207)
关键设计 (q3): **防御式求值** — 长度上限防 DoS, 正则非法降级防 header-map 动态值炸掉解析; `;name` 是 path-style 参数 (分号分隔)。 [模式: 方言子集]

### 5. 编码策略矩阵 — uri 全编码, header 不编码

场景: 同一个 {var}, 在 URL/query/header/body 里编码规则一样吗?
源码路径:
- **UriTemplate**: REQUIRED + REQUIRED 编码 (UriTemplate.java:74-76) — 未解析消失 + 全 pct-encode
- **反转语义**: decodeSlash 参数在 QueryTemplate 构造时反着传 (!decodeSlash → encodeSlash, QueryTemplate.java:137)
- **QueryTemplate**: 名 ALLOW_UNRESOLVED + 值 REQUIRED 编码 (QueryTemplate.java:132-150)
- **HeaderTemplate**: NOT_REQUIRED **不编码仅替换** (HeaderTemplate.java:126-137); 未解析忽略 (L168-171); **stripCrlf 头注入防御** (L193-195)
- **BodyTemplate**: ALLOW_UNRESOLVED 保留未解析 (BodyTemplate.java:55-60); **JSON %7B/%7D 还原** (L62-71)
关键设计 (q4): **区域语义差异是刻意设计** — URL 必须编码 (空格→%20), header 值编码会破坏语义 (Authorization 里的 = /), body 里的 { 是 JSON 的一部分要还原; 四套策略矩阵 = 四类位置的正确性。 [模式: 策略矩阵]

### 6. 幂等编码 — 已编码的值不再二次编码

场景: 参数值是 "a%20b", 展开时会变成 "a%2520b" 吗?
源码路径:
- **isEncoded 检测** (UriUtils.java:37-45): 全部字符 unreserved 或含 %XX → 已编码
- **分段编码** (L111-141): %XX 段跳过, 只编码未编码段
- 字符分类对齐 RFC 3986 (L187-229): unreserved = ALPHA/DIGIT/-._~
关键设计 (q4): **幂等编码防双重编码灾难** — 已经 pct-encode 的段原样保留; 这是模板引擎正确性的地基 (老版 alreadyEncoded API 已废弃, 统一幂等逻辑, L271-278)。 [模式: 幂等变换]

### 7. CollectionFormat — 集合参数的五种展开

场景: `List<String>` 参数怎么展开? CSV 还是重复键?
源码路径:
- **CSV/SSV/TSV/PIPES/EXPLODED** (CollectionFormat.java:28-39); 默认 EXPLODED
- join (L64-89): **EXPLODED = field=v&field=v 重复键** (L68-75, OpenAPI 风格); 分隔符模式 = field=v1,sep,v2 (L76-86)
- QueryTemplate 展开后**逗号拆解** (QueryTemplate.java:206-208): 表达式结果含逗号按逗号拆成独立值再交给 collectionFormat
关键设计 (q2): **EXPLODED 是 OpenAPI 的默认** — 重复参数名 vs 分隔符是两种 API 风格; 逗号拆解让 @Param 展开与集合格式协同。 [模式: 格式策略]

### 8. fragment — # 之后的世界

场景: uri 里带 #fragment, 怎么处理?
源码路径:
- uri() 中 `#` 后剥离 (RequestTemplate.java:464-468); target() 提取 (L527-529); url() 末尾拼回 (L550-552)
关键设计 (q1): **fragment 不参与服务端请求, 但拼回 url 保完整性** — HTTP 客户端通常忽略 fragment, 但保留语义 (浏览器重定向等场景)。 [模式: 隔离保留]

### 9. Request — 展开的终点, 不可变的新生

场景: 模板展开完, 变成什么?
源码路径:
- **final 类 + final 字段** (Request.java:36, L163-168); HttpMethod 枚举带 withBody 标志 (L38-63); 构造防御 checkNotNull (L179-191)
- Body 内置 (L517-590): isBinary = 无 charset 或空 (L553-555); **requestTemplate() Experimental 回溯** (L506-509)
关键设计 (q5): **不可变 = 可安全共享/重放** — Request 一旦生成不可改; 保留模板引用供日志/重放/调试。 [模式: 不可变值对象]

## 代码类型
Architecture (模板引擎)

## 负面空间 — Feign 模板引擎刻意不做的事

- **不做 RFC 6570 全 Level**: 仅 Simple + PathStyle 两种 (Level 1/2 部分, 对照 full 实现库)
- **不做模板预编译缓存**: 每次 resolve 全量展开 (性能换简单)
- **不做请求重放正式 API**: requestTemplate 回溯 Experimental
- **不做二进制 body 模板**: 文本 API 定位 (README 明示)
- **不做跨线程模板共享优化**: 每次调用 from() 拷贝

→ 引出: 注解怎么变成模板? → F-2 契约解析
