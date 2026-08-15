# Hermes 域发现 v27 补充(续扫第十六轮:1,500-2,500 行区间对账)— 2026-08-14

> 触发:继续深挖。做 1,500-2,500 行区间对账,发现 **hermes_cli/goals.py 的 GoalGate 是产品③验收器的直接蓝本**(此前完全未记录)。
> 结论:新增 1 个高价值域(目标门控 GoalGate),深化 ③。

---

## 一、v27 新增/深化

### 🔴 新增域:目标门控 GoalGate(产品③直接蓝本)

| # | 域 | 位置 | 设计要点 | 产品映射 |
|---|----|------|---------|---------|
| 81 | **目标门控** | hermes_cli/goals.py(2,156) | **确定性 shell 门在 LLM 判定前运行**;失败门短路判定(有界输出 → 续跑 prompt);**未变工作区跳过**(指纹 git status+HEAD sha256——stuck agent 不能重跑同一红色套件);尝试计数超限自动暂停;超时杀进程(-1)+ errors="replace" | ③验收器("门先于判定"= 验证先于完成声明) |

### 深化确认(对账其余文件)

| 文件 | 要点 |
|------|------|
| gateway/stream_consumer(2,437) | 流消费(代码围栏转义/闭合保证) |
| gateway/relay/adapter(2,371) | Relay 平台适配器(UTF-16 长度) |
| hermes_cli/profiles(2,374) | profile 管理 |
| gateway/status(2,283) | 网关状态 |
| tools/voice_mode(2,379)/vision_tools(2,224)/image_generation(1,993) | 多媒体工具(已排除面) |
| gateway/platforms/(weixin 2,419/whatsapp 2,111/signal 1,707) | 平台适配器(同构) |
| tools/environments/(docker 2,050/local 1,690) | 终端后端(已覆盖) |
| hermes_cli/(runtime_provider 2,298/commands 2,269/skills_hub 2,036/console_engine 1,673/session_recovery 1,732) | CLI 命令/运行时/会话恢复 |
| tools/(kanban_tools 2,480/skills_sync_client 2,187/send_message 2,242/cronjob_tools 1,687) | 工具(已覆盖族) |

---

## 二、关键设计(GoalGate 通用价值)

1. **"门先于判定"**:确定性命令门在 LLM 判定前运行,失败短路——**验证先于完成声明**(与 Reasonix TaskContract、Pi reducer 同哲学)
2. **"未变跳过"**:失败指纹重放不重跑——**防烧墙钟**(stuck agent 保护)
3. **"有界输出做续跑输入"**:失败输出 → 续跑 prompt——**具体证据驱动迭代**

**产品③验收器映射**:GoalGate = 章节验收的确定性门(测试命令先于"完成"声明,失败输出回注修正循环,工作区未变不重复跑)。

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v26 | — | 80 | 80 |
| v27 | 1,500-2,500 区间对账 | +1 | **81** |

> 继续:next 轮 1,000-1,500 行区间对账。
