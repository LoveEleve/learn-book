# N-15 配置存储面 — 六层深审

> 方案 A, 3 篇大纲

## 审 1: 事实错误 — 2 修正
| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 域发现称 service+model 141 文件 | 实测: service 顶层 17 类 4,222 行 + 8 子目录 (repository 20/dump 23/query 17) — 巨域拆 3 篇 |
| 2 | 规划未提迁移/查询链/dump 族 | 三篇大纲全覆盖 (persist/操作/缓存) |

## 审 2: API/实现路径编造 — 1 修正
| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "存储只有 DB" | 实测: embedded (RocksDB+JRaft) vs external (DB) 双实现 + 磁盘 dump (Raw/RocksDb) — 存储形态精确化 |

## 审 3: 文件名/目录名推断 — 1 修正
| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "query 是简单查询" | 实测: ConfigQueryHandlerChain 链式 + builder + extractor 族 (17 文件) |

## 审 4: 跨项目概念转移 — 0
## 审 5: 覆盖率 — 0 缺漏 (接口族/双实现/操作/查询链/缓存/dump 6 面全覆盖)
## 审 6: 跨层一致性 — 0 (harness 待建)

## 结论: 4 项修正落盘
