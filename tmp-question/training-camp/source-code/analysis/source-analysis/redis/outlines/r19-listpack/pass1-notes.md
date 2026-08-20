# R-19 listpack — Pass 1 探索笔记

> 域: R-19 listpack (紧凑列表编码) | 🔴 方案 A | 2026-08-13
> 源码: src/listpack.c (3150) + listpack.h (95) | Redis 7.4.2
> 已读测试: listpackTest (listpack.c:1896+, REDIS_TEST)

## 继承树/调用图

```
布局 (LP_HDR_SIZE=6): [4B total_bytes][2B num_elements] [entry]* [0xFF EOF]
entry 布局: [编码字节+数据][backlen(自身长度, 1-5B)]

编码族 (listpack.c:22-82):
  整数: 7BIT(0x00, 2B总) / 13BIT(0xC0, 3B) / 16BIT(0xF1, 4B) / 24BIT(0xF2, 5B) / 32BIT(0xF3, 6B) / 64BIT(0xF4, 10B)
  字符串: 6BIT(0x80, 1B 长度前缀) / 12BIT(0xE0, 2B) / 32BIT(0xF0, 5B)
  EOF = 0xFF

核心操作:
  lpNew (L220): 6B 头 + EOF
  lpAppend/lpPrepend/lpInsert 族 → lpInsert (L821-968) 三合一 (插入/删除/替换)
    - delete → where 强制 REPLACE; LP_AFTER → 跳下个转 LP_BEFORE (L835-843)
    - lpEncodeGetType: 字符串尝试整数编码 ("123" → 7BIT_INT) (L859)
    - backlen = 自身长度编码 (L872)
    - 内存: 一次 realloc + 一次 memmove (扩先 realloc 后 memmove / 缩先 memmove 后 realloc, L893-914)
    - 头部更新: num_elements ± 1 / total_bytes (L937-946)
    - #if 0 调试块: 强制新指针找忘记更新的 bug (L948-965)
  lpBatchInsert (L993+): 批量单次 realloc/memmove
  遍历:
    lpNext (L462-468) = lpSkip (enc+data+backlen)
    lpPrev (L473-482): 当前 entry 前 1 字节 = 前驱 backlen 末尾 → 解码前驱长度 → 跳回
    lpFirst/lpLast (L486-498)
  lpGet/lpGetIntegerValue (L515+): 解码
  lpSeek: 索引定位
  lpFind: 线性查找 (hash 小规模用)
  完整性: lpValidateIntegrity/lpValidateFirst/lpValidateNext

消费方 (跨域):
  t_hash.c:887-888 (HSET 小 hash), 1568 (TTL 标记), 1709 (复制)
  t_zset.c:1101-1108 (zset 小规模: ele+score 成对)
  t_stream.c (stream 消息)
```

## 基本元素分解

1. **紧凑布局**: 6B 头 + 变长 entry + EOF; 整数/字符串双编码族 (9 种)
2. **backlen 自描述**: 每 entry 尾部存自身长度 (1-5B) — 双向遍历 + **无级联更新**
3. **三合一写操作**: lpInsert 统一插入/删除/替换 (单次 realloc+memmove)
4. **整数嗅探**: lpEncodeGetType 字符串→整数编码 ("123" 省空间)
5. **批量接口**: lpBatchInsert/lpBatchAppend (hash 批量构建)
6. **严格解析**: lpStringToInt64 (string2ll 移植)

## 标记问题 (9 个)

1. **backlen 语义与"无级联"设计**: 每 entry 存自身长度 (固定 5B 上限) — 插入/替换变长只影响头部, 后驱不受影响 — **对比 ziplist 的 prevlen 1B/5B 级联传播** (REDIS-PLAN "级联更新"表述错误!)
2. 编码族选择: 整数 6 档 (7/13/16/24/32/64 bit) + 字符串 3 档 (6/12/32 bit) — 阈值与空间收益?
3. lpEncodeGetType 整数嗅探: "123" 编码为整数 — 什么字符串能嗅探? (严格 string2ll 语义)
4. lpInsert 三合一 (插入/删除/替换): LP_AFTER 转 BEFORE 的技巧; realloc/memmove 时机 (扩先缩后)?
5. lpPrev 向后遍历: backlen 解码跳回 — 为什么 O(1)?
6. 批量接口 lpBatchInsert: 单次 realloc 的意义 (hash 构建场景)?
7. 头部 num_elements 惰性: LP_HDR_NUMELE_UNKNOWN (65535) 时全扫描 — 何时回填?
8. 完整性校验 lpValidateIntegrity (deep 模式): 防御什么 (损坏数据/RDB 加载)?
9. 消费场景: hash/zset/stream 何时用 listpack (阈值转换)?

## 时空溯源 (代码内痕迹)

- 2017 (antirez): listpack 独立项目 (github.com/antirez/listpack) — 设计目标: **替代 ziplist, 消除级联更新**
- 7.x: ziplist 全面退役 → listpack (hash/zset/list/stream 全切换)
- #if 0 调试块: 早期调试残留 (强制新指针)
- lpStringToInt64 移植自 utils.c string2ll (2011, Pieter Noordhuis)
