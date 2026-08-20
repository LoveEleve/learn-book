# 闭环笔记 q4: 消费起点 — computePullFromWhere 5 模式

## 假设
起点模式决定新队列从哪读; 有进度续读优先。

## 验证过程
- **5 模式** (RebalancePushImpl computePullFromWhereWithException L166-220):
  - **LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST / MIN / MAX / LAST** (合并分支): 有进度 (readOffset READ_FROM_STORE ≥ 0) → **续读**; 无进度 (首次) → 重试 topic 从 **0**, 否则 **maxOffset (尾部, 新消息开始)** — 默认语义
  - **FIRST_OFFSET**: 无进度 → 0 (头部; OFFSET_ILLEGAL 会修正)
  - **TIMESTAMP**: 按时间戳查询 (queryOffsetByTimestamp)
- **与进度面闭环**: readOffset (RM-8 三类型) → 起点; 消费推进 → 新进度; 再平衡后新归属者续读
- **冻结面**: OFFSET_ILLEGAL → updateAndFreezeOffset (RM-8 交叉)

## 代码类型
Algorithmic (起点决策)

## 跨域关联
- RM-8 (进度): readOffset/冻结
- RM-3 (存储): maxOffset 查询

## 结论
起点 = 有进度续读 / 无进度 (重试从 0, 普通尾部 maxOffset) / FIRST 从 0 / TIMESTAMP 时间查询; 默认 LAST 语义。
源码位置: RebalancePushImpl.java:166-230
