# F-6 temporal-trace — 时空溯源 (CHANGELOG 依据, 浅克隆替代方案)

> 本地 git 浅克隆单提交; 溯源基于 CHANGELOG.md 全版本条目 + 代码内 @Deprecated 痕迹。

## 版本线 (CHANGELOG.md 实证)

| 版本 | 事件 | 证据 |
|---|---|---|
| 9.x 及以前 | 模板简单字符串替换; RequestTemplate 无模板引擎子包 | 代码注释/README 历史 |
| 10.x | **模板引擎重构**: template/ 子包建立 (Template/Expressions/UriTemplate), RequestTemplate 达 1119 行; RFC 6570 部分支持 | RequestTemplate.java:55 注释 + template/ 结构 |
| 10.9 | Configurable to disable streaming mode; overriding query parameter name | CHANGELOG 10.9 条目 |
| 11.x | OkHttpClient implements AsyncClient | CHANGELOG 11.9 |
| 13.12 | UrlencodedFormContentProcessor honors CollectionFormat | CHANGELOG 13.12 |
| 13.14 | HTTP QUERY method 支持 (RFC 10008) | CHANGELOG 13.14 |

## 代码内代际痕迹

- **alreadyEncoded API 废弃** (RequestTemplate.java:271-278): 老版允许参数跳过编码, 13.x 统一幂等编码逻辑 — "编码正确性是模板引擎的地基"
- **requestLine()/build() 移除** (13.x): 产出入口收敛为 request() + url() — API 简化的代际标志
- **collectionFormat 从 @RequestLine 属性到独立枚举** (CollectionFormat.java:28-39): 13.12 起 form 处理器也尊重它

## 设计代际

```
简单字符串替换 (9.x)
  → template/ 子包 + RFC 6570 方言 (10.x, RequestTemplate 1119 行)
  → 幂等编码 + 策略矩阵定型 (10.x~11.x)
  → 集合格式扩展 + QUERY 方法 (13.x)
  → 对照: Retrofit (@Path/@Query 注解直绑) vs Feign (模板表达式)
```

## 教训沉淀 (写作引用)

- "已编码值不二次编码" 是幂等变换的教科书案例 (UriUtils.java:37-45)
- 四位置编码策略矩阵 = 按语义定策略而非一刀切 (Template.java:332-350)
- 嵌套花括号保护 JSON: 模板引擎必须理解"它服务的协议的数据格式" (Template.java:289-300)
