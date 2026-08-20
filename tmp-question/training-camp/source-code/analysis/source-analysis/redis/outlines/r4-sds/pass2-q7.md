# 闭环笔记 q7: sdsIncrLen 零拷贝追加 — querybuf 的 read 直写模式

## 假设
sdsIncrLen 允许"先扩容 → read 直写 → 手动递增 len"的零拷贝追加 — networking 的查询缓冲 (querybuf) 是主要消费者。

## 验证过程
- sds.c:385-397 (注释): 经典模式 — `oldlen = sdslen(s); s = sdsMakeRoomFor(s, BUFFER_SIZE); nread = read(fd, s+oldlen, BUFFER_SIZE); sdsIncrLen(s, nread);` — **内核直接写进 sds 缓冲, 免中间拷贝**
- sds.c:399-440 (sdsIncrLen): switch 分派 + **断言守卫**: `assert((incr >= 0 && sh->alloc-sh->len >= incr) || (incr < 0 && sh->len >= (unsigned int)(-incr)))` — 正增量必须 ≤ avail (防越界写), 负增量必须 ≤ len (防下溢)
- 消费实证 (networking.c):
  - L2727: `sdsIncrLen(c->querybuf, nread)` — read 后递增
  - L2431: `sdsIncrLen(c->querybuf,-2)` — **负增量: 去 CRLF** (解析后回退)
- 配套: querybuf 用 NonGreedy 扩容 (q4) — 读多少扩多少
- 安全面: 断言而非静默截断 — 越界立即崩溃 (开发期暴露 bug)

## 代码类型
Algorithmic (零拷贝模式) + Implementation (守卫)

## 跨域关联
- R-28 (networking.c:2431,2727 querybuf) → 主消费方
- R-8 (aof.c 写缓冲) → 同类模式
- q4 (NonGreedy) → querybuf 的扩容策略配套

## 结论
sdsIncrLen = "内核直写 sds" 的零拷贝桥: MakeRoomFor 扩好空间 → read 直接落进 buf → 手动递增 len (含负向回退去 CRLF)。断言守卫保证正/负增量都不越界。这是 SDS 面向网络场景的杀手功能 — querybuf 全程零中间拷贝。
源码位置: sds.c:385-440; networking.c:2431,2727
