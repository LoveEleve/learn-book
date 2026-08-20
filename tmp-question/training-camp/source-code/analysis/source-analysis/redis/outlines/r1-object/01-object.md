# R-1 redisObject — 统一外壳: 16 字节承载一切, 编码降级到零分配

> 前置: [[R-33-zmalloc]] + [[R-4-SDS]] + [[R-19-listpack]] + [[R-7-intset]] + [[R-6-zset]] + [[R-5-quicklist]] (全部编码) | 引出: [[R-20-server]] (键空间宿主)
> 🔴 A | 8 KP | [模式: 统一外壳+编码降级链+共享池+引用计数]
> Pass 2 闭环: q1(外壳) q2(EMBSTR) q3(INT) q4(优化链) q5(引用) q6(共享池) q7(解码) q8(可观测)

**读者处境**: SET count 5 之后, 这个 5 在内存里是什么?为什么小字符串和对象"住同一个房间"?为什么 10000 以下的整数大家共用同一个对象?OBJECT ENCODING 命令怎么知道答案?这篇拆 redisObject: 16 字节外壳、三态字符串编码、共享对象池, 以及"写入时降级"的优化哲学。

### 1. 十六字节外壳 — 一切皆对象

场景: 每个值都背着什么?为什么 16 字节够用?
源码路径:
- `server.h:903-911` — `struct redisObject { unsigned type:4; unsigned encoding:4; unsigned lru:LRU_BITS(24); int refcount; void *ptr; }` — **16 字节**
- `server.h:896` — LRU_BITS=24: lru 字段双用途 (LRU 时钟 / LFU 频率+时间)
- `server.h:901-902` — OBJ_STATIC_REFCOUNT (栈上) / OBJ_SHARED_REFCOUNT (不可变共享)
关键设计: 位域压缩 (q1): type+encoding 共享 1 字节 (各 4bit), lru 24bit 双模式 — 百万对象省 MB 级; 特殊 refcount 值让"共享/栈上"零维护。[模式: 紧凑外壳 + 特殊值编码]
数据流: 任何值 → createObject → 16B 外壳 + ptr 指向数据。

### 2. EMBSTR — 对象与字符串同房

场景: 为什么小字符串的 44 是"arena 数学"?
源码路径:
- `object.c:71-93` (createEmbeddedStringObject) — `zmalloc(sizeof(robj)+sizeof(sdshdr8)+len+1)` — **robj+sds头+内容一次分配**
- `object.c:99-101` — "The current limit of 44 is chosen so that the biggest string object we allocate as EMBSTR will still fit into the 64 byte arena of jemalloc" — **16+3+44+1=64B 恰好 64B 桶**
- `object.c:102-107` — createStringObject: ≤44 → EMBSTR / 否则 RAW
关键设计: 同 chunk 分配 (q2): 一次 malloc + 对象与数据同缓存行 (访问 robj 即带出内容); 不可变是代价 (无预分配, 追加转 RAW)。[模式: 单次分配 + 缓存对齐]
数据流: SET 短串 → ≤44B? → robj+sds 同 chunk → 缓存友好。

### 3. INT 编码 — 指针就是值

场景: SET count 5 — 这个 5 有数据吗?
源码路径:
- `object.c:128-140` (createStringObjectFromLongLongWithOptions) — `value < 10000 → shared.integers[value]` (共享); 否则 `encoding=INT; ptr=(void*)value` — **零分配 (外壳即全部)**
- `object.c:159,636-643` — **maxmemory 下禁共享整数** (创建路径 L159 / 优化链 L636-643; 注释: 每个对象需私有 LRU 字段)
关键设计: 数值零分配 (q3): INT 编码把值直接塞进指针槽; 0-9999 连外壳都共享; maxmemory 的代价是共享整数无法跟踪 LRU → 禁共享。[模式: 指针复用 + 条件共享]
数据流: SET count 5 → <10000? → shared.integers[5] (0 分配) : INT 对象 (16B)。

### 4. 优化链 — 写入时降级

场景: SET "12345" 之后发生了什么?什么时候变 INT/EMBSTR?
源码路径:
- `object.c:607-683` (tryObjectEncodingEx) — 链:
  - L612-621: 仅字符串 + 仅 RAW/EMBSTR + **refcount>1 不编码** (共享不可变)
  - L625-650: `len <= 20 && string2l` → 共享整数或 INT
  - L657+: `len <= 44` → RAW 转 EMBSTR
关键设计: O(1) 降级链 (q4): 写入时一次检查走到底 — INT (免分配收益最大) 优先, EMBSTR (缓存) 次之; refcount>1 守卫保护共享对象。[模式: 写入时优化]
数据流: SET "12345" → string2l 成功 → INT 编码 (零分配); SET "hello" → ≤44 → EMBSTR。

### 5. 引用计数 — 谁用谁持

场景: 一个对象被两个 key 引用?共享对象为什么不计数?
源码路径:
- `object.c:349-359` (incrRefCount) / `L361-377` (decrRefCount) — 三态: **1→分派释放** (freeStringObject 等) / >1→递减 / 特殊值不碰
- `object.c:56-60` (makeObjectShared) — refcount = OBJ_SHARED_REFCOUNT (断言原 1)
关键设计: 三态生命周期 (q5): 共享对象 (池) 永久存活免维护 (线程安全); 栈上对象禁止 incr (panic); 释放按类型分派 (R-5/R-6/R-19 的 free 面)。[模式: 引用计数 + 特殊值]
数据流: 命令取参 → incr → 用毕 → decr → 归零分派释放。

### 6. 共享池 — 10000 整数 + 响应串

场景: 为什么 +OK 不会重复分配?0-9999 为什么常驻?
源码路径:
- `server.c:1847+` (createSharedObjects) — 响应串族: ok/emptybulk/czero/cone/emptyarray/pong + 错误族 (wrongtypeerr/oomerr/execaborterr...) + 特殊串
- `server.c:1992-1995` — `shared.integers[j] = makeObjectShared(createObject(..., (void*)j)); encoding=INT` — **0-9999 全共享 (160KB)**
- `server.h:108` — OBJ_SHARED_INTEGERS=10000
关键设计: 启动常驻池 (q6): 高频对象 (计数/索引/协议串) 回复路径零分配; OBJ_SHARED_REFCOUNT 让池对象免计数。[模式: 启动共享池]
数据流: 命令回复 → shared.ok (零分配); INCR 结果 5 → shared.integers[5]。

### 7. 解码与 LRU — 按需临时 + 双用途字段

场景: INT 对象怎么读成字符串?lru 字段怎么两用?
源码路径:
- `object.c:685-697` (getDecodedObject) — sds → incrRefCount 直返; INT → ll2string 临时缓冲
- `object.c:706-715` — 比较优化: INT 对象直接比值 (免解码)
- `object.c:32-43` (initObjectLRUOrLFU) — LRU: 分钟时钟; LFU: `(时间 << 8) | 频率`; **共享对象跳过** (无 lru)
关键设计: 按需解码 + 双用途 (q7): 解码不缓存 (INT 的字符串形态临时生成); 比较路径免解码; lru 24bit 在 LRU/LFU 两模式下各取所需。[模式: 按需转换 + 字段复用]
数据流: GET count → INT → ll2string 临时 → 回复; maxmemory LRU → 读 lru 时钟。

### 8. OBJECT 命令 — 内部可观测

场景: OBJECT ENCODING 怎么知道编码?排查内存怎么用?
源码路径:
- `object.c:1442+` (objectCommand) — ENCODING (编码名) / REFCOUNT (引用数) / IDLETIME (lru 空闲) / FREQ (LFU 频率); TYPE 在 db.c:1336
关键设计: 可观测面 (q8): 编码状态/共享情况/淘汰状态全暴露 — 调试"为什么我的 key 不是 int 编码"的入口。[模式: 内部状态命令]
数据流: OBJECT ENCODING key → 编码名 → 排查内存。

### 负面空间 — redisObject 刻意不做的事

- **不做可变 EMBSTR**: 同 chunk 不可变 (追加即转 RAW)
- **不做共享对象修改**: OBJ_SHARED_REFCOUNT 不可变 (修改前必须复制)
- **不做解码缓存**: INT 的字符串形态每次临时生成 (免长期占用)
- **不做对象池复用**: 释放后不回收外壳 (新建即分配)
- **不做类型系统扩展**: 只有内置 7 类 + Module (无用户自定义类型)

→ 引出: server 骨架是键空间的宿主 — robj 们的容器与生命周期编排 → [[R-20-server]]
