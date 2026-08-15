# Hermes 域发现 v30 补充(续扫第十九轮:kanban_stop 守卫/agent 剩余抽查)— 2026-08-14

> 承接:v29。本轮:agent/kanban_stop + 剩余抽查。
> 结论:**kanban_stop 的"叙述性结尾"检测确认**(与 #24 收敛性相关),无新域但深化。

---

## 一、v30 深化确认

### kanban_stop(agent/kanban_stop.py)

| 设计 | 位置 | 要点 |
|------|------|------|
| **叙述性结尾检测** | :1-30 | kanban worker 必须以 kanban_complete/block 结束;**模型叙述下一步就停(finish_reason=stop 无工具调用)** → 返回**有界合成 nudge** 让循环继续而非退出(否则 dispatcher 判 protocol_violation);max_attempts 2 |
| **启用条件** | HERMES_KANBAN_TASK(调度器 spawn 的 worker)且未显式禁用 | |

**价值**:这是"agent 声称完成但实际没完成"的检测之一——模型说"Let me write the report now"就停 = 伪完成。**与 #24 收敛性验证同思想**(完成声明不可信,需要终端工具证据)。

### 其余确认

- learning_graph_render:学习时间线终端渲染(桌面星座的终端版本——纯 stdlib)
- file_safety:共享文件安全规则(tools+ACP shim 共用,HERMES_HOME 防误删)
- nous_subscription/sessions_cmd:订阅特性/会话命令

---

## 二、关键设计(通用价值)

1. **"终端工具证据"**:kanban 完成 = kanban_complete/block 工具调用,不是叙述——**完成判定的工具化**(产品③验收器的直接映射)
2. **"有界 nudge 防死循环"**:nudge 有上限(2)——**防伪完成转死循环**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v29 | — | 81 | 81 |
| v30 | kanban_stop/抽查 | +0(深化 1 设计) | **81**(深化) |

> 继续:next 轮 tests 契约补查(此前 v3 只扫了 verify/技能测试)/agent/ 剩余小文件——按需收尾。
