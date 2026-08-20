# SW-4C3C LogQL compatibility — Review Notes

> 模块: `server-query-plugin/logql-plugin`
> 日期: 2026-08-18
> Review 状态: 已完成 4 轮；未发现新的已确认生产缺陷，保留明确的测试与协议风险

## Review 1：代码—测试一致性

### 检查范围
- `LogQLApiHandler.labels(...)`
- `LogQLApiHandler.labelValues(...)`
- `LogQLApiHandler.rangeQuery(...)`
- `LogQLApiHandlerTest`
- `LogQLExprVisitorTest`
- `TagAutoCompleteQueryService`

### 结果
- `labels` 和 `labelValues` 的默认时间计算与生产代码一致：当前时间回看 24 小时。
- 测试 stub/verify 使用 `eq(TagType.LOG)`、`eq("service")` 和 `any(Duration.class)`，符合 Mockito matcher 规则。
- `query_range` 仍直接使用 `start/end/direction`，测试没有假设其默认值。
- 文档没有把 query_range 的完整行为误写成已覆盖。

### 结论
通过。测试对已修复的 metadata endpoint seam 有效；query_range 缺口已记录。

## Review 2：协议—边界一致性

### 检查问题
- Loki 纳秒时间参数
- OAP 毫秒时间模型
- 默认时间窗口
- direction/order 映射
- parser error 响应
- errorReason 响应

### 结果
- metadata endpoint 的纳秒→毫秒转换路径已读源码确认。
- query_range 的输出毫秒→纳秒转换已读源码确认，但尚无端到端 handler 测试。
- `FORWARD`/`BACKWARD` 的枚举映射已在 `LogDirection` 中确认。
- parser error 在 parser 层被转成 HTTP 400。
- `LogQueryService` 的 `errorReason` 非空时被转成 HTTP 400。
- `query_range` 缺省 start/end/direction 的协议约束尚未从外部 contract 验证，因此列为未决风险，不做推断。

### 结论
通过。已区分“源码已确认”和“行为尚未由测试验证”。

## Review 3：构建—文档—工作树一致性

### 构建
执行：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am \
  -Dtest=LogQLApiHandlerTest,LogQLExprVisitorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- reactor `BUILD SUCCESS`

完整 reactor 命令：

```bash
./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test
```

结果：
- `server-core`: 216/216 PASS
- `LogQLApiHandlerTest`: 4/4 PASS
- `LogQLExprVisitorTest`: 4/4 PASS
- LogQL reactor: `BUILD SUCCESS`
- 构建输出含既有 Netty Brotli native library warning 和异步 MAL 测试线程日志，但没有转化为 Surefire failure，Maven 最终成功。

### 文档
- `pass0-discovery.md` 记录首轮边界和质疑点。
- `pass2-questions.md` 记录真实缺陷、测试修复、协议前提风险和回归结果。
- `outline.md` 记录主链、能力边界、修复、测试和剩余风险。
- 本文件记录三轮 review 结论。

### 工作树
- 保留仓库已有用户修改。
- 不使用 destructive git 操作。
- 新增 LogQL 测试文件仍是工作树未跟踪文件，提交或交接前应通过 `git status` 确认。

### 结论
文档与当前代码、测试结果一致。完整 reactor 回归已经成功，LogQL 构建状态可记录为最终通过；query_range 的协议与行为覆盖仍按剩余风险保留。

## Review 4：最终事实与交接一致性

### 检查范围
- `pass0-discovery.md`
- `pass2-questions.md`
- `outline.md`
- `review-notes.md`
- `HANDOFF-SKYWALKING-STAGE2.md`
- LogQL handler/test 当前源码
- Maven 定向与完整 reactor 输出

### 结果
- 四份 LogQL 文档均存在，文件名和绝对目录一致。
- 测试计数一致：原有 visitor 4 个，本轮 handler 4 个，LogQL 模块总计 8 个。
- 完整 reactor 结果：server-core 216/216、LogQLApiHandlerTest 4/4、LogQLExprVisitorTest 4/4，LogQL 8/8，BUILD SUCCESS。
- 总交接文档已删除过时的 matcher 失败状态，并保留 query_range 的真实剩余风险。
- 未发现旧失败结论、虚假的“全部收敛”声明或路径冲突。

### 结论
通过。当前文档可以交给下一个 AI；下一个 AI 应从 `SW-4C4` 或 query_range 专项测试中选择下一步，不应重复修复已通过的 matcher 问题。

## 已确认的问题清单

### 已修复
- labels 缺省 start/end 导致 NPE
- label-values 缺省 start/end 导致 NPE
- LogQL handler 测试 raw matcher 混用导致测试自身失败

### 未修复但已明确记录
- query_range 缺省 start/end/direction 的协议行为
- query_range 完整 handler 测试空洞
- limit null 行为
- 纳秒转换边界/溢出
- stream key 拼接碰撞和 null label 输出

## 收敛标准

SW-4C3C 才能标记“完整收敛”前，至少需要：

1. 完整 `./mvnw -pl oap-server/server-query-plugin/logql-plugin -am test` 成功。
2. 确认 Loki/SkyWalking 对 query_range 参数缺省行为的协议约定。
3. 为 query_range 增加 mock handler 测试，至少覆盖 parser error、service error、empty result、direction、时间转换和 tags/keywords 委托。
4. 对新增测试和三份文档再做一次事实、路径、行号、命令结果核对。
