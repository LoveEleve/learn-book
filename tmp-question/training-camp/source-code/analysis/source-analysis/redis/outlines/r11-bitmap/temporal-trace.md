# R-11 位操作 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | bitops.c 初版 — SETBIT/GETBIT/BITCOUNT (版权 "2009-Present"); popcount 查表 |
| 2.6 | BITOP (AND/OR/XOR/NOT) |
| 2.8 | BITPOS; redisBitpos 字对齐跳过 |
| 3.2 | **BITFIELD** (位宽整数 + overflow 三模式) |
| 4.0+ | BITCOUNT 范围单位 (BIT/BYTE) |
| 演进 | popcount: 查表 → 查表+SWAR 28 字节批 (性能演进); bitfield 是位操作的"结构化"扩展 |

## 痕迹证据

- L16-18: popcount 输入上限注释 ("up to 512 MB or more (server.proto_max_bulk_len)")
- L73-79: bitpos 返回值契约注释 (bit=0 保证 >= 0 因零填充; bit=1 可能 -1)
- L123-129: 位序设计注释 ("considering the first byte as the most significant... first bit at position zero") — MSB 优先的权威依据
- L140-144: 全 0 找 1 的 -1 特例注释
- L161-163: serverPanic 防御 ("there is a bug in the algorithm")
- L167-186: Bitfield 位序注释 + 示例图 (offset 7 设 5 位 23 → 二进制图示)

## 推断标注

- "SWAR 比逐位快 ~10×" — 算法特性推导 (4 ops/字节 vs 8 查表/字节), 非代码断言
- "BITFIELD 是位操作的结构化扩展" — 动机推断 (代码无注释, 从 API 形态判断)
- "写相同值零开销 (dirty 短路)" — 代码事实 (L540 if 条件), "零开销占比"是推断
