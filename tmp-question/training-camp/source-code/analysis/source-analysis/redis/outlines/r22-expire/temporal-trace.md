# R-22 过期机制 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | expire.c 初版 — activeExpireCycle 骨架 + EXPIRE/TTL 命令族 (版权 "2009-Present") |
| 3.2 | **可写从库过期记账引入** — 注释 L430: "a lot better than leaking the keys as implemented in 3.2" — 之前从库自产 TTL 键直接泄漏 |
| 4.0 | **effort 配置** (active_expire_effort 1-10) — "The configured expire effort will modify the baseline parameters" (L88-89) |
| 5.x | **expires_cursor 每 DB 持久游标** (server.h:980) — 早期是 expires 表头指针/游标演进 |
| 7.0 | **kvstore 分片** — 扫描从 dictScan → kvstoreScan (L332), skip 回调适配 |
| 7.x (2024) | **HFE 字段级过期** — activeExpireHashFieldCycle (L144) + 10000/秒配额 + 序列放大 (与 ebuckets 同批引入) |
| 演进 | avg_ttl 统计: 循环 (旧代码注释 L366-368: 49/50+1/50) → **pow(0.98) 常数表闭式** (L369-377 推导注释) |

## 痕迹证据

- L15-20 头注释: "When keys are accessed they are expired on-access. However we need a mechanism in order to ensure keys are eventually removed when expired even if no access is performed" — 主动过期定位 (惰性面的兜底)
- L52-55 注释: "The algorithm used is adaptive and will use few CPU cycles if there are few expiring keys, otherwise it will get more aggressive" — 自适应设计原点
- L316-318: "Here we access the low level representation... this makes this code coupled with dict.c, but it hardly changed in ten years" — 低层耦合的历史注记
- L369-377: avg_ttl 从循环到几何级数闭式的完整推导注释 — 优化痕迹
- L430: 3.2 泄漏历史 — 版本演进证据
- L442-444: DB>63 位图折衷注释 "trivial fix"

## 推断标注

- "FAST 双条件拒跑 (stale 低 + 冷却期) 是刻意的零开销设计" — 代码事实 (L218-231 显式), 动机 (省 CPU) 由 L52-55 注释支撑
- "HFE 序列放大 ×32 封顶防止无限循环" — 代码事实 (L170-174 有 32 封顶), "为什么 32"是推断 (无注释)
- "pow(0.98) 表 16 项覆盖 update_avg_ttl_times 1-16" — 代码事实 (L24 数组 16 元素 + L375 注释), 上限依据是循环里 update 次数 ≤16 是推断
