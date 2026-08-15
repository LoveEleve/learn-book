# hq37 Cron 调度抽象(Cron Scheduler)— 产品②"调度"蓝本

> 项目:Hermes(cron/ 9 文件:scheduler.py 5,432 行 + jobs.py + scheduler_provider.py + lifecycle_guard.py + blueprint_catalog.py + executions.py + monitor.py + notepad.py)
> 假设:到期 job 执行需调度器——Hermes 的 CronScheduler ABC + tick + 执行账本 + 防自杀守卫是"调度"的完整样本(域发现 v7:CronScheduler ABC,触发/执行分离,provider 不重实现;lifecycle_guard 防网关自杀循环)。
> 结论:✅ 成立——ABC 抽象/文件锁 tick/运行中 job 注册/中断/执行账本/防自杀/蓝图目录全具备,产品②"调度"直接蓝本。

---

## 一、架构全景:调度抽象

```
┌────────────────────────────────────────────────────────────┐
│ CronScheduler ABC(scheduler_provider.py:27):              │
│   resolve_cron_scheduler(132)/InProcessCronScheduler(172)  │
│   ——触发/执行分离,provider 不重实现(域发现 v7)            │
├────────────────────────────────────────────────────────────┤
│ tick(scheduler.py:5148):到期检查 + 执行                    │
│   ——文件锁(~/.hermes/cron/.tick.lock)单 tick 进程          │
│   ——运行中 job 注册(get_running_job_ids/try_register)      │
│   ——中断(mark_running_jobs_interrupted/_consume)           │
├────────────────────────────────────────────────────────────┤
│ 执行:run_job(3425)/_run_job_script(2601,声明心跳)/         │
│   _deliver_result(1843,交付)                               │
│   ——ESTOP 检查(见 hq16:暂停跳过调度)                      │
├────────────────────────────────────────────────────────────┤
│ 防自杀:lifecycle_guard.py:                                │
│   contains_gateway_lifecycle_command(98)——拒绝含 gateway   │
│   restart 命令的 job;command-shaped 锚定                   │
├────────────────────────────────────────────────────────────┤
│ 其他:blueprint_catalog(参数化 slot schema 单一真相源)/      │
│   executions(执行审计账本)/monitor/notepad                 │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:调度器 ABC(触发/执行分离)

**位置**:`scheduler_provider.py:27-172`

```
CronScheduler(ABC):调度抽象
resolve_cron_scheduler():解析具体实现
InProcessCronScheduler:进程内实现

——触发(到期判定)与执行分离;provider 不重实现
  (域发现 v7:执行/交付共享,provider 只实现触发)
```

**正确性价值**:ABC 分离——新调度后端只实现触发,执行/交付共享。

**产品④映射**:调度抽象——触发/执行分离(provider 不重实现)。

## 设计 2:tick + 文件锁(单进程)

**位置**:`scheduler.py:5148`(tick)+ 文件锁

```
tick():到期 job 检查 + 执行
  ——文件锁(~/.hermes/cron/.tick.lock)只一个 tick 进程
  ——运行中 job 注册(get_running_job_ids/try_register_running_job/
    release_running_job,486-528)
  ——中断(mark_running_jobs_interrupted/_consume_interrupted_flag,534-586)
  ——ESTOP 检查(见 hq16:暂停跳过调度——"due jobs simply wait for the
    next tick after hermes resume")

执行:_run_job_script_with_claim_heartbeat(2758,声明心跳)
  ——job 执行中保活声明
```

**正确性价值**:文件锁单 tick + 运行中注册 + 中断语义 + ESTOP 衔接。

**产品④映射**:调度单进程 + 运行注册 + 中断 + 暂停衔接。

## 设计 3:防自杀守卫(lifecycle_guard)

**位置**:`cron/lifecycle_guard.py:49-301`

```
GatewayLifecycleBlocked(49):生命周期命令异常
contains_gateway_lifecycle_command(98):检测含 gateway restart 命令的 job
  ——拒绝调度会自杀网关的 job(域发现 v7:防网关自杀循环)
  command-shaped 锚定(不是子串命中——"gateway restart"作命令词才拒)
contains_launchctl_submit_command(191):macOS launchctl 变体
_lifecycle_command_scan_with_data_exemption(287):数据豁免扫描
  (命令作为数据出现在参数里不误拒)
```

**正确性价值**:防自杀(网关 restart 命令 job 拒)+ command-shaped 锚定 + 数据豁免(不误拒)。

**产品④映射**:调度防自杀——生命周期命令守卫(command 锚定 + 数据豁免)。

## 设计 4:蓝图目录(参数化单一真相源)

**位置**:`cron/blueprint_catalog.py`

```
参数化 slot schema 单一真相源(域发现 v39):
  fill_blueprint → create_job kwargs(无第二个 job 引擎;用户永不输原始 cron)
  ——表单/斜杠命令/种子 prompt/深链共享同一 slot schema
```

**正确性价值**:单一真相源(槽 schema)——表单/命令/种子/深链不漂移。

**产品④映射**:调度配置单一真相源——蓝图参数化。

## 设计 5:执行审计账本

**位置**:`cron/executions.py`

```
执行审计账本(域发现 v39):
  非重试队列;中断 attempt 只在 owner 进程证明消失后变 unknown;
  终端态不可变
  ——执行留痕 + 终端态不可变(审计正确性)
```

**正确性价值**:执行留痕 + 终端态不可变(与 delivery_ledger 同族)。

**产品④映射**:调度执行账本——留痕 + 终端态不可变。

---

## 三、与四项目对比(调度)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes cron |
|------|----|----------|----------|-----|-------------|
| 调度器 | — | — | — | — | **CronScheduler ABC** |
| 单进程 | — | — | — | — | **文件锁 tick** |
| 防自杀 | — | — | — | — | **lifecycle_guard(command 锚定)** |
| 账本 | — | — | — | — | **executions(终端态不可变)** |
| 配置 | — | — | — | — | **blueprint(schema 单一真相源)** |

**结论**:产品"调度"参考 = Hermes cron(ABC + 文件锁 + 防自杀 + 账本 + 蓝图)。**与 hq16 ESTOP 衔接(暂停跳过调度)**。

---

## 四、面试弹药

1. **"触发/执行分离"**:CronScheduler ABC——provider 只实现触发,执行/交付共享
2. **"文件锁单 tick"**:~/.hermes/cron/.tick.lock——防多进程双 tick
3. **"防自杀守卫"**:含 gateway restart 的 job 拒(command-shaped 锚定,数据豁免不误拒)
4. **"终端态不可变"**:executions 账本——审计正确性
5. **"蓝图单一真相源"**:slot schema——表单/命令/种子/深链不漂移

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 调度器 ABC | 触发/执行分离 |
| 文件锁 tick | 单进程调度 |
| 防自杀守卫 | 生命周期命令拒(command 锚定) |
| 蓝图目录 | 配置单一真相源 |
| 执行账本 | 留痕 + 终端态不可变 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/cron/ 全目录 579 用例(test_scheduler*/test_agent_scheduling_gate/test_blueprint_catalog 等)
> 位置:tick :5148 / run_job :3425 / _deliver_result :1843 / CronScheduler ABC scheduler_provider.py:27 / lifecycle_guard.py:98 / executions.py / blueprint_catalog.py
> 关联:ESTOP(hq16 暂停跳过调度)/ lifecycle_guard 防自杀
