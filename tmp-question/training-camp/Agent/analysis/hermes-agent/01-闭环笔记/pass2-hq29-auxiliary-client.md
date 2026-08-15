# hq29 辅助客户端路由(Auxiliary Client)— 产品②"辅助任务统一入口"蓝本

> 项目:Hermes(agent/auxiliary_client.py 10,432 行 + tests/agent/test_auxiliary*.py 22 文件 313 用例)
> 假设:侧 LLM 任务(审查/摘要/评估/图像)需统一路由——Hermes 的任务级 provider 解析 + 自动降级 + 并发限流是"辅助任务统一入口"的完整样本。
> 结论:✅ 成立——任务级解析/自动路由链/固定温度契约/并发信号量/取消/进度/多后端 shim 全具备,产品②"辅助任务"直接蓝本(goal_judge/背景审查等全依赖它)。

---

## 一、架构全景:侧任务统一入口

```
┌────────────────────────────────────────────────────────────┐
│ 解析(任务级):                                             │
│   get_text_auxiliary_client(task)/get_vision_auxiliary_client│
│   → resolve_provider_client → _resolve_auto(自动路由链)    │
│   (goal_judge/curator/摘要/embedding 等每任务可 pin 自己的 │
│    provider/model/base_url/max_tokens/reasoning_effort)    │
├────────────────────────────────────────────────────────────┤
│ 自动路由链(文本任务 auto 模式):                            │
│   1. 用户主 provider + 主模型(聚合器/直连/Anthropic/Codex  │
│      全类型用)                                             │
│   2. Nous Portal(~/.hermes/auth.json 活动 provider)        │
│   3. 直连 API-key provider(z.ai/GLM/Kimi/Moonshot/MiniMax) │
│   step-2 回退 :free SKU;auxiliary.openrouter_model 覆盖     │
├────────────────────────────────────────────────────────────┤
│ 执行(call_llm):                                           │
│   任务级信号量(并发限流)+ 温度契约(_fixed_temperature_    │
│   for_model)+ 取消(aux_interrupt_protection)+ 进度钩子     │
│   + 多后端 shim(_OpenAIProxy/Anthropic/Codex/azure 适配)   │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:任务级解析(每任务可 pin)

**位置**:`auxiliary_client.py:6878`(get_text_auxiliary_client)+ `6049`(resolve_provider_client)+ `5916`(_resolve_auto)

```
任务级 provider 解析:
  auxiliary.<task>.{provider,model,base_url,api_key,max_tokens,reasoning_effort}
  ——curator/vision/embedding/title/session_search/goal_judge 每任务独立

get_text_auxiliary_client(task) → (client, model)
get_vision_auxiliary_client → 视觉后端专用
resolve_provider_client:中央路由(chat_completions/codex/anthropic/bedrock shim)
```

**正确性价值**:任务级配置隔离(每任务可 pin 自己的模型/成本);中央路由统一 shim。

**产品④映射**:辅助任务统一入口——任务级配置 + 中央路由(goal_judge/审查/摘要全经此)。

## 设计 2:自动路由链(降级次序)

**位置**:`auxiliary_client.py:5721`(_resolve_auto_route)+ `5916`(_resolve_auto)

```
文本任务 auto 模式降级链:
  1. 用户主 provider + 主模型(不管 provider 类型——聚合器/直连/
     native Anthropic/Codex)
  2. Nous Portal(auth.json 活动 provider)
  3. 直连 API-key provider(z.ai/GLM/Kimi/Moonshot/MiniMax/MiniMax-CN)
  step-2 回退 :free SKU;openrouter_model 覆盖

视觉任务 auto:选中主 provider(若支持视觉后端)→ 专用视觉链
```

**正确性价值**:主 provider 优先(成本/一致)+ 多级降级(主不可用 → Portal → 直连);:free 回退。

**产品④映射**:辅助任务路由降级链——主优先 + 多级回退(与主循环 fallback 链同族)。

## 设计 3:温度契约 + 并发限流

**位置**:`auxiliary_client.py:693`(_fixed_temperature_for_model)+ `8963`(call_llm)

```
_fixed_temperature_for_model:模型族固定温度契约
  (kimi/_arcee_trinity_thinking/_codex_gpt54_or_gpt55 特化)
call_llm:
  _acquire_sync_aux_semaphore(task)——任务级信号量(并发限流)
  _notify_aux_progress/aux_progress_hook(进度钩子)
  aux_interrupt_protection(中断保护,显式取消决策)
  _run_protected_sync_provider_call(同步调用保护)
```

**正确性价值**:温度契约(可复现)+ 任务级信号量(防并发洪泛)+ 中断/进度工程化。

**产品④映射**:辅助调用工程化——温度契约 + 并发限流 + 中断/进度(可靠侧调用)。

## 设计 4:多后端 shim

**位置**:`auxiliary_client.py:93`(_OpenAIProxy)+ `259`(_create_openai_client)+ `190`(_resolve_aux_verify)

```
_OpenAIProxy:openai 客户端代理(延迟加载/超时/重试包装)
_create_openai_client(api_key, base_url):统一客户端工厂
_resolve_aux_verify:base_url 校验
chat_completions/codex/anthropic/bedrock 适配 shim(域发现 v10)
```

**正确性价值**:多后端统一 shim——同一入口适配各 provider API 形状。

**产品④映射**:辅助客户端多后端——统一工厂 + shim 适配(provider 差异内化)。

## 设计 5:全部侧任务依赖(共享基础)

**位置**:调用方(goals.py judge_goal / background_review / curator / moa / insights)

```
goal_judge(goals.py:1060):call_llm(task="goal_judge")
background_review:call_llm(task="background_review")
curator/压缩摘要/moa 聚合/embedding 全经此

——"辅助任务统一入口"是侧 LLM 基础设施(10K 行 = 最大支撑文件)
```

**正确性价值**:统一入口消除重复路由逻辑(每个侧任务不各自解析 provider)。

**产品④映射**:侧任务基础设施集中——统一路由/限流/温度(全侧任务共享)。

---

## 三、与四项目对比(辅助任务路由)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes auxiliary_client |
|------|----|----------|----------|-----|-------------------------|
| 辅助路由 | — | boundedllm | — | llm/stream 瀑布 | **任务级解析 + 自动链** |
| 降级 | — | 渐进收缩 | — | 瀑布拦截 | **主→Portal→直连 + :free** |
| 温度 | — | 温度 0 | — | — | **模型族固定温度契约** |
| 限流 | — | 多层预算 | — | — | **任务级信号量** |
| 后端 | — | — | — | — | **多 shim(chat/codex/anthropic/bedrock)** |

**结论**:产品"辅助任务"参考 = Hermes auxiliary_client(任务级路由 + 自动链 + 温度契约 + 限流)+ Reasonix boundedllm(有界调用)。**Hermes 与 Reasonix 同"有界侧调用"哲学;Hermes 是任务级统一入口(10K 行全支撑)**。

---

## 四、面试弹药

1. **"任务级 pin"**:auxiliary.<task>.{provider,model,...}——每侧任务独立配置(goal_judge/curator/摘要)
2. **"自动路由链"**:主 provider 优先 → Nous Portal → 直连 API-key → :free 回退
3. **"温度契约可复现"**:模型族固定温度(kimi/arcee/codex 特化)——侧调用确定性
4. **"任务级信号量"**:每任务并发限流——防侧任务洪泛
5. **"多后端 shim"**:chat_completions/codex/anthropic/bedrock 统一工厂——provider 差异内化

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 任务级解析 | 每侧任务独立配置 |
| 自动路由链 | 主→Portal→直连降级 |
| 温度契约 | 侧调用确定性 |
| 并发限流 | 任务级信号量 |
| 多后端 shim | provider 差异内化 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/agent/test_auxiliary*.py 22 文件 313 用例(anthropic 池回退/自定义 anthropic/azure foundry/base_url 校验 #52608/取消/进度/温度契约/路由链)
> 位置:call_llm :8963 / get_text_auxiliary_client :6878 / resolve_provider_client :6049 / _resolve_auto :5916 / _resolve_auto_route :5721 / _fixed_temperature_for_model :693
> 依赖方:goal_judge(goals.py)/background_review/curator/moa/insights 全经此
