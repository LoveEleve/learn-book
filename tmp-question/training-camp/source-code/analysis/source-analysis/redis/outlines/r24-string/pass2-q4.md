# 闭环笔记 q4: SETRANGE/GETRANGE — 零填充与负索引

## 假设
SETRANGE 用 sdsgrowzero 零填充; GETRANGE 负索引双向转换 + 边界钳制。

## 验证过程
- setrangeCommand (t_string.c:421-479):
  - offset < 0 → 拒 (L429-432); 空值特判 (L437-440, L457-460: 不存在返回 0, 存在返回原长)
  - **长度上限** (L443-444, L463-464): checkStringLength (L19-31): `total > proto_max_bulk_len || total < size || total < append` — 上限 + **加法溢出检测** (uint64_t 转换, L23)
  - 不存在 → sdsnewlen(NULL, offset+len) (L446, 分配未填) + dbAdd; 零填充由后续 sdsgrowzero (L471) 完成
  - 存在 → dbUnshareStringValue (L467, R-21: 共享/编码对象复制) → **sdsgrowzero** (L471: 扩到 offset+len, 空位填零) + memcpy (L472)
  - 返回最终长度 (L478)
- getrangeCommand (L481-520):
  - INT 编码解码 (L494-496): ll2string 到栈 buf (免 getDecodedObject 分配)
  - **负索引转换** (L503-511): 双负且 start>end → 空 (L503-506); start = len+start; end = len+end; 负钳 0; end ≥ len → len-1
  - 边界 (L515-519): start > end || len==0 → 空 bulk
- 语义: GETRANGE 是**复制** (addReplyBulkCBuffer), SETRANGE 是**原地** (破坏性)

## 代码类型
Mechanism (范围操作)

## 跨域关联
- R-4 (sdsgrowzero) / R-21 (dbUnshareStringValue) / R-1 (INT 编码)

## 结论
SETRANGE = 零填充扩容 (sdsgrowzero) + 共享保护 (dbUnshare); GETRANGE = 负索引归一 + 免分配解码 (INT 走栈 buf)。checkStringLength 的加法溢出检测 (uint64 转换) 是防 wrap 安全面。
源码位置: t_string.c:19-31,421-520
