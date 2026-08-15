# q22 — Code-Mode/E2B/Workspace/Host(深度版:代码执行面 + code-mode 契约)

> 域:②执行(代码/工作区) | 文件:packages/(core/tools/code-mode.ts 673 + tests/code-mode.spec.ts 1797)+(e2b/e2b + fs-e2b + subprocess-e2b)+(code-runtime/code-runtime + worker-thread)+(workspace/workspace:entity/spec/paths/types)+(host/webserver + apiproxy + plugin-inventory + directory-picker*)
> review 轮次:3 轮(源码全文核心 + code-mode 测试契约 30+)

---

## 假设

Code-Mode = run_code 工具(经 code-dispatch-log 瀑布);E2B = 远程沙箱 POC;Workspace = 工作区实体;Host = 应用宿主。**code-mode 契约揭示模式贡献/风味解析/并发调度**。

## 验证

### 1. run_code(设计 1:RunCodeBridge)

```ts
// core/tools/code-mode.ts:267-294:
RunCodeBridgeOptions:requireRuntime(抛响亮配置错误)/peekRuntime(不抛,区分无运行时)/maxParallel/shapeDispatchLog
createRunCodeTool:定义 run_code(必需 code + description)
  // 占位描述 → 语言感知 getter 在 schema 发射时替换;参数校验仍按静态 spec
execute:run-scoped abort(outer signal 进,run 结算任何原因都 fire)
```

### 2. code-mode 契约(设计 2:模式/风味/调度)★ review 轮 3

```ts
// tests/code-mode.spec.ts(30+ 契约,关键):
1. 模式贡献(119-217):native 模式贡献每 schema(无 run_code 无 SDK 段,无需运行时);
   code 模式恰好 [run_code] + SDK 段声明其他工具;run_code-only 规则在命名工具指导之前;
   both 模式省略 run_code-only 规则(native 调用确实执行);深嵌套输出 schema 投影无结构化克隆递归
2. run_code 隔离(326):即使 both 模式也永不暴露给程序(无递归分派路径)
3. 风味解析(359-440):非 native 无运行时 → 每装配拒绝;运行时语言无注册 SDK 渲染器 → 拒绝;
   python 运行时选 Python SDK 渲染器;TS 运行时发 TS 风味 run_code schema;
   **惰性解析 + 语言不在风味表 → 响亮失败;无运行时降级 TS**
4. HMR(460):注册表 fiber dispose → 移除 run_code + SDK 段
5. 并发调度(509-697):Promise.all 并发安全调用(每分派日志 start 事件);exclusive 调用屏障
   (安全先排干,它单独跑,后来等待);maxParallelSubCalls 封顶重叠窗口;绑定枚举与分派间注销 → 未知工具;
   有序 pre-execute 不重叠(慢政策延迟下一 start);exclusive 屏障贯穿 post-execute(下一 start 等提交);
   run 结算排干提交中(结算事件在 turn 内追加)
```

**设计要点**:模式 = schema 贡献的声明式组合;风味惰性解析(fail loud vs 降级);并发调度 = exclusive 屏障 + 重叠窗口 + 有序政策。

### 3. E2B/Workspace/Host(设计 3:其余)

```ts
// e2b/:E2B POC(沙箱 + fs/subprocess 适配器)——共享执行世界另一端
// code-runtime:RESERVED_BINDING_GLOBALS/DUNDER_MEMBER/PORTABLE_RESERVED_WORDS(执行安全)
// workspace:WorkspaceEntity(实体宿主 + 移动校验)
// host/:webserver/apiproxy/plugin-inventory/directory-picker*/frontend-static
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | run_code(语言感知 schema + run-scoped abort) | code-mode.ts:267-294 | ②代码执行 |
| 2 | 模式贡献声明式组合(native/code/both) | code-mode.spec:119-217 | ②能力组合 |
| 3 | 风味惰性解析(fail loud vs 降级) | code-mode.spec:359-440 | ②失败语义 |
| 4 | 并发调度(屏障/窗口/有序政策) | code-mode.spec:509-697 | ②工具并发 |
| 5 | E2B/Workspace/Host | e2b + workspace + host | ②外围 |

## 面试弹药

- "模型看到的 schema 匹配语言":占位 → 语言感知 getter 发射时替换——但校验仍按静态 spec
- "run_code 永不暴露给程序":即使 both 模式——无递归分派路径(隔离)
- "exclusive 屏障贯穿 post-execute":下一 start 等提交——屏障完整
- "run 结算排干提交中":结算事件在 turn 内——不悬挂
- "run-scoped abort":run 结算任何原因都 fire——在飞分派不孤儿

## 待深挖

- [ ] e2b 的沙箱协议
- [ ] workspace 的 spec 语义
- [ ] code-runtime 的 worker-thread
