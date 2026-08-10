# Cross-Domain Reference Strategy

> Role: AI self-constraint SOP. Read before Pass 3 synthesis; consult during Pass 2 when cross-domain connections emerge.
> Executed? Output: `[06] Cross-references: N ← incoming, N → outgoing, N discovery feedback loops closed`
> Prerequisite: `01-three-layer-loop.md` + `02-dependency-order-decomposition.md` + `04-approach-decision-tree.md`
> This document assumes familiarity with 01 (Pass 0-3 framework), 02 (dependency graph), 03 (depth standards), 04 (approach decisions), and 05 (tool usage). Cross-domain references integrate knowledge from all five.
> Input: Current domain + all previously analyzed domains in the same repo
> Reading order: 01 → 02 → 03 → 04 → 05 → 06 (this doc). See 01 for full execution sequence.

## Core Principle

**Domains are not islands.** Every domain exists in a dependency graph. Analyzing domain B reveals insights about domain A — and that's a feature, not a bug. The question is: what do you do when it happens?

---

## 1. When to Look Back

During Pass 2 of domain B, you may realize domain A (already analyzed and written) was misunderstood or incomplete. Three response levels:

### Level 1: Minor Clarification (leave it)

Trigger: A single sentence in domain A's article is slightly imprecise. The insight from domain B adds nuance but doesn't change the core understanding.

Action: **Do nothing to domain A.** Embed the clarification in domain B's article:
- "Note: in domain A, we described X as Y. Domain B reveals that X is actually best understood as Z — this doesn't invalidate A's analysis but adds precision."

Reason: Domain A's article is already "final." Chasing every nuance creates infinite rewrite cycles.

### Level 2: Major Correction (append, don't rewrite)

Trigger: Domain B reveals that the core mechanism of domain A was wrong. Not a nuance — the fundamental understanding must change.

Action: **Append a correction section to domain A's article.** Do NOT rewrite the whole article:
```
### [Correction from {domain B} analysis]
Original understanding: {what domain A said}
New understanding: {what domain B revealed}
Source evidence: {file:line from B's analysis that triggered this}
```

Reason: Appending preserves the analysis history while fixing the error. Rewriting would hide the learning process and risk introducing new errors.

### Level 3: Re-analysis Required (mark for revisit)

Trigger: Domain B reveals that domain A was so fundamentally misunderstood that the entire article is invalid.

Action: **Mark domain A for re-analysis.** Add a header to the top of domain A's article:
```
> ⚠️ [RE-ANALYSIS REQUIRED — flagged from {domain B} analysis]
> Reason: [one sentence]
```

Then continue with domain B. Re-analysis happens AFTER all remaining domains in the current repo's topological order are completed. The re-analysis marker persists until then.

**Decision flow**:
```
New insight about domain A from domain B's Pass 2?
├── Is the core mechanism wrong?
│   ├── No → Level 1: minor clarification in B's article
│   └── Yes → Is the entire analysis invalid?
│              ├── No → Level 2: append correction to A's article
│              └── Yes → Level 3: mark A for re-analysis, continue B
```

---

### Circular Dependency Exception

For domains marked as circular dependencies (02 §1.4), the three-level look-back does NOT apply during the joint Round 2 analysis. Since both domains are being co-analyzed, neither is "already analyzed and written." Corrections should be embedded directly in the joint Pass 2 loop notes without marking either domain for later correction. The three-level response only applies when domain A was completed in a PRIOR analysis cycle (different repo, or same repo but domain A belongs to a different dependency sub-graph that was completed in a prior analysis session).

---

## 2. Cross-Domain Reference Format

When domain B references domain A's analysis, use this format:

### Standard Reference

```
{domain A} analysis §{section}
```

Example: `Netty EventLoop analysis §3.2`

### With Summary Embedding

When the referenced concept is central to understanding domain B, embed a 1-2 sentence summary:

```
As established in {domain A} analysis (§3.2), ByteBuf uses reference counting to manage memory — every `retain()` must be paired with a `release()`. Domain B (ChannelHandler) extends this pattern by...
```

### Dos and Don'ts

```
DO:   Embed a 1-2 sentence summary when the concept is essential to B's narrative
DO:   Use §{section} references for supporting context
DON'T: Re-explain domain A's full mechanism — reference it, don't duplicate it
DON'T: Reference domains not yet analyzed (violates 01's "no 'covered later'" rule)
```

---

## 3. New Discovery Feedback

> This is distinct from 01's "Discovered → Back to Pass 1" (which is about missing classes in the SAME domain during Pass 2). This section handles cross-domain discoveries only — new information about a DIFFERENT domain uncovered during current domain's analysis.

### Discovery Recording

When domain B's analysis uncovers new information about domain A that doesn't fit Levels 1-3 (e.g., an implementation detail that wasn't noticed before but doesn't change the core understanding):

Create a **Discovery Note** at the end of domain B's Pass 2 loop notes:

```
## Cross-Domain Discovery

Source: domain B Pass 2, loop note {#}
Discovered: {what was found about domain A}
Verified against: domain A source at {file:line}
```

When the discovery is a structural element (class/interface/pattern), use 01's Basic Element Decomposition format: "Element: [one sentence] — source {file:line}" (see 01 §Pass 1 Output Format)

### Discovery Propagation

After domain B is complete, check if the discovery note warrants action. Before verifying against other domain sources, check index freshness per 05 §Index Refresh.

| Discovery Type | Propagation Action |
|----------------|-------------------|
| Domain A missed an edge case | Append to domain A's article (Level 2) |
| Domain A has an undocumented internal interface used by B | Add §cross-reference section to A |
| Domain A's understanding was correct, just needs more detail | Level 1 — embed in B's article |

### Multi-Domain Impact

If domain B's discovery affects ≥3 already-analyzed domains:

1. Create a master note: `progress/cross-domain/{discovery-id}.md`
2. For each affected domain, add a brief reference to the master note
3. Do NOT individually edit 3+ articles — the master note is the single source of truth

---

## 4. Pass 3 Cross-Reference Section

Every domain that produces a Pass 3 article (approaches A and B-with-Pass3) MUST include a cross-reference section. For approaches C and D, see §Non-Pass-3 Domains below.

```
## 5. Cross-References

← {previous domain in topological order}: {one sentence why it's a dependency}
→ {next domain}: {one sentence what depends on this}
Also see: {related domains} — {brief description of relationship}
```

> This format refines 01's single-line condensed version — the multi-line format with explanations is authoritative. Use this format, not 01's condensed version, for all Pass 3 articles.

### Rules

- **← (previous)**: Must be the domain that this one directly depends on, per the dependency graph. If this is the first domain in topological order (zero dependencies), write "Root domain — no upstream dependencies."
- **→ (next)**: What downstream domains depend on this? If none, write "Leaf domain — no downstream dependencies"
- **Also see**: Any domain that has a thematic connection (not a direct dependency) — e.g., two different scheduling strategies in the same framework

### Non-Pass-3 Domains

Domains analyzed with approaches C or D don't produce Pass 3 articles. Handle cross-domain connections as follows:

- **C (Light)**: Record cross-domain connections as separate loop notes in the abbreviated Pass 2 output.
- **D (Scan)**: Include a "Depends on / Used by" sentence in the architecture summary paragraph.

---

## 5. Quality Gate

Before presenting a domain's article as "done," verify:

- [ ] Cross-reference section is complete (← / → / Also see)
- [ ] No references to unanalyzed domains ("will be covered later")
- [ ] All references use the standard format ({domain} analysis §{section})
- [ ] Any Level 2/3 corrections from this domain's analysis have been applied to affected domains
- [ ] Discovery notes (if any) have been propagated per §3

If any of these fail, Pass 3 is NOT complete.
