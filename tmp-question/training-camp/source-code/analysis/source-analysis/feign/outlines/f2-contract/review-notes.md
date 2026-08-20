# F-2 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 + 极简复现 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "入口 parseAndValidateMetadata(Class)" — Contract.java:40 (无拼写变体) | 通过 ✅ |
| 2 | 事实 | §1 "过滤 Object/static/default/@FeignIgnore" — L60-65 | 通过 ✅ |
| 3 | 事实 | §1 "configKey 协变合并" — L67-77, Types.resolveReturnType L71 | 通过 ✅ |
| 4 | 事实 | §2 "参数循环 + 未消费推断 body" — L117-159 (L138-158); body+form 冲突 L142-149 | 通过 ✅ |
| 5 | 事实 | §2 "isAlreadyProcessed" — MethodMetadata.java:212-222 | 通过 ✅ |
| 6 | 事实 | §3 "parseAndValidateMetadata final 化" — DeclarativeContract.java:38-41 | 通过 ✅ |
| 7 | 事实 | §3 "未识别注解 warning" — L68-82/L102-107/L141-155; errorMessageOnMixedContracts 测试 (DefaultContractTest.java:865) | 通过 ✅ |
| 8 | 事实 | §3 "参数处理固定返回 false" — L157 (13.x 与旧版差异) | 通过 ✅ |
| 9 | 事实 | §4 "REQUEST_LINE_PATTERN ^([A-Z]+)[ ]*(.*)$" — DefaultContract.java:32 | 通过 ✅ |
| 10 | 事实 | §4 "@Body 含 { → bodyTemplate" — L78-82 | 通过 ✅ |
| 11 | 事实 | §5 "@Param 名回退 -parameters" — L100-104; 空名 Hint 报错 L105-111 (DefaultContractTest.java:918-931 实证) | 通过 ✅ |
| 12 | 事实 | §5 "formParams 缺席推断" — L117-119 (issue 424 子串测试 L856-863) | 通过 ✅ |
| 13 | 事实 | §6 "CompletableFuture 剥离" — MethodInfo.java:36-39; 消费 AsynchronousMethodHandler.java:319 | 通过 ✅ |
| 14 | 事实 | §6 "Kotlin 协程特化" — KotlinMethodInfo.java:26 | 通过 ✅ |
| 15 | 事实 | §7 "Types.resolve 消解" — Types.java:207-277; 移植自 Retrofit | 通过 ✅ |
| 16 | 数字 | 锚点密度: F-2 outline .java:NNN ≥8 | 通过 ✅ (统计见 REVIEW) |
| 17 | 结构 | 负面空间 "不做契约缓存" 与 ReflectiveFeign 每次解析自洽 | 通过 ✅ |

**结论**: 17 项核对 0 修正。harness 验证参数角色分配/body 推断/formParams。

## harness 设计 (MiniContract — 契约解析极简复现)

- A. 参数角色分配: 注解标记 → URI → body 剩余法
- B. body 推断: 未消费参数 → body; 多 body 报错
- C. formParams 缺席推断: 模板里没有的 @Param → form
- D. @Body 模板 vs 字面量: 含 { 走模板
- E. configKey 生成: 类名#方法(参数类型)
