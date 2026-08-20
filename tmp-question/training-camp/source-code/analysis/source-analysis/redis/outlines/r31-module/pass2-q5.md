# 闭环笔记 q5: 事件与钩子 — 订阅事件 + 命令过滤 + keyspace 通知

## 假设
模块事件 = 订阅-分发 (moduleFireServerEvent); 命令过滤 = 执行前改写链; keyspace 通知旁路。

## 验证过程
- **事件订阅** (RM_SubscribeToServerEvent L11663): RedisModuleEvent (事件 ID + 子事件掩码) + 回调; **moduleFireServerEvent** (L11761) 分发 — 核心各处触发 (cron L1529 / loading progress / key 事件 / module change)
- **事件枚举**: REDISMODULE_EVENT_* 45 处 (redismodule.h) — 覆盖加载/持久化/复制/键空间/ACL/集群等面
- **命令过滤** (RM_RegisterCommandFilter): moduleCallCommandFilters (L6367) — **processCommand 前调用** (server.c 链, R-20 交叉); 模块可改写/拦截/追加参数 (CommandFilterArgInsert/Replace/Delete); commandfilter.c 测试
- **keyspace 通知** (R-29 交叉): moduleNotifyKeyspaceEvent — 模块键的修改可发通知 (与原生通知同构)
- **ACL 集成**: RM_CreateModuleUser / RM_ACLCheckCommandPermissions (R-32 交叉) — 模块内命令按 ACL 身份检查
- **模块 INFO**: RM_RegisterInfoFunc (L10396) — 模块注入 INFO 节
- **模块配置**: RM_RegisterStringConfig 家族 (L12869+) + RM_LoadConfigs (L12974) — 7.0 模块配置体系

## 代码类型
Interface (事件总线 + 钩子面)

## 跨域关联
- R-29 (pubsub): keyspace 通知
- R-20 (server): processCommand 过滤链 / cron 事件
- R-32 (ACL): 权限检查 API

## 结论
钩子面 = 事件订阅分发 (全局事件总线) + 命令过滤 (执行前改写) + keyspace 通知 + ACL/INFO/配置 API — 模块从"数据结构扩展"升维到"行为扩展"。
源码位置: module.c:6367,10396,11663-11761,12869-12974
