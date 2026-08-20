# R-19 listpack — 紧凑编码: 长度放在自己身上, 告别级联更新

> 前置: [[R-33-zmalloc]] (分配) + [[R-4-SDS]] (内容来源) | 引出: [[R-7-intset]] (另一条紧凑路线) | 对照: [[R-3-Dict]] (哈希链 vs 紧凑数组) + [[R-25-hash]] (消费域)
> 🔴 A | 8 KP | [模式: 自描述布局+前缀编码+统一写路径]
> Pass 2 闭环: q1(无级联) q2(编码族) q3(嗅探) q4(写路径) q5(遍历) q6(批量) q7(惰性) q8(消费)

**读者处境**: HSET 一个小 hash, 它存在哪里?为什么 100 个字段的小 hash 只占几 KB?为什么 Redis 7.0 把用了几年的 ziplist 整个换掉?zset 的 score 为什么能存成 2 字节?这篇拆 listpack: 紧凑布局、9 种编码、以及"把长度放在自己身上"的设计哲学 — 它消灭了 ziplist 最痛的级联更新。

### 1. 布局与 backlen — 长度放自己身上

场景: 每个元素后面那 1-5 字节是什么?为什么插入不会引起连锁反应?
源码路径:
- `listpack.c:22-27` — 头部 6B: 4B total_bytes + 2B num_elements; 末尾 0xFF EOF
- `listpack.c:869-872` (lpInsert) — `backlen = lpEncodeBacklen(backlen, enclen)` — **每个 entry 尾部存"自身长度"** (1-5B, LP_MAX_BACKLEN_SIZE=5)
- `listpack.c:473-482` (lpPrev) — 向后遍历: 当前 entry 前 1 字节 = 前驱 backlen 末尾 → 解码 → 跳回 — O(1)
- **ziplist 对照** (ziplist.c:55-69): prevlen **存前驱长度**且 1B (0-253)/5B (0xFE) 可变 → 前驱变长可能 1B→5B → 自身变长 → 再影响下一个 — **级联传播** (ziplist 的 __ziplistCascadeUpdate)
关键设计: 无级联 (q1): backlen 存自身长度 + 编码空间固定 (5B 足够任何 ≤2^32 长度) — 插入/替换变长**只改头部计数**, 后驱零影响; "长度放自己身上, 不给邻居留负担" 是 listpack 对 ziplist 的根本改进。[模式: 自描述布局]
数据流: lpInsert 新元素 → 写自身 backlen → 头部 total_bytes/num_elements 更新 → 完成 (无传播)。

### 2. 编码族 — 9 种前缀分层编码

场景: 2 字节能存 127 以内的整数?字符串头最小 1 字节?
源码路径:
- `listpack.c:30-82` — 编码族:
  - 整数: 7BIT (0x00, entry 2B) / 13BIT (0xC0, 3B) / 16BIT (0xF1, 4B) / 24BIT (0xF2, 5B) / 32BIT (0xF3, 6B) / 64BIT (0xF4, 10B)
  - 字符串: 6BIT (0x80, 1B 头, ≤63B) / 12BIT (0xE0, 2B 头) / 32BIT (0xF0, 5B 头)
  - EOF = 0xFF (编码空间之外)
- `listpack.c:434-446` (lpCurrentEncodedSizeBytes) — 解码侧按首字节前缀判定
- **前缀位分层** (无歧义): `0`→7BIT_INT, `10`→6BIT_STR, `110`→13BIT_INT, `1110`→12BIT_STR, `1111 0000-0100`→32BIT_STR/16/24/32/64BIT_INT
关键设计: 前缀编码 (q2): 首字节高位模式决定编码 → 解码零歧义; 整数 0-127 只要 2B, 短字符串头 1B — 小值场景空间最优。[模式: 前缀分层]
数据流: 编码选择 (按值域/长度) → 首字节前缀 + 数据 + backlen。

### 3. 整数嗅探 — 字符串当整数存

场景: HSET myhash count 5 — 那个 "5" 存成了什么?
源码路径:
- `listpack.c:850-860` (lpInsert) — `lpEncodeGetType(elestr,size,intenc,&enclen)` — 字符串先嗅探
- `listpack.c:317` (lpEncodeGetType) — 成功解析 → 按值域选整数编码并就地编码; 失败 → 字符串编码
- `listpack.c:154-179` (lpStringToInt64, 移植自 utils.c string2ll, 2011) — **严格语义**: 无空格/无前导零 (除 "0")/int64 范围 — 保证整数↔字符串无损往返
- 收益: "5" → 7BIT_INT (2B) vs 6BIT_STR (1B 头+1B 数据+backlen)
关键设计: 类型嗅探 (q3): 写入时尝试"字符串→整数"编码 — 数字字符串大省空间; 严格解析保证读回时字符串原样还原。[模式: 无损嗅探]
数据流: HSET "count" "5" → lpStringToInt64 成功 → 7BIT_INT 编码 → GET 时转回 "5"。

### 4. 三合一写路径 — 一个函数管所有写

场景: 插入/删除/替换为什么是一个函数?内存怎么搬?
源码路径:
- `listpack.c:821-968` (lpInsert):
  - L828-833: 删除 = 替换为零长元素 (where 强制 REPLACE)
  - L835-843: **LP_AFTER 转 LP_BEFORE** (跳到下个元素) — 统一为两个 case
  - L881-883: 新总长 = 旧 + 新 - 被替换; **UINT32_MAX 上限检查** (+ LISTPACK_MAX_SAFETY_SIZE=1GB 提前拦, L122-128)
  - L893-914: **扩先 realloc 后 memmove / 缩先 memmove 后 realloc** (避免越界写)
- `#if 0` 调试块 (L948-965): 强制新指针 — 抓"忘记更新引用"的调用方 bug
关键设计: 统一写路径 (q4): 删除=替换零长, AFTER=跳转 — 一个函数覆盖全部写操作, 少 API 面少 bug 面; realloc/memmove 顺序按"扩先缩后"编排。[模式: 操作合并]
数据流: 插入 → 编码准备 → 空间计算 → realloc/memmove → 写入 → 头部更新。

### 5. 双向遍历与批量 — O(1) 后退 + 单次搬迁

场景: 从尾部往前遍历怎么走?批量构建为什么快?
源码路径:
- `listpack.c:473-482` (lpPrev) — 前驱 backlen 紧贴当前 entry → 解码跳回 — O(1)
- `listpack.c:452-457` (lpSkip) — 前进 = 自身长度 + 自身 backlen
- `listpack.c:993-1080` (lpBatchInsert) — **单次 realloc + 单次 memmove** (逐元素是 N 次); 栈上 3 元素缓冲 (超量才堆分配)
关键设计: 双向 O(1) + 批量摊还 (q5/q6): backlen 让后退遍历免扫描; 批量构建从 O(N²) 复制降到 O(N) — hash 转换/复制场景关键。[模式: 自描述双向 + 批量摊还]
数据流: lpPrev: p-- → 解码 → 跳回; lpBatchInsert: 预编码 N → 单次搬移。

### 6. 惰性计数与完整性 — 头部缓存 + 畸形防御

场景: 元素超 65535 个怎么计数?RDB 加载损坏数据怎么办?
源码路径:
- `listpack.c:27,505-521` — num_elements 16bit; 超 65535 (LP_HDR_NUMELE_UNKNOWN) → **全扫描计数 + 回填**
- `listpack.c:1541-1553` (lpValidateIntegrity) — 头部大小可读 / **total_bytes == 实际 size** (防伪造头) / 末字节 EOF / deep 模式逐元素校验
关键设计: 惰性缓存 + 校验 (q7): 计数头部是"尽力而为" (超限降级扫描); 完整性双保险防 RDB/网络畸形数据引发越界。[模式: 惰性缓存 + 校验]
数据流: RDB 加载 → lpValidateIntegrity → 校验通过才可用。

### 7. 消费场景 — 小 hash/zset 的默认编码

场景: 什么时候用 listpack?什么时候转 dict?
源码路径:
- `config.c:3215-3223` — **hash-max-listpack-entries=512 / hash-max-listpack-value=64B; zset-max-listpack-entries=128 / zset-max-listpack-value=64B** (旧名 ziplist 兼容)
- `t_hash.c:605,893,932` — 插入后超阈值 → hashTypeConvert (listpack→dict, **单向**)
- `t_zset.c:1101-1108` — ele+score 成对 (score 常整数编码); `t_stream.c` — 消息存储
关键设计: 小规模默认编码 (q8): listpack 线性查找 O(N) 在 ≤512 可接受, 紧凑内存收益大; 超阈值转 dict O(1) 查找 — 空间/时间的两阶段策略。[模式: 阈值转换]
数据流: HSET → 超 512 字段? → 转换 dict : 继续 listpack。

### 负面空间 — listpack 刻意不做的事

- **不做 O(1) 随机访问**: 线性扫描定位 (seek O(N)) — 小规模专用
- **不做级联安全**: 根本无级联 (vs ziplist 有级联但维护)
- **不做原地更新**: 变长元素 = realloc+memmove 整体搬移 (无 in-place)
- **不做压缩**: 无 LZF 压缩层 (对照 quicklist 的 LZF, R-5)
- **不存储任意结构**: 只存字符串/整数 (无嵌套)

→ 引出: intset 是另一条紧凑路线 — 有序整数数组 + 升级 → [[R-7-intset]]
