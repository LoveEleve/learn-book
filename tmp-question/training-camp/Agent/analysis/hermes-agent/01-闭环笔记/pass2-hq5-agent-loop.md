# hq5 Agent 主循环(AIAgent + run_conversation + 工具执行)— 产品②执行引擎主干

> 项目:Hermes(run_agent.py 8,425 行 + agent/conversation_loop.py 7,902 行 + model_tools.py 1,617 行 + agent/tool_executor.py 2,455 行 + agent/iteration_budget.py 62 + agent/agent_runtime_helpers.py 4,199 + agent/chat_completion_helpers.py 4,724 + agent/error_classifier.py 1,905 + agent/turn_finalizer.py 798)
> 假设:Hermes 单体主循环是"自主执行引擎"最完整运行时样本——预算/中断/转向/工具批/重试 fallback 链。
> 结论:✅ 成立——每个失败模式都有工程化处理,产品②执行引擎主干参考。

---

## 一、架构全景

```
┌────────────────────────────────────────────────────────────┐
│ 外层:run_conversation(conversation_loop.py:1494)           │
│   同步 while(预算+中断+grace call)/ steer 注入/ redirect   │
└──────────────┬─────────────────────────────────────────────┘
               ▼
┌────────────────────────────────────────────────────────────┐
│ 中层:API 调用(retry + fallback 链)                         │
│   重装饰(reasoning/缓存断点)/ sanitize / middleware        │
│   响应形状验证(fallback 链内消化失败)                      │
└──────────────┬─────────────────────────────────────────────┘
               ▼
┌────────────────────────────────────────────────────────────┐
│ 内层:工具执行(concurrent/sequential/segmented)             │
│   每工具中断检查/参数解析/桥接 unwrap/预算执行/结果回注    │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:迭代预算(consume/refund)

**位置**:`agent/iteration_budget.py:32-58`

```
- 线程安全 consume/refund;每 AIAgent 独立
- 父 max_iterations(默认 500);子 delegation.max_iterations(默认 50)
  → 父子总迭代可超父上限(预算独立)
- refund:execute_code(编程式工具调用)退还
- grace call:while (...) or agent._budget_grace_call(预算耗尽一次收尾)
```

**产品④映射**:章节分析预算——父子独立 + grace call 收尾(预算耗尽不给最后一轮会产出截断章节)。

## 设计 2:中断处理(每工具前检查)

**位置**:`agent/tool_executor.py:784-818`(concurrent)+ `1604-1640`(sequential)

```
if agent._interrupt_requested: 每个工具开始前检查
被跳过的工具仍写取消结果:[Tool execution cancelled — {name} was skipped...]
- 不能静默丢弃(模型需知道没跑)
- 逐工具 flush session DB + emit terminal hook(观测链不中断)
```

**产品④映射**:中断时"已获结果 vs 未执行工具"边界对模型透明(取消占位)。

## 设计 3:工具批三模式

**位置**:`agent/tool_executor.py:759/1604/2391`

```
- concurrent:线程池并行,结果按原始顺序回注
- sequential:串行(单调用/交互工具),每工具前中断检查
- segmented:混合批分段执行(finalize=False 移交 turn-end 收尾)

并行细节:
- 中断预检:interrupt 全部跳过写取消结果
- 参数解析先行(malformed 单独标记不阻塞批)
- Tool Search 桥接 unwrap:让所有下游钩子看真实工具名("OpenClaw 教训")
- scope 门:未授予 session 的工具在 checkpoint/hook/dispatch 前拒绝
```

**产品④映射**:并行工具执行——按序回注/中断预检/参数错误不阻塞批/观测链真实名。

## 设计 4:API 调用重试 + fallback 链

**位置**:`conversation_loop.py:2508-2760`

```
1. Nous rate limit 预检(跳过 API 调用,尝试 fallback)
2. fallback 时:
   - _reapply_reasoning_echo_for_provider(需 reasoning_content 的 provider
     (DeepSeek/Kimi/MiMo)补回声垫)
   - _redecorate_prompt_cache_for_provider(缓存断点按当前 provider 重渲染)
3. _build_api_kwargs → surrogate 清洗(整载荷 json.dumps 安全;
   session_search 的 ± 重文本是记录复现;其他叶子非法码点 → 不可重试 400)
4. middleware 链:apply_llm_request_middleware + pre_api_request hook
   - 浅拷贝给 hook(深拷贝会遍历每个工具结果和 base64 图)
5. 响应形状验证(transport 定制):codex failed/cancelled → 路由 fallback
   (终端失败在链内消化,不让错误逃出)
```

**关键 4a:请求代理通道(relay)**:非流式经 relay_llm.execute(call_role delegated/fallback/primary)+ defer_logical_completion。

**产品④映射**:provider 层 fallback 重装饰 + 载荷 sanitize + 响应验证进链。

## 设计 5:/steer 与 redirect(运行中纠偏)

**位置**:`conversation_loop.py:1781-1830`(steer)+ `242-320`(redirect)

```
/steer:
- 前 API 期间的 steer 在构建 api_messages 前 drain
- 注入最后 tool 消息(不注入 user 消息——破坏角色交替)
- 无 tool 消息 → 放回待处理(工具执行后 drain 拾取)

redirect:
- _apply_active_turn_redirect(消息修正);original_user_message 同步追加
- 响应与 redirect 跨线程竞争:_redirect_crossed_response 检测
  → 丢弃 stale 响应,从修正重建(不静默丢失)
```

**产品④映射**:运行中纠偏——steer 注入协议(角色交替安全)+ 响应竞争检测。

## 设计 6:回合级状态与收尾

**位置**:`run_agent.py:4187+` + `conversation_loop.py` 收尾

```
收尾顺序(契约):
1. 持久化(messages → session DB)
2. 外部记忆同步(_sync_external_memory_for_turn:中断跳过)
3. 后台 review 取消(上一轮 background_review 若还在,fire-and-forget interrupt
   ——防双 agent 同 session 并发出站调用)
4. 压缩状态重置(跨网关缓存 agent:_last_compaction_in_place = False)
```

**产品④映射**:回合收尾契约——持久化→记忆→后台取消→压缩重置。

## 设计 7:工具参数强制(严格 provider 兼容)

**位置**:`model_tools.py:777-1095`

```
- 字符串按 schema 声明类型强制("42"→42)
- 严格 provider(DeepSeek)工具 schema 缺 name → 整请求 400
  (normalize_tool_schema 防"一个坏 schema 禁用整个工具集")
- 各类 coercion:json/number/boolean 宽容解析
```

**产品④映射**:多 provider 生存技能——schema 级参数强制。

## 设计 8:失败分类学(FailoverReason 23 类)

**位置**:`agent/error_classifier.py:24-77` + `classify_api_error`

```
23 类:auth/auth_permanent/billing/rate_limit/upstream_rate_limit/overloaded/
server_error/timeout/ssl_cert_verification/context_overflow/payload_too_large/
image_too_large/model_not_found/provider_policy_blocked/content_policy_blocked/
format_error/invalid_encrypted_content/multimodal_tool_content_unsupported/
thinking_signature/long_context_tier/oauth_long_context_beta_forbidden/
llama_cpp_grammar_pattern/unknown

每个 → 恢复策略:
- auth 永久 → abort;context_overflow → 压缩不是 failover
- ssl → fail fast 不烧重试(TLS 确定性,重试复现同一失败)
- upstream_rate_limit → 换模型非轮换凭证(用户 key 健康)
```

**产品④映射**:失败处理决策——分类学 → 恢复策略的确定性映射。

## 设计 9:回合终结器(god-file 分解 seam)

**位置**:`agent/turn_finalizer.py:1-798`

```
run_conversation 尾部(预算总结/轨迹保存/会话持久化/响应转换/
结果 dict 组装/steer drain/记忆技能 review 触发)提取为独立 seam
- 行为中性迁移(god-file 分解运动)
- 惰性 import 防循环(不 import conversation_loop)
```

**产品④映射**:大函数分解为 seam——行为中性重构模式。

## 设计 10:TurnRetryState(重试状态)

**位置**:`agent/turn_retry_state.py:93` + conversation_loop 重试循环

- 跟踪重试计数/fallback 尝试/重建消息标志
- 与 provider 重试/fallback 链配合

**产品④映射**:重试状态机(跨 retry/fallback 的一致性)。

---

## 三、与 Pi/Reasonix 对比

| 维度 | Pi | Reasonix | Hermes |
|------|----|----------|--------|
| 循环 | 双层(内工具/外队列) | Controller+turnOrchestrator | 同步 while+内层 retry+工具批 |
| 预算 | max_iterations | taskBudget+goalTokenBudget | consume/refund+grace call |
| 中断 | handleRunFailure | 渐进收缩 | 每工具检查+取消占位 |
| 纠偏 | steer/followUp 双队列 | goal loop+refs | steer 注入+redirect 竞争检测 |
| 失败链 | 截断全失败 | 失败分类四类 | **fallback 链+23 类分类学+重装饰** |
| 工具批 | 顺序/并行 | execute_one 门控链 | 三模式+桥接 unwrap+scope 门 |

**结论**:产品②执行引擎——Pi 双层循环 + Hermes 预算(grace)/中断占位/fallback 重装饰/工具批三模式 + Reasonix fail-closed 门控链。

---

## 四、面试弹药

1. **"预算耗尽也给 grace call"**:不给最后一轮收尾的 agent 产出截断章节
2. **"中断时被跳过的工具写取消占位"**:模型必须知道哪些没跑
3. **"fallback 不是换 URL"**:reasoning 回声垫/缓存断点重渲染/载荷重 sanitize
4. **"/steer 不能注入 user 消息"**:破坏角色交替 → provider 拒绝
5. **"响应与 redirect 竞争"**:stale 响应丢弃重建,不静默丢修正
6. **"钩子必须观察真实工具名"**:tool_call 桥剥开(OpenClaw 教训)

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|----------|
| 迭代预算+grace | 章节预算(可收尾上限) |
| 每工具中断检查 | 纠偏中断语义(取消占位) |
| 工具批三模式 | 多工具并行(按序回注) |
| fallback 重装饰 | 多 provider 降级 |
| steer/redirect | 运行中纠偏 |
| 回合收尾契约 | 持久化→记忆→后台→压缩 |
| 参数强制 | 严格 provider 兼容 |
| 失败分类学 | 失败→策略确定性映射 |
| 回合终结器 | 大函数 seam 分解 |
| 重试状态 | 跨 retry/fallback 一致性 |

> 覆盖设计数:12(设计 1-10 + 4a/6 子设计)
