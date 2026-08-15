# Hermes 域发现 v26 补充(续扫第十五轮:顶层大文件收官)— 2026-08-14

> 承接:v25。本轮:剩余 3,000+ 行文件快速核对(anthropic_adapter/cua_backend/plugins_cmd/doctor/agent_init 等)。
> 结论:顶层大文件全部核对完成,无新域。

---

## 一、v26 确认(顶层大文件收官)

| 文件 | 要点 |
|------|------|
| agent/anthropic_adapter(3,216) | 模型能力判定族(adaptive thinking/xhigh/fast mode/禁采样参数)——**模型能力矩阵** |
| tools/computer_use/cua_backend(3,304) | X11 活动窗口检测(xprop)/捕获目标选择/WSL 路径转换——**计算机使用后端** |
| hermes_cli/plugins_cmd(2,999) | 插件命令 |
| hermes_cli/doctor(2,998) | 诊断(已覆盖) |
| agent/agent_init(2,924) | 初始化(已覆盖 v13 部分) |
| gateway/config(2,765) | 网关配置 |
| tools/file_tools(2,749) | 文件工具(已覆盖) |

**核对结论**:顶层 20 个 >2,500 行文件(v12 对账的 17 个 + 补充)全部打开确认——**v12 的"17 个未打开"对账已闭环**。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v25 | — | 80 | 80 |
| v26 | 顶层大文件收官 | +0(对账闭环) | **80**(对账闭环) |

> **Hermes 续扫 15 轮完成**:顶层大文件(29K/19K/18K/14K/12K/11K/10K/9K/8K/7K/6K/5K 全部)覆盖,与 agent//tools//gateway/ 内部(此前 80 域)合并后——源码实现面全覆盖。
> 剩余:tests 契约(此前 v3 已扫)/plugins 内部(排除)/平台适配器(同构)——按产品价值到边际。
