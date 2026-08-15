# q7 — PermissionV2(深度版:评估管线 + ask/assert + 测试契约)

> 域:②执行契约(权限)+ 安全 | 文件:core/src/permission.ts(310)+ permission/(saved.ts 79/sql.ts)+ schema/src/permission.ts + opencode/src/permission/(index 223/arity 163,V1)+ core/test/permission.test.ts(315,11 契约)
> review 轮次:2 轮(源码全文 + 全部测试契约)

---

## 假设

权限 = 规则评估(Wildcard findLast)+ 运行时询问(Deferred 异步)+ 持久化批准(permission 表)三件套。deny 永远优先;无沙箱(授权层而非隔离层)。这是产品①"边界权限/权限预授权"的蓝本。

## 验证

### 1. 规则模型(设计 1)

```ts
// schema/permission.ts
Rule = { action, resource, effect: "allow" | "deny" | "ask" }
// V2 语义化(action/resource);V1 老式(permission/pattern)
// evaluate(core/permission.ts:76-86):rulesets.flat().findLast(Wildcard 双匹配) ?? { action, resource: "*", effect: "ask" }
```

### 2. 评估管线(设计 2:三来源 + deny 优先)

```ts
// permission.ts:155-162 evaluateInput
1. configured(sessionID, agentID?):agent 权限(permission.ts:137-145)
   —— session 省略 agent → build 默认(missingAgentPermissions = [{action:"*", resource:"*", effect:"deny"}])
2. denied 检查:任一资源 deny → 立即 deny(在合并 saved 之前)
3. all = rules + savedRules();effects = 每资源 evaluate;任一 deny → deny / 任一 ask → ask / 否则 allow
```

**测试证据**:
- 171 "uses build permissions when the Session agent is omitted"
- 197 "denies omitted-agent permissions when no primary default agent exists"(build 被删 → deny,防无 agent 策略放行)
- 144 "allows and denies from explicit rules without asking"
- 156 "allows managed output reads without granting external directory access"(tool_123 允许 + external_directory /tmp/tool-output/* 拒绝——托管输出可读但不授予目录权限)

### 3. ask vs assert(设计 3:预检 vs 强制等待)

```ts
// permission.ts:190-218
ask(input) → 评估 + 仅 ask 时入 pending + 返回 { id, effect }  —— 不等待
assert(input) → 评估 → deny: BlockedError / allow: 通过 / ask: create + Deferred.await
```

**测试证据**:
- 106 "returns the evaluated effect and only queues prompts":allow/deny 不入 pending(list 空);ask 入 pending(get 有值)
- 121 "evaluates against an explicit provider-turn agent"(agent 参数覆盖 session agent)

### 4. 异步等待(设计 4:create + Deferred)

```ts
// permission.ts:176-188 create
Deferred.make + pending.set + events.publish(Event.Asked) + onError 清理
// assert:uninterruptibleMask + Deferred.await + catchTag(DeclinedError → die)
```

**测试证据**:
- 253 "resolves an asked permission once":list/forSession/get 全可见;reply "once" → 通过 + 清理
- 268 "defects when an asked permission is declined":reply "reject" → DeclinedError(die 到调用方)→ runner 中断(q3 测试 2722)

### 5. 回复语义(设计 5:reject/once/always + 级联)

```ts
// permission.ts:220-286 reply
reject: → CorrectedError(带反馈)/DeclinedError + 同 session 全部 pending 拒绝(级联)
once: → 只通过这一个
always: → saved.add(project, action, save-resources) + 同 session pending 重评(新 saved 匹配的自动通过)
```

**测试证据**:
- 286 "stores and removes saved resources for a project":reply "always" → permission 表写入 {action:"read", resource:"src/*"};saved.list/remove 可见

### 6. 持久化批准(设计 6:PermissionSaved)

```ts
// permission/saved.ts:42-73
list(projectID?) / add({projectID, action, resources}) / remove(id)
// savedRules(permission.ts:131-135):SELECT permission WHERE project_id → effect: "allow"
```

**测试证据**:
- 232 "uses saved bash approvals while preserving configured deny precedence":saved allow → ask allow;配置 deny → ask deny(saved 永远不盖过 configured deny)

### 7. 工具集成(设计 7:leaf 自断言 + 定义过滤)

- leaf 内 permission.assert(action 如 grep/bash/read,source={tool, messageID, callID})(specs/v2/tools.md:131)
- materialize 时 whole-tool 过滤(registry.ts:106-113,132-135):deny `*` 的工具从定义中删除(catalog visibility,非执行授权)
- bash 特殊(permission.test.ts:219-251):走正常配置规则语义,无沙箱(specs/v2/session.md:204)

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 规则模型(action/resource/effect,findLast) | permission.ts:76-86 | ①权限规则 schema |
| 2 | 三来源管线(agent > saved,deny 优先)+ missingAgent deny | permission.ts:131-162 | ②边界权限优先级 |
| 3 | ask(预检)/assert(强制等待)双 API | permission.ts:190-218 | ②权限预授权 |
| 4 | Deferred 异步等待 + Event.Asked | permission.ts:176-188 | ②人机交互 |
| 5 | reject/once/always + 级联 | permission.ts:220-286 | ②审批流 |
| 6 | 持久化批准表(project 级,configured deny 优先) | permission/saved.ts | ④跨 session 权限记忆 |
| 7 | leaf 自断言 + whole-tool 定义过滤 + 无沙箱 | registry.ts:106-113 + specs | ⚠️ 产品决策参考 |

## 面试弹药

- "ask 只排队,assert 强制等待":ask 预检(调用方自己决定),assert 阻塞到用户回复——工具按需选择
- "deny 永远赢":configured deny 在合并 saved 之前检查——saved allow 不可能覆盖配置 deny(测试证明)
- "无 agent = deny":session 没 agent 且无 primary 默认 → 全 deny,防策略真空(安全默认)
- "always 级联":一次批准 → 同 session 排队中匹配请求自动过;reject 级联 → 同 session 全拒
- "托管输出可读但不授权目录":工具输出文件(如 tool_123)读允许,但 external_directory 仍需审批——最小权限

## 待深挖

- [ ] V1 permission(permission/pattern 模型 + arity.ts 163 行)与 V2 差异
- [ ] subagent-permissions(子 agent 权限继承)
