# q9 — Interaction(深度版:审批瀑布 + 政策切换 + 审计事件)

> 域:②执行契约(审批/权限) | 文件:packages/interaction/(user-approval 487/permission-presets 526/tool-ask-user 131/user-questions 239/commands 583)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Interaction = 审批/权限/命令/提问能力。核心 = approval/request 瀑布(allow/deny/ask)+ ApprovalPolicy(ask|never)+ durable 审计事件(asked/decided/policy)。fail-closed:无 answerer → 'unavailable'。

## 验证

### 1. 审批瀑布(设计 1:approval/request)

```ts
// user-approval/src/index.ts:30:
'approval/request'(waterfall): 审批请求决策(agent, tool identity, reason, signal)
  next() 委托;失败 → fail-closed 默认
  scope 过滤:agent-scoped 监听器只收该 agent
// 四结局(types.ts):allowed-once / rejected / cancelled / unavailable
```

### 2. 政策(设计 2:ApprovalPolicy)

```ts
// user-approval/src/index.ts:88-100:
ApprovalPolicy = 'ask' | 'never'
  'ask'(默认):委托组合 answerer;无 answerer → 链落 fail-closed 'unavailable'
  'never':永不问——每个 ask 确定性 resolve 'rejected'(严格 headless/CI 姿态;
    政策结局无需问就知道)
NEVER_SENTENCE:模型可见语句("approval prompts are disabled...do not request sandbox escalation")
```

### 3. 审计事件(设计 3:durable 日志)

```ts
// user-approval/src/index.ts:44-80(SessionEventMap 扩展):
'approval/asked': 审批问题已提出(log-only 审计,非 surface,无 surfaceOp;id 配对 decided)
  { id, toolName, callId?, reason? }
'approval/decided': 同一 id 的结果(每 ask 恰好一个:decision/cancellation/fail-closed unavailable)
'approval/policy': 会话审批政策切换(durable 可重放,绝不在模型转录)
  ——模型从 runtime-context 快照 + live 切换通知学政策
  ——LAST 事件 = 会话 override(effectiveApprovalPolicy)
  source: 'delegation' = 子 agent 播种的 override;缺省 = 运行时切换
```

**产品启示**:①对齐模块的权限预授权 = approval/policy 切换(durable,模型从快照学);③验收器的审批 = approval/request 瀑布挂点。

### 4. Permission Presets(设计 4:预设系统)

```ts
// permission-presets/src/index.ts:55-140:
PresetSpec / CUSTOM_PRESET / PERMISSION_SETTINGS_NAMESPACE(settingsNamespace('permission'))
KnobState(旋钮状态)/ PermissionSettings(设置模型)/ Config
// ——预设 = 用户可切换的权限配置(UI 旋钮)
```

### 5. 其他(设计 5:ask-user/questions/commands)

```ts
// tool-ask-user(131):向用户提问的工具
// user-questions(239):用户问题能力
// commands(583):人类命令(ctx.commands)——分派不经过模型 turn(architecture.md:115)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | approval/request 瀑布 + 四结局 | user-approval/src/index.ts:30 | ②审批 |
| 2 | ApprovalPolicy(ask|never + fail-closed) | index.ts:88-100 | ②安全默认 |
| 3 | durable 审计事件(asked/decided/policy) | index.ts:44-80 | ④审计 |
| 4 | 政策切换 LAST = override | index.ts:67-80 | ①预授权 |
| 5 | 权限预设 + 命令/提问 | permission-presets + commands | ②交互面 |

## 面试弹药

- "never 政策 = 结局可预知":严格 headless/CI 姿态——每个 ask 确定性 rejected(不问即知)
- "审计事件非 surface":approval 三事件 durable 但不进模型转录——模型从快照学政策
- "fail-closed unavailable":无 answerer → 明确不可用结局(不静默允许)
- "政策切换可重放":LAST approval/policy 事件 = 会话 override——子 agent delegation 播种

## 待深挖

- [ ] permission-presets 的旋钮模型
- [ ] commands 的分派语义(无模型 turn)
- [ ] 审批与 sandbox 的集成(sandbox_permissions)
