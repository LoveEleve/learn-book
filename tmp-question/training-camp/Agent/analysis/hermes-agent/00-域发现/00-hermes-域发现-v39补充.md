# Hermes 域发现 v39 补充(续扫第二十八轮:tui_gateway 方法族)— 2026-08-14

> 承接:v38。本轮:tui_gateway/(methods_session 3,304/methods_tools 1,965/methods_prompt 1,364/compute_host 893)。
> 结论:TUI 网关 = JSON-RPC 方法注册表 + 计算子进程宿主,无新域(与 tui_gateway/server v14 已确认的结构一致)。

---

## 一、v39 确认(tui_gateway 方法族)

| 文件 | 要点 |
|------|------|
| **methods_session(3,304)** | **30+ JSON-RPC 会话方法**(`def _(rid, params)` 注册表模式)——会话生命周期/恢复/切换的 TUI 面 |
| **methods_tools(1,965)/methods_prompt(1,364)** | 工具/提示词方法面(同注册表模式) |
| **compute_host(893)** | **SpikeAgent 确定性假 agent**(管道/中断测量:delta_count/delay 可控;interrupt 事件)——**测试用宿主**;ComputeHost 子进程模型(workers/RSS) |
| **methods_profiles(719)/methods_complete(626)/project_tree(786)** | 画像/补全/项目树方法面 |

**结论**:tui_gateway 是完整的 JSON-RPC 桥(方法注册表模式,每方法 `_(rid, params)`),与 v14 确认的 server/会话槽位协议一致——无独立新设计。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v38 | — | 81 | 81 |
| v39 | tui_gateway 方法族 | +0(结构确认) | **81**(确认) |

> 继续:next 轮 gateway/ 剩余小文件(platform_registry/delivery/session_context)+ acp_adapter 剩余——按需。
