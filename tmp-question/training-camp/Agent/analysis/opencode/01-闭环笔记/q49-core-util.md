# q49 — core util(深度版:token 估算/wildcard/ID 工具)

> 域:共享工具 | 文件:core/src/util/(token 5/hash 11/slug 74/wildcard 14/glob 34/flock 358(q17 已读)/identifier/binary/array/encode/error/retry/path/which/lazy/module/iife)
> review 轮次:2 轮(源码全文)

---

## 假设

util 是纯函数工具集。核心三个:Token(压缩预算估算)、Wildcard(权限匹配引擎)、Slug(会话名生成)。其中 Wildcard 是权限/策略/工具过滤的共享实现(q7/q18)。

## 验证

### 1. Token(设计 1:4 字符/令牌)

```ts
// token.ts:CHARS_PER_TOKEN = 4;estimate = max(0, round(len/4))
// 用途:压缩预算估算(q6)——JSON.stringify 后按字符数估,非 provider tokenizer
// 设计要点:快速近似(预算决策不需要精确);所有决策用同一估算函数(一致性)
```

### 2. Wildcard(设计 2:权限匹配)

```ts
// wildcard.ts:match(input, pattern):
规范化:\ → /(跨平台)
模式转义:特殊字符转义 + * → .* + ? → .
结尾 " .*" → "( .*)?"(尾随星号可选)
正则 ^pattern$(win32 时 si 标志)
// 用途:evaluate/permission/Policy 的 findLast 匹配(q7/q18)
// 注意:"*" 匹配含空串(.*)——"x/*" 匹配 "x/" 吗?.* 匹配空 → 是
```

### 3. Slug(设计 3:形容词-名词)

```ts
// slug.ts:30 形容词 + 31 名词随机组合("brave-cabin")——会话默认名
// create():随机二选一组合
```

### 4. 其他(设计 4:快扫)

```ts
// hash:fast(sha1 截断类)/sha256——项目 ID(remote 归一化哈希)/快照路径
// glob:scan/scanSync/scanSync(match: minimatch)——目录扫描(工具/技能发现)
// identifier:ID 生成(ascending 递增)
// binary/array/encode:二进制/数组/编码工具
// retry(42):重试调度;path:路径工具;which:可执行查找;lazy:惰性加载(q40 watcher)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Token 4 字符/令牌(预算估算) | token.ts | ②压缩预算(统一估算) |
| 2 | Wildcard 匹配引擎(跨平台/转义) | wildcard.ts | ②权限/策略匹配 |
| 3 | Slug 会话名生成 | slug.ts | ④标识 |
| 4 | Hash(sha1 快/sha256)+ Glob | hash.ts + glob.ts | ②工具 |

## 面试弹药

- "预算估算统一函数":所有压缩决策用同一 Token.estimate——一致性比精确更重要
- "Wildcard 尾随星号可选":结尾 " .*" 特判——"dir/*" 也匹配 "dir"(用户体验细节)
- "slug 随机命名":会话可读名("brave-cabin")——用户可识别

## 待深挖

- [ ] identifier 的 ascending 实现(事件 ID 顺序)
- [ ] retry.ts 的调度策略
- [ ] binary/encode 的用途
