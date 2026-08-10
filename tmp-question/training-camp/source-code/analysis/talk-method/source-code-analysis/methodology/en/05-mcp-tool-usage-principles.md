# MCP Tool Usage Principles

> Role: AI self-constraint SOP. Read when selecting tools during domain analysis.
> Executed? Output: `[05] Tool chain used: {semantic tool} N times, codegraph N times, grep N times (must show fallback usage)`
> Prerequisite: `01-three-layer-loop.md` + `04-approach-decision-tree.md`
> Input: Domain language (Java/C/C++), analysis pass (0-3)
> Reading order: 01 → 02 → 03 → 04 → 05 (this doc) → 06. See 01 for full execution sequence.

## Core Rule

**Always prefer semantic querying over text-pattern matching.** Tools that understand code semantics (type hierarchy, call graph, inheritance) are more accurate than grep and should be used whenever available for the target language.

---

## Tool Classification

### Semantic Layer (understands code structure)

| Tool | Language | Signature Capability |
|------|----------|---------------------|
| JavaLens | Java | type hierarchy, DI registrations, HTTP endpoints, JPA model, data flow, change impact (75 tools) |
| LSP MCP + clangd | C/C++ | go-to-definition, find references, call hierarchy, type hierarchy |
| ~~LSP MCP + jls~~ | Java | **NOT AVAILABLE** — jls (Java Language Server) not installed; fallback chain skips LSP MCP for Java |

### Index Layer (text indexing + graph queries)

| Tool | Language | Signature Capability |
|------|----------|---------------------|
| codegraph | 21 languages (incl C/C++) | search, callers, callees, impact, explore |
| codebase-memory-mcp | 66 languages | search_graph, query_graph, trace_path, search_code |
| ~~sverklo~~ | multi-language | search, refs, overview — available but NOT used in per-pass tables; prefer codegraph for structural queries |

**Prefer codegraph over codebase-memory-mcp for structural queries** (callers/callees/type hierarchy). codegraph's graph structure is more accurate and faster. Use codebase-memory-mcp for: (1) cross-domain large-scale queries, (2) `trace_path` when need to follow execution chains, (3) `search_code` for text-based grep-style searches.

**Index refresh**: Both tools cache stale data. After `git pull` or branch switch, re-index. For large repos (>100k LOC), index once at project start and trust incremental updates.

**Re-index commands**:
- codebase-memory-mcp: `mcp__codebase-memory-mcp__index_repository(project="<project-name>", path="<repo-path>")`
- codegraph: `mcp__codegraph__codegraph_index(project="<project-name>")`

Indexing is done once at project start. Re-index only on git pull or branch switch.

### Platform Layer (design context + version history)

| Tool | Scope | Signature Capability |
|------|-------|---------------------|
| GitHub MCP | GitHub repos | search_issues, issue_read, pull_request_read, search_code |
| Git MCP | local repo | git_log, git_diff, git_blame, git_show |

### Specialized Tools

| Tool | Scope |
|------|-------|
| Code Analysis Java/Spring | Spring pattern detection, architecture analysis |

---

## Per-Pass Tool Allocation

### Pass 0: Pre-Code Reading

| Step | Tool | Action |
|------|------|--------|
| Design docs | GitHub MCP | `search_issues` with domain keywords — find design discussions |
| PRs | GitHub MCP | `pull_request_read` for major refactors — read reviewer comments |
| Release notes | GitHub MCP | `get_latest_release` + read release body |
| Historical version | Git MCP | `git_checkout` to the tag closest to initial design |

**Rule**: Use GitHub MCP first. Only fall back to `git log` when the repo has no GitHub presence.

**No-GitHub fallback**:
| GitHub MCP Action | Git Fallback |
|-------------------|-------------|
| search_issues | `git log --grep="fix\|bug\|refactor\|issue"` — search commit messages for design-related terms |
| pull_request_read | `git log --merges` — read merge commit messages |
| get_latest_release | `git tag --sort=version:refname \| tail -5` — read tag messages |

### Pass 1: Scan Outline

| Step | Java | C/C++ |
|------|------|-------|
| Inheritance tree | JavaLens `get_type_hierarchy` | LSP MCP `type_hierarchy` |
| Call graph | codegraph `callers` + `callees` | codegraph `callers` + `callees` |
| DI/endpoints (Spring) | A/B only: JavaLens `get_di_registrations` + `get_http_endpoints` | N/A |
| Code search | codebase-memory-mcp `search_code` | codebase-memory-mcp `search_code` |

**For approach D (Modified Pass 1)**: Use inheritance tree and code search tools only. Skip DI/endpoint scanning and flagged question generation.

**For approach C**: Full Pass 1 (all tools), then abbreviate Pass 2 to 1-2 questions.

**Fallback for Java**: JavaLens → codegraph → grep. For C/C++: LSP MCP → codegraph → grep.

### Pass 2: Deep-Dive

| Step | Java | C/C++ |
|------|------|-------|
| Trace execution path | JavaLens `analyze_data_flow` | LSP MCP `call_hierarchy` |
| Trace call chain | codebase-memory-mcp `trace_path` | codebase-memory-mcp `trace_path` |
| Temporal tracing | **[A only]** Git MCP `git_log` + `git_diff` between early and current tag | **[A only]** Same |
| Change impact | JavaLens `analyze_change_impact` | codebase-memory-mcp `query_graph` |
| Authorship | Git MCP `git_blame` | Same |
| Go-to-definition | codegraph `search` → Read tool | LSP MCP `go_to_definition` |

**Rule**: Use semantic tools (JavaLens/LSP) for data flow and impact analysis. Use Git MCP for version history. Never use grep for execution path tracing — it misses indirect calls and polymorphic dispatch. For Java go-to-definition without jls, use codegraph `search` to locate the target class, then Read the file directly.

### Pass 3: Synthesize

| Step | Java | C/C++ |
|------|------|-------|
| Cross-domain references | codebase-memory-mcp `query_graph` | Same |
| Complexity metrics | **[A only]** JavaLens `get_complexity_metrics` | N/A |
| Circular dependencies | **[A only]** JavaLens `find_circular_dependencies` | **[A only]** codebase-memory-mcp `query_graph` |
| Architecture overview | **[A/B only]** Code Analysis `ArchitectureAnalyzer` (Spring) | **[A/B only]** codebase-memory-mcp `get_architecture` |

---

## Fallback Chain

### Java
```
JavaLens → codegraph → codebase-memory-mcp → grep
```
Note: LSP MCP is skipped for Java — jls (Java Language Server) is not installed.

### C/C++
```
LSP MCP (clangd) → codegraph → codebase-memory-mcp → grep
```

**Never skip levels.** If higher-tier tool fails, use the next tier. Do NOT skip to grep unless all semantic and index tools are unavailable for the specific query.

If a specific capability (e.g., type hierarchy) is unavailable at any tier, use next-tier structural approximations (e.g., class declaration grep for inheritance) or mark the output as `[PARTIAL: type hierarchy unavailable]`.

---

## Anti-Patterns

### 1. grep for structural queries

```
WRONG: grep -rn "implements ChannelHandler"  → misses anonymous classes, indirect implementations
RIGHT: JavaLens find_implementations("ChannelHandler") → compiler-accurate
```

### 2. grep for call chains

```
WRONG: grep "methodName" → finds text matches, not actual calls
RIGHT: codegraph callers → graph-based call resolution
```

### 3. Reading source without design context

```
WRONG: Start reading source immediately without Pass 0
RIGHT: GitHub MCP search_issues → understand WHY this code was written → then read source
```

### 4. Using JavaLens for C/C++ or LSP MCP for Java

```
WRONG: JavaLens on Redis codebase → JavaLens only supports Java
WRONG: LSP MCP for Java analysis → jls (Java Language Server) not installed
RIGHT: LSP MCP + clangd for Redis (C code); JavaLens → codegraph for Java
```

---

## Pre-Domain Tool Check (MANDATORY)

**This block MUST be output before ANY source query. It is the enforcement mechanism for the fallback chain — if you output a Tool Availability block, you cannot later use grep for call chains.**

Before starting analysis, output this block:

```
## Tool Availability: {domain name}

Language: {Java/C/C++}
Semantic tool: {JavaLens / LSP MCP+clangd}
Index tool: {codegraph (primary) / codebase-memory-mcp}
Pass 0 ready: GitHub MCP {yes/no — requires GITHUB_PERSONAL_ACCESS_TOKEN}
Temporal tracing: Git MCP {yes/no}
Fallback chain: {JavaLens→codegraph→grep / LSP+clangd→codegraph→grep}
Index status: {fresh / stale — re-index if repo was updated since last index}
Usage plan per pass: {Pass1: codegraph callers N + LSP type_hierarchy N / Pass2: codebase-memory trace_path N / Pass3: codegraph query_graph N}
**Actual usage (fill AFTER analysis):** codegraph {N} queries, LSP {N} queries, codebase-memory {N} queries, grep {N} verifications. **If grep count > semantic tool count, tool chain was violated.**
```

**Violation check**: After analysis completes, verify: was `grep` used for ANY structural query (callers/callees/type hierarchy)? If yes, the fallback chain was violated. Re-run with the correct semantic tool.
