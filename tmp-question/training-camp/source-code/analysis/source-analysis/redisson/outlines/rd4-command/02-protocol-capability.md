# RD-4 篇2 — protocol: RESP3 适配与能力探测降级

> 前置: [[RD-4-篇1]] (async 汇聚调 resp3) | 复用: [[r28-networking]] (RESP2/3 协议) | 对照: [[s75-boot-redis]] (Lettuce 协议选择) | 引出: [[RD-4-篇3]] (Lua 执行) + [[rd5-rmap]]
> 🔴 A | 2 KP | [模式: 静态映射 + 乐观探测降级]
> Pass 2 闭环: q4(RESP3 适配) q5(能力探测降级)

**读者处境**: RESP2 和 RESP3 的区别不只在协议层 — 有的命令在 RESP3 下要换个"版本"用。SORT_RO 这种只读命令, 老 Redis 不认识怎么办?Redisson 用"一遍探测 + 永久降级"的原子布尔搞定。这篇拆 RESP2/3 的命令映射表, 以及 SORT_RO/EVALSHA_RO 的能力探测降级机制。

### 概念依赖链
q4(RESP3 映射) ← q5(能力探测) — 先讲"协议版本决定命令面", 再讲"命令能力如何探测降级"。

### 核心悬念
"RESP2 和 RESP3 下, 同一条命令为什么会变成两条? 老节点不认 SORT_RO 怎么办？"

### 叙事顺序
1. 问题引入: RESP3 不只是新协议 — 命令面也变了
2. resp3 映射表 (q4) — ServiceManager RESP3MAPPING: Stream/ZSet 读族 _V2 版
3. 能力探测 (q5) — SORT_RO: static AtomicBoolean 乐观探测 + ERR 降级
4. EVALSHA_RO 同类降级 (q5) — 两个 atomic 一个模式
5. 收束: "乐观用新能力 + 永久降级缓存" 的兼容哲学

### 1. RESP3 命令映射 — 同命令两版本

场景: XREAD 在 RESP3 下为什么要 XREAD_V2?
源码路径:
- ServiceManager.java:686 `cfg.getProtocol() == Protocol.RESP3` — 协议模式
- RESP3MAPPING (ServiceManager.java:689-705): XREADGROUP_BLOCKING→XREADGROUP_BLOCKING_V2 / XREADGROUP→V2 / XREADGROUP_BLOCKING_SINGLE / XREADGROUP_SINGLE / XREAD_BLOCKING_SINGLE / XREAD_SINGLE / XREAD_BLOCKING / XREAD / HRANDFIELD / VSIM_WITHSCORESATTRIBS / ZRANGE_SINGLE_ENTRY / ZRANGE_ENTRY / ZREVRANGE_ENTRY → 各 _V2
- **受影响面**: Stream 读族 + ZSet 范围读 + HRANDFIELD + 搜索 — 响应含 map/嵌套结构, RESP3 用 map 类型精确表达
- 为什么: RESP2 顶层只有 array, 嵌套聚合用 flatten; RESP3 有 map/set/double 类型 — V2 版命令用新解码器 (ReplayMultiDecoder) 解析新结构
- async() 每次调 resp3(command) (CommandAsyncService.java:692) → resp3 方法本体 (ServiceManager.java:713-738) — 执行前统一适配, 查 RESP3MAPPING 换 _V2
关键设计 (q4): 协议演进 = **命令版本映射表 + 按协议模式切换**; RESP3 map 语义需要配套 V2 命令解码器。[模式: 静态版本映射]
数据流: Config.protocol=RESP3 → async → resp3 查表 → cmd=_V2 → 新解码器解析 map。

### 2. SORT_RO 能力探测 — 乐观用新, 失败永降

场景: 老 Redis 不认识 SORT_RO, 每次探测多浪费?
源码路径:
- `static final AtomicBoolean SORT_RO_SUPPORTED = new AtomicBoolean(true)` (CommandAsyncService:688)
- async() (CommandAsyncService.java:694-701): readOnlyMode + SORT + 支持 → **构造 SORT_RO 外层命令** `new RedisCommand("SORT_RO", getReplayMultiDecoder())` 执行
- L696-697 (CommandAsyncService.java): 已降级 (false) → 直接 SORT (读写如何分流见 embedded)
- L705-708 (CommandAsyncService.java): 失败 `ERR unknown command` → `SORT_RO_SUPPORTED.set(false)` + **递归 async(false, 原 SORT)** 降级
- SORT_RO vs SORT (CommandAsyncService.java:697 分支): SORT_RO 是只读版 (可走 slave), SORT 可能写 destination → 降级连读模式都转写
- 意义: 探测只发生一次 (首次), 之后 static atomic false 永远走旧路 — **零重复探测成本**
关键设计 (q5): 能力探测 = **乐观首发 + atomic 永久降级 + 递归重放**; static 跨实例共享, 进程内只测一次。[模式: 乐观能力探测]
数据流: 首次 SORT_RO → ERR unknown → atomic=false → 递归 SORT → 之后都 SORT。

### 3. EVALSHA_RO 同类降级 — 只读脚本的另一半

场景: Lua 只读执行也想走 slave, 不支持时怎么办?
源码路径:
- `static final AtomicBoolean EVAL_SHA_RO_SUPPORTED` (CommandAsyncService.java:542)
- evalAsync (CommandAsyncService.java:592-595): readOnly + 支持 → EVALSHA_RO; 否则 EVALSHA (下篇详 Lua)
- L611-614: `ERR unknown command` → false + 递归 evalAsync 降级 EVALSHA — **递归传 mappedScript (已 map 过, 避免二次映射)** (CommandAsyncService:613)
- 与 SORT_RO 同构 (CommandAsyncService.java:611-614): 乐观探测 + atomic 永久降级 + 递归重放
- 两者差异: SORT_RO 降级换命令名 (SORT_RO→SORT), EVALSHA_RO 降级换 EVAL 变体 (EVALSHA_RO→EVALSHA) — 都是"只读命令 → 通用命令"
关键设计 (q5): 只读优化的两面: SORT_RO (排序) + EVALSHA_RO (脚本) — 都走"读优先, 不支持退写"。[模式: 只读双降级]
数据流: EVALSHA_RO → ERR → atomic=false → EVALSHA (读写通用)。

### 负面空间 — 协议适配刻意不做的事

- **不每次请求探测**: 能力结果 static 缓存, 一次失败永降 (无法感知服务端升级)
- **不动态协议切换**: protocol 固定于 Config, 运行中不变 RESP2/3
- **不做协议协商重试**: HELLO 失败不回退 RESP2 (REDISSON 要求一致)
- **不命令级协议混合**: 同连接同一协议, 无单命令切换
- **不做未知命令尝试**: 仅 SORT_RO/EVALSHA_RO 两个探测点, 非通用机制

→ 引出: Lua 脚本怎么走 EVALSHA 缓存 + NOSCRIPT 自愈?批处理怎么分组?→ [[RD-4-篇3]]