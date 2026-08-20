# 闭环笔记 q1: sds = char* — "指针即对象"的零成本设计

## 假设
sds 直接是 char*, 头部信息内嵌在 buf 前 (s[-1] 是 flags byte) — 让 SDS 与 C 字符串 API 完全兼容 (printf/strlen 可用), 头部访问零成本。

## 验证过程
- sds.h:20: `typedef char *sds;` — 就是裸 char*
- sds.h:22-51: 所有 header `__packed__` 结构, buf 紧跟头部 — **sds 指针 = buf 起点, s[-1] 即 flags**
- sds.h:64-79 (sdslen): `unsigned char flags = s[-1]; switch(flags&SDS_TYPE_MASK)` — 读长度 = 一次负索引 + switch, 无额外指针解引用
- sds.c:142: `s[initlen] = '\0'` — 总是 \0 结尾 → printf/strlen 直接可用 (注释 L73-80: "You can print the string with printf()")
- 收益: 函数签名即 char* (兼容 C API/网络收发), 头部访问 O(1) 且无缓存缺失惩罚 (内嵌)
- 代价: 结构体不是完整对象 — 宏 (SDS_HDR) 而非方法; s[-1] 依赖 packed 布局

## 代码类型
Algorithmic (内存布局) — 核心设计决策

## 跨域关联
- R-1 (object.c 字符串对象 o->ptr 就是 sds) → 全部字符串值载体
- R-28 (networking 收发) → write/read 直接传 sds 给系统调用
- R-33 (zmalloc) → sds 头部+内容一次分配 (R-33 q4)

## 结论
"指针即对象": sds=char* + 内嵌 packed 头部, s[-1] flags 分派访问 — 与 C 生态零摩擦 (可 printf/可直传系统调用), 头部访问 O(1)。这是 SDS 能无缝嵌入 Redis 一切的根基。
源码位置: sds.h:20-79; sds.c:101-142
