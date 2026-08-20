# 闭环笔记 q4: greedy 预分配 — 1MB 分界的空间-时间权衡

## 假设
增量追加时贪心预分配 (<1MB → 2×, ≥1MB → +1MB) 避免反复 realloc; 1MB 分界是"倍增成本 vs 浪费"的权衡点; NonGreedy 是只读路径的例外。

## 验证过程
- sds.h:13: `SDS_MAX_PREALLOC (1024*1024)` — 1MB 分界常量
- sds.c:232-237 (_sdsMakeRoomFor): `if (greedy==1) { if (newlen < SDS_MAX_PREALLOC) newlen *= 2; else newlen += SDS_MAX_PREALLOC; }`
- 代价模型: 倍增 (×2) 让平均追加成本 O(1) 摊还, 但大串倍增浪费多 (1GB→2GB); 线性 (+1MB) 在大串时浪费固定 — **1MB 以下倍增 (摊还最优), 1MB 以上线性 (浪费封顶)**
- **NonGreedy (sdsMakeRoomForNonGreedy, L277-279: greedy=0)**: 只扩到刚够 (newlen = len+addlen) — **使用方: networking.c:2401,2698 (querybuf 读取)** — 客户端查询缓冲按需读取, 不贪心 (防止恶意客户端撑爆预分配)
- sdscatlen (L463-472) 默认走 greedy=1 — 增量追加路径
- 摊还分析: 连续 n 次 append 1B, greedy 下 realloc 次数 O(log n); NonGreedy 下 O(n) — 但 querybuf 每次读固定块 (readlen = PROTO_IOBUF_LEN 16KB, server.h:164; 部分场景 remaining/MASTER 扩容 networking.c:2667-2687), 增长模式是"大步长"非"1B 步"

## 代码类型
Algorithmic (摊还策略) — 经典 2× vs +C 权衡

## 跨域关联
- R-28 (networking.c:2401,2698 querybuf) → NonGreedy 消费方 (读 socket 固定块)
- R-8 (aof.c aof_buf) → sdsclear 复用 (q8)
- R-1 (object.c) → 字符串对象追加 (APPEND 命令)

## 结论
greedy 双策略: <1MB 倍增 (摊还 O(1)/次), ≥1MB 线性 +1MB (大串浪费封顶 1MB)。NonGreedy 是"固定块读取"路径 (querybuf) 的按需版 — 客户端可控增长场景禁用贪心, 防恶意膨胀。
源码位置: sds.c:232-237,277-279; networking.c:2401,2698
