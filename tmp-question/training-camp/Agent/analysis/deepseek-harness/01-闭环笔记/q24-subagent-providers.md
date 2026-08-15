# q24 — Subagent Providers(深度版:7 提供者光谱)

> 域:②执行引擎(子任务) | 文件:packages/subagent/(subagent-acp 587/subagent-claude-code 604/subagent-codex 705/subagent-dsh-sdk 375/subagent-fork-in-process 124/subagent-in-process-driver 405/subagent-spawn-in-process 94)+ subagent/subagent/src
> review 轮次:2 轮(源码结构 + 能力面)

---

## 假设

Subagent = 能力缝:7 提供者从"进程内最便宜"到"委托外部产品",共享 SubagentProvider 接口(capabilities + inheritsParentContext + start)。能力声明(CO 声明)决定协议面。

## 验证

### 1. 提供者光谱(设计 1:7 档)

| Provider | 行数 | 能力 | 父上下文 | 本质 |
|---------|:--:|------|:--:|------|
| spawn-in-process | 94 | outputSchema/depthLimit/toolFilter/persona | 否 | 同 context 新鲜子 Agent(零父上下文) |
| fork-in-process | 124 | — | ? | 进程内 fork |
| in-process-driver | 405 | 共享驱动(造 id/戳 cwd/lineage/depth) | 否 | 一次性运行驱动 |
| dsh-sdk | 375 | **NO_START_CAPABILITIES** | 否 | 进程外 JSON-RPC(父强制能力;唯一读的东西) |
| acp | 587 | — | 否 | 委托 ACP 自动化服务器 |
| claude-code | 604 | **NO_START_CAPABILITIES** | 否 | 委托 Claude Code(hooks 桥) |
| codex | 705 | — | 否 | 委托 Codex(hooks 桥) |

### 2. 能力声明(设计 2:capabilities 契约)

```ts
// spawn-in-process/src/index.ts:46-54:
capabilities = { outputSchema: true, depthLimit: true, toolFilter: true, persona: true }
  ——spawn 构造子 agent,可强制递归上限 + 作用域结构运行时 + restrict() + 作用域遮蔽 persona
inheritsParentContext = false("a spawned child starts fresh — it never sees the parent conversation")
// dsh-sdk:NO_START_CAPABILITIES——进程外无法父强制(只读一件事)
// claude-code:NO_START_CAPABILITIES——外部工具不认 dsh 的能力
```

**产品启示**:②章节子任务 = 从"进程内新鲜子 agent"(零父上下文,能力全开)到"委托外部"(无能力声明)——**能力声明决定协议面**(CO 能做多少由能力表决定)。

### 3. 共享驱动(设计 3:in-process-driver)

```ts
// subagent-in-process-driver/src/index.ts:68:
InProcessRunOptions:共享驱动选项
"the shared driver mints ids,stamps cwd/lineage/depth,drives the one-shot
 (including the structured capture when the child supports outputSchema)"
// ——spawn/fork 复用同一驱动(造 id/戳迹/一次性驱动)
```

### 4. 委托外部(设计 4:claude-code/dsh-sdk)

```ts
// claude-code:inject = ['subagents', 'subprocess']——经 subprocess 执行 claude 命令
// dsh-sdk:inject = ['subagents']——JSON-RPC 连接(dsh 自身 sdk 客户端)
//   "parent-enforced start capabilities;the ONE thing it reads off"
// acp:自动化 ACP 服务器委托
// codex(705):最大的委托实现
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 7 档光谱(同 context → 委托外部) | subagent/* | ②子任务架构 |
| 2 | capabilities 声明契约 | 各 provider | ②能力协商 |
| 3 | 共享驱动(造 id/戳迹/一次性) | in-process-driver | ②子任务执行 |
| 4 | 委托外部(hooks/JSON-RPC/ACP) | claude-code/dsh-sdk/acp | ②外部集成 |

## 面试弹药

- "能力声明决定协议面":spawn 全开(outputSchema/depthLimit)vs 外部 NO_START_CAPABILITIES——父能强制多少由能力表定
- "子 agent 零父上下文":spawn 子 agent 永远不见父对话——隔离是契约
- "共享驱动复用":spawn/fork 共用一个驱动(造 id/戳 cwd/lineage/depth)——实现不重复
- "7 档光谱":从 94 行(spawn)到 705 行(codex)——复杂度对应委托深度

## 待深挖

- [ ] in-process-driver 的完整驱动逻辑
- [ ] claude-code 的 hooks 集成细节
- [ ] codex 的委托协议
