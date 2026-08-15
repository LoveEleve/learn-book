# q48 — 事件清单全貌(深度版:三层分类 + 全事件集合)

> 域:④知识库(事件面) | 文件:schema/src/event-manifest.ts(84)+ core/src/public-event-manifest.ts(7)+ durable-event-manifest.ts(15)+ session-event.ts(521,已读)+ v1/session.ts(事件部分)
> review 轮次:2 轮(源码全文)

---

## 假设

事件清单 = 三层分类:foundation(基础域)/feature(功能域)/sessionV1Live + 扩展域(安装/LSP/TUI/MCP/Workspace 等)。ServerDefinitions(公开 API 面)vs 全量 Definitions(含 V1-only)。

## 验证

### 1. 三层分类(设计 1:foundation/feature/扩展)

```ts
// event-manifest.ts:32-61
foundation = ModelsDev + Integration + Catalog + core(sessionV1Durable + SessionEvent.Definitions)
  ——基础域(模型目录/集成/会话核心)
feature = FileSystem + Reference + Permission + Plugin + ProjectDirectories + FileSystemWatcher + Pty + Question
  ——功能域(工具/权限/插件/项目)
ServerDefinitions = foundation + feature + SessionTodo(公开 API 面)
  ——Client/SDK 只暴露这些
```

### 2. 全量 vs 公开(设计 2:Definitions vs ServerDefinitions)

```ts
// event-manifest.ts:63-82
Definitions(全量)= foundation + sessionV1Live + InstallationEvent + feature + SessionTodo
  + LspEvent + PermissionV1 + TuiEvent + McpEvent + LegacyEvent + Project
  + SessionStatusEvent + QuestionV1 + SessionCompactionEvent + VcsEvent + WorkspaceEvent + WorktreeEvent + ServerEvent
// 语义:ServerDefinitions = 公开契约(跨客户端);Definitions = 全内部 + V1-only
// durable-event-manifest.ts:SessionDurable(session 聚合) + Durable(全 durable 定义)
// public-event-manifest.ts:ServerDefinitions → Latest(公开事件面)
```

**产品启示**:④知识库的"公开事件 vs 内部事件"分层——外部读者只承诺公开面,V1 事件隔离在内部。

### 3. 事件域分布(设计 3:按域统计)

| 域 | 事件类型数(约) | durable |
|----|:--:|:--:|
| SessionV1(legacy) | ~15(含 durable 部分) | 部分 |
| SessionEvent(V2) | 33 | 28 |
| ModelsDev/Integration/Catalog | 各若干 | ? |
| FileSystem/Reference/Permission/Plugin | 各若干 | ? |
| Installation/LSP/TUI/MCP/Legacy | 各若干 | live 为主 |
| Project/Workspace/Worktree/Vcs | 各若干 | ? |

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三层分类(foundation/feature/扩展) | event-manifest.ts:32-61 | ④事件组织 |
| 2 | 公开 vs 全量(ServerDefinitions/Definitions) | event-manifest.ts:57-82 | ④契约分层 |
| 3 | Durable manifest(session 聚合 + 全 durable) | durable-event-manifest.ts | ④重放面 |
| 4 | V1-only 事件隔离(LegacyEvent/TuiEvent 等) | event-manifest.ts:63-82 | ④兼容层 |

## 面试弹药

- "公开事件面 = 承诺":ServerDefinitions 是 Client/SDK 能看到的全部——内部事件(V1/TUI)不承诺
- "foundation 是共享地基":ModelsDev/Integration/Catalog 被所有 feature 依赖
- "durable 单独成清单":重放只读 Durable manifest——live 事件不进重放面

## 待深挖

- [ ] 各域事件定义(Integration/Permission/Plugin 的事件)
- [ ] legacy-event.ts(旧事件)
- [ ] 事件版本演进策略(schema-changelog)
