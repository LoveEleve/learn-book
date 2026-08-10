# Quick Reference Card

> Role: Handy extraction of commonly used commands from methodology/01-05. Read as a quick lookup during analysis — not a replacement for the full SOPs.
> Source: methodology/en/01-05

---

## Pass 0: Pre-Code Reading

```bash
# Design docs
Read README.md
find docs/ -name "*.md" | head -5

# Issues/PRs (design discussions)
git log --oneline -30
git log --oneline --grep="refactor|design|architecture|rewrite"

# Release notes
ls CHANGELOG* RELEASE* 2>/dev/null
# If found, Read the most recent one

# Test directory as feature map
find src/test -type d | sort | head -20
```

**MCP**: GitHub MCP `search_issues` → if no GitHub, `git log --grep="fix|bug|refactor|issue"`

---

## Pass 1: Scan Outline

```bash
cd /data/workspace/source-code/code/spring/{repo}/

# Package structure
find src/main/java -type d | sort | head -30

# Core class declarations
grep -rn "^public (class|interface|enum) " src/main/java/{pkg} | head -20

# Hottest files (git log trick)
git log --pretty=format: --name-only | sort | uniq -c | sort -rn | head -10

# Read 2-3 test files
find src/test/java -name "*Test*.java" | head -5
```

**MCP** (Java): JavaLens `get_type_hierarchy` / codegraph `callers` + `callees`
**MCP** (C/C++): LSP MCP `type_hierarchy` / codegraph `callers` + `callees`

### Pass 1 Output Template
```
## Pass 1 Output: {domain name}

### Inheritance Tree / Call Graph
[ASCII art]

### Basic Element Decomposition
1. Element A: [one sentence] — source {file:line}
2. Element B: [one sentence] — source {file:line}
...

### Flagged Questions (≥5)
1. [specific question]
2. ...
```

---

## Pass 2: Deep-Dive

### Per-Question Loop
```
1. Read source location → 2. Hypothesis → 3. Verify (grep ≥2 levels) → 4. Code type → 5. Conclusion
```

### Trace Execution Path
```bash
# Call chain tracing
grep -rn "methodName(" src/main/java/{pkg} | head -20

# Temporal tracing (A approach only, 🔴 domains)
git tag --sort=version:refname | head -20
git checkout <earliest-tag>
git diff <early-tag> <current-tag> -- {core-package}/
```

**MCP**: JavaLens `analyze_data_flow` / codebase-memory-mcp `trace_path` / Git MCP `git_blame`

### Loop Note Template
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

---

## Pass 3: Synthesize

### Article Template
```
# {Article Title}

## 1. Overview
## 2. Core Mechanism
   ### 2.1 {Element A}
   ### 2.2 {Element B}
   ### 2.3 Composition: {A+B interaction}
## 3. Design Tradeoffs
## 4. Interview Points
   ### Q1: {standard question}
   ### Q2: {trap follow-up}
## 5. Cross-References
   ← {previous domain}: {one sentence why it's a dependency}
   → {next domain}: {one sentence what depends on this}
   Also see: {related domain} — {brief description}
```

**MCP**: JavaLens `get_complexity_metrics` + `find_circular_dependencies` / codebase-memory-mcp `query_graph`

### Minimal Reproduction (A approach 🔴 only)
```
Write micro-engine in harness/{framework}/:
  No concurrency safety, no backward compatibility, no configuration
  Only the core control flow (~200-300 lines)
```

---

## Tool Fallback Chains

```
Java:    JavaLens → codegraph → codebase-memory-mcp → grep
C/C++:   LSP MCP + clangd → codegraph → codebase-memory-mcp → grep
```

---

## Cross-Domain Reference Format

```
{domain} analysis §{section}
```

Discovery propagation: Level 1 (minor → embed in current) / Level 2 (major → append correction) / Level 3 (invalid → mark re-analysis). Multi-domain (≥3) → `progress/cross-domain/{id}.md`.
