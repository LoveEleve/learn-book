# Pass 2 闭环笔记 Q3: RuleManager 的简单/正则规则与缓存

## 验证过程

- `RuleManager<R>` 维护四张 map：
  - `originalRules`：原始规则（按 resource 分组）
  - `regexRules`：正则规则（key 是编译后的 `Pattern`）
  - `regexCacheRules`：正则匹配结果缓存（key 是具体 resource）
  - `simpleRules`：简单规则（key 是 resource 名） (`RuleManager.java:31-35`)
- `updateRules` 把每条规则按 `predicate`（是否 `AbstractRule.isRegex()`）分成简单/正则两类，正则规则编译成 `Pattern` 存入 `regexRules`，简单规则存入 `simpleRules` (`RuleManager.java:48-75`)。
- `getRules(resource)` 先取简单规则，再查正则缓存；缓存未命中时在锁内做正则匹配并写入 `regexCacheRules`，避免每次请求都跑正则 (`RuleManager.java:80-105`)。
- `setRules` 在规则更新时重建正则缓存，降低发布规则时的性能损耗 (`RuleManager.java:145-160`)。

## 结论

`RuleManager` 是统一规则容器，核心优化是“正则规则缓存”：简单规则直接按 resource 查，正则规则先编译成 Pattern，再按具体 resource 缓存匹配结果，避免每次请求都做正则匹配。这是规则发布与查询性能的关键设计。