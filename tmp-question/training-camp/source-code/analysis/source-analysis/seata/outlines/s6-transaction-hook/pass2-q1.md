# 闭环笔记 q1: 钩子接口 — 7 方法定义

## 假设
TransactionHook 定义事务生命周期 7 个观察点; afterCompletion 是收尾点。

## 验证过程
- **接口 7 方法** (TransactionHook:19-55): **beforeBegin / afterBegin / beforeCommit / afterCommit / beforeRollback / afterRollback / afterCompletion** — 执行计划列了 beforeBegin→afterBegin→beforeCommit/rollback→afterCompletion 但**漏列 afterCommit/afterRollback** (总数 7 正确)
- **分组**: begin 周期 2 (前/后) + commit 周期 2 + rollback 周期 2 + **afterCompletion 1 (全完成收尾)** — 3×2+1 结构
- **afterCompletion 语义** (S-1 实证): 在 finally 中触发 (L144) — **无论成功失败都触发**; **仅 Launcher** (L381-391)
- **实现类**: 业务自定义实现 (无内置默认) — 观察者模式 (TransactionHookManager 为被观察者集合)

## 代码类型
Data (接口定义)

## 跨域关联
- S-1: 触发面 (TransactionalTemplate:321-391)
- S-9: Spring 集成可注入钩子
- S-13: Saga 复用 (DefaultSagaTransactionalTemplate)

## 结论
7 钩子 = 3 周期×2 + 收尾 1; afterCompletion 全路径触发 (成功/失败); 观察者模式。
源码位置: TransactionHook.java:19-55
