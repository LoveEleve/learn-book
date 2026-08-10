# Quick Reference Card — Knowledge Planning

> Role: Handy extraction of commonly used commands and templates from methodology/01-04. Not a replacement for the full SOPs.
> Source: methodology/en/01-04

---

## TOC Extraction (01)

### Reading TOC Files
```bash
# List all book TOCs in a topic
find <topic-dir> -name "*.md" -type f

# Check TOC depth (count level-2/level-3 headings)
grep -c "^## " <toc-file>     # Level 2 (sections)
grep -c "^### " <toc-file>    # Level 3 (subsections)
```

### Per-Book Template
```
## Knowledge Point Extraction: {Book Title}

### Chapter Mapping
| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| {ch} {heading} | {concept} | {High/Medium/Low} |
```

### Per-Topic Template
```
## Topic: {Name} — Cross-Book Aggregation

N books: {count}

### Priority 1 — Consensus (majority of N, >N/2)
- {knowledge point} ({book §ref, book §ref})

### Priority 2 — Strong Signal (minority of N, >1)
- {knowledge point} ({book §ref})

### Priority 3 — Isolated (only 1 book)
- {knowledge point} [TODO: evaluate] ({book §ref})
```

---

## Depth Classification (02)

### Diagnostic Questions
```
1. Frequently asked in interviews OR commonly encountered in production?
2. Do ≥3 other knowledge points depend on it AND occasionally encountered?
3. Does the book mention it?
```

### Per-Point Template
```
## Depth Decision: {Knowledge Point}

01 Priority: P1/P2/P3
Importance: 🔴/🟡/🟢
Why: [1-2 sentence reasoning]
Target Depth: Surface/Working/Deep
Book coverage: [sufficient/insufficient/topic direction/TOC mention/not mentioned]
AI supplement: Yes/No
```

### Per-Topic Template
```
## Depth Summary: {Topic}

| Knowledge Point | 01 Pri | Importance | Depth | Supplement |
|----------------|--------|-----------|-------|-----------|
```

---

## Topic Clustering (03)

### Dependency Graph Format
```
KNOWLEDGE_POINT: {Name}
  depends on: {concept} — {reason}
              {concept} — {reason}
              [BLOCKED — Topic X not yet complete]
  does NOT depend on: {concept}
```

### Topological Sort Output
```
## Teaching Order: {Topic}

1. {leaf concept} (zero deps)
2. {next concept} (depends on 1)
3. ...
```

### Circular Dependency
```
[CIRCULAR with #{num} — three-round strategy]
```

---

## Cross-Book Merging (04)

### Coverage Map
```
| Book | Coverage | Angle | Depth |
|------|----------|-------|-------|
| {book} §X | Sufficient/Insufficient | {angle} | Deep/Working/Surface |
```

### Source Block
```
---
Sources:
  Primary: {Book} §{section} ({coverage})
  Supplementary: {Book} §{section} — {unique contribution}
  AI Supplement: [{scope}] — {ref1}, {ref2}
  Conflicts: [{description} — or "none"]
  Gap: [{Gap: {concept}} — or "none"]
---
```

---

## Canonical Markers

| Marker | en | zh |
|--------|-----|-----|
| Uncertainty | `[TODO: verify]` | `[待验证]` |
| Gap | `[Gap: {concept}]` | `[缺口：{概念}]` |
| Blocked | `[BLOCKED — Topic X not yet complete]` | `[BLOCKED — 主题 X 尚未完成]` |
| Unresolved | `[Unresolved: {claim A} vs {claim B}]` | `[未解决：{声明 A} vs {声明 B}]` |
| Circular | `[CIRCULAR with #{num}]` | `[与 #{num} 环形]` |
| Single-Book | `[N=1 — consensus signal unavailable]` | `[N=1 — 共识信号不可用]` |
