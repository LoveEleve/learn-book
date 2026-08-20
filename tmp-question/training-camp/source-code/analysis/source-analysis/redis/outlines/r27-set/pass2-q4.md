# 闭环笔记 q4: SPOP/SRANDMEMBER — 随机与 COUNT 语义

## 假设
SPOP = 随机移除 (COUNT 正数); SRANDMEMBER = 随机查看 (正负语义); 大集合双策略。

## 验证过程
- setTypeRandomElement (t_set.c:407-429): intset → 随机索引; listpack → 随机定位; HT → dictGetFairRandomKey (R-3 FAIR)
- setTypePopRandom (L430-457): 随机 + 删除
- spopWithCountCommand (L739-945):
  - **COUNT 仅正数** (L742: getPositiveLongFromObjectOrReply — 负值报错!)
  - **count >= size → 返回全部 + dbDelete + 重写 DEL/UNLINK** (L773-787)
  - count < size → **SPOP_MOVE_STRATEGY_MUL=5 双策略** (L737, L809): `remaining×5 > count` → 直接随机抽取 (listpack lpNextRandom L817 / dict 随机); 否则 → 小集合拷贝移动策略
  - **空集删键** (L773-787): 弹空 → dbDelete + 通知 + 传播 DEL/UNLINK
- **srandmemberWithCountCommand** (L998-1202):
  - **正负语义** (L1001-1016): l >= 0 → uniq=1 (**不重复**, count > size 时返回全部); l < 0 → uniq=0 (**可重复**, 恰好 count 个)
  - 大集合: count 接近 size → 全量随机排列 (L1092+); 小比例 → dictGetSomeKeys 采样 (L1164+)
- spopCommand (L946) / srandmemberCommand (L1203): 单元素版
- 语义差异: SPOP 移除 (传播 SREM 批/DEL), SRANDMEMBER 只读; **SPOP 无正负, SRANDMEMBER 有** (uniq)

## 代码类型
Mechanism (随机采样)

## 跨域关联
- R-3 (dictGetFairRandomKey/dictGetSomeKeys) / R-7 (intset 随机)

## 结论
随机命令 = 编码分派随机 + **SPOP 正数-only + 全删短路** vs **SRANDMEMBER uniq 正负**。SPOP 传播特殊: 全删 → DEL/UNLINK 重写; 部分删 → SREM 批量 (L786-789)。弹空删键维持"空集不存在"不变量 (R-26 同款)。
源码位置: t_set.c:407-457,737-809,998-1202
