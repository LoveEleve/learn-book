# 闭环笔记 q5: 能力探测降级 — SORT_RO 与 EVALSHA_RO 的失败回退

## 假设
Redisson 对"只读命令扩展" (SORT_RO/EVALSHA_RO) 采用能力探测: 首次用新命令, 服务端报 ERR unknown command 后置 AtomicBoolean=false 并降级旧命令。探测结果缓存避免每次重试。

## 验证过程
- **SORT_RO 探测** (CommandAsyncService.java:688,694-714):
  - L688 `static final AtomicBoolean SORT_RO_SUPPORTED = new AtomicBoolean(true)`
  - L694-701: readOnlyMode + SORT + SORT_RO_SUPPORTED → 构造 `new RedisCommand("SORT_RO", ...)` 用 `async` 执行
  - L705-708: 失败消息 `ERR unknown command` → SORT_RO_SUPPORTED=false → **递归 async(false=写模式, 原 SORT 命令)** 降级
  - L696-697: SORT_RO 不支持 (false) 直接降级 SORT (读转写, 因为 SORT 是写命令可能涉及排序到 dest)
- **EVALSHA_RO 探测** (L542,592-595,611-614):
  - L542 `static final AtomicBoolean EVAL_SHA_RO_SUPPORTED`
  - L592-595: readOnly + 支持 → EVALSHA_RO; else EVALSHA
  - L611-614: `ERR unknown command` → false + 递归 evalAsync 降级 EVALSHA
- 模式共性: **默认乐观用新命令能力 → 失败置 false 永久降级 → 进程内缓存** (SORT_RO_SUPPORTED 是 static, 跨实例共享)
- 降级语义: SORT_RO (只读) → SORT (可能写), 因为旧服务端只有 SORT

## 代码类型
Implementation (能力探测) — 高价值: 乐观探测 + 永久降级缓存

## 跨域关联
- Q4 (resp3) → 同为协议版本适配
- r28 (RESP3 命令面) → 服务端能力决定客户端命令选择
- Redis Cluster (VM 文档) → SORT_RO 在 cluster 只读场景

## 结论
能力探测 = 乐观使用新命令 (SORT_RO/EVALSHA_RO) → ERR unknown command → static AtomicBoolean 永久降级 + 递归旧命令。进程级缓存避免每次探测。这是"对新服务端特性乐观, 对旧服务端兼容"的标准模式。
源码位置: CommandAsyncService.java:542,688,694-714,611-614