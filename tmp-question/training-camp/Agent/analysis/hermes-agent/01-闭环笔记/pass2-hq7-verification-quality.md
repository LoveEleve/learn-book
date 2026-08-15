# hq7 验证与质量(Verify + 证据账本 + GoalGate + 守卫族)— 产品③验收器完整蓝本

> 项目:Hermes(agent/verify/(runner/recipes/environment)+ hermes_cli/verify_cmd.py + agent/verification_evidence.py 698 + agent/verification_stop.py 316 + agent/verify_hooks.py + hermes_cli/goals.py 2,156(GoalGate)+ agent/kanban_stop.py 108 + evals/readtool/)
> 假设:Hermes 有"验证→证据→完成判定"的完整链条(项目验证/验收账本/完成守卫),是产品③自动验收器的三项目中最全面的蓝本。
> 结论:✅ 成立——verify 命令 + 证据账本 + GoalGate + 守卫族 = 验收器的完整组件清单。

---

## 一、架构全景:验证三层 + 完成守卫族

```
┌────────────────────────────────────────────────────────────┐
│ 项目验证:hermes verify(verify_cmd → agent/verify)          │
│   recipe 检测(静态)+ bootstrap/build/test/start+就绪轮询   │
└──────────────┬─────────────────────────────────────────────┘
               │ 结果留痕
┌──────────────▼─────────────────────────────────────────────┐
│ 证据账本:verification_evidence(record_terminal_result/     │
│           record_verify_run:ok/scope full|targeted)        │
└──────────────┬─────────────────────────────────────────────┘
               │ 完成判定守卫
┌──────────────▼─────────────────────────────────────────────┐
│ 守卫族:GoalGate(验证门先于完成声明)                        │
│        verification_stop(编辑后无新鲜证据→提示)            │
│        kanban_stop(任务必须终端工具结束)                   │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:Recipe 检测(静态项目验证)

**位置**:`agent/verify/recipes.py:35+`(移植 grok-cli)

```
Recipe:name/kind/bootstrap/build/test/start/port/readiness
- 检测顺序:Node(lockfile→包管理器+框架)/Python(Django/FastAPI/generic,
  uv/poetry/pipenv)/Go/Rust/Java(Maven/Gradle)/Makefile/docker-compose
- 分层:agent/coding_context 拥有廉价 prompt-time 事实(清单/验证命令
  system prompt 快照);本模块拥有深度运行时 recipe(框架/启动命令/端口/就绪)
```

**关键 1a:双层合并**(verify_cmd.py:88-115)
`_merge_project_facts_commands`:project-facts 的验证命令并入 recipe test 列表(运行时 recipe 不丢 prompt 层承诺的命令);**保存的 manifest 是真相源,detected 不合并**。

**产品④映射**:章节验收的"项目验证"——静态检测 + 分层事实合并。

## 设计 2:验证执行(阶段 + 就绪轮询)

**位置**:`agent/verify/runner.py:242`(run_verify)

```
- 阶段:bootstrap/build/test/start(可选,phase_timeout/ready_timeout)
- 就绪轮询:_poll_readiness(url, timeout, interval)——证明"真的能服务 HTTP"
- 进程组终止(_terminate_process_group)
- PhaseResult.ok/VerifyResult.to_dict(结构化结果)
- _record_evidence(root, recipe, result, partial):
  partial(--phase 子集/--skip-start)→ scope downgrade 为 targeted
  (部分通过绝不当全绿呈现)
```

**产品④映射**:章节验收执行——阶段化 + 就绪探测 + partial 降级语义。

## 设计 3:证据账本(验收结果留痕)

**位置**:`agent/verification_evidence.py:1-698`

```
- VerificationEvidence:command/canonical_command/kind/scope
- kind 分类(_kind_for_command):lint/typecheck/build/format/check/test
- scope(_scope_for_args):targeted(有目标文件)/full
- record_terminal_result/record_verify_run(ok/scope/output)
- 有界:MAX_OUTPUT_SUMMARY_CHARS 2000/MAX_EVIDENCE_AGE_DAYS 30/
  MAX_EVENTS_PER_SESSION_ROOT 100/MAX_TOTAL 10000
- fail-silent:账本问题绝不改变 CLI 退出码
```

**产品④映射**:验收器可审计——ok/scope 留痕 + 部分通过不伪装全绿。

## 设计 4:GoalGate(目标门控——③"门先于判定")

**位置**:`hermes_cli/goals.py:427-545`

```
GoalGate:确定性 shell 命令,在 LLM 判定前必须通过
- 失败门短路判定:有界输出 → 续跑 prompt(agent 对照具体证据迭代)
- 未变工作区跳过:last_failed_fingerprint(git status+HEAD sha256)
  匹配 → 重放失败不重跑(stuck agent 不能烧墙钟重跑同一红色套件)
- 尝试计数:超 max_retries → 目标自动暂停(镜像 turn-budget 暂停)
- 超时杀进程(-1)+ 输出有界 + errors="replace"(Windows emoji/CJK 安全)
- GoalState 持久化/迁移(load_goal/save_goal/migrate_goal_to_session)
```

**产品④映射**:**验收器核心"门先于判定"**——确定性验证在完成声明前,失败证据回注,未变不重跑。

## 设计 5:verification_stop(turn-end 验证守卫)

**位置**:`agent/verification_stop.py:1-316`

```
- 纯策略(从不自己跑检查):把被动验证账本变有界 follow-up
- 触发:模型编辑代码后想立刻结束而无新鲜证据
- 非代码扩展名(文档/散文/数据/标记)无验证行为 → 不提示
- nudge 上限 _MAX_CHANGED_PATHS_IN_NUDGE=8
```

**产品④映射**:"编辑后必须新鲜验证"——完成判定守卫(与 GoalGate 互补)。

## 设计 6:kanban_stop(任务终端工具守卫)

**位置**:`agent/kanban_stop.py:1-108`

```
- kanban worker 必须以 kanban_complete/block 结束
- 模型叙述下一步就停(finish_reason=stop 无工具调用)→ 有界合成 nudge
  让循环继续(否则 dispatcher 判 protocol_violation)
- max_attempts 2(防伪完成转死循环)
- 启用:HERMES_KANBAN_TASK(调度器 spawn 的 worker)
```

**产品④映射**:"完成 = 终端工具证据,不是叙述"——与 #24 收敛性验证直接呼应。

## 设计 7:verify_hooks(验证钩子)

**位置**:`agent/verify_hooks.py:69`

- 验证相关钩子(触发验证时机/结果处理)

**产品④映射**:验收器钩子扩展点。

## 设计 8:评测 harness(真实 agent A/B)

**位置**:`evals/readtool/(runner.py/tasks.py/fixtures.py/report.py)`

```
- 真实 AIAgent 跑确定性 hostile 工作区(9 种 fixture:
  package-lock.json 80K 行 token 陷阱/单行 600KB min.js/近 EOF 探测/
  空文件/Unicode 文件名/FIFO 阻塞/伪装扩展名 PNG)
- 指标:accuracy(子串/正则判题)+ api_turns/tool_calls/read_file_calls/
  total_tokens/wall_s(per-task 均值,绝不求和)
- A/B:baseline vs 候选,reps 3,report.py 对比
- 动机:Command Code 的 read-tool 评测表 Hermes 列有错——真实跑
  ("不信任任何人的能力表")
```

**产品④映射**:验收器自身的评测——真实 agent 跑确定性场景,不信能力表。

## 设计 9:验收器元问题(自证)

**位置**:`verification_evidence` 的 fail-silent + `verify_hooks` + GoalGate 指纹

- 验收结果自身可审计(账本)
- 守卫的守卫:GoalGate 的指纹逻辑可测试(未变跳过语义)

**产品④映射**:**元验收**——验收器自己被检查(D17 五问之五)。

---

## 三、与 Pi/Reasonix 对比(产品③决策输入)

| 维度 | Pi | Reasonix | Hermes |
|------|----|----------|--------|
| 项目验证 | — | — | **verify recipe(静态检测+就绪轮询)** |
| 证据留痕 | reducer 损坏检测 | completion GapKind 8 | **verification_evidence kind/scope** |
| 完成判定 | — | Verdict 四态(Partial 终端) | **GoalGate(门先于判定)+ 守卫族** |
| 完成声明 | — | Claim 分离 | **verification_stop/kanban_stop(叙述不是完成)** |
| 评测 | conformance | e2ebench | **readtool 真实 agent A/B** |
| 自证 | conformance 工厂 | — | **账本可审计 + 守卫可测** |

**结论**:产品③自动验收器 = 
```
项目验证(verify recipe)→ 证据账本(kind/scope)→ 完成判定(GoalGate 门先于判定)
+ 叙述检测(verification_stop/kanban_stop)+ 评测(真实 agent A/B)+ 自证(账本)
```

---

## 四、面试弹药

1. **"GoalGate 门先于判定"**:确定性验证命令在 LLM 完成声明前,失败输出回注修正循环
2. **"未变工作区跳过"**:git 指纹重放失败不重跑——stuck agent 不能烧墙钟
3. **"partial 绝不当全绿"**:--phase 子集 → scope=targeted 降级呈现
4. **"叙述不是完成"**:kanban 必须以终端工具结束;编辑后无新鲜证据 → 提示——完成需要工具化证据
5. **"不信任能力表"**:readtool 用真实 AIAgent 跑确定性场景(Command Code 表的 Hermes 列有错)
6. **"fail-silent 账本"**:账本问题绝不改变 CLI 退出码——观测不干预执行

---

## 五、产品映射汇总

| 设计 | 产品③(自动验收器)用法 |
|------|----------------------|
| Recipe 检测 | 章节验证静态检测+分层合并 |
| 就绪轮询 | "证明真的能跑" |
| 证据账本 | 验收留痕(ok/scope) |
| GoalGate | 门先于判定(核心) |
| verification_stop | 编辑后必须新鲜验证 |
| kanban_stop | 完成=终端工具证据 |
| verify_hooks | 验收器扩展点 |
| readtool 评测 | 验收器自身 A/B |
| 元验收 | 账本可审计+守卫可测 |

> 覆盖设计数:11(设计 1-9 + 1a/2 子设计)
