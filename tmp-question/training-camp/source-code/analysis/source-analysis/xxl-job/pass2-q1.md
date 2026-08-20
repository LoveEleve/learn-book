# Pass 2 闭环笔记 XJ-1: Admin 调度主链

## 初始假设
- `JobScheduleHelper` 只是一个定时扫表线程，真正的 trigger 细节在别处，边界可以分很开。

## 验证过程
- `JobScheduleHelper.start()` 同时启动两条线程：
  - `scheduleThread`：每秒扫描 DB 中未来 5 秒内要触发的任务 (`PRE_READ_MS=5000`)，持有 `schedule_lock` 行级锁，做 pre-read + misfire/触发/刷新 next time (`JobScheduleHelper.java:32-127`)。
  - `ringThread`：按秒对 `ringData` 时间轮执行触发 (`JobScheduleHelper.java:129-172`)。
- `scheduleThread` 的三类核心路径：
  1. `now > next + 5s`：misfire，按 `MisfireStrategyEnum` 决定是否 `FIRE_ONCE_NOW`，然后刷新下次时间。
  2. `now > next` 但未超过 5s：立刻触发一次，再刷新；若新的 next 仍落在 5s 内，追加进时间轮。
  3. `next` 落在未来 5s：不立刻触发，只推进时间轮并刷新下一次时间。
- 真正发起执行不是 `JobScheduleHelper` 自己，而是统一调用 `JobTriggerPoolHelper.trigger(...)` (`JobScheduleHelper.java:71-112, 159-162`)。
- `JobTriggerPoolHelper` 根据近 1 分钟内同一 job 的超时次数，把 trigger 任务投递到 fast/slow 两个线程池之一 (`JobTriggerPoolHelper.java:52-98`)。
- 真正构造 `TriggerParam`、选 executor、发 HTTP 请求是在 `XxlJobTrigger.trigger/processTrigger` 中完成 (`XxlJobTrigger.java:39-190`)。

## 结论
`JobScheduleHelper` 不是孤立的“扫表线程”，而是 **调度扫描 + 时间轮 + TriggerPool + XxlJobTrigger** 的前半段主链。XJ-1 必须整体覆盖这四段，不能只讲 Cron 扫描，否则“任务为什么会真正发到 executor”会断链。