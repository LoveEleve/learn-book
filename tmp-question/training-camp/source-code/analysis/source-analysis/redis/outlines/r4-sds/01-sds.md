# R-4 SDS — 动态字符串: 一个 char* 承载的长度感知、分级扩容、零拷贝

> 前置: [[R-33-zmalloc]] (usable 联动/分配器) | 引出: [[R-3-Dict]] (键的载体) | 对照: [[R-19-listpack]] (另一种紧凑编码)
> 🔴 A | 8 KP | [模式: 指针即对象+分级头+三路扩容+双标准+零拷贝桥]
> Pass 2 闭环: q1(指针即对象) q2(分级头) q3(扩容三路) q4(预分配) q5(usable) q6(双标准) q7(零拷贝) q8(复用)

**读者处境**: Redis 里每个键和值都是字符串 — 为什么 Redis 不直接用 C 的 char* 而要发明 SDS?同样存 "hello", 为什么 Redis 能存 5 字节, 而 C 字符串要浪费 6 字节 (还要担心截断)?追加 10 万次字符串为什么不会卡?客户端发来 1GB 数据为什么能边收边处理不用二次拷贝?这篇拆 Redis 字符串的内脏: 指针即对象、5 级头部、三路扩容、以及"内核直写"的零拷贝桥。

### 1. 指针即对象 — 一个 char* 的自我修养

场景: sds 到底是什么?为什么能直接 printf 又能存二进制?
源码路径:
- `sds.h:20` — `typedef char *sds;` — **就是裸 char***
- `sds.h:22-51` — 头部 `__packed__` 内嵌 buf 前: sdshdr5 (flags 单字节) / 8/16/32/64 (len+alloc+flags)
- `sds.h:64-79` — sdslen: `unsigned char flags = s[-1]; switch(flags&SDS_TYPE_MASK)` — **一次负索引 + switch 拿长度**
- `sds.c:142` — `s[initlen] = '\0'` — 总是 \0 结尾 (注释 L73-80: "You can print the string with printf()")
关键设计: 指针即对象 (q1): buf 起点就是 sds 指针, 头部在指针"后面" — C 生态零摩擦 (printf/系统调用/网络收发直传), 头部访问 O(1)。[模式: 内嵌头部 + 负索引访问]
数据流: sdsnewlen → 分配 (头部+内容+1) → s = sh+hdrlen → 调用方拿 s 直接用。

### 2. 五级头部 — 长度决定头大小

场景: 同样存字符串, 头部为什么有 5 种?sdshdr5 为什么特殊?
源码路径:
- `sds.c:40-52` — sdsReqType 阈值: <32→5, <256→8, <65536→16, <2^32→32, 其他→64
- `sds.h:22-27` — **sdshdr5: 只有 1 字节 flags** (3bit 类型 + 5bit 长度, 上限 31) — 注释 "**never used, we just access the flags byte directly**"
- `sds.c:87` — 空串创建强制 8 (`type 5 is not good at this`); `sds.c:241-244` — 扩容永不落 5 (无 alloc 字段, 每次追加都要重分配)
- `sds.c:316-318` — sdsResize 的 would_regrow → 禁止降到 5 (预期再增长)
关键设计: 空间最优分级 (q2): 长度编码进头部字宽 (1/2/4/8 字节), 短串 1 字节头; **sdshdr5 是"静态串专用"** — 无法记录可用空间, 一切可能增长的字符串强制升级 8。[模式: 分级头 + 增长性排除]
数据流: initlen → sdsReqType → hdrlen → 分配 hdrlen+len+1。

### 3. 扩容三路 — 头部不变才能 realloc

场景: 追加字符串时, 什么情况原地扩展?什么情况必须搬家?
源码路径:
- `sds.c:248-262` (_sdsMakeRoomFor) — 三路:
  1. `avail >= addlen` → **原地返回** (L226)
  2. `oldtype==type` → `s_realloc_usable` — **realloc 原地** (allocator 可能零拷贝膨胀)
  3. 类型升级 → `s_malloc_usable + memcpy + s_free` — 注释 L253-254: "**Since the header size changes, need to move the string forward, and can't use realloc**"
- `sds.c:327-343` (sdsResize 缩容) — use_realloc = 同型 || (降型 && type>8): **伪降型** — 保留旧头 (s[-1] 不更新), 只把分配缩到 oldhdrlen+size+1, alloc 字段缩到 size; 真换头只在升级或降到 8/5
关键设计: 三路由"头部是否变化"决定 (q3): realloc 要求 buf 偏移不变 (同型); 升级后 buf 起点变了, 裸 realloc 无法"平移内容到新偏移" → 显式 malloc+memcpy; 缩容的 use_realloc 是"旧头继续用 + 分配紧凑化"的伪降型。[模式: 布局感知的扩容]
数据流: 追加 → avail 够? 原地 : (同型? realloc : malloc+memcpy+free) → alloc=usable。

### 4. 预分配 — 2× 还是 +1MB, 何时不贪心

场景: 为什么追加 1 字节, 有时翻倍分配, 有时只多分 1MB?谁不预分配?
源码路径:
- `sds.h:13` — `SDS_MAX_PREALLOC (1024*1024)` — 1MB 分界
- `sds.c:232-237` — greedy=1: `newlen < 1MB → newlen *= 2; else newlen += 1MB` — **倍增 vs 线性**
- `sds.c:277-279` — sdsMakeRoomForNonGreedy (greedy=0): 只扩到刚够
- 消费: `networking.c:2401,2698` — **querybuf 读取用 NonGreedy** (读多少扩多少)
关键设计: 摊还 vs 浪费封顶 (q4): <1MB 倍增让 n 次追加的 realloc 次数 O(log n) (摊还 O(1)); ≥1MB 线性 +1MB 防大串翻倍浪费; NonGreedy 用于"客户端可控增长"路径 (querybuf) — 防恶意客户端撑爆预分配。[模式: 双策略预分配]
数据流: sdscatlen → MakeRoomFor(greedy=1) → 2×/线性; querybuf 读 → NonGreedy 按需。

### 5. usable 接力 + 缩容优化 — 与 zmalloc 的合谋

场景: SDS 的 alloc 字段为什么常常比 len 大得多?缩容为什么有时连 realloc 都省了?
源码路径:
- `sds.c:93-105` (_sdsnewlen) — `s_malloc_usable(hdrlen+initlen+1, &usable)` → **alloc = usable-hdrlen-1** (分配器实际, 免费膨胀, 上限 sdsTypeMaxSize) — R-33 q4 直接衔接
- `sds.c:332-338` (sdsResize) — `alloc_already_optimal = (je_nallocx(newlen,0) == zmalloc_size(sh))` — **预查询目标桶, 相同则跳过 realloc** (注释: realloc 即使不移动也有成本)
- `sds.c:354` — alloc 字段设回精确 size — 注释 L296-299: 防调用方检测到富余空间后反复 resize
关键设计: 跨层接力 (q5): jemalloc 的 size class 膨胀被 SDS 记账为 alloc (免费预分配); 缩容用 je_nallocx 预判"桶没变就不调 realloc" — 两层都是"省一次调用"。[模式: 分配器信息利用]
数据流: 创建 → usable=桶大小 → alloc=usable; 缩容 → nallocx 查询 → 桶不变? 跳过 realloc。

### 6. 双标准 — 二进制安全与 C 兼容共存

场景: 存了二进制内容 (含 \0), 为什么还能 printf 打印?len 和 \0 谁说了算?
源码路径:
- `sds.c:73-80` — 注释: "always null-terminated...can contain \0 characters in the middle, **as the length is stored in the sds header**"
- `sds.c:142` — 创建即 \0; `sds.c:463-472` — 追加后补 \0
- `sds.h:64-79` — sdslen 读 len 字段 (非 strlen)
- `sds.c:191-194` — sdsupdatelen: 手动改 buf 后按 strlen 同步 len ("hacked manually" 修复工具)
关键设计: NUL 免费兼容层 (q6): \0 只作"可打印终止", 语义全在 len — 中间 \0 完全合法。代价: 每串 +1 字节 NUL; 截断后需手动 updatelen。[模式: 双标准契约]
数据流: GET 返回二进制 → sdslen 长度为准 → RESP 传输完整内容 (含 \0)。

### 7. 零拷贝 — 内核直写 sds

场景: 客户端 1GB 数据怎么边收边用?为什么不用中间缓冲?
源码路径:
- `sds.c:385-397` (注释) — 模式: `MakeRoomFor(BUFFER_SIZE) → read(fd, s+oldlen, BUFFER_SIZE) → sdsIncrLen(s, nread)` — **内核直接写进 sds 缓冲**
- `sds.c:399-440` (sdsIncrLen) — 断言守卫: `assert(alloc-len >= incr)` (正增量防越界) / `assert(len >= -incr)` (负增量防下溢)
- 消费: `networking.c:2727` (read 后递增), `networking.c:2431` (**负增量 -2 去 CRLF**)
关键设计: 零拷贝桥 (q7): MakeRoomFor 扩好空间 → read 直写 → 手动递增 len — querybuf 全程零中间拷贝; 负增量让"读进多余字节再回退"免费。[模式: 直写 + 手动记账]
数据流: read 事件 → NonGreedy 扩容 → read 直写 querybuf → sdsIncrLen(nread) → 解析 → sdsIncrLen(-2) 去 CRLF。

### 8. 复用三件 — 清空留缓冲 / 免清零 / 分配器互通

场景: AOF 每轮写缓冲怎么复用?大缓冲创建为什么不用清零?Lua 怎么和 SDS 分同一个内存池?
源码路径:
- `sds.c:200-203` — sdsclear: `sdssetlen(s, 0); s[0]='\0'` — **len 归零, alloc 不动** (注释: "all the existing buffer...set as free space")
  - 消费: `aof.c:1216` `sdsclear(server.aof_buf)` — **AOF 写缓冲每轮复用**
- `sds.c:97-98` — SDS_NOINIT: 跳过 memset (免清零, 创建即覆盖) — aof/config/networking/object 使用
- `sds.h:256-258` — 导出分配器 sds_malloc/realloc/free: 宿主程序 (Lua) 与 SDS 分配器互通 (谁分配谁释放)
关键设计: 省一次操作 (q8): 清空复用 (AOF 高频循环) / 免清零 (大缓冲) / 分配器互通 (跨语言边界)。[模式: 生命周期微优化]
数据流: AOF flush 后 → sdsclear (缓冲复用) → 下轮 append 免分配。

### 负面空间 — SDS 刻意不做的事

- **不做惰性拷贝**: sdscpy 直接覆盖 (无写时复制/引用计数) — 需要时 sdsdup 显式复制
- **不做内存池**: 不自行缓存释放的块 (分配直接走 R-33/分配器, jemalloc 已有 tcache)
- **不做自动压缩**: sdsclear 后 alloc 保留 (要紧凑得显式 sdsResize/sdsRemoveFreeSpace)
- **不做编码检测**: 内容纯字节, 不识别字符集 (编码是上层的事)
- **不做线程安全**: sds 操作无锁 (Redis 主线程单写, R-33 的原子只是记账)

→ 引出: Dict 的键是 sds — 键哈希 (siphash) 与键值存取如何建立 → [[R-3-Dict]]
