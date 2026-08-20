# SW-3F GenAI Analyzer — Outline

> 模块: `oap-server/analyzer/gen-ai-analyzer`
> 日期: 2026-08-18

## 1. 域定位
`SW-3F` 是一个薄分析器：
- 输入：SW trace span / Zipkin span 上的 GenAI 相关 tags
- 中间态：`GenAIMetrics`
- 输出：4 类与虚拟 GenAI 服务相关的 `Source`

它并不负责模型注册、真实计费、存储、查询或 OAL 执行本身。

## 2. 主体链路
- 模块启动：`GenAIAnalyzerModuleProvider.prepare/start`
- SW 分支：`VirtualGenAIProcessor -> extractMetricsFromSWSpan -> transferToSources`
- Zipkin 分支：`SpanForward -> extractMetricsFromZipkinSpan -> transferToSources`
- source 输出：`ServiceMeta / ServiceInstance / GenAIProviderAccess / GenAIModelAccess`

## 3. 关键行为
### 3.1 provider / model 识别
- `gen_ai.response.model` 缺失 -> 直接跳过
- `gen_ai.provider.name` 缺失 -> 用 model prefix matcher 推断
- Zipkin 还额外兼容 `gen_ai.system`

### 3.2 token / ttft / latency
- token、ttft 解析失败 -> 记 0
- SW latency = `endTime - startTime`
- Zipkin latency = `duration / 1000`（microseconds -> milliseconds）
- Zipkin status = `error` tag 是否为空

### 3.3 estimated cost
- cost 按配置中的 `per million tokens` 费率线性累计
- `GenAIMetrics` 内保留 `double`
- provider/model access source 中通过 `Math.round(...)` 转成 `long`
- 未命中 model config 时，cost=0

## 4. 配置与 matcher
- 配置实际由 `GenAIPricingConfigLoader` 加载
- 模块侧 `GenAIConfigLoader` 只是把通用 pricing config 映射成模块配置对象
- `GenAIProviderPrefixMatcher` 是 `GenAIModelMatcher` 的薄包装

因此当前模块真实逻辑重点不在“配置解析算法”，而在 span tag -> metrics/source 的归一化。

## 5. 输出 source 语义
`transferToSources(...)` 固定输出：
- `ServiceMeta`：虚拟 GenAI provider 服务
- `ServiceInstance`：虚拟 GenAI model 实例
- `GenAIProviderAccess`：provider 访问指标
- `GenAIModelAccess`：model 访问指标

provider / model 名都经过 `NamingControl` 规范化；因此输出实体名可能被裁剪，不总是原始 tag 文本。

## 6. 测试覆盖
原有测试覆盖：
- 配置装载
- provider 匹配
- SW span happy path
- provider 缺失、model 缺失、unknown model、invalid token、error span

本轮新增测试覆盖：
- Zipkin 分支提取
- legacy `gen_ai.system` fallback
- provider 推断 fallback
- `transferToSources(...)` 的 4-source 输出
- `NamingControl` 对 provider/model 名的裁剪
- rounding 行为
- null metrics -> empty list

## 7. 外域关系
- `SW-3A`：SW trace 侧通过 `VirtualGenAIProcessor` 接入
- `SW-7`：Zipkin receiver 侧通过 `SpanForward` 接入
- `SW-4/5`：GenAI OAL 指标查询与存储不在本域展开

## 8. 当前结论
`SW-3F` 当前已达到可交接状态：
- 边界清晰
- SW / Zipkin 双入口已核实
- source 转换与 naming 语义已补测试
- 未发现需要立即修复的源码缺陷

剩余风险：
- `segment` 参数当前未使用，未来若需要按 trace 级上下文增强，要重新审计接口语义
- cost rounding 是当前设计选择，不代表最终产品语义一定最优
