# Hermes 域发现 v46 补充(续扫第三十五轮:tests 契约补查)— 2026-08-14

> 承接:v45。本轮:tests/(agent/hermes_cli/cron)契约面补查。
> 结论:契约测试命名族确认(host contract/invariants/gate contract),无新域。

---

## 一、v46 确认(tests 契约面)

| 测试族 | 要点 |
|--------|------|
| **test_context_engine_host_contract** | ContextEngine 主机契约:transition 跳过引擎缺失的可选钩子/会话切换后重绑定内置压缩器/**update_from_response 转发规范缓存桶**/engine 收集器转发注册命令 |
| **test_busy_policy_invariants** | 忙碌策略不变量:bypass 集从注册表派生(不硬编码)/interrupt→dispatch 类 |
| **test_remote_spending_gate_contract** | 远程花费门契约 |
| cron 测试族 | 调度门/蓝图目录/claim_job_for_fire/计算下次运行——调度契约 |

**契约测试命名模式**:`*_contract.py`(主机契约)/`*_invariants.py`(不变量)——**契约测试的命名即声明**(与 Pi conformance 工厂、Reasonix 一致性测试同思想)。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v45 | — | 81 | 81 |
| v46 | tests 契约补查 | +0(契约族确认) | **81**(确认) |

> **Hermes 续扫 35 轮完成**:gateway 剩余/transports/tests 契约面全部确认。剩余:gateway/platforms 具体适配器(同构)/hermes_cli 150 以下(命令小工具)——**程序化收敛最终达成**。
