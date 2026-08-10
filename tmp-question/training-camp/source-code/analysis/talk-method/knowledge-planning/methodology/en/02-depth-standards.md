# Knowledge Point Depth Standards

> Role: AI self-constraint SOP. Must read before writing content for any knowledge point.
> Prerequisite: `01-toc-extraction.md`
> Reading order: 01 → 02 (this doc) → 03 → 04.
> Note: This model's depth criterion differs from source-analysis 03 (code carries design decisions?) — here depth is driven by **learner need** (reader perspective), not code-structural value.

## Core Principle

**Books provide topic boundaries, not content.** The TOC tells you WHAT to cover. The AI generates the content. Depth is determined by the knowledge point's real-world relevance — NOT by how much the book says about it.

### Relationship with 01 §3

| Dimension | 01 §3 (Priority) | 02 §2 (Importance) |
|-----------|-----------------|---------------------|
| What it measures | Multi-book consensus signal | Real-world relevance |
| How it's computed | Book appearance count / N | Interview frequency + production frequency + dependency count |
| What it determines | Confidence in "this matters" | How deep AI should write |

**They are independent axes:** A Priority 1 knowledge point (appears in 3/3 books) can be 🟢 low importance. A Priority 3 knowledge point (appears in 1/3 books) can be 🔴 high importance. Judge each separately.

---

## 1. Three-Layer Depth Model

| Layer | Output | Example: CFS Scheduler |
|-------|--------|----------------------|
| **表层** (Surface) | Concept name + one-sentence definition | "CFS is Linux's Completely Fair Scheduler, using a red-black tree ordered by vruntime" |
| **工作层** (Working) | Mechanism principles + behavior explanation + why it's designed this way | vruntime calculation, nice value mapping, how I/O-bound tasks naturally get priority, scheduling latency design |
| **深层** (Deep) | Working-level content + key architectural components + optional key source snippets as structural evidence | Text description of `sched_entity`, `cfs_rq`, `pick_next_task_fair()` components and their relationships |

### Deep Layer: Required vs Optional

```
REQUIRED (structural components):
  - List key data structures with field names and their roles
  - List key functions with their entry/exit points
  - Describe the flow between components (architecture diagram in text)
  - Format: plain text description, NOT C syntax

OPTIONAL (source snippets as evidence):
  - struct definitions, function signatures, call chain excerpts
  - Format: actual code blocks from kernel source or reference
  - Purpose: structural evidence, not full source analysis
  - Must be grep-verifiable. Mark as [TODO: verify] if unverified.
```

---

## 2. Importance Classification

Every knowledge point is assigned an importance level. Depth is determined by importance, NOT by book page count.

| Importance | Criteria | Book Role | AI Role | Depth |
|-----------|----------|-----------|---------|-------|
| 🔴 High-Freq / Core | Frequently asked in interviews AND/OR commonly encountered in production | Provides topic direction | **Write in detail** — supplement when books are insufficient | Deep |
| 🟡 Common / Supporting | Needed to understand ≥3 other knowledge points AND occasionally encountered | Provides topic direction | Working-level explanation | Working |
| 🟢 Obscure / Edge | Rare scenarios, pure extension knowledge | If book mentions it, include a brief note | **1-2 sentences** — definition only | Surface |

### Diagnostic Questions

```
1. Frequently asked in interviews OR commonly encountered in production?
   ├── Yes → 🔴, Deep layer
   └── No → Continue

2. Do ≥3 other knowledge points depend on understanding this,
   AND it is occasionally encountered in real usage?
   ├── Yes → 🟡, Working layer
   └── No → Continue

3. Does the book mention it?
   ├── Yes → 🟢, Surface layer (book mentions → 1-2 sentences)
   └── No → Default: do NOT include. If it's within topic boundary
            and a 🔴 item's supporting concept → mark as
            [Gap: {concept}] per 01 §5 Step 3. Do NOT supplement here — this is deferred to 04 §4 where 🔴 gaps WILL be AI-supplemented at merge stage.
```

### Judgment Sources

```
Q1 (Interview/Production frequency):
  - Reference common interview resources (JavaGuide, 小林coding, 面试鸭)
  - Think: "Would ≥50% of backend engineer interviews ask about this?"
  - Think: "Does a backend engineer encounter this in production monthly?"
  - If uncertain → mark [TODO: verify importance — needs user confirmation]

Q2 (Dependency count):
  - Uses the preliminary dependency scan output from 03 §1 (same artifact as 
    03's coarse graph — shared, not independently built here)
  - "A depends on B" = without understanding B's mechanism,
    you cannot understand A's behavior or design rationale.
    Dependency is MECHANISM-level, NOT conceptual correlation.
    Count only direct dependencies within the same or adjacent topic.
  - **Two-phase execution**: Phase 1 (this doc) uses the coarse scan — 
    Q2 defaults to "No" when uncertain. Phase 2 (after 03 §2 builds the 
    full dependency graph) RE-RUNS Q2 for any 🟡 candidates flagged as 
    potentially under-classified. The re-run must happen before writing.
```

### Boundary Rules

- **Lower bound**: Interview + production essentials MUST reach deep layer
- **Upper bound**: 🟢 items limited to surface layer (1-2 sentences)
- **AI supplement**: For 🔴 items where books provide only topic direction, AI must expand to deep-layer coverage. This supplements WITHIN the book's scope — the topic IS from the TOC, only the depth is self-generated. For 🔴 items COMPLETELY absent from the book → mark `[Gap]`, do NOT fabricate here — supplement happens at merge stage (see 04 §4).
- **Single-book topics**: N=1 can still have 🔴 items — "frequently asked" is independent of "how many books cover it"

| Scenario | Action |
|----------|--------|
| 🔴, book gives topic direction only | AI writes deep from external knowledge |
| 🔴, book covers but insufficient depth | AI supplements to deep |
| 🔴, book does NOT mention at all | Mark `[Gap: {concept}]`, per 01 §5 Step 3 |
| 🟡, book covers sufficiently | AI writes working-level (no supplement needed) |
| 🟡, book doesn't cover, ≥3 deps | AI writes working-level |
| 🟢, mentioned in TOC | 1-2 sentences |
| 🟢, not mentioned in TOC | Do NOT include |

---

## 3. Per-Layer Writing Requirements

### Surface Layer (🟢)

```
Required: Concept name + one-sentence definition
Optional: Origin (which version/introduced by whom)
Prohibited: Mechanism explanation, behavioral analysis, source snippets

Example:
"O(1) Scheduler was Linux's scheduler before CFS (2.6.0-2.6.22), 
using priority arrays with constant-time scheduling."
```

### Working Layer (🟡)

```
Required: How the mechanism works + why it was designed this way
Required: Behavior under different scenarios (normal, edge case, failure)
Optional: Comparison with alternatives, tradeoffs
Prohibited: Source code snippets, kernel struct definitions

Example:
"The Completely Fair Scheduler (CFS) assigns each task a virtual runtime (vruntime).
Tasks with smaller vruntime run first. I/O-bound tasks naturally accumulate less 
vruntime, giving them implicit priority over CPU-bound tasks. The nice value 
maps to a weight that scales how fast vruntime grows — higher priority (lower 
nice) means slower vruntime growth and more CPU time."
```

### Deep Layer (🔴)

```
Required: All working-layer content
Required: Key architectural components (data structure names + field roles, 
          function signatures + call flow, architecture diagram in text)
Optional: Source snippets as structural evidence (struct definitions, function 
          call chains — grep-verified from kernel source or official docs)
Prohibited: Line-by-line source analysis, implementation details, patch-level discussion

Example (key architectural components in text):
"Components:
- sched_entity: per-task scheduling state (vruntime, exec_start, load weight)
- cfs_rq: per-CPU runqueue root (tasks_timeline: red-black tree root, 
  min_vruntime: prevents new task starvation, nr_running: active task count)
- pick_next_task_fair() → pick_next_entity() selects leftmost node

Flow: timer interrupt → update_curr() → resched → schedule() 
→ pick_next_task_fair() → context_switch()

Optional source evidence:
  struct sched_entity { u64 vruntime; struct load_weight load; ... }
  struct cfs_rq { struct rb_root_cached tasks_timeline; u64 min_vruntime; ... }
  (grep-verified from kernel/sched/sched.h)"
```

---

## 4. Anti-Fabrication Rules (apply to ALL writing layers)

These rules apply whenever AI writes content — not just during supplement.

1. **Every structural claim must be independently verifiable.**
   - Kernel structures: `grep 'struct sched_entity'` in kernel source repo
   - Function signatures: confirm function exists via source grep or external reference
   - Call chains: verify each function in the chain exists
   - Mark as `[TODO: verify]` if source access is not available

2. **Cross-verify structural claims from ≥2 sources** (kernel source grep + official docs + reputable online reference). If only 1 source, mark `[TODO: verify]`.

3. **TODO marker family** — canonical markers across all knowledge-planning docs:
   - `[TODO: verify]` — uncertain claim needs verification (default)
   - `[TODO: evaluate]` — decision needs evaluation (e.g., is this a knowledge point?)
   - `[TODO: verify importance — needs user confirmation]` — importance unclear
   - `[TODO: <action>]` — other pending action
   All markers use `[TODO: ...]` prefix (en) or `[待...]` (zh). Do NOT create new marker formats outside this family.

4. **No invented algorithms or formulas** — if exact formula unknown, describe conceptually.

5. **Supplement is filling gaps within the book's scope** — NOT adding unrelated topics.

---

## 5. Output Format

### Per-Knowledge-Point Depth Decision

```
## Depth Decision: {Knowledge Point Name}

01 Priority: P1/P2/P3 (from 01 §3)
Importance: 🔴/🟡/🟢
Why: [1-2 sentence reasoning from diagnostic questions]
Target Depth: Surface / Working / Deep
Book coverage: [sufficient (working+ content) / insufficient (surface-level or tangential) / topic direction only (TOC mentions the concept but no section content) / mentioned in TOC (only TOC entry, no chapter body) / not mentioned (zero coverage)]

Mapping to 04 §1 Coverage Levels:
- sufficient → Sufficient
- insufficient → Insufficient
- topic direction only → Insufficient
- mentioned in TOC → Insufficient
- not mentioned → Absent
AI supplement needed: Yes / No
If yes, supplement scope: [conceptual (mechanism explanation) / structural (data structures + call flow) / full deep-layer (all §3 deep requirements)]
```

### Per-Topic Depth Summary

```
## Depth Summary: {Topic Name}

| Knowledge Point | 01 Pri | Importance | Depth | Supplement |
|----------------|--------|-----------|-------|-----------|
| CFS Scheduling | P1 | 🔴 | Deep | Full deep-layer |
| Real-time Scheduling | P2 | 🟡 | Working | None |
| O(1) Scheduler | P2 | 🟢 | Surface | None |
```

---

## 6. Completion Checklist

After classifying depth for all knowledge points in a topic:

- [ ] **Every knowledge point classified**: 🔴/🟡/🟢 assigned with reasoning
- [ ] **01 Priority recorded**: P1/P2/P3 from 01 §3 cross-book aggregation
- [ ] **Lower bound met**: All interview + production essentials reach deep layer
- [ ] **Upper bound respected**: 🟢 items limited to surface layer (1-2 sentences)
- [ ] **AI supplement scope defined**: Every AI-supplemented knowledge point has a supplement scope listed
- [ ] **Gap items marked**: 🔴 items with zero book coverage marked `[Gap]`, not fabricated
- [ ] **No invented claims**: All structural claims during supplement are verifiable and ideally cross-verified from ≥2 sources
- [ ] **Uncertainty markers consistent**: All uncertain claims use `[TODO: verify]` (en) / `[待验证]` (zh)
- [ ] **Depth decisions documented**: Per-knowledge-point decision table and per-topic summary both completed
- [ ] **Ready for user review**: User confirms importance classification before AI starts writing content

---

## 7. Anti-Patterns

### 1. Book-page-count determines depth

```
WRONG: "This book has 50 pages on process scheduling → Deep"
RIGHT: Real-world relevance determines depth. A 2-page 🔴 topic gets AI expansion.
       A 50-page 🟢 topic gets 1-2 sentences.
```

### 2. All knowledge points treated equally

```
WRONG: Every extracted knowledge point gets working-layer treatment
RIGHT: 🔴 deep, 🟡 working, 🟢 surface — depth matches importance
```

### 3. Supplement becomes source code analysis

```
WRONG: "CFS deep → read kernel source → explain enqueue_entity() line by line"
RIGHT: Deep = working-layer content + architectural components (struct/function names, 
       call flow, architecture diagram) + optional source snippets as structural evidence.
       Do NOT trace into implementation bodies.
```

### 4. Skipping interview/production essentials

```
WRONG: "Book doesn't cover scheduling latency in detail → skip"
RIGHT: 🔴 items MUST be fully covered regardless of book depth
```

### 5. Confusing 01 Priority with 02 Importance

```
WRONG: "P1 (appears in 3 books) → must be 🔴 deep"
RIGHT: P1 = multi-book consensus signal. 🔴 = real-world importance.
       They are independent axes. Judge each separately.
```

### 6. "Will be covered later" / "see above" in knowledge explanation

```
WRONG: "CFS uses nice values (nice values will be explained in §Process Priority)"
WRONG: "As mentioned in §Process Priority, nice values determine..."
       — backward reference to a concept NOT yet explained (reader hasn't seen it)
RIGHT: "CFS uses nice values (see §Process Priority — covered earlier)"
       — backward reference to an ALREADY-EXPLAINED concept is correct.
       If CFS depends on nice values, nice values are
       explained FIRST, then CFS can reference them.

Knowledge points MUST follow topological order — just like domains in source-code-analysis 02. Any complex concept = basic concepts combined. A depends on B → explain B before A. Never write forward references to unlearned concepts. Backward references to already-explained concepts are not only allowed but expected.
