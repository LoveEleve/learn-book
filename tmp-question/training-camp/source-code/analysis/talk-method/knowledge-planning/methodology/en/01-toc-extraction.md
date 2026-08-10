# How to Extract Knowledge Points from TOC

> Role: AI self-constraint SOP. Must read before extracting knowledge points from any book.
> Prerequisite: None (foundational document)
> Input: One or more book TOCs (table of contents only, NO body text)
> Note: Topic assignment is user-provided or pre-specified before extraction begins. This document handles per-topic extraction only.
> Reading order: 01 (this doc) → 02 → 03 → 04. Execution: user provides topic assignment → 01 extraction → 02 depth classification → 03 clustering+ordering → 04 merging → write.

## Core Constraints

1. **Only infer from TOC** — do not fabricate concepts, algorithms, or details not present in the directory structure. If the TOC only reaches level 2 (Chapter → Section), extract only to level 2. Do not guess what subsection content "should" exist.
2. **Mark uncertainty** — every inferred knowledge point that cannot be directly confirmed from the TOC heading text must be tagged `[TODO: verify]`. High-confidence extractions do not need this tag.
3. **Knowledge-point-first** — the organizing principle is "what must a backend engineer understand," not "what does the book contain" and NOT "what does an interviewer ask."
4. **No fabrication** — never invent concepts, formulas, or claims that the TOC does not support.

---

## 1. Extraction Strategy by TOC Granularity

| TOC Level | Example | Can Extract | Cannot Extract |
|-----------|---------|------------|----------------|
| Level 1 (Chapter only) | Ch3: "Process Scheduling" | Single broad knowledge point | Any sub-decomposition (no section detail) |
| Level 2 (Ch→Section) | Ch3 → 3.1 CFS / 3.2 Real-time / 3.3 Preemption | Coarse-grained knowledge point list | CFS internal vruntime calculation logic |
| Level 3 (Ch→Section→Subsec) | 3.1 CFS → 3.1.1 Sched Entity → 3.1.2 Vruntime | Decomposed sub-knowledge points | Specific algorithm details |
| Level 4+ (Deeper subsections) | 3.1.1.1 Sched avg vs Sched entity | Fine-grained sub-knowledge points | Implementation-level code details |
| Generic section headings | "Data structures and algorithms" | **Nothing** — too vague to extract | — |

**Rule**: Extract only to the depth the TOC provides. Do not guess deeper content from the book title or reputation. Level 4+ TOCs are rare but extractable — they provide fine-grained knowledge point decomposition but still do not provide implementation details.

---

## 2. What Counts as a Knowledge Point

### Is a Knowledge Point

- A concept a backend engineer cannot work without understanding (e.g., without understanding virtual memory, you cannot understand mmap)
- A mechanism that affects system behavior (e.g., how CFS scheduler's vruntime affects CPU allocation)
- A cross-component interaction contract (e.g., what registers are saved during kernel/user mode switch)

### Is NOT a Knowledge Point

- Pure command/tool usage (e.g., "grep's -r parameter" — this is a man page, not knowledge)
- Pure history (e.g., "Linux 0.01's scheduler was only 100 lines" — interesting but non-essential)
- Overly specific hardware details (e.g., "Intel CPU's L1 cache is 32KB" — hardware manual, not knowledge)

### Relationship with §3 Priority

§2 determines **whether** something is a knowledge point (per-knowledge-point quality). §3 determines its **priority tier** in a multi-book topic (per-knowledge-point consensus signal). These serve different purposes and do not conflict:

- If a knowledge point passes §2 but only appears in 1 book → still a knowledge point, but Priority 3 (Isolated)
- If a knowledge point appears in 3 books but fails §2 (e.g., "grep -r parameter" mentioned in 3 TOCs) → still NOT a knowledge point
- Priority tiers in §3 NEVER override the §2 yes/no decision

---

## 3. Multi-Book Cross-Validation — TOC Consensus Signal

When multiple books share similar chapter topics but with different subsection structures, this is NOT duplication — it is a **signal**:

```
Book A: 3.1 CFS / 3.2 Real-time Scheduling / 3.3 Multicore Scheduling
Book B: 2.1 Scheduling Policy / 2.2 Context Switch / 2.3 Preemption
Book C: 4.1 CFS / 4.2 Sched Entity / 4.3 Vruntime Calculation

All 3: CFS → P1 (Consensus — majority of N)
2 of 3: Real-time Scheduling → P2 (Strong Signal — minority of N)
Only 1: Vruntime Calculation → P3 (Isolated — needs evaluation)
```

**Generalized rule**:

| Books containing the knowledge point | Priority |
|--------------------------------------|----------|
| Majority (>N/2) of books | **Priority 1 — Consensus** |
| Minority of N books (>1) | **Priority 2 — Strong Signal** |
| Exactly 1 (isolated to one book) | **Priority 3 — Isolated** [TODO: evaluate if backend engineer needs this] |

For single-book topics (N = 1): all knowledge points are Priority 1 — no cross-validation possible. Note this in the aggregation output: `[N=1 — consensus signal unavailable]`.

**Multi-book cross-validation is not deduplication — it's the community/industry vote on what matters.**

---

## 4. Output Format

### Per-Book Extraction

```
## Knowledge Point Extraction: {Book Title}

### Chapter Mapping
| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| Ch3 Process Scheduling | CFS scheduling strategy | High (3.1 explicitly mentions "Completely Fair Scheduler") |
| Ch3 Process Scheduling | Scheduler data structures | Medium (3.5 heading is vague; needs full text verification) |
| Ch3 Process Scheduling | Context switch mechanism | High (3.3 heading explicitly says "context switch") |
```

### Per-Topic Aggregation (after all books in a topic extracted)

```
## Topic: {Topic Name} — Cross-Book Knowledge Point Aggregation

N books analyzed: {count}

### Priority 1 — Consensus (appears in majority of N books, >N/2)
- CFS scheduling strategy (Book A §3.1, Book B §2.1, Book C §4.1)

### Priority 2 — Strong Signal (appears in minority of N books, but >1)
- Real-time scheduling (Book A §3.2, Book C §4.2)

### Priority 3 — Isolated (appears in only 1 book)
- Vruntime calculation [TODO: evaluate if backend engineer needs this] (Book C §4.3)
```

### Confidence Levels

| Level | Criteria |
|-------|----------|
| **High** | TOC heading explicitly names the concept; no disambiguation needed |
| **Medium** | TOC heading suggests the concept but wording is ambiguous or uses generic terms |
| **Low** | Only vaguely related to the TOC heading; concept inferred from context rather than explicit heading text |

Confidence is determined by **TOC heading features** only — explicit name match, heading specificity, concept-to-heading distance. Do not use AI "feeling uncertain" as a criterion. When in doubt between two levels, choose the LOWER one.

---

## 5. Execution Procedure

### Step 1: Single-Book Extraction
1. Read the TOC from top to bottom
2. For each chapter/section, identify candidate knowledge points using §2 criteria
3. Assign confidence level per §4 criteria
4. Output per-book extraction table

### Step 2: Multi-Book Aggregation (per topic)
1. Collect all per-book extraction tables for the topic
2. Group by knowledge point name (normalize synonyms: "Context Switch" = "上下文切换" = "Process Switch")
3. Count appearances across books
4. Assign priority tier per §3 rule
5. Output per-topic aggregation table

### Step 3: Gap Analysis
1. Compare extracted knowledge points against expected topic scope (from topic name: "Process Management", "Memory Management", etc.)
2. Flag any obvious missing knowledge points: `[Gap: {concept} not covered by any book in this topic — needs external source or topic scope re-evaluation]`

---

## 6. Completion Checklist

After extracting knowledge points for all books in a topic:

- [ ] **All books extracted**: Per-book extraction table exists for every book in the topic
- [ ] **No fabrication**: Every extracted knowledge point traces back to a TOC heading
- [ ] **Uncertainty marked**: All Medium and Low confidence items tagged `[TODO: verify]`
- [ ] **Priority tiers assigned**: Cross-book aggregation complete with consensus-based priority
- [ ] **Gaps flagged**: Any missing expected knowledge points documented with `[Gap: ...]`
- [ ] **Single-book topics noted**: If N=1, "consensus signal unavailable" annotation present
- [ ] **Synonym normalization**: Same concept across books uses a SINGLE canonical name
- [ ] **Ready for user review**: All outputs formatted per §4 templates

---

## 7. Anti-Patterns

### 1. Copying TOC directly

```
WRONG: "Chapter 1 → Knowledge Point 1, Chapter 2 → Knowledge Point 2"
RIGHT: Chapter headings are INPUT, not output. Decompose each into sub-knowledge points targeting backend engineers.
```

### 2. Guessing from book reputation

```
WRONG: "This is 'Linux Kernel Deep Dive', so it MUST cover memory barriers in detail"
RIGHT: If the TOC doesn't show memory barriers, don't extract them. Mark the gap: "[Gap: memory barriers not covered by any book in this topic]"
```

### 3. Interview-question-first extraction

```
WRONG: "CFS → 'how is CFS different from O(1)?' → extract as Q&A pair"
RIGHT: "CFS scheduling strategy" — the knowledge point. Interview questions come AFTER knowledge is mastered.
```

### 4. Marking everything HIGH confidence

```
WRONG: All rows marked "High" because headings "sound right"
RIGHT: Only mark HIGH when the heading text leaves zero ambiguity. Default to the LOWER level when in doubt.
```

### 5. Synonym drift across books

```
WRONG: Book A extracted as "Context Switch", Book B as "上下文切换", Book C as "Process Context Switching"
RIGHT: Use a SINGLE canonical English name for the same concept across all books. Note the original heading phrasing in parentheses if useful.
```
