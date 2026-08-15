# Hermes 域发现 v29 补充(续扫第十八轮:gateway/relay 协议族)— 2026-08-14

> 承接:v28。本轮:gateway/relay/(transport/descriptor/auth/media/ws_transport)。
> 结论:**Relay connector 协议族确认**(4 关注点 + HMAC 签名认证)——②执行远程形态。

---

## 一、v29 深化确认(gateway/relay)

| 设计 | 位置 | 要点 |
|------|------|------|
| **RelayTransport 协议** | transport.py:42 | **4 关注点**:生命周期(connect/disconnect)/握手(返回 CapabilityDescriptor)/入站(set_inbound_handler 回调)/出站(send_outbound + **send_interrupt 按 session_key 路由到拥有 socket**) |
| **能力描述** | descriptor.py | CapabilityDescriptor(连接器声明的平台能力) |
| **HMAC 签名认证** | auth.py:51-142 | sign/verify_signature(_hmac_hex)/**make_token(带 TTL)/make_upgrade_token**/verify_token/verify_delivery_signature——**双签名(升级 token + 投递签名)** |
| **WS 传输** | ws_transport.py | WebSocket 客户端传输 |
| **实验状态** | transport.py:41 | EXPERIMENTAL(≥2 个 Class-1 平台验证才稳定)——**契约成熟度声明** |

---

## 二、关键设计(通用价值)

1. **"send_interrupt 按 session_key 路由"**:中断信号沿拥有该会话的 socket 下发——**中断的定向路由**(与 turn_lease 同族)
2. **"TTL token + 投递签名"**:升级 token 带 TTL,投递签名独立验证——**双因素认证**
3. **"能力描述握手"**:连接器声明能力 → 网关适配——**能力协商**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v28 | — | 81 | 81 |
| v29 | gateway/relay 协议族 | +0(深化 2 设计) | **81**(深化) |

> 继续:next 轮 gateway/ 剩余子目录(platforms 适配器族已确认同构)/hermes_cli/ 剩余命令——按需收尾。
