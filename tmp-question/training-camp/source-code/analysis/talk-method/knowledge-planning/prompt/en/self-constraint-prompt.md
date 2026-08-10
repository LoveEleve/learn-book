# AI Self-Constraint Prompt — Knowledge Planning

> **READ BEFORE EVERY KNOWLEDGE PLANNING SESSION.** This is a binding contract. Violating any rule in this document means the planning is invalid.
> Authority: Core constraints (§1), execution procedure (§2), and cross-topic rules (§3) are extracted from methodology/01-04. Pitfalls (§4) supplement with anti-patterns from all four docs.
> Reading order: 01 → 02 → 03 → 04 → this prompt (last, as execution summary).

---

## 1. Core Constraints

Before starting ANY knowledge point extraction, verify ALL of these. If any cannot be satisfied, do not proceed.

### Fabrication Prevention

1. **Every knowledge point must trace back to a TOC heading.** If the TOC does not show the concept, do NOT extract it. If uncertain, write `[TODO: verify]` — never guess.
2. **Every structural claim must be independently verifiable.** Kernel structs, function names, call chains — grep-verify against source. If source access unavailable, mark `[TODO: verify]`.
3. **No invented algorithms, formulas, or concepts.** If exact formula unknown, describe conceptually. Do not fabricate details to fill gaps.
4. **Confidence levels are determined by TOC heading features only** — explicit name match, heading specificity, concept-to-heading distance. Do not use AI "feeling uncertain" as a criterion. When in doubt, choose the LOWER confidence level.
5. **Extract only to the depth the TOC provides.** Do not guess deeper content from book title or reputation. Level 4+ TOCs are rare but extractable; they provide fine-grained decomposition but still no implementation details.

### Process Integrity

6. **Knowledge-point-first** — the organizing principle is "what must a backend engineer understand," NOT "what does the book contain" and NOT "what does an interviewer ask."
7. **Complete 01 extraction BEFORE 02 depth classification.** Complete 02 BEFORE 03 clustering. Complete 03 BEFORE 04 merging. Do NOT skip steps.
8. **Dependency ordering ALWAYS overrides importance ordering.** A depends on B → teach B first regardless of whether A is 🔴 and B is 🟡.
9. **No "will be covered later" (forward references to unlearned concepts).** If concept A depends on unlearned concept B, either teach B first or mark A as BLOCKED. Backward references to already-explained concepts are not only allowed but expected — that is the purpose of topological order.
10. **🟢 knowledge points without ANY book coverage are OMITTED.** Do NOT fabricate content for 🟢 gaps. Only 🔴 and 🟡 gaps get AI supplement.

### Depth Standards

11. **Depth is determined by interview + production frequency (🔴/🟡/🟢), NOT by book page count or book coverage.** Books provide topic boundaries only; AI generates ALL content.
12. **Deep layer (🔴) includes structural components** (struct names + field roles, function signatures + call flow, architecture diagram in text) + optional source snippets as structural evidence. Do NOT trace into implementation bodies line-by-line.
13. **Two independent axes**: 01 Priority (multi-book consensus signal) and 02 Importance (real-world relevance) are independent. A P1 knowledge point can be 🟢. A P3 knowledge point can be 🔴. Judge each separately.

---

## 2. Pre-Planning Checklist

### Per-Topic (before extraction, once per topic)

- [ ] **Topic assignment confirmed**: User has specified which topic to plan (e.g., "OS 内核", "内存管理")
- [ ] **Book list confirmed**: All books contributing to this topic identified
- [ ] **TOC files readable**: All TOC files exist and are accessible
- [ ] **Topic scope clear**: One-paragraph scope statement drafted (what mechanism boundary defines this topic)

### Per-Topic (after extraction, before 02 depth classification)

- [ ] **All books extracted**: Per-book extraction table exists for every book in the topic
- [ ] **No fabrication**: Every extracted knowledge point traces back to a TOC heading
- [ ] **Uncertainty marked**: All Medium and Low confidence items tagged `[TODO: verify]`
- [ ] **Priority tiers assigned**: Cross-book aggregation complete with P1/P2/P3 consensus-based priority
- [ ] **Single-book topics noted**: If N=1, `[N=1 — consensus signal unavailable]` annotation present
- [ ] **Synonym normalization**: Same concept across books uses a SINGLE canonical name
- [ ] **Gaps flagged**: Any missing expected knowledge points documented with `[Gap: ...]`

### Per-Topic (after depth classification, before 03 clustering)

- [ ] **Every knowledge point classified**: 🔴/🟡/🟢 assigned with diagnostic question reasoning
- [ ] **01 Priority recorded**: P1/P2/P3 from cross-book aggregation carried over
- [ ] **Lower bound met**: All interview + production essentials reach deep layer
- [ ] **Upper bound respected**: 🟢 items limited to surface layer (1-2 sentences)
- [ ] **AI supplement scope defined**: Every AI-supplemented knowledge point has supplement scope listed (conceptual / structural / full deep-layer)
- [ ] **Gap items marked**: 🔴 items with zero book coverage marked `[Gap]`, not fabricated
- [ ] **User confirmed importance classification**: User has reviewed and approved the depth decision table

### Per-Topic (after clustering, before 04 merging)

- [ ] **Topic boundary defined**: Mechanism boundaries clear, no knowledge point forced into wrong topic
- [ ] **Dependency graph built**: Every knowledge point has a complete dependency list with [BLOCKED] markers for incomplete deps
- [ ] **Topological sort verified**: No dependency appears AFTER its dependent in the teaching order
- [ ] **Circular deps identified**: Three-round strategy recorded for each circular pair
- [ ] **Cross-topic deps marked**: Any knowledge point depending on another topic's content flagged with prerequisite topic name
- [ ] **User confirmed topic structure and teaching order**: User has approved before merging begins

---

## 3. Execution Procedure Contract

### Step 1: 01 Extraction

| MUST | MUST NOT |
|------|----------|
| Extract only from TOC headings | Copy TOC directly as knowledge points |
| Mark Medium/Low confidence as `[TODO: verify]` | Mark everything HIGH confidence |
| Use canonical English names for same concept across books | Use different names for same concept in different books |
| Flag `[Gap: {concept}]` for missing expected knowledge points | Fabricate concepts not in any TOC |
| Note `[N=1 — consensus signal unavailable]` for single-book topics | Treat single-book topics as having consensus signal |

### Step 2: 02 Depth Classification

| MUST | MUST NOT |
|------|----------|
| Use diagnostic questions (interview/production frequency → dependency count → book mention) | Use book page count to determine depth |
| Mark 🔴 items with zero book coverage as `[Gap]` | Fabricate 🔴 content without marking `[Gap]` |
| Respect upper bound: 🟢 items get 1-2 sentences only | Write deep-layer content for 🟢 items |
| Record both 01 Priority and 02 Importance for every knowledge point | Confuse P1 (consensus) with 🔴 (importance) |
| Define supplement scope for every AI-supplemented knowledge point | Supplement without specifying scope |

### Step 3: 03 Clustering & Ordering

| MUST | MUST NOT |
|------|----------|
| Cluster by mechanism boundary, not book title | Cluster by book title |
| Build dependency graph BEFORE writing any content | Start writing before dependency graph is complete |
| Topological sort so dependencies appear before dependents | Teach by importance order instead of dependency order |
| Mark cross-topic dependencies with prerequisite topic name | Allow "will be covered later" forward references |
| Apply three-round strategy for circular dependencies | Ignore circular dependencies |

### Step 4: 04 Merging

| MUST | MUST NOT |
|------|----------|
| Select ONE primary source per knowledge point | Treat multi-book coverage as duplication to eliminate |
| Supplement with UNIQUE angles from other books | Force-merge contradictory information |
| Resolve or document all cross-book conflicts | Write a "compromise" that sounds right but is fabricated |
| AI supplement 🔴 gaps from external knowledge with ≥2 references | Supplement 🟢 gaps (🟢 gaps are omitted) |
| Include complete source attribution block for every knowledge point | Supplement without citing sources |
| Mark `[BLOCKED — Topic X not yet complete]` for cross-topic dependencies | Write content that depends on an incomplete topic |

### Depth Self-Diagnostic (after writing content, before presenting)

If ANY of these are true, depth is insufficient → **return and deepen**:

- [ ] All descriptions are "what it does" with zero "why it does it this way"
- [ ] Zero structural components listed for a 🔴 knowledge point
- [ ] Cannot name the design tradeoff or alternative approach
- [ ] A 🔴 knowledge point has no mechanism explanation, only a definition
- [ ] AI supplement lacks ≥2 source references

---

## 4. Cross-Topic Rules

From 03 §3. Every topic's output must satisfy:

1. **Prerequisite marking**: Every topic lists prerequisite topics and specific dependent concepts with one-sentence dependency chain explanation
2. **BLOCKED handling**: If a knowledge point depends on a knowledge point from an incomplete topic → mark `[BLOCKED — Topic X not yet complete]`, do NOT write content
3. **Topic dependency graph**: Every topic has prerequisite topics listed. Foundation topics (zero intra-curriculum dependencies) are taught first.
4. **No circular topic deps**: If Topic A depends on B AND B depends on A, attempt re-clustering first. If mechanism-level circularity is genuine, apply three-round strategy at topic level.
5. **Gap propagation**: Gap markers (`[Gap]`) from 01 §5 and 02 must be merged into 04 §4 gap analysis. New gaps detected during coverage mapping are appended.

---

## 5. Pitfall Checklist

From methodology anti-patterns (01 §7, 02 §7, 03 §5, 04 §10). Check AFTER planning but BEFORE presenting.

- [ ] **No TOC copying** — chapter headings are INPUT, not output. Each decomposed into sub-knowledge points targeting backend engineers (01 §7 Anti-Pattern 1)
- [ ] **No book-reputation guessing** — "This is a famous book so it MUST cover X" is fabrication. If TOC doesn't show it, don't extract it (01 §7 Anti-Pattern 2)
- [ ] **No interview-question-first extraction** — knowledge points are concepts/mechanisms, not Q&A pairs (01 §7 Anti-Pattern 3)
- [ ] **No synonym drift** — same concept across books uses a SINGLE canonical name (01 §7 Anti-Pattern 5)
- [ ] **No book-page-count depth** — a 50-page 🟢 topic gets 1-2 sentences; a 2-page 🔴 topic gets AI expansion (02 §7 Anti-Pattern 1)
- [ ] **No equal treatment** — 🔴 deep, 🟡 working, 🟢 surface. Depth matches importance (02 §7 Anti-Pattern 2)
- [ ] **No supplement-as-source-analysis** — deep = structural components + call flow + optional source snippets. NOT line-by-line source walkthrough (02 §7 Anti-Pattern 3)
- [ ] **No skipped essentials** — 🔴 items MUST be fully covered regardless of book depth (02 §7 Anti-Pattern 4)
- [ ] **No P1=🔴 confusion** — Priority (consensus) and Importance (relevance) are independent axes (02 §7 Anti-Pattern 5)
- [ ] **No forward references** — "will be covered later" is forbidden. Backward references to taught concepts are correct (02 §7 Anti-Pattern 6)
- [ ] **No book-title clustering** — topics are mechanism boundaries, not book titles (03 §5 Anti-Pattern 1)
- [ ] **No importance-ordered teaching** — dependency order ALWAYS overrides importance order (03 §5 Anti-Pattern 2)
- [ ] **No forced low-count topics** — if <5 knowledge points AND no clear mechanism boundary → merge with adjacent topic (03 §5 Anti-Pattern 4)
- [ ] **No duplication treatment** — multi-book coverage is complementary perspectives, not redundancy to eliminate (04 §10 Anti-Pattern 1)
- [ ] **No force-merged contradictions** — present both views with attribution, or resolve with verification (04 §10 Anti-Pattern 2)
- [ ] **No unattributed supplements** — every AI supplement cites ≥2 references (04 §10 Anti-Pattern 3)
- [ ] **No 🟢 gap filling** — 🟢 knowledge points without book coverage are OMITTED (04 §10 Anti-Pattern 4)

---

## Violation Protocol

If ANY rule in §1 or §3 is violated during planning:

1. **Pause the current step immediately.**
2. **Fix the violation** — re-read affected TOC, re-verify claims, backtrack to the violated step.
3. **Do NOT proceed to the next step until the violation is resolved.**
4. **If the violation is unrecoverable** (e.g., prerequisite topic not yet complete), mark the knowledge point or topic as [BLOCKED] and move to the next unblocked item.

This prompt was extracted from methodology/01-04 on 2026-08-05. Core rules and execution contracts derive from verified SOP documents. Pitfalls supplement with anti-patterns from all four docs.
