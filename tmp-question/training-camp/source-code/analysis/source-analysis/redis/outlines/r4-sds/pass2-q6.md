# 闭环笔记 q6: 双标准 — 二进制安全 + C 字符串兼容

## 假设
SDS 同时满足两个世界: 内容是二进制安全的 (len 字段为准, 中间可含 \0), 但总是 \0 结尾 (printf/strlen 可直接用) — 双标准的冲突面怎么处理?

## 验证过程
- sds.c:73-80 (_sdsnewlen 注释): "The string is always null-terminated...so even if you create an sds string with 'abc',3 you can print the string with printf(). However the string is binary safe and can contain \0 characters in the middle, as the length is stored in the sds header"
- sds.c:142: `s[initlen] = '\0'` — 创建即加 NUL
- sds.c:463-472 (sdscatlen): `s[curlen+len] = '\0'` — 追加后补 NUL
- sds.h:64-79 (sdslen): 读 len 字段 (非 strlen) — 中间 \0 不影响长度
- sdsupdatelen (sds.c:191-194): 手动改 buf 后同步 len (用 strlen) — "hacked manually" 场景的修复工具
- 冲突处理: NUL 只作"兼容性终止", 不作语义 — 一切长度操作走 len; 代价: 每字节内容 +1 NUL 开销, 且被截断的 sds (SETRANGE 到中间) 需手动 sdsupdatelen
- sdscatrepr (sds.c:1100+): 可打印转义 (\x 序列) — 调试/协议输出用

## 代码类型
Interface (双标准契约)

## 跨域关联
- R-28 (networking 协议) → RESP 传输二进制安全字符串 (GET 返回含 \0 内容)
- R-1 (object.c) → 值对象的字符串载体
- R-10 (t_stream 字段值) → 二进制内容

## 结论
双标准: 永远 \0 结尾 (C 兼容, printf/系统调用无痛) + len 字段为准 (二进制安全)。NUL 是"免费兼容层", 语义全在 len — 中间 \0 完全合法。这是 SDS 取代裸 C 字符串的杀手锏: 兼容不丢, 安全不失。
源码位置: sds.c:73-80,142,463-472; sds.h:64-79
