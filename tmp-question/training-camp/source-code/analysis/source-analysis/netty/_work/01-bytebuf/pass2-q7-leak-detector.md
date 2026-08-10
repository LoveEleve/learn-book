## Loop Note: Q7 — ResourceLeakDetector 泄漏检测

**Hypothesis**: ResourceLeakDetector 跟踪 ByteBuf 的分配栈，在 GC 时检测未 release 的 buf（泄漏），分 4 级控制性能开销。

**Verification** (ResourceLeakDetector.java:44-92):
- 默认级别: `Level.SIMPLE` (line 46)，系统属性 `io.netty.leakDetectionLevel` (line 44)
- `Level.DISABLED` (line 69) — 关闭检测，零开销
- `Level.SIMPLE` (line 74) — ~1% 采样，检测到泄漏时报告（默认）
- `Level.ADVANCED` (line 79) — 采样率与 SIMPLE 相同，但每个泄漏都记录
- `Level.PARANOID` (line 84) — 100% 检测（性能开销大，仅开发调试用）

**Code type**: Implementation

**设计权衡**:
| 级别 | 开销 | 场景 |
|------|:--:|------|
| DISABLED | 0% | 生产环境确认无泄漏后 |
| SIMPLE | ~1% 采样 | 生产默认——概率性检测平衡开销与覆盖 |
| ADVANCED | ~1% 采样 | 排查泄漏——记录完整栈 |
| PARANOID | 100% | 开发调试——确认无泄漏 |

**结论**: ResourceLeakDetector 是引用计数的安全网——引用计数保证确定性释放，泄漏检测报告"本该释放但未释放"的对象。Heap 和 Direct 都检测，但对 Direct 更重要（GC 不回收堆外内存）。source: ResourceLeakDetector.java:44-92
