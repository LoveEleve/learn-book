# q20 — Session-Query + Workflow(深度版:语料检索 + 工作流 + 测试契约)

> 域:④知识库(检索)+ ②执行(工作流) | 文件:packages/(session-query/session-query:corpus/cursor/documents/extraction/filters/sources/tracing + tool-session-query:operations 275+/presentation + tests/tool-session-query.spec.ts 2075)+(workflow/workflow:types/runtime-types + workflow-worker-thread:host/meta/protocol/realm/runtime/session/worker + tests/workflow-worker-thread.spec.ts 1474)
> review 轮次:3 轮(源码全文核心 + 双测试契约)

---

## 假设

Session-Query = 逻辑语料库(live 优先 + 持久化回退)+ 检索操作(搜索/跟踪/读取)。Workflow = 工作流能力(worker-thread provider,realm 物化)。**测试契约揭示边界过滤/时间精确/跨线程生命周期**。

## 验证

### 1. 逻辑语料(设计 1:live 优先)

```ts
// session-query/corpus.ts:11-60:
LogicalSession = { header(克隆源头), events(克隆原始日志) }
LogicalSessionSource:借用的源(仅一次同步批投影期间有效;调用方必须克隆保留输出)
SessionCorpus:可选服务注入(ctx.inject(['sessionPersistence']))——持久化挂载时绑定
listSessions(signal):完整逻辑语料,newest-first 确定性顺序
```

### 2. 检索契约(设计 2:边界/时间/权限)★ review 轮 3

```ts
// tests/tool-session-query.spec.ts(20+ 契约,关键):
1. 注册(237):五 cursor-free 工具 + prompt + 超时 + 纯 generic 呈现器,dispose 移除
2. 并发(290):generation-bound 搜索互斥,精确观察并行
3. 查询归一化(366):归一化查询 + 编译 inclusive session/event 过滤器(单父 OR 子句)
4. 时间精确(429-529):同毫秒十进制边界 → 相邻数值不塌缩;反向精确边界 <1ms 拒绝;
   不等长余数隐式尾零;尾零分数拼写 = 同一精确时刻;负 epoch 毫秒分数边界;
   平台解析器不能产生有限值 → 拒绝
5. 过滤编译(542):单边时间戳 + 独立根/父子句
6. 权限(576-614):无 agent fail-closed;直接跨工作区目标拒绝;null-cwd 只允许 self;
   隐藏/不存在父猜测不可区分(不调用搜索);隐藏父下可见子不可发现
```

**设计要点**:时间边界精确到"相邻数值不塌缩"(interval 完整性);权限 fail-closed + 隐藏父防枚举。

### 3. Workflow 契约(设计 3:跨线程生命周期)★ review 轮 3

```ts
// tests/workflow-worker-thread.spec.ts(30+ 契约,关键):
1. 端到端(180):agent() 文本结果/phases/log/args/返回值/事件
2. 参数转发(211-237):outputSchema/agentOptions/provider 跨线程转发;start-request provider 覆盖选子
3. 预发布拒绝(257-335):无效 provider 路由/无效 per-run 总 agent 上限/未注册配置 provider → 发布前拒绝;
   总 agent 上限在引擎天花板下强制
4. 生命周期序(349-432):异步 provider start 先等(早结算结果延后宣布);早结果拒绝但 ChildStarted 在 ChildFailed 前;
   provider start 拒绝分类 AGENT_START(丢早结果,无假生命周期对);pending start abort 一次(不发布生命周期)
5. 跨线程错误(457):子结果拒绝 = 致命 AGENT_RESULT 错误("broken provider is not a failed child")
```

**设计要点**:生命周期序精确(ChildStarted 先于 ChildFailed);provider 故障分类(AGENT_START/AGENT_RESULT 区分);发布前拒绝(配置错误不发布)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 逻辑语料(live 优先 + 借源契约) | session-query/corpus.ts | ④全书检索 |
| 2 | 时间边界精确(相邻值不塌缩) | tool-session-query.spec:429-529 | ④检索正确性 |
| 3 | 权限 fail-closed(隐藏父防枚举) | tool-session-query.spec:576-614 | ④安全 |
| 4 | Workflow 生命周期序(ChildStarted 先于失败) | workflow-worker-thread.spec:349-457 | ②子任务事件 |
| 5 | 错误分类(AGENT_START/AGENT_RESULT) | workflow spec:403-457 | ③错误契约 |

## 面试弹药

- "同毫秒边界不塌缩":十进制精确到相邻数值——时间过滤完整性
- "隐藏父防枚举":不存在与隐藏不可区分——不泄漏存在性
- "ChildStarted 先于 ChildFailed":生命周期事件序强制——观察者依赖
- "broken provider ≠ failed child":AGENT_RESULT 致命错误分类——责任明确
- "发布前拒绝配置错误":无效路由/上限发布前拒——misconfiguration fails loud

## 待深挖

- [ ] cursor/filters 的分页过滤细节
- [ ] tracing 的跟踪语义
- [ ] workflow 的 realm 协议
