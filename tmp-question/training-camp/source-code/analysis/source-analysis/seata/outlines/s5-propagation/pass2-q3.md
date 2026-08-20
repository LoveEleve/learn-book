# 闭环笔记 q3: 嵌套决策矩阵 — 6×2 场景穷举

## 假设
传播决策 = 传播类型 × 有无现有事务 (6×2 = 12 决策点); 嵌套场景有确定结果。

## 验证过程
- **决策矩阵** (TransactionalTemplate:66-113, S-1 实证):
  | 传播 | 有事务 | 无事务 |
  |:--|:--|:--|
  | REQUIRED | JOIN | CREATE_NEW |
  | REQUIRES_NEW | SUSPEND + CREATE_NEW | CREATE_NEW |
  | NOT_SUPPORTED | SUSPEND + 无事务执行 | 无事务执行 |
  | SUPPORTS | JOIN | 无事务执行 |
  | NEVER | THROW | 无事务执行 |
  | MANDATORY | JOIN | THROW |
- **嵌套场景** (推理):
  - **REQUIRES_NEW 内 REQUIRES_NEW**: 两段挂起 (栈式 holder) → 内层完成后恢复外层 — 独立事务链
  - **NOT_SUPPORTED 内 MANDATORY**: 挂起后无事务 → **MANDATORY 抛** (无现有事务) — 嵌套传播的"环境清空"效应
  - **NEVER 内 REQUIRED**: 无事务环境 → REQUIRED 新建 — 传播在挂起后重新决策
- **挂起恢复闭环** (L147-152): 外层 finally resume(suspendedResourcesHolder) — **每层挂起必恢复** (栈式)
- **SuspendedResourcesHolder 可嵌套**: 多级挂起各自持有 xid

## 代码类型
Architecture (决策矩阵)

## 跨域关联
- S-1: 消费面 (本矩阵即 TransactionalTemplate switch)
- S-9: 注解 propagation 参数 → 决策入口

## 结论
决策 = 6×2 矩阵; 嵌套 = 栈式挂起恢复 + 环境清空效应 (NOT_SUPPORTED 内 MANDATORY 抛)。
源码位置: TransactionalTemplate.java:66-113,147-152
