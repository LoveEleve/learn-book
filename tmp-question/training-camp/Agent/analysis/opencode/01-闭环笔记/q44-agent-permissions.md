# q44 — Agent + 子 agent 权限(深度版:默认权限 + 继承规则)

> 域:②执行(agent) | 文件:opencode/src/agent/(agent.ts 453/subagent-permissions.ts 27/prompt/*.txt)+ core/src/agent.ts(V2)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Agent 系统分 V1(配置 agent,InstanceState 按目录)与 V2(core/agent.ts,State.Transformable)。关键设计:默认权限集(含 .env 保护)、子 agent 权限派生(继承 deny + 禁用 task/todowrite)、自动生成 agent。

## 验证

### 1. Agent 模型(设计 1:Info 15 字段)

```ts
// agent.ts:35-56
Info = { name, description?, mode: "subagent"|"primary"|"all", native?, hidden?,
  topP/temperature/color, permission: Ruleset, model?, variant?, prompt?, options, steps? }
// 核心:permission 是 V1 规则集;mode 区分主/子 agent
```

### 2. 默认权限集(设计 2:安全默认 + .env 保护)

```ts
// agent.ts:108-136
whitelistedDirs = [truncate GLOB, tmp/*, skillDirs/*, referenceDirs/*](external_directory 自动 allow)
defaults = Permission.fromConfig({
  "*": "allow",
  doom_loop: "ask",
  external_directory: { "*": "ask", ...whitelist allow },
  question: "deny", plan_enter: "deny", plan_exit: "deny",
  read: { "*": "allow", "*.env": "ask", "*.env.*": "ask", "*.env.example": "allow" },
})
// build agent:defaults + { question: allow, plan_enter: allow } + user 配置
// 关键:*.env 读取要 ask(环境文件保护,gitignore 模式镜像)
```

**设计要点**:agent 权限默认"读全开 + 敏感文件 ask + 破坏性 ask";.env 保护是安全默认。

### 3. 子 agent 权限(设计 3:deriveSubagentSessionPermission)

```ts
// subagent-permissions.ts:14-27
derive(parentSessionPermission, subagent):
1. 继承父会话的 deny 规则 + external_directory 规则
   ("Parent agent restrictions only govern that agent; the subagent's own permissions determine its capabilities")
2. 子 agent 无 task/todowrite 权限 → 默认 deny(防子 agent 再开子 agent/改清单)
// 结论:继承"限制"(deny/外部目录),能力看子 agent 自己的配置
```

### 4. 自动生成(设计 4:generate)

```ts
// agent.ts:368-445:generate(description, model?) → { identifier, whenToUse, systemPrompt }
// 用 LLM 从描述生成新 agent(identifier/whenToUse/systemPrompt)——动态 agent 创建
```

### 5. 提示词模板(prompt/*.txt)

```ts
// prompt/:compaction.txt(压缩)/explore.txt(探索)/summary.txt(摘要)/title.txt(标题)
// ——V1 专用 agent 的 prompt 模板
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Agent Info 15 字段(mode 区分主/子) | agent.ts:35-56 | ②agent 模型 |
| 2 | 默认权限集(读全开+.env ask+外部目录白名单) | agent.ts:108-136 | ②安全默认(产品可抄) |
| 3 | 子 agent 继承 deny + 禁 task/todowrite | subagent-permissions.ts:14-27 | ②权限继承 |
| 4 | generate 自动创建 agent | agent.ts:368-445 | ②动态 agent |
| 5 | 专用 prompt 模板 | prompt/*.txt | ②模板管理 |

## 面试弹药

- ".env 保护是安全默认":read *.env → ask(除 .env.example)——环境变量泄漏防护
- "子 agent 继承限制不继承能力":父 deny + external_directory 传给子;能力看子自身配置——最小权限
- "防子 agent 再开子 agent":无 task 权限 → 默认 deny——递归深度控制
- "白名单目录自动 allow":技能/引用/截断目录 external_directory 免问——常用目录不打扰

## 待深挖

- [ ] agent prompt 模板内容(compaction/explore/summary/title)
- [ ] V2 core/agent.ts 的 select/resolve 与 V1 对比
- [ ] generate 的提示词与模型选择
