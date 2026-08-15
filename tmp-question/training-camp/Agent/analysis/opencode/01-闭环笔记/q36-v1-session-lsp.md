# q36 — V1 会话服务 + LSP(深度版:消息部件级 API + 语言服务)

> 域:②参考(V1 服务面)+ 集成 | 文件:opencode/src/session/session.ts(1018)+ lsp/(server 1983/client 650/lsp 507/language 121/launch 21/diagnostic 29)
> review 轮次:1-2 轮(接口面 + 核心结构)

---

## 假设

V1 会话服务 = 消息/部件级细粒度 API(updatePart/removeMessage...),与 V2 的"事件溯源 + 投影"形成鲜明对比。LSP 是语言服务集成(诊断/hover/符号),供工具与 IDE 使用。

## 验证

### 1. V1 会话操作面(设计 1:32 操作,部件级)

```ts
// session.ts:415-466
生命周期:list/listGlobal/create(可带 parentID 子会话)/fork/touch/get/remove
元数据:setTitle/setArchived/setMetadata/setAgentModel/setPermission/setRevert/setSummary/setShare/setWorkspace
消息:updateMessage/removeMessage/getPart/updatePart/updatePartDelta/findMessage(最新优先谓词搜索)
diff:会话文件快照 diff
// BusyError(session.ts:409):会话忙时操作失败(单执行器)
```

**对比 V2**:V1 是"直接写存储"(updatePart 修改部件),V2 是"事件驱动投影"(q9)。V1 的 updatePartDelta(字段级 delta)是流式更新的手写实现——V2 的 delta 事件 + 投影器取代了它。

### 2. 子会话(设计 2:fork + parentID)

```ts
// fork({sessionID, messageID?}):从消息处分叉子会话
// create(parentID):子会话(children(parentID) 查询)
// 对比 V2:V2 无 fork 域(OperationUnavailableError 未实现)——V1 独有能力
```

### 3. LSP 能力面(设计 3:lsp.ts 12 方法)

```ts
// lsp.ts:309-433
init/status/hasClients/touchFile(诊断触发)
diagnostics/hover/definition/references/implementation/documentSymbol/workspaceSymbol
// server.ts(1983):LSP 服务器进程管理(启动/通信/协议)
// client.ts(650):LSP 客户端
// language.ts(121):语言检测;launch.ts:启动配置
```

### 4. 工具集成(设计 4:lsp 工具)

```ts
// tool/lsp.ts:LspTool(实验 flag)——模型可查询符号/诊断
// TODO 注:V2 的 LSP 集成未接线(等 V2 运行时)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | V1 部件级 API(直接写存储) | session.ts:415-466 | ②对比 V2(事件投影) |
| 2 | 子会话(fork/parentID/children) | session.ts:260-279 | ②分支(产品知识库可借鉴) |
| 3 | LSP 12 方法能力面 | lsp/lsp.ts:309-433 | ②语言服务集成 |
| 4 | LSP 工具(实验) | tool/lsp.ts | ②模型可查符号 |

## 面试弹药

- "V1 直接写存储 vs V2 事件投影":updatePartDelta 手写流式更新 vs delta 事件 + 投影器——V2 的确定性来源
- "fork = V1 独有能力":消息处分叉子会话(V2 未实现)——产品④"章节分支"可借鉴
- "LSP 按需启动":工具调用才触发语言服务(init 惰性)——资源控制

## 待深挖

- [ ] lsp/server.ts 的协议细节(1983 行)
- [ ] V1 session 的 BusyError 单执行器语义
- [ ] findMessage 谓词搜索的用途(task 工具)
