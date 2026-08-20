# 闭环笔记 q2: 入队错误 — DIRTY_EXEC 与 CMD_NO_MULTI

## 假设
入队期任何错误 → flagTransaction → DIRTY_EXEC → EXEC 时报 EXECABORT; 部分命令禁入事务。

## 验证过程
- flagTransaction (multi.c:86-89): CLIENT_MULTI 时置 DIRTY_EXEC
- rejectCommand 系列 (server.c:3757-3785): **统一 flagTransaction + 被拒命令是 EXEC → execCommandAbort** (L3764-3766) — EXEC 本身被拒 (如 OOM 时 CMD_DENYOOM) 直接中止事务
- flagTransaction 调用点: rejectCommand (L3760) / rejectCommandSds (L3772) / ACL 拒绝 (L4012)
- **CMD_NO_MULTI** (server.c:3979-3981): `CLIENT_MULTI && cmd->flags & CMD_NO_MULTI` → rejectCommandFormat "Command not allowed inside a transaction" — grep 实证仅 **4 命令**: psync/save/shutdown/sync
- **DIRTY_EXEC 语义**: 队列期错误 (语法/权限/参数) 记标志, EXEC 时统一 EXECABORT — 而不是入队期逐条报错中断 (客户端可 pipeline 继续发)
- 与 DIRTY_CAS 区别: DIRTY_CAS → EXEC 回 nullarray (非错误); DIRTY_EXEC → EXECABORT 错误 (multi.c:149-157)

## 代码类型
Mechanism (错误传播)

## 跨域关联
- R-23: CMD_DENYOOM (OOM 时 EXEC 被拒)
- R-20: 命令标志体系 (CMD_NO_MULTI 是其中一个)
- R-32 (未来): ACL 拒绝 → flagTransaction

## 结论
入队期错误延迟到 EXEC 才爆发 (EXECABORT), 客户端 pipeline 语义不被中断; 4 个禁入命令防破坏性管理命令进事务。
源码位置: multi.c:86-89; server.c:3757-3785,3979-3981
