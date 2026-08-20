# 闭环笔记 q6: BITCOUNT/BITPOS — 范围与单位

## 假设
BITCOUNT = 范围计数 (start/end + BYTE/BIT 单位 + 首尾掩码); BITPOS = 范围定位。

## 验证过程
- bitcountCommand (bitops.c:775-866):
  - 范围解析 (L779-790): start/end + 单位 (BIT/BYTE, L783-786)
  - **负索引归一** (L790+): 同 R-24 GETRANGE (len+start, 钳制)
  - **首尾掩码** (L800-830): 范围不整字节时 — first_byte_neg_mask/last_byte_neg_mask (L786-787 声明) — 只数范围内的位
  - 核心计数: 整字节 redisPopcount + 首尾掩码调整 (L830+)
- bitposCommand (L867-1031): start/end 范围 (BYTE/BIT 单位) → redisBitpos; 位偏移调整 (BIT 单位时 start 位偏移)
- 语义: BITCOUNT 返回范围内的 1 位数; 掩码方案免子串拷贝

## 代码类型
Mechanism (范围计数)

## 跨域关联
- R-24 (负索引同款) / R-1 (INT 编码解码)

## 结论
BITCOUNT = **范围 + 掩码** 方案: 整字节直接 popcount, 边界字节用掩码 — 免子串分配。BIT/BYTE 单位决定 start/end 语义。负索引与 GETRANGE 同款 (R-24)。
源码位置: bitops.c:775-1031
