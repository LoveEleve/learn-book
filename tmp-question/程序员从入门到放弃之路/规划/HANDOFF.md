# 内功修炼 — 知识规划交接文档

> 2026-08-08 | 7 主题完成, 5 主题待规划 | 语言: C/POSIX/C++混编
> 路径: `/data/workspace/source-code/book/成长之路/tmp-question/程序员从入门到放弃之路/`

---

## 零、启动步骤

拿到本文件后，按顺序执行：

1. **验证环境**
   ```bash
   ls /data/workspace/source-code/book/成长之路/tmp-question/程序员从入门到放弃之路/
   # 应看到: 规划/  内功修炼/  MySQL-数据库/  Java内功修炼/  HANDOFF.md
   ```

2. **读方法论** — 必须 Read 以下文件后再开始规划
   ```bash
   # 核心方法论 (按顺序读)
   cat .../knowledge-planning/methodology/en/01-toc-extraction.md
   cat .../knowledge-planning/methodology/en/02-depth-standards.md
   cat .../knowledge-planning/methodology/en/03-topic-clustering.md
   
   # 自我约束 Prompt (最后读)
   cat .../knowledge-planning/prompt/en/self-constraint-prompt.md

3. **确认已完成规划**
   ```bash
   wc -l 规划/内功修炼/01-OS内核.md   # 622
   wc -l 规划/内功修炼/04-网络.md      # 349 (largest)
   ls 规划/内功修炼/0*.md | wc -l      # 7 files
   ```

4. **检查待规划主题的原始TOC**
   ```bash
   ls MySQL-数据库/     # 14 books
   ls Java内功修炼/     # 19 books (use only JVM/GC/language subsets)
   ls 内功修炼/          # 24 books (2 for algorithms, already verified)
   ```

5. **开始规划** — 从待规划清单第一个主题开始，逐本 Section 级提取

---

## 方法论参考路径

| 组件 | 绝对路径 |
|------|------|
| methodology (en) | `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/talk-method/knowledge-planning/methodology/en/` |
| methodology (zh) | `.../knowledge-planning/methodology/zh/` |
| prompt (en) | `.../knowledge-planning/prompt/en/self-constraint-prompt.md` |
| prompt (zh) | `.../knowledge-planning/prompt/zh/自我约束prompt.md` |
| skills | `.../knowledge-planning/skills/en/` |
| 规划产出目录 | `/data/workspace/source-code/book/成长之路/tmp-question/程序员从入门到放弃之路/规划/` |
| TOC 源目录 | `.../内功修炼/` `.../MySQL-数据库/` `.../Java内功修炼/` |

---

## 一、完成状态

| # | 主题 | 文件 | 🔴 | 🟡 | 🟢 | 行数 | 状态 |
|:--:|------|------|:--:|:--:|:--:|:---:|:--:|
| 1 | OS 内核 | `规划/内功修炼/01-OS内核.md` | 26 | 35 | 23 | 622 | ✅ |
| 2 | 内存深度 | `规划/内功修炼/02-内存深度.md` | 16 | 20 | 5 | 228 | ✅ |
| 3 | 文件系统 | `规划/内功修炼/03-文件系统.md` | 6 | 6 | 10 | 115 | ✅ |
| 4 | 网络 | `规划/内功修炼/04-网络.md` | 16 | 34 | ~30 | 349 | ✅ |
| 5 | 系统性能 | `规划/内功修炼/05-系统性能.md` | 12 | 17 | ~8 | 223 | ✅ |
| 6 | eBPF | `规划/内功修炼/06-eBPF.md` | 11 | 12 | 5 | 202 | ✅ |
| 7 | 系统编程(C/C++) | `规划/内功修炼/07-系统编程.md` | 14 | 10 | 4 | 144 | ✅ |
| **合计** | | | **101** | **134** | **~85** | **1883** | |

### TOC 验证状态

24 本 内功修炼 TOC 已逐本验证无遗漏章节（2026-08-08），对照各自 `规划/` 提取表。
**MySQL 14 本 / Java 内功修炼 19 本 TOC 尚未验证** — 新 AI 规划时需逐本验证。

---

## 二、待规划

| # | 主题 | 书本数 | 书名清单 |
|:--:|------|:---:|------|
| 8 | MySQL-数据库 | 14 | MySQL内核设计与实现 / MySQL是怎样运行的 / 数据库内核揭秘 / 深入浅出存储引擎 / MySQL-8查询性能优化 / 大数据SQL优化原理与实践 / 千金良方金字塔法则 / MySQL高可用解决方案 / MySQL复制技术与生产实践 / 深入理解MySQL主从原理 / MySQL-Concurrency / MySQL实战 / DBA实战手记 / HikariCP连接池实战 |
| 9 | Java GC 专题 | 2 | 深入探索JVM垃圾回收 / 新一代垃圾回收器ZGC设计与实现 |
| 10 | Java JVM 核心 | 3 | 揭秘Java虚拟机 / JVM规范 / Java性能权威指南 |
| 11 | Java 语言+工程 | 6+ | JLS语言规范 / Java深度调试技术 / Effective-Java / Modern-Concurrency-in-Java / On-Java基础卷 / On-Java进阶卷 / Java开发实战 |
| 12 | 算法(面试) | 2 | 数据结构与算法之美 / 算法第4版 |

> 中间件 (Redis高手心法/Dubbo/RocketMQ/微服务白皮书) — 归 分布式 主题，不在 内功修炼。

---

## 三、目录结构

```
程序员从入门到放弃之路/
├── 规划/
│   ├── README.md                    ← 总索引
│   ├── HANDOFF.md                   ← 本交接文档
│   ├── 内功修炼/
│   │   ├── 01-OS内核.md              ← 6本, 84 KPs, 622行
│   │   ├── 02-内存深度.md            ← 2本, 41 KPs, 228行
│   │   ├── 03-文件系统.md            ← 1本, 22 KPs, 115行
│   │   ├── 04-网络.md               ← 5本, ~80 KPs, 349行
│   │   ├── 05-系统性能.md            ← 3本, ~37 KPs, 223行
│   │   ├── 06-eBPF.md               ← 3本, 28 KPs, 202行
│   │   └── 07-系统编程.md            ← 2本, 25 KPs, 144行
│   ├── MySQL/
│   │   └── 08-MySQL.md              ← [待开始] 14本
│   ├── Java/
│   │   ├── 09-Java-GC.md            ← [待开始] 2本
│   │   ├── 10-Java-JVM.md           ← [待开始] 3本
│   │   └── 11-Java-语言工程.md       ← [待开始] ~6本
│   └── 算法/
│       └── 12-算法.md               ← [待开始] 2本
│
├── 内功修炼/                       ← 24本原始TOC (全部验证通过 ✅)
├── MySQL-数据库/                   ← 14本原始TOC (待验证)
└── Java内功修炼/                   ← 19本原始TOC (待验证, 其中12本用于规划)
```

---

## 四、方法论 — 已固化规则 (24条)

### 01 提取
1. **Section 级粒度** — TOC 有 §X.Y 就必须拆到 §X.Y，不容许多节压缩为 1 行
2. **逐书立即写文件** — 提取完一本立即 Edit 追加到 `规划/{分类}/{编号}-{主题}.md`，不攒到最后
3. **步骤标记** — 每本带 `[01 #N/M done]` + KPs 计数声明
4. **三列表** — `| Original Chapter | Inferred Knowledge Point | Confidence |`
5. **逐本深审** — 每本提取完 → deep review → 修复 → 再继续下一本

### 01 聚合
6. **P1/P2/P3 逐项书源** — 聚合表每项标注具体书号，插入 01 提取和 02 深度之间
7. **N=1 标注** — `[N=1 — consensus signal unavailable]`
8. **N=3 无 P2** — P1=≥2(>1.5), P3=1, 不存在 P2

### 02 深度分类
9. **🔴 表** — `| Knowledge Point | 01 Pri | 为什么🔴 |`
10. **🟡 表** — `| Knowledge Point | 01 Pri | 说明 |`（禁止逗号串）
11. **🟢 表** — `| Knowledge Point | 01 Pri | 放在哪 |`
12. **8.5-9 分标准** — 诊断 Q1→🔴 Q2→🟡 Q3→🟢

### 03 聚类
13. **机制边界定义** — 每个集群说明为什么独立
14. **依赖链** — "A 依赖 B = 不理解 B 的机制无法理解 A 的行为"
15. **教学顺序** — 依赖图 → 拓扑排序

### 全局
16. **逐本 TOC 验证** — 原始 TOC 逐一对照规划文件，确认无遗漏章节

### 写作规范
17. TOC-only: AI 生成全部内容，书籍只提供主题边界
18. 语言: 内核层 C/POSIX，系统编程 C/C++混编，Java 专题 Java
19. 深度: 8.5-9/10，🔴含 struct/call chain，🟡机制+原因，🟢1-2句
20. 拓扑教学: A依赖B→先写B，禁止前向引用；后向引用已讲概念允许
21. 面试考点独立存储到 `{章节}/面试考点.md`，不写入正文
22. 代码块必须可编译或标注 `[pseudocode]`
23. 不限制行数/章数，讲透为止
24. 跨书合并仅在 04 步骤（需正文），TOC-only 跳过 04

---

## 五、下一步

1. **MySQL 规划** (`MySQL/08-MySQL.md`) — 14 本书，逐本 Section 级提取，从 B1(MySQL内核设计与实现) 开始
2. **Java GC 规划** (`Java/09-Java-GC.md`) — 2 本书
3. **Java JVM 规划** (`Java/10-Java-JVM.md`) — 3 本书
4. **Java 语言+工程** (`Java/11-Java-语言工程.md`) — ~6 本书
5. **算法** (`算法/12-算法.md`) — 2 本书, 面向面试/刷题单独规划

> **注意**: `MySQL/08-MySQL.md` 尚未开始。旧摘要版 `MySQL-数据库/规划-MySQL.md` 已作废（75行，不符方法论）。新 AI 按方法论从 B1 逐本开始。

---

## 六、已知陷阱

1. 一次性写多本书会被用户删除重来——每次只写一本
2. 逗号串🟡🟢表会被喊回——全部转表格
3. 聚合表缺 P1/P2/P3 逐项书源→需要插入01提取和02深度之间
4. 规划产出必须立即写文件——对话中的讨论不等于文件
5. N=1 缺 `[consensus signal unavailable]` 标注被要求修复
6. 🟡🟢表缺 `01 Pri` 列被要求修复
