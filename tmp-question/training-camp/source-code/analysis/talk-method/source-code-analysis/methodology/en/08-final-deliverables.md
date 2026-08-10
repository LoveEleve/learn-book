# Appendix: Final Deliverables for Source-Code Analysis Projects

> This appendix defines the complete file structure after the full methodology pipeline. Not "what should exist" — "what MUST exist."

## Final Output Directory Structure

```
{project_root}/docs/{project_name}/
│
├── planning/                          # ← Planning phase output (must all exist at outline completion)
│   ├── 00-domain-list.md              # Domain discovery — full domain inventory (🔴/🟡/scheme/topology)
│   ├── 01-book-plan.md                # Book-level plan — volume structure/narrative flow
│   ├── 02-approach-selection.md       # Approach selection — A/B/C/D decisions
│   │
│   ├── knowledge-planning/            # ← Knowledge planning (per-domain output)
│   │   ├── vol-01/                    # One sub-directory per volume
│   │   │   ├── 01-OS-kernel.md        # Domain knowledge planning file
│   │   │   │   ├── 01 Per-source extraction  # source file → KP table
│   │   │   │   ├── 01 Aggregation           # P1/P2/P3 tiered table
│   │   │   │   ├── 02 Depth classification   # 🔴/🟡/🟢 table
│   │   │   │   └── 03 Clustering            # mechanism boundaries + dependency graph + teaching order
│   │   │   └── ...
│   │   └── ...
│   │
│   ├── outlines/                      # ← Per-Article Outline (produced from knowledge planning)
│   │   ├── vol-01/
│   │   │   ├── 01-os-abstraction.md   # Domain outline — TOC format (numbered layers + source anchors)
│   │   │   ├── 01-os-abstraction-pass1.md  # Methodology materials (Pass 1 exploration)
│   │   │   └── ...
│   │   └── ...
│   │
│   └── HANDOFF.md                     # ← Handoff document (§0 full methodology + startup commands)
│
├── articles/                          # ← Writing phase output (Pass 3)
│   ├── vol-01/
│   │   ├── 01-os-abstraction.md       # Complete article (written from outline)
│   │   └── ...
│   └── ...
│
└── harness/                           # ← Minimal reproduction (🔴A domains, optional)
    └── g1-gc/
        └── region-model.c             # ~200-300 line core mechanism verification
```

## Required Files at Each Phase Completion

### Planning Phase Complete (before handoff to writing AI)

| File | Content | Format Requirements |
|------|------|------|
| `00-domain-list.md` | Full domain inventory | domain#/path/design decisions/interview/prod/Hub/confidence/dependency graph/topology |
| `01-book-plan.md` | Volume structure | domain count per volume/🔴A/🟡B/core questions/narrative links |
| `02-approach-selection.md` | Approach selection | per-domain A/B/C/D scheme + reasoning |

Per-domain output:

| File | Content | Format Requirements |
|------|------|------|
| `knowledge-planning/{volume}/{domain}.md` | Knowledge planning | 01 per-source extraction table + 01 aggregation(P1/P2/P3) + 02 depth classification(🔴🟡🟢) + 03 clustering(dependency graph+teaching order) |
| `outlines/{domain}/0N-{title}.md` | Per-Article Outline | TOC format + teaching narrative design (scenario opening→What→Why→How→summary closure) + cross-layer annotations [C++:/Kernel:/x86:] + source anchors + core suspense + OUTBOUND bridge |
| `outlines/{volume}/{domain}-pass1.md` | Pass 1 exploration notes | inheritance tree/element decomposition/≥5 flagged questions |
| Mega-domain extra: `outlines/{volume}/{domain}/01-{title}.md` ~ `{N}-{title}.md` | Per-article independent outlines | Each: TOC format + independent core suspense + independent estimate |

Global:

| File | Content |
|------|------|
| `HANDOFF.md` | Complete methodology + startup commands + completed domain list + domains needing rework |

### Writing Phase Complete (final delivery)

| File | Content |
|------|------|
| `articles/{volume}/{domain}.md` | Complete article — written from outline, natural narrative, no AI template markers |
| `articles/{volume}/{domain}/01-{title}.md` ~ `{N}-{title}.md` | Mega-domain per-article complete writing |

## OpenJDK Project's Current Gaps

Mapped against the above standard, OpenJDK project planning phase completion:

| File | Exists? | Gap |
|------|:--:|------|
| `00-domain-list.md` | ✅ | — |
| `01-book-plan.md` | ✅ | — |
| `02-approach-selection.md` | ✅ | — |
| `knowledge-planning/**/*.md` | ❌ | **Completely missing** — never did per-source extraction/aggregation/classification/clustering |
| `outlines/*/*.md` | ⚠️ Partial | vol-01~03 mostly early thin domains, format is narrative prose not TOC |
| `outlines/*/*-pass1.md` | ⚠️ Partial | vol-01~03 completely missing, vol-04/06 have partial |
| `HANDOFF.md` | ✅ | v13 methodology fully inlined |

**Conclusion**: The `knowledge-planning/` directory is completely empty — this is the largest structural gap in the OpenJDK project.
