# 闭环笔记 q8: SipHash — 防哈希碰撞攻击的密钥化哈希

## 假设
Redis 用 SipHash (密钥化哈希) 替代简单哈希: 16 字节随机 seed 作为密钥, 攻击者不知道 seed 就无法构造碰撞 — 防 HashDoS 攻击。

## 验证过程
- dict.c:92-113: `static uint8_t dict_hash_function_seed[16]` + `dictGenHashFunction → siphash(key, len, seed)` + `dictGenCaseHashFunction → siphash_nocase` (大小写不敏感, 命令表用)
- siphash.c: 独立实现 (128-bit key, 2-4 轮精简版)
- seed 来源: dictSetHashFunctionSeed 由 server.c 启动时随机生成 (getRandomBytes) — [待验证 seed 生成点]
- 历史: 2014 年前 Redis 用 MurmurHash2/直接哈希 → **2014 年 12 月切换到 SipHash** (HashDoS 攻击事件后, antirez 公告) — 当时大量哈希表框架被曝碰撞攻击 (Java/PHP/Node.js)
- 代价: SipHash 比简单哈希慢 (密码学级 ~2-4 轮), 但 Redis 哈希表规模相对小, 收益 (安全性) > 成本
- 变体: siphash_nocase (命令名查找 — 大小写不敏感); dictHashKey 包装 (dict.c:83-90: useStoredKeyApi 分派)

## 代码类型
Algorithmic (安全哈希选择) — 安全权衡

## 跨域关联
- R-20 (命令表 dictGenCaseHashFunction) → 大小写不敏感查找
- R-21 (键空间 sdsHashFunction → dictGenHashFunction) → 键哈希
- 安全面: HashDoS 攻击面 (恶意客户端构造碰撞键)

## 结论
SipHash + 随机 seed: 哈希结果依赖启动时随机密钥 — 攻击者无法预知碰撞集。2014 HashDoS 事件后的安全升级, 用"每次请求的密码学哈希成本"换"输入不可预测性"。命令表用 nocase 变体保持大小写不敏感。
源码位置: dict.c:92-113; siphash.c
