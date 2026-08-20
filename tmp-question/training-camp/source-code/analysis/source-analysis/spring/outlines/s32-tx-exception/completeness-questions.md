# T-4 DataAccessException 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | MySQL 1062(Duplicate entry) → Spring 怎么变成 DuplicateKeyException 的？ | §1 |
| 2 | 如果数据库不在 sql-error-codes.xml 中 — 异常还能翻译吗？ | §1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | 四层回退链为什么是 custom→error code→SQLState→subclass 这个顺序？ | §1 |
| 4 | @Repository 的异常翻译和 T-1 的 @Transactional 有什么关系？ | §1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | SQLException 和 DataAccessException 的区别是什么？ | §1 |

## 覆盖: 5 问 / 3 身份 / 100%
