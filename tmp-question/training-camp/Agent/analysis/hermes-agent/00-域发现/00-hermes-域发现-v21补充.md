# Hermes 域发现 v21 补充(续扫第十轮:approval 智能拒绝)— 2026-08-14

> 承接:v20。本轮:tools/approval.py(4,919)细看。
> 结论:**智能拒绝的 owner 覆盖语义**确认(覆盖受限),深化 ②安全。

---

## 一、v21 深化确认(approval 智能拒绝)

| 设计 | 位置 | 要点 |
|------|------|------|
| **smart_denied 覆盖受限** | :2801-2934/4189-4349 | **智能拒绝后 owner 覆盖禁用 allow_session/allow_permanent**——只能覆盖为 allow_once 或 deny(owner 覆盖受约束,不能解除智能拒绝的会话/永久许可) |
| **hardline 命令检测** | :432-657 | _hardline_rm_path/_hardline_block_result(硬线 rm/危险命令) |
| **审批会话/永久** | :2622-2710 | approve_session/approve_permanent |
| **连续拒绝熔断** | :2448+ | smart approval 连续拒绝电路断路器(此前 v9 已记) |

---

## 二、关键设计(通用价值)

1. **"智能拒绝的覆盖受限"**:AI 判定 DENY 后,owner 只能 allow_once/deny(不能解除为 session/permanent)——**人与 AI 的审批层级**(AI 否决权高于 owner 的便利授权)
2. **"hardline 不可绕过"**:硬线命令检测独立于智能审批

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v20 | — | 80 | 80 |
| v21 | approval 智能拒绝 | +0(深化 1 设计) | **80**(深化) |

> 继续:next 轮 config_defaults(4,547)/tts_tool(4,502)/cli_commands_mixin(3,650)——按需收尾。
