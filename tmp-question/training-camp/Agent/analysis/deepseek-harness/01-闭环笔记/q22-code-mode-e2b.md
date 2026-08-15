# q22 — Code-Mode/E2B/Workspace/Host(深度版:代码执行面)

> 域:②执行(代码/工作区) | 文件:packages/(core/tools/code-mode.ts 673)+(e2b/e2b + fs-e2b + subprocess-e2b)+(code-runtime/code-runtime + worker-thread)+(workspace/workspace:entity/spec/paths/types)+(host/webserver + apiproxy + plugin-inventory + directory-picker*)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Code-Mode = run_code 工具(经 code-dispatch-log 瀑布);E2B = 远程沙箱 POC;Workspace = 工作区实体(实体宿主);Host = 应用宿主(webserver/apiproxy/插件清单)。

## 验证

### 1. run_code(设计 1:RunCodeBridge)

```ts
// core/tools/code-mode.ts:267-294:
RunCodeBridgeOptions:
  requireRuntime: 解析 ctx.codeRuntime 或抛响亮配置错误(注册表组装时共享)
  peekRuntime: 不抛读取(undefined = 无运行时)——"no runtime"(降级 TS)vs "unknown language"(响亮失败)
  maxParallel: 并行子调用上限
  shapeDispatchLog: 运行 tools/code-dispatch-log 瀑布(已结算子分派)
createRunCodeTool:定义 run_code 工具(必需 code + description)
  // 占位描述 → 语言感知 getter 在 schema 发射时替换(模型看到的 schema 匹配 SDK 段语言)
  // 参数校验仍按静态 spec(语言无关)
execute:run-scoped abort(outer signal 进,run 结算任何原因都 fire——在飞子分派被杀,排队未启动放弃)
// RESERVED_BINDING_GLOBALS/RESERVED_ERROR_MEMBERS/DUNDER_MEMBER/PORTABLE_RESERVED_WORDS(绑定安全)
```

### 2. E2B(设计 2:远程沙箱 POC)

```ts
// e2b/:e2b(沙箱)+ fs-e2b(fs 适配器)+ subprocess-e2b(subprocess 适配器)
// ——"E2B POC: sandbox + FS/subprocess adapters"(AGENTS.md:17)
//   远程沙箱 = fs/subprocess 共享执行世界的另一端(架构:指向远程 → 全家迁移)
```

### 3. Code-Runtime(设计 3:代码运行时)

```ts
// code-runtime:index + worker-thread 变体
// RESERVED_*:绑定全局/错误成员/保留字(执行环境安全)
```

### 4. Workspace(设计 4:工作区实体)

```ts
// workspace/workspace:WorkspaceEntity implements Workspace(实体宿主 WorkspaceEntityHost)
// WorkspaceMoveInvalidError(实体移动校验)+ spec/paths/types
```

### 5. Host(设计 5:应用宿主)

```ts
// host/:webserver(HTTP 服务)+ apiproxy(API 代理)+ plugin-inventory(插件清单)+
//   directory-picker*(目录选择器)+ frontend-static(前端静态)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | run_code(语言感知 schema + run-scoped abort) | core/tools/code-mode.ts | ②代码执行 |
| 2 | E2B POC(远程沙箱适配器) | e2b/* | ②远程沙箱扩展 |
| 3 | Code-Runtime(保留字安全) | code-runtime | ②执行环境 |
| 4 | Workspace 实体 + 移动校验 | workspace/workspace | ②工作区 |
| 5 | Host(webserver/apiproxy/清单) | host/* | ②应用宿主 |

## 面试弹药

- "模型看到的 schema 匹配语言":占位描述 → 语言感知 getter 发射时替换——但校验仍按静态 spec(语言无关)
- "run-scoped abort":run 结算任何原因都 fire——在飞子分派被杀不孤儿,排队未启动放弃
- "no runtime 降级 vs unknown language 响亮失败":peekRuntime 区分两种缺失
- "E2B = 共享执行世界的另一端":远程沙箱适配器——provider 切换全家迁移的实证

## 待深挖

- [ ] code-mode 的 SDK 段(SDK_SECTION_ORDER = 150)
- [ ] e2b 的沙箱协议
- [ ] workspace 的 spec 语义
