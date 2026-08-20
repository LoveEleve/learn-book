# 闭环笔记 q2: setTypeAddAux — 三编码写入与转换链

## 假设
写入 = 三编码分支; intset 遇非整数 → listpack 或 HT (按规模); dict 用预定位插入免双查。

## 验证过程
- setTypeAddAux (t_set.c:104-208):
  - 整数入参 (L106-117): intset 直插 (L107-111, success 后 maybeConvertIntset); 否则 ll2string 转字符串
  - **HT 分支** (L120-133): **dictFindPositionForInsert 预定位** (L124) → dictInsertAtPosition (L128) — 一次哈希定位 (免 dictAddRaw 双查); sds 复用 (str_is_sds 免复制 L122)
  - **LISTPACK 分支** (L134-159): lpFind → 未命中: ≤128 && ≤64B && lpSafeToAdd → lpAppendInteger/lpAppend (L145-152); **超限 → 转 HT** (L155-156)
  - **INTSET 分支** (L160-203):
    - 可整数化 (L162-168): intsetAdd + maybeConvertIntset
    - **非整数** (L169-202): 评估规模 — **≤128 且值≤64B 且 intset 元素也≤64B 表示 → 转 LISTPACK** (L181-194, lpShrinkToFit); 否则 → **转 HT** (L196-200)
- maybeConvertIntset (L57-61): intsetLen > 512 → HT
- maybeConvertToIntset (L66-88): **HT/listpack → intset** (全整数且≤512 时; sinterstore 场景 L1392) — **降级! 与 hash/list 单向不同**

## 代码类型
Mechanism (编码内写入)

## 跨域关联
- R-7 (intset API) / R-19 (listpack) / R-3 (dict 预定位)

## 结论
set 写入 = 三编码分支 + **intset 非整数双路转换** (规模决定 listpack 还是 HT) + dict 预定位插入优化。**双向转换是 set 特有**: 全整数结果可降回 intset (sinterstore), 与 hash/list 单向升级形成对比。
源码位置: t_set.c:57-88,104-208
