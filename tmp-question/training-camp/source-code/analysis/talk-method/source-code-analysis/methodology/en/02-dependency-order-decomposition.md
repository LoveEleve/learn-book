# Dependency Order & Decomposition Principles

> Role: AI self-constraint SOP. Must read before execution.
> Prerequisite: `01-three-layer-loop.md`
> Reading order: 01 → 02 (this doc) → 03 → 04 → 05 → 06. See 01 for full execution sequence.

## Principle 1: Dependency Order

### 1.1 Baseline Rule

A domain nearly always depends on MULTIPLE other domains. Assume multi-dependency by default.

```
WRONG: "B will be covered later" — reader cannot understand A without B
RIGHT: ALL dependencies analyzed first → A references them as already-known
```

### 1.2 Build Dependency Graph (mandatory, before first domain)

For every domain in a repo, list ALL domains it depends on. Format:

```
Netty N-3 Pipeline:
  depends on: N-2 (EventLoop — HeadContext registered on EventLoop)
              N-1 (ByteBuf — data flows through Pipeline as ByteBuf)
  does NOT depend on: N-4 (opposite direction)
```

Do this for ALL domains in the repo before analyzing the first one. Any domain whose dependency list is [PENDING] is blocked — do not start until the graph is complete.

**Pre-analysis scanning**: Before building the dependency graph, do a lightweight scan of ALL domains — `find src/main/java -type d` for package structure, `grep '^public (class|interface)'` for core class declarations, and `grep -E '^import|^@'` for top-level dependencies. This is NOT the full Pass 1 — no inheritance tree, no flagged questions. Just enough to identify dependency relationships between domains.

### 1.3 Topological Sort

After building the graph, sort domains so every dependency appears BEFORE the domain that needs it. Domains with zero dependencies go first.

### 1.4 Circular Dependencies

**Detection**: If A depends on B and B depends on A, the topological sort fails.

**Strategy (three rounds)**:

1. **Both domains do Pass 0 (Pre-Code Reading) + a modified Pass 1 (inheritance tree + element decomposition only) — skip flagged questions and test file reading.** Establish basic concepts of BOTH sides — just enough to name the interface boundary between them.
2. **Then both do Pass 2 (Deep-Dive) together** — cross-verify between A and B in a single deep-dive session. Grep and read source in both domains simultaneously, cross-referencing interfaces, shared assumptions, and interaction points.
3. **Then both return to their respective approaches (per 04) and complete Pass 3 if applicable**

The principle: when concepts are interdependent, you cannot fully learn one before the other. Do Pass 0+1 on both first, then dive deep on both with cross-references.

**Examples of circular dependencies**:
- Spring: `BeanPostProcessor` ↔ `BeanFactory` (BPP modifies beans created by BF, BF is created via BPP)
- Netty: `Channel` ↔ `ChannelPipeline` (Channel holds Pipeline reference, Pipeline holds Channel reference)
- Kafka: `Producer` ↔ `RecordAccumulator` ↔ `Sender` (circular via background thread + batch queue)

---

## Principle 2: Composition/Decomposition

**Rule**: Any complex source code = basic code combined. Analysis strategy is decompose-then-compose.

```
WRONG: Analyze "Pipeline's complete request chain" directly
→ Get lost in HeadContext → Handler → TailContext complexity

RIGHT: First answer "What basic elements compose Pipeline?"
  ① Doubly-linked list (head → handler1 → handler2 → tail)
  ② Chain-of-responsibility (fireChannelRead → next.invoke())
  ③ Thread safety (EventLoop single-thread guarantee)
  
  Then: How the three combine = Pipeline's complete chain
```

**Embedded in Pass 1**: The "Scan Outline" output MUST include this question:

> **"What basic elements compose this module?"**

This forces decomposition into independently analyzable parts.

---

## Pre-Analysis Checklist (per repo)

Execute before starting the first domain in any repo:

- [ ] Complete dependency graph built (ALL domains mapped, no 【待补充】)
- [ ] Circular dependencies identified and marked
- [ ] For circular deps: three-round strategy recorded
- [ ] Cross-domain reference strategy reviewed (see 06 for format and discovery handling)
- [ ] Hub candidates identified: domains that are dependency root for ≥10 others (flag for Hub upgrade per 04 §Hub Upgrade)
- [ ] Topological sort verified (no dependency appears after its dependent)
- [ ] Confirm Pass 1 output template includes 'Basic Element Decomposition' section (see 01 Principle 2 reference)
