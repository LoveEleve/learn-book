# Pass 2 闭环笔记 Q1: AuthorityRule 的判定逻辑

## 验证过程

- `AuthorityRule` 只有两个核心字段：`strategy`(白名单/黑名单) 和继承自 `AbstractRule` 的 `limitApp`/`resource`。
- `AuthorityRuleChecker.passCheck` 的判定：
  1. origin 为空或 limitApp 为空 → 放行
  2. 用 `limitApp.indexOf(requester)` 粗匹配，再按逗号拆分做精确匹配，避免 `app1` 误匹配 `app10`
  3. 黑名单 + 命中 → 拒绝
  4. 白名单 + 未命中 → 拒绝
  5. 其余 → 放行 (`AuthorityRuleChecker.java:25-52`)
- `AuthorityRuleManager` 的 listener 在加载时校验规则，且**一个资源最多一条 authority 规则**，冗余规则被忽略 (`AuthorityRuleManager.java:96-120`)。

## 结论

Authority 是黑白名单判定：origin 精确匹配 limitApp 列表，黑名单命中即拒、白名单未命中即拒。规则管理上强制“一资源一规则”，避免多条黑白名单语义冲突。