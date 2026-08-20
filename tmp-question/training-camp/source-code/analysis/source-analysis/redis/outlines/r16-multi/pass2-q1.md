# 闭环笔记 q1: 命令队列 — multiState 与 queueMultiCommand

## 假设
multiState 数组 + 预分配 2 + 倍增; 入队即转移 argv 所有权; DIRTY 后冻结。

## 验证过程
- multiState (server.h:1005-1016): commands 数组 + count + **cmd_flags (任一命令有某标志则置位)** + **cmd_inv_flags (~flags OR, 全命令都有某标志可知)** + argv_len_sums (内存统计) + alloc_count
- multiCmd (server.h:998-1003): argv/argv_len/argc/cmd
- initClientMultiState (multi.c:14-21) / freeClientMultiState (L24-36: 逐命令逐 argv decrRefCount + zfree)
- queueMultiCommand (L39-75):
  - **DIRTY_CAS|DIRTY_EXEC 直接 return** (L46-47) — 事务已注定失败, 不浪费内存 (pipeline 场景注释 L42-45)
  - **预分配 2** (L48-53): "assuming it is used to execute at least two commands" — 最小分配假设
  - 倍增扩容 (L54-57, INT_MAX/2 上限)
  - 入队 (L58-67): 复制 c->cmd/argc/argv 指针; cmd_flags/cmd_inv_flags 累计; argv_len_sums += argv_len_sum + sizeof(robj*)*argc
  - **转移所有权** (L71-74): c->argv=NULL/argc=0 — 客户端当前参数归 mstate 所有 (不 double free)
- processCommand 接入 (server.c:4193-4201): CLIENT_MULTI 且非 exec/discard/multi/watch/quit/reset → queueMultiCommand + 回复 **QUEUED**

## 代码类型
Mechanism (命令队列)

## 跨域关联
- R-1: argv robj 引用计数转移
- R-20: processCommand 命令链
- R-28: pipeline 语义 (注释)

## 结论
队列 = 指针所有权转移 (零拷贝 argv) + 预分配 2/倍增; cmd_flags/cmd_inv_flags 双累计供后续优化判断 (任一/全部命令属性); 事务已脏则冻结入队。
源码位置: server.h:998-1016; multi.c:14-75; server.c:4193-4201
