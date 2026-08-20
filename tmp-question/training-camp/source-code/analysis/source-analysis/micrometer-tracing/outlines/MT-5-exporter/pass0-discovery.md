# MT-5 Exporter 抽象与过滤 — Pass 0 发现

> 通读: `FinishedSpan`(248) / `SpanReporter` / `SpanFilter` / `SpanExportingPredicate` / `SpanIgnoringSpanExportingPredicate` / `TestSpanReporter`
> 日期: 2026-08-17

## 1. 域职责
- 这是 span 完成后的出口层，不负责 span 创建/生命周期
- 负责把 finished span 变成可过滤、可报告、可测试的对象

## 2. FinishedSpan
- finished span 的统一数据模型:
  - name / start/end/duration
  - string tags / typed tags
  - events
  - spanId / parentId / traceId
  - local/remote ip/port/service
  - error / kind
  - links
- typed tags:
  - `setTypedTags(Map<String,Object>)` 将 List 以逗号连接，其余 `String.valueOf`
  - `getTypedTags()` 默认把现有 string tags 包成 Object map
- default capability:
  - duration = `Duration.between(start,end)`
  - localIp/localServiceName 默认 null
  - links 默认空列表，addLink/addLinks 默认 no-op
- 说明：接口允许不同 tracer 对 typed tags/links 的支持程度不同

## 3. SpanReporter
- `report(FinishedSpan)` 是唯一核心出口
- `AutoCloseable`，默认 close no-op
- reporter 应假设 span 已结束并可发送

## 4. SpanFilter
- `map(FinishedSpan)`：在 filtering/export predicate 之前修改 finished span
- 它是 mutation hook，不是 boolean predicate

## 5. SpanExportingPredicate
- `isExportable(FinishedSpan)` 决定是否导出
- 与 SpanFilter 的顺序契约：先 map，再 predicate

## 6. SpanIgnoringSpanExportingPredicate
- 合并两组 regex:
  - `spanNamePatternsToSkip`
  - `additionalSpanNamePatternsToIgnore`
- 通过静态 `ConcurrentHashMap<String, Pattern>` 缓存编译结果
- 使用 `Pattern.matcher(name).matches()`，是**全字符串匹配**而不是 contains/find
- name 为空时直接 exportable=true
- 匹配任一 regex → false

## 7. TestSpanReporter
- ConcurrentLinkedQueue 保存 finished spans
- `poll()` 取出并移除一条
- `spans()` 返回 queue 副本
- `close()` 清空已收集 spans

## 8. 待 Pass 1 验证
- Q1: FinishedSpan typed tags 对 null/List/数字/布尔值的精确转换
- Q2: duration 的负值/时间顺序异常是否原样暴露
- Q3: SpanIgnoring predicate 的 matches 全匹配语义、regex 编译失败语义、cache 复用
- Q4: SpanFilter 与 predicate 的组合顺序是否被实际调用方保证
- Q5: TestSpanReporter close/poll/spans 的并发与副本语义
- Q6: Wavefront/bridge 是否依赖 FinishedSpan 的 default links/local service 能力