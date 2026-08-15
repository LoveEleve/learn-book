# hq18 目标审查器(Goal Judge)— 产品③"独立审查器"蓝本

> 项目:Hermes(hermes_cli/goals.py:1006 judge_goal + tools/kanban_tools.py:233 goal_judge 接线 + GoalContract:332 + _parse_judge_response:863)
> 假设:目标完成的判定不能靠 agent 自报——Hermes 用 auxiliary 模型独立评估"目标是否满足",与 Reasonix Goaleval/BoundedLLM 同构,是"独立审查器"的完整样本。
> 结论:✅ 成立——四态 verdict/契约严格判定/fail-open 语义/parse 与 transport 失败区分/连续性自动暂停/kanban 门接线全具备,产品③"验收器独立审查"直接蓝本。

---

## 一、架构全景:谁判定"目标完成"

```
┌────────────────────────────────────────────────────────────┐
│ 触发:回合后评估(evaluate_after_turn)                        │
│   CLI/gateway 驱动 → GoalManager.evaluate_after_turn        │
│   kanban goal_mode 工人交接前(_goal_mode_handoff_rejection) │
├────────────────────────────────────────────────────────────┤
│ judge_goal(goals.py:1006):auxiliary 模型独立评估            │
│   输入:goal + last_response + 可选(contract/subgoals/       │
│         background_processes 快照)                          │
│   输出:(verdict, reason, parse_failed, wait_directive,      │
│          transport_failed)                                  │
│   verdict:"done"/"continue"/"wait"/"skipped"                │
├────────────────────────────────────────────────────────────┤
│ 输入增强(三路可选,可共存):                                 │
│   contract(GoalContract):严格对照 Verification 判 DONE,     │
│     Constraint 违反拒完成                                    │
│   subgoals(/subgoal 用户加判据):追加进判据列表              │
│   background_processes:等待中的进程(CI poller/build)→ wait  │
│     verdict 命名 pid,停泊循环而非重戳                       │
├────────────────────────────────────────────────────────────┤
│ 失败语义(刻意 fail-open + 可追踪):                         │
│   transport 错 → ("continue", ..., transport_failed=True)   │
│     ——连续 N 次自动暂停(防坏 judge 烧光回合预算)            │
│   parse 失败 → parse_failed=True——连续 N 次自动暂停          │
└────────────────────────────────────────────────────────────┘
```

**与 Reasonix Goaleval 同构验证**:goaleval 独立无工具/无历史/无压缩/缓存隔离 + 四 outcome;judge_goal 独立 auxiliary 调用(temperature=0/固定 prompt/可复现)+ 四态 verdict——**同一独立审查器模式**。Hermes 特有:wait 停泊(进程等待)+ contract 严格判定 + 失败分类自动暂停。

---

## 二、设计 1:四态 verdict(含 wait 停泊)

**位置**:`goals.py:1006-1090`(judge_goal)+ `863-960`(_parse_judge_response)

```
verdict 四态:
- done:目标满足
- continue:未完成(继续)
- wait:等某物(进程/秒数)→ wait_directive {"pid": int} 或 {"seconds": int}
  ——停泊循环而非重戳(CI poller/build 场景);无 pid/秒数 → 降级 continue
- skipped:judge 不可达(空 goal/空响应/aux 不可用)

_parse_judge_response(容错解析):
- 空 → continue + parse_failed=True
- markdown 代码栅栏剥除(```json 包装)
- 全 blob JSON 优先;失败 → _JSON_OBJECT_RE 抓第一个 JSON 对象
- 新形状 {"verdict": ...} 优先;旧形状 {"done": bool} 兼容
- wait 无可用 pid/秒数 → 降级 continue("can't park on nothing")

prompt 输入裁剪:
- goal 截 2000 / contract 截 2500 / response 截 _JUDGE_RESPONSE_SNIPPET_CHARS
- 当前时间注入(judge 需要知道"现在")

测试:
- test_goal_gates.py / test_goals.py(解析/形状兼容/代码栅栏)
```

**产品④映射**:验收器四态语义(完成/继续/等待/跳过)+ 容错解析(栅栏/形状兼容/降级)。

## 设计 2:contract 严格判定(GoalContract)

**位置**:`goals.py:332`(GoalContract)+ `1050-1065`(contract 分支)

```
GoalContract:结构化完成契约(Objective/Verification/Constraints 等)
- 呈现时 contract 优先于 subgoals/plain;contract + subgoals 共存时
  subgoals 追加进 contract 块("judge sees a single source of truth")
- judge 严格对照 Verification criterion 判 DONE;
  Constraint 违反 → 拒绝完成
- draft_contract(goals.py:~1095):objective 展开为结构化契约
  (auxiliary goal_judge 任务,主模型优先 + 缓存安全——是侧 LLM 调用非回合)
  ——失败 → None,调用者回退裸 free-form goal("missing/weak aux model
  绝不阻塞设目标")

测试:
- test_goal_gates.py(contract 判定/Constraint 违反)
```

**产品④映射**:验收器的契约基准——结构化判定标准(Verification 判 DONE/Constraint 拒绝);契约草拟失败回退宽松模式(不阻塞)。

## 设计 3:失败语义分类(parse vs transport,自动暂停)

**位置**:`goals.py:1006-1090`(judge_goal docstring 逐条)+ `863`(parse)

```
两轴失败分类(刻意区分):
- parse_failed=True:judge 调用成功但输出不可用(空/非 JSON)
  ——弱 judge 模型信号 → 连续 3 次自动暂停(防静默烧预算)
- transport_failed=True:judge 根本够不到 API(auth 401/timeout/DNS/
  connection error)→ 连续 5 次自动暂停(永久配置问题信号,如坏 API key)
  ——"Repeated transport failures signal a permanent config problem"

★ 计数互不干扰 + 重置语义(goals.py:1787-1804):
- parse_failed 只在"调用成功但输出坏"时 +1;transport 错时 parse_failed=False
  → 重置——"flaky network doesn't trip the auto-pause meant for bad judge
  models"(网络抖动不误触弱模型暂停)
- transport_failed 同理独立计数
- 两计数持久化到 goal state(consecutive_parse_failures/transport_failures,
  goals.py:557/561)——重启保留

fail-open 语义(刻意):transport 错 → ("continue", ..., True)
  ——"continue" 与真实"还没完成"不可区分是设计接受:
    坏 judge 不烧光回合预算 = 用"可能早退"换"不卡死"

空 goal → skipped("empty goal");空响应 → continue("nothing to evaluate")
```

**正确性价值**:
1. parse/transport 两轴区分——弱模型 vs 配置问题可分辨
2. fail-open 有界(连续 N 次自动暂停)——不是无限 continue
3. 空输入早退(空 goal/空响应不烧调用)

**产品④映射**:验收器失败分级——评估器不可用 = 有界 continue(可追踪自动暂停),不是无限循环也不是假通过。

## 设计 4:kanban 门接线(fail-open 双保险)

**位置**:`tools/kanban_tools.py:233-275`(_goal_judge_available/_goal_mode_handoff_rejection)

```
_goal_judge_available():探测 auxiliary goal_judge 客户端可用
  ——judge_goal 是 fail-open at source("continue" 与真实判定不可区分),
    完成门不能把"不可达"当拒绝(否则未配置/降级 aux 卡死每个
    goal_mode worker——永远关不了自己的任务)
  → 先探测可用性,只在 judge 真可达时强制门
  → 镜像 judge_goal 内部同一客户端查找

_goal_mode_handoff_rejection(task, evidence):
  task.goal_mode + judge 可用 → judge_goal(goal=title+body, last_response=evidence)
  verdict != "done" → 返回 reason(拒绝交接)
  judge 异常 → fail-open 允许交接(日志警告)
  → 与 kanban_stop(终端工具证据)+ GoalGate(验证门)组成三守卫族

测试(test_kanban_goal_mode.py 4 用例):
- test_judge_rejects_premature_completion(早交接被拒)
- test_non_goal_mode_task_skips_gate(非 goal_mode 跳过)
- test_loop_stops_when_worker_already_completed
- test_legacy_db_migrates_goal_columns
```

**正确性价值**:**双保险 fail-open**——judge 不可达时 worker 不被卡死(可用性探测),judge 可达时才强制门(真实判定);异常也允许交接(永不 wedge)。

**产品④映射**:验收器的"门先于判定"接线——审查器不可达不卡死执行;审查器可达才强制门(与 GoalGate"门先于判定"同族)。

## 设计 5:wait 停泊 + 后台进程快照

**位置**:`goals.py:1138`(gather_background_processes)+ `1808-1835`(wait 应用)+ `1012-1020`(wait 语义)

```
gather_background_processes(task_id):
  process_registry.list_sessions 快照;只返回 RUNNING(已退出无物可等)
  永不抛(失败 → [] → 目标循环降级到无 wait 屏障行为)

wait 语义(goals.py:1808-1835,三形态 + 自动恢复):
- wait_on_session(session_id)/wait_on(pid)/wait_for_seconds(秒数)
- ★ 屏障语义:停泊时"回合已计(judge 调用发生了),但无 continuation 触发"
  ——循环经 is_waiting() 短路停住;pid 退出/期限过后下一次
  evaluate_after_turn 自动落过屏障恢复
  ("The loop resumes automatically when the pid exits or the deadline passes")
- wait 无可用 pid/秒数 → 降级 continue("can't park on nothing")
```

**产品④映射**:验收器的等待语义——"在等外部进程"是合法状态,不是继续也不是完成(与 continue 区分)。

---

## 三、与 Reasonix Goaleval 对比(独立审查器家族)

| 维度 | Reasonix Goaleval | Hermes Goal Judge |
|------|-------------------|-------------------|
| 独立性 | 四隔离(无工具/无历史/无压缩/缓存隔离) | auxiliary 独立调用(任务级 provider 路由) |
| 输出 | 四 outcome(complete/continue/blocked/uncertain) | 四态(done/continue/wait/skipped) |
| 严格判定 | — | **GoalContract(Verification 判 DONE/Constraint 拒绝)** |
| 等待 | — | **wait 停泊(pid/seconds)** |
| 失败 | fail-closed(错误=暂停) | **fail-open + parse/transport 两轴自动暂停** |
| 兜底 | — | **双保险(可用性探测 + 异常放行)** |
| 缓存 | 隔离 | 主模型优先 + 缓存安全(侧 LLM 调用非回合) |

**结论**:产品③"独立审查器"= Reasonix Goaleval(四隔离架构)+ Hermes Goal Judge(contract 严格判定/wait 停泊/fail-open 两轴)。**两家族互补:goaleval 是"内容质量"审查(章节),goal_judge 是"目标完成"审查(任务)**。

---

## 四、面试弹药

1. **"continue 不可区分是设计接受"**:fail-open 的 continue 与真实"还没完成"无法区分——但配合连续 N 次自动暂停有界,坏 judge 不烧光回合预算
2. **"parse vs transport 两轴"**:弱模型(parse_failed)vs 配置问题(transport_failed,auth 401/坏 key)——可分辨可分别暂停
3. **"wait 停泊不重戳"**:agent 在等 CI poller/build,judge 返回 wait 命名 pid → 循环停泊;无 pid/秒数降级 continue
4. **"contract 严格判定"**:GoalContract 的 Verification 判 DONE/Constraint 拒绝;subgoals 追加进 contract("single source of truth")
5. **"双保险 fail-open"**:judge 不可达不卡死 worker(可用性探测)——未配置 aux 的 goal_mode worker 永远关不了自己的任务 = 灾难;可达才强制门
6. **"draft_contract 失败回退"**:契约草拟失败 → 裸 free-form goal——弱 aux 模型绝不阻塞设目标

---

## 五、产品映射汇总

| 设计 | 产品③用法 |
|------|---------|
| 四态 verdict | 验收语义(完成/继续/等待/跳过) |
| contract 严格判定 | 结构化验收基准(Verification/Constraint) |
| 失败两轴 | 评估器弱 vs 配置错可分辨 + 自动暂停 |
| fail-open 有界 | 坏 judge 不烧预算(连续 N 次暂停) |
| 双保险接线 | 审查器不可达不卡死执行 |
| wait 停泊 | "在等外部进程"是合法验收状态 |
| draft 回退 | 契约草拟失败不阻塞 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_kanban_goal_mode.py(4)+ test_goal_gates.py(GoalGate/contract 判定)+ test_goals.py(judge 解析)+ test_goal_continuation_drain/test_goal_verdict_send/gateway(回合后评估接线)
> 位置:judge_goal goals.py:1006 / GoalContract :332 / _parse_judge_response :863 / GoalGate :427(同文件三组件:门+审查器+契约)/ kanban 接线 kanban_tools.py:233
