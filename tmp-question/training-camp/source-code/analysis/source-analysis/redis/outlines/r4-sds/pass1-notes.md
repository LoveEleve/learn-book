# R-4 SDS — Pass 1 探索笔记

> 域: R-4 SDS (动态字符串) | 🔴 方案 A | 2026-08-13
> 源码: src/sds.c (1473) + sds.h (264) | Redis 7.4.2
> 已读测试: sdsTest (sds.c:1226-1473, **50 个 test_cond 断言**: 创建/拼接/裁剪/range/cmp/repr/split/join/printf/模板)

## 继承树/调用图

```
sds = char* (sds.h:20) — 指针即对象, 头部在 buf 前 (s[-1] = flags)

5 种 header (sds.h:22-51, 全部 __packed__):
  sdshdr5:  flags(3bit type + 5bit len, 上限 31) + buf[]    [从不作 struct 用, 直接访问 flags]
  sdshdr8:  uint8 len + uint8 alloc + flags + buf[]         [1<<8-1]
  sdshdr16: uint16 len + uint16 alloc + flags + buf[]       [1<<16-1]
  sdshdr32: uint32 ...                                      [1<<32-1]
  sdshdr64: uint64 ...                                      [LONG_MAX==LLONG_MAX 才用]

inline 访问族 (sds.h:64-193, 零成本 switch):
  sdslen/sdsavail/sdsalloc/sdssetlen/sdsinclen/sdssetalloc — 读 s[-1] flags 分派

创建 (sds.c:81-164):
  _sdsnewlen(init, initlen, trymalloc):
    sdsReqType(initlen) 选型 (31/255/65535/2^32 阈值, L40-52)
    空串强制 SDS_TYPE_8 (L87: "type 5 is not good at this")
    s_malloc_usable(hdrlen+initlen+1, &usable) ← R-33 联动!
    usable-hdrlen-1 → alloc 字段 (免费膨胀, 上限 sdsTypeMaxSize)
    SDS_NOINIT 不 memset; 总是 \0 结尾 (L142)

扩容 (L217-268) _sdsMakeRoomFor(s, addlen, greedy):
  1. avail >= addlen → 原地返回
  2. greedy: <1MB → newlen*2; ≥1MB → newlen+1MB (SDS_MAX_PREALLOC, sds.h:13)
  3. oldtype==type → s_realloc_usable (原地, 头部不变)
  4. 类型升级 → s_malloc_usable + memcpy + s_free (头部变了不能 realloc!)
  5. usable 再利用 → alloc=usable

缩容 (L287-344) sdsResize(s, size, would_regrow):
  use_realloc = (oldtype==type || (type<oldtype && type>SDS_TYPE_8)) — 缩小到 8 以上可 realloc
  jemalloc 优化: je_nallocx(newlen)==zmalloc_size(sh) → 分配大小已最优, 跳过 realloc (L332-338)
  would_regrow → 禁止降到 5 (预期再增长)

追加 (L463+): sdscatlen → sdsMakeRoomFor + memcpy + sdssetlen — 增量路径
零拷贝: sdsIncrLen (L399+, 注释 L385-397: read 直写 + 手动 incr — networking querybuf 模式)
复用: sdsclear (L200-203: len=0 保留缓冲)
导出分配器: sds_malloc/sds_realloc/sds_free (sds.h:256-258, 宿主程序兼容)
```

## 基本元素分解

1. **指针即对象**: sds = char*, 头部信息内嵌 buf 前 (s[-1] flags) — C 字符串兼容零成本
2. **分级 header**: 5 种类型按长度阈值 — 空间最优 (小串 1-2 字节头)
3. **扩容策略**: 原地/realloc/malloc+memcpy+free 三路 + greedy 预分配 + usable 联动
4. **缩容策略**: sdsResize (realloc 条件 + jemalloc nallocx 优化)
5. **访问族**: inline switch (len/avail/alloc get/set)
6. **双标准**: 总 \0 结尾 (printf 兼容) + len 字段 (二进制安全)
7. **零拷贝/复用**: sdsIncrLen / sdsclear

## 标记问题 (10 个)

1. sds = char* 的"指针即对象"设计 — s[-1] 读 flags 的代价与收益? 为什么不用结构体指针?
2. 5 类型阈值 (31/255/65535/2^32) — sdshdr5 为什么 1 字节 (5 bit len)? 空串为什么强制 8?
3. 扩容三路 (原地/realloc/升级 malloc+memcpy+free) — 为什么头部变化不能 realloc?
4. greedy 预分配 1MB 分界 (2× vs +1MB) — 为什么? NonGreedy 谁用?
5. usable 联动 (R-33) — alloc=usable 免费膨胀在 SDS 的具体体现? sdsResize 的 je_nallocx 优化?
6. 二进制安全 + C 兼容双标准 — \0 结尾 vs len 字段, 冲突怎么处理?
7. sdsIncrLen 零拷贝追加模式 — networking querybuf 的消费方式?
8. sdsclear 缓冲复用 — 为什么保留 alloc?
9. SDS_NOINIT 使用方 (aof/config/networking/object)?
10. 导出分配器 (sds_malloc/realloc/free) 的动机?

## 时空溯源 (代码内痕迹)

- SDSLib 2.0, 版权 2006-Present — Redis 最老组件 (比 zmalloc 还早, 2006 antirez)
- sdshdr5 注释 "never used...just access the flags byte" — 历史设计残留 (早期有 5 类型, 后 5 退化为标志)
- sdsResize/NonGreedy (3.2+): 缩容重构 + 非贪心追加 (AOF 用)
- SDS_NOINIT: 免 memset 优化 (大缓冲)
- 导出分配器: Lua 宿主集成 (R-30 连接)
