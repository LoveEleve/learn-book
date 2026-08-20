# 闭环笔记 q3: 触发编排 — 7 trigger 的时机与守卫

## 假设
钩子触发嵌在模板各阶段; 守卫规则 (Launcher/Participant) 精确。

## 验证过程
- **触发点 7 处** (TransactionalTemplate:223-315,321-391):
  - beginTransaction: triggerBeforeBegin → tx.begin → triggerAfterBegin (L313-315)
  - commitTransaction: triggerBeforeCommit → tx.commit → triggerAfterCommit (L223-252)
  - rollbackTransaction: triggerBeforeRollback → tx.rollback → triggerAfterRollback (L268-270)
  - finally: **triggerAfterCompletion** (L144)
- **守卫差异** (S-1 实证):
  - begin/commit/rollback 的 trigger **无守卫** — 但外层方法本身 Launcher 才执行 (beginTransaction L306-311)
  - **afterCompletion 显式守卫**: **仅 Launcher** (L381-391) — Participant 的 finally 不触发收尾钩子
  - 注释 "Of course, the hooks will still be triggered" (L124) — Participant join 也走触发
- **异常语义** (L322-328): 每个钩子 catch(Exception) → LOGGER.error → **继续下一个** — 钩子失败不破坏主流程
- **cleanUp** (L393-402): **Launcher 才 TransactionHookManager.clear** (L399-401) — Participant 不清 (共享外层钩子)

## 代码类型
Implementation (触发编排)

## 跨域关联
- S-1: 模板生命周期 (本域嵌入点)
- S-5: 传播挂起 (钩子与挂起顺序 — 挂起后 begin 前钩子?)

## 结论
触发 = 模板 7 点嵌入 + Launcher 守卫 (afterCompletion/cleanUp) + 异常不中断。
源码位置: TransactionalTemplate.java:223-315,321-402
