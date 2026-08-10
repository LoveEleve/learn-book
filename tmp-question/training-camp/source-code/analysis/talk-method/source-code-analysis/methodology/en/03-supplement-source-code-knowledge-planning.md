# Supplement: Knowledge Planning Flow for Source-Code Analysis Projects

> This document supplements 00-domain-discovery.md and 01-three-layer-loop.md — explaining where knowledge planning fits for pure source-code projects.

## Complete Flow

```
Domain Discovery (00)
  ↓
Knowledge Planning (knowledge-planning/05)
  Per-source extraction → Aggregation → Depth classification → Clustering
  ↓
Approach Selection (04) + Book-level Planning
  ↓
Per-Article Outline (01 §Pass 3)
```

## Why Knowledge Planning Must Be Inserted

**Consequence of skipping knowledge planning** (OpenJDK evidence):
- G1 GC: 45,692 lines source / 195 files → 1 article, 57-line outline
- Root cause: jumped straight to "write outline" without first extracting mechanisms from source → could only write an overview from memory

**Effect of doing knowledge planning** (内功修炼 evidence):
- OS Kernel: 6 books / 287 raw KPs → extraction → aggregation → classification → clustering → structured outlines

## Key Principles

1. **Why was this always skipped before?** The methodology's default flow is "domain discovery → Per-Article Outline" — designed for projects with book TOCs. Pure source-code projects lack the "book" as an intermediate layer, so knowledge planning must be inserted between domain discovery and Per-Article Outline.
2. **Knowledge planning doesn't replace Pass 0+1+2**: Knowledge planning is a step between domain discovery and Pass 0. Pass 0+1+2 must still be executed — knowledge planning determines "what to teach", Pass 0+1+2 handles "how to deeply verify".
3. **Mega-domains need per-subtopic knowledge planning**: For mega-domains like G1 GC, domain-level knowledge planning cannot substitute for detailed per-subtopic extraction.
