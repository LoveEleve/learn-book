# 闭环笔记 q3: processInputBuffer — 解析循环

## 假设
解析循环 = 逐命令消费 querybuf: reqtype 判定 → 解析 → 执行/标记 → trim。

## 验证过程
- processInputBuffer (networking.c:2559-2653):
  - 循环条件 (L2561): `while(c->qb_pos < sdslen(c->querybuf))`
  - **四个提前退出** (L2563-2580): CLIENT_BLOCKED / CLIENT_PENDING_COMMAND (已有待执行) / master+忙脚本 (L2573, 只积累不执行) / CLOSE_AFTER_REPLY|CLOSE_ASAP
  - **reqtype 判定** (L2583-2589): 首字节 `*` → PROTO_REQ_MULTIBULK; 否则 INLINE (telnet 兼容)
  - 解析分派 (L2591-2597): INLINE → processInlineBuffer; MULTIBULK → processMultibulkBuffer; 失败 break
  - argc==0 (空命令) → resetClient (L2600-2601)
  - **io 线程标记** (L2606-2610): io_threads_op != IDLE → CLIENT_PENDING_COMMAND + break (解析已完成, 执行留给主线程)
  - 执行 (L2613-2618): processCommandAndResetClient → C_ERR (客户端死) → 返回
  - **querybuf trim** (L2622-2644):
    - MASTER (L2635-2639): 按 repl_applied trim (master querybuf 同时是复制流, 不能按 qb_pos)
    - 普通 (L2640-2644): `sdsrange(querybuf, qb_pos, -1); qb_pos = 0` — 消费完即收缩
  - 内存统计更新 (L2649-2650)
- processCommandAndResetClient (L2501-2525): current_client 设置/processCommand/commandProcessed (L2459: blocked 不 reset + master 复制偏移)

## 代码类型
Mechanism (解析循环)

## 跨域关联
- R-2 (io_threads_op) / R-20 (processCommand) / R-9 (master 复制流)

## 结论
解析循环 = **"读多少解析多少"** 的消费模型: querybuf 是滑窗 (qb_pos), trim 即回收。io 线程场景下解析与执行分离 (PENDING_COMMAND 标记)。master 客户端的 trim 语义不同 (querybuf = 复制流代理)。
源码位置: networking.c:2459-2653
