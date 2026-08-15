# 闭环笔记 Q5:工具系统(两套实现)

> 域:harness/tools/(1190 行)+ coding-agent/core/tools/(4150 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

Pi 有"抽象层工具(harness)+ 编码专用工具(core)"两套实现。AgentTool 接口是循环消费的核心契约;core 版工具通过可插拔 Operations 接口支持远程执行。

## 验证过程

### 1. 两套工具的结构对比

| 维度 | harness 版(agent 包) | core 版(coding-agent 包) |
|------|---------------------|--------------------------|
| 工具数 | 5(bash/read/write/edit/edit-diff) | 10(+find/grep/ls/output-accumulator) |
| 行数 | 1190 | 4150 |
| 创建函数 | createXxxTool | createXxxTool **+ createXxxToolDefinition** |
| 定位 | 通用抽象(跨模式复用) | 编码 agent 专用(含 UI 渲染) |

**核心发现**:
- core 版每个工具有**双创建函数**:`createXxxTool`(AgentTool,给 agent 循环)+ `createXxxToolDefinition`(ToolDefinition,给扩展系统)(core/tools/index.ts:73-78)
- core 版工具 import 了 TUI 组件(theme/Container/Text)做**结果渲染**——harness 版纯逻辑无 UI

### 2. AgentTool 契约(agent/src/types.ts:386-)— 循环消费的核心

```ts
interface AgentTool<TParameters, TDetails> extends Tool<TParameters> {
  label: string;                                  // UI 标签
  prepareArguments?: (args) => TParameters;        // 兼容垫片(schema 验证前)
  execute: (toolCallId, params, signal?, onUpdate?) => Promise<AgentToolResult<TDetails>>;
  executionMode?: "sequential" | "parallel";       // 单工具覆盖默认模式
}
```

**AgentToolResult**(types.ts:361-381):
```
content: 返回给模型的文本/图片
details: 结构化细节(日志/UI)
usage: 工具自身消耗(不计入主上下文)
addedToolNames: 动态注册的新工具(自进化!)
terminate: 提示停止当前工具批
```

### 3. 可插拔 Operations 接口 — 远程执行支持

**每个核心工具都有 Operations 接口**:
```ts
// bash.ts:62-80
BashOperations.exec(command, cwd, {onData, signal, timeout, env}) 
  → { exitCode }   // 注释:Override to delegate to remote systems (e.g. SSH)

// find.ts:55-66
FindOperations.exists(path) + glob(pattern, cwd, {ignore, limit})

// grep.ts:56-68
GrepOperations.isDirectory(path) + readFile(path)
```

**设计价值**:**工具执行后端可插拔**——本地实现是默认,SSH/远程可替换。产品"分析远程仓库/远程执行"直接可扩展。

### 4. OutputAccumulator:有界内存流式输出(output-accumulator.ts 222 行,100% 读完)

```
append(data):流式 UTF-8 解码 + 滚动 tail(内存只留 tailText)
  - maxRollingBytes = maxBytes * 2(滚动窗口)
  - trimTail 裁剪时跳过多字节字符边界((buffer[start] & 0xc0) === 0x80)
  - 行统计(completedLines/currentLineBytes)
超限(shouldUseTempFile):才开临时文件(ensureTempFile)
snapshot:截断内容 + 截断详情 + fullOutputPath
```

**核心设计**:
- **内存有界**:无论输出多大,内存只保留 tail(2× 预算)
- **临时文件按需**:超限才落盘,不超限零 IO
- **多字节安全**:裁剪不切坏 UTF-8 字符
- **行级统计**:截断提示 "Showing lines X-Y of N"

**产品映射**:产品"读取大文件/长工具输出"直接抄——有界内存 + 按需落盘。

### 5. bash 工具工程细节(bash.ts 510 行)

- **超时**:MAX_TIMEOUT_MS = 2^31-1(最大约 24.8 天)(:25)
- **detached 进程 + 进程树管理**:trackDetachedChildPid/killProcessTree(:114-148)——超时/abort 时杀整个进程树
- **命令 stdin 传输**:commandTransport === "stdin"(:102)——避免命令行注入
- **节流更新**:BASH_UPDATE_THROTTLE_MS 限制更新频率(:374-387)
- **cwd 存在性检查**:exec 前 fsAccess(:97-100)
- **截断提示**:显示 "Showing lines X-Y of N. Full output: <path>"(:415-424)——**截断不丢信息,全量在临时文件**

### 6. systemPromptContribution — 工具自描述系统提示

每个工具导出:
```ts
bashToolSystemPromptContribution = {
  snippet: "Execute bash commands (ls, grep, find, etc.)",
  guidelines: ["You can inspect PI_* environment variables..."],
}
```
收集点:`server/create-harness.ts:102-123`(各工具的 snippet/guidelines 拼进 system prompt 的 toolSnippets)。

**设计价值**:**工具向 system prompt 自我声明能力**——新增工具自动获得提示词描述,不用手写。产品"验收器/知识库工具"同样自描述。

### 7. bash 的错误处理细节(bash.ts 431-510)

```
execute 流程:
  resolveSpawnContext(commandPrefix/cwd/spawnHook/exposeSessionEnvironment)
  → OutputAccumulator + 节流 onUpdate
  → ops.exec(可插拔后端)
  → 退出码非 0 → 结果带 isError + 退出码 + stderr
  → 超时/abort → killProcessTree + 错误结果
```

**关键**:commandPrefix(命令前缀拼接)、spawnHook(命令改写钩子)——**命令审计/沙箱注入点**。

### 8. AgentTool ↔ ToolDefinition 双向转换(index.ts + wrapper)— review 新增

```
wrapToolDefinition: ToolDefinition → AgentTool(扩展工具接入核心循环)
createToolDefinitionFromAgentTool: AgentTool → ToolDefinition(注册表归一)
```

**关键设计**:**AgentSession 内部注册表是 definition-first**——即使外部传 AgentTool,也转成 ToolDefinition 再注册(注释:"keeps AgentSession's internal registry definition-first")。
**产品映射**:产品工具注册表统一用一种规范(definition),外部多种形式(AgentTool/自定义)转换后接入。

### 9. 工具组合预设 = 权限分级(tools/index.ts:138-166)— review 新增,修正域发现结论

```
createReadOnlyToolDefinitions: read/grep/find/ls(只读!)
createCodingToolDefinitions: read/bash/edit/write(可写)
createAllToolDefinitions: 全部 7 个
```

**重要修正**:域发现里"Pi 无细粒度权限"的结论**不完整**——Pi 通过**工具组合预设**实现权限分级:
- 只读模式 = 只挂 read/grep/find/ls(没有 bash/edit/write)
- 编码模式 = 加 bash/edit/write
- SDK 暴露(createReadOnlyTools/createCodingTools:sdk.ts:119-120)

**产品映射**:产品"学习模式(只读分析)vs 写书模式(可写文件)"用工具集合切换,不用复杂权限系统。

### 10. setActiveToolsByName 动态切换(agent-session.ts:928-946)— review 新增

```
setActiveToolsByName(toolNames):
  从注册表取工具 → agent.state.tools = 新集合
  → _rebuildSystemPrompt(validToolNames) 重建系统提示
  → 提示词只描述活动工具
```

**关键设计**:**换工具集 → system prompt 同步重建**——模型看到的工具描述永远与活动集合一致。
**产品映射**:产品运行时切换"分析模式/写书模式",提示词自动跟随。

### 11. _buildRuntime 工具组装(2556-2608)— review 新增

```
基础工具(可被 override)→ 扩展工具(ExtensionRunner)→ 默认活动集:
  baseToolsOverride 存在 → 用 override 的工具名
  否则 → ["read", "bash", "edit", "write"](默认 4 个写工具)
```

**关键**:默认只激活 4 个核心工具,其余按需 setActiveToolsByName 激活——**最小权限原则**。

### 12. edit 工具:精确优先 + 模糊兜底(edit-diff.ts:206-244)— review 第三轮新增

```
fuzzyFindText 两阶段:
1. 精确匹配(indexOf)——找到就用原文
2. 模糊匹配(normalizeForFuzzyMatch 后 indexOf)
   ——归一化:去掉行尾空白等(渐进变换,33 行注释)
   ——模糊匹配时在归一化空间计算,返回 contentForReplacement = 归一化内容
```

**设计价值**:LLM 生成的 oldText 可能带多余空格/不一致换行——**精确失败不立即报错,模糊兜底再失败才报错**。这是"模型输出的容错"。

### 13. edit 错误消息 = 给 LLM 的反馈(edit-diff.ts:257-293)— review 第三轮新增

4 种错误,错误消息专门写给 LLM 看:
```
not found: "The old text must match exactly including all whitespace and newlines."
duplicate: "Found N occurrences... The text must be unique. Please provide more context."
empty oldText: "oldText must not be empty"
no change: "The replacement produced identical content."
```

**核心设计**:**错误消息即反馈**——不是"编辑失败",而是告诉 LLM 为什么失败、怎么改("provide more context to make it unique")。LLM 下一轮直接可行动。
**产品映射**:产品验收器/分析工具的错误消息同样要"写给模型看"——错误信息指导下一步行动。

### 14. 文本健壮性处理(edit-diff.ts 全套)— review 第三轮新增

```
detectLineEnding/normalizeToLF/restoreLineEndings:CRLF/LF 检测与恢复
stripBom:BOM 剥离(UTF-8 BOM)
generateUnifiedPatch:unified diff 生成(4 行上下文)
generateDiffString:带行号的显示 diff + firstChangedLine
```

**设计价值**:编辑工具对"文本边界"(换行符/BOM)的完整处理——**跨平台文件编辑不破坏原格式**。产品写书工具同样需要(写 Markdown 保留 LF)。

## 代码类型

Interface(AgentTool 契约)+ Implementation(具体工具)

## 跨域关联

- → 被依赖:agent-loop(executeToolCalls 消费 AgentTool)
- ← 依赖:truncate.ts(截断)、render-utils.ts(渲染)、shell.ts(进程管理)

## 结论

核心可抄设计 14 个:
1. **AgentTool 契约(label/prepareArguments/execute/executionMode)** → 产品工具接口
2. **双创建函数(AgentTool + ToolDefinition)** → 同一工具两种暴露
3. **可插拔 Operations 接口** → 远程执行/沙箱扩展
4. **OutputAccumulator 有界内存 + 按需落盘** → 长输出处理
5. **截断提示(全量在临时文件)** → 截断不丢信息
6. **进程树管理(detached + killProcessTree)** → 超时/abort 杀干净
7. **systemPromptContribution 自描述** → 工具自动进系统提示
8. **AgentTool ↔ ToolDefinition 双向转换** → 注册表归一
9. **工具组合预设 = 权限分级(只读/可写)** → 模式切换(修正"Pi 无权限模型")
10. **setActiveToolsByName + 提示词重建** → 运行时动态换工具集
11. **默认最小工具集(4 个)** → 最小权限原则
12. **精确优先 + 模糊兜底匹配** → 模型输出容错
13. **错误消息写给 LLM 看(指导下一步)** → 反馈驱动迭代
14. **文本健壮性(CRLF/BOM/补丁生成)** → 跨平台文件处理

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| AgentTool 契约 | ✅ 抄 | 产品工具接口(读/写/搜索/分析工具) |
| Operations 可插拔 | ✅ 抄 | 产品工具后端可换(本地/SSH/沙箱) |
| OutputAccumulator | ✅ 抄 | 读大源码文件的有界内存处理 |
| 截断+临时文件 | ✅ 抄 | 工具输出截断不丢全量 |
| 进程树管理 | ✅ 抄 | bash 超时/abort 安全 |
| systemPromptContribution | ✅ 抄 | 新工具自动描述 |
| definition-first 注册表 | ✅ 抄 | 工具注册统一规范 |
| **工具预设 = 权限分级** | ✅ 抄 | **学习模式(只读)vs 写书模式(可写)** |
| 动态换工具集+提示词重建 | ✅ 抄 | 运行时模式切换 |
| 默认最小工具集 | ✅ 抄 | 最小权限 |
| 精确+模糊匹配 | ✅ 抄 | 分析工具容错 |
| **错误消息=反馈** | ✅ 抄 | 验收器失败时指导 LLM 怎么改 |

## 面试问答弹药

- **Q**:工具接口怎么设计?→ A:AgentTool 契约——label/prepareArguments/execute(signal+onUpdate)/executionMode
- **Q**:工具能远程执行吗?→ A:能——每个工具 Operations 接口可插拔,默认本地,SSH 可替换
- **Q**:bash 输出很大怎么办?→ A:OutputAccumulator 有界内存 + 超限落临时文件,截断提示显示行号+全量路径
- **Q**:超时/中断怎么处理?→ A:detached 进程 + 进程树跟踪,超时杀整棵树,不留孤儿进程
- **Q**:工具怎么让模型知道?→ A:systemPromptContribution——工具自描述 snippet/guidelines,自动拼进 system prompt
- **Q**:Pi 有权限模型吗?→ A:有工具集分级——只读预设(read/grep/find/ls)vs 可写预设(+bash/edit/write),SDK 按模式选择
- **Q**:运行时切换工具?→ A:setActiveToolsByName——换集合 + system prompt 同步重建
- **Q**:edit 匹配不上怎么办?→ A:精确优先 + 模糊兜底(归一化后匹配);还不行,错误消息告诉 LLM 怎么改("provide more context")
