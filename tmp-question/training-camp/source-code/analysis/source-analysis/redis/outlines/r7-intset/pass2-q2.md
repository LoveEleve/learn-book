# 闭环笔记 q2: 编码选择与字节序 — 2/4/8 字节三档 + 统一小端

## 假设
encoding 字段 = 元素字节宽 (2/4/8); 值域决定档位; 头部与内容统一小端存储 (大端平台读写转换)。

## 验证过程
- 编码定义 (intset.c:41-43): `INTSET_ENC_INT16 = sizeof(int16_t)` — **encoding 直接就是字节宽** (2/4/8)
- _intsetValueEncoding (L46-53): 值域判定: `v < INT32_MIN || v > INT32_MAX → INT64; v < INT16_MIN || v > INT16_MAX → INT32; else INT16` — 测试断言 (L418-429): ±32767→16, ±32768→32, ±2^31→32/64, ±2^63→64
- **字节序** (_intsetGetEncoded L56-74, _intsetSet L82-95): `memcpy + memrevXXifbe` — 读: memcpy 到本地变量 + 大端时反转; 写: 反转后 memcpy — **存储统一小端**
- 头部: `intrev32ifbe(is->encoding)` 每次访问转换 (L78 等)
- 为什么小端: intset 可能被 RDB 持久化/跨平台传输 — 统一字节序保证数据可移植; memcpy 而非直接解引用 = 避免未对齐访问 (flexible array 上 int64 对齐风险)
- 收益: 小集合 (全 int16) 每元素 2B — 100 万整数 set 省 75% vs int64

## 代码类型
Interface (存储格式) + Implementation (字节序)

## 跨域关联
- R-8 (RDB 序列化) → 字节序一致性的理由
- R-19 (listpack 同款 intrev 处理) → 同族设计

## 结论
encoding 即字节宽 (2/4/8), 值域单调选择; 存储统一小端 (memcpy+memrev, 免对齐问题) — RDB/网络交换可移植。三档粗粒度 (vs listpack 六档) — intset 只服务"纯整数集合"场景。
源码位置: intset.c:41-53,56-95
