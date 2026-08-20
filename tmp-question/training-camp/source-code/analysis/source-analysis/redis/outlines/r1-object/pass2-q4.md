# 闭环笔记 q4: tryObjectEncoding — 编码优化链

## 假设
字符串写入键空间后走优化链: RAW/EMBSTR → INT (可解析) → 共享整数或 INT; 小字符串 → EMBSTR — 顺序: 先 INT 后 EMBSTR, 全程 O(1) 检查。

## 验证过程
- tryObjectEncodingEx (object.c:607-678):
  - L612-616: 仅字符串类型 + 仅 RAW/EMBSTR (sdsEncodedObject) — 已是 INT 的跳过
  - L618-621: **refcount > 1 不编码** (共享对象不可变, 注释: "may end in places where they are not handled")
  - L625-650: INT 尝试: `len <= 20 && string2l` → 共享整数 (条件 q3) 或转 INT
  - L657+: **EMBSTR 尝试**: `len <= 44` → RAW 转 EMBSTR (同 chunk 分配)
- tryObjectEncoding (L679-683): 包装 (try_trim 参数)
- 触发: SET 命令写入前 (setGenericCommand); 编码是"写入时优化, 读时透明"
- 顺序意义: 先 INT (免分配收益最大) 后 EMBSTR (缓存友好); INT 失败才看 EMBSTR
- refcount>1 守卫: 共享/多引用对象不碰 — 优化只对"独有"对象

## 代码类型
Algorithmic (优化管线) — 写入路径

## 跨域关联
- R-20 (SET 命令) → 触发面
- q2/q3 (EMBSTR/INT) → 优化目标
- R-33 (分配) → 优化收益

## 结论
优化链 = 写入时的 O(1) 编码降级: INT (免分配) → 共享 (池) → EMBSTR (缓存); refcount>1 守卫保证不破坏共享对象。读路径透明 (getDecodedObject 按需解码)。
源码位置: object.c:607-683
