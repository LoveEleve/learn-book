# q37 — effect 运行时基建(深度版:InstanceState + Runner 四态 + Bridge)

> 域:共享基础设施 | 文件:opencode/src/effect/(instance-state 69/runner 217/bridge 84/app-runtime 135/run-service 47/instance-ref 11/instance-registry 12/promise 17/runtime-flags 78/bootstrap-runtime 19/config-service 67)
> review 轮次:2 轮(源码全文)

---

## 假设

opencode 应用层有三件核心运行时原语:InstanceState(按目录隔离状态)、Runner(单执行器四态状态机,V1 会话用)、EffectBridge(原生回调重入 Effect 世界)。它们是"应用层 vs core 层"的分界工具。

## 验证

### 1. InstanceState(设计 1:按目录 ScopedCache)

```ts
// instance-state.ts:39-69
make(init):ScopedCache(key = directory, lookup = init(ctx))——capacity 无限
get:ScopedCache.get(cache, directory)——按目录取实例
registerDisposer:(directory) => invalidate(cache, directory)(instance-registry 注册,实例消失时清理)
// 语义(AGENTS.md):"If two open directories should not share one copy of the service, it needs InstanceState"
// 与 core 的 LayerMap(60 分钟回收)不同:这是"手动失效"的按目录缓存(无 TTL)
```

### 2. Runner(设计 2:四态单执行器)

```ts
// runner.ts:33-38
State = Idle | Running(run) | Shell(shell) | ShellThenRun(shell + pendingRun)
ensureRunning(work):Running/ShellThenRun → await;Shell → 排队(PendingHandle);Idle → 启动
startShell(work, ready?):非 Idle → Busy 失败;Idle → 启动 shell(可被取消)
cancel:中断当前执行 + 失败 done(Cancelled)+ idle 回调
// 语义:shell 与 run 互斥;shell 执行中 run 排队,shell 结束自动接 run(ShellThenRun → Running)
// onInterrupt:中断时的替代结果(onBusy/onIdle 钩子)
```

**对比**:这是 V1 会话的单执行器(V1 session prompt/shell 互斥);V2 的 RunCoordinator(q11)是"每 key 一个 drain"——V1 的 shell/run 互斥在 V2 变为 coordinator 的 drain 语义。

### 3. EffectBridge(设计 3:回调重入)

```ts
// bridge.ts(84 行)
EffectBridge.make():bridge.promise(effect)——Promise 包装(原生回调里 await)
bridge.fork(effect)——fork 到实例上下文
// 用途:插件工具(Zod/Promise 世界)调宿主 Effect ask(q33)、GitLab workflow approvalHandler(q34)
// 关键:回调不丢失实例上下文(InstanceRef)
```

### 4. 其他(设计 4:辅助件)

```ts
// run-service.ts(47):makeRuntime({runPromise, runFork, runCallback})——共享 memoMap 去重层
// app-runtime.ts(135):运行时构建(应用服务层组装)
// instance-ref.ts:InstanceRef/WorkspaceRef(当前实例上下文 Reference)
// runtime-flags.ts(78):实验 flag 读取
// config-service.ts(67):配置服务(读取 + 缓存)
// promise.ts(17):Promise 适配
// bootstrap-runtime.ts(19):启动运行时
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | InstanceState 按目录缓存 + 手动失效 | instance-state.ts:39-69 | ②实例隔离 |
| 2 | Runner 四态(Idle/Running/Shell/ShellThenRun) | runner.ts:33-38 | ②单执行器(对比 V2 coordinator) |
| 3 | EffectBridge 回调重入(不丢实例上下文) | bridge.ts | ②异世界桥接 |
| 4 | makeRuntime memoMap 去重 + flags | run-service + runtime-flags | ②运行时装配 |

## 面试弹药

- "InstanceState = 手动失效的按目录缓存":ScopedCache + registerDisposer,实例关闭即清理——无 TTL 的 LayerMap
- "Runner 四态 = 会话单执行器":shell 与 run 互斥,shell 中 run 排队,结束自动接——V1 的确定性执行模型
- "Bridge 保留实例上下文":原生回调重入 Effect 世界,不丢 InstanceRef——插件/外部模型集成的关键
- "V1 Runner vs V2 Coordinator":单执行器(忙即拒)vs 每 key drain(可排队)——架构演进

## 待深挖

- [ ] app-runtime.ts 的完整运行时构建
- [ ] instance-registry 的 disposer 机制
- [ ] runtime-flags 的 flag 清单
