# hq26 监控平面(Monitoring Plane)— 产品②"可观测"蓝本

> 项目:Hermes(agent/monitoring/ 9 文件 2,037 行:events/emitter/otlp_exporter/gateway_health/gateway_health_export/policy/redaction/cron_health)
> 假设:遥测必须 content-free(无 prompt/消息/工具参数)——Hermes 用 typed 事件 + 无条件脱敏 + OTLP 导出,是"安全可观测"的完整样本。
> 结论:✅ 成立——content-free 事件/脱敏 fail-closed/install_id 匿名标识/OTLP 可选依赖/fail-isolated 分派全具备,产品②"可观测"直接蓝本。

---

## 一、架构全景:content-free 遥测

```
┌────────────────────────────────────────────────────────────┐
│ typed 事件(events.py,仅三种形状):                         │
│   GatewayHealthEvent(content-free 健康快照/生命周期)       │
│   GatewayDiagnosticEvent(脱敏诊断,operator 自有可观测)     │
│   CronExecutionEvent(content-free cron 执行投影)           │
│   ——无 prompt/消息/工具参数/会话历史/用量分析             │
├────────────────────────────────────────────────────────────┤
│ 脱敏(redaction.py,无条件一次清理):                       │
│   秘密先(force=True 用户配置不能关)+ fail-closed(红actor    │
│     不能跑 → [redaction-unavailable] 不发原文)             │
│   PII 后(email→[email]/phone→[phone]/uuid→[id])            │
│   "deliberately no setting to weaken this"                 │
├────────────────────────────────────────────────────────────┤
│ 身份(policy.py):install_id 稳定伪匿名标识                  │
│   (service.instance.id;可轮换:清 config 即换)             │
├────────────────────────────────────────────────────────────┤
│ 导出(otlp_exporter.py):OTLP 可选依赖(fail-isolated)       │
│ emitter:分派线程 fail-isolated(遥测故障不碰主线程)        │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:typed 事件三形状(content-free)

**位置**:`events.py:20-86`(三类事件)

```
GatewayHealthEvent:name/state 迁移/active_agents/gateway_busy/
  drainable/platform_count/fatal_platform_count/profile/install_id/
  version/supervision_mode/pid/ts_ns——纯健康快照,零内容

GatewayDiagnosticEvent:subsystem/error_class/error_code/platform/
  severity/source_logger——脱敏诊断(operator 自有可观测)

CronExecutionEvent:status/job_key/duration_ms/delivery_outcome/
  error_class——cron 执行投影

"These are the only event shapes the monitoring plane emits:
no prompts, messages, tool args/results, session history, or usage
analytics."
```

**正确性价值**:content-free 是硬边界——遥测不携带任何对话内容/工具参数;三形状全列举防漂移。

**产品④映射**:遥测 content-free——可观测不牺牲隐私;事件形状显式全列举。

## 设计 2:无条件脱敏(fail-closed + 无弱化开关)

**位置**:`redaction.py:23-66`(redact_for_export)

```
一次无条件清理,无模式无旋钮——每个出进程字符串都过 redact_for_export:
1. 秘密先:redact_sensitive_text(force=True 用户配置不能关)
   + bearer/token 形状模式(xoxb-/sk-/ghp- 等)+ 星号字面量
   + ★ fail-closed:红actor 不能跑 → [redaction-unavailable] 不发原文
2. PII 后:email→[email]/phone→[phone](E.164 保守)/uuid→[id]

"deliberately no setting to weaken this"——无弱化开关是设计
```

**正确性价值**:
1. 秘密 fail-closed(红actor 坏 = 不发,绝不发原文)
2. 无弱化开关(设计层面)
3. PII 保守(不误伤代码/ID)

**产品④映射**:遥测脱敏无条件——fail-closed + 无弱化旋钮 + 保守 PII。

## 设计 3:install_id 匿名身份

**位置**:`policy.py:17-54`(ensure_install_id)

```
稳定可重置伪匿名标识:
- 空 → 铸 UUID 并持久化回 config(可轮换:清 monitoring.install_id 即换)
- 必须存活重启(成为导出信号的 service.instance.id)
- 持久化失败 fail-open(只读 home/托管 scope → 临时 id,下次重铸)
- "carries no account identity"
```

**正确性价值**:可区分实例但不可关联账户;可轮换(隐私控制)。

**产品④映射**:遥测匿名身份——稳定可重置,不携带账户身份。

## 设计 4:OTLP 可选依赖 + fail-isolated 分派

**位置**:`otlp_exporter.py:1-270` + `emitter.py:36-190`

```
- OTLP 导出可选依赖(缺库不影响主流程)
- emitter 分派线程 fail-isolated:遥测故障不碰主线程/主循环
- 导出信号含 install_id(service.instance.id)/version/profile 等
- reset_emitter_for_tests(测试隔离)
```

**正确性价值**:遥测可观测性失败不影响被观测对象(与 delivery/lifecycle"主数据优先"同族)。

**产品④映射**:遥测可选 + fail-isolated——可观测层故障零影响主流程。

## 设计 5:gateway 健康导出

**位置**:`gateway_health.py:1-469` + `gateway_health_export.py:1-643` + `cron_health.py`

```
- gateway_health:健康状态机(健康快照/生命周期)
- gateway_health_export:导出层(OTLP 资源属性/事件)
- cron_health:cron 执行健康投影
```

**产品④映射**:健康导出的分层(状态机 + 导出层 + cron 投影)。

---

## 三、与四项目对比(可观测)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes monitoring |
|------|----|----------|----------|-----|-------------------|
| 遥测事件 | — | event Kind | OTLP/runID | telemetry-otel | **content-free typed 三形状** |
| 脱敏 | — | — | 密钥检测 | — | **无条件 fail-closed + 无弱化** |
| 身份 | — | — | runID 8 位随机 | — | **install_id 稳定可轮换** |
| 导出 | — | — | OTLP | OTLP | **OTLP 可选依赖 + fail-isolated** |
| 纪律 | — | — | — | 模型可见⟺已记录 | **content-free 硬边界** |

**结论**:产品"可观测"参考 = Hermes monitoring(content-free + 无条件脱敏 + 匿名身份)+ OpenCode runID(审计链路)+ dsh telemetry-otel。**Hermes 独特贡献:content-free 硬边界 + 脱敏 fail-closed + install_id 可轮换**。

---

## 四、面试弹药

1. **"content-free 是硬边界"**:遥测不携带 prompt/消息/工具参数/会话历史——三形状全列举防漂移
2. **"脱敏 fail-closed"**:红actor 不能跑 → [redaction-unavailable] 不发原文——绝不泄漏
3. **"无弱化开关是设计"**:"deliberately no setting to weaken this"——不是没做,是不做
4. **"install_id 可轮换"**:清 config 即换身份——可区分实例但不可关联账户
5. **"OTLP 可选 + fail-isolated"**:缺库/故障不碰主流程——可观测层失败零影响

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| content-free 事件 | 遥测不牺牲隐私(三形状全列举) |
| 无条件脱敏 | 秘密 fail-closed + PII 保守 + 无弱化 |
| install_id | 匿名身份稳定可轮换 |
| OTLP 可选 | 导出可选依赖 |
| fail-isolated | 遥测故障零影响主流程 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/monitoring/(test_emitter 4/test_export_redaction 4/test_cron_health_export 5 = 13 用例)+ gateway/agent 关联测试
> 位置:events.py:20/46/67(三事件)/ redaction.py:redact_for_export / policy.py:ensure_install_id / emitter.py:MonitoringEmitter / otlp_exporter.py
> 结构:agent/monitoring/ 9 文件 2,037 行
