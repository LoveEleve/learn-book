# F-5 URI 模板引擎 — 知识规划 (KP)
> **ℹ 8-16 深化版 (备查)**: 本 KP 为同会话 8-16 深化产物, 与权威版 (f6-template/f3-proxy/f1-builder) 重复。内容已并入权威版对应域, 本文件保留作增强参考。

> 域: F-5 | 级别: 🟡 | 方案: B | 大纲: outlines/f5-uri-template/outline.md (8 节)

## §01 域定位

Feign 模板引擎 = 声明式接口注解 (`@RequestLine("GET /users/{id}")`) 到可发送 HTTP 请求的**中间语言**。四类模板 (URI/Query/Header/Body) 共享解析器, 以不同选项 (REQUIRED/ALLOW + 编码开关) 区分语义。定义特征: RFC6570 变体 + Feign 正则修饰符扩展 + 双重编码防护 + 输入护栏。

## §02 源文件清单 (template/ 10 + RequestTemplate + CollectionFormat + QueryMapEncoder)

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| Template.java | 351 | 基类: 构造五参/expand/resolveExpression/parseFragment/ChunkTokenizer | 1,2,4 |
| UriTemplate.java | 77 | REQUIRED+编码, encodeSlash 可配 | 1 |
| QueryTemplate.java | 229 | 值集合/过滤/纯参/CopyOnWriteArrayList | 1,6 |
| HeaderTemplate.java | 196 | ALLOW+不编码 | 1 |
| BodyTemplate.java | 72 | ALLOW+不编码 | 1 |
| Expressions.java | 295 | 工厂: 正则/操作符/长度限制/SimpleExpression/PathStyleExpression | 3,4,8 |
| Expression.java | 74 | 抽象: name/pattern/matches | 3 |
| Literal.java | 49 | 字面量块 | 2 |
| TemplateChunk.java | 23 | 块接口 | 2 |
| UriUtils.java | 230 | 编码: isEncoded/encodeChunk/encodeInternal/pctEncode | 5 |
| RequestTemplate.java | 1119 | 消费端: resolve/query/header/body/字段面 | 7 |
| CollectionFormat.java | — | 5 值枚举 CSV/SSV/TSV/PIPES/EXPLODED | 6 |

## §05 闭环要点 (Pass 2 内化)

### q1 四类模板选项矩阵
URI = REQUIRED+编码+encodeSlash 可配; Query 名 = ALLOW+编码, 值 = REQUIRED+编码; Header/Body = ALLOW+不编码。根因: 路径缺参=请求错误, 头缺参=合法; URI 需转义, 头/体是"原样发送"。

### q2 求值链
ChunkTokenizer (层级计数) → Expressions.create (工厂, 失败降级 Literal) → Template.expand (三态: 值/缺失 REQUIRED 删/缺失 ALLOW 留) → SimpleExpression 五分支 (Iterable/Map/Optional/带名/裸值) + 模式校验。

### q3 编码面
UriUtils 双保险: isEncoded 短路 (encodeChunk) + PCT 分割重编码 (encodeInternal)。encodeSlash=false → `%2F`→`/` 解码与"保 %xx"形成推拉。reserved 集: generic+sub+额外。

### q4 安全面
表达式长度限制 10000 (系统属性可配/可关), 检查在 stripBraces 后 → 防嵌套绕过; 正则修饰符 ReDoS 缓解。CollectionFormat.EXPLODED null 特例 + join/split 对称。

## §06 负面空间 (6 条)

非完整 RFC6570 / 不服务端渲染 / 不缓存编译结果 / 不国际化协商 / 不递归展开 / 不 query 排序

## §07 交叉引用

- ← 无 (叶子域)
- → F-2 Contract (MethodMetadata 模板工厂, RequestTemplateFactoryResolver 三分支)
- → F-3 Client 执行 (RequestTemplate.resolve 消费 + Retryer 重试重放)
- → F-5 与 F-7 form (FormEncoder 的 ContentProcessor 与模板交互)
- 另见: Spring UriComponentsBuilder / OkHttp HttpUrl / RFC6570
