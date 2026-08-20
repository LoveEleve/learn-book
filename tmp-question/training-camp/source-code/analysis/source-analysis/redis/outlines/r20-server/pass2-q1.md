# 闭环笔记 q1: main 启动管线 — 阶段编排

## 假设
main 的阶段顺序有严格约束: 哨兵模式必须先于配置文件解析 (配置会填充哨兵结构); 测试模式/check 模式/正常模式三分支。

## 验证过程
- main (server.c:6917+):
  - L6922-6928: REDIS_TEST 模式 (--test/--accurate/--large-memory/--valgrind)
  - L6970: zmalloc_set_oom_handler (R-33 装配)
  - L6985-6987: getRandomBytes → dictSetHashFunctionSeed (R-3 SipHash seed!)
  - L7006-7012: **sentinel 先于配置**: 注释 "We need to init sentinel right now as parsing the configuration file in sentinel mode will have the effect of populating the sentinel data structures" — 哨兵配置填充依赖结构先建
  - L7014-7021: redis-check-rdb/aof 模式 (exec_name 判断)
  - L7023+: 参数解析 (-v/--version/配置项)
  - 中间: loadServerConfig → initServer → 就绪通知 → aeMain (L7251)
- 顺序约束: OOM handler → 哈希 seed → 哨兵结构 → 配置 → 服务初始化 → 事件循环
- 返回: aeDeleteEventLoop + return 0

## 代码类型
Glue (启动编排) — 生命周期

## 跨域关联
- R-33 (OOM handler) / R-3 (seed) → 前置装配
- R-14 (sentinel) → 模式分支
- R-2 (aeMain) → 事件循环入口

## 结论
启动管线 = 依赖序约束的编排: 内存层 (OOM) → 安全层 (哈希 seed) → 模式分支 (哨兵/check/正常) → 配置 → 服务初始化 → 事件循环。哨兵必须先于配置是"配置填充依赖结构"的硬约束。
源码位置: server.c:6917-7256
