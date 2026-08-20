# 闭环笔记 q3: INT 编码 — 指针直接存值

## 假设
整数可表示的字符串: INT 编码 — o->ptr 直接存 long 值 (零分配!); 共享整数 (0-9999) 优先; 条件: ≤20 字符可解析 + 非共享对象。

## 验证过程
- createStringObjectFromLongLongWithOptions (object.c:128-140):
  - L131-132: `value < OBJ_SHARED_INTEGERS && AUTO → shared.integers[value]` (共享)
  - L134-137: 否则 `createObject(OBJ_STRING, NULL); encoding=INT; ptr=(void*)value` — **零分配** (外壳之外无数据)
- tryObjectEncodingEx (L607-678): `len <= 20 && string2l(s,len,&value)` → 可 INT 编码; 共享条件 (L636-643): `maxmemory==0 || !(policy & NO_SHARED_INTEGERS)` — **maxmemory 下避免共享整数** (每个对象需私有 LRU 字段); 创建路径同条件 (L159)
- RAW → INT: sdsfree + ptr=value (原地转换); EMBSTR → INT: 新对象 (embstr 不可变)
- getDecodedObject (L685-697): INT → ll2string 临时缓冲转字符串 (读时解码)
- 比较优化 (L706-715 注释): 两个 INT 对象比较直接用值 (免解码) — compareStringObjectsWithFlags

## 代码类型
Algorithmic (零分配编码) — 内存优化

## 跨域关联
- R-20 (INCR/DECR 命令) → INT 对象消费
- R-19 (listpack 整数编码) → 同哲学 (数值免字符串)
- R-23 (LRU) → NO_SHARED_INTEGERS 关联

## 结论
INT 编码 = "指针就是值": 数值字符串零分配 (外壳 16B 即全部); 共享整数 (10000 池) 让 0-9999 连外壳都共享; maxmemory 下禁用共享 (LRU 需要私有字段)。解码在读时临时转换。
源码位置: object.c:128-140,607-678,685-697
