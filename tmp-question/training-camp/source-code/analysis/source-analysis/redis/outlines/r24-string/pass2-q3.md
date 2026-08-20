# 闭环笔记 q3: INCR 原地更新与溢出

## 假设
INCR 在满足条件下原地改 INT 编码的 ptr (零分配); 溢出检查双符号位。

## 验证过程
- incrDecrCommand (t_string.c:580-614):
  - lookupKeyWrite + checkType (L584-585) + getLongLongFromObjectOrReply (L586)
  - **溢出检查** (L589-593): `(incr < 0 && oldvalue < 0 && incr < LLONG_MIN-oldvalue) || (incr > 0 && oldvalue > 0 && incr > LLONG_MAX-oldvalue)` — 符号对齐才检查 (正+正/负+负)
  - **原地更新四条件** (L596-598): o && refcount==1 && encoding==INT && (value<0 || value>=OBJ_SHARED_INTEGERS) && LONG_MIN≤value≤LONG_MAX
    - 即: 不可共享 (值越出 0-9999 共享池) 才直接改; 否则走新建 (共享池对象不可变, R-1)
  - 原地: `o->ptr = (void*)((long)value)` (L601) — 零分配
  - 新建: createStringObjectFromLongLongForValue (L603) + dbReplaceValue/dbAdd
  - 统计/通知: signalModifiedKey + notify "incrby" (L610-611) + dirty (L612)
- DECR (L620-622): incr=-1; DECRBY (L631-641): **incr==LLONG_MIN 特判** (L636-639, 取负会溢出)
- INCRBY (L624-629): 参数解析后同路
- INCRBYFLOAT (L643-674): long double 运算 + **NaN/Inf 拒绝** (L654-657, 防脏值入库) + createStringObjectFromLongDouble (L658) + **重写 SET KEEPTTL** (L671-673, 浮点格式化差异不传播)

## 代码类型
Mechanism (原地优化)

## 跨域关联
- R-1 (INT 编码/共享整数/refcount) / R-21 (dbReplaceValue/dbAdd)

## 结论
INCR = 溢出双检查 (符号对齐) + **四条件原地更新** (INT 编码 + 独占 + 非共享 + LONG 范围) — 高频自增零分配。INCRBYFLOAT 因浮点精度重写 SET KEEPTTL — "格式差异不传播"。
源码位置: t_string.c:580-674
