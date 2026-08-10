# Knowledge Planning for Source-Code Analysis Projects

> Role: AI self-constraint protocol. Must-read before knowledge planning for pure source-code projects (no book TOC reference).
> Prerequisites: knowledge-planning/01~04 (TOC extraction workflow)
> Input: Source repository file/package structure
> Reading order: 01 → 02 → 03 → 04 → 05 (this document). Use only in "no book TOC reference" mode.

## Core Principle

Source-code analysis projects use the **same methodology pipeline** as book knowledge planning (extraction → aggregation → classification → clustering), but the "raw material" differs:

| | Book Knowledge Planning | Source-Code Knowledge Planning |
|------|------|------|
| Raw Material | Book TOC (chapters/sections/subsections) | Source files/package structure |
| Extraction Unit | Each section → one knowledge point | Each source file/class → one mechanism |
| Aggregation Signal | How many books mention the same concept | How many source files reference the same mechanism |
| Classification Baseline | What does a backend engineer need to understand | What does a JVM/framework developer need to understand |
| Clustering Baseline | Concept dependency chain | Topological ordering (A depends on B → B taught first) |

## 1. Extraction — Extracting Knowledge Points from Source Code

### 1.1 Extraction Units

When extracting knowledge points from source repositories, use **package** as the first-level granularity and **core class/file** as the second-level granularity.

```
Package scan → list all sub-packages → identify core classes per sub-package → each core class = 1-N mechanisms
```

### 1.2 Output Format

```markdown
## 01 Extraction — Per-Source Mapping

### Package: hotspot/share/gc/g1/ (195 files)

| Source File | Inferred Knowledge Point | Confidence |
|------------|------------------------|------------|
| heapRegion.hpp:191 | HeapRegion — G1's basic heap allocation unit | High |
| heapRegion.hpp:191 | Region's four roles: Eden/Survivor/Old/Humongous | High |
| heapRegionRemSet.hpp | RSet — cross-Region reference index | High |
| g1ConcurrentMark.hpp | SATB concurrent marking algorithm | High |
| g1Policy.hpp | GC cycle orchestration and prediction model | Medium |
```

### 1.3 Confidence

| Confidence | Meaning | Example |
|------|------|------|
| High | Directly read from class declaration/method body | `class HeapRegion` → mechanism clear |
| Medium | Inferred from class name, method body not yet verified | `class G1Policy` → orchestration logic pending |
| Low | Guessed from class name, source reading needed | `class G1MMUTracker` → may belong to monitoring subsystem |

### 1.4 What Counts as a "Mechanism"

**Counts as a mechanism** (corresponds to "counts as a knowledge point" in book planning):
- An independent data structure design (e.g., `HeapRegion` layout)
- An algorithm implementation (e.g., SATB concurrent marking)
- A cross-component interaction protocol (e.g., JIT → GC barrier injection protocol)

**Does NOT count as a mechanism**:
- getter/setter/toString — pure data access
- Pure utility classes (e.g., `GrowableArray` — infrastructure, no domain-level design decision)
- Build configuration (e.g., flag declarations in `globals.hpp` — belongs to Arguments domain)

## 2. Aggregation — Cross-File Deduplication

Unlike book knowledge planning's multi-book cross-validation, source-code knowledge planning's aggregation is based on **file reference frequency**.

```markdown
## 01 Aggregation — Cross-File Summary

| KP | Source Files | P-Level |
|----|-------------|---------|
| RSet 3-level storage | heapRegionRemSet.hpp, g1RemSet.cpp, g1RemSetSummary.cpp | P1 |
| SATB concurrent marking | g1ConcurrentMark.hpp, g1SATBCardTableModRefBS.cpp, ptrQueue.hpp | P1 |
| Humongous reclamation | g1CollectedHeap.cpp:1448, heapRegion.hpp | P2 |
```

**Aggregation Rules**:

| Source Files Referenced | Tier | 
|----------|:--:|
| ≥5 files reference | P1 — System-wide consensus |
| 2-4 files reference | P2 — Locally important |
| 1 file only | P3 — Evaluate if standalone knowledge point |

## 3. Depth Classification — 🔴🟡🟢

Uses the same classification criteria as book knowledge planning (03-Analysis Depth Standards), but the judgment baseline shifts from "backend engineer" to "JVM/framework developer".

```markdown
## 02 Depth Classification

### 🔴 Deep (carries core design decisions)

| KP | Why 🔴 |
|----|---------|
| SATB concurrent marking | Algorithm choice (SATB vs Incremental Update) — design decision |
| RSet 3-level storage | Data structure choice (sparse→fine→coarse) — design decision |

### 🟡 Working (has design decisions but not core)

| KP | Notes |
|----|-------|
| GC cycle orchestration | Prediction model is important but not G1-unique |

### 🟢 Surface (mechanism-level understanding sufficient)

| KP | Placement |
|----|----------|
| MMUTracker | Sub-topic in tuning chapter |
```

## 4. Clustering — Teaching Order

```markdown
## 03 Clustering

### Cluster A: Region Model (6 KP)
Region types/layout/commit/BlockOffsetTable — bottom layer, no prerequisites

### Cluster B: Young GC (5 KP)  
Depends on A (Region), not on marking — teach simple STW collection first

### Cluster C: Concurrent Marking (8 KP)
SATB/barrier/concurrent marking/remark — depends on A+B

### Cluster D: RSet + Refinement (6 KP)
Depends on A+C — need marking results to know which cards to record

### Cluster E: Mixed GC + Full GC (7 KP)
Depends on A+B+C+D — aggregates all knowledge

### Teaching Order: A → B → C → D → E
```

## 5. Relationship with Per-Article Outline

Knowledge planning (01→02→03→04) is the **prerequisite step** for Per-Article Outlines.

```
Correct flow: 01 Extraction → 02 Aggregation → 03 Classification → 04 Clustering → Per-Article Outline
Wrong flow: Jump directly to Per-Article Outline (skipping extraction/aggregation/classification/clustering)
```

Knowledge planning answers "what knowledge points exist, how should they be classified, how should they be ordered" — Per-Article Outline uses these answers to write "what each article covers".

## 6. Multi-Dimensional Analysis — Three Perspectives Beyond Pure Source Code Extraction

Source code extraction (§1-§4) covers the "implementation layer" — but some projects require additional analysis dimensions to determine domain boundaries and content depth:

### 6.1 Project Type Pre-Assessment

Before starting knowledge planning, determine the project type:

| Project Type | Example | Core Analysis Dimensions |
|------|------|------|
| **Pure Framework** | Netty | Source extraction (§1-§4) + design intent |
| **Spec Reference Impl** | Tomcat (Servlet spec) | Source extraction + **spec-to-impl mapping** + design patterns |
| **Middleware** | RocketMQ | Source extraction + architecture layering + protocol design |

For **spec reference implementations** (e.g., Tomcat implementing Jakarta Servlet specification), the source architecture is **spec-driven** — domain boundaries must reflect "spec interface → implementation class" mappings.

### 6.2 Specification/Standard Dimension

If the project is a reference implementation of a specification, each domain in the knowledge plan must annotate:

```
→ Implements Spec: ServletContext / ServletConfig / ServletContainerInitializer
```

Output format — spec-implementation mapping table:

| Spec Interface | Implementation | Domain |
|------|------|:--:|
| `jakarta.servlet.Servlet` | StandardWrapperValve.invoke() | T-3 |
| `jakarta.servlet.FilterChain` | ApplicationFilterChain.doFilter() | T-3 |

**Detection criterion**: Check if the project contains independently defined spec interface packages (e.g., `jakarta.servlet.*`) with corresponding `org.apache.*` implementations.

### 6.3 Design Pattern Dimension

The "Key Design" column in knowledge planning should explicitly label GoF patterns used, helping readers understand "why it's organized this way" not just "how it's implemented":

| Pattern | Where | Label Format |
|------|------|------|
| Chain of Responsibility | Pipeline-Valve + FilterChain double-chain nesting | `[Pattern: Chain of Responsibility]` |
| Template Method | ContainerBase.startInternal/stopInternal | `[Pattern: Template Method]` |
| Adapter | CoyoteAdapter (coyote.Request→HttpServletRequest) | `[Pattern: Adapter]` |

### 6.4 Architecture Dimension

Source extraction only reveals "local implementation" — the architecture dimension supplements "global design intent":

- **Control Flow Direction**: Who calls whom? (Tomcat's "spec calls user" vs Netty's "framework calls user" — inverted control flow affects architectural understanding)
- **Layering Intent**: What problem does each layer solve? What interface communicates between layers?
- **Lifecycle Topology**: Startup/shutdown ordering relationships (e.g., DAG dependency startup)

### 6.5 Dimension Applicability

Not all dimensions apply to all projects:

| Dimension | Spec Reference Impl | Pure Framework | Middleware |
|------|:--:|:--:|:--:|
| Spec Mapping | ✅ Required | — | Optional |
| Design Patterns | ✅ | ✅ | ✅ |
| Architecture Intent | ✅ | ✅ | ✅ |

## 7. Pre-Domain Audit — Must-Read Before Inheriting Previous AI's Scope Planning

When inheriting a source code analysis project from a previous AI, its "scope planning" (domain discovery) cannot be used directly as knowledge planning input — it must be audited first.

### 7.1 Audit Checklist

1. **Domain Overload Check**: Does a single domain contain too many mechanisms? (Metric: > 10 core classes AND > 5000 lines → consider splitting)
2. **Sub-Mechanism Omission**: Do mechanisms "mentioned in one sentence" in the domain description deserve independence? (Verify with `wc -l` the mechanism's code size)
3. **Exclusion List Verification**: Are excluded functional modules truly unnecessary? (Criterion: Does the Spring Boot ecosystem replace them through other components?)
4. **Spec Gap Check** (spec reference implementations only): Do all spec-defined interfaces have corresponding implementation domains?

### 7.2 Domain Count Control Principle

- **6-8 domains**: Learning-oriented analysis (understanding core chains is sufficient)
- **10+ domains**: Full framework analysis (for mastery seekers)

**Prohibited**: Excessively expanding domains because "a missing sub-mechanism was discovered" — the sub-mechanism may already be covered as a major sub-topic in an existing domain (verification: read the domain description's source lines + core class list).

### 7.3 Post-Audit Actions

Audit outcomes may be:
- **No changes**: Original scope planning is reasonable → proceed directly to §1 source extraction
- **Minor adjustments**: Remove domains replaced by ecosystem (e.g., Tomcat Session → replaced by Spring Session Redis) → adjust domain count
- **Dimension supplementation**: Without increasing domain count, add spec/pattern/architecture annotations to knowledge planning

## 8. Handling Obsolete Mechanisms — Scenes/Data Flows Must Use Contemporary Ecosystem as Primary

### 8.1 Principle

In source code analysis articles, **scene sentences (场景:) and data flows (数据流:) must not use obsolete configuration styles as the reader's default experience**. The framework's actual usage in the contemporary ecosystem must be the primary presentation — obsolete mechanisms may only be mentioned secondarily as "alternative approaches."

### 8.2 Typical Scenarios

**Tomcat/Servlet Container Projects**:

| Obsolete Style | Contemporary Style | Rule |
|------|------|------|
| `server.xml` `<Connector>` configuration | Spring Boot `server.port=8080` / `TomcatServletWebServerFactory` programmatic config | Contemporary as primary; `server.xml` only as "in standalone deployments..." appendix |
| `web.xml` `<servlet>`/`<filter>` declarations | `@WebServlet`/`@WebFilter` annotations / Spring Boot auto-config scanning | Annotations as contemporary; `web.xml` as "in traditional deployments..." appendix |
| `server.xml` `<Valve>` configuration | `WebServerFactoryCustomizer` → `engine.getPipeline().addValve()` | Programmatic API as primary |

**Detection method**: `grep -rn 'server\.xml\|web\.xml' outlines/` → All references must be in secondary form ("in standalone deployments...", "in traditional deployments..."), never as the main scene sentence.

### 8.3 Judgment Criteria

Whether a mechanism is "obsolete" is not about whether it still exists in source code (source may retain extensive historical compatibility code), but about **whether mainstream usage in the current ecosystem has migrated to alternatives**. Judgment flow:

1. How does the mechanism's configuration work in contemporary projects (Spring Boot 2.x+/Quarkus/Micronaut)?
2. If the contemporary approach differs from the historical one → use contemporary approach as the main scene in outlines
3. Historical approach only appears as an appendix note "for understanding the underlying principle," never as the first-reading scene entry point

### 8.4 Why This Matters

Readers have a strong "present-tense reflex" to source code analysis scene sentences — if the first sentence mentions `server.xml` configuration, a modern developer will judge "this is legacy / I don't need to learn this." Contemporary-primary scene sentences **immediately connect the source code analysis to the reader's daily development experience**, significantly improving learning motivation and retention.

**Bad example**: "Configured in `server.xml`: `<Connector port="8080" protocol="HTTP/1.1">`"
**Good example**: "Spring Boot `server.port=8080` — how does `TomcatServletWebServerFactory` create the Connector behind the scenes? In standalone deployments, `server.xml` achieves the same effect through reflection to `Http11NioProtocol`"
