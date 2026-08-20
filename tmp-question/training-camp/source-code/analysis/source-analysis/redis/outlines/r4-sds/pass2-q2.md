# 闭环笔记 q2: 5 类型分级 — 空间最优 vs 操作代价

## 假设
SDS 按长度阈值选 5 种 header (sdshdr5/8/16/32/64), 小串用 1 字节头省空间; sdshdr5 的特殊性在于长度编码进 flags (上限 31), 从不用于会增长的字符串。

## 验证过程
- sds.c:40-52 (sdsReqType): `<32→5, <256→8, <65536→16, <2^32→32, else→64` (LONG_MAX==LLONG_MAX 才用 64)
- sds.h:24-27 (sdshdr5): 仅 `unsigned char flags` (3 bit type + **5 bit len, 上限 31**) + buf — 无 len/alloc 字段
- sds.h:22-23 注释: "**sdshdr5 is never used, we just access the flags byte directly**" — 从不当 struct 用, 只是 1 字节布局
- sds.c:87: `if (type == SDS_TYPE_5 && initlen == 0) type = SDS_TYPE_8` — **空串强制 8** ("type 5 is not good at this")
- sds.c:241-244 (_sdsMakeRoomFor): `if (type == SDS_TYPE_5) type = SDS_TYPE_8` — **扩容永不落 5** ("not able to remember empty space")
- sds.c:316-318 (sdsResize): would_regrow → 禁止降到 5
- 收益: 短串 (1-31B) 仅 1 字节头 (普通 C 字符串还要 NUL 终止, SDS 5 头比裸 C 还省? 不 — 5 头也有 buf+1 字节 flags); 8/16/32 头 2-5 字节

## 代码类型
Algorithmic (分级策略)

## 跨域关联
- R-1 (object.c 共享整数/短字符串) → 短字符串编码 (EMBSTR 44B 阈值内多为 8 型)
- R-33 (zmalloc) → 头+内容一次分配, 分级影响请求大小

## 结论
5 级分级 = 空间最优: 长度编码进头部字宽 (31/255/65535/2^32 阈值)。sdshdr5 是"静态串专用" (1 字节头 + 5 bit len), 无法记录 alloc → **增长型字符串 (空串创建/扩容) 强制跳 8**, 否则每次追加都要重分配。
源码位置: sds.c:40-52,87,241-244; sds.h:22-27
