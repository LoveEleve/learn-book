# q52 — V2 会话小文件 + State 完整 + Reference(深度版:收尾)

> 域:④知识库(会话面)+ 支撑 | 文件:core/src/session/(info 50/error 24/todo 78+/message 2/schema 9/prompt 1/event 2)+ state.ts(128)+ reference.ts(+)
> review 轮次:2 轮(源码全文)

---

## 假设

会话小文件 = Facade 的支撑件:info(fromRow 行→Info 映射)、todo(全量替换事务)、error(解码错误族)。message/schema/prompt/event 是 schema 的 re-export 门面。Reference = 命名引用(本地/git 源)的 State 化管理。

## 验证

### 1. Info 映射(设计 1:fromRow)

```ts
// info.ts:fromRow:session 行 → SessionSchema.Info
// 字段:id/projectID/title/parentID/agent/model/cost/tokens/location/subpath/revert/time{created,updated,archived}
// ——Facade 的 get/list 输出形状(q10)
```

### 2. Todo(设计 2:全量替换事务)

```ts
// todo.ts:update:事务内 delete 全部 + insert 新列表(position 排序)
// 语义:todo 是全量同步(todowrite 工具每次写完整清单)——无增量
// Event:todo 事件发布(EventV2)
```

### 3. 错误族(设计 3:解码错误)

```ts
// error.ts:MessageDecodeError(消息解码失败——存储损坏检测)+ ContextSnapshotDecodeError(epoch 快照损坏)
// 用途:读取路径的显式错误契约(不静默)
```

### 4. 门面文件(设计 4:re-export)

```ts
// message.ts/schema.ts/prompt.ts/event.ts:core 侧 re-export schema 门面
// ——core 消费方只 import core 路径,不直接依赖 schema 内部
```

### 5. State 完整(设计 5:transform/reload 细节)

```ts
// state.ts(128):State.create({initial, draft, finalize})
// 前文(q27)已读:transform 注册 + reload 重放 + batch 合并
// 补充:Registration{dispose}(Scope 移除)+ Transformable{transform, reload}
// ——agent/command/reference 等"可编辑配置"的统一状态机
```

### 6. Reference(设计 6:命名引用)

```ts
// reference.ts:Source = LocalSource | GitSource(本地目录/git 仓库)
// State 化(transform 可增删)+ materialized 缓存(仓库克隆物化)
// guidance 用(q2:core/reference-guidance)+ RepositoryCache 支撑
// ——产品①的 @引用(命名外部上下文)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Info 映射(行→模型) | info.ts | ④读取形状 |
| 2 | Todo 全量替换事务 | todo.ts | ②清单同步 |
| 3 | 解码错误显式契约 | error.ts | ④损坏检测 |
| 4 | schema re-export 门面 | message/schema/prompt/event.ts | ②依赖方向 |
| 5 | State 完整(transform/reload/batch) | state.ts | ②配置状态机 |
| 6 | Reference(本地/git 命名引用) | reference.ts | ①@引用 |

## 面试弹药

- "todo 全量同步":每次写完整清单(无增量协议)——简单性优先
- "解码错误不静默":MessageDecodeError 显式抛出——存储损坏可诊断
- "re-export 门面":core 消费方不直接 import schema 内部——依赖边界

## 待深挖

- [ ] reference 的 git 克隆物化细节
- [ ] state 的 batch 语义边界
