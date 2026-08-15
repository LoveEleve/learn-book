# q15 — 支撑域收尾(深度版:Context/Storage/Spill/Session-Query 等)

> 域:支撑 | 文件:packages/(context/agent-instructions + session-reference)+(storage/storage)+(spill/spill)+ session-query/(session-query + tool-session-query)+ attachment + code-runtime + feedback + plan + preset
> review 轮次:2 轮(源码结构 + 核心接口)

---

## 假设

支撑域补齐:Context 注入(agent-instructions = 文件指令投影;session-reference = 会话引用)、Storage(后端注册表 + KV 面)、Spill(溢出)、Session-Query(会话检索 + 工具)、其他小域。

## 验证

### 1. Agent-Instructions(设计 1:文件指令投影)

```ts
// context/agent-instructions/src/index.ts:80-130:
apply(ctx, config):resolveConfig + 版本缓存 + 基线准备
compose(agent, signal, claimed, pending, touchedPaths):从文件系统指令组装 user 消息
  maxBytes <= 0 → undefined(禁用)
  fileSystem = ctx.get('fs')(可选服务)
  authorityMessages = [...claimed];cwd = agent.session.header.cwd ?? process.cwd()
  findProjectRoot(cwd, markers, fs, signal)——项目根发现
  baseline identity 比较:visibleBaseline.baselineIdentity === identity → keepVisibleBaseline
// 提交边界:"Execution ancestry and the enclosing durable step are the two commit
//   boundaries before an asynchronous projection may mutate the agent inbox"
//   openSteps/stepTouches:步骤级触摸跟踪
// 事件监听器不被 await,每个投影必须对着同一 agent 更早文件结果产生的 inbox 组合
//   → projectionTails WeakMap(串行化)
```

**设计要点**:指令注入是"投影"不是"注入即发"——异步投影必须过两个提交边界(执行谱系 + 封闭 durable 步骤)才能改 agent inbox。

### 2. Session-Reference(设计 2:会话引用)

```ts
// context/session-reference:config/uri/serialization/projection/types
// ——会话引用 URI + 序列化 + 投影(跨会话引用)
```

### 3. Storage(设计 3:后端注册表 + KV)

```ts
// storage/storage:BackendRegistry + StorageError{StorageErrorCode} + StorageBackend
//   KvFacet/KvUnit/KvUnitDescriptor + UNIT_NAME_RE(^[a-z][a-z0-9_]*$)
// storageBackendServiceKey(name):后端服务键
// "Service packages default-export their service class and nothing else"(packages/AGENTS.md 规则)
```

### 4. Spill(设计 4:溢出)

```ts
// spill/spill:溢出处理(index/types/invariant)——code-dispatch-log 的 spill 政策(预览+定位符,q3)
```

### 5. Session-Query(设计 5:会话检索)

```ts
// session-query:corpus/cursor/documents/extraction/filters/sources/tracing(语料/游标/提取/过滤)
// tool-session-query:operations/presentation/service-boundary/workspace-access(模型工具)
```

### 6. 其他(设计 6:小域)

```ts
// attachment:attachment 能力(brand/error)
// code-runtime:代码运行时(worker-thread 变体)
// feedback:消息反馈;plan:plan mode 日志状态;preset:per-session 组合
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 指令投影(两提交边界 + 串行化) | agent-instructions | ①指令注入 |
| 2 | Storage 后端注册表 + KV | storage/storage | ④存储抽象 |
| 3 | Spill 溢出 | spill/spill | ②输出边界 |
| 4 | Session-Query 检索 | session-query | ④检索 |
| 5 | 小域(attachment/code-runtime/feedback) | 各包 | ②支撑 |

## 面试弹药

- "异步投影的两提交边界":执行谱系 + 封闭 durable 步骤——投影不能乱改 inbox(与事件溯源的提交语义一致)
- "storage 后端注册表":BackendRegistry + 服务键——存储后端可插拔
- "指令 = 投影不是注入":compose 对着"该 agent 更早文件结果产生的 inbox"组合——串行化保证

## 待深挖

- [ ] session-query 的语料/提取实现
- [ ] spill 的溢出策略细节
- [ ] code-runtime 的 worker-thread 语义
