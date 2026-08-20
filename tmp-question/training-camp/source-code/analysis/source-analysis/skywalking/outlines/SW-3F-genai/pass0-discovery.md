# SW-3F GenAI Analyzer — Pass 0 发现

> 模块: `oap-server/analyzer/gen-ai-analyzer`
> 生产 Java: **9**
> 测试 Java: **2**（本轮后）
> 日期: 2026-08-18

## 1. 域职责
- 从 SkyWalking span tags / Zipkin span tags 中提取 GenAI 调用指标
- 归一化为 `GenAIMetrics`
- 再转换成虚拟 GenAI service / instance / provider / model 四类 `Source`
- 在模块启动时装载 GenAI OAL 定义

这不是通用 LLM 管理模块，也不是 query/storage 域；它是一个薄分析器 + source 映射层。

## 2. 入口链
### SW trace 主链
`VirtualServiceAnalysisListener`
→ `VirtualGenAIProcessor.prepareVSIfNecessary(...)`
→ `IGenAIMeterAnalyzerService.extractMetricsFromSWSpan(...)`
→ `transferToSources(...)`
→ 4 个 `Source`

### Zipkin 主链
`SpanForward`
→ `extractMetricsFromZipkinSpan(...)`
→ `transferToSources(...)`

## 3. 模块装配
`GenAIAnalyzerModuleProvider`：
- `prepare()`：加载配置、构造 `GenAIProviderPrefixMatcher`、注册 `IGenAIMeterAnalyzerService`
- `start()`：装载 `oal/virtual-gen-ai.oal`，注入 `NamingControl`
- `requiredModules()`：只依赖 `CoreModule`

## 4. 输入标签与推断规则
关键标签：
- `gen_ai.response.model`
- `gen_ai.provider.name`
- `gen_ai.system`（Zipkin/OTLP legacy fallback）
- `gen_ai.usage.input_tokens`
- `gen_ai.usage.output_tokens`
- `gen_ai.server.time_to_first_token`

规则：
- model 名为空 -> 直接跳过
- provider 缺失 -> 用 matcher 依据 model prefix 推断
- model 配置缺失 -> cost=0，但指标仍继续产出
- token/ttft 解析失败 -> 回退 0

## 5. 输出模型
`transferToSources(...)` 固定输出 4 个 source：
1. `ServiceMeta`（provider 作为虚拟服务）
2. `ServiceInstance`（model 作为虚拟实例）
3. `GenAIProviderAccess`
4. `GenAIModelAccess`

并统一通过 `NamingControl` 做 service / instance 名规范化。

## 6. 首轮质疑点
- Q1: Zipkin 分支是否与 SW 分支保持同等 provider fallback 语义
- Q2: `transferToSources(...)` 的 4 个 source 是否完整、顺序是否稳定
- Q3: cost double -> long rounding 是否是设计，而非精度丢失 bug
- Q4: provider/model 为空、unknown model、legacy `gen_ai.system` 的行为
- Q5: `SegmentObject` 参数在 SW 分支是否实际参与计算

## 7. 当前结论
- 现有官方测试主要覆盖 SW span 路径
- Zipkin 路径、legacy provider fallback、source 转换、NamingControl 修整，是本轮需要重点补证的薄弱区
