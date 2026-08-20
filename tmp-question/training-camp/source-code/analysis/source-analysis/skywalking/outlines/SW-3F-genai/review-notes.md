# SW-3F GenAI Analyzer — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 边界与调用链复核
确认：
- `SW-3F` 不是配置中心或 query 域
- 真实逻辑中心是 `GenAIMeterAnalyzer`
- SW trace 和 Zipkin 都会进入该 analyzer
- `GenAIConfigLoader` / `GenAIProviderPrefixMatcher` 都是薄包装，不是复杂核心算法

## Review 2 — 负面空间质疑
重点质疑：
1. Zipkin 分支是否缺测试
2. `transferToSources(...)` 是否固定输出 4 个 source
3. provider fallback 是否兼容 legacy `gen_ai.system`
4. `NamingControl` 是否改变虚拟实体名
5. cost rounding 是否会造成隐藏回归

结果：
- 这些点都可由新增单测直接覆盖
- 暂未暴露真实源码 bug

## Review 3 — 测试交叉与收敛
新增：
- `GenAIMeterAnalyzerAdvancedTest`

验证命令：
```bash
./mvnw -pl oap-server/analyzer/gen-ai-analyzer -am -Dtest=GenAIMeterAnalyzerAdvancedTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/analyzer/gen-ai-analyzer -am test
```

结果：通过。

## 最终判断
- 本域当前未发现源码 defect
- 先前薄弱的 Zipkin/source/naming 区域已补回归
- 文档、源码、测试已一致

## 剩余风险
- `segment` 参数当前未使用，只能视为接口预留
- cost rounding 为设计语义，不是缺陷；若产品要求展示更精细货币值，需要跨域调整模型与查询
