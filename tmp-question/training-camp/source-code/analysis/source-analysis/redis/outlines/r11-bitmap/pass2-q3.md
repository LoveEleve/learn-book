# 闭环笔记 q3: Bitfield — 任意位宽存取与溢出

## 假设
BITFIELD = 任意位宽 (≤64) 整数在任意偏移的存取 + 溢出三模式 (WRAP/SAT/FAIL)。

## 验证过程
- 类型解析 (bitops.c:429-459): getBitfieldTypeFromArgument — iN/uN (N≤64); 非法拒绝
- **setUnsignedBitfield** (L188-202): 按位宽生成掩码 → 清位 → 置位; **setSignedBitfield** (L203-207): 符号扩展
- **getUnsignedBitfield** (L208-221) / **getSignedBitfield** (L222-265): 按位提取 + 符号扩展
- **溢出检查** (L267-360):
  - 无符号 (L267-303): max = 2^bits-1 (64 位特判 UINT64_MAX); 上溢/下溢判定 (L277-291); **WRAP: 截断低 bits 位** (L295-300) / **SAT: 钳到 max/0** (L281,288)
  - 有符号 (L304-360): max = 2^(bits-1)-1, min = -max-1 (L306-307); 同三模式
- bitfieldGeneric (L1032-1262): GET/SET/INCRBY 子命令循环 + overflow 前缀 (默认 WRAP)
- 位序: MSB 优先 (L171-186 注释: 偏移 7 设 5 位 → 示例图)

## 代码类型
Mechanism (位宽整数)

## 跨域关联
- R-24 (字符串承载) / R-28 (addReply 家族)

## 结论
BITFIELD = 位图的"结构化访问": 跨字节任意位宽整数 + 三溢出模式 (WRAP 截断/SAT 钳制/FAIL 报错)。64 位特判 (UINT64_MAX/INT64_MAX) 防移位 UB — 安全面细节。
源码位置: bitops.c:188-360,429-459,1032-1262
