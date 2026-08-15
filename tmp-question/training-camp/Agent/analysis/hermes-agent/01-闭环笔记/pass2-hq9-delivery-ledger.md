# hq9 交付义务账本(Delivery Ledger)— 产品④"at-least-once 投递"蓝本

> 项目:Hermes(gateway/delivery_ledger.py 374 行 + gateway/platforms/base.py:6470-6530 生产者 + gateway/run.py:11067-11184 消费者 + tests/gateway/test_delivery_ledger*.py ×3)
> 假设:生成的最终响应未确认送达前崩溃 = 唯一无声丢失的产物(turn 已烧 token、文本只在 Python 局部变量)。Hermes 用持久化义务账本 + 三检查点 + 崩溃认领重投,是"诚实 at-least-once"的完整样本。
> 结论:✅ 成立——状态机/死主认领/attempts 预算/模糊性标记/崩溃语义显式,产品④"投递可靠性"直接蓝本。

---

## 一、架构全景:为什么需要交付账本(#58818/#41696/#63695)

```
丢失路径:最终响应生成 → 平台 ACK 之间崩溃/计划重启
  → turn 已烧 tokens,文本只存在于 Python 局部变量 → 无声丢弃

┌────────────────────────────────────────────────────────────┐
│ 生产者(gateway/platforms/base.py):最终响应发送前            │
│   record_obligation(state='pending')  → 任何发送尝试前      │
│   mark_attempting(state='attempting') → await 前            │
│   send → mark_delivered / mark_failed(按 SendResult)        │
└──────────────┬─────────────────────────────────────────────┘
               │ state.db(共享文件,WAL,owner pid + 进程启动时间)
┌──────────────▼─────────────────────────────────────────────┐
│ 消费者(gateway/run.py 启动时):sweep_recoverable()           │
│   认领死进程所有、未交付行 → 重投                       │
│   pending   → 直接重投(无重复风险)                     │
│   attempting→ 带 ♻️ 可见重投标记(平台可能已有——诚实至少一次)│
│   failed    → 带标记(重启 = 自然重试边界)               │
│   delivered → 无事(保留期后清除)                       │
└────────────────────────────────────────────────────────────┘
```

**关键决策(#61790 合同评审)**:早期 delivery-outbox 尝试对模糊发送静默重投,评审后关闭——**模糊性显式标记,绝不静默重复**。

---

## 二、设计 1:三检查点状态机(pending/attempting/delivered/failed)

**位置**:`gateway/delivery_ledger.py:14-18`(模块注释)+ `188-233`(record/mark 函数)

```
record_obligation()  state='pending'     发送尝试前(INSERT OR REPLACE,同 id 不产生重复行)
mark_attempting()    state='attempting'  await 前(每轮一次)
mark_delivered()     state='delivered'   SendResult.success 才可
mark_failed()        state='failed'      确定性拒绝 + last_error(500 字符截断)
```

> ★ review 补深(2026-08-15 第三轮):INSERT OR REPLACE 的"幂等"边界——同 id 重录**不产生重复行**,
> 但会把已 delivered/failed 的行**重置为 pending + attempts=0**(绕过 attempts 预算)。
> 安全依赖调用纪律:record 只在"新回合最终响应发送前"调用一次(base.py:6501),
> 重投路径(run.py:11067+)只调 mark_* 不调 record——所以重置路径实际走不到;
> 但产品实现须保持同纪律,勿在重投/重试路径复用 record。

**崩溃语义显式**(模块注释 25-31):
- `pending` — 发送从未开始:直接重投,无重复风险
- `attempting` — 崩溃在 await 中途:平台**可能已有**消息 → 带可见标记重投
- `failed` — 确定性拒绝一次:重启是自然重试边界,带标记
- `delivered` — 无事,保留期后清除

**产品④映射**:知识库"投递确认"状态机——未确认 = 可重投,模糊 = 标记不静默。

## 设计 2:幂等 obligation_id(稳定 id 防重录)

**位置**:`delivery_ledger.py:179-185`(compute_obligation_id)

```
payload = f"{session_key}|{message_ref}|{content}"
id = sha256(payload)[:24]

- session_key 携带 platform/chat/thread → 同 chat 不同线程/topic 永不碰撞
  (cron-topic 碰撞类是早期 outbox 失败的教训)
- message_ref = 触发入站消息 id → 区分同一 session 内的不同回合
- 同回合同内容重录幂等(INSERT OR REPLACE)

测试(test_stable_and_distinct):
- 同 (sk, msg, content) → 同 id;改任一 → 不同 id;长度 24
```

**产品④映射**:知识库事件幂等键——"同一逻辑事件"的确定性指纹,重放不重复。

## 设计 3:死主认领 + 原子重盖章(deliverable_platforms 守卫)

**位置**:`delivery_ledger.py:236-306`(sweep_recoverable)+ `135-176`(owner 活性)

```
_owner_alive(pid, started_at):
  - pid + 进程启动时间双检查(pid 复用防护:新进程 ≠ 旧进程)
  - get_process_start_time 失败 → os.kill(pid,0) 降级探测
    (EPERM 算存活 / ProcessLookupError 死亡 / PermissionError 存活)

sweep_recoverable(now, deliverable_platforms):
  1. 死主行才认领(_owner_alive False)
  2. attempts >= MAX_ATTEMPTS(3)或超 STALE_AFTER_SECONDS(24h)→ abandoned
  3. deliverable_platforms 过滤:本 boot 连不上的平台不认领
     —— attempts 是重投预算,只能花在真发送上(见设计 4)
  4. 原子认领:UPDATE ... WHERE obligation_id=? AND (owner_pid IS ? OR owner_pid=?)
     —— 条件守卫防两个 gateway 同时认领同一行(rowcount 校验)
  5. claimed 返回行携带 needs_marker(state != 'pending')

测试(test_dead_owner_pending_claimed_without_marker):
- 死主 pending 行认领,无标记,attempts=1
- 认领重盖章 → 同进程第二次 sweep 不重复认领
```

**产品④映射**:知识库崩溃恢复的"主人死判"——pid + 启动时间双检防 pid 复用误判;认领原子化防双网关并发。

## 设计 4:attempts 预算只花在真发送上(#61790 教训)

**位置**:`delivery_ledger.py:279-285` + 测试 TestAttemptsOnlySpentOnRealSends/TestUnconnectedPlatformKeepsItsBudget

```
问题:attempts 是重投预算,每 boot 只应花在真实发送上。
     平台本 boot 连接失败 → caller 的 adapter 分支跳过 → 若已认领,
     每 boot 烧一次 attempts → MAX_ATTEMPTS boot 后从未发过一次就 abandoned,
     恰好丢失账本要保证的响应。
     且该失败与创建义务的崩溃相关:杀死的发送的网络故障往往下次 boot 还在。

机制:deliverable_platforms(值字符串集合)过滤——absent 平台行不动,
     留给以后 boot;stale 截止仍约束它们。

测试:
- test_absent_platform_does_not_burn_attempts:
  MAX_ATTEMPTS+2 次不可达平台 sweep → attempts 仍 0,state 仍 attempting,非 abandoned
- test_row_still_delivers_once_its_platform_returns:
  平台恢复后第一次认领 → attempts=1 正常重投
- test_row_survives_boots_where_its_platform_is_down(端到端 runner):
  MAX_ATTEMPTS+1 boot 平台宕 → 不 abandoned、attempts=0
```

**产品④映射**:知识库重试预算语义——"预算只花在可能成功的动作上";失败相关性问题(网络故障持续)显式承认。

## 设计 5:连接泄漏修复(_transaction 必须 close)

**位置**:`delivery_ledger.py:115-132`(_transaction 注释)

```
问题:sqlite3.Connection.__enter__/__exit__ 只 commit/rollback,不关闭连接。
     with _connect() 单独用 → 每次调用泄漏连接 + WAL/SHM 文件描述符,
     推迟到 GC —— 长跑 gateway 耗尽 RLIMIT_NOFILE
     (cron-ledger 兄弟 bug #69567 / PR #69594)
     record_obligation 跑在每个出站最终响应 → 本账本是最高频泄漏者。

修复:contextmanager 显式 conn.close()(finally)

测试(test_ledger_operations_close_every_connection):
- 委托包装连接追踪 open/close → 每个操作都关闭
```

**产品④映射**:知识库写路径的 fd 纪律——高频调用点必须显式关闭,不能依赖 GC。

## 设计 6:主数据优先(ledger 失败绝不阻塞发送)

**位置**:`delivery_ledger.py:36-37`(best-effort 原则)+ base.py:6530 生产者 try/except + run.py 消费者 try/except

```
- 生产者:ledger_enabled 检查 + 全部调用 try/except
  → 账本故障只记录 debug 日志,绝不影响/延迟实际发送
- 消费者:sweep 失败 → logger.debug + return 0(不炸启动)
- ledger_enabled 默认开(config 门,字符串宽松解析),读取失败回退 True
```

**产品④映射**:观测/可靠性层永远不影响主链路——账本是"尽力而为的保险",不是前置条件。

## 设计 7:生产者排除规则(什么不记录)

**位置**:`gateway/platforms/base.py:6489-6513`(排除条件 6489 + 记账 6503-6523)

```
不记录:
- is_ephemeral_response(瞬态回复便宜,可再生成)
- 斜杠命令/typed_command_prefix 开头(可再生成)
- 空文本

记录时机:文本内容最终确定后、_send_with_retry 之前
```

**产品④映射**:知识库"什么值得持久化"的判据——可再生产物不记,昂贵产物必记。

> ⚠ 测试缺口:排除规则(斜杠/ephemeral/空文本)仅 producer 测试 docstring 声明
> (test_delivery_ledger_producer.py:5),**无专门测试用例锁定**——属"文档承诺 > 测试锁定",
> 产品实现时须补测试(ephemeral 响应/slash 命令不产生 obligation 行)。

## 设计 8:重投与 resume 的优先级(红重发先于重跑)

**位置**:`gateway/run.py:11067-11184` + 启动序 12224

```
启动恢复顺序:
  1. _redeliver_pending_obligations()——账本重投(答案已生成,重投比重跑便宜且更正确)
  2. _schedule_resume_pending_sessions()——transcript 恢复重跑
  3. _finish_startup_restore()

重投完成后 clear_resume_pending(session_key):
  —— 答案已到达(或已欠该 session),不要同时走 resume 路径重跑回合

端到端测试(test_pending_redelivers_plain_and_clears_resume):
- pending 行重投无标记 + clear_resume_pending 被调用
- attempting 行重投带 RECOVERED_MARKER 前缀
- 慢账本调用不阻塞事件循环(_blocking_probe 见证)
```

**产品④映射**:知识库崩溃恢复的优先级——"已完成工作的重交付"先于"重做工作"。

> ★ review 补深(2026-08-15 深度 review 发现):
> - **clear_resume_pending 无条件执行**(run.py:11161-11168)——即使 send 失败/抛异常也清,
>   注释"The answer reached (or was owed to) this session":行仍在 ledger(state=failed)等后续
>   boot 重试,但 resume 路径被清——**防止两个恢复机制同时跑(双份回复)**。
>   这是"恢复互斥"语义:账本重投与 transcript 重跑二选一,账本优先。
> - **_owner_alive 保守语义**(delivery_ledger.py:171-172):`started_at is None`(记录时拿不到
>   进程启动时间)→ 返回 True(视为活)——**宁可延迟认领,不误判死**(EACCES 同理按活,
>   注释 "windows-footgun: ok — EPERM counts as alive below")。

## 设计 9:毒行防护(不能自旋)

**位置**:`delivery_ledger.py:33-34` + `272-278` + `309-336`(_prune)

```
- **仅死主行**可 abandoned(活主行被 owner_alive continue 跳过,永不 abandon——即使超限/超期,
  活进程可能仍在处理)→ attempts >= 3 或超 STALE_AFTER_SECONDS 24h → abandoned
- abandoned 保留短暂后清除;delivered 保留 7 天(_RETENTION_SECONDS)后清除
- _MAX_ROWS 500 上限:超限删除按 state 优先级(delivered 先删,active 最后)updated_at 升序
- 模块级 _DB_LOCK 线程锁(delivery_ledger.py:56):全部写路径(record/_update_state/sweep/prune)
  串行化——防 to_thread 卸载后的并发写交错
- mark_failed 的 last_error 截断 500 字符(delivery_ledger.py:232)——错误留痕有界
```

**产品④映射**:知识库毒行/毒事件防护——尝试上限 + 过期 + 容量上限三保险,绝不让失败行无限重试。

---

## 三、与四项目对比(投递可靠性)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes delivery ledger |
|------|----|----------|----------|-----|----------------------|
| 投递确认 | — | — | 消息端到端 | WriteBehind 耐久屏障 | **三检查点状态机(pending/attempting/delivered/failed)** |
| 崩溃恢复 | 恢复三态(0/1/2) | 意图先持久化+逐文件发布 | durable/live-only | shutdown_flush 冲刷 | **死主认领(pid+启动时间)+ 原子重盖章** |
| 重复语义 | — | — | — | — | **模糊性标记(诚实 at-least-once,不静默)** |
| 预算 | — | — | — | — | **attempts 只花在真发送(deliverable_platforms)** |
| 幂等 | seq 全序 | — | 版本化事件 | — | **obligation_id sha256 稳定指纹** |
| 毒行 | corrupt() 拒绝 | — | — | — | **超限 abandoned + stale 过期 + 容量上限** |

**结论**:产品④"投递确认"参考 = Hermes delivery ledger 全案 + dsh shutdown_flush(关闭路径不丢)+ Pi 恢复三态。**与 dsh 的"模型可见⟺已记录"互补:ledger 是"已生成⟺已记录(直到确认)"**。

---

## 四、面试弹药

1. **"唯一无声丢失的产物"**:最终响应生成→平台 ACK 之间崩溃 = turn 已烧 token、文本只在局部变量——账本把"欠平台的响应"持久化
2. **"诚实 at-least-once"**:attempting 崩溃 = 平台可能已有 → 带 ♻️ 可见标记重投,绝不静默重复(#61790 合同评审关闭了静默重投)
3. **"attempts 是预算不是计数器"**:不可达平台不认领——否则每 boot 烧一次,从未发过就 abandoned(失败与崩溃相关,网络故障往往还在)
4. **"pid + 启动时间双检"**:pid 复用防护——新进程 ≠ 旧进程,认领不误判活主
5. **"with _connect() 泄漏 fd"**:sqlite 上下文只 commit 不 close,高频调用点耗尽 RLIMIT_NOFILE(#69567)——显式 close 是纪律
6. **"红重发先于重跑"**:答案已生成的会话重投比重跑回合便宜且更正确,重投后清 resume_pending 防双跑

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|---------|
| 三检查点状态机 | 知识库投递确认(pending/attempting/delivered/failed) |
| 幂等 obligation_id | 知识库事件幂等键(重放不重复) |
| 死主认领 + 原子重盖章 | 崩溃恢复认领(pid+启动时间双检 + 条件 UPDATE) |
| attempts 只花真发送 | 重试预算语义(不可达不烧) |
| _transaction 显式 close | 写路径 fd 纪律 |
| 主数据优先 | 可靠性层绝不阻塞主链路 |
| 排除规则 | "什么值得持久化"判据(可再生不记) |
| 重投先于重跑 | 崩溃恢复优先级(重交付 > 重做) |
| 毒行防护 | 失败行三保险(超限/过期/容量) |

> 覆盖设计数:9(设计 1-9)
> 测试契约:test_delivery_ledger.py(12 用例:状态机/幂等/认领/预算/平台守卫)+ test_delivery_ledger_producer.py(5:正常记账送达/失败留痕/record 不阻塞/update 不阻塞/attempting 崩溃可认领)+ test_delivery_ledger_fd_leak.py(连接全关闭);⚠ 排除规则无专测(见设计 7)
