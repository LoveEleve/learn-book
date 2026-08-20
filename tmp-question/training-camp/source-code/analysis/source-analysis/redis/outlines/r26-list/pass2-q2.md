# 闭环笔记 q2: pushGenericCommand — 推送与唤醒

## 假设
push = lookup → 创建 (XX 特判) → 转换预判 → listTypePush 批量 → 通知; 阻塞唤醒由 dbAdd 的 signalKeyAsReady 触发。

## 验证过程
- pushGenericCommand (t_list.c:464-492):
  - lookupKeyWrite + checkType (L466-467)
  - 不存在 (L469-474): **XX → 0** (L470-471, LPUSHX/RPUSHX); 否则 createListListpackObject + dbAdd (L475-476)
  - **转换预判** (L477): listTypeTryConversionAppend (GROWING, 新增值超限转 quicklist)
  - 批量 push (L478-482): listTypePush ×N + dirty++
  - 回复长度 (L484) + signalModifiedKey + **notify "lpush"/"rpush"** (L486-491)
- **阻塞唤醒触发** (R-21 已交付): dbAddInternal 内 signalKeyAsReady (db.c:192) — 新键创建即检查阻塞者
  - **关键机制**: t_list.c 无 signalKeyAsReady 调用 — **唤醒只需 dbAdd 路径**! 因为 BLPOP 只阻塞"不存在/空"键 (pop 空后 listElementsRemoved 删键, L736-755), 阻塞者等待的键必然不存在 → push 必然走 dbAddInternal (L192) → 唤醒; 已存在非空键的 push 不需要唤醒 (阻塞者不在等待它)
  - 佐证: t_stream.c:2083,2725 有显式 signalKeyAsReady (stream 键不因消费而删, 语义不同)

## 代码类型
Mechanism (推送)

## 跨域关联
- R-5 (listTypePush) / R-21 (signalKeyAsReady) / R-28 (addReply)

## 结论
push = 创建/XX 特判 + 转换预判 + 批量插入 + 通知。**唤醒只需 dbAdd 路径**: 空键不存在是 Redis 不变量 (pop 空即删键), 阻塞者等待的键必然不存在 → push 走 dbAddInternal (db.c:192) → signalKeyAsReady。stream 例外 (键不因消费删除, 需显式唤醒)。
源码位置: t_list.c:464-492,736-755; db.c:192; t_stream.c:2083
