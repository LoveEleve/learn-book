# q33 — V1 工具注册表(深度版:模型过滤 + 插件兼容边界)

> 域:②参考(V1 工具面) | 文件:opencode/src/tool/registry.ts(450)+ tool/(tool.ts/shell/truncate/plan/task/lsp/code-mode)+ 对比 V2(core/src/tool/)
> review 轮次:2 轮(源码全文)

---

## 假设

V1 注册表 = 20 内置工具 + 自定义(插件/目录)的聚合,按模型/provider/flag 过滤,插件工具保留 Zod 兼容(边界盒装)。V2 的注册表是更干净的"不透明定义",V1 的复杂度(过滤/钩子/兼容)揭示了迁移动机。

## 验证

### 1. 工具集合(设计 1:builtin + custom)

```ts
// registry.ts:204-247
内置 20 个:invalid/shell/read/glob/grep/edit/write/task/fetch/todo/search/skill/patch/question/lsp/plan/execute(实验)
  + question 按 client 启用("app"/"cli"/"desktop" 或 enableQuestionTool flag)
  + codeMode(execute 工具,experimentalCodeMode flag)
  + lsp/plan 实验 flag
custom:目录扫描 {tool,tools}/*.{js,ts}(file:// 动态导入)+ 插件 p.tool
// InstanceState 按目录实例化(每打开项目一套)
```

### 2. 模型过滤(设计 2:usePatch 逻辑)

```ts
// registry.ts:286-298
usePatch = modelID.includes("gpt-") && !includes("oss") && !includes("gpt-4")
gpt- 系 → apply_patch;其他 → edit/write
webSearchEnabled:provider = opencode 或 flags.exa/parallel
// 结论:工具面按模型能力适配(不同模型家族的训练差异)
```

### 3. 插件 Zod 兼容(设计 3:边界盒装)

```ts
// registry.ts:120-176 fromPlugin:
args = def.args ?? {}(#27451/#27630:Zod 静默容忍 undefined 的修复)
allZod → z.object(args) + zodJsonSchema;否则 legacyJsonSchema(entries)
parameters = Schema.declare(safeParse 校验)(V1 转 Effect Schema 边界)
execute:EffectBridge.make() 把 Effect ask 桥成 Promise(插件接口)
  + truncate.output(截断) + withSpan(工具遥测:tool.name/session.id/message.id/call.id)
```

**设计要点**:插件是 Promise/Zod 世界,宿主是 Effect/Schema 世界——fromPlugin 是转换边界(Zod→JSON Schema→Effect Schema + Effect→Promise 桥)。V2 的 Tool.make 消除了这个双世界问题(插件也用同一构造器)。

### 4. 定义钩子(设计 4:tool.definition 触发)

```ts
// registry.ts:305-334
plugin.trigger("tool.definition", { toolID }, output)——插件可改写定义(description/parameters/jsonSchema)
task 工具描述 += describeTask(子 agent 列表,权限过滤)
execute 工具描述 += describeCodeMode(MCP 工具目录 + 可见性过滤)
```

### 5. 对比 V2(设计 5:迁移动机)

| 维度 | V1 | V2 |
|------|----|----|
| 工具值 | 公开字段(id/parameters/jsonSchema/execute) | 不透明(冻结对象 + WeakMap) |
| 插件兼容 | Zod→Schema 双向转换(边界盒装) | 同一 Tool.make |
| 过滤 | 注册表内硬编码(usePatch/flag) | materialize 权限过滤 |
| 输出截断 | truncate.output(注册表内) | ToolOutputStore(结算层) |
| 遥测 | withSpan 手写属性 | publisher 统一事件 |

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 20 内置 + custom(目录/插件) | registry.ts:204-247 | ②工具面 |
| 2 | 模型过滤(usePatch/webSearchEnabled) | registry.ts:286-298 | ②模型适配 |
| 3 | 插件 Zod 兼容边界(双向转换) | registry.ts:120-176 | ②异世界桥接 |
| 4 | tool.definition 钩子(插件改写定义) | registry.ts:305-334 | ②扩展点 |
| 5 | V1 vs V2 差异(迁移动机) | 对比 | ②架构演进 |

## 面试弹药

- "双世界边界盒装":插件(Zod/Promise)↔ 宿主(Effect/Schema)——fromPlugin 单向转换,不渗透
- "模型过滤是硬编码妥协":usePatch 按模型家族——V2 用权限过滤 + 注册表覆盖替代
- "工具定义可被插件改写":tool.definition 钩子——定义时插件可注入描述/参数
- "Zod 静默容忍 undefined 的坑":#27451 修复——args ?? {} 显式规范化

## 待深挖

- [ ] tool/shell.ts(V1 shell 工具,与 V2 bash 对比)
- [ ] truncate.ts(截断策略)
- [ ] plan/task 工具(模式切换)
