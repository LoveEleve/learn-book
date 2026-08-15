# Reasonix 域发现 v51 补充(续扫第三十七轮:jobs 管理器核心)— 2026-08-14

> 承接:v50。本轮:internal/jobs/jobs.go(2,071)——taskmonitor 声称的"唯一真相源",此前只看了头部。
> 结论:**崩溃恢复的所有权证明协议**(只有持 session 租约的运行时能修复废弃 Running 记录)确认——②执行后台任务的高价值机制。

---

## 一、v51 深化确认(jobs 管理器)

| 设计 | 位置 | 要点 |
|------|------|------|
| **所有权证明修复** | jobs.go:1560-1620 | 持久化 Running 记录可能属于其他管理器/进程:**只有持 session 租约(sessionOwnershipProbe)的运行时能修复为 Interrupted**;无证明的观察者 defer(保持 session 可重载供后续绑定);managerOwnerIsLive 活跃检测 |
| **修复失败不发布内存 tombstone** | :1581-1588 | 持久态仍说 Running 时绝不发布内存 Interrupted——**防 live 与机器状态静默分歧**(session 保持可重载) |
| **validatePathSegment 穿越防护** | :359-380 | parentSession/kind 含路径分隔符/控制字符/NUL → 拒绝(#6932:防 `../../etc` 逃逸 temp root 创建文件) |
| **startInvalid** | :381-411 | 验证失败 → 注册为 Failed 观察对象(不启动 goroutine,wg 不受影响) |
| **stalledWarning** | :201-206/466-468 | 每 job 一次停滞警告(基于"job 自有可见输出"非通用活动) |
| **teardownGrace** | :213-223 | 拆解宽限(超时报告 TimedOut) |

---

## 二、关键设计(通用价值)

1. **"所有权证明才可修复"**:崩溃恢复的修复权 = 持租约者——**破坏性修复的授权证明**(无证明只 defer 不破坏)
2. **"防 live/机器状态分歧"**:修复失败绝不发布内存 tombstone——**状态一致性的严格性**
3. **"路径穿越防护"**:parentSession/kind 的路径段校验——**输入防御**(#6932 实证)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v50 | — | 102 | 102 |
| v51 | jobs 管理器核心 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 chat_tui.go(5,569 头部之外)/sessioninbox/disk(431)/cmd 主入口。
