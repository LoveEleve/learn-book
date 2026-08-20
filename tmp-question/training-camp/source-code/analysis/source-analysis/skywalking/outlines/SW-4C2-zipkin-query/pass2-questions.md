# SW-4C2 Zipkin query compatibility — Pass 2 问题收敛

> 模块: `oap-server/server-query-plugin/zipkin-query-plugin`
> 日期: 2026-08-18

## Q1: 空/blank traceId 是否会进入 DAO
初版存在真实缺陷：
- `getTraceById(...)` 使用 `StringUtil.isEmpty(traceId)`
- 对空白字符串（如 `"   "`）判定为非空
- 随后继续调用 `ZipkinQueryService.getTraceById(...)`
- 最终可能在 DAO 获取/查询阶段报错，而不是返回协议层 400

修复：
- 改为 `StringUtil.isBlank(traceId)`
- 空字符串与纯空白字符串统一返回 400

## Q2: traceMany 的重复 ID 语义
已核实：
- 空串 -> 400
- 逗号拆分上限 1000
- 每个 ID trim 后先 `Span.normalizeTraceId(...)`
- 使用 `LinkedHashSet` 去重
- 发现重复 -> 400
- 查询无结果 -> 404

## Q3: config.json 是否包含关键 UI/query 配置
已测试确认返回：
- environment
- queryLimit
- defaultLookback
- searchEnabled
- dependency.enabled=false

## Q4: names response 是否排序/cache
`cachedResponse(...)` 会：
- 原地 `Collections.sort(values)`
- 返回 JSON array
- 当 shouldCache=true 时添加 `Cache-Control: max-age=<namesMaxAge>, must-revalidate`

结论：当前兼容层对 names 类 API 采用稳定排序和条件缓存。

## Q5: v1/v2 DAO 分支
`ZipkinQueryService.getZipkinQueryDAO()` 首次获取 DAO 时判断是否实现 `IZipkinQueryV2DAO`，之后：
- v2：读取 `SpanWrapper` 并转换
- v1：读取旧 `Span`

两条路径随后都进入 Zipkin span 响应编码与 event 追加语义。

## Q6: 当前是否还有已确认源码问题
本轮除 blank traceId 校验外，未发现新的可复现生产代码缺陷。

新增 handler 测试已覆盖：
- config response
- blank traceId
- empty traceIds
- duplicate traceIds
