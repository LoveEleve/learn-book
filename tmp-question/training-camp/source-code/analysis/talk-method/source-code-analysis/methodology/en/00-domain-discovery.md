# Domain Discovery — How to Find What to Analyze

> Role: AI self-constraint SOP. Must read before starting analysis of any NEW framework.
> Prerequisite: None (foundational document)
> Input: Framework source code directory + entry point class/method
> Reading order: 00 (this doc) → 01 → 02 → 03 → 04 → 05 → 06.

## Core Principle

**Entry point is the only human-specified input.** Everything else is derived automatically. The AI reads the entry point, follows the execution flow outward, and discovers which subsystems the framework depends on. Those subsystems are the domains.

---

## 1. How to Find the Entry Point

The entry point is the **facade/bootstrapping class** that users call to start the framework. It's the class that creates the initial objects, configures them, and starts the execution.

```
Find the entry point:
- Read README.md or docs/ for "Getting Started" examples
- Search for public methods that return a framework object or start its lifecycle
  (constructor, static factory method, or instance method)
- Look for class names containing "Bootstrap", "Starter", "Main", "Launcher",
  "Factory", "Application", "Context", "Engine"
- Examples:
  Netty → ServerBootstrap.bind()  (instance method with args)
  Spring → ApplicationContext.refresh()  (instance method)
  Kafka → KafkaProducer.send() / KafkaConsumer.poll()  (instance method)
  JVM → CreateJavaVM() in src/share/vm/prims/jni.cpp  (native function)
```

**Input format**: `{entry point class}.{method}()`

### Frameworks Without an Entry Point

Some frameworks (libraries, middleware, embedded frameworks) don't have a single user-facing entry point. In that case:

```
1. Find the MOST USED public API class — the class users interact with most
2. Read its constructor or factory method to see what it creates
3. If still unclear → ask the user: "Which class do you consider the entry point?"
```

The entry point doesn't have to be the "first class users touch" — it just needs to be a class that creates/configures the framework's core objects.

---

## 2. Expand from Entry Point

From the entry point, trace **what gets created, configured, and called**. Each major object or subsystem is a candidate domain.

### Execution Flow Tracing

```
1. Read the entry point method body
2. For each object created (new Xxx):
   → candidate domain: what does this class do?
3. For each configuration call (setXxx, addXxx):
   → candidate domain: what does the configured object do?
4. For each method call (obj.doSomething()):
   → candidate domain: what subsystem does this belong to?
```

### Expansion Depth

Expand **one level at a time**, not all at once:

```
Level 0: Entry point (user-specified)
Level 1: Objects created/configured/called by entry point
Level 2: Objects created/configured/called by Level 1 candidates
...
```

**Stop condition**: Expand until no new candidates are found. Each level should produce fewer new candidates than the previous — if a level produces zero new candidates, expansion is complete. Do NOT expand into implementation details (private methods, internal helpers) — only follow public/protected boundaries.

### Candidate Domain Mapping

Each candidate maps to one or more source packages:

```
Candidate → Package(s) → Design decision test → Domain?
```

---

## 2.5 Sidepath Discovery — Scanning for Independent Subsystems

Entry-point expansion (§2) only finds domains on the **main execution path**. Large frameworks (JVM, Linux kernel, database engines) also have **independent subsystems** that are not called from the primary entry point — they start on their own, on demand, or from separate processes.

### When to Scan

Scan when:
- The entry-point expansion stops producing new candidates, BUT
- The framework has directories at the top level that were NOT explored by the expansion

### How to Scan

```
1. ls the top-level source directories
   JVM: src/hotspot/share/
   Netty: src/main/java/io/netty/
   Kernel: kernel/ mm/ fs/ net/

2. For each top-level directory NOT reached by entry-point expansion:
   → Check: does it contain design decisions?
   → Example: JVM's services/ (JMX/JFR/SA) — not called from CreateJavaVM

3. Mark these as CANDIDATES for sidepath
   → Apply §3 design decision test
   → Apply §3.5 signal-based classification
```

### Sidepath Candidates Example: JVM

```
Entry-point expansion finds (from CreateJavaVM):
  ✅ CodeCache, G1Heap, Metaspace, SymbolTable, Interpreter, JIT, Safepoint

Sidepath scan finds (NOT on CreateJavaVM path):
  services/jmx       → JMX management service    → [test: design decision?]
  jfr/               → JFR recording engine        → [test: design decision?]
  sa/                → Serviceability Agent         → [test: design decision?]
  signal/             → Signal handling             → [test: design decision?]
  launcher/           → java command launcher       → [separate process, not JVM internal]
```

### Sidepath vs Main Path

| Discovery Type | Source | Confidence | Action |
|---------------|--------|------------|--------|
| **Main path** | Entry-point expansion | High (execution-verified) | Proceed normally |
| **Sidepath** | Directory scan | Medium (not execution-verified) | Apply §3 test; if passes → domain; if unsure → `[TODO: verify]` |

Sidepath candidates that pass §3 become domains like any other. The only difference is they were discovered via scan rather than execution flow.

---

## 3. Design Decision Test — FILTER Only

**This section decides ONE thing: is this candidate a domain at all?** It is a binary filter (domain / not-domain), NOT a classifier (🔴/🟡). Classification happens in §3.5.

For each candidate, apply:

```
Is it a domain?
├── Does it contain ANY design decisions?
│   ├── No → ✂️ Exclude: pure tool, delegation wrapper, deprecated code, test infra
│   └── Yes → Is it just a thin wrapper over a single existing mechanism?
│              ├── Yes → ✂️ Exclude: no framework-level decision added
│              └── No → ✅ Domain candidate → proceed to §3.5 for classification
```

### Quantitative Pre-Check

Before making a subjective judgment, collect objective evidence:

| Threshold | Action |
|-----------|--------|
| Package has **≥ 50 source files** | MUST read 3-5 key classes before deciding — do NOT dismiss from intuition |
| Single class has **≥ 500 lines** | MUST check whether it carries independent design decisions — large classes often do |
| Package has **independent subpackage structure** (≥ 3 subdirectories) | Do NOT dismiss as "just an instance of X" — check for sub-domains |

### Domain-Level vs Code-Level Criterion

| Level | Criterion | Applied To | Used By |
|-------|-----------|-----------|---------|
| **Domain level** (00, this doc) | "Is this candidate a domain?" (binary yes/no) | Subsystems/packages | Domain discovery |
| **Code level** (03) | "Does this code fragment carry a design decision?" | Individual methods/classes | Depth analysis (Pass 2) |

🔴 vs 🟡 classification is done in §3.5 using 3 signals, NOT here.

### What to Exclude

- Pure tool classes (PlatformDependent, StringUtil, ObjectUtil)
- Pure delegation wrappers (no design decision added)
- Deprecated code paths (mark as `[deprecated]`, skip)
- Configuration parsing (unless it carries design decisions)
- Test infrastructure (already read in Pass 1 as feature map, not domains)

---

## 3.5 Domain Classification — 🔴 vs 🟡

**This section is the MAIN classifier.** §3 decided whether each candidate IS a domain. This section determines what TIER each domain belongs to.

### Primary Classification: Defining-Characteristic Test

```
Does the framework lose its defining characteristic without this domain?
├── Yes → 🔴 Core domain — this IS what makes Netty Netty, Spring Spring
│         (EventLoop, ByteBuf, Pipeline, GC, BeanFactory, refresh())
└── No → 🟡 Supporting domain — built on core mechanisms, has independent design decisions
          (Codec, Bootstrap, Promise/Future, @Transactional, @Scheduled)
```

**Self-check**: If your classification intuition says 80% are 🔴, you're using the wrong test. Most domains should be 🟡 — 🔴 is reserved for the few mechanisms that define the framework.

### Secondary Classification: 3-Signal Confidence

After assigning 🔴/🟡, validate confidence using 3 signals:

| Signal | What It Measures | How AI Uses It |
|--------|-----------------|----------------|
| **Interview frequency** | How often asked in technical interviews | From AI training data |
| **Production frequency** | How often used in real systems | From AI training data |
| **Framework dependency** | How many other domains depend on it | From §4 dependency graph |

| Confidence | Criteria | Action |
|-----------|----------|--------|
| **High** | All 3 signals agree with classification | AI decides independently |
| **Medium** | 2 of 3 agree | AI presents with reasoning; user may adjust |
| **Low** | ≤ 1 of 3 agree OR signals conflict | AI presents for user decision |

### Quantitative Tiebreaker

When signals are ambiguous or classification is uncertain, use code volume as DISAMBIGUATOR (not primary factor):

| Signal | Evidence |
|--------|----------|
| Domain has **≥ 100 source files** or a **single class ≥ 500 lines** | Confirms the domain is substantial — do NOT reduce tier from 🟡 to ✂️ without reading 5 key classes |
| Domain has **< 10 source files** and **no class ≥ 200 lines** | Could genuinely be thin — but still check for algorithm-level decisions (e.g., HashedWheelTimer = 5 files but time wheel algorithm IS a design decision) |

### Classification Procedure

```
For each domain candidate that passed §3:
  1. Apply defining-characteristic test → 🔴 or 🟡
  2. Score 3 signals → assign High/Medium/Low confidence
  3. If Low confidence → apply quantitative tiebreaker → re-evaluate
  4. Output: Tier + Confidence + Reasoning
```

### Output Format

```
### Core Domains (🔴)
| Domain | Design Decision | Interview | Production | Hub | Confidence |
|--------|----------------|:---------:|:----------:|:---:|:----------:|
| EventLoop | Single-thread event loop | 高频 | 主流 | ✅ | High |

### Supporting Domains (🟡)
| Domain | Design Decision | Interview | Production | Hub | Confidence |
|--------|----------------|:---------:|:----------:|:---:|:----------:|
| Codec | SPI encode/decode | 高频 | 主流 | ❌ | Medium |

Confidence legend:
- High = 3/3 signals agree, AI decision final
- Medium = 2/3 agree, AI presents, user may adjust
- Low = ≤1/3 agree, requires user decision
```

---

## 4. Dependency Graph & Topological Sort

After identifying domains, build the dependency graph:

```
Domain A → depends on → Domain B, C
Domain B → depends on → Domain D
Domain C → depends on → Domain D
Domain D → (leaf — no dependencies)
```

**Rules**:
- `A depends on B` = without understanding B's mechanism, you cannot understand A's behavior or design rationale
- This is MECHANISM-level dependency, NOT package import
- Circular dependencies → apply three-round strategy (from 02 §1.4)

**Topological sort**: Leaves first → hubs next → application-level last. This becomes the teaching order.

---

## 5. Output Format

```
## Domain Discovery: {Framework Name}

### Entry Point
{entry point class}.{method}()

### Core Domains (🔴)
| Domain | Package | Design Decision | Dependencies | Signals | Confidence |
|--------|---------|----------------|--------------|---------|------------|
| {name} | {pkg} | {one sentence} | {list} | {interview/prod/hub} | {High/Medium/Low} |

### Supporting Domains (🟡)
| Domain | Package | Design Decision | Dependencies | Signals | Confidence |
|--------|---------|----------------|--------------|---------|------------|
| {name} | {pkg} | {one sentence} | {list} | {interview/prod/hub} | {High/Medium/Low} |

### Excluded (not domains)
| Class/Package | Reason |
|---------------|--------|
| {name} | Pure tool class / no design decision |

### Teaching Order (topological)
1. {leaf domain}
2. {next domain}
3. ...
```

**Note**: Depth (A/B/C/D) is NOT determined here — it's determined per-domain by 04 §Approach Decision Tree after domain discovery is complete. This document only outputs WHICH domains exist and their dependency order.

---

## 6. Execution Procedure

### Step 1: Entry Point Input
Get the entry point from the user: `{entry point class}.{method}()`

### Step 2: Level-1+ Expansion
Read the entry point method body. For each object created/configured/called, list it as a candidate. Then expand recursively: Level-1 → read each candidate's key classes (2-3 per candidate) → Level-2 candidates → repeat until no new candidates found.

### Step 3: Design Decision Test
For each candidate, check: does this subsystem carry design decisions? Filter out glue code.

### Step 4: Sidepath Scan
After entry-point expansion stops producing new candidates, scan top-level source directories NOT reached by the expansion. For each, apply §3 design decision test. Sidepath candidates that pass become domains with Medium confidence (per §2.5).

### Step 5: Signal-Based Classification
For all surviving domains (main path + sidepath), classify importance using the 3 signals (interview frequency + production frequency + framework dependency). Assign confidence tier (High/Medium/Low).

**Confidence handling**:
- High → AI decides independently, no user review needed
- Medium → AI presents classification with reasoning, proceeds unless user objects within the current session
- Low → AI presents to user for decision, waits for confirmation before proceeding

**Degradation**: If the framework is not in AI's training data (e.g., internal/company framework), interview/production signals are unavailable → fall back to framework dependency only. All domains get Medium confidence → user reviews all.

### Step 6: Dependency Graph
For all surviving domains, list ALL dependencies per domain. Build the graph.

### Step 7: Topological Sort
Sort domains so every dependency appears before its dependent.

### Step 8: Output
Write the domain list, exclusion list, and teaching order in the output format above (§5).

### Step 9: Cross-Reference Validation (MANDATORY)

**After outputting the domain list, validate it against existing knowledge sources.** This prevents the AI from systematically missing domains that are known to be important.

**Procedure**:

```
1. Read the execution plan / prior planning doc for this framework (if exists)
   → e.g., issue/Netty源码学习范围规划.md, issue/Spring源码学习范围规划.md

2. Build a "baseline domain list" from the execution plan:
   Netty example: 执行计划 lists ByteBuf/EventLoop/Pipeline/Codec/HTTP Codec/...

3. Compare: methodology-produced vs baseline:
   | Domain | Methodology | Baseline | Status |
   |--------|------------|----------|--------|
   | HTTP Codec | ✂️ excluded | 🟡 | GAP — re-examine |
   | handler扩展 | ✂️ excluded | 🟡 | GAP — re-examine |
   | HashedWheelTimer | 🟡 | 🟡 | MATCH |

4. For each GAP (baseline has, methodology missing):
   → Re-read 3-5 key classes from that package
   → Re-apply §3 filter + §3.5 classification
   → Either: confirm the gap was correct exclusion (with reasoning)
     OR: add the domain (it was missed)

5. For each EXTRA (methodology has, baseline doesn't):
   → Explain why methodology discovered it
   → Entry-point expansion? Sidepath? Differing classification standard?

6. Output coverage report at the END of 00-domain-list.md:
   ## Coverage Report
   | Source | Domains | Coverage |
   |--------|:-------:|:--------:|
   | Execution Plan | N | — |
   | Methodology/00 | M | M/N (XX%) |
   
   ### Gap Analysis
   | Domain | Methodology | Baseline | Resolution |
   |--------|------------|----------|-----------|
   | X | ✂️ | 🟡 | Re-examined: [excluded correctly because...] / [ADDED: missed because...] |

7. If coverage < 80%: STOP. The methodology has a systematic gap.
   Re-examine §3 criteria — are you being too strict with 🟡?
8. If coverage > 100% (more domains than baseline): Document why methodology found extra domains.
```

**Why this step exists**: Netty domain discovery took 3 rounds (9→10域) before converging. With a cross-reference validation step, the execution plan's 12 domains would have been compared immediately in round 1 — the gaps (HTTP Codec) would be caught before presenting to the user. This step is the difference between "AI guesses once" and "AI systematically verifies."

---

## 7. Example: Netty

### Entry Point
`io.netty.bootstrap.ServerBootstrap.bind()`

### Level-1 Expansion

```
bind() → needs EventLoopGroup (thread model subsystem)
       → needs ChannelInitializer (pipeline initialization)
       → needs Channel (I/O abstraction)
```

### Level-2 Expansion

```
EventLoopGroup → NioEventLoopGroup → NioEventLoop
  → io.netty.channel.nio.* → core: event loop thread model

ChannelInitializer → ChannelPipeline → ChannelHandler
  → io.netty.channel.* → core: request processing chain

Channel → ByteBuf → ByteBufAllocator
  → io.netty.buffer.* → core: memory management
```

### Level-3 Expansion

```
ByteBufAllocator → PooledByteBufAllocator → PoolArena
  → io.netty.buffer.* → sub-domain: memory pool (design decision: pooling algorithm)
```

### Domain List

| Domain | Package | Design Decision | Dependencies |
|--------|---------|----------------|--------------|
| EventLoop | io.netty.channel | Single-thread event loop model | (leaf) |
| ByteBuf | io.netty.buffer | Reference counting vs GC | (leaf) |
| Pipeline | io.netty.channel | Chain of responsibility | ByteBuf |
| ChannelHandler | io.netty.channel | SPI extension mechanism | Pipeline |
| Memory Pool | io.netty.buffer | Pooling allocation algorithm | ByteBuf |
| Codec | io.netty.handler.codec | SPI encode/decode abstraction | ChannelHandler |
| SSLHandler | io.netty.handler.ssl | TLS security layer | ChannelHandler |
| ChunkedWriteHandler | io.netty.handler.stream | Chunked transfer | ChannelHandler |

### Excluded
| Class/Package | Reason |
|---------------|--------|
| PlatformDependent | Pure tool class — no design decision |
| StringUtil | Pure utility — no design decision |
| IdleStateHandler | Timer-based utility — mechanism trivial |

### Sidepath Scan
Netty has no independent subsystems — all packages on the main execution path from `bind()`. Sidepath scan produces zero new candidates. This is common for middleware frameworks; large frameworks (JVM, kernel) typically have more sidepath discoveries.

---

## 8. Completion Checklist

After domain discovery for a framework:

- [ ] **Entry point recorded**: User-specified or AI-identified entry point documented
- [ ] **All levels expanded**: Expansion continued until no new candidates found
- [ ] **Sidepath scan completed**: Top-level directories scanned for independent subsystems
- [ ] **Design decision test applied**: Each candidate (main + sidepath) evaluated for design decisions
- [ ] **Dependencies mapped**: Every domain has a complete dependency list
- [ ] **Circular deps identified**: Three-round strategy recorded for each circular pair
- [ ] **Topological sort verified**: No dependency appears after its dependent
- [ ] **Exclusion list documented**: All excluded classes/packages with reasons
- [ ] **Cross-reference validated (Step 9)**: Domain list compared against existing execution plan / baseline. Coverage report written. All gaps explained. Coverage ≥ 80% or systematic gap resolved.

---

## 9. Anti-Patterns

### 1. Listing all packages as domains

```
WRONG: find src/main/java -type d → every package is a domain
RIGHT: Only packages that carry design decisions are domains.
       Tool packages, utility packages, deprecated packages → exclude.
```

### 2. Using usage frequency as domain criterion

```
WRONG: "Backend engineers use Netty → they need to know EventLoop"
RIGHT: "EventLoop carries design decisions about thread models"
       Usage frequency is for knowledge-planning, not source analysis.
       Source analysis asks: does the code carry design decisions?
```

### 3. Skipping internal subsystems

```
WRONG: "Memory pool is internal — backend engineers don't need to know it"
RIGHT: Memory pool carries design decisions (pooling algorithm).
       Source analysis is about design intent, not usage convenience.
       Internal mechanisms that carry design decisions ARE domains.
```

### 4. Not building dependency graph before teaching order

```
WRONG: "CFS is 🔴, nice values are 🟡 → teach CFS first"
RIGHT: Build dependency graph → topological sort → teaching order.
       Importance determines depth (04), dependencies determine order.
```

### 5. Mixing domain discovery with approach selection

```
WRONG: Output includes "Depth: A/B/C/D" for each domain
RIGHT: Domain discovery outputs WHICH domains exist and their order.
       Approach selection (A/B/C/D) is done per-domain by 04.
```

### 6. Using code volume intuition instead of §3.5 signals

```
WRONG: "This package has 200+ files → must be important → 🟡"
       "This package has 5 files → not important → ✂️ exclude"
RIGHT: Run ALL 3 signals for EVERY candidate.
       Code volume is a data point for the QUANTITATIVE TIEBREAKER only.
       Small packages CAN carry algorithm-level decisions (HashedWheelTimer, 5 files).
EXAMPLE: Netty's codec-http has 262 files. Intuition says "it's just a Codec instance."
       But §3.5 signals: interview 高频 (HTTP stuck/unpacking), production 主流.
       If signals say 2/3 and it has ≥ 100 files → quantitative tiebreaker says 🟡.
```

---

## 10. After Domain Discovery: Knowledge Planning vs Per-Article Outline

Domain discovery answers "what to analyze" — but **how to organize the teaching order of these domains**? For pure source-code projects (no book TOC reference), after domain discovery **do NOT jump directly to Per-Article Outline**. Do knowledge planning first:

```
Domain Discovery (00)
  ↓
Knowledge Planning (knowledge-planning/05 — for source-code projects)
  01 Per-source extraction — extract mechanisms from each source file
  02 Aggregation — cross-file dedup, P1/P2/P3 tiering
  03 Depth classification — 🔴/🟡/🟢 classification
  04 Clustering — dependency-ordered teaching sequence
  ↓
Approach Selection (04) + Book-level Planning (01)
  ↓
Per-Article Outline (01 §Pass 3)
```

**Why you can't skip this**: Domain discovery gives you a set of "domains" (e.g., G1 GC is one domain), but a domain may have 30+ sub-mechanisms spread across 195 files. Without first doing per-source extraction → aggregation → classification → clustering, you can't determine how many articles a domain should split into or what each should cover — that's why G1 GC's 45,692 lines of source produced only a 57-line outline.

**When knowledge planning is needed**:
- Pure source-code projects (no book TOC) → **MUST** do knowledge planning after domain discovery
- Projects with book TOC reference → use book knowledge planning directly (knowledge-planning/01)
