# 闭环笔记 q6: 加载面与核心边界 — moduleInitModulesSystem + 与核心的耦合点

## 假设
加载 = 配置队列 → dlopen; 与核心边界 = 全局数据结构 + 钩子调用点。

## 验证过程
- **系统初始化** (moduleInitModulesSystem L11999-12060): 全局结构 — modules dict / loadmodule_queue / module_pipe (线程唤醒管道) / Timers rax / EventListeners / CommandFilters / KeyspaceSubscribers / PostExecUnitJobs / 模块 GIL 标志
- **加载队列** (moduleLoadFromQueue L12109): server.loadmodule_queue (config "loadmodule" 解析) → moduleLoad (dlopen + RedisModule_Init 入口) — **加载失败 exit(1)** (模块是信任扩展)
- **moduleLoad**: dlopen → RedisModule_Init(ctx, name, ver, APIVER) → 模块 onload 回调注册 (类型/命令/事件/过滤)
- **与核心边界 (hook 调用点)**: 
  - moduleCallCommandFilters (processCommand 前, server.c)
  - moduleFireServerEvent (cron/server.c:1529 + loading + key 事件)
  - moduleNotifyKeyspaceEvent (R-29)
  - moduleDefragValue/moduleLateDefrag (R-18)
  - moduleHandleBlockedClients (事件循环, R-2)
  - modulePostExecUnitJobs (R-30 afterCommand 链)
  - **module_handle_pipe**: 线程唤醒管道 → 事件循环 (后台线程通知主线程)
- **卸载**: moduleFreeModuleStructure (L12150) — 类型/过滤/依赖关系 (usedby/using 模块依赖图) 释放
- **模块依赖**: RedisModule_AddModuleDependency / using/usedby 列表 — 模块间依赖管理

## 代码类型
Interface (加载面 + 边界)

## 跨域关联
- R-2 (events): 线程管道/阻塞处理
- R-20 (server): 加载管线
- R-29 (pubsub): keyspace 通知旁路

## 结论
加载 = 信任扩展 dlopen (失败即退出); 与核心边界 = 8 大钩子点 (命令过滤/事件/keyspace/defrag/阻塞/作业队列/管道) — 模块系统是 Redis 各域的"旁路扩展层", 每个钩子都在已讲域里见过。
源码位置: module.c:11999-12060,12109-12150
