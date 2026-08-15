# q22 — Database(深度版:bun/node 双实现 + 迁移日志)

> 域:存储 | 文件:core/src/database/(sqlite.bun.ts 183/sqlite.node.ts 178/migration.ts 60+/migration.gen.ts/schema.gen.ts 274/path.ts)+ effect-drizzle-sqlite/
> review 轮次:2 轮(源码全文)

---

## 假设

存储层是"双运行时适配":同一 Effect SqlClient 抽象下,bun:sqlite 和 node:sqlite 两实现,运行时可切换。迁移 = 自管日志表(migration)+ TypeScript 迁移(不依赖 Drizzle journal)。

## 验证

### 1. 双实现(设计 1:同一接口两运行时)

```ts
// sqlite.node.ts:47-145 / sqlite.bun.ts(同构)
make(config):
  native = Sqlite.Native(DatabaseSync / bun Database)
  compiler = Statement.makeCompilerSqlite
  connection:execute/executeRaw/executeValues/executeUnprepared/executeStream(未实现)/loadExtension
  semaphore(1) + transactionAcquirer(uninterruptibleMask + Scope finalizer 释放)
  Client.make({ acquirer, compiler, transactionAcquirer, spanAttributes: [{db.system.name: "sqlite"}] })
// 差异:bun 版 extra export()(数据库导出);node 版 setReadBigInts/setReturnArrays
// nativeLayer:enableForeignKeyConstraints: true + PRAGMA journal_mode = WAL(默认)
```

**设计要点**:WAL 模式默认开启(并发读写);外键约束强制;连接经 Semaphore(1) 串行(单写者模式)。

### 2. 迁移(设计 2:自管日志 + 播种)

```ts
// migration.ts:20-38 apply:
1. 查 sqlite_master 表
2. 已有 session 表 → applyOnly(增量迁移)
3. 非空且无 session 表 → die("Database is not empty and has no session table")(拒绝初始化)
4. 空库 → 事务:创建全部 schema + migration 表 + 记录全部迁移(种子)
// applyOnly(migration.ts:40-72):
1. CREATE TABLE IF NOT EXISTS migration
2. 无记录但存在 __drizzle_migrations(Drizzle 旧日志)→ 播种迁移(INSERT OR IGNORE SELECT name)
   —— "Seed the new journal once so TypeScript migrations don't replay old SQL"
3. 逐迁移:未完成 → 事务(up + INSERT migration)
// 并发保护:Semaphore(1)(多进程迁移互斥)
```

### 3. schema 生成(schema.gen.ts 274 行)

- Drizzle schema.sql.ts 定义(snake_case + Timestamps 组合)
- schema.gen.ts 为生成产物(含全部 CREATE TABLE + 索引)

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双运行时实现(同接口) | sqlite.bun/node.ts | ②运行时可移植 |
| 2 | WAL + 外键 + Semaphore 串行 | sqlite.node.ts:147-161 | ④存储一致性 |
| 3 | 自管迁移日志 + 旧日志播种 | migration.ts:20-72 | ④演进兼容 |
| 4 | 非空无 session 拒绝初始化 | migration.ts:32 | ④防误初始化 |

## 面试弹药

- "双运行时适配":同一 Effect SqlClient,bun/node 可切换——运行时选择不渗透业务层
- "迁移日志自管":TypeScript 迁移 + migration 表;旧 Drizzle 日志播种一次——不用 Drizzle journal 依赖
- "WAL + 单写者 semaphore":读并发 + 写串行——SQLite 的经典正确组合
- "非空库无 session → die":拒绝在别人的数据上建表——防误初始化

## 待深挖

- [ ] effect-drizzle-sqlite 包的封装细节
- [ ] database/path.ts(目录列规范化)
