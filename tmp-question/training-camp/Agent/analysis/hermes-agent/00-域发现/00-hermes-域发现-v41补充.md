# Hermes 域发现 v41 补充(续扫第三十轮:hermes_cli/cron 剩余)— 2026-08-14

> 承接:v40。本轮:hermes_cli/(service_manager/inventory/skin_engine)+ cron/(blueprint_catalog/executions)。
> 结论:**自动化蓝图**(参数化 slot 单一真相源)与**执行审计账本**确认,深化 ②定时/③审计。

---

## 一、v41 深化确认

### cron/blueprint_catalog(799) — 自动化蓝图

| 设计 | 位置 | 要点 |
|------|------|------|
| **参数化蓝图** | :1-30 | 一个定义多处渲染:表单(仪表盘)/预填斜杠命令(CLI/TUI)/种子 prompt(agent)/拷贝命令+hermes:// 深链(文档);**slot schema 是单一真相源**;fill_blueprint 验证后转 create_job kwargs——**无第二个 job 引擎**;用户永不输原始 cron(schedule_template 固定 + 只参数化人友好部分) |

### cron/executions(280) — 执行审计账本

| 设计 | 要点 |
|------|------|
| **审计账本非重试队列** | 记录每次尝试的已知信息;**中断 attempt 只在 owner 进程证明消失后变 unknown**;终端态不可变——**审计状态机的严谨性** |

### 其他确认

- hermes_cli/inventory(856):provider/model 清单单一化(消除 2 个隐藏 bug——dashboard 漏 v12+ providers 键/TUI canonical-merge 键错)——**单一事实源消除重复推导**的实证
- service_manager(1,125):服务管理器抽象(systemd/launchd/Windows + s6 检测)
- skin_engine(1,068)/curses_ui(995):皮肤/终端 UI

---

## 二、关键设计(通用价值)

1. **"蓝图单一真相源"**:一个 slot schema 渲染到所有表面——**配置的声明式渲染**(与 Pi TypeBox schema、Reasonix manifest 同思想)
2. **"审计账本的非重试语义"**:记录 ≠ 重试;中断态需 owner 消失证明——**审计与执行的分离**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v40 | — | 81 | 81 |
| v41 | hermes_cli/cron 剩余 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 hermes_cli/ 剩余命令收尾 + docker/scripts 部署面——按需。
