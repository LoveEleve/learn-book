# 闭环笔记 q7: zsetAdd 命令面 — 双编码路径 + 选项族

## 假设
zsetAdd 统一处理两种编码 (listpack/skiplist) + ZADD 选项 (NX/XX/GT/LT/INCR) + NaN 守卫; 编码超阈值时在命令内转换。

## 验证过程
- zsetAdd (t_zset.c:1425+):
  - L1427-1435: 选项解析 (incr/nx/xx/gt/lt) + NaN 校验 (L1435-1436: "NaN as input is an error regardless of all the other parameters")
  - listpack 路径 (L1448-1480): zzlFind 存在检查 → NX 短路 / INCR 累加 / GT-LT 判定 / **score 变更 → 先删后插** (zzlDelete+zzlInsert, L1473-1475) → **超阈值转换** (L1480: `zzlLength+1 > zset_max_listpack_entries || sdslen(ele) > zset_max_listpack_value` → zsetConvert)
  - skiplist 路径 (L1490+): dictFind 成员检查 → 同样选项 → zslDelete (旧) + zslInsert + dictAdd/dictReplace
- 双编码统一: 选项语义 (NX/XX/GT/LT/INCR) 在两路径完全一致 — 命令面对编码透明
- INCR 语义: score += curscore (原子累加); NaN 守卫防 "INCR 溢出"
- 返回: out_flags (NAN/NOP/UPDATED/ADDED) → 命令层响应 "+0/-0" 等

## 代码类型
Interface (命令语义) + Glue (双编码路由)

## 跨域关联
- R-19 (listpack 编码) / R-3 (dict) → 两路径
- R-21 (ZADD 命令实现) → 消费
- q8 (转换) → 阈值触发

## 结论
zsetAdd = 双编码透明的命令内核: 选项 (NX/XX/GT/LT/INCR) 在两路径完全一致, NaN 是全局守卫; 编码超阈值在命令路径内转换 (listpack→skiplist 一次到位)。命令面复杂度被收敛到一个函数。
源码位置: t_zset.c:1425-1530
