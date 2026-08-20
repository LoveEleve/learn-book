# Pass 2 闭环笔记 SS-6: 当前更适合聚焦 Encrypt 而不是笼统“数据脱敏”

## 初始假设
- 执行计划里的“数据脱敏”可以直接覆盖当前所有敏感数据处理 feature。

## 验证过程
- 当前源码中敏感数据相关 feature 至少分成两块：
  - `features/encrypt`：`EncryptAlgorithm`、`EncryptSQLRewriteContextDecorator`、merge/result decorator、条件/参数/token 重写等完整主线
  - `features/mask`：独立 feature，有自己的 concept 文档与 `MaskMergedResult`
- `encrypt` 是完整的“算法 SPI + SQL rewrite + merge + metadata reviser + rule builder”体系；`mask` 在当前候选代码里只看到结果合并与独立 feature 文档。
- 若在 6 域上限内把这两块笼统合并成“数据脱敏”，会掩盖 encrypt 的主战场深度，也会让 mask 的边界变糊。

## 结论

SS-6 当前应优先聚焦 `features/encrypt` 主线，把标题改成“加密与查询辅助改写”更准确；`mask` 作为旁枝/边界说明保留，而不是把两者硬压成一个模糊的‘数据脱敏’域。