# AI Self-Constraint Prompt

> **READ BEFORE EVERY DOMAIN ANALYSIS.** This is a binding contract. Violating any rule in this document means the analysis is invalid.
> Authority: Core constraints (§1) and approach contracts (§3) are extracted from methodology/01-06. Cross-domain rules (§4) derive from 06. Pitfalls (§5) supplement with cross-session writing errors.
> Reading order: 01 → 02 → 03 → 04 → 05 → 06 → this prompt (last, as execution summary).

---

## 1. Core Constraints

Before starting ANY domain analysis, verify ALL of these. If any cannot be satisfied, do not proceed.

### Fabrication Prevention

1. **Every function name, class name, method signature must be grep-verified** before appearing in output. If unsure, write `[TODO: verify]` — never guess.
2. **Every numeric claim (line counts, byte counts, timeout values) must be `wc -l` or source-read verified.** No mental-model estimates.
3. **Every data structure claim (fields, types, relationships) must be read from actual header/source.** Do not infer from names.
4. **Never write a technology name without grep-verifying it exists in the codebase** (e.g., Caffeine, useGeneratedKeys, selectorExpression).
5. **Code blocks must be compilable or explicitly marked**: All code blocks are either verbatim from source (annotated with `file:line`) or marked `[pseudocode]`. Per 01 Core Constraint #5.

### Process Integrity

6. **Must complete ALL Pass 1 outputs before entering Pass 2.** At least 3 loop-closed questions required before Pass 3.
7. **No "covered later" — ALL dependencies must be analyzed first.** Refer to the dependency graph (02).
8. **No line count limits on any output.** Explain thoroughly — "讲透为止." Minimums exist (≥5 flagged questions), maximums do NOT.
9. **Every conclusion must cite source location:** `file:line`.
10. **Always prefer semantic tools (JavaLens/LSP/codegraph) over text-pattern matching (grep).** Fall back to grep only when all semantic/index tools are unavailable (05 fallback chain: Java: JavaLens→codegraph→grep, C: LSP+clangd→codegraph→grep).
11. **If Pass 2 discovers a new class/package not in Pass 1's inheritance tree → PAUSE, return to Pass 1** to scan that package/class, update the tree, and add flagged questions (01 §Discovered → Back to Pass 1).

### Depth Standards

12. **Ask "does this code carry a design decision?" — NOT "is this low-level optimization?"** (03). Deep-dive if yes. Design decision signals include: data structure choice, concurrency strategy, memory strategy, compression/encoding, exception handling strategy.
13. **Two consecutive loop notes concluding "getter/setter, no design decision" → re-select flagged questions AND do not continue deep-diving the same file** (03).

---

## 1.5 Pre-Domain Questions

**Ask these BEFORE any domain analysis.** AI proposes answers; user confirms or corrects. If AI can find the answer from source code itself (README, pom.xml, class names), do NOT ask the user — find it yourself.

### Questions

| # | Question | AI Can Self-Answer? | How |
|---|----------|---------------------|-----|
| Q1 | Which framework? Version? | **Yes** — from directory name, pom.xml, build.gradle | `cat pom.xml \| grep version`, `ls` root |
| Q2 | What is the entry point (class.method)? | **Yes** — from README, docs/, class names | Search for Bootstrap/Starter/Factory/Context/Engine |
| Q3 | Goal: interview prep, production understanding, or both? | **No** — user judgment required | AI proposes: "Both (interview → working layer, production → deep layer)" |
| Q4 | Depth expectation: full deep-dive or scan-first? | **No** — user judgment required | AI proposes: "🔴 full deep, 🟡 standard" |
| Q5 | Time constraint? | **No** — user judgment required | AI proposes: "None — analyze all domains" |

### Format

```
Q1: Which framework are we analyzing? Version?
➡️ I suggest: {framework} {version} (from {evidence})
   Confirmed?

Q2: What is the entry point?
➡️ I suggest: {Class}.{method}() (from {evidence})
   Confirmed?

Q3: Goal — interview prep, production, or both?
➡️ I suggest: Both — interview gets working layer, production gets deep layer.
   Confirmed?

Q4: Depth — full deep-dive or scan-first?
➡️ I suggest: 🔴 full deep-dive (A), 🟡 standard (B), 🟢 surface (C/D).
   Confirmed?

Q5: Time constraint?
➡️ I suggest: None — analyze all domains.
   Confirmed?
```

### Rule

- **Q1-Q2**: AI MUST self-answer from source. Only ask user if AI cannot find it.
- **Q3-Q5**: User MUST answer (or confirm AI's proposal). These determine depth and scope.
- **Never ask the user for something AI can look up.** Find facts from source, not from the user.

---

## 2. Pre-Domain Checklist

### Repo-Level (once per repo, before first domain)

- [ ] **Pre-analysis scanning**: Lightweight scan of ALL domains — `find src/main/java -type d` + `grep '^public (class|interface)'` + `grep -E '^import|^@'` for top-level deps. NOT full Pass 1 (02 §Pre-analysis scanning).
- [ ] **Dependency graph built**: ALL domains mapped with dependency lists. No [PENDING] (02 §1.2).
- [ ] **Circular deps identified**: Three-round strategy recorded for each circular pair (02 §1.4).
- [ ] **Hub candidates flagged**: Domains that are dependency root for ≥10 others → potential A upgrade (04 §Hub Upgrade).

### Domain-Level (per domain, before Pass 0)

- [ ] **Dependency graph**: All dependencies for this domain already analyzed (02 §1.2). Block if any are [PENDING].
- [ ] **Approach selected**: A/B/C/D determined per decision tree (04). Document with reasoning.
- [ ] **Approach Selection Output recorded** (04 format):
  ```
  ## Approach Selection: {domain name}
  Priority: {🔴/🟡} | Approach: {A/B/C/D} | Passes: {...}
  Temporal tracing: {yes/no} | Minimal reproduction: {yes/no}
  Dependencies to analyze first: {list} | Reason: [one sentence]
  ```
- [ ] **Leaf domain check**: If 🟡 and zero outgoing dependencies → B minimum, unless truly a thin wrapper (04 §Leaf Domains).
- [ ] **Tool availability**: Semantic tool (JavaLens/LSP+clangd), index tool (codegraph/codebase-memory-mcp), GitHub MCP, Git MCP all confirmed working. Fallback chain verified (05 §Pre-Domain Tool Check).
- [ ] **Index fresh**: Codegraph and codebase-memory-mcp indices up to date. Re-index if repo was updated since last index. First-time repos: create index now (05 §Index Refresh).
- [ ] **Temporal tracing ready**: For A approach 🔴 domains, identify earliest semantic tag.
- [ ] **Hub upgrade check**: If 🟡 domain is flagged in repo-level hub scan → upgrade to A per 04 §Hub Upgrade.
- [ ] **No compromised integrity**: This domain will NOT reference an unanalyzed domain.

---

## 3. Approach Behavior Contract

### A: Full Deep-Dive (🔴)

| MUST | MUST NOT |
|------|----------|
| Complete Pass 0→1→2→3 | Skip Pass 3 |
| If Pass 2 discovers new class/package → PAUSE, back to Pass 1 | Continue Pass 2 without updating inheritance tree |
| Temporal tracing (checkout early version) | Skip temporal tracing |
| Minimal reproduction in harness/ | Write article without harness verification |
| Interview questions (≥2 standard + ≥1 trap) | Omit design tradeoffs section |
| Read ≥2 test files in Pass 1 | Skip test files |

### B: Standard (🟡 with design decisions)

| MUST | MUST NOT |
|------|----------|
| Complete Pass 0→1→2 | Skip to Pass 3 without ≥5 flagged questions worth synthesizing |
| If Pass 2 discovers new class/package → PAUSE, back to Pass 1 | Continue Pass 2 without updating inheritance tree |
| Pass 2 loop notes with explicit cross-domain connections | Write article without any cross-domain references |
| Design tradeoffs in loop notes where applicable (per 03 depth criteria) | Treat all flagged questions as equal depth |
| Read ≥2 test files in Pass 1 | Skip test files |

### C: Light (🟡 simple mechanism)

| MUST | MUST NOT |
|------|----------|
| Complete Pass 0→1 | Skip Pass 0 (even simple domains need context) |
| Abbreviated Pass 2: answer 1-2 most critical questions | Write full narrative (loop notes are final output) |
| Mark remaining flagged questions as `[trivial — skipped]` (per 04 §C) | Silently ignore flagged questions |
| Read ≥2 test files in Pass 1 | Skip test files |

### D: Scan (glue/enumeration/wrapper)

| MUST | MUST NOT |
|------|----------|
| Modified Pass 1: inheritance tree + element decomposition | Generate flagged questions |
| Architecture summary paragraph ("Depends on / Used by") | Skip entirely (even glue needs documentation) |
| Cross-domain connections in summary | Write deep-dive loop notes |

### Depth Self-Diagnostic (after Pass 2, before Pass 3)

After Pass 2 completes, if ANY of these are true, depth is insufficient → **return to Pass 2 and deepen** (03):

- [ ] All descriptions are "what it does" with zero "why it does it this way"
- [ ] Zero cross-domain connections mentioned
- [ ] Cannot name the alternative design that was rejected
- [ ] Any loop note < 200 words
- [ ] Source references are only method signatures, no method bodies

---

## 4. Cross-Domain Rules

From 06. Every domain's output must satisfy:

1. **Cross-reference section**: ← (dependency) / → (depends on me) / Also see (related). Root domain: "Root domain — no upstream dependencies." Leaf domain: "Leaf domain — no downstream dependencies."
2. **Reference format**: `{domain} analysis §{section}`. Embed 1-2 sentence summary when essential to narrative.
3. **No unanalyzed references**: Never write "will be covered later" (01 Core Constraint #7).
4. **Look-back on discovery**: If Pass 2 uncovers issues in an already-analyzed domain:
   - Minor clarification → embed in current domain's article (Level 1)
   - Major correction → append correction section to affected domain (Level 2)
   - Complete invalidation → mark affected domain [RE-ANALYSIS REQUIRED], continue (Level 3)
   - Circular dep exception: During joint Pass 2, embed corrections directly — no marking needed.
5. **Multi-domain discovery**: If ≥3 domains affected, create `progress/cross-domain/{discovery-id}.md` — master note is single source of truth.

---

## 5. Pitfall Checklist

From actual writing errors across sessions. Check AFTER writing but BEFORE presenting.

- [ ] **No fabricated function names** — every function/class name grep-verified against source (01 Core Constraint #1)
- [ ] **No fake technology stack** — Caffeine, useGeneratedKeys, selectorExpression all grep-confirmed or absent (01 Core Constraint #4)
- [ ] **No wrong Redis key prefixes** — every key format read from `RedisKeyConstants` or equivalent
- [ ] **No "recorded for later" escape** — every finding is either FIXED or PROVEN not an issue
- [ ] **No "编译通过 = 已验证" shortcut** — after code changes, must restart service + curl functional test
- [ ] **No code blocks as pseudocode** — all code blocks are either verbatim from source (annotated) or explicitly marked `[pseudocode]` (01 Core Constraint #5)
- [ ] **No stale index** — verified index fresh before Pass 1 (05 §Index Refresh)
- [ ] **Exit check**: All checklist items in this document passed before presenting domain as "done"

---

## 6. Wait-What — User Stop Signal

**User can interrupt AI at ANY time** by saying:
- "wait" / "stop" / "不对" / "等等"
- "我听不懂" / "重新讲"

When user says any of these:

1. **STOP the current analysis immediately** — do not continue to the next pass
2. **Re-pitch the current state** — explain where you are, what you've done, what you're about to do
3. **Ask for clarification** — "Where did I lose you? What should I re-explain?"
4. **Do NOT proceed** until user confirms the corrected understanding

This is NOT a violation — it's a safety valve. The user has the right to interrupt ANY analysis at ANY time for ANY reason.

---

## Violation Protocol

If ANY rule in §1 or §3 is violated during analysis:

1. **Pause the current pass immediately.**
2. **Fix the violation** — re-read affected source, re-grep missing claims, backtrack to the violated pass.
3. **Do NOT proceed to the next pass until the violation is resolved.**
4. **If the violation is unrecoverable** (e.g., dependency not yet analyzed), mark domain as [BLOCKED] and move to the next analyzable domain.

This prompt was extracted from methodology/01-06 on 2026-08-05. Core rules and approach contracts derive from verified SOP documents. Pitfalls supplement with cross-session writing errors.
