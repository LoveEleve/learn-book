# Analysis Depth Standards

> Role: AI self-constraint SOP. Must read before Pass 1 (question selection) and during Pass 2 execution.
> Executed? Output: `[03] Design decision test: N "why" answers found, N "what-only" identified for deepening`
> Prerequisite: `01-three-layer-loop.md`
> Reading order: 01 → 02 → 03 (this doc) → 04 → 05 → 06. See 01 for full execution sequence.

## Core Question

Given a code fragment, decide: how deep is deep enough?

## Decision Criterion

**Do NOT ask: "Is this low-level optimization?" — Ask: "Does this code carry a design decision?"**

### Must Deep-Dive (carries design decisions)

| Signal | Example |
|------|------|
| Data structure choice (why A over B) | SelectedSelectionKeySet uses array instead of HashSet — NIO Selector O(n) key traversal is a real bottleneck |
| Concurrency strategy (why lock A over B) | LazyIndex uses ReentrantLock not synchronized — to defer mmap until first access |
| Memory strategy (why allocate this way) | sdshdr5 never used for expansion — auto-upgrades to sdshdr8 on append, avoids frequent realloc for short strings |
| Compression/encoding choice (why compress middle) | quicklist middle nodes LZF-compressed — saves memory, ends uncompressed for head/tail performance |
| Exception handling strategy (why swallow/propagate/transform) | Seata undo_log SQLIntegrityConstraintViolationException → concurrent rollback protection |

### Can Skip (no design decision)

| Signal | Example |
|------|------|
| getter/setter/toString/hashCode | Pure data access, no decision |
| Logging, exception wrapping | `log.error("xxx", e)` — no decision |
| Pure delegation | `return target.doX()` — no decision |
| Deprecated code paths | Mark as `[deprecated]`, skip analysis |
| Test files (already read in Pass 1) | Pass 1 reads tests for intent, Pass 2 ignores |

### Gray Zone (requires judgment)

| Signal | Handling |
|------|------|
| Constant value choice (why 512 not 256) | If comment explains → design decision, deep-dive. If not → mark `[TODO: verify]`, skip by default |
| Config default values | Same logic |
| Sort/priority values | If cross-domain impact (e.g., Filter order) → design decision, deep-dive |

## Insufficient Depth Detection

After Pass 2 completes, if ANY of these are true, depth is insufficient:

- [ ] All descriptions are "what it does" with zero "why it does it this way"
- [ ] Zero cross-domain connections mentioned (any reference to how this mechanism interacts with or depends on other domains — not formal Pass 3 cross-references, just awareness of cross-domain implications during deep-dive)
- [ ] Cannot name the alternative design that was rejected
- [ ] Any loop note < 200 words
- [ ] Source references are only method signatures, no method bodies

→ **Return to Pass 2 and deepen**.

## Over-Analysis Detection

If this is true, you're wasting effort:

- [ ] Two consecutive loop notes all conclude "getter/setter, no design decision"

→ You chose the wrong questions. **Re-select flagged questions**, do not continue deep-diving the same file.
