# hq8 回合租约(TurnLease + Registry)— 产品②"并发回合串行化"蓝本

> 项目:Hermes(gateway/turn_lease.py 352 行 + gateway/run.py 接线 + gateway/session_state.py + agent/agent_runtime_helpers.py note_turn_start)
> 假设:gateway 多路由键映射同一 session_id 时,回合级 [load history → run → flush] 必须按解析后的 session 串行化;Hermes 用代际作用域 + 身份校验的租约实现,是 Fencing Token 家族的完整产品化样本。
> 结论:✅ 成立——回合租约关闭了"每 routing key 守卫看不到"的并发碰撞路线,代际/身份/超时 fail-closed/有界注册表/旋转 rebind 全部具备。

---

## 一、架构全景:为什么需要回合租约(#64934)

```
问题:busy 守卫按 ROUTING KEY 键控(_active_sessions/_running_agents),
      但持久化 transcript 按 SESSION_ID 所有——switch_session() 使 key→id 多对一:
      /resume 具名会话、CLI 连续性重绑、异步委派完成钉住、Telegram topic 绑定
      → 两个 routing key 映射同一 session_id → 两个 agent 对象并发回合
      → 每个 per-key 守卫都看不到碰撞:
        1. flush 按完成序而非到达序持久化
        2. 身份标记去重可整行吞掉记录
        3. 第二个回合在没见过第一个回合交换的历史基线上跑
           → 永久 user;user 交替楔子(repair_message_sequence 每次请求永远再修)

┌────────────────────────────────────────────────────────────┐
│ 获取:会话解析最终确定后(switch_session/tip-walk 之后)       │
│      transcript load 之前——立即获取                        │
├────────────────────────────────────────────────────────────┤
│ 持有:[load history → run → flush] 序列化区域               │
├────────────────────────────────────────────────────────────┤
│ 释放:dispatch 层 finally(每个退出路径)+ 旋转 rebind         │
│       代际作用域 + 身份校验 + 幂等                           │
└────────────────────────────────────────────────────────────┘
```

**租约 vs 守卫的关系**:同 key 消息在回合运行时被两个 routing-key 守卫扣住,永远到不了获取点——所以锁在别名键路线之外完全无竞争;**竞争只出现在别名键路线**(第二个回合等第一个 flush)。

---

## 二、设计 1:获取时机(load 之前,解析之后)

**位置**:`gateway/run.py:17996-18012`(_handle_message_with_agent)

```
获取点在 session_entry.session_id 最终确定之后、transcript load 之前:
  _lease_token = await _lease_registry.acquire(
      session_entry.session_id, owner_key=_quick_key,
      generation=run_generation, timeout=HERMES_TURN_LEASE_TIMEOUT)

超时 → 清 session env tokens(防早退泄漏 task-local 身份)→ raise
释放:_handle_message 的 finally 经 _release_turn_lease(见设计 2)
```

**正确性价值**:
1. 序列化边界 = [load → run → flush] 全区域,不是局部
2. 超时路径先行清理 task-local 身份(早退不泄漏)——`_clear_session_env(_session_env_tokens)` 在 propagate 前调用
3. 消息在获取点之前已被 per-key 守卫扣住 → 无竞争路径零开销

**产品④映射**:知识库"读日志→分析→写日志"回合级互斥——竞态窗口是"写序颠倒 + 去重吞行 + stale 基线",不是"写文件本身"。

## 设计 2:代际作用域 + 身份校验释放(#28686 所有权教训)

**位置**:`gateway/turn_lease.py:97-123`(TurnLeaseToken)+ `324-352`(release)

```
TurnLeaseToken:session_id/owner_key/generation/released
release(token) → 只有当 token 是当前 holder 才释放:
  if lease.holder is not token: return False   # stale unwind 安全
  lease.holder = None → lock.release()
幂等:token.released 置位后再次 release 是 no-op

测试(test_runner_release_turn_lease_is_token_scoped_and_bare_safe):
- 错误 generation → 不弹 token 不释放
- 正确 (key, generation) → 释放
- 幂等(第二次 False)
- 空 key 守卫
```

**正确性价值**:dispatch 层以 pair encoding 持有 token——`TurnState.lease_token` + `lease_generation`(session_state.py:70-75,每 session key 至多一个持有 token,与注册表按 session 串行化一致);`_turn_lease_tokens` 是迁移保留的 legacy 视图(TurnLeaseTokenView,键 (session_key, generation),setter 走 get-or-create state 写 pair)。/stop、/new 导致 generation bump 的 stale unwind 只能弹它自己的 token,注册表身份检查再兜底——**陈旧解卷永远不能释放新回合的租约**。

**产品④映射**:Fencing Token 三变体之一(Pi writer-leases / Hermes compression lease / Hermes turn lease)——代际标识让"谁持有"不可伪造。

## 设计 3:超时 fail-closed(绝不无序列化运行)

**位置**:`turn_lease.py:72-94`(TurnLeaseTimeoutError)+ `191-264`(acquire)+ `run.py:16830-16844`(dispatch 处置)

```
acquire 超时 → raise TurnLeaseTimeoutError(session_id/owner_key/generation/wait_seconds)
  —— 不返回降级 token(降级 token 曾授权"无序列化运行"的不安全路径,测试锁定)

dispatch 处置(外部分发层拥有可见的拒绝/重发通知):
  return "⏳ Another turn is still running on this session. To protect the
          transcript, this message was not processed. Wait for the active
          turn to finish, then resend it."
  —— 且:/goal judge 不消费该通知(拒绝不是完成的回合,不触发目标续跑)

测试(test_timeout_fails_closed + test_full_dispatch_rejects_lease_timeout_without_running_goal_hook):
- 超时既不窃取也不释放 holder 的租约
- transcript load 绝不开始(load_transcript 断言不被调用)
- _run_agent 不被调用;_post_turn_goal_continuation 不被 await
```

**正确性价值**:
1. 超时是失败信号,不是妥协信号——调用者必须拒绝回合
2. 拒绝消息 ≠ 完成的回合(/goal 不吞通知,防合成续跑循环)
3. 租约等待有自己的时钟(HERMES_TURN_LEASE_TIMEOUT),与 agent 不活动超时无关——短租约预算即使正常 agent 超时很长也要及时拒绝

**产品④映射**:知识库回合超时 = 拒绝写而不是"尽力并发写"——失败闭环优于数据损坏。

## 设计 4:有界注册表(容量 vs 正确性)

**位置**:`turn_lease.py:58-69`(DEFAULT_MAX_LEASES=512)+ `160-189`(registry/_evict_idle)

```
- 每 session 租约 map 大小上限 512
- 驱逐只移除 idle 条目(holder=None + 锁未持 + 无 pending acquire),最旧优先
- live 租约永不驱逐——正确性优先于容量(会话突发可瞬态超上限)
- idle 定义:_SessionLease.idle = holder is None and not lock.locked()
                                  and pending_acquires == 0
```

**正确性价值**:驱逐窗是"容量管理",绝不能成为并发破坏源——被驱逐的只会是无人持有、无人等待的条目。

**产品④映射**:知识库租约注册表同样有界(防无限 session 泄漏),但活租约优先。

## 设计 5:等待者交接窗保护(pending_acquires 计数)

**位置**:`turn_lease.py:126-149`(_SessionLease)+ `191-264`(acquire 中 pending 计数)

```
问题:asyncio.Lock.release() 唤醒等待者但留下瞬时未锁窗——容量驱逐在该窗
     可能孤儿化旧锁,同一 session 的后续获取取得第二把锁并发执行。

机制:每次 acquire 前 pending_acquires += 1(wait_for 可能在底层锁协程运行前
     就调度了 acquire,所以即使看似无竞争的获取也要计数),finally 递减。
     idle 判定包含 pending_acquires == 0 → 驱逐无法孤儿化正在交接的锁。

测试(test_registry_does_not_evict_lease_during_waiter_handoff):
- 唤醒窗内 _get_or_create("other") 触发驱逐 → shared 的锁对象仍是原 lease
- test_registry_does_not_evict_an_uncontended_acquire_before_it_locks:
  锁看似自由但 acquire 已调度 → pending 计数保护
- test_timed_out_acquire_does_not_pin_idle_registry_entry:
  超时获取不钉住 idle 条目(可驱逐)
- test_cancelled_acquire_does_not_pin_idle_registry_entry:
  取消获取同样不钉住
```

**正确性价值**:两方向正确——等待者交接窗内不驱逐(防双锁),超时/取消后不留残影(防泄漏)。

**产品④映射**:异步取消 vs 注册表驱逐的确定性边界——与 hq4 提交栅栏同族(取消要么赢要么等完整,驱逐要么先驱逐要么不驱逐)。

## 设计 6:旋转 rebind(压缩旋转后边界跟随)

**位置**:`turn_lease.py:266-322`(rebind)+ `run.py:18603/19115`(调用点)

```
问题:压缩(hygiene pre-compression/agent 内压缩)可在回合飞行中旋转 durable
     session_id → flush 目标新 id → 序列化边界必须跟随,否则别名键解析新 id
     (topic tip-walk 落在新 child)可启动租约看不见的并发回合。

机制:同一个 _SessionLease 对象注册到新 id(旧映射保留到 idle 驱逐),
     两个 id 的获取者对同一把锁串行化——不移动锁状态,不碰 asyncio 内部。
     仅当前 holder 可 rebind(身份校验同 release),token 跟随新 id。

调用点(gateway/run.py):
- hygiene-compression 后(_rebind_turn_lease(_quick_key, run_generation, _hyg_new_sid),18603)
- agent 结果 session_id 旋转后(19115)——**仅当 session 仍是回合开始时的 id**
  (`session_entry.session_id == _run_start_session_id`)才 rebind:若回合中已被别处改过 id,
  不再抢夺归属,保持旧 id(fail-open 不覆盖他人状态)

边界:新 id 已有 live 租约(目标 session 正有别的回合)→ 大声记录,保持旧 id
     ——fail-open 绝不死锁(holder 回合中途不能等待)。
     rebind 不替换有唤醒等待者的目标(handoff 中的租约仍是活域)。

测试(test_rebind_moves_serialization_to_new_session_id + test_rebind_does_not_replace_target_during_waiter_handoff)
```

**正确性价值**:旋转不是换边界,是边界跟随身份——避免"压缩后丢失串行化"的隐藏窗。

**产品④映射**:知识库压缩旋转 session id 时,回合互斥边界必须跟随(参考架构 §四 已引)。

## 设计 7:跨代理绊线 note_turn_start(检测 + 命名,不预防)

**位置**:`agent/agent_runtime_helpers.py:462-486`

```
- 会话级在途注册表,检测"同一 session 上一回合未完成 turn-end persist 时新回合启动"
- 不预防任何东西——命名事件(two turn ids),让放行第二回合的 dispatch 路由可从日志识别
- 无论是否重叠都接管在途槽(崩溃未 persist 的回合最多一条警告)
```

**与租约的关系**:turn_lease 预防(串行化),note_turn_start 检测(告警)——双保险;日志配对(WARNING 命名 session + 两个 routing key)。

**产品④映射**:预防层 + 检测层并存——预防失效时检测层命名失败,运维可定位。

## 设计 8:已知限制(诚实声明)

**位置**:`turn_lease.py:41-48`

```
- CLI 进程经 CLI-continuity 共享 session 在任何进程内锁之外 → 需要 DB 级租约(独立设计)
- 回合中途压缩旋转留小别名窗:tip-walk 可解析新 child id 而 parent-holding 回合仍在飞
  → mid-turn binding-sync 站点是后续别名租约的正确位置
```

**产品④映射**:进程内租约 ≠ 跨进程租约——产品多进程写知识库时必须升级 DB 级租约(hq3 压缩租约是 SQLite 版参考)。

---

## 三、与四项目对比(Fencing Token 家族)

| 维度 | Pi(writer-leases) | Hermes 压缩租约(hq3) | Hermes turn lease(hq8) | 产品取 |
|------|-------------------|---------------------|----------------------|--------|
| 保护对象 | 多进程写知识库 | 章节压缩/索引重建互斥 | 回合 [load→run→flush] 串行化 | 三层都要 |
| 键 | 文件/存储 | session 压缩锁 | 解析后 session_id | 按域 |
| 代际 | 可用性刷新 seq | 无 | **generation(回合代)** | 代际作用域(四项目共证) |
| 释放校验 | 身份 | holder 限定 | **identity-checked + 幂等** | 身份校验 |
| 超时 | — | fail open(跳过压缩) | **fail-closed(拒绝回合)** | 按正确性成本分级 |
| 驱逐 | — | TTL 过期回收 | **有界注册表只驱逐 idle** | 有界 |
| 旋转 | — | — | **rebind 边界跟随** | 产品独有 |

**结论**:产品回合级互斥参考 = turn lease 全案(代际/身份/超时 fail-closed/有界/rebind)+ Pi writer-leases(跨进程)+ hq3 压缩租约(SQLite 事务原子获取)。

---

## 四、面试弹药

1. **"busy 守卫为什么漏"**:守卫按 routing key 键控,持久化按 session_id 所有——多对一映射让两个 agent 对象并发同一 transcript,per-key 守卫互相看不见
2. **"降级 token 是历史教训"**:超时曾返回降级 token → 授权无序列化运行 → 测试锁定 fail-closed(测试名直接写"fails_closed_instead_of_authorizing")
3. **"stale unwind 不能释放新回合"**:generation + identity 双重校验;/stop /new 的 generation bump 让旧 token 只弹自己
4. **"驱逐窗 vs 交接窗"**:asyncio.Lock.release 唤醒等待者的瞬时未锁窗,pending_acquires 计数防孤儿锁——同 session 永远一把锁
5. **"拒绝 ≠ 完成的回合"**:/goal judge 不消费拒绝通知——合成续跑循环被测试锁定

---

## 五、产品映射汇总

| 设计 | 产品用法 |
|------|---------|
| 获取时机(load 前) | 章节 [读库→分析→写库] 回合级互斥起点 |
| 代际 + 身份释放 | 陈旧解卷不能释放新回合(四项目代际模式) |
| 超时 fail-closed | 回合超时 = 拒绝写,不是尽力并发写 |
| 有界注册表 | 租约 map 有界,活租约优先 |
| pending_acquires 交接窗 | 异步取消/驱逐的确定性边界(与提交栅栏同族) |
| rebind 旋转跟随 | 压缩旋转 session id 后互斥边界跟随 |
| note_turn_start 绊线 | 预防层 + 检测层并存(检测命名失败) |
| 已知限制诚实声明 | 进程内 ≠ 跨进程——多进程需 DB 级租约 |

> 覆盖设计数:8(设计 1-8)
> 测试契约:test_turn_lease.py(12 用例,串行化/超时 fail-closed/驱逐窗/交接窗/rebind/接线全锁定)
