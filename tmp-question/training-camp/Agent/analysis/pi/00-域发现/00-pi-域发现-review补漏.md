# Pi 域发现深度 Review 补充报告(2026-08-14)

> 触发:Hermes 深度 review 后复盘——Hermes 从 27 域被逼到 80 域(用户 5 次要求深扫),Pi 只做了 6 轮 review 到 59 域。用同一方法(文件体量排序 + 覆盖核对)对 Pi 复测。
> 结论:**Pi 域发现确实存在系统性遗漏**——按体量排序后找到多个 load-bearing 文件完全未进域清单。

---

## 一、复测方法

```
1. 文件体量排序(find + wc -l,排除 test/tui/examples/scripts)
2. 对 top 40 源码文件逐个 grep 域发现文档,核对覆盖率
3. 对核心目录(agent/src/harness/、coding-agent/src/core/)全文件核对
4. 记录:遗漏文件 + 设计价值判断
```

---

## 二、确认的遗漏(按严重度排序)

### 🔴 高价值遗漏(设计决策明确,产品映射直接)

| # | 文件 | 体量 | 设计价值 | 产品映射 |
|---|------|:--:|---------|---------|
| 1 | **harness/reducer.ts** | 667 | **事件归约器**:RecordLogCorruption 12 种损坏原因(单写者记录协议的状态机不变量:multiple_open_operations/non_consecutive_attempt/tool_call_mismatch/duplicate_tool_invocation…);restore 必须拒绝损坏态而非修复——**单写者协议的正确性证明** | ④知识库(事件溯源重放的损坏检测) |
| 2 | **modes/interactive/interactive-mode.ts** | **6,436** | Pi 最大源文件!域发现只说"modes 支撑域一句话";interactive-mode 是完整交互形态(TUI 会话/流式/编辑/审批) | ①对齐(交互形态) |
| 3 | **core/extensions/runner.ts** | 1,236 | 扩展运行时:BeforeAgentStart/BeforeProviderRequest/ContextEvent/CompactOptions 等**完整事件钩子契约** + 生命周期 | ③验收器(钩子契约) |
| 4 | **core/event-bus.ts** | ~60 | 事件总线抽象(emit/on/clear + handler 错误隔离) | ②执行(事件分发) |
| 5 | **harness/events.ts** | 102 | harness 事件类型(域发现 0 覆盖) | ②执行 |

### 🟡 中价值遗漏(支撑域缺细节)

| # | 文件 | 体量 | 说明 |
|---|------|:--:|------|
| 6 | core/agent-session-runtime.ts + services.ts | — | session 创建运行时 + 服务绑定(cwd 边界诊断) |
| 7 | core/diagnostics.ts | — | **ResourceCollision 检测**:extension/skill/prompt/theme 资源名冲突,winner/loser 溯源 | 
| 8 | ai/src/api/openai-codex-responses.ts | 1,647 | Codex Responses API 适配(域发现只提 api 层一句话) |
| 9 | ai/src/api/bedrock-converse-stream.ts | 1,188 | Bedrock 流式适配 |
| 10 | ai/src/api/mistral-conversations.ts | 931 | Mistral 适配 |
| 11 | ai/src/api/google-vertex.ts | 596 | Vertex 适配 |
| 12 | core/provider-attribution.ts | — | provider 归因 |
| 13 | core/cache-stats.ts / timings.ts | — | 缓存统计/耗时 |
| 14 | core/source-info.ts / pi-manifest.ts | — | 包信息/清单 |

### 🟢 低价值遗漏(组件/边缘)

- modes/interactive/components/(tree-selector 1,427/session-selector 1,031/config-selector 942/settings-selector 893)
- modes/interactive/theme/theme.ts(1,335)
- harness/result.ts(63)/system-prompt.ts(34)/proxy.ts/stream-fn.ts

---

## 三、为什么 Pi 漏了而 Hermes 被逼出来了

| 维度 | Pi(v1-v6) | Hermes(v1-v11) |
|------|-----------|----------------|
| review 轮次 | 6(agent 自发) | 11(用户 5 次强制) |
| 方法 | 按目录感觉扫描 | 按文件体量排序 + 覆盖核对 |
| 结果 | 59 域,漏 4 个 load-bearing | 80 域,全部 load-bearing 覆盖 |
| 教训 | **"感觉扫完了"= 假收敛** | 穷尽性 = 文件清单对账,不是感觉 |

**核心教训(v11 已在 Hermes 域发现记录,Pi 实证加强)**:
1. **体量排序是穷尽性检查的第一工具**——interactive-mode.ts 6,436 行是 Pi 最大文件,6 轮 review 都没注意到
2. **#24 在域发现层面的实证**:Pi 声称"59 域收敛",实际漏了 reducer.ts(事件溯源正确性核心!)等 load-bearing 文件
3. **reducer.ts 的遗漏尤其严重**:它正是产品④知识库"重放即真相"的**损坏检测层**(12 种损坏原因枚举),是 Pi 参考架构应该抄的核心,却完全没进分析

---

## 四、处置建议

- [ ] Pi 域发现补 v7(体量排序复测):+4 高价值域(reducer/interactive-mode/extensions-runner/event-bus)
- [ ] Pi 闭环笔记补:reducer.ts 的损坏检测协议(产品④事件溯源重放的正确性层)
- [ ] 方法论 D13 补充:**域发现 pass 1 必须含"文件体量排序 top N 逐一核对",禁止按目录感觉扫描**
- [ ] 参考架构检查:Pi 参考架构是否引用 reducer 损坏检测?(若无 → 补)
