# 闭环笔记 Q2 — Uri: gRPC 自研 RFC 3986 解析器

假设: gRPC 不用 java.net.URI 而自研 Uri (1184 行) — 因为 target 格式 (dns:///host:port?param=value) 需要严格校验与查询参数 API。

验证过程:
- **组件存储** (api/Uri.java:162-191): "Components are stored percent-encoded, just as originally parsed for transparent parse/toString round-tripping" (L163-164)
- **严格校验** (L185-190): `hasAuthority() && !path.isEmpty() && !path.startsWith("/") → IllegalArgumentException "Has authority -- Non-empty path must start with '/'"`; 无 authority 时 `path.startsWith("//") → "No authority -- Path cannot start with '//'"` — 构造器统一检查
- **parse** (L197-202): IllegalArgumentException → URISyntaxException 包装 ("if 's' is not a valid RFC 3986 URI")
- **create 手写解析** (L208-240): **3.1 Scheme** — "Look for a ':' before '/', '?', or '#'" (L214-229, 找不到 → "Missing required scheme."); **3.2 Authority** — "//" 后扫到 '/' '?' '#' (L232-240) — 逐节手写 RFC 3986 文法
- 查询参数: getQueryParameter (gRPC target 的 ?param=value)
- 对照: java.net.URI 的宽松 authority (L119 注释, "RFC 3986 expects every authority to look like...")

代码类型: Algorithmic (文法解析)

结论: gRPC target 解析是**自研严格解析器**: 格式 `scheme://authority/path?query`, 组件 percent-encoded 存储 (round-trip 无损); 校验规则在构造器统一强制; 手写 scheme/authority 切分按 RFC 3986 3.1/3.2 节。**被放弃的方案: java.net.URI** — 其宽松 authority 解析 (接受任意字符串) 与 gRPC target 严格性冲突; 且缺查询参数 API。 [跨域: G-3 ManagedChannelImplBuilder target → NameResolver] [标准: RFC 3986] (Uri.java:162-191,197-240)
