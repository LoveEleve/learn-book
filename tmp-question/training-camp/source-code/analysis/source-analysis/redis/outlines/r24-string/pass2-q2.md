# 闭环笔记 q2: setGenericCommand — 传播重写

## 假设
SET 的传播面统一: 带 TTL → 重写 SET...PXAT; 带 GET → 剥 GET 重写。

## 验证过程
- setGenericCommand (t_string.c:63-129):
  - GET 标志: 先 getGenericCommand (L72-74) — 返回旧值
  - NX/XX 条件 (L78-85): 不满足 → GET 时返回旧值 (L82, 语义: SET key v GET 失败也返回旧值), 否则 null/abort
  - **expire 传播归一** (L95-105): setExpire (L96) → **重写 argv = SET key val PXAT <绝对毫秒>** (L99-102) — 与 R-22 的 PEXPIREAT 归一同一哲学 (主从时钟无关)
  - **GET 剥离** (L111-128): 重写 argv 去掉所有 GET 参数 (L119-123, 大小写不敏感匹配) — 从库/AOF 不需要 GET 语义 (值已返回)
  - KEEPTTL 传播: 原命令保留 (无重写)
- 编码: setCommand 调 tryObjectEncoding (L283, R-1 优化链) — 值对象压缩存储
- dirty 计数 + notify "set" (L92-93)
- GETSET (L410-419): setKey + 重写 SET (L418)
- SETEX/PSETEX (L292-300): 固定标志 + 单位

## 代码类型
Mechanism (传播归一)

## 跨域关联
- R-22 (PEXPIREAT 归一) / R-1 (tryObjectEncoding) / R-28 (rewriteClientCommandVector)

## 结论
SET 传播 = **"值 + PXAT 绝对戳"** 最小形式: TTL 转绝对毫秒 (主从时钟无关), GET 剥离 (执行面语义), KEEPTTL 原样。与 EXPIRE 族 (R-22) 同一"传播归一"哲学。
源码位置: t_string.c:63-129,410-419
