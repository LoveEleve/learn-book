# 闭环笔记 q2: initServer 矩阵 — 信号/事件循环/键空间

## 假设
initServer 是初始化矩阵: 信号处理 → 线程面 → 事件循环 → 键空间分配 (7.x kvstore 分片) → list 族 → 周期注册。

## 验证过程
- initServer (server.c:2591+):
  - L2593-2597: 信号: SIGHUP/SIGPIPE 忽略 + setupSignalHandlers (SIGTERM/SIGINT → 优雅关闭) + ThreadsManager_init (io threads) + makeThreadKillable
  - L2610-2614: 状态初始化 (aof_state/hz/pid/in_fork_child/main_thread_id/errors rax)
  - L2653: createSharedObjects (R-1 共享池)
  - L2657: `server.el = aeCreateEventLoop(maxclients+CONFIG_FDSET_INCR)` — 事件循环
  - L2665-2680: **键空间 (7.x kvstore)**: `server.db = zmalloc(sizeof(redisDb)*dbnum)`; cluster 模式 slot_count_bits=14 (16384 槽分片!) — `kvstoreCreate(&dbDictType, slot_count_bits, flags)` + expires + `hexpires = ebCreate()` (R-21 连接) + blocking_keys/ready_keys/watched_keys
  - L2700+: list 族 (clients/slaves/monitors/pending_write/pending_read...)
  - 结尾: aeCreateTimeEvent(serverCron, 1ms) (L2757) + 监听端口注册 (L2447 之前已看)
- 对比旧版: 7.x 键空间从单 dict → kvstore 分片 (slot_count_bits: 非 cluster 0, cluster 14) — R-21 详述

## 代码类型
Glue (初始化矩阵) — 生命周期

## 跨域关联
- R-2 (aeCreateEventLoop/TimeEvent) → 事件面
- R-21 (kvstore 键空间) → 数据面
- R-1 (共享对象) / R-3 (dict) → 组件装配

## 结论
initServer = 全组件装配: 信号 (忽略+优雅) → 线程 → 事件循环 → 键空间 (kvstore 分片, cluster 14bit) → 客户端 list 族 → cron 注册。7.x 键空间分片是最大的结构变化。
源码位置: server.c:2591-2772
