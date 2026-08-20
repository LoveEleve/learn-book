# 闭环笔记 q1: SET 标志矩阵与互斥校验

## 假设
SET 有 9 个可选标志, 解析器统一校验互斥性; 条件设置语义 (NX/XX) 在 setGenericCommand 执行。

## 验证过程
- 标志定义 (t_string.c:49-58): OBJ_NO_FLAGS / SET_NX(1<<0) / SET_XX(1<<1) / EX(1<<2) / PX(1<<3) / KEEPTTL(1<<4) / SET_GET(1<<5) / EXAT(1<<6) / PXAT(1<<7) / PERSIST(1<<8)
- parseExtendedStringArgumentsOrReply (L188-270):
  - 大小写不敏感前缀匹配 (L195-196: 首字符+次字符判定 — 手动 strncasecmp)
  - **互斥校验在解析时内联**: NX 分支检查 `!(*flags & OBJ_SET_XX)` (L197); XX 分支检查 `!(*flags & OBJ_SET_NX)` (L202) — 后出现者拒绝
  - KEEPTTL 检查 `!(EX|EXAT|PX|PXAT|PERSIST)` (L211-213)
  - EX 检查 `!(KEEPTTL|PERSIST|EXAT|PX|PXAT)` (L224-226) — 同类 TTL 标志互斥
  - PERSIST 仅 COMMAND_GET (L216); NX/XX/GET/KEEPTTL 仅 COMMAND_SET (L197,202,208,213)
  - 非法选项 → syntaxerr (L265-266)
- setGenericCommand 执行面 (L78-85): `NX && found` 或 `XX && !found` → abort (L81-84, GET 时仍返回旧值)
- setkey_flags 组装 (L88-89): KEEPTTL||expire → SETKEY_KEEPTTL; found → SETKEY_ALREADY_EXIST/DOESNT_EXIST — 传给 R-21 setKey 四态路由

## 代码类型
Mechanism (标志矩阵)

## 跨域关联
- R-21 (setKey 四态) / R-1 (tryObjectEncoding) / R-22 (setExpire)

## 结论
标志 = 位域 + 解析期互斥: 同类标志 (TTL 族/PERSIST) 互斥, 条件标志 (NX/XX) 互斥, 且按命令族 (GET/SET) 分域。setGenericCommand 只消费标志位 — 解析与执行分离。
源码位置: t_string.c:49-58,188-270
