# hq20 会话停滞通知(Session Stall)— 产品②"执行活性策略门"蓝本

> 项目:Hermes(gateway/session_stall.py 121 行 + gateway/run.py:12945 消费者 + agent/session_activity 共享契约)
> 假设:停滞检测不能自造并行进度钟——Hermes 消费共享活动观察契约(observation-only)做 notify-once 策略门,是"停滞通知策略"的完整样本。
> 结论:✅ 成立——纯策略无副作用/观察契约单一来源/notify-once 闩锁/边界分离全具备,产品②"执行活性策略"直接蓝本。

---

## 一、架构全景:策略门 vs 进度钟

```
边界分离(模块注释明确,保持独立):
- shutdown_watchdog = 进程/事件循环活性
- delivery_ledger = 出站投递义务
- ★ session_stall = 停滞*策略门*(挂起入站存在 = 排队跟进),
  不是出站义务,不是进度时间戳

┌────────────────────────────────────────────────────────────┐
│ 共享活动观察契约(agent.session_activity /                  │
│   AIAgent.get_activity_summary,#72039) = 单一进度来源       │
│   ——本模块不发明并行进度钟(不取 turn-start/入站事件时间戳)  │
├────────────────────────────────────────────────────────────┤
│ 纯策略(session_stall.py,无副作用):                        │
│   should_emit:timeout>0 + 有挂起入站 + 未通知 + idle>=timeout│
│   should_clear:无挂起入站→清 / timeout<=0→清 / idle 未知→保持闩锁│
│     / idle<timeout→清                                       │
├────────────────────────────────────────────────────────────┤
│ 消费者(run.py:12945):扫描挂起入站会话,每停滞剧集 notify-once│
└────────────────────────────────────────────────────────────┘
```

**契约设计**:通知/超时/杀/重试策略留在各自组件;共享契约只观察(timestamp + 有界描述 + provenance)。

---

## 二、设计 1:emit 判定(四条件)

**位置**:`session_stall.py:27-43`(should_emit)

```
should_emit(timeout_seconds, idle_seconds, has_pending_inbound, already_notified):
  1. timeout > 0
  2. has_pending_inbound(挂起入站 = 排队跟进存在)
  3. not already_notified(notify-once)
  4. idle_seconds 非 None 且 >= timeout

任一不满足 → False(纯函数可测)
```

**正确性价值**:四条件纯函数——"挂起入站 + 停滞超时"才通知,notify-once 防重复轰炸。

**产品④映射**:知识库停滞通知策略——"有排队工作 + 无进展超时"才提示,一次性。

## 设计 2:clear 判定(闩锁语义)

**位置**:`session_stall.py:46-60`(should_clear)

```
should_clear(timeout, idle_seconds, has_pending_inbound):
  - 无挂起入站 → True(剧集结束)
  - timeout <= 0 → True
  - ★ idle 未知 → False(保持闩锁——"观测缺口不是恢复")
  - idle < timeout → True(活动恢复)

★ 闩锁方向:未知进度不解除通知——观测缺口 ≠ 恢复(与"未知 = 保守"同哲学)
```

**正确性价值**:**观测缺口不算恢复**——activity 快照暂时拿不到时保持通知,不假装问题解决了。

**产品④映射**:知识库停滞闩锁——"不知道是否恢复"时保持告警状态。

## 设计 3:空闲秒数解析(共享契约单一来源)

**位置**:`session_stall.py:72-121`(resolve_session_idle_seconds_from_activity)

```
解析顺序:
1. seconds_since_activity(优先,有限时直接用;负 → 0.0)
2. 否则 last_activity_at / last_activity_ts(时钟差;负 → 0.0)
3. 无可用时间戳 → None(调用者不落回 turn-start/挂起入站钟)

非有限值(float inf/nan)→ 落回第二路径/None
```

**正确性价值**:**单一进度来源**(#72039 契约)——绝不从 turn-start/入站事件时间戳发明并行钟;不可用返回 None 而非猜测。

**产品④映射**:知识库活性判定单一来源——不造并行进度钟;不可用 = 未知(不猜测)。

## 设计 4:notify-once 消费者(扫描 + 闩锁表)

**位置**:`gateway/run.py:12945-13080`

```
扫描挂起入站会话(_pending_messages 槽位):
  has_pending = pending_event is not None
  activity = _session_activity_for_stall(session_key)(仅 has_pending 时查)
  idle_seconds = resolve_session_idle_seconds_from_activity(activity)
  already = notified_map.get(session_key)
  → should_clear → 清闩锁;否则 should_emit → 通知 + 记录闩锁

边界:
- pending sentinel 无 activity → 跳过(不通知——无进度信息)
- 无共享 summary → 忽略原始时钟(不造钟)

★ 消费者细节(测试锁定,笔记 v1 遗漏):
- 压缩 provenance 日志/跳过活跃压缩心跳(test_..._logs_compression_provenance/
  test_..._skips_active_compression_heartbeat——压缩中不算停滞)
- 总结缺口后不重通知(test_..._does_not_renotify_after_summary_gap)
- 发送失败/软失败重试(test_..._retries_after_send_failure/soft_send_failure)
- 恢复后再停滞重新通知(test_..._renotifies_after_resume_then_restall)
- 卡死发送有界(test_..._bounds_wedged_send)
- 队列事件溢出也通知(test_..._queued_events_overflow_notifies)
- 扫描 profile 适配器(test_..._scans_profile_adapters)
- timeout<=0 禁用(test_..._disabled_when_timeout_zero)
```

**正确性价值**:消费者只做"扫挂起入站 + 查共享活动 + 策略门 + 闩锁表";无进度信息不通知。

**产品④映射**:知识库停滞通知消费者——策略与时钟分离,观测缺口不误判。

---

## 三、与四项目对比(执行活性)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes session_stall |
|------|----|----------|----------|-----|----------------------|
| 活性检测 | — | 心跳 stale | — | — | **共享活动契约单一来源** |
| 通知策略 | — | — | — | repeat 提醒 | **四条件纯函数 + notify-once** |
| 闩锁 | — | — | — | — | **观测缺口不解除(未知≠恢复)** |
| 边界 | — | — | — | — | **与 watchdog/delivery 明确分离** |
| 互补 | — | — | — | — | **停滞通知(策略门)vs 心跳(活性)** |

**结论**:产品"执行活性"参考 = Hermes session_stall(纯策略门 + notify-once + 闩锁)+ hq6 委派心跳(活性检测)+ dsh repeat 提醒(收敛)。**停滞通知是"策略门"(有排队工作+无进展才提示),不是进度钟**。

---

## 四、面试弹药

1. **"不发明并行进度钟"**:从 turn-start/入站事件时间戳推进度 = 第二时钟,与真实活动漂移——共享契约单一来源(#72039)
2. **"观测缺口不是恢复"**:activity 快照拿不到 → 保持闩锁——不假装问题解决
3. **"notify-once 防轰炸"**:停滞剧集只通知一次;活动恢复或挂起排空才清闩锁
4. **"策略门不是义务"**:挂起入站存在 = 排队跟进(策略门),不是 delivery_ledger 的出站义务,也不是 watchdog 的进程活性
5. **"无进度信息不通知"**:pending sentinel 无 activity → 跳过;无共享 summary → 忽略原始时钟

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 四条件 emit | 停滞通知纯函数(挂起+超时+notify-once) |
| 闩锁 clear | 观测缺口不解除(未知≠恢复) |
| 共享契约解析 | 活性单一来源(不造并行钟) |
| notify-once 消费者 | 扫描+闩锁表(无进度不通知) |
| 边界分离 | 与 watchdog/delivery 明确分工 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_session_stall_watchdog.py(24 用例:emit 四条件/clear 闩锁/格式化/解析顺序+非有限拒绝/消费者 notify-once/跳过新鲜/跳过无挂起/闩锁清空/跳过 sentinel/忽略原始钟/压缩 provenance/跳过压缩心跳/总结缺口不重通知/发送重试×2/重新停滞重通知/卡死发送有界/队列溢出/多 profile 扫描/timeout 禁用/默认配置)
> 位置:should_emit :27 / should_clear :46 / resolve :72 / 消费者 run.py:12945
> 关联:#72016(停滞通知策略门)/#72039(共享活动契约)
