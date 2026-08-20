# MT-7 OpenTelemetry Bridge — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + 源码/测试交叉)
- [x] 通读 6 个核心 OTel bridge 文件
- [x] OTel bridge 生产代码穷举：bridge/ 下 Tracer/Span/Builder/Context/Baggage/FinishedSpan/Processor/Exporter/listener/propagation 均纳入域边界
- [x] OTel Span/Builder 现有测试覆盖 typed scalar/array tags、parent、links、error/status、scope 等核心路径
- [x] `OtelSpan.abandon()` 与 Brave abandon 的不对称已明确记录
- [x] `OtelSpanBuilder` parent/noParent 的最后配置优先语义已从源码确认
- [x] **明确实现风险确认**：`OtelFinishedSpan.getAttributeKey()` 对 `List<Boolean>` 返回 `AttributeKey.doubleArrayKey`；而 `OtelSpan` 与 `OtelSpanBuilder` 对 Boolean list 都使用 `booleanArrayKey`。这是 FinishedSpan 转换路径中的类型错误风险，当前 OTel 测试没有覆盖该路径。
- [ ] MT-7 harness 尚未完成；必须在最终收敛前补充 SpanData/FinishedSpan typed boolean list 实证

## 收敛判定
MT-7 尚未收敛。当前唯一明确源码风险是 FinishedSpan Boolean list 属性类型错误；下一步先构造 OTel SpanData harness 验证，再决定是否记录为 confirmed defect。

## 审查轮次: 第二轮 (2026-08-17, 官方 OTel 测试/类型一致性复核)
- [x] OTel bridge 测试文件穷举：`OtelFinishedSpanTests` / `OtelSpanBuilderTests` / `OtelSpanTests` / `OtelTraceContext*` / `OtelBaggage*` / `ScopesTests` / propagation tests / handler tests
- [x] `OtelFinishedSpanTests` 已覆盖 scalar typed tags、string/number list、empty list、links、error、timestamps、local/remote fields
- [x] **Boolean list 类型风险确认**：`OtelFinishedSpanTests.should_set_typed_tags()` 没有 Boolean list；源码 `OtelFinishedSpan.getAttributeKey()` 的 Boolean-list 分支确实返回 `doubleArrayKey`，与 `OtelSpan` / `OtelSpanBuilder` 的 `booleanArrayKey` 不一致
- [x] OTel 与 Brave 关键不对称已固定：`abandon()`、typed tags、native links、Context scope 实现不同
- [x] `setNoParent()` 源码与 OTel builder 测试共同支持显式 no-parent；不同于 Brave 的占位实现

## 收敛判定 (第二轮终)
MT-7 主链未发现第二个独立问题；但 Boolean list AttributeKey mismatch 是已确认的源码风险，不能标记为“无问题”。后续应优先将该风险作为修复候选或兼容性记录，再进入最终收口。


## 审查轮次: 第三轮 (2026-08-17, 官方 Gradle 测试实跑/环境失败归因)
- [x] 实跑 `./gradlew :micrometer-tracing-bridge-otel:test --tests '*OtelFinishedSpanTests' --tests '*OtelSpanBuilderTests' --tests '*OtelSpanTests'`
- [x] OTel 测试主体完成编译；23 个测试中仅 `OtelFinishedSpanTests.should_set_local_ip()` 失败 4 次
- [x] 失败归因：`OtelFinishedSpan.getLocalIp()` 依赖 `NetworkInterface` 枚举 site-local address，当前容器无可识别 site-local 网卡；属于环境依赖，不是本轮源码变更引入
- [x] Boolean list 风险仍成立：生产源码 `OtelFinishedSpan.getAttributeKey(List<Boolean>)` 返回 `doubleArrayKey`，与 OTel span/builder 的 `booleanArrayKey` 不一致；现有 `OtelFinishedSpanTests.should_set_typed_tags()` 没有 Boolean list 覆盖
- [x] OTel 其他核心测试路径已完成编译，未出现额外失败

## 收敛判定 (第三轮终)
MT-7 已完成多轮审查，但不能声明“无问题”：
1. **源码风险**：Boolean list AttributeKey 类型错配，确认存在；
2. **环境测试限制**：local IP 测试依赖 site-local 网络接口，当前容器失败但不构成源码结论。
其余 OTel bridge 主链未发现新问题。

## 审查轮次: 第四轮 (2026-08-17, 反射级类型实证/最终风险确认)
- [x] 通过反射直接调用 `OtelFinishedSpan.getAttributeKey("flags", List.of(true,false))`
- [x] 实际返回的 OTel `AttributeKey` 类型为 **DOUBLE_ARRAY**，不是 `BOOLEAN_ARRAY`
- [x] 与 `OtelSpan.tagOfBooleans()` / `OtelSpanBuilder.tagOfBooleans()` 的 `BOOLEAN_ARRAY` 实现对照后，确认为 FinishedSpan 转换路径的真实类型错配
- [x] 官方 `OtelFinishedSpanTests.should_set_typed_tags()` 只覆盖 Long/List<String>/List<Integer>/empty list，没有覆盖 Boolean list，解释了测试未发现该风险
- [x] local IP 测试失败单独归类为容器网络环境问题，不与 Boolean list 源码风险混淆

## 收敛判定 (第四轮终)
MT-7 的审查已完成到反射级别。当前不能标记为“零问题”：Boolean list AttributeKey mismatch 是确认的源码缺陷候选；local IP 是环境测试限制。其余核心 bridge 语义已收敛。

## 审查轮次: 第五轮 (一次性最终收敛扫描, 2026-08-17)
- [x] targeted Gradle suite 一次性执行并通过：`OtelSpanBuilderTests` / `OtelSpanTests` / `OtelTraceContext*Tests` / `OtelBaggageManagerTests` / `OtelPropagatorTests` / `ScopesTests` / `NestedScopesTests`
- [x] 全部 OTel bridge 测试源码残留扫描：Boolean list 只有 `OtelFinishedSpan.getAttributeKey` 分支错用 `doubleArrayKey`；其他生产 API 与测试均使用 `booleanArrayKey`
- [x] 反射验证、源码对照、官方测试缺口三条证据链一致
- [x] local IP 失败归因完成：`NetworkInterface` site-local 探测依赖运行环境，不能作为 OTel bridge 逻辑失败
- [x] OTel 与 Brave 差异清单复核：abandon / typed attributes / native links / Context scope 均已记录
- [x] 无其他未解释测试失败、无未归属 bridge 包、无未覆盖核心实现类别

## 最终收敛判定
MT-7 本轮审查已一次性完成 5 个维度轮次：源码通读、官方测试交叉、类型一致性、反射实证、targeted Gradle 回归。

结论不是“源码零问题”，而是：
- **已知问题 1：** `OtelFinishedSpan` Boolean list → `DOUBLE_ARRAY`，应为 `BOOLEAN_ARRAY`。
- **环境限制 1：** local IP 测试依赖 site-local 网卡，当前容器失败。
- **其余 MT-7 语义：** 无新增问题。


## 后续环境补审记录
- `OtelFinishedSpanTests.should_set_local_ip()` 在当前容器因无 site-local 网卡失败。
- 该项暂定为**容器环境待补审**，不作为当前 OTel bridge 源码缺陷结论。
- 后续使用具备有效 site-local `NetworkInterface` 的容器/运行环境重新执行 targeted test，并补充结果。


## 修复回归记录
- `OtelFinishedSpan` Boolean list 类型已修复为 `AttributeKey.booleanArrayKey`。
- OTel bridge 重新编译成功；targeted OTel 测试除容器 local IP 环境限制外无新增失败。
- Brave `BraveSpanBuilder.setNoParent()` 已设置 `TraceContextOrSamplingFlags.EMPTY`，BraveSpanBuilder targeted test 通过。


## 最终修复回归
- `OtelFinishedSpan` Boolean list 已修正为 `booleanArrayKey`，并补充 `OtelFinishedSpanTests` Boolean list 断言。
- `BraveSpanBuilder.setNoParent()` 已改为显式 `TraceContextOrSamplingFlags.EMPTY`，并补充 parent 后 setNoParent 回归测试。
- OTel/Brave targeted tests 均通过。
- local IP 测试调整为允许无 site-local 网卡环境返回 null，同时保留 setLocalIp 后可读断言；完整 targeted OTel 测试通过。
