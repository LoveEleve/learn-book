# R-28 上 — 协议解析: 从字节流到 argv

> 前置: [[R-2-events]] (读事件/postpone) + [[R-4-SDS]] (querybuf 消费链) + [[R-1-object]] (argv 对象) | 引出: [[R-28-下]] (输出/生命周期) + [[R-20-server]] (processCommand) | 对照: [[R-2-下]] (io 线程只解析不执行)
> 🔴 A | 4 KP | [模式: 三行状态机+零拷贝借道+滑窗消费+分级限流]
> Pass 2 闭环: q2(读路径) q3(解析循环) q4(多行解析) q8(安全面)

**读者处境**: redis-cli 敲一条 GET, 服务器端从字节流到 argv 经历了什么?为什么 32KB 以上的参数不复制?未认证的客户端为什么只能发 16KB 的 bulk?querybuf 为什么不会无限膨胀?这篇拆协议解析: 读取路径、解析状态机、RESP 多行解析、安全限流。

### 1. 读取路径 — readlen 的四种决策

场景: 一次读多少?怎么分配?
源码路径:
- readQueryFromClient (networking.c:2655-2764): postpone 检查 (L2662, io 线程延期) → **readlen 决策** (L2667-2688)
- **四种 readlen** (L2667-2688): 默认 PROTO_IOBUF_LEN=16KB (server.h:164); 大参数 (bulklen ≥ 32KB) → 读剩余精确字节 (L2677-2682, 零拷贝准备); MASTER → 回升 16KB (L2686-2687, #9100)
- **分配双策略** (L2691-2707): NonGreedy (L2698, 大参数/初分配, 按需防膨胀 — R-4) vs Greedy (L2703, readlen = sdsavail 一次多读省 read)
- connRead → sdsIncrLen (L2727) → **querybuf 上限** (L2739-2755: client_max_querybuf_len 默认 1GB config.c:3226; 未认证 >1MB 断开)
- 统计 (L2731-2737): lastinteraction + 输入字节
关键设计 (q2): **"多读省系统调用 vs 精确读省复制"** 权衡: 普通 Greedy 读满, 大参数精确读让 bulk 独占 querybuf 首部。[模式: 双策略分配]
数据流: socket → readlen 决策 → 分配 → connRead → sdsIncrLen → 上限检查 → 解析。

### 2. 解析循环 — querybuf 滑窗

场景: 一条连接上多个命令怎么连续处理?
源码路径:
- processInputBuffer (L2559-2653): `while(qb_pos < len)` 消费
- **四个提前退出** (L2563-2580): BLOCKED / PENDING_COMMAND / master+忙脚本 (L2573) / CLOSE_AFTER_REPLY|CLOSE_ASAP
- **reqtype 判定** (L2583-2589): 首字节 `*` → MULTIBULK; 否则 INLINE (telnet 兼容)
- **io 线程标记** (L2606-2610): 解析完成但执行留给主线程 (CLIENT_PENDING_COMMAND)
- 执行 (L2613-2618): processCommandAndResetClient → commandProcessed (L2459-2490: blocked 不 reset / master 复制偏移)
- **querybuf trim** (L2622-2644): 普通按 qb_pos 裁剪 (L2640-2644); **master 按 repl_applied** (L2635-2639, querybuf 兼作复制流代理)
关键设计 (q3): 滑窗消费 + 立即 trim = **querybuf 常驻小**; io 线程下解析/执行分离。[模式: 滑窗消费]
数据流: qb_pos → 判定类型 → 解析 → 执行 → trim → 下一条。

### 3. RESP 多行解析 — 三行状态机

场景: *3 $3 GET 怎么变成 argv?
源码路径:
- processMultibulkBuffer (L2292-2452):
  - **首行 `*` 计数** (L2297-2340): 超 64KB 计数行报错 (L2304); ll > INT_MAX 报错 (L2319); **未认证 >10 报错** (L2323)
  - argv 初始化 (L2335-2339): `argv_len = min(multibulklen, 1024)` 起步; **2× 增长** (L2416-2419, 上限 INT_MAX)
  - **`$` 长度行** (L2345-2407): 非 `$` 报错 (L2361); ll < 0 或 > proto_max_bulk_len (默认 512MB, config.c:3206) 报错 (L2370-2374, master 豁免); **未认证 >16KB 报错** (L2375)
  - **大参数预对齐** (L2382-2406): sdsrange 到首部 + NonGreedy 预分配 — 零拷贝准备
  - **数据读取** (L2411-2444): 不足 bulklen+2 → 等更多
    - **零拷贝路径** (L2424-2435): 非 master && qb_pos==0 && bulklen ≥ 32KB && querybuf 恰好整包 → **createObject 直接借用 querybuf** (L2429) + sdsIncrLen(-2) 去 CRLF (L2431)
    - 普通路径 (L2437-2440): createStringObject 复制
- 协议错误 (setProtocolError L2252): 标记 + 断开 (防死循环)
关键设计 (q4): **三行状态机 (* → $ → data) 全上限防护**; 大参数零拷贝 (32KB 阈值: 一次复制 32KB 值得省?)。[模式: 状态机+零拷贝]
数据流: * 行 → 计数 → $ 行 → 长度 → data → argv[argc++] → multibulklen-- → 0 则执行。

### 4. 安全面 — 未认证分级限流

场景: 没认证的连接能占多少资源?
源码路径:
- **未认证三级限** (networking.c:2323,2375,2745): multibulk ≤10 参数 / bulk ≤16KB / querybuf ≤1MB — 认证前无法执行命令, 防资源洪水
- **全局上限**: proto_max_bulk_len 512MB (L2371) / client_max_querybuf_len 1GB (L2744) / PROTO_INLINE_MAX_SIZE 64KB (L2304)
- 协议错误处理 (L2252-2291): CLIENT_PROTOCOL_ERROR + 断开 — 不降级不重试
- authRequired (L103-111): 认证状态判定
关键设计 (q8): **认证前收紧, 认证后放宽** — 未认证连接只能小打小闹; 协议错误一律断开。[模式: 分级限流]
数据流: 未认证 → 10 参数/16KB/1MB 限 → AUTH → 512MB/1GB 全局限。

### 负面空间 — 协议解析刻意不做的事

- **不做流式解析**: 整条命令必须完整才执行 (无半命令执行)
- **不做协议协商**: RESP2/3 由 HELLO 显式切换, 无自动探测
- **不做未认证队列**: 未认证直接限流, 不排队
- **不做零拷贝常态**: 仅 ≥32KB 大参数才借用 querybuf (小参数复制开销可忽略)
- **不做错误重试**: 协议错误即断连, 客户端自行重连

→ 引出: 回复怎么组装和发送?→ [[R-28-下]]
