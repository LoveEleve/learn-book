# F-6 模板引擎 — 知识规划 (KP)

> 域级: 🔴 A (定义特征: README "processes annotations into a templatized request") | 模块: RequestTemplate (1119) + template/ (10 文件: Template 351/Expressions 295/UriUtils 230/QueryTemplate 229/HeaderTemplate 196/UriTemplate 77/BodyTemplate 72/Expression 74/Literal 49/TemplateChunk 23) + Request (591)
> 日期: 2026-08-15 | 版本: 13.14-SNAPSHOT

## 一、机制提取 (逐源)

### M1 RequestTemplate 结构 (1119)
- queries LinkedHashMap (L56, 保序) / headers TreeMap CASE_INSENSITIVE (L57, 去重排序) / uriTemplate/bodyTemplate/resolved 标志 (L58-69)
- **模板探测正则** (L55): `(?<!\\)\?` 负向后顾 — 识别非表达式内的 `?`
- from() 拷贝工厂 (L123-146): 共享不可变模板 + 深拷贝 Map
- resolve 后 query "烧进" uri (L199-233) — resolved 副本 queries 清空 (L207)

### M2 resolve 展开流程 (L182-262)
- 副本隔离 from(this) (L187) → uriTemplate.expand (L194) → query 展开并轨 (L199-233, QUERY_STRING_PATTERN 判 ?/&) → header 展开以**字面量**追加 (L238-253, appendHeader(..., true) 防二次展开) → bodyTemplate.expand (L255-257) → resolved=true (L260)
- **request() 强制校验** (L287-292): 未 resolve 抛 IllegalStateException("template has not been resolved.")
- url() = path() + queryLine() + fragment (L543-555; queryLine L1036-1063)
- **fragment 切分** (L464-468): `#` 后剥离; target() 提取 (L500-535)
- 旧 API 差异: requestLine()/build() 已移除 (13.x)

### M3 构建方法族
- method(String) → HttpMethod 枚举 (L302-310); uri() 拒绝绝对路径 (L449-451) + 自动补 `/` (L455-462); target() 必须绝对 (L507-509) + 去尾斜杠 (L510-512)
- appendQuery (L665-685): 空 values 删参 (L667-671); query/header/body 各方法 (L624-923)
- **appendHeader Content-Type 特例** (L820-827): 客户端只能产一种 content type 恒覆盖
- body() 清空 bodyTemplate 防双重处理 (L914-915) + 自动维护 Content-Length (L917-920)
- decodeSlash 重建成 uriTemplate+query 模板 (L339-355); **反转语义**: QueryTemplate 构造时传 !decodeSlash (QueryTemplate.java:137) — 参数叫 decodeSlash 内部存 encodeSlash

### M4 Template 基类 (351)
- templateChunks (Literal ∪ Expression) (L36-42); 展开主流程 (L98-130): Expression → resolveExpression, Literal 原样; 全部未解析返回 null (L124-127)
- **表达式解析** (L132-151): 有值 → expand; **encodeSlash=false 时 %2F→/** (L138-141); 无值且 allowUnresolved → 字面量保留 (L144-148)
- **parse 管线** (L202-330): parseTemplate → parseFragment → ChunkTokenizer 逐字符扫描 `{`/`}`; **嵌套花括号当字面量** (L289-300, 层级计数, 灵感 Apache CXF)
- EncodingOptions: REQUIRED/NOT_REQUIRED (L332-345); ExpansionOptions: ALLOW_UNRESOLVED/REQUIRED (L347-350)

### M5 表达式求值 (Expressions 295 + Expression 74)
- **语法正则** (L57): `^(\{([+#./;?&=,!@|]?)(.+)\})$` — RFC 6570 Level 2/3 运算符前缀; **Feign 偏离: 允许 $ 和正则值修饰符**
- 工厂 create (L76-141): **长度上限 10000** (L89-94, 系统属性 feign.template.expression.maxLength) → 冒号切变量名/正则 (L107-112) → 嵌套 `{` 字面量 (L115-118) → `;` 走 PathStyleExpression (L129-137) → **正则非法降级字面量** (L138-140, 防 header-map 动态值炸)
- 求值 (L179-209): Iterable/Map/Optional 分派; **展开结果必须匹配 :regex 否则抛** (L204-207)
- 集合展开 (L211-239): 分隔符 `,`; Map 展开 k=v (L241-261); PathStyle `{;name}` (L271-294)

### M6 四模板策略矩阵
- **UriTemplate**: REQUIRED + REQUIRED 编码 (L74-76) — 未解析消失 + 全 pct-encode
- **QueryTemplate**: 名 ALLOW_UNRESOLVED+REQUIRED 编码 / 值 REQUIRED+REQUIRED (L132-150); 展开后**逗号拆解** (L188-215, L206-208); queryString 走 collectionFormat.join (L217-228)
- **HeaderTemplate**: NOT_REQUIRED 不编码仅替换 (L126-137); 未解析忽略 (L168-171); **stripCrlf 头注入防御** (L193-195)
- **BodyTemplate**: ALLOW_UNRESOLVED+NOT_REQUIRED (L55-60); **JSON %7B/%7D 还原** (L62-71)
- 语义: uri/query 全编码、header/body 不编码、body 保留未解析 — 各区域语义差异的根本

### M7 UriUtils 编码 (230)
- **已编码检测 isEncoded** (L37-45): 全 unreserved 或 %XX — 幂等编码前提
- **分段编码** (L111-141): %XX 段跳过, 只编码未编码段 — 防双重编码
- 逐字节 encodeChunk (L150-171); 字符分类对齐 RFC 3986 (L187-229): unreserved ALPHA/DIGIT/-._~ (L219-221)

### M8 CollectionFormat (90)
- CSV/SSV/TSV/PIPES/EXPLODED (L28-39); join (L64-89): EXPLODED 重复键 field=v&field=v (L68-75); 分隔符模式 field=v1,sep,v2 (L76-86); 字段名恒 encode

### M9 Request 不可变对象 (591)
- final 类 + final 字段 (L36, L163-168); HttpMethod 枚举带 withBody (L38-63); 构造防御 checkNotNull (L179-191); 协议固定 HTTP_1_1
- Body 内置 (L517-590): isBinary = 无 charset 或空 (L553-555); **requestTemplate() Experimental 回溯** (L506-509, 日志/重放)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 resolve 全流程 | P1 | 模板→Request 的核心 |
| M5 表达式求值 | P1 | 语法与编码语义 |
| M4 基类解析管线 | P1 | 表达式切分/嵌套处理 |
| M6 四模板策略 | P1 | 区域语义差异 |
| M7 幂等编码 | P1 | 防双重编码 |
| M3 构建方法族 | P2 | 使用面 |
| M8/M9 | P2 | 格式/载体 |

## 三、负面空间

- **不做 RFC 6570 全 Level 支持**: 仅 Simple + PathStyle 两种表达式 (Level 1/2 部分)
- **不做请求重放 API**: requestTemplate 回溯为 Experimental
- **不做模板缓存**: 每次 resolve 全量展开, 无预编译缓存
- **不做二进制模板**: 面向文本 API (README 明示)
- **不做跨请求模板复用**: 每次调用从 metadata 拷贝
