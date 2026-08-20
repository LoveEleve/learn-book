# R-11 Bitmap — Pass 1 探索笔记

> 域: R-11 Bitmap (bitops.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/bitops.c (1269) | Redis 7.4.2

## 调用图

```
核心算法:
redisPopcount (L19-71): 256 查表 + SWAR 28 字节批 (0x55555555/0x33333333/0x0F0F0F0F/0x01010101)
redisBitpos (L80-165): 字对齐跳过 (全 0/全 1) + MSB 扫描; 全 0 找 1 → -1 / 找 0 → count*8 (零填充假设)
Bitfield 家族 (L188-360): set/getSigned/Unsigned (任意位宽 ≤64, MSB 优先); Overflow 检查 (WRAP/SAT/FAIL)

命令面:
setbitCommand (L511): offset → byte/bit (bit = 7-(offset&7), MSB 优先) → dirty 三条件 → 更新 + 返回旧值
getbitCommand (L558): 定位读取
bitopCommand (L586): AND/OR/XOR/NOT + NOT 单键检查 → 多键 maxlen 语义 (短键零填充)
bitcountCommand (L775): start/end 范围 + BIT/BYTE 单位 + 首尾掩码
bitposCommand (L867): 位定位 (byte 或 bit 单位)
bitfieldGeneric (L1032): GET/SET/INCRBY 子命令 + overflow 控制

辅助:
getBitOffsetFromArgument (L393): offset 解析 + 负偏移 (hash 模式)
getBitfieldTypeFromArgument (L429): iN/uN 类型解析
lookupStringForBitCommand (L460): 位命令字符串准备 (扩容零填充)
getObjectReadOnlyString (L492): 编码解码 (INT→栈 buf)
```

## 基本元素分解

1. **redisPopcount**: 查表 + SWAR 批量 (28 字节/轮)
2. **redisBitpos**: 字对齐跳过 + MSB 扫描 + 特殊值 (-1/零填充)
3. **Bitfield 位宽操作**: 任意位宽 ≤64 存取 + 溢出三模式
4. **SETBIT/GETBIT**: 位定位 (MSB 优先) + dirty 三条件
5. **BITOP**: 四运算 + maxlen 语义
6. **BITCOUNT/BITPOS**: 范围 + 单位 + 掩码

## 标记问题 (8 个)

1. redisPopcount 的 SWAR 算法原理?
2. redisBitpos 的字对齐跳过怎么加速?
3. Bitfield 任意位宽的跨字节存取?
4. 溢出三模式 (WRAP/SAT/FAIL) 检查?
5. SETBIT 的 MSB 优先位序?
6. BITOP 的 maxlen 语义 (短键零填充)?
7. BITCOUNT 的 BIT/BYTE 单位 + 掩码?
8. 负偏移 (hash 模式) 语义?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): bitops.c 初版 — SETBIT/GETBIT/BITCOUNT (版权 2009-Present)
- 2.8: BITOP (AND/OR/XOR/NOT)
- 3.2: BITFIELD (位宽整数操作 + overflow)
- 4.0+: BITPOS; BITCOUNT 范围单位 (BIT/BYTE)
- 演进: popcount 查表 + SWAR 演进; bitfield 是位操作的"结构化"扩展

## 大域拆分判断

1269 行单文件 — **不拆** (🟡 B, 6 闭环足够)。
