# Cross-Book Deduplication & Coverage Verification

> Role: AI self-constraint SOP. Must read after clustering and before writing content.
> Prerequisite: `01-toc-extraction.md` + `02-depth-standards.md` + `03-topic-clustering.md`
> Input: Per-topic aggregation (from 01) + depth classification (from 02) + teaching order (from 03)
> Reading order: 01 → 02 → 03 → 04 (this doc).

## Core Principle

**Multiple books covering the same knowledge point provide complementary PERSPECTIVES, not duplicate content.** Merging them is NOT about eliminating redundancy — it's about assembling the richest possible understanding from all available sources.

---

## 1. Coverage Mapping

For each knowledge point in a topic, map which books cover it and from what angle:

```
## Coverage Map: CFS Scheduler

| Book | Coverage | Angle | Depth |
|------|----------|-------|-------|
| Book A §3.1-3.3 | Sufficient | Algorithm principles | Working |
| Book B §4.2-4.5 | Sufficient | Kernel implementation | Deep |
| Book C §2.4 | Insufficient (1 page) | Performance tuning only | Surface |

Merged depth target: Deep (from 02 classification — this is a 🔴 knowledge point)
Primary source: Book B (most complete kernel-implementation angle)
Supplementary: Book A (algorithm clarity), Book C (tuning angle — marginal)
```

### Coverage Levels

| Level | Criteria | Merging Action |
|-------|----------|----------------|
| **Sufficient** | Book provides working-layer or better content for this knowledge point | Use as primary or supplementary source |
| **Insufficient** | Book mentions the topic but coverage is surface-level or tangential | Use only if it adds a unique angle not in primary sources |
| **Absent** | Book does NOT cover this knowledge point at all | Do NOT reference this book for this knowledge point |

### Single-Book Topics (N=1)

If a topic has only ONE book contributing:
- Primary source is that book by default
- No cross-book deduplication needed — no conflicts possible
- Coverage gaps are MORE likely — be extra vigilant with gap analysis (§4)
- Mark topic-level status: `[N=1 — consensus signal unavailable]`

---

## 2. Multi-Source Merging

### Primary Source Selection

For each knowledge point, select ONE primary source:

```
Primary source = the book that:
1. Covers the knowledge point at the required depth (per 02)
2. Has the most complete STRUCTURAL coverage (data structures, call flow, architecture)
3. If multiple books meet both → pick the one with BEST teaching narrative

Other books contributing to the same knowledge point are SUPPLEMENTARY.
```

### Primary Source Tiebreaker

When 2+ books have IDENTICAL coverage level and angle:

```
Use 01 §3 consensus signal: the book that appears in more chapters
across the SAME knowledge point family is the primary source.
If still tied → choose the one with better examples or clearer structure.
```

### Merging Rules

```
1. Base content from PRIMARY source — follow its structure and narrative flow
2. Supplement with UNIQUE angles from other books:
   - Book B adds implementation detail that Book A lacks → add it
   - Book C adds performance tuning perspective → add it as a sub-section
   - Do NOT repeat what the primary source already covers
3. Do NOT force-merge contradictory information — flag as conflict (§3)
4. Do NOT fabricate "bridging" content to connect different books' perspectives
   — if they truly disagree, present both views with source attribution
```

### Merging Output

```
## Content Plan: CFS Scheduler

Primary source: Book B §4.2-4.5 (kernel implementation angle)
Depth target: Deep (🔴)

Content outline:
1. CFS design principles (Book A §3.1 — algorithm clarity)
2. Kernel implementation (Book B §4.2-4.5 — primary)
   - sched_entity, cfs_rq structures
   - pick_next_task_fair() call path
   - vruntime tracking and update
3. Performance implications (Book C §2.4 — supplementary)
   - Scheduling latency and tuning
   - /proc/sched_debug interpretation

Cross-book supplements:
- Book A: vruntime fairness concept (better pedagogical explanation)
- Book C: tuning angle (only available here)
```

---

## 3. Inconsistency Handling

When two books provide CONTRADICTORY information about the same knowledge point:

### Detection

```
1. Compare structural claims (function names, struct field names, call chains)
2. Compare mechanism descriptions (how does X work?)
3. Compare numeric claims (default values, thresholds, timeouts)
```

### Resolution

| Scenario | Action |
|----------|--------|
| Obvious error in one book (outdated version, typo) | Use the **correct** book. Note the discrepancy: `[Note: Book A §3 says X, but Book B §4 says Y. Y is correct per kernel source verification.]` |
| Both could be right (different versions, different configs) | Present BOTH with version/config context. Mark: `[Version note: Book A (pre-5.0) describes X; Book B (5.0+) describes Y.]` |
| Cannot resolve (no external source to verify) | Present BOTH views. Mark: `[Unresolved: Book A §3 says X; Book B §4 says Y. Needs kernel source verification.]` |

### Conflict Recording

```
## Cross-Book Conflicts: {Knowledge Point}

| Claim | Book A (§3) | Book B (§4) | Resolution |
|-------|------------|------------|------------|
| default weight | 1024 | 1024 / 1.25 | Both correct (different contexts — explained) |
| nice-to-weight mapping | formula X | formula Y | [Unresolved: Book A §3 says X; Book B §4 says Y. Needs kernel source verification.] |
```

---

## 4. Gap Analysis & AI Supplement

**Input from 01/02**: Gap markers (`[Gap]`) recorded in 01 §5 and 02 must be merged into this gap analysis. New gaps detected during coverage mapping are appended.

### Gap Types

| Gap | Detection | Action |
|-----|-----------|--------|
| **Shallow coverage** | Book covers at surface level but needs deep (per 02) | AI supplements to target depth |
| **Angle gap** | Book covers mechanism but lacks structure/diagram | AI supplements structural component |
| **Complete gap** | No book covers this 🔴 knowledge point | AI writes from external knowledge — mark `[Gap: {concept} — AI-supplemented]` with rationale |

### AI Supplement Decision Flow

```
Knowledge point has coverage gap?
├── Is it 🔴 (interview/production essential)?
│   ├── Yes → AI MUST supplement to target depth (per 02)
│   └── No → Is it 🟡 (needed by ≥3 other knowledge points)?
│              ├── Yes → AI supplements to working level
│              └── No → 🟢: omit this knowledge point (do NOT fabricate)
```

### Supplement Quality Rules

1. **Source attribution**: Every supplemented claim must cite at least 2 independent references (kernel source, official docs, authoritative online references)
2. **Unverifiable claims**: Mark `[TODO: verify]` (aligned with 01 §Core Constraint #2)
3. **Do NOT supplement if the knowledge point is 🟢**: Surface-level knowledge points with zero book coverage are OMITTED — not supplemented
4. **Supplement scope**: Only supplement within the knowledge point's topic boundary — do not add unrelated material

---

## 5. Combined Conflict + Gap Scenario

When a knowledge point has BOTH cross-book conflicts AND coverage gaps:

```
Example: Book A says "default weight is 1024", Book B says "default weight is 1024 / 1.25".
Both books only provide surface coverage — 🔴 target needs deep-layer content.

Resolution:
1. First resolve conflict (per §3): determine which version is correct
2. Then supplement from correct version to deep level
3. If unresolvable → present both views AND mark as [Unresolved: {claim A} vs {claim B} — needs verification],
   supplement from the more likely correct version with caveat
```

---

## 6. Cross-Topic Dependency Check During Supplement

When AI supplements a knowledge point that depends on a knowledge point from another topic (per 03 §3):

```
IF the dependency is BLOCKED (prerequisite topic not yet complete):
  → DO NOT write content for this knowledge point
  → Mark as [BLOCKED — Topic X not yet complete]
  → Move to the next unblocked knowledge point in this topic
```

---

## 7. Source Attribution Format

Every knowledge point's final content must include a source block:

```
---
Sources:
  Primary: {Book} §{section} ({coverage: sufficient/insufficient})
  Supplementary: {Book} §{section} — {unique contribution}
  AI Supplement: [{conceptual / structural / full deep-layer (per 02)}] — supplemented from {reference 1}, {reference 2}
  Conflicts: [{description + resolution} — or "none"]
  Gap: [{Gap: {concept}} — AI-supplemented — or "none"]
  Cross-topic dependency: [{BLOCKED — Topic X not yet complete} — or "none"]
---
```

---

## 8. Final Content Structure Template

After merging is complete, the final knowledge point content follows this structure:

```
# {Knowledge Point Name}

{Depth-level content per 02 — see 02 §3 for layer requirements}

---
Sources: {as above}
---
```

---

## 9. Completion Checklist

After merging for all knowledge points in a topic:

- [ ] **Coverage mapped**: Every knowledge point has a coverage map with per-book coverage level and angle
- [ ] **Primary source selected**: Each knowledge point has ONE designated primary source with justification (tiebreaker applied if needed)
- [ ] **Single-book topics marked**: N=1 topics annotated `[N=1 — consensus signal unavailable]`
- [ ] **Supplements identified**: Cross-book supplements documented with unique contribution noted
- [ ] **Conflicts resolved or documented**: All cross-book discrepancies recorded with resolution status
- [ ] **Gaps addressed**: 🔴/🟡 gaps have AI supplement plans; supplements cite ≥2 references; 🟢 gaps omitted
- [ ] **Combined scenarios handled**: Conflict+gap knowledge points resolved per §5
- [ ] **No fabricated bridges**: No content invented solely to connect different books' perspectives
- [ ] **Source attribution**: Every knowledge point has a complete source block with all fields
- [ ] **Cross-topic dependencies checked**: BLOCKED dependencies marked and skipped
- [ ] **Ready for user review**: Merge plan confirmed by user before content writing begins
- [ ] **Ready for writing**: Content outline exists for all knowledge points

---

## 10. Anti-Patterns

### 1. Treating multi-book coverage as duplication

```
WRONG: "Books A, B, C all cover CFS → pick one, discard the rest"
RIGHT: Books A, B, C cover CFS from DIFFERENT angles → merge the unique angles.
       Algorithm (A) + implementation (B) + tuning (C) = complete picture.
```

### 2. Force-merging contradictions

```
WRONG: "Book A says X, Book B says Y → write a compromise that sounds right"
RIGHT: Present both views with attribution. If one is verifiably correct, note the discrepancy.
       If unresolvable, present both and flag for verification.
```

### 3. Supplementing without attribution

```
WRONG: Adding deep-level structural content without citing sources
RIGHT: Every supplemented claim must cite ≥2 references. Mark unverifiable claims clearly.
```

### 4. Filling 🟢 gaps

```
WRONG: "No book covers this 🟢 knowledge point → AI writes a short section anyway"
RIGHT: 🟢 knowledge points without ANY book coverage are OMITTED.
       Only supplement 🔴 and 🟡 gaps.
```
