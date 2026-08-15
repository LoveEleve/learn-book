# hq19 缩放至零 + 唤醒(Scale-to-Zero + Wake)— 产品②"无人值守长跑休眠"蓝本

> 项目:Hermes(gateway/scale_to_zero.py 232 行 + gateway/wake.py 184 行 + gateway/run.py:8170 watcher + relay go_dormant + Fly Machines API)
> 假设:无人值守长跑需要"空闲即休眠、事件即唤醒"——Hermes 用空闲谓词 + 自挂起(own the suspend call)+ 双策略唤醒实现,是"长跑休眠"的完整样本。
> 结论:✅ 成立——空闲三条件/自挂起防黑洞/唤醒双策略/429 重试/失败语义全具备,产品②"无人值守长跑"直接蓝本。

---

## 一、架构全景:为什么自己挂起自己

```
为什么 gateway 自挂起而非 autostop:"suspend"(模块注释明确):
- Fly Proxy 只按入站代理连接判空闲——看不见在飞 agent 回合(仅出站 LLM
  流量),应用无法信号"未就绪不可挂"
- 2026 年中 Proxy 不再把开放出站 socket 当活动——relay WebSocket 不再
  掩盖竞态:Fly 会在任务中途挂机,甚至可能在 go_dormant() 翻转 relay
  目标前挂(缓冲事件黑洞)
- 拥有挂起调用关闭两者:只在空闲谓词成立 AND dormant quiesce 完成后才挂

┌────────────────────────────────────────────────────────────┐
│ 装配条件(should_arm 三合一):                              │
│   Labs 开关(HERMES_SCALE_TO_ZERO env 戳,非用户 config)     │
│   + 消息仅 relay/无(直接连接平台持活 socket 不能缩零)       │
│   + wakeUrl 已注册(挂起实例无可达唤醒目标 = 黑洞)           │
├────────────────────────────────────────────────────────────┤
│ 空闲谓词(is_idle 三条件):                                  │
│   无在飞 agent 回合 + 无入站 N 分钟 + 无后台工作            │
│   (后台 delegate_task/kanban/bg terminal——中途挂会丢)       │
├────────────────────────────────────────────────────────────┤
│ DORMANT 序列(watcher,run.py:8170):                        │
│   relay.go_dormant()(going_idle→ack + supervisor 保留)     │
│   → 刻意不 mark_resume_pending(D13:挂起保留 RAM)           │
│   → suspend_self(本地 flaps socket,自己挂)                 │
├────────────────────────────────────────────────────────────┤
│ 唤醒(wake.py 双策略):                                     │
│   push 适配器:注入合成 MessageEvent(internal=True)          │
│   stateless 适配器(API server):self-POST /v1/chat/completions│
│     带原始 X-Hermes-Session-Id(恢复真会话)                 │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:装配条件三合一(不选装 = 零行为变化)

**位置**:`scale_to_zero.py:118-131`(should_arm)+ `71-78`(enabled)

```
should_arm(enabled, relay_only_or_absent, wake_url) = 三者全真:
1. HERMES_SCALE_TO_ZERO env 戳 truthy(NAS Labs 开关唯一入口,非用户 config 键)
2. 消息仅 relay/无(_platform_name 比较免 import 枚举——relay 丢弃后无其他)
3. wakeUrl 注册(挂起实例无可达唤醒目标 = 黑洞)

任一不满足 → watcher 永不启动(无空闲计时/无休眠)——非选装实例行为与今天完全一致

config 语义:
- idle_timeout_minutes 是 config.yaml(D2 行为设置进 config 非 env),默认 5 分钟
- parse_idle_timeout_seconds:非数值/非正值降级默认,永不返回 <=0
  (0/负超时 = 立即休眠,永非本意)
```

**正确性价值**:选装门禁三合一——不选装零行为变化;黑洞防护(wakeUrl 必须存在);平台结构条件(直接连接平台不能缩零)。

**产品④映射**:知识库休眠的选装门禁——默认关闭,三重条件全满足才武装。

## 设计 2:空闲谓词三条件(纯函数可测)

**位置**:`scale_to_zero.py:134-151`(is_idle)

```
is_idle(running_agent_count, seconds_since_last_inbound, idle_timeout_seconds, has_live_background_work):
  无在飞回合 AND 无后台工作 AND 入站静默 >= 超时
  ——任一活动保持清醒(中途挂 = 丢工作)

纯函数(plain inputs):单元测试无需活网关
```

**正确性价值**:空闲判定纯函数化——三条件可独立测试;后台工作算活动(防中途挂丢)。

**产品④映射**:知识库空闲判定——"在飞回合 + 后台工作 + 入站"三信号,与 ESTOP 的"暂停新工作"互补(这里是自动空闲,那里是手动暂停)。

## 设计 3:自挂起(own the suspend call,防黑洞)

**位置**:`scale_to_zero.py:154-232`(self_suspend_available/suspend_self)+ `run.py:8170-8240`(watcher)

```
self_suspend_available:FLY_APP_NAME + FLY_MACHINE_ID + /.fly/api socket
  ——非 Fly(本地/ACA/测试)False → watcher 跳过挂起,休眠仍发生
   (平台永不冻结进程,仅成本不省)

suspend_self:POST /v1/apps/{app}/machines/{id}/suspend 于本地 flaps socket
  - Fly 机器内 Machines API 无需 token——socket 本身就是凭证
  - stdlib-only 刻意:纯 unix-socket HTTP/1.1,无 httpx/requests 热路径依赖,
    无 async 管道冻结中途
  - 失败语义:2xx 才算成功;失败 = 保持运行(fail-awake 绝不 fail-frozen)
    ——"costs money but loses no work"
  - fire-and-forget:成功后内核很快冻结本进程,之后无意义代码可跑

watcher 序列(run.py:8170):
  go_dormant()(going_idle→ack + supervisor 保留,绝不 stop/restart drain/
  disconnect——F12/F14;进程保持存活,Fly 冻结+恢复)
  → 刻意不 mark_resume_pending(D13——挂起保留 RAM;仅迁移 autostop:"stop"
    或见 kill 才需 revive)
  → 之后才 suspend(严格顺序:空闲谓词 → dormant 完成 → 挂)
  + re-arm cooldown(:8235,`max(interval, 60.0)`——唤醒的 drained backlog
    不立即再 quiesce;dormant 后 relay connector 排空缓冲积压)
```

**正确性价值**:
1. **own the suspend call** 关闭 Fly Proxy 两个盲区(看不见出站 LLM 流量/不再把出站 socket 当活动)
2. 严格顺序:谓词 → dormant → suspend;dormant 用 go_dormant 而非 drain(F12/F14 纪律)
3. fail-awake 方向(挂失败 = 花钱不丢活)

**产品④映射**:知识库进程自挂起——空闲判定拥有者自己挂(不依赖外部代理判空闲);挂失败 fail-awake。

## 设计 4:唤醒双策略(按适配器能力)

**位置**:`wake.py:45-94`(adapter_supports_push/deliver_wake)+ `97-184`(_self_post_chat_completion)

```
adapter_supports_push:读适配器类 supports_async_delivery 标志(默认 True)
  ——镜像 session_context.async_delivery_supported 但读类属性而非
    request-scoped contextvar(后台 watcher 在无绑定会话上下文外运行)

策略 1(push 适配器:telegram/discord/插件平台):
  注入合成 MessageEvent(internal=True)经 adapter.handle_message——既有唤醒路径原样保留

策略 2(stateless 适配器:API server,supports_async_delivery=False):
  ★ 为何不能走 handle_message:build_session_key() 派生键
    (agent:main:api_server:group:<sid>)永不匹配真实回合的原始
    X-Hermes-Session-Id 键(_bind_api_server_session)→ 唤醒落平行隐形会话
  → 改 self-POST /v1/chat/completions 带原始 session id 头——
    真实回合的同一入口 → 恢复真会话(全历史),结果客户端下次轮询可见

self-POST 细节:
- 绑定地址通配(0.0.0.0/::/*)→ 回环;IPv6 字面量括 []
- 403 门:session continuation 经 X-Hermes-Session-Id 需 API_SERVER_KEY 配置
  ——缺 key = 硬错误响亮抛(不在无人看的新指纹会话跑唤醒)
- 429(全局 max_concurrent_runs 上限)→ 退避重试(2s/5s/10s)——无 per-session
  锁,并发回合 last-writer-wins,但全局并发上限值得等
- WAKE_TURN_TIMEOUT=600s(整回合同步 stream=false 的长工具回合不中途杀)
- 失败 RAISE(有界重试后)——调用者可回卷游标/重试而非静默丢事件
```

**正确性价值**:
1. 双策略按能力分派——push 注入事件,stateless 走真实入口(会话键不匹配的坑有详细注释)
2. 403 硬错误(缺 API_SERVER_KEY 不静默跑错会话)
3. 失败 raise(不静默丢唤醒事件)

**产品④映射**:知识库唤醒双策略——push 适配器事件注入/stateless 走真实入口;会话键匹配是正确性关键。

---

## 三、与四项目对比(长跑休眠)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes scale-to-zero |
|------|----|----------|----------|-----|----------------------|
| 空闲判定 | — | — | — | — | **三条件纯函数(回合/后台/入站)** |
| 休眠机制 | — | — | — | — | **自挂起(own suspend,flaps socket)** |
| 唤醒 | — | — | — | — | **双策略(push 事件/self-POST 真会话)** |
| 失败方向 | — | fail-closed | — | — | **fail-awake(挂失败花钱不丢活)** |
| 选装门禁 | — | — | — | — | **三合一(Labs 戳/relay-only/wakeUrl)** |
| 互补 | — | — | — | ESTOP(手动暂停) | **自动空闲休眠 vs ESTOP 手动暂停** |

**结论**:产品"无人值守长跑"参考 = Hermes scale-to-zero(自挂起/双策略唤醒)+ ESTOP(手动暂停)。**与 ESTOP 互补:一个是自动空闲休眠(省成本),一个是操作者手动暂停(安全阀)**。

---

## 四、面试弹药

1. **"own the suspend call"**:Fly Proxy 按入站连接判空闲,看不见出站 LLM 流量;2026 中连出站 socket 都不算活动——不自挂 = 任务中途被挂 + 缓冲黑洞
2. **"wakeUrl 缺失 = 黑洞"**:挂起实例无可达唤醒目标永不醒——装配条件之一
3. **"fail-awake 绝不 fail-frozen"**:挂失败 = 保持运行(costs money but loses no work)——方向正确
4. **"stateless 适配器唤醒的会话键坑"**:handle_message 派生键永不匹配真实 X-Hermes-Session-Id → 唤醒落平行隐形会话;self-POST 走真实入口
5. **"缺 API_SERVER_KEY 硬错误"**:403 门——不在无人看的新指纹会话跑唤醒
6. **"D13 不 mark_resume_pending"**:挂起保留 RAM——revive 仅在迁移 autostop:"stop" 或见 kill 才需要

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 装配三合一 | 休眠选装门禁(不选装零变化) |
| 空闲三条件纯函数 | 空闲判定可测(回合/后台/入站) |
| 自挂起 | 空闲拥有者自己挂(own suspend) |
| 双策略唤醒 | push 事件/stateless 真会话 |
| fail-awake | 挂失败花钱不丢活 |
| 严格序列 | 谓词 → dormant → suspend |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_scale_to_zero.py(12 用例:装配/空闲/超时解析/挂起)+ test_scale_to_zero_watcher.py(15:watcher 序列/re-arm cooldown)+ test_wake_delivery.py(3:双策略)+ relay/test_relay_going_idle.py(关联)
> 位置:should_arm :118 / is_idle :134 / suspend_self :170 / watcher run.py:8170 / deliver_wake wake.py:56
> 常量:HERMES_SCALE_TO_ZERO/FLY_APP_NAME/FLY_MACHINE_ID/.fly/api socket/DEFAULT_IDLE_TIMEOUT=5min/WAKE_TURN_TIMEOUT=600s/_RETRY_DELAYS=(2,5,10)s
