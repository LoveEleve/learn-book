# 闭环笔记 q4: 边界面 — 守卫/异常/兜底

## 假设
路由边界: 守卫直通 / 未知类型兜底 / 异常统一包装。

## 验证过程
- **守卫面** (ExecuteTemplate:69-73): 非 AT 且无 GlobalLock → **statementCallback.execute(targetStatement)** — 直通零开销 (无识别/无路由)
- **兜底面** (L84-88,159-163): sqlRecognizers 空 → PlainExecutor; 路由 default → PlainExecutor — 未识别 SQL 原样执行 (无镜像)
- **异常包装** (L170-176): executor 抛非 SQLException → SQLException — 统一接口契约
- **NotSupportYet**: INSERT_ON_DUPLICATE_UPDATE/UPDATE_JOIN 非 3 方言 → 显式不支持 (L137-139,153-155)
- **Multi 面** (L166-168): 多识别器 (批量语句) → MultiExecutor (sqlserver 变体)
- **GlobalLock 场景**: requireGlobalLock → 走路由 (SelectForUpdate 锁查询面)

## 代码类型
Implementation (边界面)

## 跨域关联
- S-12: GlobalLock 场景路由
- S-4: StatementCallback 契约

## 结论
边界 = 守卫直通 (非 AT) + Plain 兜底 (未识别) + SQLException 统一包装 + 方言不支持显式抛。
源码位置: ExecuteTemplate.java:69-73,84-88,137-176
