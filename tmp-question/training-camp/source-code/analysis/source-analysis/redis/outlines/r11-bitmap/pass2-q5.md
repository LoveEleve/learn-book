# 闭环笔记 q5: BITOP — 四运算与 maxlen 语义

## 假设
BITOP = AND/OR/XOR/NOT 四运算; NOT 单键限制; 结果长度 = maxlen (短键零填充)。

## 验证过程
- bitopCommand (bitops.c:586-774):
  - **操作解析** (L596-608): AND/OR/XOR/NOT 前缀匹配
  - **NOT 单键限制** (L610-616): `c->argc != 4` → 报错
  - 多键读取 (L618-640): src 数组 + len 数组 + maxlen/minlen 统计 (L631-632)
  - **运算循环** (L650+): 结果 = maxlen 长度 — **短键零填充** (读时越界按 0); AND 时短键贡献 0 → 结果尾零; OR/XOR 同
  - **零结果优化**: AND 遇到 0 字节提前
  - 写入 (L750+): dbAdd/dbSetValue + notify "set"
- 传播: BITOP 原样复制 (结果由从库重算 — 确定性)

## 代码类型
Mechanism (位运算)

## 跨域关联
- R-24 (字符串) / R-28 (addReply)

## 结论
BITOP = 多键位运算, **maxlen 语义** (结果取最长, 短键零填充) — 与长度无关的键按 0 参与。NOT 单键限制是语义约束 (NOT 无交换律)。传播原样 (结果确定性)。
源码位置: bitops.c:586-774
