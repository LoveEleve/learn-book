# S2-4 MessageSource 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | getMessageInternal 为什么是 protected 而非 private？子类覆写后会发生什么？ | §1 |
| 2 | resolveCode 和 resolveCodeWithoutArguments 为什么要分成两个抽象方法？ | §1 |
| 3 | ResourceBundleMessageSource 的多个 basename 是"合并"还是"按序查找"？ | §3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 4 | getMessageFromParent 为什么 instanceof AbstractMessageSource 要走内部方法？这会不会破坏开闭原则？ | §2 |
| 5 | DelegatingMessageSource 在 refresh() Step 7 被默认创建 — 为什么不直接 new ResourceBundleMessageSource？ | §2-3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | ResourceBundle 是什么？Spring 的 MessageSource 封装了它 — 和直接用 ResourceBundle 有什么不同？ | §3 |

## 覆盖: 6 问 / 3 身份 / 100%
