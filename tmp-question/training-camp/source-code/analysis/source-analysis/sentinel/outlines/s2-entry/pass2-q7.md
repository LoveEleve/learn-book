# Pass 2 闭环笔记 Q7: MAX_CONTEXT_NAME_SIZE — 上下文上限与双降级

## 初始假设
- 上下文上限与链上限(6000)是同一套机制。

## 验证过程
- 读 `Constants.java:36`: `MAX_CONTEXT_NAME_SIZE = 2000`;同文件 :37 是 MAX_SLOT_CHAIN_SIZE = 6000 — **两个独立上限, 并排声明**。
- 读 `ContextUtil.java:130-149` (trueEnter): 两处检查(本地读前 + 锁内重查):
  - 超过 2000 → `setNullContext()` (L131/L139) — contextHolder 置 NULL_CONTEXT 单例, 警告日志只打一次 (shouldWarn, L163-173)。
- 语义差异(对照 S-1 链上限):
  | | MAX_SLOT_CHAIN_SIZE=6000 | MAX_CONTEXT_NAME_SIZE=2000 |
  |---|---|---|
  | 超限表现 | 链 null → 无检查放行 | 上下文 = NULL_CONTEXT → 无检查放行 |
  | 载体 | CtSph.chainMap | ContextUtil.contextNameNodeMap |
  | 警告 | 无(静默) | 一次 WARN(RecordLog) |
  | 恢复 | COW 换新 Map 后新资源可建 | ThreadLocal 值跨调用不失效(本线程一直 NULL 直到 exit) |

## 代码类型
- Implementation(防御内存膨胀的双保险)

## 跨域关联
- S-2 → S-1: 两上限同文件同哲学(失控保护), 链上限见 S-1 Q8

## 结论
2000(上下文)与 6000(链)是**并列的两道防膨胀闸门**: 上下文超限 → NULL_CONTEXT(带一次 WARN), 链超限 → 空链(静默)。两者的资源检查都在 entry 阶段被短路(CtSph L120-124 与 L138-141)。