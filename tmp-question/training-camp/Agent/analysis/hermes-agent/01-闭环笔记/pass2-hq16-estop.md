# hq16 紧急停止(ESTOP)— 产品②"可恢复暂停"蓝本

> 项目:Hermes(agent/estop.py 174 行 + cron/scheduler.py:5193 接入 + gateway/kanban_watchers.py:70 + gateway/run.py:15550 + hermes_cli)
> 假设:全局紧急停止 = 只暂停新工作、永不杀在途工作、可恢复——Hermes 用哨兵文件实现,是"可恢复暂停"的完整样本。
> 结论:✅ 成立——哨兵文件/一次性 stat/损坏 fail-safe/每组件日志一次/网关豁免族全具备,产品②"无人值守长跑的安全阀"直接蓝本。

---

## 一、架构全景:暂停新工作,不杀在途

```
hermes pause → 写 $HERMES_HOME/ESTOP(哨兵)
hermes resume → 删哨兵
哨兵存在期间:
  cron 调度跳过到期任务(cron/scheduler.py:tick)
  kanban 调度器跳过 spawn worker(gateway/kanban_watchers.py)
  新 gateway 回合得到"⏸️ Hermes is paused"简短回复(gateway/run.py:_handle_message)
在途工作永不杀——这是暂停新工作,不是 panic/exit。

设计哲学:
- 检查 = 单次 os.stat——调用者可每 tick 跑;无 OS 之外缓存——engage/disengage
  在下一次检查立即生效
- 哨兵体可选 JSON {"reason", "engaged_at"};损坏/空文件仍算启用(fail safe):
  即使 touch ~/.hermes/ESTOP 创建的也暂停
- 移植:gastownhall/gastown estop.go (MIT)
- 相关先例:#26778(/panic 杀/退出语义——刻意不同,本设计可恢复)、
  #44617(中断在途 cron——刻意不在范围)
```

**与 kill/exit 的区别**:/panic 是杀+退出;/pause 是可恢复——"安全阀"不是"紧急出口"。

---

## 二、设计 1:哨兵文件 + 一次性 stat

**位置**:`agent/estop.py:54-71`(sentinel_path/is_engaged)

```
sentinel_path() = _hermes_home() / "ESTOP"(profile-aware,调用时解析)

is_engaged():
  try: return sentinel_path().exists()
  except OSError: return True   # ★ fail safe

★ stat 错误 fail safe:无法确定哨兵是否存在(权限/瞬态 I/O 失败)→ 报告已启用。
  合同 = 暂停必须保持即使哨兵不可读——fail-open 会在文件系统异常时
  静默解除操作者的紧急停止(注释明确)
```

**正确性价值**:单 stat 便宜(每 tick 跑);fail safe 方向正确(宁可误暂停不可误解除)。

> ★ review 补深(2026-08-15 深度 review 发现)——**双层方向不同**:
> - 模块内部 **fail-safe**(is_engaged stat 错误 → engaged,estop.py:70-71)
> - 接入层 **fail-open**(kanban_watchers.py:60-68 注释:"the sentinel gate must
>   not become a new crash surface for the dispatcher"——模块不可导入 →
>   dispatch 继续;cron/gateway 同样 try/except ImportError)
> 平衡:守护自身不能成为崩溃面(接入 fail-open),但一旦可运行就必须
> 保持暂停(内部 fail-safe)。产品实现须保持同平衡——两层方向相反是设计,
> 不是矛盾。

**产品④映射**:知识库全局暂停开关——哨兵文件 + 一次性 stat + fail safe(误暂停可恢复,误解除不可挽回);接入层 fail-open(守护不成为崩溃面)。

## 设计 2:engage/disengage(幂等 + fail-safe 写入)

**位置**:`agent/estop.py:74-101`(engage/disengage)

```
engage(reason):
  写 JSON {"engaged_at": ISO, "reason": reason};失败 → touch 空文件
  ——空/部分哨兵仍暂停(fail safe);幂等(重复 engage 更新文件)
disengage():
  unlink;FileNotFoundError → False(未暂停);OSError → False(解除失败)
get_state():
  哨兵体解析 {"reason", "engaged_at"};损坏/不可读 → 两字段 None
  ——"暂停是权威,元数据不是"(损坏哨兵仍报告已启用)
```

**正确性价值**:engage 的写失败降级 touch(仍暂停);元数据与权威分离。

**产品④映射**:知识库暂停状态——权威(存在性)与元数据(原因/时间)分离;损坏不破坏权威。

## 设计 3:每组件日志一次(check_paused)

**位置**:`agent/estop.py:142-168`(check_paused)+ `41-42`(日志簿)

```
check_paused(component, logger) → bool:
  - 未启用:清 _logged_components[component](重新武装)+ False
  - 启用且首见:记录组件名(日志簿)+ 带 reason 日志一次
  - 启用已见:只 True 不再日志

→ 长暂停不每 tick 刷一行日志;disengage→engage 转换重新武装

组件:cron / kanban(不同调度循环各自计)
```

**正确性价值**:日志纪律——长暂停不刷屏;重新武装让每次 engagement 恰一条日志。

**产品④映射**:知识库暂停/恢复的日志纪律——一次 engagement 一条日志,不刷 tick。

## 设计 4:三消费面(各组件语义)

**位置**:`cron/scheduler.py:5193-5198` + `gateway/kanban_watchers.py:70-73` + `gateway/run.py:15550`

```
1. cron(tick):check_paused("cron") → True → return 0(跳过本次调度)
   ——从不碰在途运行;到期任务等 resume 后下一 tick
2. kanban:check_paused("kanban") → not → spawn worker
   ——调度器不 spawn;已 spawn 的不杀
3. gateway 新回合:paused_reply() → 有 → 简短回复,不跑 agent
   ——★ 豁免族(见设计 5)
```

**正确性价值**:三组件统一"新工作暂停、在途不动"语义;各组件独立日志。

**产品④映射**:知识库多组件统一暂停语义——cron/调度/入口三处同契约。

## 设计 5:gateway 豁免族(暂停不吞关键交互)

**位置**:`gateway/run.py:15560-15610`

```
暂停期间仍放行的 6 类(注释:吞掉会 stall 暂停承诺不碰的工作):
1. 内部事件(is_internal=False 才检查暂停)——后台进程完成事件(在途工作)绕过:
   "pause stops NEW work, it never kills or orphans running work"
2. 已识别的斜杠命令(_resolve_estop_cmd 非 None)——/status /help /new /approve
   保持可用;/pause off 是消息-only 用户的带内恢复路径
3. update_prompt_pending 的响应(更新流程继续)
4. 在飞会话的 steering/中断消息(_is_session_running——
   含 pending clarify + 工具审批,由运行中 agent 持有)
5. 挂起确认(slash_confirm.get_pending)
6. 阻塞审批(has_blocking_approval)

★ 位置语义(笔记 v1 遗漏,run.py:15532-15534):
  - 暂停检查在**认证之后**——未授权发送者保持正常静默/pairing 行为,
    不能探测暂停状态(防探测面)
  - 概括注释:"pause blocks new AGENT turns, not control traffic"

测试(test_estop.py 24 用例):
- test_gateway_internal_events_bypass_estop ✓
- test_gateway_slash_commands_bypass_estop ✓
- test_gateway_pause_command_engages_and_resumes ✓(暂停可自解除)
```

**正确性价值**:暂停 ≠ 静默——控制命令(尤其 /resume)/在途工作交互必须穿透;否则暂停变成困局。

**产品④映射**:知识库暂停的豁免族——"暂停新工作"不包括"拦截解除命令/在途交互"。

## 设计 6:CLI 命令 + status 行(hermes pause/resume + status 显示)

**位置**:`hermes_cli/commands.py:142`(pause 命令)+ `hermes_cli/status.py:115`(_estop_status_line)

```
CommandDef("pause", "Pause new work globally (emergency stop); '/pause off' resumes")
——注意:CLI 的 "resume" 命令是会话恢复(不同语义);ESTOP 解除走
  'hermes resume' 或 '/pause off'(gateway 斜杠)

★ 第五消费点:hermes status 的暂停行(_estop_status_line,status.py:115)
  ——单 stat 检查 $HERMES_HOME/ESTOP;暂停时显示 "paused (reason: ops)" 行

测试:
- test_cli_pause_engages_with_reason / test_cli_pause_idempotent
- test_cli_resume_disengages / test_cli_resume_when_not_paused
- test_builtin_subcommands_include_pause_resume / test_status_line_when_paused
- test_pause_command_registered_for_gateway
```

**产品④映射**:知识库暂停的 CLI 面——engage/resume/status 全命令面。

---

## 三、与四项目对比(可恢复暂停)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes ESTOP |
|------|----|----------|----------|-----|--------------|
| 暂停语义 | — | — | — | — | **只停新工作,在途永不杀** |
| 机制 | — | — | — | — | **哨兵文件 + 单 stat** |
| fail-safe | — | fail-closed | — | — | **stat 错误 = 仍启用(误暂停 > 误解除)** |
| 解除路径 | — | — | — | — | **斜杠/CLI 豁免族(暂停不自锁)** |
| 日志 | — | — | — | — | **每组件每 engagement 一次** |
| 先例 | — | — | — | — | **gastown estop.go 移植;vs /panic 杀退出(刻意不同)** |

**结论**:产品"无人值守长跑的安全阀"参考 = Hermes ESTOP 全案。**与 dsh scale_to_zero(休眠)互补:ESTOP 是操作者手动暂停,scale_to_zero 是自动空闲休眠**。

---

## 四、面试弹药

1. **"暂停 ≠ panic"**:/panic 杀+退出(#26778);ESTOP 可恢复——安全阀不是紧急出口
2. **"fail-safe 方向"**:stat 错误 = 仍启用——fail-open 会在文件系统异常时静默解除操作者紧急停止(注释明确);误暂停可恢复,误解除不可挽回
3. **"哨兵损坏仍暂停"**:touch ~/.hermes/ESTOP 空文件也算——"暂停是权威,元数据不是"
4. **"暂停不自锁"**:豁免族 6 类(斜杠命令尤其 /resume/在飞 steering/审批/确认)——吞掉会 stall 暂停承诺不碰的工作
5. **"单 stat 每 tick 可跑"**:无 OS 之外缓存——engage 下一检查即生效;每组件日志一次不刷屏

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 哨兵 + 单 stat | 全局暂停开关(便宜 + 即时生效) |
| fail-safe | 误暂停 > 误解除 |
| 元数据与权威分离 | 损坏哨兵仍暂停 |
| 每组件日志一次 | 长暂停不刷屏 |
| 三消费面 | cron/调度/入口统一契约(统计 status 为第五消费点) |
| 豁免族 | 暂停不吞控制命令/在途交互 |
| CLI 面 | engage/resume/status |

> 覆盖设计数:6(设计 1-6)
> 测试契约:test_estop.py(24 用例:哨兵/解除/原因时间戳/状态/损坏 fail-safe/回复/日志一次/cron 跳过/kanban 阻止/网关回复/内部绕过/CLI 全命令/状态行/stat 错误 fail-safe/斜杠绕过/pause 命令网关注册)
> 接入点(5 个消费点):cron/scheduler.py:5193 / gateway/kanban_watchers.py:70 / gateway/run.py:15550 / hermes_cli/commands.py:142 / hermes_cli/status.py:115(_estop_status_line)
> 移植:gastownhall/gastown estop.go(MIT);先例 #26778(/panic 杀退出,刻意不同)/#44617(中断在途 cron,刻意出范围)
