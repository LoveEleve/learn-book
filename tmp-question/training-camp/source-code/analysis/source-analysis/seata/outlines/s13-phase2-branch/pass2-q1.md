# 闭环笔记 q1: 处理器族 — 5 实现 + SPI 注册

## 假设
RM 按分支类型多态处理; 处理器经 SPI 注册。

## 验证过程
- **处理器族 5 实现** (AbstractRMHandler 子类): **RMHandlerAT** (rm-datasource) / **RMHandlerXA** / **RMHandlerTCC** (tcc) / **RMHandlerSaga + RMHandlerSagaAnnotation** (saga) — 执行计划未提 SagaAnnotation
- **SPI 注册** (DefaultRMHandler:49-54): **EnhancedServiceLoader.loadAll(AbstractRMHandler)** → allRMHandlersMap.put(getBranchType)
- **多态分发** (L66-82): handle(BranchCommitRequest/RollbackRequest/UndoLogDeleteRequest) → **getRMHandler(branchType)** — 按分支类型路由
- **getResourceManager** (RMHandlerAT:122-123): DefaultResourceManager.getResourceManager(BranchType.AT) — 资源管理器多态
- **MDC**: xid/branchId 日志上下文

## 代码类型
Architecture (处理器族)

## 跨域关联
- S-1: getCore 多态 (TC 侧对称面)
- S-4: DataSourceManager (资源管理器)
- S-13: 分发器 (本域核心)

## 结论
处理器族 = 5 实现 SPI 注册 + DefaultRMHandler 多态分发 + 资源管理器多态。
源码位置: DefaultRMHandler.java:40-82; RMHandlerAT.java:39-128
