# Session Handoff — 源码分析方法论 + 知识规划方法论 + openjdk-book 实战

> 交接给下一个 AI
> 日期: 2026-08-05

---

## 一、已完成

### 1. 源码分析方法论（source-code-analysis/）

```
talk-method/source-code-analysis/
├── methodology/ (en+zh, 00-06, ~1100行, 3轮审查49修复+00新写)
│   ├── 00-domain-discovery.md           域发现——从入口点展开（新写）
│   ├── 01-three-layer-loop.md          Pass 0→1→2→3 框架
│   ├── 02-dependency-order-decomposition.md  依赖图+拓扑排序+环形依赖三轮
│   ├── 03-analysis-depth-standards.md   "承载设计决策吗"深度标准
│   ├── 04-approach-decision-tree.md     A/B/C/D 方案决策树
│   ├── 05-mcp-tool-usage-principles.md  9个MCP工具按Pass分配+降级链
│   └── 06-cross-domain-reference-strategy.md 跨域引用+发现回流+三级响应
├── prompt/ (en+zh, ~158行)
│   └── self-constraint-prompt.md        AI启动契约——不可违反的硬约束
├── skills/ (en+zh, ~153行)
│   └── 01-quick-reference.md            通用速查卡（bash/grep/MCP模板）
└── harness/                              12框架空目录（待源码分析后回填）
```

**9 个 MCP 工具已安装**: codebase-memory-mcp / codegraph / sverklo / GitHub MCP / Git MCP / LSP MCP / JavaLens / Code Analysis Spring / clangd

### 2. 知识规划方法论（knowledge-planning/）

```
talk-method/knowledge-planning/
├── methodology/ (en+zh, 01-04, ~1000行, 1轮审查22修复)
│   ├── 01-toc-extraction.md            从TOC提取知识点（TOC粒度/置信度/多书交叉）
│   ├── 02-depth-standards.md           三层深度模型+三档重要性+AI补充规则
│   ├── 03-topic-clustering.md          主题聚类+拓扑排序+环形依赖三轮
│   └── 04-cross-book-dedup.md          跨书合并+不一致处理+缺口分析+AI补充
```

### 3. openjdk-book 实战验证

用方法论重写了 `vol-01/ch14/01-interpreter-init.md`（462→316行），验证了方法论的有效性。所有关键声明 grep 验证通过。

---

## 二、关键发现——方法论缺口

**核心缺口**: 方法论 01-06 解决了"怎么分析"，但**没有解决"分析什么"**。

337 个域是从之前 AI 的规划文档来的——但**没有一个方法论教 AI 如何在新框架中发现域**。知识规划 01 教了"怎么从 TOC 提取知识点"，但源码分析缺少对应的"怎么从源码树中发现知识域"的规范。

### 缺口定义

```
源码分析方法论: 给定一个框架（如 Netty）
  01: 怎么扫轮廓？    ← ✅ 已有（三层循环）
  02: 怎么依赖排序？   ← ✅ 已有
  03: 怎么判深度？     ← ✅ 已有
  04: 怎么选方案？     ← ✅ 已有
  05: 用什么工具？     ← ✅ 已有
  06: 怎么跨域引用？   ← ✅ 已有

缺口:
  00: 怎么从源码树中发现"该分析什么"？ ← ❌ 缺失
```

### 缺口需要什么

一份新的方法论文档——"如何从源码中发现知识域"——告诉 AI：
- 用什么命令发现框架的模块结构（find + grep）
- 用什么规则判断"这个模块值得分析"（设计决策密度？）
- 用什么格式输出"域发现清单"（域名/优先级/依赖关系/预估深度）
- 和知识规划 01（TOC 提取）的对应关系是什么

---

## 三、当前项目状态

| 项目 | 状态 |
|------|------|
| 源码分析方法论 01-06 | ✅ 定稿 |
| 知识规划方法论 01-04 | ✅ 定稿 |
| openjdk-book 实战验证 | ✅ 1 篇重写完成 |
| "发现知识域"方法论 | ❌ 未开始 |
| openjdk-book 93 篇存量 review | ❌ 未开始（等用户决定范围） |
| 知识规划 Phase 1 执行 | ❌ 未开始 |

---

## 四、下一个 AI 的第一个动作

**优先事项**: 讨论"如何从源码中发现知识域"方法论。

建议路径：
1. 讨论如何定义"知识域发现"的标准（模块结构扫描 → 设计决策密度评估 → 域输出格式）
2. 借鉴知识规划 01 的产出格式（章节映射/置信度/多书交叉），写出源码版
3. 写入 `source-code-analysis/methodology/en/00-domain-discovery.md`（或作为 01 的前置）

---

## 五、参考文档

| 文档 | 路径 |
|------|------|
| 源码分析方法论 | `talk-method/source-code-analysis/methodology/` |
| 知识规划方法论 | `talk-method/knowledge-planning/methodology/` |
| 实战标杆文章 | `openjdk-book/docs/openjdk/vol-01/ch14/01-interpreter-init.md` |
| 方法论标杆文章 | `openjdk-book/docs/openjdk/vol-01/ch10/07-g1-concurrent-mark-creation.md`（1289行） |
| openjdk 源码 | `/data/workspace/source-code/openjdk11u/` |
| 交接文档索引 | `talk-method/progress/` |
