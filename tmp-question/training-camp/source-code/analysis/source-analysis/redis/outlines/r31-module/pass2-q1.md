# 闭环笔记 q1: API 面与版本化 — 361 个 RM_ 函数 + typemethods 版本字段

## 假设
模块 API = 直接链接 extern 符号; 版本兼容靠 API 结构版本字段。

## 验证过程
- **API 数量穷举** (grep 实证): module.c 中 **361 个 RM_ 函数定义** (620 处 RM_ 引用含注释/声明)
- **API 契约头**: redismodule.h (模块编译用头) — 361 个声明 + 常量/结构
- **版本兼容策略**:
  - 模块声明 REDISMODULE_APIVER_1 (RedisModule_Init 第 4 参)
  - **typemethods 版本字段** (RM_CreateDataType L6938-6969): 结构首字段 version, 按版本启用字段 — v1 (rdb_load/save, aof_rewrite, mem_usage, digest, free) → v2 (aux_load/save+triggers) → v3 (free_effort/unlink/copy/defrag) → v4 (mem_usage2/free_effort2/unlink2/copy2) → v5 (aux_save2)
  - 事件枚举扩展 (REDISMODULE_EVENT_* 45 处引用)
- **ctx 生命周期** (moduleCreateContext/moduleFreeContext L804): RedisModuleCtx 包装 (module/客户端/标志), 命令/回调间创建释放
- **AutoMemory**: RM_AutoMemory — 上下文内存自动释放 (模块内存所有权辅助)

## 代码类型
Interface (API 面)

## 跨域关联
- 全部域 (模块是 Redis 的"用户态扩展层")

## 结论
API 面 = 361 个直接链接函数 + redismodule.h 契约; 兼容 = APIVER 常量 + typemethods 版本字段 (v1-v5 增量) + 事件枚举扩展; ctx 是模块与核心的交互载体。
源码位置: module.c:6931-6990,804-864; redismodule.h
