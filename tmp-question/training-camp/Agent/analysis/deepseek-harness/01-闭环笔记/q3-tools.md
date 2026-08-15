# q3 — Tools 注册表 + 执行管线(深度版:五事件管线 + 守卫)

> 域:②执行引擎(工具) | 文件:packages/core/tools/src/(index.ts 1946/schema.ts 617/presentation.ts 389/code-mode.ts 673/json-schema.ts 656)+ docs/tool-execution-pipeline.md
> review 轮次:2 轮(源码全文核心)

---

## 假设

Tools = scoped 注册表 + 守卫执行管线。管线 = 5 事件:pre-execute(allow/deny/ask)→ execute(around 包装)→ post-execute(接受/替换/阻断)→ code-dispatch-log(durable 日志副本)→ result(emit)。工具 UI 渲染意图是设计一部分。

## 验证

### 1. 五事件管线(设计 1:waterfall × 4 + emit × 2)

```ts
// index.ts:152-207:
'tools/pre-execute'(waterfall): allow/deny/ask;next() 委托 allow;缺审批支持 → ask 变 deny;
  async 门必须观察 exec.signal;注册表在 settle 后重查取消(不放弃它们的 promise)
'tools/execute'(waterfall): 超时/重试/指标包装;next() 返回归一化结果;包装只能改 exec.signal,
  调用身份不可变;注册表在执行体前重熔原始 caller signal(替换不能脱离调用方取消)
'tools/post-execute'(waterfall): 接受/替换/丰富/阻断归一化结果;抛出的工具也到达此瀑布;
  调用方取消只替换"成功接受"的结果
'tools/code-dispatch-log'(waterfall): 替换 run_code 子分派的 DURABLE 日志副本(如 spill 策略的
  preview+locator);仅日志副本受影响——程序已收到完整值,模型两者都看不到
'tools/result'(emit): 冻结 lossless-JSON 最终结果(监听失败隔离)
'tools/change'(emit): 未过滤注册表通知(全局变化关乎每个 agent 的下次组装)
// scope 过滤:agent-scoped 监听器只收该 agent 的调用
```

**产品启示**:②工具执行的"门 + 包装 + 结果修正 + 审计"四层分离——验收器可挂 post-execute 改结果、result 做审计、code-dispatch-log 做溢出处理。

### 2. 守卫管线(设计 2:执行序)

```ts
// index.ts:1470-1500:
1. callerCancelled(exec) → toolAbortedBeforeDispatchResult
2. waterfall pre-execute → gate(allow/deny/ask)
3. ask → serviceAsk(审批服务)
4. callerCancelled + approvalCancelled → aborted-before-dispatch
5. decision.allow → guardReason(工具限制);否则 denialReason
6. denialReason → 拒绝结果(Error: {reason}, isError: true)
7. callerCancelled 重查 → ...
8. execute body
// ToolNotFoundError / ToolOutputError(schema 校验失败)
// TOOL_ABORTED / TOOL_ABORTED_BEFORE_DISPATCH 常量
```

### 3. 工具定义(设计 3:ToolDefinition)

```ts
// index.ts:212-291 + schema.ts(617):
ToolOutputDefinition / ToolDefinition extends ToolSchema
ToolExecutionInput: 调用输入(caller agent/signal)
ToolExecutionResult: 归一化结果(内容 + 错误)
// code-mode.ts(673):代码模式工具(run_code 子分派 + code-dispatch-log)
// json-schema.ts(656):JSON Schema 处理(模型→工具参数)
// presentation.ts(389):结果视图(ReadResultView/WebResultView/WebSearchResultView)——工具 UI 渲染意图
```

### 4. 作用域(设计 4:scoped 注册)

```ts
// tools/change + agent-scoped 过滤:@deepseek-ai/dsh-scope
// 每 agent 可用工具集可不同(preset 的 isolate realm,architecture.md:112)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 五事件管线(pre/post/execute/log/result) | index.ts:152-207 | ②工具执行分离 |
| 2 | 守卫管线(取消重查 + ask + guardReason) | index.ts:1470-1500 | ②权限/取消 |
| 3 | ToolDefinition + schema 校验 | index.ts:212-291 + schema.ts | ②工具契约 |
| 4 | code-dispatch-log(日志副本替换) | index.ts:189 | ②溢出/审计 |
| 5 | 呈现层(UI 渲染意图) | presentation.ts | ②工具展示 |

## 面试弹药

- "门/包装/结果修正/审计四层":pre-execute 门 + execute 包装 + post-execute 修正 + result 审计——验收器想挂哪层都有缝
- "替换不能脱离取消":execute 包装只能改 signal,注册表重熔 caller signal——取消语义不可绕过
- "code-dispatch-log 只改日志副本":程序拿完整值、模型看预览、日志存 preview+locator——三视角分离
- "ask 缺支持变 deny":fail-closed(无审批支持 = 拒绝)

## 待深挖

- [ ] serviceAsk 的审批集成(interaction 域)
- [ ] guardReason 的工具限制配置
- [ ] code-mode 的 run_code 语义
