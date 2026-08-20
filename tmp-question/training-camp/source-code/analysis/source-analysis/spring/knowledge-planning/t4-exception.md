# T-4 DataAccessException 异常翻译 — SQLException→DataAccessException 映射体系

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | SQLExceptionTranslator + SQLErrorCodeSQLExceptionTranslator + SQLExceptionSubclassTranslator
> 基线: T-1 链路 — @Repository 自动翻译 JPA/Hibernate 异常 → 依赖于本域

---

## §0.8

- 🟡 Working，1篇 — SQLExceptionTranslator接口 + AbstractFallbackSQLExceptionTranslator回退链 + SQLErrorCodeSQLExceptionTranslator(error code映射) + SQLExceptionSubclassTranslator(子类映射)
- 设计模式: [模式: 责任链]—自定义+error code+SQLState+子类四层回退异常翻译

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SQLExceptionTranslator.java:56 | translate() | **接口**: `translate(String task, String sql, SQLException ex)→DataAccessException` | High |
| AbstractFallbackSQLExceptionTranslator.java:95 | translate() | **回退链**: customTranslator→doTranslate→SQLState fallback→SQLExceptionSubclass fallback→DataAccessException(兜底) | High |
| SQLErrorCodeSQLExceptionTranslator.java:75 | doTranslate() | **error code映射**: sql-error-codes.xml→MySQL error 1062(Duplicate entry)→DuplicateKeyException / error 1213(Deadlock)→DeadlockLoserDataAccessException | High |
| SQLExceptionSubclassTranslator.java:67 | doTranslate() | **子类映射**: SQLTransientException→TransientDataAccessException / SQLNonTransientException→NonTransientDataAccessException→子类细分 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 异常翻译是单一职责的简单机制——核心是回退链(custom→error code→SQLState→subclass)+error code映射表。1篇(~35行)覆盖接口→回退链→映射表。

**P1 核心** 🔴: AbstractFallbackSQLTranslator 三层回退链 — **为什么** 🔴** 🔴: 异常翻译保障"数据库无关性"—同一语义错误(重复键)在MySQL(1062)/PostgreSQL(23505)/Oracle(ORA-00001)有不同error code—sql-error-codes.xml为每个数据库提供统一映射→DataAccessException子类

**映射示例**: MySQL 1062→DuplicateKeyException / 1213→DeadlockLoserDataAccessException / 1205→CannotAcquireLockException / 1146→BadSqlGrammarException
