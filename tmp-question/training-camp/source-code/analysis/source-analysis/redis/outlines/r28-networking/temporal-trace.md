# R-28 networking 协议 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | networking.c 初版 — RESP 解析/静态输出缓冲 (版权 "2009-Present") |
| 早期 | INLINE 命令支持 (telnet 兼容, processInlineBuffer L2174); 单 buf 输出 |
| 3.x/4.0 | **输出双缓冲** (静态 buf + reply 链表); writev 批量写 (_writevToClient L1844) |
| 4.0 | clientReplyBlock 16KB 节点 + reply_bytes 记账 + 超限断连 |
| 6.0 | **共享 repl 缓冲** (replBufBlock, R-9) — 从库不持私有 reply 链表 |
| 6.0 | io threads (postponeClientRead L4491, R-2) |
| 7.x | **大参数零拷贝** (PROTO_MBULK_BIG_ARG=32KB, querybuf 借用 L2424-2435); **未认证分级限流** (10/16KB/1MB, L2323/2375/2745); reqres (RESP3 请求响应日志); CLIENT KILL/CLIENT SETINFO 等命令族扩展 |
| 演进 | querybuf 从"整包重分配"→"滑窗 qb_pos + trim" (早期无 qb_pos, 7.x 大参数预对齐后定型) |

## 痕迹证据

- L316-322: _addReplyToBuffer 的 sanitizer suppression 注释 — 静态 buf 用 zmalloc_usable_size 边界 (R-33 联动)
- L365-367: 新节点 "take over the allocation's internal fragmentation" — zmalloc_usable 吃碎片
- L2382-2395: 大参数预对齐的完整设计注释 (为什么只有 ≤ ll+2 才 trim)
- L2421-2423: "if a non-master client's buffer contains JUST our bulk element... just use the current sds string" — 零拷贝动机
- L2684-2685: master 扩大 readlen 的 #9100 引用 (版本修复痕迹)
- L2724-2725: 未认证 querybuf 上限注释 (1MB)
- L419-420: push 消息暂存的注释 (MULTI/EXEC 中订阅推送顺序)

## 推断标注

- "静态 buf 16KB = 大多数回复零分配" — 16KB 常量是事实 (server.h:165), "大多数回复 ≤16KB"是推断 (无统计断言, 但 buf_peak 统计存在)
- "大参数阈值 32KB 是复制成本权衡" — 32KB 常量是事实 (server.h:167), 权衡依据是推断
- "未认证 16384 bulk 与 16KB IOBUF 相关" — 数值接近, 相关性是推断 (无注释)
