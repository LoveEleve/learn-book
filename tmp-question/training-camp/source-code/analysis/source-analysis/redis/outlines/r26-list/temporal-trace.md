# R-26 t_list+blocked — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | t_list.c 初版 — LPUSH/LPOP 家族 (版权 "2009-Present"); blocked.c 同期 — BLPOP 阻塞框架 |
| 2.8 | **阻塞框架重构**: blockForKeys 双向注册 (bstate.keys + blocking_keys); ready_keys 队列 + handleClientsBlockedOnKeys |
| 3.2 | quicklist 引入 (R-5) 替代 linkedlist+ziplist 混合 |
| 6.0 | LMOVE/BLMOVE (替换 RPOPLPUSH 语义); 阻塞类型扩展 (WAIT/WAITAOF 复制面, 6.0+); unblock_on_nokey (XREADGROUP) |
| 7.x | list-max-listpack-size (旧 ziplist 别名); LIST_CONV_SHRINKING + beforeConvertCB 回调 (lazyfree 联动) |

## 痕迹证据

- t_list.c:110-127: listTypeTryConversionRaw 的 GROWING/SHRINKING 分派 (双编码转换方向)
- blocked.c:442-444: ready_keys dict 防重注释 ("avoid putting the same key again and again in case of multiple pushes made by a script or in the context of MULTI/EXEC")
- blocked.c:404-406: "Currently we assume key blocking will require reprocessing the command" — 重处理设计原点
- blocked.c:319-321: 新列表交换注释 ("like a BLMOVE would do, then the new unblocked command will get processed right away")
- blocked.c:555-556: FIFO 注释 ("We serve clients in the same order they blocked")
- blocked.c:565-567: 防无限循环注释 (重阻塞场景)
- server.h:398-407: 10 种阻塞类型枚举 (WAIT/WAITAOF 是复制/持久化面)

## 推断标注

- "空键不存在不变量" — 代码事实: listElementsRemoved 空即 dbDelete (L748-750) + 唤醒只需 dbAdd (db.c:192); "不变量"命名是推断 (无注释明说, 但行为一致)
- "stream 例外需显式唤醒" — t_stream.c:2083 显式 signalKeyAsReady 是事实; "因为 stream 键不因消费删"是推断 (stream 有 PEL/消息保留语义支撑)
- "FIFO 公平性" — listAddNodeTail + 从头遍历是事实; "公平性动机"由注释 (L555-556) 支撑
