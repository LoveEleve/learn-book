# hq25 工具搜索桥(Tool Search Bridge)— 产品②"工具渐进披露"蓝本

> 项目:Hermes(tools/tool_search.py 1,078 行 + model_tools.py:311 接入 + OpenClaw 移植)
> 假设:MCP/插件工具增长使模型可见 tools 数组膨胀——Hermes 用三桥工具(tool_search/tool_describe/tool_call)渐进披露,是"工具延迟加载"的完整样本。
> 结论:✅ 成立——核心永不延迟/三档披露/无状态目录/桥接同路由/展示解包全具备,产品②"工具面可扩展"直接蓝本。

---

## 一、架构全景:渐进披露(progressive disclosure)

```
启用时,MCP/非核心插件工具替换为三桥工具,按需浮现;核心 Hermes 工具永不延迟。

┌────────────────────────────────────────────────────────────┐
│ 设计约束(openclaw-tool-search-report):                    │
│ * _HERMES_CORE_TOOLS 永不延迟("Always-load means           │
│   always-load. No exceptions.")                            │
│ * 目录无状态跨回合(每次从当前工具定义重建)——OpenClaw cron  │
│   回归教训(#84141):session 键控目录与活注册表漂移 → 静默工具脱落│
│ * 桥工具经 handle_function_call 同路由——guardrails/插件钩子│
│   /审批流/结果截断全部同样触发                             │
│ * 显示与轨迹解包:CLI 活动流/gateway/轨迹总显示底层工具     │
├────────────────────────────────────────────────────────────┤
│ 三档披露(2026 年 7 月计划):                               │
│ Tier 0:无 MCP/插件工具 → 纯透传,一切 eager               │
│ Tier 1:延迟工具清单 ≤ 预算(min(threshold_pct 默认 5%      │
│   × 上下文, listing_max_tokens))→ 桥 + 技能式清单(名称+    │
│   短描述;超预算降级纯名称清单)                             │
│ Tier 2:单工具清单超预算(如 Cloudflare 平 API 面 ~3300 工具 │
│   名称就 ~32K token)→ 裸桥 + 每服务器一行摘要(服务器名+    │
│   工具数)——模型仍知道哪些域可达;单独工具经 tool_search 发现│
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:核心永不延迟(分类)

**位置**:`tool_search.py:191-256`(classify_tools)+ `204`(is_deferrable)

```
classify_tools → (visible, deferrable):
- 核心(_HERMES_CORE_TOOLS)→ 永在 visible(不延迟)
- 桥工具名保留(BRIDGE_TOOL_NAMES)——注册重名被 registry 覆盖保护拒绝
- MCP/插件(非核心非桥)→ deferrable(可延迟)

测试:
- test_core_tools_never_defer / test_bridge_tools_never_defer
- test_unknown_tool_not_deferrable(未知工具不延迟——保守)
```

**正确性价值**:核心工具永远随 API 发送(窄腰原则);未知工具不延迟(保守)。

**产品④映射**:工具面的"核心永载 + 边缘可延迟"——与"窄腰/能力在边缘"宪法一致。

## 设计 2:无状态目录(防漂移)

**位置**:`tool_search.py:321-432`(CatalogEntry/build_catalog/search_catalog)

```
目录跨回合无状态——每次从当前工具定义重建:
  "This is the lesson from OpenClaw's cron regression(openclaw#84141):
  a session-keyed catalog that drifts out of sync with the live tool
  registry produces silent tool dropouts"

build_catalog:每工具条目(CatalogEntry:名称/短描述/搜索文本/来源分类)
search_catalog:BM25 评分 + limit 截断(默认 5,上限 max_search_limit 钳制)
```

**正确性价值**:**无状态 = 与活注册表永远一致**(session 键控目录漂移 = 静默脱落事故驱动);BM25 检索 + 有界 limit。

**产品④映射**:工具目录无状态重建——"目录漂移 = 静默脱落"是事故驱动的核心纪律。

## 设计 3:三档披露(激活决策)

**位置**:`tool_search.py:275-319`(should_activate/listing_token_budget)+ `772-850`(assemble_tool_defs)

```
assemble_tool_defs(幂等——桥已在内则 no-op):
- 无 deferrable → 透传(tier 0)
- should_activate(config, deferrable_tokens, context_length):
  延迟工具 token 估算 ≤ 预算 → 激活
- 激活 → 三桥替换 + 清单(按预算档位)

token 估算(CHARS_PER_TOKEN=4.0):
  "Underestimating leads to false negatives(tool search not activated
  when it should);overestimating leads to false positives.4.0 errs
  slightly toward underestimating,which is the safer default"
  ——低估 = 安全方向(该激活时激活)

清单降级:
  full(name+短描述)→ 超预算 → names-only → 超预算 → 每服务器一行摘要
```

**正确性价值**:
1. 激活判定 = token 预算(可量化);低估安全方向
2. 清单降级链(完整→名称→服务器摘要)——模型始终知道哪些域可达
3. 幂等装配(桥已在内 no-op)

**产品④映射**:工具面披露预算——token 预算量化 + 降级链(域可达性永不丢)。

## 设计 4:桥接同路由(guardrails 不丢)

**位置**:`tool_search.py:884-945`(dispatch 三桥)+ `966-1078`(校验/解包)

```
dispatch_tool_search/describe:构建目录 + 检索 → 命中格式化
validate_deferred_call_args:延迟工具调用参数校验
resolve_underlying_call:解包底层调用

关键:桥工具经 handle_function_call 同路由——
  guardrails、插件 pre/post 钩子、审批流、结果截断全部同样触发
  ("Bridge tools route through model_tools.handle_function_call exactly
  like a direct call")
```

**正确性价值**:桥接不绕过安全层——延迟工具走同一管线(guardrails/钩子/审批/截断)。

**产品④映射**:工具面扩展不丢安全——桥接同路由是纪律。

## 设计 5:展示解包(用户看底层工具)

**位置**:`tool_search.py:850-883`(is_bridge_tool/_format_search_hit/_available_source_summary)

```
显示与轨迹解包:
  CLI 活动流/gateway/保存轨迹总显示底层工具名,非桥名
  ("Display and trajectory unwrap is implemented here so the user
  always sees the underlying tool, not the bridge")
```

**正确性价值**:用户可见性——桥是内部机制,用户看到的是真实工具。

**产品④映射**:可观测性解包——桥内部化,显示层永远真实。

---

## 三、与四项目对比(工具延迟加载)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes tool_search |
|------|----|----------|----------|-----|--------------------|
| 延迟加载 | 工具预设分级 | — | 工具注册表 | 能力缝三角色 | **三桥渐进披露** |
| 核心保护 | — | — | — | — | **核心永不延迟** |
| 目录 | — | — | — | — | **无状态重建(防漂移)** |
| 预算 | — | — | — | token 预算 | **listing token 预算 + 降级链** |
| 桥接 | — | — | — | — | **同路由(guardrails 不丢)** |
| 显示 | — | — | — | — | **解包(用户看底层工具)** |

**结论**:产品"工具面可扩展"参考 = Hermes tool_search(三桥渐进披露 + 无状态目录 + 预算降级)+ dsh 能力缝(provider 切换)+ Pi 工具预设分级。**Hermes 独特贡献:无状态目录(防静默脱落)+ 三档预算降级 + 桥接同路由**。

---

## 四、面试弹药

1. **"核心永不延迟"**:_HERMES_CORE_TOOLS 永随 API——窄腰原则("Always-load means always-load")
2. **"无状态目录防静默脱落"**:session 键控目录与活注册表漂移 = OpenClaw cron 回归(#84141)——每次重建
3. **"低估是安全方向"**:token 估算 4.0 偏低估——该激活时激活,不假阳性
4. **"清单降级链"**:完整→名称→服务器摘要——模型始终知道哪些域可达(Cloudflare 3300 工具案例)
5. **"桥接同路由"**:guardrails/插件钩子/审批/截断全部同样触发——延迟不丢安全
6. **"显示解包"**:用户看到底层工具非桥——桥是内部机制

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 核心永不延迟 | 窄腰原则(核心永载) |
| 无状态目录 | 防静默脱落(事故驱动) |
| 三档披露 | 预算量化 + 降级链(域可达性) |
| 桥接同路由 | 延迟不丢安全层 |
| 显示解包 | 用户看底层工具 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_tool_search.py(30 用例:配置解析/核心与桥永不延迟/未知不延迟/分类/token 估算/BM25 检索/limit 钳制/无 deferrable 透传/幂等/查询必填/空搜索保持源可达/短描述截断/validator 不阻塞不可验证工具/桥调用分发)
> 位置:classify_tools :230 / assemble_tool_defs :772 / search_catalog :432 / dispatch_tool_search :884 / validate_deferred_call_args :966 / resolve_underlying_call :1019
> 常量:CHARS_PER_TOKEN=4.0/BRIDGE_TOOL_NAMES 三桥/tool_search 预算 threshold_pct=5%
> 移植:OpenClaw(openclaw-tool-search-report;#84141 教训)
