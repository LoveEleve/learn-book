# R-16 事务 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 1.2.0 (2009-2010) | MULTI/EXEC 初版 (commands.def 实证; multi.c 版权 2009-Present — 与 Redis 同年) |
| 2.0.0 | DISCARD 命令 |
| 2.2.0 | **WATCH/UNWATCH** — CAS 语义引入 (db->watched_keys dict 按 key 映射客户端列表, multi.c:237-244 注释) |
| 2.6+ | CMD_NOSCRIPT (事务命令禁入 Lua 脚本 — 对照 R-30) |
| 7.x | **watchedKey 重构**: 客户端侧独立节点 → **嵌入式 listNode + redis_member2struct** (multi.c:246-252 注释 — 消除 listSearchKey/dictFind); isWatchedKeyExpired (expired:1 位域) — HFE/过期语义时代 (对照 R-22); mstate 增加 cmd_flags/cmd_inv_flags/argv_len_sums (内存统计+优化面) |

## 痕迹证据

- multi.c:42-45: DIRTY 冻结注释 (pipeline 场景动机)
- multi.c:48-53: 预分配 2 注释 ("at least two commands")
- multi.c:246-252: 嵌入式节点重构注释 (O(1) 摘除动机 — 时空溯源关键证据)
- multi.c:390-393: touch 后立即 unwatch 注释 (内存开销动机)
- multi.c:443-445: 迭代中不可 unwatch 的 UAF 注释
- server.c:4193-4196: 入队白名单注释 (exec/discard/multi/watch/quit/reset 不入队)
- multi.c:146-148: "failed EXEC ... technically it is not an error" — nullarray 语义权威依据
- db.c:620-623: signalModifiedKey 双失效 (touchWatchedKey + trackingInvalidateKey)

## 推断标注

- "isWatchedKeyExpired 7.x HFE 时代引入" — 版本推断 (仓库浅克隆; expired 位与 HFE 语义相关)
- "嵌入式重构 7.x" — 版本推断 (注释风格 + kvstore 同期)
- "EXEC 1.2.0" — commands.def 实证 (非推断)
