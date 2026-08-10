# Session Handoff — 源码分析方法论建设

> **SUPERSEDED** by `session-002-handoff.md`
> 本 AI 完成的工作交接，下一个 AI 继续讨论
> 日期: 2026-08-05

---

## 一、项目背景

对 32 个 Spring 生态仓库（339 知识域）做系统源码分析。在正式开始前，先建立方法论基础设施。

## 二、已完成

### talk-method 目录结构

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/talk-method/
├── methodology/         ← AI SOP 操作规范
│   ├── zh/              ← 中文（人类审核）
│   │   ├── 01-三层循环框架.md            ✅ 已定稿（但需要同步 en 新增的 Pass0/时空溯源/极简复现）
│   │   ├── 02-依赖顺序与分解原则.md       ✅ 已定稿（多依赖+循环依赖+拓扑排序）
│   │   ├── 03-分析深度标准.md             ✅ 已定稿（设计决策判断标准）
│   │   ├── 04-方案选择决策树.md           空壳
│   │   ├── 05-MCP工具使用原则.md          空壳
│   │   └── 06-跨域交叉引用策略.md         空壳
│   └── en/              ← 英文（AI 执行用）
│       ├── 01-three-layer-loop.md          ✅ 已定稿（含 Pass0+时空溯源+极简复现）
│       ├── 02-dependency-order-decomposition.md  ✅ 已定稿
│       ├── 03-analysis-depth-standards.md  ✅ 已定稿
│       ├── 04-approach-decision-tree.md    空壳
│       ├── 05-mcp-tool-usage-principles.md 空壳
│       └── 06-cross-domain-reference-strategy.md  空壳
├── prompt/              ← 空（AI 启动时读的自约束 prompt，最高优先级）
├── skills/              ← 空（含 7 个领域子目录 generic/io/container/data/mq/rpc/observability）
├── harness/             ← 空（含 12 个框架子目录，用于极简复现验证）
└── progress/            ← 进度跟踪
    └── 源码分析执行计划.md   ← 339 域的完整路线图
```

### 关键决策

| 决策 | 内容 |
|------|------|
| AI 只读英文 | en/ 目录 = AI execution SOP；zh/ = 人类审核用 |
| 文档是 SOP 不是教程 | 每份文档是给 AI 的机械指令，含 bash 命令、产出模板、checkbox |
| 多依赖是常态 | 每个域列出**所有**依赖域，拓扑排序后才开始分析 |
| 循环依赖两轮化解 | 先双浅→再合深→再各收 |
| 深度标准 = 设计决策 | 不跳过"底层优化"——跳过的是无设计信息的机械代码 (getter/logger/deprecated) |
| 禁止编造 | 函数名/常量/数据结构必须 grep 验证后才能写入 |
| 禁止"后面讲" | 依赖必须先讲，不能引用未分析的域 |

---

## 三、下一步讨论

### 下一步：继续 methodology/04-05-06

先写完 methodology，再讨论 prompt。理由：prompt 是 methodology 的"执行摘要 + 强制规则"——如果 04(方案选择)、05(MCP 使用)、06(交叉引用) 都没定，prompt 写不全。

**待写文件**：

```
en/04-approach-decision-tree.md        ← 什么时候用 A/B/C/D 方案
en/05-mcp-tool-usage-principles.md     ← search_graph vs trace_path vs query_graph
en/06-cross-domain-reference-strategy.md ← 跨域回头看时机+引用格式
zh/04-05-06 同步
```

**写完后**：再讨论 `prompt/` 目录——从完整的 methodology 中提取约束，生成 AI 分析每个域前必读的启动契约。

### 同步：zh/01 需要更新

en/01 新增了 3 个段落（Pass0/时空溯源/极简复现），zh/01 还没同步。建议先同步再继续讨论。

### 外部参考

本会话从 GitHub/Google 收集的外部资源：
- Aria Stewart "How To Read Source Code" — 代码类型分类法
- OSS Guide — git log trick、Test 优先
- orient skill — Spinellis/Hermans/Storey 学术引用
- Google AI 的四阶段方法论 — Phase0(周边文档)、时空溯源(早期版本)、极简复现(费曼法)、认知负载控制

---

## 四、上下文状态（fs 验证）

- 当前讨论到: `methodology/04-方案选择决策树` — 尚未开始
- 下一个待写文档: `en/04-approach-decision-tree.md`
- zh/01 需要同步 en/01（en 224行 vs zh 162行，差 3 个新增段落）
- en/04-06 全 0 行(fs 验证)，zh/04-06 全 7 行(空壳)
- 中文文件按本地习惯命名，英文文件使用英文命名

## 五、下一个 AI 的第一个动作

1. 读本 handoff 文档
2. 同步 zh/01-三层循环框架.md（补 Pass0/时空溯源/极简复现三段）
3. 开始讨论 `methodology/04-方案选择决策树`
