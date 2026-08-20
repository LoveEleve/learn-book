# R-24 字符串命令 — SET/INCR 族与编码链

> 前置: [[R-1-object]] (INT/EMBSTR/RAW 编码链) + [[R-21-db]] (setKey/键空间) + [[R-4-SDS]] (二进制安全) + [[R-28-networking]] (addReply) | 引出: [[R-11-bitmap]] (bit 操作) + [[R-9-replication]] (传播) | 对照: [[R-22-expire]] (TTL 传播归一)
> 🟡 B | 6 KP | [模式: 标志矩阵+传播归一+原地优化+三层防护]
> Pass 2 闭环: q1(SET 标志) q2(传播重写) q3(INCR 原地) q4(范围命令) q5(GETEX 三路径) q6(LCS 防护)

**读者处境**: SET key 100 为什么比 SET key "abc" 省内存?INCR 每秒百万次怎么做到零分配?SET key v GET 的旧值怎么返回还能传播?为什么 INCRBYFLOAT 会变成一条 SET?这篇拆字符串命令: SET 标志矩阵、传播归一、INCR 原地优化、范围命令、LCS。

### 1. SET 标志矩阵 — 9 位域与互斥校验

场景: SET 的 NX/XX/GET/KEEPTTL/EX 等怎么组合?
源码路径:
- 标志位 (t_string.c:49-58): NX(1<<0)/XX(1<<1)/EX(1<<2)/PX(1<<3)/KEEPTTL(1<<4)/GET(1<<5)/EXAT(1<<6)/PXAT(1<<7)/PERSIST(1<<8)
- **解析期互斥** (L188-270): NX×XX (L197,202) / KEEPTTL×TTL 族 (L211-213) / TTL 族互斥 (L224-226) — 互斥时后出现者落入 else 报 **syntaxerr** (L265-266); 非法选项同报
- 命令族分域: NX/XX/GET/KEEPTTL 仅 SET (L197,208,213); PERSIST 仅 GETEX (L216)
- 执行面 (L78-85): `NX && found` 或 `XX && !found` → abort (GET 时仍返回旧值)
- setkey_flags 组装 (L88-89) → R-21 setKey 四态路由
关键设计 (q1): 位域 + **解析期互斥** = 错误提前 (语法面), 执行期只消费标志。[模式: 标志矩阵]
数据流: SET 参数 → 解析互斥 → 标志位 → setGenericCommand → setKey 四态。

### 2. 传播归一 — PXAT 与 GET 剥离

场景: SET 的传播面怎么保证主从一致?
源码路径:
- setGenericCommand (L63-129): NX/XX 检查 (L78-85) → setKey (L91) → **expire 重写 PXAT** (L99-102: SET key val PXAT <绝对毫秒>)
- **GET 剥离** (L111-128): 传播 argv 去掉 GET 参数 (L119-123) — 从库不需要"返回旧值"
- GETSET (L410-419): setKey + 重写 SET (L418)
- SETEX/PSETEX (L292-300): 固定标志
关键设计 (q2): **传播最小化**: 值 + 绝对时间戳 (主从时钟无关, 同 R-22 PEXPIREAT 哲学); GET 是执行面语义不传播。[模式: 传播归一]
数据流: SET k v EX 60 → setExpire → 重写 SET k v PXAT <ts> → AOF/从库。

### 3. INCR 原地优化 — 四条件零分配

场景: 计数器每秒百万自增, 怎么避免每次分配?
源码路径:
- incrDecrCommand (L580-614): 溢出检查 (L589-593, 符号对齐才查)
- **原地更新四条件** (L596-598): o 存在 && refcount==1 && INT 编码 && (value<0 || value≥10000) && LONG 范围 → `o->ptr = value` (L601, 零分配)
  - 共享池对象 (0-9999) 不可变 → 走新建 (R-1)
- 溢出特判: DECRBY LLONG_MIN (L636-639, 取负溢出)
- **INCRBYFLOAT** (L643-674): long double + NaN/Inf 拒 (L654-657) → **重写 SET KEEPTTL** (L671-673, 浮点格式化差异不传播)
关键设计 (q3): **高频路径零分配** (独占 INT 对象直接改 ptr); 浮点命令因精度差异改走 SET。[模式: 原地优化]
数据流: INCR → 溢出查 → 四条件? → ptr 直改 / createStringObjectFromLongLongForValue。

### 4. 范围命令 — 零填充与负索引

场景: SETRANGE 越界怎么写?GETRANGE 负索引怎么算?
源码路径:
- checkStringLength (L19-31): proto_max_bulk_len 上限 + **加法溢出检测** (uint64 转换, L23-26)
- setrangeCommand (L421-479): offset<0 拒 → 不存在 sdsnewlen 分配 (L446) / 存在 dbUnshare (L467, 共享保护) → **sdsgrowzero 零填充** (L471, 空位填零) + memcpy
- getrangeCommand (L481-520): **INT 解码免分配** (L494-496, ll2string 栈 buf) → 负索引归一 (L503-511: len+start, 钳 0, end≥len→len-1) → 边界 (L515-519)
关键设计 (q4): SETRANGE = **原地零填充扩容**; GETRANGE = **负索引双向归一** + INT 免分配解码。[模式: 范围操作]
数据流: SETRANGE k 5 "abc" → 扩到 8 字节零填 → 写 "abc"; GETRANGE -3 -1 → 尾部 3 字节。

### 5. GETEX/GETDEL — 读改写三路径

场景: 读一个值还能顺便改 TTL?
源码路径:
- getexCommand (L340-397): 解析 (COMMAND_GET 域) → lookup → **先校验 expire 再回复** (L358-365) → 三路径:
  1. **PXAT 已过期** (L369-378): 删 + 重写 DEL/UNLINK
  2. 有 expire (L379-388): setExpire + 重写 PEXPIREAT
  3. PERSIST (L389-396): removeExpire + 重写 PERSIST
- getdelCommand (L399-408): 读 + dbSyncDelete + 重写 DEL
- 共同: "never propagated as is" (L367-368) — 统一改写
关键设计 (q5): **读改写合一**: 值先返回, TTL 三路径各自重写单一传播命令 (同 R-22 归一)。[模式: 读后副作用]
数据流: GETEX k PX 100 → 返回值 → setExpire → 重写 PEXPIREAT。

### 6. LCS — DP 三层防护

场景: 最长公共子串怎么算?内存怎么防?
源码路径:
- 选项 (L741-767): LEN/IDX/WITHMATCHLEN/MINMATCHLEN; LEN+IDX 互斥 (IDX 含 LEN)
- **三层防护** (L770-799): UINT32_MAX-1 长度限 (L770, 表索引安全) → 表大小乘法双重检查 (L789) → **512MB 内存限** (L790, proto_max_bulk_len) → **ztrymalloc 降级** (L794, 分配失败给错误, R-33)
- DP 主循环 (L802-823): LCS[i][j] 三态; 回溯 (L848-905): 匹配区间聚合
关键设计 (q6): DP O(n×m) 空间 = **瞬态大内存** → 三层防护 (长度/内存/分配降级)。[模式: DP+防护]
数据流: LCS a b → 选项 → 表分配 (防护) → DP 填表 → 回溯 → 回复。

### 负面空间 — 字符串命令刻意不做的事

- **不做字符串截断**: 值按 proto_max_bulk_len 全量拒绝, 无静默截断
- **不做 INCR 浮点落库**: INCRBYFLOAT 用 long double 运算后存十进制串 (无二进制浮点精度损失)
- **不做 GETRANGE 惰性**: 总是复制返回 (无引用计数共享切片)
- **不做 SET 返回值缓存**: GET 旧值每次新分配 (无缓存)
- **不做 LCS 优化变体**: 纯 DP (无 Myers/位并行), 靠防护而非算法

→ 引出: 位操作命令怎么扩展字符串?→ [[R-11-bitmap]]
