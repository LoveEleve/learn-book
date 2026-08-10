# Netty 源码分析 — 交接文档 v2

> 日期: 2026-08-08
> 状态: **全部 13 章完成!** NIO (Ch1-Ch3) + Netty 核心 (Ch4-Ch9) + 扩展 (Ch10-Ch12+Ch14) | 跳过 Ch13 Epoll

---

## §零 核心约束 (新 AI 必须先读)

### 0.1 必须区分三类文件

| 目录 | 性质 | 格式 | 读者 |
|------|------|------|------|
| `knowledge-planning/` | 内部验证 — 逐源提取表证明内容覆盖了全部源码知识点 | 提取表+聚合表+P1/P2/P3+深度分类+聚类 | AI 自己 |
| `outlines/` | 最终交付物 — 另一个 AI 能直接读并写出文章 | TOC 格式 (### N. 标题 + 源码锚点) | 新 AI / 用户 |
| `_work/` | 方法论文档 — Pass 0-1 + Pass 2 闭环笔记 | 素材，不是交付物 | AI 自己 |

**严禁混淆**: knowledge-planning 是内部验证工具，outlines 是交付物。呈报 outlines 时说"这是大纲"✅，呈报 knowledge-planning 时说"这是大纲"❌。

### 0.2 完整管线 (每章必须走)

```
01 逐源提取 → 01 聚合 (P1/P2/P3) → 02 深度分类 (🔴🟡🟢) → 03 聚类 (教学顺序)
  → outlines/ TOC 大纲 → completeness-questions.md (全视角验证)
```

**不可跳过任何步骤**。Ch4-Ch9 的旧大纲 (已在 `_archive/`) 就是因为跳过了 knowledge-planning 而直接跳到 Per-Article Outline。

### 0.3 TOC 大纲格式

每篇大纲必须是分层编号 TOC 格式 — **不是叙事散文**。正确格式：

```markdown
### 1. 核心机制 — 一句话描述
  - 子项 — 解释 (SourceFile.java:NNN)
  - 子项 — 解释 (SourceFile.java:NNN)

### 2. 机制二 — 一句话描述
  - 子项 (SourceFile.java:NNN)
```

**严禁**: "问题引入"、"叙事顺序"、"概念依赖链"、"收束"等叙事段。这些是写作指导，不是大纲。

### 0.4 全视角验证

每个域大纲完成后必须写 `completeness-questions.md` — 从开发者/性能工程师/架构师/SRE/学生等身份提问，验证每问都能在 outline 找到答案。无缺口才能宣称完成。

### 0.5 桥规则

每个域末篇收束段必须显式写 "引出 Ch N+1"。跨章桥必须一致 — 不能 Ch3 结尾说"引出 Ch5"但下一章是 Ch4。

### 0.6 裸行号禁止

大纲中每处源码引用必须带完整文件名 — `(ByteBuf.java:233)` ✅，`(line 233)` ❌。

---

## 一、项目位置

```
Netty 源码:  /data/workspace/source-code/code/spring/netty/
JDK 11 源码: /data/workspace/jdk11u/
项目产出:    /data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/netty/
方法论:      /data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/talk-method/
```

方法论文档路径:
- 源码知识规划: `talk-method/knowledge-planning/methodology/zh/05-源码分析项目的知识规划.md`
- 最终产出物: `talk-method/source-code-analysis/methodology/zh/08-最终产出物.md`
- 三层循环: `talk-method/source-code-analysis/methodology/zh/01-三层循环框架.md`
- 方案选择: `talk-method/source-code-analysis/methodology/zh/04-方案选择决策树.md`

---

## 二、当前状态

### 2.1 NIO 上卷 (Ch1-Ch3) — ✅ 全部完成

| 域 | knowledge-planning | outlines | 篇数 | 锚点 | 全视角 | 状态 |
|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| Ch1 ByteBuffer | ✅ | ✅ | 3 | 27 | 32问 | ✅ |
| Ch2 Channel | ✅ | ✅ | 3 | 13 | 16问 | ✅ |
| Ch3 Selector | ✅ | ✅ | 2 | 8 | 12问 | ✅ |

### 2.2 Netty 下卷 (Ch4-Ch14) — Ch4 ✅, Ch5-Ch14 ⏳

| 域 | 方案 | knowledge-planning | outlines | 状态 |
|:--:|:--:|:--:|:--:|:--:|
| Ch4 ByteBuf | 🔴A | ✅ | ✅ (5篇) | ✅ |
| Ch5 EventLoop | 🔴A | ✅ | ✅ (4篇) | ✅ |
| Ch6 Promise/Future | 🟡B | ✅ | ✅ (3篇) | ✅ |
| Ch7 Pipeline | 🔴A | ✅ | ✅ (4篇) | ✅ |
| Ch8 MemoryPool | 🔴A | ✅ | ✅ (4篇) | ✅ |
| Ch9 Bootstrap | 🟡B | ✅ | ✅ (2篇) | ✅ |
| Ch10 Codec | 🟡B | ✅ | ✅ (2篇) | ✅ |
| Ch11 HTTP Codec | 🟡B | ✅ | ✅ (2篇) | ✅ |
| Ch12 HTTP/2 Codec | 🟡B | ✅ | ✅ (1篇) | ✅ |
| Ch13 Epoll | 🟡B | 跳过 | — | — |
| Ch14 HashedWheelTimer | 🟡B | ✅ | ✅ (1篇) | ✅ |

### 2.3 Netty 源材料 (已从 Pass 2 确定)

Ch4-Ch9 的源文件已在 `_work/` 中通过 Pass 2 深度扫描。Pass 2 闭环笔记可用作 knowledge-planning 的参考 — 但不是替代。

| 域 | _work/ 子目录 | Pass 2 闭环笔记 |
|:--:|------|:--:|
| Ch4 ByteBuf | `_work/01-bytebuf/` | 9 个 (Q1-Q9) |
| Ch5 EventLoop | `_work/02-eventloop/` | 10 个 (Q1-Q10) |
| Ch6 Promise | `_work/03-promise/` | 3 个 (Q1-Q3) |
| Ch7 Pipeline | `_work/04-pipeline/` | 7 个 (Q1-Q7) |
| Ch8 MemoryPool | `_work/05-memory-pool/` | 3 个 (Q1, Q2-Q5, Q6) |
| Ch9 Bootstrap | `_work/06-bootstrap/` | 1 个 (Q1-Q3 merged) |

### 2.4 基础文档

| 文件 | 状态 | 说明 |
|------|:--:|------|
| `00-domain-list.md` | ✅ | 11 域清单, 4🔴+7🟡 |
| `00-book-plan.md` | ✅ | 14 章 (Ch1-Ch3 NIO + Ch4-Ch14 Netty) |
| `00-approach-selection.md` | ✅ | 4A + 7B |
| `HANDOFF.md` | ✅ | 本文档 |

### 2.5 归档文件

| 目录 | 内容 | 说明 |
|------|------|------|
| `outlines/_archive/` | 24 个旧大纲文件 (Ch1-Ch9) | 旧叙事散文格式, 仅供参考 |
| `_work/` | 6 个域工作目录 | Pass 0-1 + Pass 2 素材 |

---

## 三、启动命令

```bash
# 1. 读方法论 (绝对路径)
Read /data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/talk-method/knowledge-planning/methodology/zh/05-源码分析项目的知识规划.md

# 2. 查看已完成域
BASE=/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/netty
ls $BASE/knowledge-planning/
ls $BASE/outlines/ch1-bytebuffer/
ls $BASE/outlines/ch2-channel/
ls $BASE/outlines/ch3-selector/

# 3. 查看 Ch1 全管线示例 (按此格式继续 Ch4)
Read $BASE/knowledge-planning/ch1-bytebuffer.md      # 知识规划 (内部验证)
Read $BASE/outlines/ch1-bytebuffer/01-core-abstraction.md  # TOC 大纲 (交付物)

# 4. Ch4 是下一个 — Netty ByteBuf
# Netty 源码在 /data/workspace/source-code/code/spring/netty/buffer/src/main/java/io/netty/buffer/
# 参考 _work/01-bytebuf/ 的 Pass 2 闭环笔记了解域结构
# 然后执行完整管线: 01逐源提取 → 01聚合 → 02深度 → 03聚类 → TOC outlines → 全视角验证
```

---

## 四、踩坑记录

| # | 坑 | 如何避免 |
|:--:|------|------|
| 1 | 跳步 — 直接从域发现跳到 Per-Article Outline | 每章必须走完整 01→02→03→04 管线 |
| 2 | 混淆 knowledge-planning 和 outlines | knowledge-planning=内部, outlines=交付物 |
| 3 | 大纲写成叙事散文 ("问题引入""叙事顺序") | 换成 TOC 格式 — `### N. 标题` |
| 4 | 裸行号 `(line 233)` | 必须带文件名 `(ByteBuf.java:233)` |
| 5 | 批量写文章 | 一次一章, 一章走完完整管线再下一章 |
| 6 | 从 Ch4 开始不从头 | 知识规划必须从 Ch1 开始, 全部域走完 |
| 7 | 跨章桥断裂 | 每章末篇必须写 "引出 Ch N+1" |
| 8 | 跳过全视角验证 | 每域必须写 completeness-questions.md |
| 9 | 忘记 metodology 路径声明 | 开始任何域前输出方法论路径块 |
| 10 | 信任旧 Pass 2 笔记 | Pass 2 是素材 — 不能替代 01 逐源提取 |
