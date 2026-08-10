# Talk-Method — AI Source Analysis & Knowledge Planning Infrastructure

> **READ FIRST.** This is the entry point for ALL AI agents working with this project. Do NOT read individual docs randomly — follow the prescribed reading order below.

## What Is This

Two complete methodological frameworks for AI agents:

| Framework | Purpose | When to Use |
|-----------|---------|------------|
| **source-code-analysis** | How to analyze ANY framework's source code | Analyzing JVM/Netty/Spring/Kafka/Redis source |
| **knowledge-planning** | How to plan knowledge from book TOCs | Planning curriculum from book table-of-contents |

Both frameworks share the same pattern: **methodology** (SOPs) → **prompt** (AI constraint contract) → **skills** (quick-reference cards).

---

## Directory Structure

```
talk-method/
├── index/                    ← YOU ARE HERE — AI usage guide
├── source-code-analysis/
│   ├── methodology/en+zh/    ← 7 SOP docs (00-06)
│   ├── prompt/en+zh/         ← AI self-constraint contract
│   └── skills/en+zh/         ← Quick-reference card
├── knowledge-planning/
│   ├── methodology/en+zh/    ← 4 SOP docs (01-04)
│   ├── prompt/en+zh/         ← AI self-constraint contract
│   └── skills/en+zh/         ← Quick-reference card
└── progress/                 ← Session handoff docs
```

---

## Source Code Analysis — Reading Order

When you need to analyze a framework's source code:

### 1. ONE-TIME SETUP (per project lifetime)

```
prompt/en/self-constraint-prompt.md     ← Read ONCE. Binding contract.
skills/en/01-quick-reference.md         ← Read ONCE. Command/template cheatsheet.
```

### 2. PER-FRAMEWORK ANALYSIS

```
methodology/en/00-domain-discovery.md   ← Discover WHAT to analyze
  → From entry point + sidepath scan + signal classification
  → Output: domain list with 🔴/🟡 tiers and confidence levels

methodology/en/01-three-layer-loop.md   ← Pass 0-3 execution
methodology/en/02-dependency-order-decomposition.md   ← Build dependency graph + topological sort
methodology/en/03-analysis-depth-standards.md    ← "Does code carry design decisions?"
methodology/en/04-approach-decision-tree.md  ← Select A/B/C/D per domain
methodology/en/05-mcp-tool-usage-principles.md     ← Which MCP tool at which pass
methodology/en/06-cross-domain-reference-strategy.md  ← Cross-domain references + discovery feedback
```

### 3. EXECUTION FLOW

```
prompt §1.5: Pre-Domain Questions (grill-me — align with user)
  → prompt §2: Pre-Domain Checklist
    → 00: Discover domains
      → 04: Select approach per domain
        → 01: Execute Pass 0→1→2→3
          ← 03: Depth check (during Pass 2)
          ← 05: Tool allocation (during all passes)
          ← 06: Cross-domain references (during Pass 3)
```

---

## Knowledge Planning — Reading Order

When you need to plan knowledge from book TOCs:

### 1. ONE-TIME SETUP

```
prompt/en/self-constraint-prompt.md     ← Read ONCE. Binding contract.
skills/en/01-quick-reference.md         ← Read ONCE. Template cheatsheet.
```

### 2. PER-TOPIC PLANNING

```
methodology/en/01-toc-extraction.md     ← Extract knowledge points from TOC
  → Output: per-book extraction tables + per-topic aggregation

methodology/en/02-depth-standards.md    ← Classify depth (🔴/🟡/🟢)
  → Output: per-knowledge-point depth decision + per-topic summary

methodology/en/03-topic-clustering.md   ← Cluster topics + build dependency graph
  → Output: topic structure + teaching order

methodology/en/04-cross-book-dedup.md   ← Merge multi-book coverage + handle gaps
  → Output: content plan + source attribution
```

### Output Directory

All analysis outputs are saved under the analysis root, organized by framework:

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/source-analysis/
├── netty/
│   ├── 00-domain-list.md         ← Domain discovery output
│   ├── 01-eventloop/             ← Domain EventLoop (Pass 0-3)
│   ├── 02-bytebuf/               ← Domain ByteBuf (Pass 0-3)
│   └── ...
├── jvm/
├── spring/
├── dubbo/
└── ...
```

AI creates this directory when analysis begins. Each domain gets its own subdirectory containing Pass outputs.

---

## Key Conventions

### Language

- **en/** = AI reads for execution
- **zh/** = Human review only

### Canonical Markers

| Marker | en | zh |
|--------|-----|-----|
| Uncertainty | `[TODO: verify]` | `[待验证]` |
| Gap | `[Gap: {concept}]` | `[缺口：{概念}]` |
| Blocked | `[BLOCKED — Topic X not yet complete]` | `[BLOCKED — 主题 X 尚未完成]` |
| Unresolved | `[Unresolved: {claim A} vs {claim B}]` | `[未解决：{声明 A} vs {声明 B}]` |
| N=1 | `[N=1 — consensus signal unavailable]` | `[N=1 — 共识信号不可用]` |

### MCP Tools

9 tools installed and available. See `source-code-analysis/methodology/en/05-mcp-tool-usage-principles.md`.

### Grill-Me

Before any analysis, ask 5 questions (AI proposes answers, user confirms). See `source-code-analysis/prompt/en/self-constraint-prompt.md §1.5`.

### Wait-What

User can interrupt AI at any time. See `source-code-analysis/prompt/en/self-constraint-prompt.md §6`.

---

## Quick Start — Source Analysis

```
1. Read index/en/README.md           (this file)
2. Read prompt/en/self-constraint-prompt.md
3. Read skills/en/01-quick-reference.md
4. Ask user Pre-Domain Questions (§1.5)
5. Discover domains (00)
   → AI discovers domains + classifies importance
   → PRESENT domain list to user for confirmation
   → Wait for user approval before step 6
6. Select approach per domain (04)
7. Execute Pass 0-3 per domain (01 + 03 + 05 + 06)
```

## Quick Start — Knowledge Planning

```
1. Read index/en/README.md           (this file)
2. Read prompt/en/self-constraint-prompt.md
3. Extract knowledge points (01)
4. Classify depth (02)
5. Cluster + order (03)
6. Merge + supplement (04)
```
