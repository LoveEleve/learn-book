# q50 — V1 会话内部 + ModelsDev + Flag + v1 兼容层(深度版:收尾)

> 域:②参考(V1 内部)+ 支撑 | 文件:opencode/src/session/(run-state 151/instruction 237/summary 160/status 56/prompt/*.txt)+ core/src/(models-dev 266/flag/flag.ts/v1/*)
> review 轮次:2 轮(源码全文核心)

---

## 假设

V1 会话内部 = Runner 包装(run-state:每会话一个 Runner,InstanceState 管理)+ 指令系统(从历史 read 工具提取路径)+ 摘要(git diff 统计)。ModelsDev = 模型目录拉取;Flag = 环境变量集中读取;v1/ = 兼容层。

## 验证

### 1. RunState(设计 1:每会话 Runner)

```ts
// run-state.ts:35-69
InstanceState:runners = Map<SessionID, Runner>(按目录实例化,scope 关闭全 cancel)
Runner.make(onIdle:删除 runner + status idle / onBusy:busy / onInterrupt)
ensureRunning/startShell/assertNotBusy(BusyError)/cancel(含后台任务取消)
// 与 q37 Runner 的关系:这里把 Runner 每会话实例化 + 绑定状态
```

### 2. Instruction(设计 2:历史路径提取)

```ts
// instruction.ts:extract(messages):
遍历 V1 部件:read 工具已完成 + 未 compacted → 提取 state.metadata.loaded 路径
// systemPaths():从历史读过的文件构建系统上下文路径集合
// find/resolve:目录内指令查找(与 V2 的 instruction-context 呼应,q2)
// clear(messageID):清除特定消息的提取
```

**设计要点**:V1 指令系统"从历史 read 调用提取路径"——隐式学习读过什么;V2 改为显式 AGENTS.md 发现(q2)。

### 3. Summary(设计 3:git diff 统计)

```ts
// summary.ts:unquoteGitPath(git 引号路径解析)
// 会话摘要:additions/deletions/files/diffs(git 驱动)
// SessionTable.summary_*(q10)
```

### 4. 提示词模板(prompt/*.txt)

```ts
// prompt/:anthropic/beast/build-switch/codex/copilot-gpt-5/default/gemini/gpt/kimi/meta/plan-mode/plan-reminder/plan/trinity
// ——按模型家族/模式分发的系统提示词(provider 差异)
```

### 5. ModelsDev(设计 4:模型目录拉取)

```ts
// models-dev.ts:1-35
CatalogModelStatus = alpha|beta|deprecated
USER_AGENT = opencode/{channel}/{version}/{client}
CostTier/InterleavedField(schema):模型成本/推理字段映射
// Flock 锁 + 拉取(models.dev URL,可配置 OPENCODE_MODELS_URL)
// 用途:Catalog 的模型元数据源(q39)
```

### 6. Flag(设计 5:环境变量集中读取)

```ts
// flag/flag.ts:
truthy(env)/Config.boolean 两种读取
清单:OTEL 端点/自动堆快照/GIT_BASH/配置文件(OPENCODE_CONFIG/CONTENT)/禁用开关
  (autoupdate/prune/autocompact/models-fetch/mouse/FFF)
  /服务器密码用户名/实验开关(filewatcher/references/workspaces)
// 关键:部分 getter 访问时求值(测试/CLI 运行时设置 env)——非模块加载时快照
// OPENCODE_DISABLE_PROJECT_CONFIG 是 getter(q2 的指令开关)
```

### 7. v1 兼容层(设计 6:v1/ 目录)

```ts
// core/src/v1/(session.ts/permission.ts/config/):
V1 错误族:OutputLengthError/AuthError/AbortedError/StructuredOutputError/APIError/ContextOverflowError/ContentFilterError
// 错误分类驱动 V1 重试/溢出策略(q12)
// AGENTS.md:"Retained V1 contracts should live under src/v1/... once the V1 isolation PR runs"
//   ——V1 隔离目录,新代码不依赖
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | RunState 每会话 Runner + 状态绑定 | run-state.ts:35-69 | ②单执行器管理 |
| 2 | V1 指令 = 历史路径提取 | instruction.ts | ②对比 V2 显式发现 |
| 3 | Summary git diff 统计 | summary.ts | ④会话摘要 |
| 4 | 模型家族提示词模板 | prompt/*.txt | ②提示词分发 |
| 5 | ModelsDev 目录 + USER_AGENT | models-dev.ts | ②模型元数据 |
| 6 | Flag 集中读取(访问时求值) | flag/flag.ts | ②配置开关 |
| 7 | V1 错误族 + 隔离目录 | core/src/v1/ | ④迁移兼容 |

## 面试弹药

- "V1 指令是隐式的":从 read 历史提取路径 vs V2 显式 AGENTS.md 发现——设计演进
- "Flag 访问时求值":getter 而非模块加载快照——测试/运行时设置 env 有效
- "模型家族提示词":anthropic/gpt/gemini/kimi 分模板——provider 差异在提示词层
- "V1 隔离目录":v1/ 子树保留,新代码禁止依赖——迁移路径明确

## 待深挖

- [ ] prompt 模板内容差异(如 codex vs default)
- [ ] models-dev 的拉取/缓存协议
- [ ] V1 permission.ts 的错误/规则细节
