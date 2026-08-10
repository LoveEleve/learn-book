# Topic Clustering & Prerequisite Dependencies

> Role: AI self-constraint SOP. Must read before designing topic structure and teaching order.
> Prerequisite: `01-toc-extraction.md` + `02-depth-standards.md`
> Input: All extracted knowledge points for all books (from 01) + depth classifications (from 02)
> Reading order: 01 → 02 → 03 (this doc) → 04.

## Core Principle

**Knowledge points do not exist in isolation.** Any complex concept = basic concepts combined. If concept A depends on understanding concept B, B must be taught before A — never write "will be covered later" or "see above." This is isomorphic to source-code-analysis 02.

---

## 1. Topic Clustering

### How to Cluster

Books provide raw material. Topics are formed by **mechanism boundaries** — NOT by book titles.

```
WRONG: "Book A, B, C all have 'Linux' in title → one topic"
RIGHT: Process scheduling and memory management are separate mechanisms → separate topics
```

One book may contribute knowledge points to MULTIPLE topics. E.g., "Linux Kernel Internals" may contribute to process scheduling, memory management, AND file systems.

### Clustering Rules

1. **Mechanism boundary**: Knowledge points sharing the same underlying mechanism belong to the same topic
2. **Knowledge point count**: A topic should have ≥5 knowledge points; if <5, consider merging with an adjacent topic. *Adjacent* = shares a mechanism family (e.g., both handle process lifecycle events) OR sits on the same dependency chain (A→B→C means B is adjacent to both A and C). Pure spatial proximity is NOT sufficient for adjacency.
3. **Dependency locality**: If ≥80% of knowledge points in a candidate topic have internal dependencies (they depend on each other without crossing topic boundaries), the topic boundary is correct. Evaluate this rule using the preliminary dependency scan below — not the full §2 dependency graph which is built after clustering. The preliminary scan is coarse (1-2 most obvious deps per knowledge point); the 80% threshold tolerates this coarseness.
4. **No forced fit**: A knowledge point that doesn't naturally fit any topic → create a cross-topic reference or standalone mini-topic

### Preliminary Dependency Scan (before clustering finalization)

Before finalizing topic boundaries, do a lightweight dependency scan:
- For each knowledge point, list its 1-2 most obvious dependencies
- Use this coarse graph to evaluate the 80% locality rule
- The full dependency graph (with ALL deps, mechanism-level justification) is built in §2 — this is a preliminary estimate only

### Convergence Condition

Stop clustering when ALL of the following are met:
1. All knowledge points are assigned to a topic
2. Each topic has a written scope paragraph with mechanism boundary
3. The 80% locality rule is verified for all topics (via preliminary dependency scan)

If after 2 iterations the clustering is still unstable, flag the unstable knowledge points as `[TODO: clustering boundary unclear — needs manual review]` and proceed — the §2 full dependency graph will resolve remaining ambiguities.

### Clustering Output

```
## Topic Proposal

### Topic 1: {Name}
Scope: {one paragraph describing mechanism boundaries}
Knowledge points: {list with 01 Priority and 02 Importance}
Books contributing: {book A §X, book B §Y, ...}

### Topic 2: {Name}
...
```

---

## 2. Internal Topic Ordering

### Build Dependency Graph (mandatory, before writing any topic)

For every knowledge point in a topic, list ALL knowledge points it depends on:

```
KNOWLEDGE_POINT: CFS Scheduler
  depends on: Process Priority (nice values) — CFS uses nice→weight→vruntime
              Red-Black Tree — CFS tasks_timeline is an rb_root
              Timer Interrupt — scheduler is invoked from timer
              [BLOCKED — Topic 1 not yet complete]
  does NOT depend on: Real-time Scheduling (independent mechanism)
```

### Dependency Definition

```
"A depends on B" = without understanding B's mechanism,
you cannot understand A's behavior or design rationale.

Dependency is MECHANISM-level, NOT conceptual correlation.
"CFS and RT scheduling both involve the scheduler" is correlation, not dependency.
"CFS uses nice value mapping to compute weight" is mechanism dependency.
```

Do this for ALL knowledge points in the topic before writing the first one. Any knowledge point whose dependency list is incomplete → blocked.

### Topological Sort

After building the graph, sort knowledge points so every dependency appears BEFORE the dependent knowledge point. Zero-dependency knowledge points go first.

### Circular Dependencies

**Detection**: If A depends on B AND B depends on A, topological sort fails.

**Strategy (three rounds, isomorphic to source-code-analysis 02 §1.4)**:

1. **Both knowledge points do surface-level explanation only** — establish basic concepts on both sides, just enough to name the interface boundary
2. **Joint working/deep-level write** — explain both in a single session with cross-references
3. **Independent completion** — each returns to its depth level (per 02) and completes independently

**Examples of circular concepts**:
- Process Scheduling ↔ Context Switch (scheduling decides WHAT to switch, context switch is HOW to switch)
- Page Fault ↔ Memory Allocation (page fault triggers allocation, allocation changes what causes page faults)
- VFS ↔ File System Implementation (VFS defines the interface, implementation follows VFS contract)

### Ordering Output

```
## Teaching Order: {Topic Name}

Topological sort result:
1. Process Priority (leaf, zero deps)
2. Red-Black Tree (leaf, zero deps)
3. Timer Interrupt (leaf, zero deps)
4. CFS Scheduler (depends on 1, 2, 3)
5. Context Switch (depends on 4) [CIRCULAR with #4 — three-round strategy]
6. Multicore Scheduling (depends on 4, 5)
```

---

## 3. Cross-Topic Prerequisites

> §2→§3 flow: Cross-topic dependencies are discovered DURING §2's per-knowledge-point dependency graph construction — when a dependency belongs to a different topic. Record them immediately and propagate to §3 for formal prerequisite marking. Do NOT defer cross-topic dependency detection until §3.

### Dependency Definition (same as §2)

Cross-topic dependency follows the SAME definition as intra-topic (§2): **Dependency is MECHANISM-level, NOT conceptual correlation.** A Topic B depends on Topic A if and only if a knowledge point in Topic B requires understanding a knowledge point in Topic A's mechanism to explain its behavior or design rationale. Mere topical relatedness ("both are about Linux internals") does NOT constitute a dependency.

### Inter-Topic Dependencies

A knowledge point in Topic B may depend on a knowledge point in Topic A — the entire Topic B can only start after Topic A is complete.

```
Example:
Topic 3 (Memory Management) → depends on Topic 1's "Virtual Memory" concept
→ Topic 3 is BLOCKED until Topic 1 is done

Topic 6 (eBPF) → depends on Topic 1 (kernel internals) AND Topic 4 (networking)
→ Topic 6 is BLOCKED until Topics 1 AND 4 are done
```

### Dependency Marking

For each topic, list:
- **Prerequisite topics**: Must be completed BEFORE this topic
- **Dependent concepts**: Which specific knowledge points from other topics are needed
- **Why**: One sentence explanation of the dependency chain

```
Topic 3: Memory Management
Prerequisites: Topic 1 (Process)
Dependent concepts: Virtual Memory (Topic 1 §MMU), Page Tables (Topic 1 §TLB)
Why: Cannot understand paging/swapping without virtual memory concepts
```

### Cross-Topic Dependency Graph

Build a TOPIC-level dependency graph for the entire curriculum:

```
Topic 1 (Process Core)     ← No prerequisites (foundation)
Topic 2 (Memory)           ← Depends on Topic 1
Topic 3 (File System)      ← Depends on Topics 1, 2
Topic 4 (Networking)       ← Depends on Topics 1, 2
Topic 5 (Scheduling/Perf)  ← Depends on Topic 1
Topic 6 (eBPF/Observability) ← Depends on Topics 1, 4
Topic 7 (Data Structures)  ← No prerequisites (foundation, cross-cutting)
Topic 8 (System Programming) ← Depends on Topics 1, 2, 4
```

---

## 4. Completion Checklist

After designing topics and ordering for all knowledge points:

### Per-Topic

- [ ] **Topic boundary defined**: Mechanism boundaries clear, no knowledge point forced into wrong topic
- [ ] **Dependency graph built**: Every knowledge point has a complete dependency list with [BLOCKED] markers for incomplete deps
- [ ] **Topological sort verified**: No dependency appears AFTER its dependent in the teaching order
- [ ] **Circular deps identified**: Three-round strategy recorded for each circular pair
- [ ] **Cross-topic deps marked**: Any knowledge point depending on another topic's content is flagged with prerequisite topic name and dependent concept

### Curriculum-Level

- [ ] **Topic dependency graph built**: Every topic has prerequisite topics listed
- [ ] **Foundation topics first**: Topics with zero intra-curriculum dependencies are taught first
- [ ] **No circular topic deps**: If Topic A depends on B AND B depends on A, attempt re-clustering first (most circular topic deps indicate incorrect boundaries). If re-clustering fails (mechanism-level circularity is genuine — e.g., scheduling ↔ memory management), apply the three-round strategy (§2 Circular Dependencies) at the topic level: surface both topics → joint deep → independent completion.
- [ ] **Teaching sequence established**: Complete topic-level order documented
- [ ] **Blocked topics identified**: Any topic whose prerequisites are not yet complete
- [ ] **Ready for user review**: Topic structure and teaching order confirmed by user before writing begins

---

## 5. Anti-Patterns

### 1. Clustering by book title

```
WRONG: "Books A, B, C are about Linux → one Linux topic"
RIGHT: Books are raw material. Topics are mechanism boundaries. 
       A single book may contribute to 3 different topics.
```

### 2. Teaching by importance, not dependency

```
WRONG: "CFS is 🔴, Process Priority is 🟡 → teach CFS first"
RIGHT: CFS depends on Process Priority → teach Process Priority first regardless of importance
       Dependency ordering ALWAYS overrides importance ordering for teaching sequence
```

### 3. "Covered later" in cross-topic references

```
WRONG: "CFS uses nice values (nice values explained in Topic Process Priority, covered later)"
RIGHT: If Topic Process Priority isn't done yet → CFS CANNOT be in this topic.
       Either re-cluster or mark CFS as BLOCKED until Process Priority is complete.
```

### 4. Forcing low-count topics

```
WRONG: "Topic has only 3 knowledge points → must be its own topic"
RIGHT: If <5 knowledge points AND no clear mechanism boundary → merge
       with adjacent topic that shares mechanism dependencies.
RIGHT: If <5 knowledge points AND has a clear, independent mechanism boundary
       → keep as standalone mini-topic. The ≥5 rule is a guideline, not a
       hard cutoff. Mechanism boundary is the primary clustering criterion.
```
