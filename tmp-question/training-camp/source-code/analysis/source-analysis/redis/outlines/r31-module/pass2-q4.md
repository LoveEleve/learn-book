# 闭环笔记 q4: 执行面 — RM_Call 临时客户端 + 阻塞与线程

## 假设
RM_Call = 临时客户端走完整命令链; 后台线程经 ThreadSafeContext 锁进临界区; 阻塞 API 走 R-26 框架。

## 验证过程
- **RM_Call** (L6304-6358+): fmt 格式串 (moduleCreateArgvFromUserFormat) → **moduleAllocTempClient** (临时客户端) + **CLIENT_DENY_BLOCKING** (防阻塞, L6323-6326, 可 ALLOW_BLOCK 覆盖) + DB 对齐 (c->db = ctx->client->db) + resp 选项 (RESP3/RESP_AUTO) + **RUN_AS_USER** (ACL 身份, R-32 交叉) + ctx->module->in_call++ (重入检测)
- **后续执行**: call() 全链 (ACL/传播/keyspace 通知) — 与正常命令同构; 回复 → RedisModuleCallReply (callReply 家族 — RESP3 全类型解析, R-28 交叉)
- **阻塞 API**: RM_BlockClient / RM_BlockClientOnKeys (R-26 框架) — 模块可把客户端挂到阻塞框架; moduleHandleBlockedClients (L8311) 事件循环面 (R-2 交叉); helloblock.c/blockonkeys.c 测试
- **线程面** (L8522-8587): RM_GetThreadSafeContext (后台线程取 ctx) + **RM_ThreadSafeContextLock/Unlock** — 后台线程通过全局锁进入主线程临界区 (单线程核心的并发边界); 锁内才能安全调 RM_Call/操作键
- **超时与保护**: busy_module_yield_flags (R-30 交叉 — 模块长命令也进 busy 面, server.c:691)

## 代码类型
Implementation (执行与并发面)

## 跨域关联
- R-20 (server): call() 链
- R-26 (list): 阻塞框架
- R-2 (events): 事件循环/后台线程
- R-30 (lua): busy 模式对照

## 结论
执行面 = 临时客户端同构 (RM_Call) + 阻塞框架接入 (RM_BlockClient) + 后台线程经全局锁进临界区 (ThreadSafeContext) — 模块在单线程核心外开"受控并发"窗口。
源码位置: module.c:6304-6358,8311,8522-8587
