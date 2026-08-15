# q8 — 工具系统 V2(深度版:不透明值 + 结算管线 + 输出托管)

> 域:②执行(工具) | 文件:core/src/tool/(tool.ts 162/registry.ts 147/application-tools.ts/tools.ts/builtins + leaves)+ tool-output-store.ts(211)+ specs/v2/tools.md + core/test/(application-tools 452/tool-* 各测试)
> review 轮次:2 轮(源码全文 + 测试抽查)

---

## 假设

V2 工具系统:工具 = 不透明值(冻结空对象 + WeakMap 运行时),结算 = decode→execute→encode→project→bound 管线,输出托管 = bounded preview 留历史 + 完整文本进托管文件。工具不知道上下文限制,注册表统一边界。

## 验证

### 1. 不透明值机制(设计 1:冻结对象 + WeakMap)

```ts
// tool.ts:69-76,152-156
const runtimes = new WeakMap<AnyTool, Runtime>()
make(config):tool = Object.freeze({})  ← 值本身是空对象,运行时全在 WeakMap
runtimeOf(tool):不是本工具值 → TypeError "Invalid Core Tool value"(防伪造)
```

**设计要点**:工具值是"符号化句柄",schemas/executor 不在公开字段——外部无法直接调用/篡改,只能走注册表。

### 2. make 的结算管线(设计 2:六步)

```ts
// tool.ts:91-129 settle:
1. Schema.decodeUnknownEffect(input)(call.input) → 失败:ToolFailure("Invalid tool input: ...")
2. config.execute(input, context) → Effect<Output, ToolFailure>
3. Schema.encodeEffect(output) → 失败:"Tool returned an invalid value for its output schema"
4. structured? toStructuredOutput(input, output) → encode structured(结构化输出投影)
5. toModelOutput?(input, output) → Content[](file → data URI)
6. 无 toModelOutput:string 输出 → [{ type: "text" }];非 string → []
```

**definition 缓存**(tool.ts:79-89):每 name 一个 ToolDefinition 缓存(同工具多名字注册只生成一次 schema)。

### 3. 注册与权限(设计 3:withPermission + validateName)

```ts
// tool.ts:134-148
validateName:/^[A-Za-z][A-Za-z0-9_-]{0,63}$/(provider 中立语法)
withPermission(工具, "edit"):装饰器——edit/write/apply_patch 共享 "edit" action(默认 = 工具名)
// registry.ts:106-113,132-135 materialize:deny `*` 的工具从定义删除(whole-tool 过滤)
```

### 4. 输出托管(设计 4:ToolOutputStore.bound)

```ts
// tool-output-store.ts:13-17
MAX_LINES 2000 / MAX_BYTES 50KB / RETENTION 7 天 / MANAGED_DIRECTORY "tool-output"(global.data 下)
// bound(tool-output-store.ts:138-174):
1. 媒体(file)与文本分离——只量文本,媒体保留(producer-owned limits)
2. 无 content → structured JSON.stringify(2 空格缩进)作为量测对象
3. 超限 → write(完整文本,文件名 tool_{ascending},flag "wx" 排他)+ boundedPreview 留历史
4. marker:"... output truncated; full content saved to ${outputPath} ..."
```

**preview 策略**(tool-output-store.ts:74-104):
- 行采样:headLines(ceil/2)+ tailLines(floor/2),中间截断
- 行数达标但字节超 → 字节截断:takePrefix(headBytes)+ takeSuffix(tailBytes)(UTF-8 安全,按 char 累加 byteLength)
- **保头保尾**——"Generic truncation preserves the beginning and end of textual output"(CONTEXT.md:191)

**cleanup**(tool-output-store.ts:176-189 + 199-211):每小时一次全局扫描,7 天保留;tool_ 前缀过滤。

### 5. 结算语义(设计 5:registry.settleWith 七步)

```ts
// registry.ts:50-82:
1. 有效查找:local 最新 或 application
2. stale 校验:advertised identity ≠ 当前 → "Stale tool call"
3. settle(tool, call, context) → decode/execute/encode/project
4. ToolFailure → 结构化 error result
5. resources.bound → 输出托管
6. ToolOutput.toResultValue → 结算值
7. 返回 { result, output?, outputPaths? }
```

**stale rejection**(specs/v2/tools.md:151):materialize 时捕获每个广告名字的注册身份(不保留 handler);结算时身份不符 → 拒绝(工具被替换/移除后旧调用不执行)。

### 6. 应用 vs Location 注册(设计 6:双服务)

```ts
// ApplicationTools(process 级,公开 opencode.tools.register)+ Tools(Location 级窄能力)
// Location 注册覆盖 process 注册(registry.ts:107-111)
// Scope 关闭 → 移除自己的注册,露出上一个(registry.ts:94-102)
```

**测试证据**(session-runner.test.ts:558 "advertises and executes a globally attached application tool"):全局注册 → 工具定义进请求 + 执行上下文完整(sessionID/agent/assistantMessageID/toolCallID)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 不透明值(冻结对象 + WeakMap + 防伪造) | tool.ts:69-76,152-156 | ②工具契约安全 |
| 2 | make 六步结算管线 + definition 缓存 | tool.ts:91-129 | ②工具执行正确性 |
| 3 | withPermission 装饰器 + validateName | tool.ts:134-148 | ②权限声明 |
| 4 | 输出托管(bounded preview 保头保尾 + marker + 托管文件) | tool-output-store.ts:138-174 | ②上下文保护 |
| 5 | 结算七步 + stale rejection | registry.ts:50-82 | ②执行正确性 |
| 6 | 应用 vs Location 双注册 + Scope 覆盖 | registry.ts:85-123 | ②插件工具生命周期 |

## 面试弹药

- "工具值 = 冻结空对象":schemas/executor 全在 WeakMap 运行时,值本身不可篡改、不可伪造——外部无法直接调用
- "保头保尾截断":行采样 + 字节截断(takePrefix/takeSuffix,UTF-8 安全),中间 marker 指向托管文件——模型看到的是"两头 + 指针"
- "媒体不量测":file 类型输出保留原样(producer-owned limits),只量文本——图片不受文本限制污染
- "stale rejection 防执行错误实现":广告的是 A 工具,结算时被换 B → 拒绝(identity 校验)
- "结构化-only 也托管":无 content 时量 JSON.stringify 缩进文本,超限同样托管——结构化结果不被截断

## 待深挖

- [ ] 内置工具 leaves 的权限断言模式(read/grep/bash/apply-patch 各一)
- [ ] tool-output-store.test.ts 的边界测试契约
- [ ] V1 tool/registry.ts(450)对比
