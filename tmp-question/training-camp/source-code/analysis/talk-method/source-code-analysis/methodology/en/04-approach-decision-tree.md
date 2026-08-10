# Approach Decision Tree

> Role: AI self-constraint SOP. Must read before starting each domain analysis.
> Prerequisite: `02-dependency-order-decomposition.md` + `03-analysis-depth-standards.md`
> Input: Domain priority (🔴/🟡) from `progress/源码分析执行计划.md`
> Reading order: 01 → 02 → 03 → 04 (this doc) → 05 → 06. See 01 for full execution sequence.

## Core Question

Given a domain, which analysis approach should be applied?

## Four Approaches

### A: Full Deep-Dive

**Passes**: 0 → 1 → 2 → 3

**Additional**:
- Temporal tracing (checkout early version, read git diff to see evolution)
- Minimal reproduction (build micro-engine in `harness/{framework}/`)

**Trigger**: Domain is 🔴

### B: Standard

**Passes**: 0 → 1 → 2 → Pass 3 (optional)

Pass 3 is done only if the domain has ≥5 flagged questions worth synthesizing into a complete narrative. If not, stop at Pass 2 with loop notes. Note: 01's ≥3 closed questions is the Pass 2 quality gate. This ≥5 is the Pass 3 eligibility check — they serve different purposes.

**Additional**: None (no temporal tracing, no minimal reproduction)

**Trigger**: Domain is 🟡 AND carries design decisions (03 criteria)

### C: Light

**Passes**: 0 → 1 → abbreviated Pass 2

Abbreviated Pass 2: answer 1-2 most critical flagged questions only. Output as short loop notes, no narrative synthesis. Remaining flagged questions: mark as `[trivial — skipped]` with brief one-line reason.

**Additional**: None

**Trigger**: Domain is 🟡 AND mechanism is simple (03 skip signals: getter/delegation/enumeration/logging)

### D: Scan

**Passes**: Modified Pass 1 (skips flagged questions and test file reading)

Output: inheritance tree + basic element decomposition. No flagged questions, no deep-dive.

**Additional**: Add a one paragraph summary describing the domain's role in the larger architecture — enough for cross-domain references.

**Trigger**: Pure glue code, thin wrappers, configuration classes, pure enumerations

---

## Decision Tree

```
Domain is 🔴?
├── Yes → A: Full Deep-Dive (mandatory: Pass 0-3, temporal tracing, minimal reproduction)
└── No  → Pure glue/thin wrapper/config/enumeration?
           ├── Yes → D: Scan (Pass 1 only, plus architecture summary)
           └── No  → Carries design decisions? (03 criterion)
                      ├── Yes → B: Standard (Pass 0-2, optional Pass 3)
                      └── No  → C: Light (Pass 0-1, abbreviated Pass 2)
```

### Quick Lookup Table

| Priority | Has Design Decisions? | Approach |
|----------|-----------------------|----------|
| 🔴 | Any | A |
| 🟡 | Yes | B |
| 🟡 | No (simple) | C |
| 🟡 | Glue/enum/wrapper | D |

---

## Special Cases

### Hub Upgrade

If a 🟡 domain is the dependency root for ≥10 other domains, upgrade from B to A. The cost of insufficient understanding here cascades across many downstream domains.

Exception: if the Hub itself is a thin wrapper (pure delegation, no design decisions), output the upgrade flag but explicitly note the exception reasoning.

Check: `grep` the domain name in `progress/源码分析执行计划.md` — if it appears in ≥10 "前置依赖" lines, flag for Hub upgrade.

### Circular Dependencies

Apply the three-round strategy from `02-dependency-order-decomposition.md`, with these depth rules:

- **Round 1 (shallow)**: Both domains → Pass 0 + Modified Pass 1 (02 §1.4: inheritance tree + element decomposition only, skip flagged questions and test file reading). No Pass 2. Establish basic concepts on both sides — just enough to name the interface boundary between them.
- **Round 2 (cross-verify)**: Run Pass 2 as a single joint session across both domains. For each flagged question from Round 1, grep and read source in BOTH domains together. Cross-verify interfaces, shared assumptions, interaction points.

Joint Pass 2 output: separate loop notes per domain, but each note references the other domain's source locations when relevant.

- **Round 3 (independently)**: Each domain returns to its normal approach (A/B/C/D) and completes Pass 3 if applicable.

### Leaf Domains

Even if 🟡, leaf domains (zero outgoing dependencies) are foundations — B is the minimum, never skip to C/D unless it's truly a thin wrapper.

---

## Approach Selection Output

Before starting a domain, output this block:

```
## Approach Selection: {domain name}

Priority: {🔴/🟡}
Approach: {A/B/C/D}
Passes: {Pass 0→1→2→3 / Pass 0→1→2 / Pass 0→1 / Pass 1}
Temporal tracing: {yes/no}
Minimal reproduction: {yes/no}
Dependencies to analyze first: {list from execution plan}

Reason: [one sentence — why this approach was chosen]
```
