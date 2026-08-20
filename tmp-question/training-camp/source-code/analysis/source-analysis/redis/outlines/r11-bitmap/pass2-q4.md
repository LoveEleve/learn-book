# 闭环笔记 q4: SETBIT/GETBIT — 位定位与 dirty

## 假设
SETBIT = offset → byte/bit 定位 (MSB 优先) + dirty 三条件 (新键/变长/位变化) 才更新。

## 验证过程
- 定位辅助 (bitops.c:393-428): getBitOffsetFromArgument — 无符号 offset + **负偏移拒绝** (hash=0 时)
- lookupStringForBitCommand (L460-491): 位命令字符串准备 — 不存在创建 + **超长扩容零填充** (sdsgrowzero 式)
- getObjectReadOnlyString (L492-510): INT/EMBSTR 解码 (llbuf 栈 buf)
- setbitCommand (L511-557):
  - **位定位** (L531-534): `byte = offset >> 3` / `bit = 7 - (offset & 0x7)` — **MSB 优先** (第 0 位 = 字节最高位)
  - **dirty 三条件** (L540): `dirty || (!!bitval != on)` — 新键/扩容/值变化才写 (值相同零开销)
  - 返回**旧值** (L555: bitval ? 1 : 0)
- getbitCommand (L558-585): 定位读取 (越界 → 0)
- 位序一致性: SETBIT/BITFIELD/BITPOS 全 MSB 优先 (L184-186 注释)

## 代码类型
Mechanism (位定位)

## 跨域关联
- R-24 (字符串承载/dbUnshare) / R-1 (编码)

## 结论
SETBIT = MSB 优先定位 + **dirty 短路** (写相同值零成本) + 返回旧值 (读改写合一)。扩容零填充走 sds (R-4)。位序 MSB-first 是位命令族统一约定。
源码位置: bitops.c:393-491,511-585
