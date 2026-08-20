# 闭环笔记 q8: objectCommand — TYPE/OBJECT 可观测面

## 假设
TYPE/OBJECT ENCODING/REFCOUNT/IDLETIME/FREQ 命令暴露对象内部状态 — 编码可观测性 (调试/监控)。

## 验证过程
- objectCommand (object.c:1442+): help 列表 (ENCODING/FREQ/IDLETIME/REFCOUNT) + TYPE 在 t_string.c 或 server.c?
- OBJECT ENCODING: 按 encoding 返回字符串名 (int/embstr/raw/hashtable/quicklist/skiplist/listpack/intset...)
- OBJECT REFCOUNT: 返回对象 refcount (共享对象显示特殊值? 验证)
- OBJECT IDLETIME: `(lruclock - o->lru)` 秒级估算
- OBJECT FREQ: LFU 计数器 (8bit)
- TYPE: 返回类型名 (string/list/set/zset/hash/stream/module)
- 用途: 调试编码状态 (为什么我的 key 不是 int 编码?); 监控共享情况; R-19/R-7 的编码转换可观测

## 代码类型
Interface (可观测命令)

## 跨域关联
- R-19/R-7/R-6/R-5 (全部编码) → 展示面
- R-23 (IDLETIME/FREQ) → 淘汰状态
- 生产面: OBJECT ENCODING 排查内存问题

## 结论
OBJECT 命令族 = 对象内部的可观测窗口: 编码名/引用数/空闲时间/访问频率 — 调试编码状态与共享情况的入口。TYPE 是类型面, OBJECT 是内部面。
源码位置: object.c:1442+
