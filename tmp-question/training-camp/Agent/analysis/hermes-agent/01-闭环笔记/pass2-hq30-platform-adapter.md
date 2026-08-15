# hq30 平台适配器抽象(Base Platform Adapter)— 产品②"平台抽象"蓝本

> 项目:Hermes(gateway/platforms/base.py 7,322 行 + 20+ 平台实现 + tests/gateway/test_platform_base.py 77 用例)
> 假设:20+ 平台(Telegram/Discord/Slack...)同一 agent 核心——Hermes 用 BasePlatformAdapter ABC 抽象平台能力,是"平台抽象"的完整样本。
> 结论:✅ 成立——能力抽象/消息原语/会话绑定/认证/媒体/审批/锁全具备,产品②"多入口适配"直接蓝本(域发现 v10 已标:20+ 平台适配器为同构实现,抽象是核心)。

---

## 一、架构全景:平台能力抽象

```
┌────────────────────────────────────────────────────────────┐
│ BasePlatformAdapter(ABC,2884):平台能力抽象面              │
│   发送/编辑/删除/草稿流式/审批/clarify/私信/媒体/          │
│   平台锁/消息长度函数/markdown→平台格式                     │
├────────────────────────────────────────────────────────────┤
│ 核心消息原语:                                             │
│   send(3895)/edit_message(3951)/delete_message(3980)/      │
│   send_draft(3268 草稿流式)/send_typing/图片族              │
├────────────────────────────────────────────────────────────┤
│ 会话与认证:                                               │
│   build_source(7012)/toolsets_for_source(7122)/            │
│   set_authorization_check(3677)/enforces_own_access_policy  │
│   /authorization_is_upstream(3178)                         │
├────────────────────────────────────────────────────────────┤
│ 能力标志:                                                 │
│   supports_draft_streaming(3209)/streaming_overflow_limit   │
│   /message_len_fn(3117 平台消息长度)/render_message_event   │
│   (3310,hq21 渲染钩子)                                     │
├────────────────────────────────────────────────────────────┤
│ 生命周期:connect(3870)/disconnect(3890)/has_fatal_error/   │
│   重连/平台锁(acquire_scoped_lock 防跨 profile 凭证冲突)    │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:能力抽象(发送族)

**位置**:`base.py:3268-4400`(发送族)

```
send(3895)/edit_message(3951)/delete_message(3980):
  ——平台消息三原语抽象

send_draft(3268):草稿流式(平台支持则原生草稿,否则 fallback)
send_slash_confirm(4144)/send_clarify(4179)/send_private_notice(4253):
  ——审批/澄清/私信通道抽象
send_typing(4273)/send_multiple_images(4314)/send_image(4371):
  ——打字指示/图片族

消息长度:message_len_fn(3117)/max_message_length_for_chat(3125)/
  message_len_fn_for_chat(3140)——平台消息上限抽象
```

**正确性价值**:发送族全抽象——平台差异(长度/草稿/图片)内化;同构实现零重复。

**产品④映射**:多入口适配抽象——发送原语族 + 平台差异内化(消息长度/草稿/媒体)。

## 设计 2:会话与认证

**位置**:`base.py:3677`(set_authorization_check)+ `7012`(build_source)+ `7122`(toolsets_for_source)

```
- set_authorization_check:平台认证检查注入(upstream 授权 vs 本地策略)
- enforces_own_access_policy(3150)/authorization_is_upstream(3178):
  平台是否自有访问策略/上游授权
- build_source:事件源构建(会话键派生)
- toolsets_for_source:平台 → 工具集映射(平台决定哪些工具集)
```

**正确性价值**:认证策略抽象(平台自有 vs 本地)+ 工具集按平台(能力面随平台)。

**产品④映射**:入口适配的认证与能力面——平台决定工具集 + 认证策略分层。

## 设计 3:能力标志(声明式)

**位置**:`base.py:3209`(supports_draft_streaming)+ `3251`(streaming_overflow_limit)+ `3331`(format_tool_event)

```
- supports_draft_streaming:是否支持草稿流式(声明式)
- prefers_fresh_final_streaming(3228):偏好新鲜最终流
- streaming_overflow_limit:流式溢出上限
- render_message_event/format_tool_event(hq21 渲染钩子——默认 1:1 保留)
- enforces_own_access_policy:自有访问策略
```

**正确性价值**:声明式能力标志——平台能力差异由标志表达,调度层按标志分支(非 isinstance 特判)。

**产品④映射**:入口适配能力声明——标志驱动调度(非类型特判)。

## 设计 4:生命周期 + 平台锁

**位置**:`base.py:3870`(connect)+ `3890`(disconnect)+ 平台锁

```
connect(is_reconnect)/disconnect:生命周期(重连语义)
has_fatal_error/fatal_error_message(3394)/fatal_error_code(3402):
  fatal 错误状态(可观测)
set_fatal_error_handler(3420):fatal 处理器
acquire_scoped_lock(域发现 v10):平台锁防跨 profile 凭证冲突
  (每平台凭据唯一,两 profile 同 token 冲突)
```

**正确性价值**:生命周期 + fatal 状态(可观测)+ 平台锁(凭证唯一性)。

**产品④映射**:入口适配生命周期——重连/fatal 状态/凭证锁。

## 设计 5:同构实现(20+ 平台一个抽象)

**位置**:gateway/platforms/(20+ 适配器)

```
telegram(10,542)/discord(10,522)/slack(9,611)... = BasePlatformAdapter 契约实现
——域发现排除清单确认:同构重复,保留 base.py 抽象
(每平台差异 = 覆盖能力标志/发送原语/认证)
```

**正确性价值**:20+ 平台同一抽象——新平台成本 = 实现契约(域发现排除清单依据)。

**产品④映射**:多入口的可扩展性——契约实现成本低(平台差异 = 覆盖标志)。

---

## 三、与四项目对比(平台抽象)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes BasePlatformAdapter |
|------|----|----------|----------|-----|---------------------------|
| 平台面 | — | — | — | — | **ABC 能力抽象(20+ 平台)** |
| 消息原语 | — | — | — | — | **send/edit/delete/草稿/媒体族** |
| 认证 | — | — | — | — | **upstream vs 本地策略** |
| 能力标志 | — | — | — | — | **声明式(草稿/溢出/渲染)** |
| 锁 | — | — | — | — | **平台锁(凭证唯一性)** |

**结论**:产品"多入口适配"参考 = Hermes BasePlatformAdapter(能力 ABC + 声明式标志 + 生命周期)。**与 hq21 渲染钩子衔接(render/format_tool_event 在 adapter);产品 CLI 优先(域发现弃用平台具体实现,保留抽象)**。

---

## 四、面试弹药

1. **"20+ 平台一个抽象"**:telegram/discord/slack 等 = BasePlatformAdapter 契约实现——新平台成本 = 实现契约
2. **"声明式能力标志"**:supports_draft_streaming/streaming_overflow_limit——调度按标志分支非类型特判
3. **"认证策略分层"**:enforces_own_access_policy/authorization_is_upstream——平台自有 vs 本地
4. **"平台锁防凭证冲突"**:acquire_scoped_lock——两 profile 同 token 冲突防护
5. **"能力面随平台"**:toolsets_for_source——平台决定工具集

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 能力 ABC | 多入口统一抽象 |
| 消息原语族 | 发送/编辑/删除/草稿/媒体 |
| 认证分层 | upstream vs 本地策略 |
| 声明式标志 | 能力差异标志驱动 |
| 生命周期 + 锁 | 重连/fatal/凭证唯一性 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_platform_base.py(77 用例)+ tests/gateway/platforms/(各平台测试)+ relay/test_relay_adapter.py
> 位置:BasePlatformAdapter :2884 / send :3895 / edit_message :3951 / build_source :7012 / supports_draft_streaming :3209 / render_message_event :3310 / connect :3870
> 平台:20+ 适配器(telegram 10,542/discord 10,522/slack 9,611 等,域发现排除清单同构确认)
