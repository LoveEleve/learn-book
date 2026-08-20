# R-31 module — Pass 1 探索笔记

> 域: R-31 模块系统 | 🟡 B 方案 | 2026-08-14
> 源码: src/module.c (14002 巨型单文件) + tests/modules/ (40 个 .c 测试模块) | Redis 7.4.2

## 调用图

```
加载面:
moduleInitModulesSystem (module.c:11999) → moduleLoadFromQueue (L12109) — dlopen 模块队列
  → RedisModule_Init (模块入口) → RM_... 注册 API (创建类型/命令/订阅事件)
命令分派:
模块命令 → processCommand → moduleCommandGenericCommand (模块命令执行器, 走完整命令链)
类型系统:
RM_CreateDataType (L6931): 9 字符名×6bit + 10bit encver → 64 位类型 ID (moduleTypeEncodeId L6654)
  + typemethods v1-v5 版本化方法表 (rdb_load/save, aof_rewrite, mem_usage, digest, free, aux, unlink, copy, defrag)
  → RDB MODULE_2 序列化 (R-8 交叉) + AOF rewrite + defrag (R-18 moduleDefragValue)
执行面:
RM_Call (L6304): fmt 参数 → 临时客户端 → call() 全链 (DENY_BLOCKING/RUN_AS_USER/RESP3/in_call)
RM_BlockClient / RM_BlockClientOnKeys (helloblock.c/blockonkeys.c 测试): 阻塞 API + moduleHandleBlockedClients (L8311)
线程面:
RM_GetThreadSafeContext (L8522) + RM_ThreadSafeContextLock/Unlock (L8587): 后台线程 → 主线程临界区
事件面:
RM_SubscribeToServerEvent (L11663) → moduleFireServerEvent (L11761): REDISMODULE_EVENT_* 系列
命令过滤:
moduleCallCommandFilters (L6367): RM_RegisterCommandFilter — 命令执行前改写/拦截
与核心边界 (钩子):
moduleNotifyKeyspaceEvent (R-29) / moduleDefragValue+moduleLateDefrag (R-18) / moduleFreeContext (L804)
moduleCallCommandFilters (server.c:6367 调用) / moduleFireServerEvent (server.c:1529 cron)
统计: modules dict + RedisModule struct (版本/加载标志)
```

## 基本元素分解

1. **API 面**: **361 个 RM_ 函数** (grep 实证) — 模块 .so 直接链接 extern 符号
2. **类型系统**: 64 位类型 ID + 版本化方法表 + RDB/AOF/defrag 全序列化契约
3. **命令注册**: RM_CreateCommand (strflags → redisCommand, 键位置参数)
4. **执行面**: moduleCommandGenericCommand + RM_Call (临时客户端)
5. **阻塞与线程**: RM_BlockClient 家族 + ThreadSafeContext 锁
6. **事件与钩子**: 订阅事件 + 命令过滤 + keyspace 通知 (R-29)

## 标记问题 (20 问)

1. 361 个 API 怎么版本化? (REDISMODULE_APIVER / typemethods 版本字段)
2. 类型 ID 怎么编码? (9 字符×6bit + 10bit encver)
3. typemethods v1-v5 各加了什么?
4. RM_CreateCommand 的 strflags 怎么解析? (写/只读/管理/ACL 分类)
5. 模块命令怎么走 processCommand? (moduleCommandGenericCommand)
6. RM_Call 的临时客户端? (DENY_BLOCKING/RUN_AS_USER)
7. 后台线程安全怎么保证? (ThreadSafeContext + 全局锁)
8. RM_BlockClient 怎么与 R-26 阻塞框架交互?
9. 事件订阅面? (REDISMODULE_EVENT_* 系列)
10. 命令过滤 (CommandFilter) 机制?
11. RDB 序列化怎么调模块回调? (MODULE_2)
12. AOF rewrite 怎么调模块? (aof_rewrite)
13. 模块键的内存记账? (mem_usage)
14. 模块与 ACL 交互? (RM_CreateModuleUser/ACLCheckCommandPermissions)
15. 模块加载/卸载生命周期? (moduleInitModulesSystem)
16. 模块错误处理? (RM_ReplyWithError/err)
17. 模块调试面? (RM_Log/INFO 集成)
18. 模块与复制的交互? (RM_Replicate 或自动)
19. 模块 keyspace 通知 (R-29 交叉)?
20. defrag 三入口 (R-18 交叉)?

## 时空溯源 (代码内痕迹)

- module.c 版权 2020-Present (RSAL 时代); 模块系统 **4.0 引入** (2018, RedisModule API)
- 演进: typemethods 版本字段 (v2 aux → v3 free_effort/unlink/copy/defrag → v4 mem_usage2 → v5 aux_save2) — **API 向后兼容按版本字段**
- 事件系统: REDISMODULE_EVENT_* 枚举扩展
- 线程安全: RM_ThreadSafeContext (4.0 初版) → 7.x 优化 (锁粒度)
- 测试面: tests/modules/ 40 个 .c (datatype/blockonkeys/defragtest/hooks/commandfilter/propagate 等)

## 大域拆分判断

14002 行巨型单文件 — 按 01 方法论属"大域" (10000-30000); 🟡 B 方案 6 闭环 — **单篇大纲** (每节覆盖一个大机制面); HANDOFF 标注"独立扩展系统, 面试低频"

## 域级怀疑审计 (HANDOFF §四 R-31 简案 断言复查)

| HANDOFF 断言 | 验证 | 结论 |
|:--|:--|:--|
| "**RedisModule_* API 家族** (类型/命令/线程/定时器)" | 361 个 RM_ 函数 (grep 穷举); 定时器 RM_CreateTimer (hellotimer.c) | **接受** ✅ (数字穷举: 361) |
| "**module 类型系统** (ModuleType, RDB 序列化 MODULE_2)" | RM_CreateDataType + moduleTypeEncodeId (9×6bit+10bit=64 位) + typemethods v1-v5 | **接受** ✅ |
| "命令注册 (createCommand)" | RM_CreateCommand (L1253) — 实际函数名 RM_CreateCommand | **接受** ✅ |
| "**后台线程** (RM_Call 线程安全面)" | RM_GetThreadSafeContext/Lock/Unlock (L8522-8587) + RM_Call (L6304) | **接受** ✅ |
| "与核心的边界 (module.c 全局钩子)" | moduleCallCommandFilters/moduleNotifyKeyspaceEvent/moduleFireServerEvent/moduleDefragValue | **接受** ✅ |
| "标注: 独立扩展系统, 面试低频, 可最后或按需简略" | 用户 2026-08-14 确认**完整版** | **接受** ✅ (用户覆盖) |
