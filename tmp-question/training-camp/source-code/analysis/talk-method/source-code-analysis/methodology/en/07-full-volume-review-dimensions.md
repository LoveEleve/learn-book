# Full-Volume Systematic Review Dimensions

> Role: AI self-constraint protocol. Must-read after all domain Per-Article Outlines are complete.
> Prerequisites: All methodology documents 01-06
> Input: All domain Per-Article Outline files (36-38 documents)
> Reading order: 01 → 02 → ... → 06 → 07 (this document). Final review before outline acceptance.

## Core Principle

**Per-domain deep review converges on accuracy. Full-volume systematic review converges on completeness.** Per-domain review verifies "is what's written correct?" within each individual domain. Full-volume review verifies "is the whole coherent?" across domains and volumes. Neither dimension substitutes for the other — an ensemble of individually-passing domains can still have broken bridges, incorrect dependencies, or coverage gaps.

Full-volume review MUST use **dimension rotation** — each round focuses on one irreducible inspection plane. Zero findings on the same dimension over consecutive rounds = convergence on that dimension.

---

## 1. Five-Dimension Checklist

When executing full-volume review, iterate through this checklist one dimension at a time. Each round reports: dimension name, findings count, fixes count, convergence status.

### Dimension 1: Narrative Bridge Consistency + Structural Completeness

**Check**:
- Each domain's IN bridge correctly picks up the previous domain's OUT bridge
- Each domain's OUT bridge points to the correct next domain (no skipping, no circular)
- Volume boundary bridges (last domain of previous volume → first domain of next volume) explicitly exist
- Each domain contains five required structural elements: concept dependencies/prerequisites, design tradeoffs, core suspense, estimated scope, narrative plan

**Detection**:
```bash
# Extract all bridges
grep "→ 从\|→ 下一\|→ 卷" outlines/vol-0*/*.md

# Check structural elements
grep -c "核心悬念\|设计权衡\|概念依赖\|预估\|叙事计划" <file>
```

### Dimension 2: Source Anchor Density

**Check**: Number of precise source anchors (`file:line` format) per outline. Anchor density directly measures "was this outline written from source code or from memory?"

- 🔴A domains: ≥8 anchors
- 🟡B domains: ≥4 anchors
- 🟡C domains: ≥2 anchors

**Note**: Bare line numbers (e.g. `line 630` without filename) do NOT count — must include filename.

**Detection**:
```bash
grep -c '\.cpp:\|\.hpp:\|\.java:' <file>
```

### Dimension 3: Forward Reference + Dependency Correctness

**Check**: Does each domain's declared dependencies (e.g. `Depends on: X + Y`) point to domains already taught in the topological order?

- Depended-upon domain must have sequence number < current domain
- "Mentioning" future domains in body text is acceptable; "declaring as dependency" is not
- Extra check across volume boundaries: domain A in vol-03 cannot declare dependency on a vol-04 domain

**Detection**:
```bash
# Extract all dependency declarations
grep "依赖：\|先修：" outlines/vol-0*/*.md

# Cross-check: is each dependency's topological # < current domain's #?
```

### Dimension 4: Thread / Cross-Cutting Concern Coverage Matrix

**Check**: Select a cross-cutting concern (JVM internal threads / GC barrier propagation / safepoint check points) and trace its appearance across all domains. Build a coverage matrix — which domains mention it, which should but don't.

- Thread coverage example: Signal Dispatcher Thread appears 0 times → gap
- Each cross-cutting concern should be covered in ≥1 domain

**Process**:
1. Select cross-cutting concern (thread types / GC barrier chain / signal handling path)
2. grep all outlines for occurrence count
3. Cross-reference source header files to confirm "what threads/barriers/signal handlers exist"
4. Build "should-have vs actually-has" gap table

### Dimension 5: Negative Space + Opening Scene Quality

**Check**:

**Negative space**: For contrast-type domains (domains with explicit "we deliberately don't do X" design boundaries, e.g. C1 vs C2, G1 vs CMS), check whether there's an explicit declaration of "what we don't do". Tabular format preferred — optimization name / why not done / which domain does do it.

**Opening scene quality**: Does each domain's opening scene start from a specific, reader-relatable situation — not an abstract concept summary. Good: "`System.gc()` does not directly call G1's collection code — it first passes through `CollectedHeap`." Bad: "JVM's GC framework is important."

**Detection**:
```bash
# Negative space detection
grep -c "不做\|故意不\|选择不做\|跳过" <file>

# Opening scene quality (at least 3 concrete situational nouns/verbs per domain)
head -20 <file> | grep -c "pthread\|jcmd\|jconsole\|OOM\|Spring\|Lambda"
```

---

## 2. Execution Order and Convergence Detection

```
R1: Dimension 1 (bridges+structure)      → report findings+fix
R2: Dimension 2 (anchor density)          → report findings+fix  ← NEW
R3: Dimension 3 (forward references)      → report findings+fix
R4: Dimension 4 (cross-cutting concerns)  → report findings+fix  ← NEW
R5: Dimension 5 (negative+openings)       → report findings+fix  ← NEW

Convergence signal: two consecutive rounds with zero findings, using different dimensions = review exhausted.
```

**Prohibited**: Running R6 (same dimension as any prior round) after R5 zero findings. When dimensions are exhausted, proceed to writing phase — Pass 2/3.

---

## 3. Anti-Patterns

### 1. Reviewing the same dimension repeatedly

```
WRONG: R1 bridges → fix → R2 bridges again → zero findings
RIGHT: R1 bridges → R2 anchor density → R3 forward refs → each round, new dimension
```

### 2. Zero findings = "didn't look closely enough"

```
WRONG: After zero findings, reflexively thinking "there must be more, review again"
RIGHT: Zero findings + new dimension = that dimension is genuinely clean. Not laziness.
```

### 3. Marking gaps as "acceptable" instead of fixing

```
WRONG: vol-03 missing 6 OUT bridges → tag "pre-stabilization, acceptable"
RIGHT: Fix directly. One-line bridge fix cost is negligible, completeness gain is high.
```

### 4. Cross-cutting concern too narrow

```
WRONG: Only checking JVM internal threads → 10 threads × 36 domains = 360 cells too granular → missing 1 cell proves nothing
RIGHT: Thread coverage uses "thread → covered domains" sparse matrix to see overall pattern (not per-cell counting)
```

### 5. Format compliance ≠ content quality

```
WRONG: R1-R9 all format dimensions (bridges/structure/anchors/forward refs) → all pass → claim "done"
RIGHT: At least one round switches to content depth — use "reverse writing test" or "multi-perspective questioning"
```

### 6. Mega-domain treated with regular split rules

```
WRONG: G1 GC 45,692 lines → Pre-Pass 3 says "force split 3-4 articles" → still superficial
RIGHT: Source >30,000 lines → mega-domain → one volume → 8-10 independent article outlines
```

### 7. Outline complete without multi-perspective questioning

```
WRONG: Outline deep review passes → claim "done" → user says "where's the question doc?" → scramble to make one
RIGHT: After every domain outline, automatically execute 01-three-layer-loop §Outline Completeness Verification → produce question document
```

---

*After full-volume review is complete, there should be no more review rounds — proceed directly to outline acceptance → Pass 2/3 writing.*
