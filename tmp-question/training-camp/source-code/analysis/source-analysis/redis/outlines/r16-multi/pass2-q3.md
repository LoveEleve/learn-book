# 闭环笔记 q3: EXEC — 双失败路径与执行循环

## 假设
DIRTY_CAS → nullarray; DIRTY_EXEC → EXECABORT; 正常执行逐命令 call + ACL 复查; DENY_BLOCKING 保护。

## 验证过程
- execCommand (multi.c:127-235):
  - 非 MULTI → "EXEC without MULTI" (L133-136)
  - **isWatchedKeyExpired 检查** (L139-141): 有 WATCH 键已过期 → DIRTY_CAS
  - **双失败路径** (L149-157): DIRTY_EXEC → shared.execaborterr (EXECABORT 错误对象, server.c:1894); 仅 DIRTY_CAS → shared.nullarray[resp] (nil 数组, "technically not an error" L146-148); 两者都 discardTransaction
  - 执行准备 (L160-173): 保存 old_flags → **CLIENT_DENY_BLOCKING 置位** (L163, 注释 "we do not want to allow blocking commands inside multi") → unwatchAllKeys (L166, "ASAP otherwise we'll waste CPU cycles") → server.in_exec=1 (L168)
  - 保存 orig argv/cmd (L170-173) + addReplyArrayLen(count) (L174)
  - **逐命令执行** (L175-222): 恢复 mstate 的 argc/argv/cmd (L176-179); **ACL 复查** (L184-207, "in case they were changed after the commands were queued" L181-182, 5 种拒绝原因); **CLIENT_ID_AOF → CMD_CALL_NONE** (L209-212, AOF 加载期不重复传播 — R-8 未来域); 断言无阻塞 (L214)
  - **mstate 回写** (L218-221): "Commands may alter argc/argv" — 命令可能改写参数 (如 GEOADD 重写), 回写保一致性
  - 收尾 (L224-234): 恢复 DENY_BLOCKING → 恢复 orig argv/cmd → discardTransaction → in_exec=0
- execCommandAbort (L115-125): discard + **-EXECABORT 前缀错误** (L118-119) + **replicationFeedMonitors** (L124, "Send EXEC to clients waiting data from MONITOR" L121-123)

## 代码类型
Mechanism (事务执行)

## 跨域关联
- R-20: call()/CMD_CALL_FULL
- R-2: CLIENT_DENY_BLOCKING (R-26 阻塞框架交叉)
- R-8 (未来): AOF 客户端 CMD_CALL_NONE
- R-32 (未来): ACLCheckAllPerm

## 结论
EXEC = 单线程内顺序执行队列 (天然原子); 两失败语义 (CAS→nil / EXEC→abort); 入队后 ACL 变更复查; DENY_BLOCKING 防阻塞命令破坏原子性。
源码位置: multi.c:115-235; server.c:1894
