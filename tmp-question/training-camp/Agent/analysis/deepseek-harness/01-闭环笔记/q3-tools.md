# q3 — Tools 注册表 + 执行管线(深度版:五事件管线 + 守卫 + 测试契约)

> 域:②执行引擎(工具) | 文件:packages/core/tools/src/(index.ts 1946/schema.ts 617/presentation.ts 389/code-mode.ts 673/json-schema.ts 656)+ tools/tests/tools.spec.ts(2785)+ docs/tool-execution-pipeline.md
> review 轮次:3 轮(源码全文核心 + 测试契约 60+)

---

## 假设

Tools = scoped 注册表 + 守卫执行管线。管线 = 5 事件:pre-execute(allow/deny/ask)→ execute(around 包装)→ post-execute(接受/替换/阻断)→ code-dispatch-log(durable 日志副本)→ result(emit)。工具 UI 渲染意图是设计一部分。**测试契约揭示归一化/降级/审计语义**。

## 验证

### 1. 五事件管线(设计 1:waterfall × 4 + emit × 2)

```ts
// index.ts:152-207:
'tools/pre-execute'(waterfall): allow/deny/ask;next() 委托 allow;缺审批支持 → ask 变 deny
'tools/execute'(waterfall): 超时/重试/指标包装;只能改 exec.signal,调用身份不可变
'tools/post-execute'(waterfall): 接受/替换/丰富/阻断归一化结果;抛出的工具也到达此瀑布
'tools/code-dispatch-log'(waterfall): 替换 run_code 子分派的 DURABLE 日志副本
'tools/result'(emit): 冻结 lossless-JSON 最终结果(监听失败隔离)
'tools/change'(emit): 未过滤注册表通知
```

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
```

### 3. 测试契约(设计 3:归一化/降级/审计)★ review 轮 3

```ts
// tools/tests/tools.spec.ts(60+ 契约,关键):
1. schema 泄漏防护(54-75):schemas() 丢弃 host 回调 + timeoutMs——"预算必须永不到模型"
2. 契约违反归一化(132-199):非克隆结果 → 最终通知前归一化;抛错 final content 回调不二次调用
3. canonical 输出强制(224):每个原始注册必须声明 canonical 输出
4. body 校验(237-262):有损/schema 不匹配 → post-execute 前拒绝;抛 body 快照 → 无效工具输出
5. post-execute 决策矩阵(342-550):双投影替换失败/block → valueless 失败/值替换不制造额外上下文/
   非 JSON 失败投影安全/失败分派拒绝值替换/拥有工具消失 → 失败值替换/wrapper 失败元数据归一化
6. 嵌套复合分派(550-570):展示元数据仅嵌套复合分派抑制;嵌套结论在嵌套结果上转发
7. 未知/抛错工具(620-644):isError 结果;hostile 抛值(检查+强制都抛)归一化
8. pre-execute 权限(676-709):deny 模式;ask 无审批缝 → 降级 deny;无理由 → 默认消息
9. ask 路由经 ctx.approval(720-840):allowed-once 分派/拒绝理由/取消理由/ABORTED_BEFORE_DISPATCH/
   无通道 deny/无 agent 不问(无路由无审计)/rogue 结局 → isError
10. post-execute 补充(856-961):block → isError+纠正反馈/内容转换序(快照后)/additionalContexts
    在结果上让循环缓冲(工具推迟/wrapper/post-execute 上下文按序)
```

**设计要点**:归一化是全覆盖的(契约违反/抛错/非 JSON 都安全);降级链完整(ask→deny→默认消息→无通道);审计经 result(冻结 JSON)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 五事件管线(pre/post/execute/log/result) | index.ts:152-207 | ②工具执行分离 |
| 2 | 守卫管线(取消重查 + ask + guardReason) | index.ts:1470-1500 | ②权限/取消 |
| 3 | schema 泄漏防护(回调/timeout 永不到模型) | tools.spec:54-75 | ②模型安全 |
| 4 | 归一化矩阵(契约违反/抛错/非 JSON) | tools.spec:132-262,644 | ②健壮性 |
| 5 | ask 降级链(无缝→deny→默认消息) | tools.spec:697-840 | ②安全默认 |
| 6 | 嵌套复合分派(元数据抑制+结论转发) | tools.spec:550-570 | ②子分派 |
| 7 | 呈现层(UI 渲染意图) | presentation.ts | ②工具展示 |

## 面试弹药

- "预算永不到模型":timeoutMs 从 schemas() 剔除——模型看不到执行预算
- "归一化全覆盖":契约违反/抛错/hostile 值都安全归一化——不崩管线
- "ask 降级链完整":无审批缝→deny;无理由→默认消息;无通道→deny——fail-closed 每一步
- "无 agent 不问":agent-less 执行跳过 ask(无路由无审计)——边界精确
- "嵌套结论转发":复合分派的结论在嵌套结果上携带——不丢失

## 待深挖

- [ ] code-mode.spec.ts(1797)的 run_code 契约
- [ ] 呈现层的投影器契约
- [ ] json-schema 的生成细节
