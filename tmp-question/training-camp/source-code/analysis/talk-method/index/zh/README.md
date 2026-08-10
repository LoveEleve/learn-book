# Talk-Method — AI 源码分析与知识规划基础设施

> **先读这个。** 这是所有 AI 代理使用此项目的入口。不要随意读单个文档——按下面规定的阅读顺序来。

## 这是什么

两个完整的 AI 方法论框架：

| 框架 | 用途 | 使用场景 |
|------|------|---------|
| **source-code-analysis** | 如何分析任意框架的源码 | 分析 JVM/Netty/Spring/Kafka/Redis 源码 |
| **knowledge-planning** | 如何从书籍 TOC 中规划知识 | 从书籍目录规划课程 |

两个框架共用同一模式：**methodology**（SOP 操作规范）→ **prompt**（AI 约束契约）→ **skills**（快速参考卡）。

---

## 目录结构

```
talk-method/
├── index/                    ← 你在这里 — AI 使用指南
├── source-code-analysis/
│   ├── methodology/en+zh/    ← 7 份 SOP（00-06）
│   ├── prompt/en+zh/         ← AI 自约束契约
│   └── skills/en+zh/         ← 快速参考卡
├── knowledge-planning/
│   ├── methodology/en+zh/    ← 4 份 SOP（01-04）
│   ├── prompt/en+zh/         ← AI 自约束契约
│   └── skills/en+zh/         ← 快速参考卡
└── progress/                 ← 会话交接文档
```

---

## 源码分析 — 阅读顺序

需要分析框架源码时：

### 1. 一次性设置（每个项目周期一次）

```
prompt/en/self-constraint-prompt.md     ← 读一次。绑定契约。
skills/en/01-quick-reference.md         ← 读一次。命令/模板速查。
```

### 2. 逐框架分析

```
methodology/en/00-domain-discovery.md   ← 发现要分析什么
  → 从入口点展开 + 旁路扫描 + 信号分类
  → 产出：带 🔴/🟡 层级和置信度的域清单

methodology/en/01-three-layer-loop.md   ← Pass 0-3 执行
methodology/en/02-dependency-order-decomposition.md   ← 构建依赖图 + 拓扑排序
methodology/en/03-analysis-depth-standards.md    ← "代码承载设计决策吗"
methodology/en/04-approach-decision-tree.md  ← 按域选 A/B/C/D 方案
methodology/en/05-mcp-tool-usage-principles.md     ← 各 Pass 用哪个 MCP 工具
methodology/en/06-cross-domain-reference-strategy.md  ← 跨域引用 + 发现反馈
```

### 3. 执行流程

```
prompt §1.5：域分析前问题（grill-me——对齐用户）
  → prompt §2：域分析前检查单
    → 00：发现域
      → 04：按域选方案
        → 01：执行 Pass 0→1→2→3
          ← 03：深度检查（Pass 2 时）
          ← 05：工具分配（全 Pass 时）
          ← 06：跨域引用（Pass 3 时）
```

---

## 知识规划 — 阅读顺序

需要从书籍 TOC 规划知识时：

### 1. 一次性设置

```
prompt/en/self-constraint-prompt.md     ← 读一次。绑定契约。
skills/en/01-quick-reference.md         ← 读一次。模板速查。
```

### 2. 逐主题规划

```
methodology/en/01-toc-extraction.md     ← 从 TOC 提取知识点
  → 产出：单书提取表 + 主题聚合

methodology/en/02-depth-standards.md    ← 深度分类（🔴/🟡/🟢）
  → 产出：逐知识点深度决策 + 主题汇总

methodology/en/03-topic-clustering.md   ← 主题聚类 + 依赖图
  → 产出：主题结构 + 教学顺序

methodology/en/04-cross-book-dedup.md   ← 多书合并 + 缺口处理
  → 产出：内容计划 + 来源标注
```

---

### 产出目录

所有分析产出存放在分析根目录下，按框架名组织：

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/
├── netty/
│   ├── 00-domain-list.md         ← 域发现清单
│   ├── 01-eventloop/             ← 域 EventLoop 的各 Pass
│   ├── 02-bytebuf/               ← 域 ByteBuf 的各 Pass
│   └── ...
├── jvm/
├── spring/
├── dubbo/
└── ...
```

AI 开始分析时创建此目录。每个域有独立子目录存放各 Pass 产出。

---

## 关键约定

### 语言

- **en/** = AI 执行专用
- **zh/** = 仅供人类审查

### 统一标记

| 标记 | en | zh |
|------|-----|-----|
| 不确定性 | `[TODO: verify]` | `[待验证]` |
| 缺口 | `[Gap: {concept}]` | `[缺口：{概念}]` |
| 阻塞 | `[BLOCKED — Topic X not yet complete]` | `[BLOCKED — 主题 X 尚未完成]` |
| 未解决 | `[Unresolved: {claim A} vs {claim B}]` | `[未解决：{声明 A} vs {声明 B}]` |
| 单书 | `[N=1 — consensus signal unavailable]` | `[N=1 — 共识信号不可用]` |

### MCP 工具

9 个工具已安装可用。详见 `source-code-analysis/methodology/en/05-mcp-tool-usage-principles.md`。

### Grill-Me

每次分析前问 5 个问题（AI 提推荐答案，用户确认）。详见 `source-code-analysis/prompt/en/self-constraint-prompt.md §1.5`。

### Wait-What

用户可随时中断 AI。详见 `source-code-analysis/prompt/en/self-constraint-prompt.md §6`。

---

## 快速开始 — 源码分析

```
1. 读 index/zh/README.md           （本文）
2. 读 prompt/en/self-constraint-prompt.md
3. 读 skills/en/01-quick-reference.md
4. 问用户域分析前问题（§1.5）
5. 发现域（00）
   → AI 发现域 + 分类重要性
   → 向用户呈报域清单确认
   → 等用户批准后再进入第 6 步
6. 按域选方案（04）
7. 执行 Pass 0-3（01 + 03 + 05 + 06）
```

## 快速开始 — 知识规划

```
1. 读 index/zh/README.md           （本文）
2. 读 prompt/en/self-constraint-prompt.md
3. 提取知识点（01）
4. 深度分类（02）
5. 聚类 + 排序（03）
6. 合并 + 补充（04）
```
