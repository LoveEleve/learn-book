# 闭环笔记 q3: popGenericCommand — 弹出与空键

## 假设
pop = 单元素/COUNT 范围两态; 空键删除 + 删除信号 (XREADGROUP 解阻)。

## 验证过程
- popGenericCommand (t_list.c:757-809):
  - COUNT 解析 (L762-769): 仅 argc==3; 负值拒绝 (getPositiveLongFromObjectOrReply)
  - lookupKeyWriteOrReply (L771): 不存在 → hascount ? 空数组 : null (RESP 按版本)
  - **COUNT=0 快速路径** (L775-778): 空数组
  - 单元素 (L780-789): listTypePop + addReplyBulk + **listElementsRemoved** (L785)
  - COUNT 范围 (L791-807): rangelen = min(count, llen); 首/尾段定位 (L797-798); addListRangeReply + listTypeDelRange + listElementsRemoved
- **listElementsRemoved** (L736-755): **空 list → dbDelete 键** (L748-750) + signalDeletedKeyAsReady (L751, XREADGROUP unblock_on_nokey 解阻) + notify "del"
- mpopGenericCommand (L811-845): 多键顺序尝试 (L817-833); 传播重写 [LR]POP COUNT (L835-842)
- BLPOP 前置: 先非阻塞尝试 (lookupKeyWrite + listTypePop), 失败才 blockForKeys — 阻塞只发生在空键

## 代码类型
Mechanism (弹出)

## 跨域关联
- R-5 (listTypePop/listTypeDelRange) / R-21 (signalDeletedKeyAsReady/dbDelete)

## 结论
pop = 单/COUNT 两态 + 空键删键 (list 空即不存在的不变量) + 删除信号 (XREADGROUP 语义)。BLPOP 先试后阻 — 阻塞条件是"现在拿不到"。
源码位置: t_list.c:736-845
