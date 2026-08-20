# 闭环笔记 q1: 路由表 — 守卫 + 6 分支 + Multi + Plain

## 假设
ExecuteTemplate 是统一路由入口: 守卫 → 识别 → 分支选择 executor。

## 验证过程
- **入口守卫** (ExecuteTemplate:69-73): **!requireGlobalLock() && BranchType.AT != getBranchType() → 直通原 statement** — 非 AT 无 GlobalLock 场景零开销
- **路由表** (L100-168):
  - INSERT → **EnhancedServiceLoader.load(InsertExecutor, dbType)** — 方言 SPI 可插拔
  - UPDATE → **SqlServerUpdateExecutor (sqlserver) / UpdateExecutor** (L108-113)
  - DELETE → SqlServerDeleteExecutor / DeleteExecutor
  - SELECT_FOR_UPDATE → SqlServerSelectForUpdateExecutor / SelectForUpdateExecutor (S-2 行锁面)
  - **INSERT_ON_DUPLICATE_UPDATE → MySQL/Mariadb/PolarDBX 专用** (其他 → **NotSupportYetException**) (L125-141)
  - **UPDATE_JOIN → 同上 3 方言** (L143-158)
  - default → **PlainExecutor** (兜底)
- **多识别器 → MultiExecutor** (L166-168) — 批量 SQL (MULTI_UPDATE/MULTI_DELETE)
- **异常包装** (L170-176): 非 SQLException → SQLException

## 代码类型
Architecture (路由核心)

## 跨域关联
- S-4: StatementProxy 调用 (入口)
- S-2: SelectForUpdate (镜像行锁)
- S-12: requireGlobalLock (GlobalLock 场景)

## 结论
路由 = 守卫 (AT/GlobalLock) + 6 类型分支 + Multi + Plain; INSERT 方言 SPI; 2 类型仅 3 方言支持。
源码位置: ExecuteTemplate.java:56-177
