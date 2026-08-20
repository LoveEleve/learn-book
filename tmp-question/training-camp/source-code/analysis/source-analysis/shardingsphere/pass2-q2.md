# Pass 2 闭环笔记 SS-2: SQL 改写必须分成抽象层与 feature decorator

## 初始假设
- SQL 改写就是 `ShardingSQLRewriteContextDecorator` 这一个类。

## 验证过程
- `infra/rewrite` 提供通用抽象 `SQLRewriteContextDecorator`，这是所有 feature 改写的统一扩展点。
- 在 feature 层，至少存在：
  - `ShardingSQLRewriteContextDecorator`
  - `EncryptSQLRewriteContextDecorator`
- 这说明改写不是分片专属能力，而是“通用 rewrite 框架 + feature-specific decorator”的两层结构。
- 如果只写 `ShardingSQLRewriteContextDecorator`，会误导读者以为 rewrite 机制生于分片 feature 本身，而看不到 infra 抽象层。

## 结论

SS-2 不能只讲 sharding decorator，而应明确拆成两层：
1. `infra/rewrite` 的通用 `SQLRewriteContextDecorator` 抽象
2. `features/sharding` 的 `ShardingSQLRewriteContextDecorator` 具体实现

这也是后续把 encrypt/mask 等改写插件纳入同一框架的前提。