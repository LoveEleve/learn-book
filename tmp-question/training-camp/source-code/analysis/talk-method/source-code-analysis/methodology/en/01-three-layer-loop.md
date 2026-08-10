# Three-Layer Loop Framework

> Role: AI self-constraint SOP. Must read before execution.
> Executed? Output: `[01] Pass{0/1/2/3}: N flagged questions → N loop-closed → N design tradeoffs`
> Prerequisite: `progress/源码分析执行计划.md`
> Reading order: 01 (this doc) → 02 → 03 → 04 → 05 → 06. Execution: 02's pre-analysis checklist first → 04's approach selection per domain → 01's Pass 0-3 execution → guided by 03 depth standards → using 05 tool allocation → 06 cross-domain references.

## Operation Modes: Review vs Write

This methodology supports TWO distinct operation modes. Choosing the wrong mode for the task is a fundamental error.

### Mode A: Review Existing Chapters (审查存量文章)

**Input**: Existing chapter text. **Goal**: Find and fix real content issues — inaccurate claims, insufficient depth, stale references.

| Pass | What to Do |
|------|-----------|
| Pass 1 | Decompose existing chapter into concept blocks. Flag ≥5 questions about accuracy, completeness, or depth of specific claims. |
| Pass 2 | **grep-verify key claims** against source. Test depth per methodology/03: does each explanation carry a design decision, or is it "what" without "why"? |
| Pass 3 | **Fix real content issues found in Pass 2** — inaccurate claims, missing depth, broken cross-references. Output: improved version of existing chapter. |

**Anti-patterns in Review Mode**:
- Adding 前置依赖 + 设计权衡 sections without doing Pass 2 source verification → format audit, not content review
- Rewriting the entire chapter from scratch → that's Write Mode, not Review Mode
- Declaring "no issues found" after only a structural scan → every chapter has depth gaps; if you found none, you didn't look

### Mode B: Write New Chapters (写新文章)

**Input**: Source code only (no existing text). **Goal**: Create original chapter with methodology structure.

| Pass | What to Do |
|------|-----------|
| Pass 0 | Read design docs, issues, PRs, release notes — build context before touching source. |
| Pass 1 | Scan packages, core class declarations, test files. Build inheritance/call graph. Flag ≥5 questions. |
| Pass 2 | Deep-dive per question: hypothesis → grep trace → verify → conclude. Each question becomes a loop note file. |
| Pass 3 | Synthesize loop notes into article: problem-first, dependency-ordered, concept-progression, naturally narrated. Design tradeoffs woven in, not appended. Source-verified throughout. |

**Anti-patterns in Write Mode**:
- Starting with "0. Source Code Listing" before explaining the problem → code-first, not problem-first
- Using AI-template formatting (numbered § sections, ✅❌ symbols, "→ 细节：[link]" after every paragraph) → natural narrative is the goal

### Mode Selection Rule

If the task is "REVIEW chXX" → Mode A. If the task is "WRITE chXX from scratch" → Mode B. Never confuse them.

---

## Core Constraints

1. **No fabrication**: Every function name, constant value, or data structure description must be `grep`-verified against source before appearing in output. Uncertain items marked `[TODO: verify]`, never guessed.
2. **No skipping**: Must complete all Pass 1 outputs before entering Pass 2. At least 3 loop-closed questions required before entering Pass 3 (see Pass 2 Completion Checklist for definition of 'loop-closed').
3. **No "covered later"**: If this domain depends on classes/interfaces from another domain, the dependency MUST be analyzed first. Never write "will be covered later."
4. **Every conclusion has source location**: Format `file:line`.
5. **Code blocks must be compilable or explicitly marked**: Every code block in output is either verbatim from source (annotated with `file:line`) or explicitly marked `[pseudocode]`. No pseudocode presented as real code.
6. **Numeric claims MUST be grep-verified**: Every numeric constant (count, size, threshold, formula) must be `grep`-verified against source. "cores/2" written without grep → fabrication. Actual: `availableProcessors() * 2` clamped by memory (PooledByteBufAllocator.java:104-117).
7. **Class name ≠ behavior**: Do NOT infer a class's responsibility from its name alone. `AdaptivePoolingAllocator` was assumed to "select heap/direct" — actually adapts pool capacity based on historical usage. Read the source body, not just the class declaration.
8. **External API behavior claims require verification**: Claims about JDK/NIO behavior (e.g., "SocketChannel.write() only accepts DirectBuffer") must be verified against JDK source or documentation — do not rely on framework intuition.
9. **Each loop note MUST be written to file immediately**: After completing the grep→verify→conclusion cycle for a question, write the loop note file BEFORE moving to the next question. Do NOT batch-write after all questions are done — leads to forgetting files (ByteBuf Q2/Q3 incident). After all Pass 2 questions: `find output-dir -type f | sort` to verify every expected loop note exists.

### Self-Enforcement Protocol

**Before ANY analysis or review, output this declaration block. Each item must include EVIDENCE COUNT — declaration is worthless without numbers.**

```
## 已读声明 + 执行证据

- [x] methodology/01 Pass 1: {N} concept blocks decomposed
- [x] methodology/01 Pass 2: {N} source claims grep-verified, {N} found inaccurate/missing
- [x] methodology/01 Pass 3: {N} structural issues found (missing sections, stale maps)
- [x] methodology/03 Depth test: {N} claims checked for "why" vs "what-only"
- [x] methodology/05 Tool chain: codegraph {N} queries, grep {N} verifications
- [x] Source path: {path}
```

**Post-Execution Self-Audit**

After analysis completes, verify these are TRUE:
- [ ] grep-verified ≥3 claims against actual source code → if not, **Pass 2 was not executed, restart**
- [ ] Found at least ONE content inaccuracy or missing claim → if zero found, **review was a format scan, not a content review**
- [ ] Used at least one semantic tool (codegraph/LSP) beyond grep → if not, **methodology/05 violated**
- [ ] Checked ALL numeric claims against source → if `cores/2` appears without grep verification, **fabrication**
- [ ] Checked ALL class-name-based inferences against actual source body → if `AdaptivePoolingAllocator` was described without reading its `allocate()` method, **fabrication**
- [ ] ALL N loop notes exist as files → `find output/ -name "pass2-q*.md" | wc -l` matches expected count
- [ ] Concept coverage audit: compared Pass 2 questions against known feature surface of this domain (test files, public API, book TOCs) → no known feature left uncovered
- [ ] ALL code examples are runtime-verifiable → `register(selector, OP_READ)` without attachment means downstream `key.attachment()` returns null (NPE at runtime, even though it compiles)
- [ ] ALL fix applications sync'd across all sections → fixing "NioEventLoop" in narrative also fixed in Pass 0 metadata section (grep the term across the full file after every fix)

**If any self-audit item is unchecked, the analysis is incomplete. Do NOT present to user until corrected.**

---

## Pass 0: Pre-Code Reading (before scanning source)

**Code is the result of design, not its source. Read design sources first.**

### Execute Commands

```bash
# 1. README + docs/ + design docs
Read README.md
find docs/ -name "*.md" | head -5
# Read any architecture/design docs found

# 2. Issues + PRs (highest information density)
# Git log for recent significant changes
git log --oneline -30
# Search for design-related commits
git log --oneline --grep="refactor\|design\|architecture\|rewrite"

# 3. Release notes / CHANGELOG
ls CHANGELOG* RELEASE* 2>/dev/null
# If found, Read the most recent one (highest version number for CHANGELOGs)
# — evolution faster than code reading

# 4. Test directory as feature map
find src/test -type d | sort | head -20
```

### Output

Nothing — absorption only. Build context before touching implementation code. Skip this step only if zero docs/issues/release-notes exist (rare).

### Pre-Pass 1 Check

Before entering Pass 1, verify MCP index freshness. If the repo was updated since the last index (git pull, branch switch), re-index before scanning. If the repo has never been indexed (first-time analysis), create the index now using codebase-memory-mcp `index_repository` (see 05 for commands). See 05 for per-tool re-index procedures.

---

## Pass 1: Scan Outline

### Execute Commands

```bash
# 1. Locate source directory (from execution plan)
cd /data/workspace/source-code/code/spring/{repo}/

# 2. Package structure
find src/main/java -type d | sort | head -30

# 3. Core class declarations
grep -rn "^public (class|interface|enum) " src/main/java/{pkg} | head -20

# 4. Hottest files (git log trick)
git log --pretty=format: --name-only | sort | uniq -c | sort -rn | head -10

# 5. Read 2-3 test files
find src/test/java -name "*Test*.java" | head -5
# Read first 2-3

# 6. MCP query call relationships
mcp__codebase-memory-mcp__search_graph(
  project="data-workspace-source-code-code-spring-{repo}",
  query="{core class name}"
)
```

### Output Format

```
## Pass 1 Output: {domain name}

### Inheritance Tree / Call Graph
[ASCII art]

### Basic Element Decomposition (Principle 2)
1. Element A: [one sentence] — source {file:line}
2. Element B: [one sentence] — source {file:line}
...

### Flagged Questions (≥5)
1. [specific question] — Why? How is it implemented?
2. ...
```

### Completion Checklist

- [ ] Inheritance tree / call graph drawn
- [ ] Answered: "What basic elements compose this module?"
- [ ] ≥5 flagged questions, each with source location
- [ ] Read ≥2 test files

---

## Pass 2: Deep-Dive

### Per Question Loop

```
1. Read the question's source location (Read tool)
2. Form hypothesis: "I think this mechanism works as: XXX"
3. Verify hypothesis:
   - grep trace call chain (≥2 levels)
   - Read callee key method bodies
   - Check variable naming, comments, exception handling for design intent
4. Determine code type (pick one):
   - Glue → ask: what interfaces does it connect? How are errors handled?
   - Interface → ask: is the contract complete? What internals are exposed?
   - Implementation → ask: what is the state lifecycle? What's the risk of changing it?
   - Algorithmic → ask: what are the data structures? What's the time complexity?
5. Conclusion: hypothesis validated or disproven? Keep source location evidence.
```

### Output Format

```
## Loop Note: {question}
Hypothesis: [one sentence]
Verification:
  - grep {pattern} → found {file:line}, confirms/refutes XXX
  - Read {file:line} → in method body: XXX
  - Traced to {file:line} → XXX
Code type: {Glue/Interface/Implementation/Algorithmic}
Cross-domain: [if applicable] {domain} → {connection type}
Conclusion: [one sentence, with source locations]
```

### Completion Checklist

- [ ] ≥3 flagged questions loop-closed
- [ ] Each loop has ≥2 source location evidence points
- [ ] Each loop tagged with code type
- [ ] No "I guess" — all conclusions backed by source
- [ ] If new package/class discovered → **PAUSE, loop back to Pass 1**
- [ ] **Files exist**: `find output/ -name "pass2-q*.md" \| wc -l` ≥ flagged question count
- [ ] **Concept coverage audit**: Pass 2 loop notes + flagged questions cover the domain's full feature surface (verified against test file names, public API surface from Pass 1, and framework book TOCs)
- [ ] **No class-name inference**: Every behavioral claim came from reading method bodies, not from class names

### Discovered → Back to Pass 1

When Pass 2 discovers a class not in Pass 1's inheritance tree:
1. Return to Pass 1 and scan that package/class
2. Update inheritance tree
3. Add new flagged questions
4. Continue Pass 2

### Temporal Tracing (🔴 domains only)

For core domains, reduce complexity by reading early versions:

```bash
# Find earliest semantically meaningful tag
git tag --sort=version:refname | head -20
# Checkout early version for skeletal understanding
git checkout <earliest-tag>
# Read the simplified version — no backward-compat glue, no edge-case optimization
# Then: git diff <early-tag> <current-tag> -- <core-package>/ to see evolution
```

Early versions reveal design intent stripped of years of compatibility layers. Use only for 🔴 domains where current version complexity is blocking understanding.

If you check out an early version, revisit Pass 1's inheritance tree and Pass 2 loop notes with both versions in view — the early version serves as a comparison baseline, not a replacement for current-version analysis.

---

## Pre-Pass 3: Multi-Article Planning for Large Domains

**The Pass 3 output template assumes ONE domain = ONE article. For large domains (≥6 loop-closed questions), one article is insufficient — it becomes a superficial tour, not deep analysis.**

### When to Split

Count Pass 2 loop-closed questions (N). If N ≥ 6, the domain MUST be split into multiple articles.

### How to Split

```
1. Cluster loop notes by topic affinity into M groups (2-4 notes per group)
2. Each group becomes one article:
   - Title: meaningful sub-topic name
   - Question-to-topic mapping: which Q#s it covers
   - Core problem statement: "What question does this article answer?"
3. Order articles by concept dependency (foundation → application)
4. Present the article plan to user for approval before writing ANY article
```

### Article Plan Template

```
## Pass 3 Article Plan: {domain name}

Pass 2 produced {N} loop-closed questions → split into {M} articles:

| # | Title | Covers | Core Question |
|:--:|------|:--:|------|
| 上 | {title} | Q1, Q2, ... | {one sentence} |
| 中 | {title} | Q4, Q5, ... | {one sentence} |
| 下 | {title} | Q3, Q6, ... | {one sentence} |

Each article follows the Pass 3 output template (§ below).
Does this plan look right? Any missing topics?
```

### Anti-Pattern

```
WRONG: ByteBuf has 9 loop notes → cram all into one article → each note gets 2 paragraphs
RIGHT: ByteBuf → split into 3 articles → each note gets full narrative treatment

WRONG: Split into too many articles (M too large) → each article still shallow
RIGHT: 2-4 notes per article — enough to be substantial, not so many that depth collapses
```

### Mega-Domain Splitting (NEW) — When N ≥ 20, One Domain = One Volume

**Trigger**: Pass 2 loop notes N ≥ 20, OR source lines ≥ 30,000, OR file count ≥ 100.

**Difference**: Regular large domains (6 ≤ N < 20) split into 2-5 articles within one outline file. Mega-domains (N ≥ 20) split into **8-15 articles** — each with its own outline file, the entire domain occupies **one volume**'s worth of content.

**Examples**:
```
Regular large domain: ByteBuf (9 notes) → split into 3 articles, single outline file
Mega-domain:   G1 GC (30+ notes, 45,692 lines/195 files) → split into 8-10 articles, independent files, one full volume
Mega-domain:   C2 Compiler (30+ notes, 139,595 lines/129 files) → split into 8-10 articles, one full volume
Mega-domain:   JFR (20+ notes, 215 files) → split into 6-8 articles
```

**Mega-domain article plan template**:
```
## Mega-Domain Article Plan: {Domain Name}

Pass 2 produced {N} loop notes → source {L} lines / {F} files → Mega-domain → split into {M} articles

| # | Title | Notes Covered | Core Question | Estimated Length |
|:--:|------|:--:|------|:--:|
| 1 | Region Model | Q1,Q2,Q5 | How does G1 chop the heap into 2048 blocks? | 2000-3000 |
| 2 | GC Cycle Orchestration | Q3,Q8 | When to do young GC vs mixed GC? | 2500-3500 |
...
| 10 | Tuning & Logging | Q28,Q29,Q30 | What does every entry in gc.log mean? | 2000-3000 |

Each article gets its own outline file: {domain}/01-region-model.md through {domain}/10-tuning-logging.md
```

**Anti-Patterns**:
```
WRONG: G1 GC 45,692 lines → Pre-Pass 3 claims "force split 3-4 articles" → still superficial
RIGHT: N ≥ 20 → escalate to mega-domain → split into 8-10 articles → genuine depth coverage

WRONG: Mega-domain and regular large domain share the same split template → named "Part 1/2/3"
RIGHT: Mega-domain each article gets its own outline file → independent title and clear boundaries
```

---

## Per-Article Writing Outline

**Before writing any article, produce a one-page outline. Present it to the user for confirmation.** The outline bridges the gap between "4 independent loop notes" and "one coherent narrative." Without it, Pass 3 Step 2 ("write complete narrative") is a black box.

### Teaching Narrative Design (before Outline Format)

> An outline must not be just a technical inventory — it must guide the AI to write articles that readers can understand. Reference standard: 《MySQL是怎样运行的》(How MySQL Works, by 小孩子4919).

**Technical Inventory vs Teaching Narrative**:

| | Technical Inventory (WRONG) | Teaching Narrative (RIGHT) |
|------|------|------|
| Title | "CodeBuffer + AbstractAssembler — container for machine code" | "Starting from a single record — InnoDB record storage structure" / "The big box that holds records — InnoDB data page structure" |
| Structure | Top-down enumeration of technical mechanisms | Simple scenario → gradual complexity → summary closure |
| Language | Assumes reader knows background | Builds understanding from zero |
| Knowledge transfer | Answers "what did this code do?" | Answers "how do you understand why this code does this?" |

**Five elements of Teaching Narrative Design**:

1. **Reader Situation Analysis** — Where does the reader stand? What do they already know? What new perspective will this open?
2. **Core Narrative Arc** — What scenario/story threads all KPs together? **Simple → complex → summary**, one increment per step
3. **Metaphor Anchor** — An everyday analogy for each 🔴 mechanism — not a technical translation, but a cognitive bridge
4. **The "Aha Moment"** for each article — What will the reader suddenly understand after reading? One sentence
5. **What→Why→How order** — First build intuition ("what phenomenon occurs") → then explain the cause ("why this design is needed") → finally show the implementation ("how the source code does it")
6. **Cross-Layer Knowledge Embedding** — The outline must annotate which external knowledge layers each mechanism depends on, guiding the AI to embed explanations during writing:
   - **C++ Systems Programming Layer**: JVM is a C++ program — involving POSIX APIs (mmap/mprotect/pthread/sigaction/dlopen), memory model (volatile/atomic/memory_order), template metaprogramming (SFINAE/type traits). Annotate with `[C++: concept]` to prompt AI to explain.
   - **Linux Kernel Layer**: JVM's behavior is determined by kernel mechanisms — virtual memory (overcommit/THP/khugepaged), signal handling (SIGSEGV dispatch), scheduling (CFS/nice), cgroup (v1/v2 subsystems). Annotate with `[Kernel: concept]` to prompt AI to explain kernel behavior.
   - **x86/CPU Hardware Layer**: Some mechanisms relate directly to CPU — TLB/cache line/page table walk/MESI protocol/LOCK prefix. Annotate with `[x86: concept]` to prompt AI to briefly explain the hardware layer.

**Cross-Layer Embedding Rules**:

Outline level (this step's output):
- Each 🔴 mechanism should have at least one external knowledge layer annotation, 2-3 sentence summary — prompting the AI "what background knowledge to explain here"
- Annotation format: append after source anchor `[C++: mmap MAP_NORESERVE semantics]` / `[Kernel: vm.overcommit_memory parameter controls overcommit]` / `[x86: TLB miss cost ~100 cycles]`

Writing level (Pass 3 execution, not this step):
- Cross-layer knowledge must be **expanded in detail** — not one-liners. Explain POSIX API parameters/return values/error codes, referencing `man 2 mmap` / `man 3 pthread_cond_wait` for verification when necessary
- Linux kernel behavior must explain the causal chain — "JVM calls mmap → kernel handles page fault → overcommit_memory decides allocation → if OOM → OOM Killer behavior"
- x86 hardware layer must give order-of-magnitude — "TLB miss ≈ 100 cycles (3 page table walks), L1 cache hit = 4 cycles. Huge pages reduce page table walk from 4 levels to 3"
- **man page verification**: descriptions of POSIX APIs / syscalls / kernel parameters must be cross-verified using the correct man section based on API type:
  - `man 2` — system calls: mmap, mprotect, futex, sigaction, clone, sched_setaffinity
  - `man 3` — C library functions: pthread_cond_wait, dlopen, sem_init, clock_gettime
  - `man 5` — file formats / config: proc(5), cgroups(5), locale(5)
  - `man 7` — overview / conventions: signal(7), pthreads(7), cgroups(7), capabilities(7), overcommit(7)
  - Annotate with `[man 2 mmap]` / `[man 7 signal]` in outline to prompt AI to consult the correct section during writing
- Cross-layer knowledge is NOT a standalone section — integrate naturally into the narrative flow, introduced when needed by source code analysis

**Narrative Arc Construction Rules**:

- Opening is a **concrete scenario**, not an abstract concept. Example: "You wrote an infinite recursion. JVM didn't crash — why?"
- Each section answers **one question the reader naturally asks**. The answer to the previous section raises the next question.
- Ending returns to the opening scenario and explains the answer.

### Outline Format

```
## Article Outline: {article title}

### Concept Dependency Chain
[How do the notes in this article depend on each other? Which must be taught first?]

### Narrative Order
1. Problem introduction — what real problem does this article solve?
2. {first concept} — the foundation
3. {second concept} — builds on the first
4. ...
   Natural transitions between each step.

### Core Tension
[What is the central question readers should be able to answer after reading?]
[One sentence. E.g.: "Why did Netty reinvent the buffer when Java NIO already has ByteBuffer?"]
```

### Example

```
## Article Outline: ByteBuf Core Abstraction

### Concept Dependency Chain
Q8 capacity expansion → Q2 reference counting → Q1 dual-index → Q9 byte order

### Narrative Order
1. Problem: Java NIO ByteBuffer's flip/compact state machine — why it breaks in Pipeline
2. Dual-index model: readerIndex/writerIndex replace flip — how it enables handler independence
3. Capacity expansion: what happens when writable space runs out? 4MB threshold
4. Reference counting: why GC isn't enough for direct memory. retain/release lifecycle
5. Byte order: BigEndian default, LE suffix, SwappedByteBuf — network protocol alignment

### Core Tension
"Why did Netty reinvent the buffer when Java NIO already has ByteBuffer?"
```

### Rule

- **Outline MUST be presented to user before writing. User confirms or adjusts.**
- Outline prevents: dumping notes in random order, treating loop notes as sections, missing the "problem→solution" narrative arc.
- If user asks "这篇应该先讲什么?" you skipped this step.

---

## Pass 3: Synthesize

### Steps

1. Take all Pass 2 loop notes, order by logical sequence
2. **Write Per-Article Outline** (see § above) — present to user for confirmation
3. Write complete narrative — explain thoroughly, no line limit
4. Add design tradeoffs — "why was this design chosen over alternatives"
5. Add cross-domain references (from execution plan)
6. Verify content requirements checklist above — all items covered

**Note**: Interview questions are a SEPARATE companion file per chapter (`面试考点.md`), not embedded in the main chapter. Source analysis is a book, not an interview prep guide.

### Skip Conditions

Pass 3 skip follows the approach decision in `04-approach-decision-tree.md`. Summary:
- **A (🔴)**: Pass 3 is mandatory
- **B (🟡)**: Pass 3 is optional — do only if ≥5 flagged questions are worth synthesizing
- **C (🟡 simple)**: Pass 3 is skipped — abbreviated Pass 2 output is final
- **D (glue/enum)**: Pass 3 is not applicable — only Modified Pass 1

### Content Requirements

**These define WHAT must be covered, not HOW to write it.** Expression is free — narrative, Q&A, problem-driven, whatever fits the content. No section-number templates, no fill-in-blank formats, no word limits.

Each article MUST include:
- [ ] **Core mechanism** — explained thoroughly. A complete novice can understand it. No line limit. No section count limit. Write until it's clear.
- [ ] **Design tradeoffs** — "why was this design chosen over the alternative?" Show the rejected path.
- [ ] **Cross-domain references** — ← what this depends on / → what depends on this / Also see related. Root/leaf domains note their boundary.
- [ ] **Source anchors** — every code claim has `file:line`. Code blocks are verbatim from source or explicitly marked `[pseudocode]`.

Expression rules:
- [ ] **Natural prose**. Do NOT use: `## 0. 问题` / `§3.1` / `✅❌` markers / `→ 细节：[link]` footers. Write like a book chapter, not like an AI template.
- [ ] **NO artificial limits**: no word count targets, no section count constraints. The methodology controls conceptual order (problem→mechanism→tradeoffs→cross-refs), not output format.

### Anti-Patterns for Pass 3 Writing

```
WRONG: Full of "## 0. 问题——" "## 1. 依赖链——" "§3.1" "✅ 已就绪" markers
       → AI味太重了, 不是书籍质量
RIGHT: "上一节我们理解了 ByteBuf 的双指针模型。现在的问题是：Netty 如何管理这些缓冲区的内存？"
       → 自然叙事, 问题驱动, 对话感

WRONG: Setting a 800-word target and cutting content to fit
       → 方法论写"explain thoroughly, no line limit" for a reason
RIGHT: Writing until the mechanism is clear. If it takes 300 lines, it takes 300 lines.

WRONG: Repeating the same "→ 细节：[link]" footer after every paragraph
       → 模板填充, 不是写作
RIGHT: Cross-references flow naturally in prose: "后面 EventLoop 域会详细展开"
```

### Completion Checklist

- [ ] A complete novice can understand the core mechanism
- [ ] ≥1 design tradeoff shown (rejected alternative named)
- [ ] ≥1 cross-domain reference
- [ ] All source references have `file:line`
- [ ] Content requirements checklist (§ above) ALL items checked
- [ ] Book quality — natural prose, no AI template markers

### Minimal Reproduction (Feynman Method)

For 🔴 domains after Pass 3, verify understanding by building a micro-engine:

```
"What I cannot create, I do not understand."

Write a minimal implementation in harness/{framework}/:
  - No concurrency safety, no backward compatibility, no configuration
  - Only the core control flow (~200-300 lines)
  - Example targets: 200-line Reactor loop, 300-line IoC container

If it runs → understanding verified. If it doesn't → gap found, return to Pass 2.
```

---

## Outline Completeness Verification: Multi-Perspective Questioning

> Execute after deep review (R1+R2), before claiming "outline complete." The user should not need to request this — it is mandatory.

### Core Principle

**Outline completeness is not measured by "does this outline look complete" — but by "can 7 different personas read this outline and answer their respective questions."**

If a typical question from any persona has no corresponding mechanism in the outline → outline is incomplete.

### When to Execute

After each domain's Per-Article Outline passes deep review (R1+R2), immediately execute multi-perspective questioning verification.

### Seven Personas

The following 7 personas are the standard set for **JVM/compiler/GC domains**. For other domains (e.g., networking framework, database), adapt the personas to domain-relevant roles — the number and types are not fixed, but must cover multiple perspectives.

| Persona | Perspective | Typical Questions | Applicability |
|------|------|------|------|
| **Developer** | "I need to modify this code" | Invariants? Concurrency guarantees? Edge cases? | All domains |
| **Performance Engineer** | "I need to tune this" | Flag effects? Monitoring metrics? Rules of thumb? | Domains with tuning knobs |
| **Architect** | "Why this design" | Why A over B? Trade-offs? | All domains |
| **Researcher** | "How does it compare" | vs alternatives? Theoretical limits? Unsuitable scenarios? | Domains with multiple implementations |
| **SRE** | "It broke in production" | Log diagnosis? Crash tracing? Degradation paths? | Production-viable domains |
| **Subsystem Developer** | "How does the layer above/below interact" | Barrier injection? Optimization elimination? Callback interfaces? | Domains with cross-layer interaction |
| **Student** | "I'm seeing this for the first time" | Why this name? Can you explain with an analogy? | All domains |

### Execution Steps

```
1. List the domain's 8-10 sub-topics (natural clustering)
2. For each persona × each sub-topic → generate 2-5 questions that persona would genuinely ask
3. For each question, mark whether the current outline covers the corresponding mechanism
   - ✅ Outline has it → note the corresponding section
   - ⚠️ Partial coverage → note what's missing
   - ❌ Outline lacks it → mark as incomplete
4. Aggregate ❌ and ⚠️ → return to outline → fill gaps → re-verify
5. Once all ✅ → retain the question document as verification evidence (`{domain}-completeness-questions.md`)
6. When presenting the outline for user confirmation, include the question document's coverage stats (❌/⚠️/✅ counts)
```

**Note**: Gap filling after verification does not require user intervention — AI autonomously fills gaps → re-checks → until all ✅. Only escalate to the user when a particular type of question consistently cannot be answered (e.g., student questions keep being unsatisfactory).

### Output Format

```
## Multi-Perspective Verification: {Domain Name}

| # | Persona | Sub-Topic | Question | Outline Coverage |
|:--:|------|------|------|:--:|
| 1 | Developer | Region | Why must Region size be a power of 2? | ✅ Article 1-L3 |
| 2 | Architect | SATB | Why SATB over incremental update? | ⚠️ Article 4 has mechanism, no comparison |
| 3 | SRE | Full GC | How to diagnose "To-space exhausted" in gc log? | ❌ Not covered |

⚠️ 2 partial, ❌ 1 missing → return to outline for completion.
```

### Quantity Baselines

The following are **total question counts** (across all applicable personas), not per-persona counts.

| Domain Type | Source Lines | Min Total Questions | Min Personas | Verification Granularity |
|------|:--:|:--:|:--:|------|
| Regular | <10,000 | 30 | 5 | Domain-level (one outline = one verification) |
| Large | 10,000-30,000 | 50 | 6 | Domain-level or per-article (per-article for ≥3 articles) |
| Mega | >30,000 | 80 | 7 | **Per-article** (each article outline gets its own question doc + verification) |

### Anti-Patterns

```
WRONG: All questions are "What is X?" — interview-question mindset
RIGHT: Each persona asks "Why" + "Compared to Y" + "What if it fails"

WRONG: Ask questions, done. Never map them back to the outline
RIGHT: Mark each question's coverage → aggregate gaps → fix → re-verify

WRONG: User has to remind "generate the question document" every time
RIGHT: Automatically produce the question document after every domain outline
```
