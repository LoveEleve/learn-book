# Reasonix 域发现 v15 补充(续扫第一轮:taskmonitor 任务监视层)— 2026-08-14

> 承接:v14(101 域收官声明)。用户继续追问——打开之前未细看的 internal/taskmonitor。
> 结论:**"收官声明"再次证伪**,taskmonitor 有 3 个高价值设计(纯观察层/双状态分离/控制版本化),新增 1 个域 + 深化。

---

## 一、v15 新增/深化

| # | 域 | 文件 | 设计要点 | 产品映射 |
|---|----|------|---------|---------|
| 102 | **任务监视 TaskMonitor** | internal/taskmonitor/(model/recorder/control/jsonstore/tmux) | **TaskState 7 态**(queued/running/waiting/succeeded/failed/cancelled/stale)+ **RuntimeState 独立**(alive/exited/unknown);**纯观察层**("不实现第二状态机,以 jobs.Manager 为唯一真相源");TaskSnapshot/TaskEvent JSON 树存储;控制操作版本化(ControlResult.schema_version/version);JobKiller 路由(sessionID+jobID 定位);tmux 集成(参数数组防注入);字段长度上限(256/1024 防内存耗尽) | ②执行(任务监视,与 #24 收敛相关) |

**深化 2 设计**:
1. **"纯观察层不实现第二状态机"**:taskmonitor 读 jobs.Manager 快照,不解析内部文件、不维护并行状态机——**观察与真相源分离**(防状态漂移)
2. **"运行时与生命周期状态独立"**:requeued 任务 = queued 生命周期 + exited 运行时——**两个正交状态维度**

---

## 二、关键设计(通用价值)

1. **"纯观察层"三项目对比**:Reasonix taskmonitor(不实现第二状态机)↔ Hermes 活动观察契约(session_activity observation-only)↔ Pi ———**"观察层不得有第二真相"是监控架构的共同原则**(与 Hermes session_stall"消费共享活动观察契约"直接呼应)
2. **"双状态维度"**:生命周期(queued→succeeded)与运行时(alive/exited)正交——**状态建模避免把两个维度混成一个**
3. **"字段长度上限"**:自由文本字段限长防内存耗尽——**输入防御**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v14 | — | 101 | 101 |
| v15 | taskmonitor 任务监视层 | +1 | **102** |

> 教训:v14"101 域收官"声明被证伪——taskmonitor 从未打开。继续:next 轮 control 剩余(approval 670/slash 671/goal 1,051)。
