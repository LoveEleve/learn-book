# hq28 MoA 多层聚合(Mixture-of-Agents)— 产品②"多模型协作"蓝本

> 项目:Hermes(agent/moa_loop.py 2,453 行 + tests/agent/test_moa*.py 22 文件 106 用例)
> 假设:并行引用模型给建议 + 聚合器综合——Hermes 的 MoA 运行时是"多模型协作"的完整样本(隐私/预算/缓存/降级全工程化)。
> 结论:✅ 成立——并行槽 fan-out/聚合器无上限/隐私过滤/参考裁剪/失败降级/缓存控制/温度契约全具备,产品②"多模型协作"直接蓝本。

---

## 一、架构全景:并行引用 + 聚合

```
┌────────────────────────────────────────────────────────────┐
│ 引用 fan-out(_run_references_parallel):                   │
│   并行槽(slot)调参考模型;参考消息裁剪(_trim_messages_for_  │
│   reference);工具结果预算(_REFERENCE_TOOL_RESULT_BUDGET)   │
├────────────────────────────────────────────────────────────┤
│ 聚合(aggregate_moa_context):                              │
│   引用模型建议综合为聚合上下文                             │
│   ★ 聚合器调用永不设上限("reference_max_tokens applies    │
│     ONLY to the reference fan-out——硬编码 cap 曾截断长聚合  │
│     综合 #53580")                                          │
├────────────────────────────────────────────────────────────┤
│ 隐私(moa.privacy_filter — ''|display|full):               │
│   reference 输出脱敏(秘密形状经 redact_sensitive_text,     │
│     MoA 过滤器从不重实现秘密红action)                      │
├────────────────────────────────────────────────────────────┤
│ 呈现/降级:                                                │
│   _degraded_notice(失败参考标签 + loud 策略)               │
│   参考引导(_attach_reference_guidance/peel——聚合输入标记)  │
├────────────────────────────────────────────────────────────┤
│ 门面:MoAClient(chat.completions facade + usage 消费)       │
│ 追踪:moa_trace(侧通道 JSONL,见域发现 v36)                 │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:并行引用 fan-out(槽 + 预算)

**位置**:`moa_loop.py:795`(_run_references_parallel)+ `661`(_trim_messages_for_reference)+ `964`(_truncate_tool_result)

```
_run_references_parallel:并行槽调参考模型
- 参考消息裁剪(引用不需要全部上下文——裁剪到参考相关)
- 工具结果预算(_REFERENCE_TOOL_RESULT_BUDGET + _render_tool_calls
  结构化呈现工具调用)
- _successful_references/_failed_reference_labels(成功/失败分类)

槽配置:
- _slot_runtime/_slot_reasoning_config/_merge_slot_extra_body(每槽运行时/
  推理配置/extra_body 合并)
- _maybe_apply_moa_cache_control(缓存控制)
- _preset_temperature(预设温度契约)
```

**正确性价值**:并行 fan-out + 裁剪/预算(参考输入有界)+ 槽级配置(每模型独立运行时)。

**产品④映射**:多模型协作的 fan-out——并行槽 + 输入有界(裁剪/预算)+ 槽级配置。

## 设计 2:聚合器无上限(事故驱动)

**位置**:`moa_loop.py:1233-1392`(aggregate_moa_context)

```
★ 核心纪律:reference_max_tokens 只应用于引用 fan-out——
  聚合器自身综合调用永不设上限:
  "A hardcoded cap on the aggregator call previously truncated long
  aggregator syntheses(#53580)——passing reference_max_tokens to both
  calls here would silently reintroduce that bug"

temperature/aggregator_temperature 默认 None(call_llm 省略 temperature 当 None)
```

**正确性价值**:聚合输出不受引用预算污染(#53580 事故驱动)——预算分离是正确性。

**产品④映射**:多模型聚合的预算分离——引用有界/聚合无上限(截断事故驱动)。

## 设计 3:隐私过滤(三档)

**位置**:`moa_loop.py:57-181`(红action 族)+ `76`(_moa_privacy_mode)

```
moa.privacy_filter — ''|display|full:
- 引用输出脱敏(参考块/聚合 prompt 的保存)
- ★ "secret/credential shapes(API-key...)via redact_sensitive_text——
  the MoA filter never re-implements those"(秘密红action 复用,不重实现)
- _redact_reference_outputs/_redact_trace_messages/_redact_trace_accounting
  (参考输出/追踪消息/记账三面脱敏)
```

**正确性价值**:隐私过滤三档;秘密红action 复用全局(不重实现,防漏)。

**产品④映射**:多模型协作的隐私——三档过滤 + 秘密复用全局红action。

## 设计 4:失败降级(参考失败不炸聚合)

**位置**:`moa_loop.py:1202-1231`(_is_failed_reference/_degraded_notice)+ `1227`(策略)

```
- _is_failed_reference:失败参考识别
- _degraded_notice(failed_labels, policy):失败标签 + 降级策略(loud 等)
  ——参考失败 → 降级通知(聚合仍进行,但标注哪些参考失败)
- 参考引导:聚合输入带 guidance 标记,聚合后 peel(输入/输出分离)
```

**正确性价值**:参考失败不炸聚合——降级通知标注(诚实);guidance 输入/输出分离。

**产品④映射**:多模型协作的降级——部分参考失败聚合继续 + 诚实标注。

## 设计 5:MoAClient 门面 + 追踪

**位置**:`moa_loop.py:2313-2453`(MoAClient/build_moa_facade)+ `1534`(MoAChatCompletions)

```
MoAClient(preset_name, reference_callback, agent):
  chat.completions facade(MoAChatCompletions)
  consume_reference_usage(引用 fan-out 用量弹出——不污染主计数)
  last_aggregator_slot(解析的聚合槽)

追踪:moa_trace 侧通道(完整 MoA 回合 JSONL,端到端离线审计;
  不进 messages 表/永不重放——破坏角色交替;默认零开销)
```

**正确性价值**:门面统一入口;引用用量独立消费(不与主回合混合);追踪侧通道隔离。

**产品④映射**:多模型协作门面——统一入口 + 用量分离 + 侧通道追踪。

---

## 三、与四项目对比(多模型协作)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes MoA |
|------|----|----------|----------|-----|------------|
| 多模型 | — | planner+executor 双模型 | — | — | **并行引用 + 聚合** |
| 聚合上限 | — | — | — | — | **聚合无上限(#53580 事故)** |
| 隐私 | — | — | — | — | **三档过滤 + 秘密复用** |
| 降级 | — | 分场景降级 | — | — | **失败参考降级通知(loud)** |
| 用量 | — | — | — | — | **引用/聚合用量分离** |

**结论**:产品"多模型协作"参考 = Hermes MoA(并行 fan-out + 聚合无上限 + 隐私三档 + 降级)+ Reasonix 双模型(planner/executor 角色)。**Hermes 是"同任务多视角综合",Reasonix 是"异任务分工"——两种多模型模式**。

---

## 四、面试弹药

1. **"聚合无上限是事故驱动"**:硬编码 cap 曾截断长聚合综合(#53580)——reference_max_tokens 只给 fan-out
2. **"MoA 过滤器从不重实现秘密红action"**:秘密形状复用 redact_sensitive_text——不重实现防漏
3. **"引用失败不炸聚合"**:失败参考 → 降级通知(loud 策略标注失败标签)
4. **"用量分离"**:consume_reference_usage 独立——引用 fan-out 不污染主计数
5. **"追踪侧通道"**:moa_trace JSONL 端到端审计,不进 messages 表(角色交替)

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 并行 fan-out | 多槽参考 + 输入有界(裁剪/预算) |
| 聚合无上限 | 预算分离(截断事故驱动) |
| 隐私三档 | 参考输出脱敏(秘密复用) |
| 失败降级 | 部分参考失败聚合继续 + 诚实标注 |
| 门面 + 用量分离 | 统一入口 + 独立用量 + 侧通道追踪 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/agent/test_moa*.py 22 文件 106 用例(聚合器缓存控制/成本槽/冷启动缓存 #66793/context max tokens/可观测桥/进度/静默参考输出/推理 effort/参考 system prompt/slot API mode/slot max tokens/API mode 切换/追踪流式捕获等)
> 位置:aggregate_moa_context :1233 / _run_references_parallel :795 / _redact_reference_outputs :84 / _degraded_notice :1227 / MoAClient :2313
> 配置:moa.privacy_filter(''|display|full)/preset 温度契约/降级策略(loud)
