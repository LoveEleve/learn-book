# MT-5 Exporter 抽象与过滤 — outline 收敛版

> 核心文件: `FinishedSpan` / `SpanReporter` / `SpanFilter` / `SpanExportingPredicate` / `SpanIgnoringSpanExportingPredicate` / `TestSpanReporter`
> harness: `MiniMT5` **12/12 PASS** | 日期: 2026-08-17

## 一、FinishedSpan
- finished span 统一承载:
  - name / start / end / duration
  - string tags / typed tags
  - events
  - trace/span/parent IDs
  - local/remote address/service
  - error / kind / links
- `getDuration()` = `Duration.between(start,end)`
- `setTypedTags(...)`：List 值逗号连接，其余 `String.valueOf`
- `getTypedTags()` 默认把 string tags 包成 Object map
- local IP/service、links 等部分能力有 default no-op，允许不同 bridge 能力不完全一致

## 二、出口接口
- `SpanReporter.report(FinishedSpan)`：唯一核心导出入口
- `SpanReporter.close()`：默认 no-op，具体 reporter 可释放资源
- `SpanFilter.map(FinishedSpan)`：完成后 span 的 mutation hook
- `SpanExportingPredicate.isExportable(FinishedSpan)`：最终导出判定
- 约定顺序：**filter map → exporting predicate → reporter**

## 三、名称过滤
- `SpanIgnoringSpanExportingPredicate` 合并主 skip list + additional skip list
- regex 通过静态 `ConcurrentHashMap<String, Pattern>` 缓存
- 使用 `Pattern.matcher(name).matches()`：**全字符串匹配**
- 空 span name 默认 exportable
- 任一 pattern 命中 → 不导出

## 四、TestSpanReporter
- ConcurrentLinkedQueue 保存 finished spans
- `poll()` 取出并移除一条
- `spans()` 返回 queue 副本
- `close()` 清空 queue

## 五、验证
- 官方 `SpanIgnoringSpanExportingPredicateTests`：主列表、additional 列表、regex cache
- harness `MiniMT5`：
  - duration
  - typed tags（数字/布尔/List/null）
  - full regex vs partial regex
  - empty name
  - reporter report/poll/copy/close

## 六、边界
- `SpanFilter` / `SpanReporter` 的组合调用方可能位于 bridge/reporter 实现，不在 core exporter 接口本身内完成
- `FinishedSpan` 的 default links/local service 并不代表所有 bridge 一定支持，需要在 MT-6/MT-7 继续验证