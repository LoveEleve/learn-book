# OpenCode 域发现(深度探索版)— v10 完成

> 项目:anomalyco/opencode(v1.18.18 tag)
> 版本:v10 — 2026-08-14(全域 53 份笔记,278 设计)
> v1→v2:Review 轮 1(包级全量核对)补 10 域;Review 轮 2(共享包)补 S1-S9;Review 轮 3-6(D15/D16/测试覆盖/一致性)
> v2→v3:Q1-Q18(118)/ v3→v4:Q19-Q24(28)/ v4→v5:Q25-Q30(25)/ v5→v6:Q31-Q36(26)
> v6→v7:Q37-Q42(26)/ v7→v8:Q43-Q46(19)/ v8→v9:Q47-Q50(18)/ v9→v10:Q51-Q53(补丁解析/会话小文件/V1 工具桥,13 设计)

---

## 一、总览:monorepo 结构

```
packages/schema/    ← 叶子:wire/存储契约(Schema leaf,浏览器安全,无运行时)
packages/protocol/  ← HttpApi 分组定义 + middleware 占位(Client 的唯一来源)
packages/server/    ← 权威 HttpApi 实现(handlers/routes/middleware)
packages/core/      ← ★ 领域核心:V2 Session Core(事件溯源执行引擎)+ LLM 适配 + 权限 + 工具
packages/llm/       ← LLM 纯协议层(route/client/executor/auth/cache-policy,不依赖 Core)
packages/opencode/  ← ★ 应用组装:V1 legacy session + CLI + 插件 + MCP + ACP + 控制面
packages/plugin/    ← 插件公开 API 面
packages/client/    ← 生成式 Promise/Effect 客户端
packages/sdk-next/  ← Embedded OpenCode(进程内 host,组合 Client+Core+Server)
packages/sdk/js/    ← legacy JS SDK
packages/tui/ app/ console/ desktop/ ui/ web/ session-ui/  ← 前端(排除视觉)
packages/effect-drizzle-sqlite/ httpapi-codegen/ http-recorder/ stats/  ← 基建
specs/v2/           ← V2 设计规格(权威:session/tools/provider-model/provider-policy/catalog/instructions)
sdks/vscode/        ← IDE 扩展
```

**依赖纪律**(AGENTS.md):Schema → Core/Protocol → Server;Client 永不依赖 Core/Server;sdk-next 组合全部。

**测试纪律**:测试在 `packages/core/test/`、`packages/opencode/test/`、`packages/llm/test/`(行为契约,方法论点:测试即行为契约)

---

## 二、域清单

### 🔴 核心域(V2 Session Core — 事件溯源执行引擎)

**定位**:这是 OpenCode 的灵魂,也是产品②④的主蓝本。执行引擎从"V1 内存循环"迁移到"V2 事件溯源"。

### ⭐ 共享基础设施(被多域依赖的小文件——深挖优先级最高)

| # | 域 | 位置 | 被谁依赖 |
|---|----|------|---------|
| S1 | **Location** | core/src/location.ts(39)+ location-service-map.ts(18)+ location-services.ts(115) | runner/epoch/builtins/权限/工具/指令全域 |
| S2 | **Database** | core/src/database/database.ts(57)+ sqlite.bun(183)/sqlite.node(178) | 所有存储域(event/session/permission/project) |
| S3 | **FSUtil** | core/src/fs-util.ts(274) | 指令/filesystem/工具/Repository |
| S4 | **Wildcard** | core/src/util/wildcard.ts(14)+ opencode/src/util/wildcard.ts(19) | 权限/策略/工具过滤 |
| S5 | **Snapshot** | core/src/snapshot.ts(266) | runner(Step 快照)/revert/facade |
| S6 | **Token 估算** | core/src/util/token.ts | compaction(预算估算) |
| S7 | **Flock 文件锁** | core/src/util/flock.ts(358)+ effect-flock.ts(284) | 多进程写共享资源(global/models-dev/npm/repository-cache/mcp-auth/plugin-install);有测试 | 
| S8 | **KeyedMutex** | core/src/effect/keyed-mutex.ts(45) | 同 key 排队/不同 key 并行(file-mutation/git/plugin);有测试 |
| S9 | **Sync Fence(控制面)** | opencode/src/server/shared/fence.ts(60)+ routes/instance/httpapi/middleware/fence | x-opencode-sync header + event seq 状态同步栅栏(workspace-routing) |

### 核心域清单

| # | 域 | 位置 | 设计决策(一句) | 测试契约 |
|---|----|------|----------------|---------|
| 1 | **EventV2 事件溯源** ⭐共享包 | core/src/event.ts(638)+ event/sql.ts + schema/src/event.ts(125)+ schema/src/session-event.ts(521) | 事件=版本化契约(define/latest/durable);投影器+commit+落库同事务;重放四级校验;owner 三级栅栏;durable/live-only 边界(28+4);Started→Delta→Ended 事件族 | core/test/event.test.ts(1124 行,44 契约) |
| 2 | **SessionRunner 执行引擎** | core/src/session/runner/(llm.ts 432/index/model/to-llm-message/max-steps/publish-llm-event) | run 双层循环(shouldRun/needsContinuation);单 provider turn 7 步;压缩=异常转移(TurnTransitionError);unsettled tools 兜底矩阵;崩溃前失败化 running 工具 | core/test/session-runner.test.ts + session-runner-recorded.test.ts + session-runner-tool-events.test.ts |
| 3 | **SessionInput 持久化收件箱** | core/src/session/input.ts(288)+ schema/src/session-input.ts | 双游标(admitted_seq/promoted_seq);admit 幂等+冲突检测(LifecycleConflict);steer 批量/queue 单个;事件+投影同事务原子提升 | core/test/session-prompt.test.ts |
| 4 | **SystemContext 代数** ⭐共享包 | core/src/system-context/(index.ts 320/registry.ts 49/builtins.ts 50)+ instruction-context.ts(101)+ skill/guidance.ts(76)+ reference/guidance.ts(69) | 六字段源+类型隐藏;initialize/reconcile/replace 三操作;Unchanged/Updated/ReplacementReady/Blocked 四态;Unavailable=stale-while-revalidate;空渲染拒绝;稳定 key 序 | core/test/system-context/(index 18 契约/builtins 4)+ instruction-context.test.ts(8) |
| 5 | **Context Epoch** | core/src/session/context-epoch.ts(174)+ session/error.ts | 每 epoch 一个不可变 Baseline(session_context_epoch 表);snapshot 模型隐藏;ContextUpdated 事件带 commit 回调原子推进;压缩/搬家=epoch 终点 | (无独立测试,覆盖于 session-runner/session-projector) |
| 6 | **SessionProjector 投影器** | core/src/session/projector.ts(458)+ message-updater.ts(397) | 事件→消息投影(seq=事件 seq);SessionMessageUpdater.Adapter(最新未完成 assistant/超时行被新 turn 取代);usage 增量累加;历史合成收件箱记录 | core/test/session-projector.test.ts |
| 7 | **SessionV2 Facade** | core/src/session.ts(486)+ session/(schema/info/store/history/todo/revert/error) | 全部 V2 操作唯一入口:create(投影竞态处理)/list(anchor 游标分页)/messages/message/context/events/history/prompt(wake)/switchAgent/switchModel/revert(stage/clear/commit);shell/skill/compact/wait 暂为 OperationUnavailableError | core/test/session-create.test.ts + session-history.test.ts + move-session.test.ts |
| 8 | **Compaction** | core/src/session/compaction.ts(247) | 双触发(预估:context-buffer / 溢出:provider 拒绝);7 段摘要模板+合并规则;head/recent 切割(keep 8000);Started=attempt/Ended=生效;溢出压缩只一次绝不循环 | core/test/session-compaction.test.ts |
| 9 | **SessionExecution + RunCoordinator** | core/src/session/execution.ts(34)+ execution/local.ts(46)+ run-coordinator.ts(104) | process-global、Session-ID 索引;wake 合并(sliding-capacity-1);interrupt 幂等;drain 无持久身份;LocationServiceMap 路由;noopLayer(只记录不执行) | core/test/session-run-coordinator.test.ts |
| 10 | **ToolRegistry V2** | core/src/tool/(registry.ts 147/tool.ts 162/tools.ts/application-tools.ts + leaves:read/write/grep/glob/bash/apply-patch/edit/webfetch/websearch/skill/todowrite/question) | 不透明 Definition 单 executor;结算七步;stale rejection(身份校验);作用域注册覆盖+关闭即移除;leaf 自断言权限;whole-tool 定义过滤 | core/test/(application-tools/tool-read/tool-bash/tool-edit/tool-apply-patch/tool-webfetch/tool-websearch/tool-write/tool-skill/tool-todowrite/tool-question).test.ts |
| 11 | **PermissionV2** | core/src/permission.ts(310)+ permission/(saved.ts 79/sql.ts)+ schema/src/permission.ts + opencode/src/permission/(index 223/arity 163) | action+resource+effect 规则;evaluate findLast 后写覆盖;三来源管线(agent 配置>saved 批准,deny 优先);ask(预检)/assert(强制等待)双 API;Deferred 异步等待;reject 级联/always 级联+持久化批准表;无沙箱决策 | core/test/permission.test.ts |
| 12 | **ToolOutputStore** | core/src/tool-output-store.ts(211) | 超大输出→托管文件(全局唯一名/扁平目录);bounded preview 留历史;保留失败=结算失败(不发布 lossy success) | core/test/tool-output-store.test.ts |

### 🟢 v2 新增(Review 轮 1:包级全量核对)

| # | 域 | 位置 | 设计决策 | 测试契约 |
|---|----|------|---------|---------|
| 13 | **Policy(工具定义过滤)** | core/src/policy.ts(49) | action/resource/effect 声明式策略;load/evaluate/hasStatements;与 PermissionV2 同族但独立(定义可见性过滤用) | core/test/policy.test.ts |
| 14 | **LocationMutation(路径安全)** | core/src/location-mutation.ts(162) | 路径解析:相对路径必须留在 Location 内;绝对路径在外需 external_directory 审批;不接收 project references;kind 不校验目标类型 | core/test/location-filesystem.test.ts |
| 15 | **GlobalBus(事件出口)** | opencode/src/bus/global.ts(22) | EventEmitter 包装;EventV2Bridge 双通道发布目标;无 id 事件自动补 id(syncEvent.id) | — |
| 16 | **Command(命令系统)** | core/src/command.ts(64)+ schema/command.ts + opencode/src/command/(index/template) | 斜杠命令/init 命令;命令模板 + $ARGUMENTS 展开 | core/test/command.test.ts + config/command.test.ts |
| 17 | **Repository(仓库抽象)** | core/src/repository.ts(214)+ repository-cache.ts(259) | 代码仓库服务 + 缓存;project 发现/克隆/检测 | core/test/repository.test.ts + repository-cache.test.ts |
| 18 | **Shell(执行抽象)** | core/src/shell.ts(226)+ tool/shell/ | shell 选择/preferred;命令执行封装 | core/test/shell.test.ts + tool-bash.test.ts |
| 19 | **Process(子进程)** | core/src/process.ts(261)+ cross-spawn-spawner.ts(507) | 子进程管理;spawner 抽象(cross-spawn 封装);Bash AppProcess 输出捕获限制 | core/test/process/*.test.ts + effect/cross-spawn-spawner.test.ts |
| 20 | **Global(全局配置)** | core/src/global.ts(87) | config 路径/版本/安装元数据;指令域依赖(config/AGENTS.md) | core/test/global.test.ts |
| 21 | **Project V2** | core/src/project/(schema/sql/directories/copy/copy-strategies) | 项目服务:目录发现/复制策略/项目行 | core/test/project.test.ts + project-directories.test.ts + project-copy.test.ts |
| 22 | **opencode 运行时基建** | opencode/src/effect/(instance-state/instance-ref/instance-registry/run-service/runner/bridge/app-runtime/bootstrap-runtime/promise/runtime-flags/config-service) | InstanceState(ScopedCache 按目录隔离实例);makeRuntime(memoMap 去重);EffectBridge(原生回调重入) | core/test/effect/ + opencode/test/effect/ |

### 🟡 支撑域(应用层,原 #13-#34 顺延)

| # | 域 | 位置 | 设计决策 |
|---|----|------|---------|
| 13 | **V1 Legacy SessionPrompt** | opencode/src/session/(prompt.ts 1631/processor.ts 718/session.ts 1018/llm.ts 404/compaction.ts 608/retry/overflow/reminders/run-state/message-v2/todo) | V1 while(true) 单体循环;处理器三态(compact/stop/continue);结构化输出=强制工具;Task 子任务;V2 shadow bridge 过渡 |
| 14 | **EventV2Bridge** | opencode/src/event-v2-bridge.ts(71) | V1 bus 兼容(properties 形状)+ sync 事件双通道;无 location 时从 InstanceRef 附路由实例 location |
| 15 | **MCP** | opencode/src/mcp/(index.ts 1004/oauth-provider 259/oauth-callback 194/catalog 170/auth 163) | 19 方法能力面;5 态状态机;三传输(stdio/StreamableHTTP/SSE);OAuth 设备流;V2 注册表桥接为 follow-up |
| 16 | **Skills V2** | core/src/skill.ts + skill/(discovery.ts 213/guidance.ts 76)+ opencode/src/skill/(index 354/discovery 140) | 目录扫描+isSafeSegment/isSafeRelativePath 安全校验;guidance Context Source(权限过滤,只列名称+描述,正文走 skill 工具) |
| 17 | **Agent V2** | core/src/agent.ts + opencode/src/agent/(agent.ts/subagent-permissions/prompt) | agent 选择/权限继承;skill-guidance 按 agent 过滤 |
| 18 | **LLM 包** | packages/llm/src/(llm.ts 186/route/client.ts 436/executor 385/auth 156/protocol 84/cache-policy 111/tool.ts 253/tool-runtime 78/provider-error 43) | 纯协议层;单 llm.stream 入口;promptCacheKey;isContextOverflowFailure;recorded golden 测试 |
| 19 | **Provider/Auth/Credential** | core/src/provider.ts + opencode/src/provider/(provider/auth/transform/model-status/error)+ core/src/credential.ts + oauth/page.ts(276)+ github-copilot/ | 供应商适配;凭证存储(credential 表);OAuth 授权页;Copilot provider |
| 20 | **Catalog/Model** | core/src/catalog.ts(301)+ model.ts + models-dev.ts(266)+ aisdk.ts(235) | Generation Controls vs Model Request Options 分区;models.dev 适配;AI SDK 兼容 |
| 21 | **Plugin V1+V2** | opencode/src/plugin/(loader/install/index/meta/pty-environment)+ packages/plugin/src/v2/ | 插件加载生命周期;V2 窄能力(Tools 注册) |
| 22 | **ACP** | opencode/src/acp/(service 1105/event 421/tool 364/permission 254/session 232/usage 243/content 等 12 文件) | Agent Client Protocol 服务端(Zed/编辑器集成) |
| 23 | **Server/Protocol/Client/SDK** | packages/server/src/(api/handlers/routes/middleware/auth/cors)+ opencode/src/server/(server 226/routes)+ packages/protocol/src/(api.ts 86/groups/middleware/errors)+ packages/client/ + sdk-next/ + sdk/js/ | 权威 HttpApi;SSE;SDK Contract IR;Promise/Effect 双发射器;Embedded OpenCode |
| 24 | **Config** | opencode/src/config/ + core/src/config/(agent/attachments/command/compaction/experimental/formatter/lsp/markdown/mcp/plugin/provider/reference/tool-output/watcher) | 配置 schema 自导出模式;fromConfig 权限规则转换 |
| 25 | **Observability** | core/src/observability/(otlp 79/logging 71)+ packages/stats/ + http-recorder/ | OTLP 导出;统计;HTTP 录制(cassette/redaction) |
| 26 | **Snapshot/PTY/Revert** | core/src/snapshot.ts + core/src/pty/ + pty-ticket + session/revert.ts(121) | 文件快照 diff;PTY 会话/票据;会话回滚(stage/clear/commit 事件驱动) |
| 27 | **Database/Storage** | core/src/database/(schema.gen 274/sqlite.bun 183/sqlite.node 178/migration)+ opencode/src/storage/(storage/schema) | SQLite bun+node 双实现;Drizzle schema;迁移 |
| 28 | **CLI** | opencode/src/cli/(bootstrap/upgrade/network/effect/tui + cmd/run.ts 1011 + cmd/*) | CLI 入口/运行/升级 |
| 29 | **LSP** | opencode/src/lsp/(client/diagnostic/language/launch/server/lsp) | 语言服务集成 |
| 30 | **Question** | core/src/question.ts + opencode/src/question/(index/schema) | 用户提问机制(会话内) |
| 31 | **Git/Worktree** | opencode/src/git/ + worktree/ + core/src/git.ts | git 封装/worktree |
| 32 | **Sync(legacy)** | opencode/src/sync/(README.md 179/schema 11) | 事件溯源同步设计文档(单写者+投影器);已迁移到 EventV2 |
| 33 | **Background/Control-Plane/Account** | core/src/background-job.ts + opencode/src/background/ + control-plane/(workspace/adapters/dev)+ account/(account/repo/schema/url) | 后台任务;云工作区;账号 |
| 34 | **Share/Installation/Flag** | opencode/src/share/ + core/src/share/ + installation/ + core/src/flag | 会话分享;安装;功能开关 |

### 🟢 扫描域(简扫)

| 域 | 位置 | 说明 |
|----|------|------|
| TUI 键盘/输入 | packages/tui/src/(keymap/input/runtime/terminal) | 终端输入(排除视觉组件) |
| httpapi-codegen | packages/httpapi-codegen/ | API 代码生成 |
| effect-drizzle-sqlite / effect-sqlite-node | packages/ | SQLite Effect 封装 |
| enterprise | packages/enterprise/ | 企业版 |

---

## 三、排除清单

| 排除 | 原因 |
|------|------|
| packages/app/ console/ desktop/ web/ ui/ session-ui/ 视觉组件 | 前端 UI 视觉层(保留 server 语义) |
| packages/tui/ 组件渲染 | 终端视觉(保留 keymap/input/runtime) |
| packages/stats/ app 前端 | 统计 UI |
| packages/llm/src/providers/* | 供应商同构实现(保留 route/executor 抽象) |
| packages/llm/test/recorded-* | 录制黄金测试(如有需要单列) |
| sdks/vscode | IDE 扩展(可作集成参考) |
| script/ infra/ perf/ | 构建/发布 |
| packages/web/ docs | 文档站 |
| patches/ | 补丁 |

---

## 四、关键发现(深度探索)

### 发现 1:两套执行引擎(迁移期架构,产品启示核心)
- **V1**(opencode/src/session/prompt.ts 1631 行):内存循环 + 事件发布,处理器链三态驱动
- **V2**(core/src/session/):事件溯源执行引擎——收件箱 + 事件日志 + 投影器
- AGENTS.md 明确:"Do not bridge through legacy SessionPrompt.loop(...) or delegate orchestration to an in-memory tool loop"
- **产品②④直接蓝本:执行循环状态在 DB(收件箱),不在内存**

### 发现 2:共享包(深挖优先级)
- 见上文"⭐ 共享基础设施"小节(S1-S9:Location/Database/FSUtil/Wildcard/Snapshot/Token/Flock/KeyedMutex/Sync Fence)
- **已深挖**:EventV2(44 契约)+ SystemContext(30 契约)
- **待深挖**:ToolOutputStore(工具注册表依赖)、SessionStore(runner/execution/facade 依赖)、Location(全域依赖)

### 发现 3:事件溯源三支柱(与 Pi 一致,更系统化)
1. session_input 收件箱(admitted/promoted 双游标)
2. event_sequence + event 表(aggregate seq 单调,唯一索引硬约束)
3. 投影器同事务原子写(消息 + promoted 标记)
4. 版本化事件定义(schema 演进)

### 发现 4:Context Epoch = 上下文即状态
- Baseline(不可变,provider-cache 前缀)+ Snapshot(模型隐藏,比较)+ Mid-Conversation System Message(持久化差异)
- reconcile 返回 exactly one action 的代数设计
- Unavailable = stale-while-revalidate(非移除)

### 发现 5:权限系统完整语义
- 规则评估(Wildcard,findLast 后写覆盖)+ 运行时询问(Deferred 异步)+ 持久化批准(permission 表)
- reject 级联(同 session 全拒)/ always 级联(同 session 匹配自动过)
- deny 优先于 allow;无沙箱(宿主权限,授权层而非隔离层)

### 发现 6:MCP 差异点(对比 Pi)
- Pi:不支持 MCP(架构决策);OpenCode:完整支持(19 方法 + OAuth 设备流 + 三传输)
- V2 注册表桥接为 follow-up(核心工具 AGENTS.md 明示 gap)

### 发现 8:理论层检查(D15)结果

| 理论模式 | OpenCode 实现 | 位置 |
|---------|--------------|------|
| 事件溯源 | EventV2(aggregate seq + 投影器同事务) | event.ts |
| 栅栏/Fencing | owner 三级栅栏(claim/strictOwner)+ 控制面 Sync Fence | event.ts + server/shared/fence.ts |
| 文件锁(多进程互斥) | Flock(flock/effect-flock,358+284 行) | util/flock.ts |
| 按 key 互斥 | KeyedMutex(同 key 排队/不同 key 并行) | effect/keyed-mutex.ts |
| 日志全序 | aggregate 单调 seq + 唯一索引硬约束 | event/sql.ts |
| 快照隔离类 | Context Epoch 的 immutable baseline + snapshot 比较 | context-epoch.ts |
| 状态机不变量 | reconcile 四态代数 + 事件 seq 连续性校验 | system-context + event.ts |
| 乐观并发 | 收件箱幂等重试(onConflictDoNothing + 重读) | session/input.ts |
| 双阶段(Prepare/Commit 类) | 事件事务内 commit 回调(失败全回滚) | event.ts:236-352 |

### 发现 9:正确性问题检查(D16)结果

| 正确性机制 | 位置 | 防止的 bug |
|-----------|------|-----------|
| 事件 seq 连续性校验(die) | event.ts:294-302 | 事件乱序/丢失 |
| 重放分叉检测(deepStrictEqual) | event.ts:262-290 | 静默覆盖历史 |
| 事件 ID 全局唯一 | event.ts:303-315 | 事件重复 |
| 投影器失败 = 事件不提交 | event.ts:236-352 | 状态与日志不一致 |
| 空渲染拒绝(requireText) | system-context/index.ts:309-312 | 空白上下文 |
| 收件箱幂等 + LifecycleConflict | session/input.ts:37-115 | 重复 admit/冲突覆盖 |
| tool stale rejection | tool/registry.ts:60-61 | 执行错误实现 |
| 输出托管保留失败 = 结算失败 | tool-output-store.ts | lossy success 伪装完整 |
| 崩溃残留工具失败化 | runner/llm.ts:119-139 | 副作用静默重放 |

### 发现 7:JD 关键词覆盖

| 关键词 | 覆盖 | 位置 |
|--------|------|------|
| 可观测 7/10 | ✅ OTLP/logging/结构化事件 | core/src/observability + event.ts |
| 权限 5/10 | ✅✅ 完整系统 | permission.ts + opencode/permission |
| 事件溯源/可控可查 | ✅ EventV2 + 收件箱 | event.ts + input.ts |
| 上下文管理 6/10 | ✅ Context Epoch + Compaction | context-epoch + compaction |
| MCP 6/10 | ✅ client + OAuth | opencode/src/mcp |
| Skills 5/10 | ✅ discovery + guidance | core/src/skill |
| Sandbox 6/10 | ⚠️ 无沙箱(架构决策) | specs/v2/session.md |
| 评测 8/10 | ⚠️ 无独立 evals(对比 Pi) | perf/ |
| 隔离/审计 | ⚠️ 授权层而非隔离层 | permission + specs |

---

## 五、闭环笔记清单(已完成,146 设计)

### 第一轮(q1-q18,118 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q1 | event-v2 | 11 | 定义层三件套/提交协议(投影+commit 同事务全回滚)/错误策略三分/无窗口 tail/重放四级校验/owner 三级栅栏/28+4 durable 边界/事件族模式/版本演进/V1 桥接 |
| q2 | system-context | 7 | 六字段源+惰性渲染/三操作代数/四态 reconcile/Unavailable 全路径/空渲染拒绝/指令域三竞态保护/Registry 稳定序 |
| q3 | session-runner | 7 | 双层循环/runTurn 7 步/发布管线状态机/异常转移压缩/兜底矩阵(7 场景)/崩溃恢复/max-steps |
| q4 | session-input | 8 | 双游标/admit 幂等+并发/冲突检测/steer-queue+cutoff/投影原子/重放不调度/legacy 合成/resume 三态 |
| q5 | context-epoch | 8 | 四阶段/基线不可变(变化=时间序消息)/移除语义/切换保留基线/Unavailable/压缩=新世代/初始化阻塞/agent system 组合 |
| q6 | compaction | 8 | 双触发/7 段模板+合并规则/head-recent/媒体不嵌入 base64/序列化五类型/attempt-生效契约/溢出一次/新 Epoch |
| q7 | permission | 7 | 规则模型/三来源管线(deny 优先)/ask-assert/Deferred 等待/级联/持久化批准/无沙箱 |
| q8 | tools | 6 | 不透明值(WeakMap)/六步结算/withPermission/输出托管(保头保尾)/结算七步+stale/双注册 |
| q9 | projector | 7 | 事件事务内投影/消息 CRUD+usage/Assistant 生命周期/工具状态机/delta-定稿/直通消息/忽略清单 |
| q10 | session-facade | 5 | 18 操作/历史过滤规则(compaction+baseline)/seq 游标分页/唯一索引/revert 三操作 |
| q11 | run-coordinator | 7 | Entry 四字段/run 加入/唤醒合并/successor 语义/interrupt 幂等/trampoline/Location 路由 |
| q12 | v1-legacy | 7 | while(true) 单体/处理器三态/重试策略(退避+jitter)/usable 预算/overflow 联动/结构化输出=强制工具/deny-blocked |
| q13 | llm | 6 | Route 四轴/三入口/generateObject 强制工具/工具调度三径/时间序 system 降级/录制测试 |
| q14 | skills-agent | 5 | 发现六重校验/版本化原子更新/指导源权限过滤/正文走工具/agent 默认+继承 |
| q15 | acp | 4 | 能力面声明/会话操作映射/prompt 转换/MCP 注册 |
| q16 | snapshot | 6 | git 树内容寻址/六操作/operation 化错误/Step 快照对/revert/PTY env 合并 |
| q17 | shared-packages | 5 | Flock(breaker+心跳+token)/KeyedMutex/Sync Fence/Token 估算/InstanceState |
| q18 | policy-location | 4 | Policy(无 ask)/三层路径校验/外部目录审批/解析审批分离 |

### 第二轮(q19-q24,28 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q19 | builtin-tools | 5 | 统一四步权限序列/bash 超时=结构化结果/apply_patch 三阶段+CAS+部分失败报告/read 类型分派+图像/写 BOM 保留 |
| q20 | mcp | 6 | 19 方法+5 态/三传输/完整 OAuth 授权码流(动态注册)/pending 暂存 commit/凭证 URL 绑定/回调服务器 |
| q21 | protocol-server | 5 | HttpApi 18 组/端点契约+SSE/不透明游标(base64url)/SDK Contract IR 双发射器/错误划分 |
| q22 | database | 4 | 双运行时实现/WAL+外键+Semaphore/自管迁移日志+旧日志播种/非空拒绝初始化 |
| q23 | message-translation | 4 | 七类型翻译表/模型一致性控制元数据(reasoning 降级)/hosted 工具内联/compaction checkpoint 模板 |
| q24 | move-plugin | 4 | 同项目强制/git 变更集搬运(capture-apply-discard)/Moved 事件联动/插件 plan-resolve-load |

### 第三轮(q25-q30,25 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q25 | file-mutation-git | 4 | 五写操作(create 排他/BOM 保留/CAS/幂等 remove)/CAS=KeyedMutex 锁内比较+写入/git 七组操作+仓库级锁/change 捕获-应用-丢弃 |
| q26 | remaining-tools | 5 | 三类权限资源模式(文件系/搜索系/会话系)/edit 精确替换+CRLF+CAS 错误/技能正文按需加载/URL 即资源/会话内工具 |
| q27 | question-command-state | 3 | Question 批量+Deferred+Location 归属/Command State 化/State 可重放转换(transform-reload-batch) |
| q28 | config | 4 | 三来源合并(global<project<.opencode)/V1 自动迁移/Policy 反向顺序/打开缓存+宽松解析 |
| q29 | v1-message-model | 5 | message/part 双表/9 种部件转换/媒体兼容矩阵(provider 表驱动)/错误跳过/分支压缩 vs checkpoint |
| q30 | server-wiring | 5 | 双路由面(webHandler/createRoutes)/Scope 生命周期+幂等停止/ConfigProvider 每监听器重建/init-projectors/CLI 编排 |

### 第四轮(q31-q36,26 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q31 | observability | 4 | runID 关联键/OTLP logs+traces 双通道(懒加载+AI SDK span 打通)/结构化日志扁平化/录制密钥检测 |
| q32 | location-services | 4 | 34 服务 group(Location 级 vs 全局级)/LayerMap 惰性实例化+60 分钟回收/get(ref) 路由/Node+LayerNode hoist |
| q33 | v1-tool-registry | 5 | 20 内置+custom/模型过滤(usePatch)/插件 Zod 兼容边界(双世界转换)/tool.definition 钩子/V1 vs V2 差异 |
| q34 | v1-llm | 5 | 双运行时 seam+回退/repairToolCall(大小写+invalid)/workflow 会话预批防循环/tracer Proxy/请求准备管线 |
| q35 | project-workspace | 4 | 项目 ID 三源(remote>cached>root)/非 git=global/workspace 标识/repository 引用解析+缓存 |
| q36 | v1-session-lsp | 4 | V1 部件级 API(直接写存储 vs 事件投影)/子会话 fork/V2 未实现/LSP 12 方法/工具集成 |

### 第五轮(q37-q42,26 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q37 | effect-runtime | 4 | InstanceState 按目录缓存+手动失效/Runner 四态(Idle-Running-Shell-ShellThenRun)/EffectBridge 回调重入/makeRuntime memoMap |
| q38 | v1-compaction | 5 | select(tail 预算+splitTurn)/prune(工具输出擦除 40k 保护)/process(compaction agent+插件钩子)/overflow 重放/复用 core 模板 |
| q39 | aisdk-catalog | 4 | 钩子链两阶段(sdk/language)+Scope 注册/双缓存/fetch 包装(信号聚合+chunk 超时+请求体修复)/Catalog 政策 |
| q40 | filesystem-ripgrep | 5 | 平台保护清单(隐私目录)/Ripgrep 进程原语(不掺权限)/Watcher 平台绑定+降级/FFF 双实现/fs-util 纯函数 |
| q41 | auth-credential | 4 | Auth JSON 0600+环境注入/OAuth 刷新持久化/Credential 每集成一凭证(事务替换)/解码容错 |
| q42 | control-plane-account | 4 | Workspace 云同步(SSE+历史重放)/Session Warp/WorkspaceContext AsyncLocal/设备码登录+令牌刷新 |

### 第六轮(q43-q46,20 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q43 | message-schema | 5 | 8 类型消息联合(type 判别)/工具 4 态+pending 输入是 string/双 metadata(call/settlement)/Prompt 附件 source 审计/收件箱结构 |
| q44 | agent-permissions | 5 | Agent Info 15 字段/默认权限集(.env ask 保护+外部目录白名单)/子 agent 继承 deny+禁 task/自动生成/专用 prompt 模板 |
| q45 | codegen-server | 4 | compile→IR(portable+requiredForClient)/SessionLocation 按会话路由/Basic 认证+query token+PTY 票据豁免/handler 错误映射+游标构造 |
| q46 | provider-peripheral | 6 | Provider 双 API(AISDK/Native)/Schema 降级(OpenAI 兼容)/Integration 认证方法族/NPM sanitize/BackgroundJob 输出分片/Share |

### 第七轮(q47-q50,20 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q47 | layer-node | 5 | Node 三 kind+类型级依赖检查/双 tag(global/location 方向强制)/hoist 裁剪/compile+replacement 校验/service-use 惰性 Proxy |
| q48 | event-manifest | 4 | 三层分类(foundation/feature/扩展)/公开 vs 全量(ServerDefinitions)/durable 单独清单/V1-only 隔离 |
| q49 | core-util | 4 | Token 4 字符估算(统一预算)/Wildcard 匹配引擎(尾随星号可选)/Slug 会话名/Hash+Glob |
| q50 | v1-internals | 7 | RunState 每会话 Runner/指令=历史路径提取/Summary git diff/模型家族提示词模板/ModelsDev 目录/Flag 访问时求值/V1 错误族隔离 |

### 第八轮(q51-q53,14 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q51 | patch-parser | 5 | 标记式格式(Add/Delete/Update)/严格解析/四级宽容匹配(exact→Unicode 规范化)/derive(BOM+倒序应用)/File.Diff |
| q52 | session-files-state | 6 | Info 映射/Todo 全量替换事务/解码错误契约/schema 门面/State 完整/Reference 命名引用 |
| q53 | v1-tools-system | 3 | MCP 资源工具化(10MB+mime 白名单)/模型家族提示词分发/模式切换提醒 |

**合计:278 设计**(53 份笔记;v10 修正:q43-q46 实际 20、q47-q50 实际 20、q51-q53 实际 14,此前 274 为对账错误)
