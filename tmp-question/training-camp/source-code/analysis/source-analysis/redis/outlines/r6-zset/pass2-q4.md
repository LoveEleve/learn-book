# 闭环笔记 q4: 同分字典序 — 确定性排序的契约

## 假设
skiplist 排序键是 (score, ele) 复合: score 相同按 ele 字典序 — 保证全序 (总可比较), ZRANGE 结果确定可预测。

## 验证过程
- zslInsert 比较 (t_zset.c:147-150): `forward->score < score || (score == score && sdscmp(ele) < 0)` — 复合比较
- zslDelete 同 (L230-233); zslGetRank 用 `<= 0` (L514-516) — 定位到相同 (score, ele) 的最后一个? 不 — GetRank 用 <= 确保走到目标本身
- 为什么需要: **score 可重复** (ZADD 允许同分) — 单靠 score 无法定位唯一节点; ele 字典序 (sdscmp) 提供全序
- 影响面:
  - ZRANGE BYSCORE 同分区间内按 ele 序 (可预测)
  - ZRANK 对重复分值的排名确定性
  - ZADD 更新已存在成员: 先删旧 (score, ele) 再插新 — 定位靠复合键
- 对照: 其他实现 (如 LevelDB) 用 (key, seq) 复合 — 同思路

## 代码类型
Interface (排序契约)

## 跨域关联
- R-4 (sdscmp) → 字典序比较
- R-21 (ZRANGE/ZRANK) → 消费者
- q3 (排名) → 复合键下的确定性

## 结论
(score, ele) 复合排序 = 全序契约: 重复分值下唯一可定位, 所有命令结果确定。sdscmp 字典序是 Redis 的字符串总序 (二进制安全比较)。
源码位置: t_zset.c:147-150,230-233,508-528
