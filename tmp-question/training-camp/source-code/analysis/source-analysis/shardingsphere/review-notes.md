# ShardingSphere 域规划 — 审查记录

## 本轮方法论阶段
- 仅做域规划审查与闭环验证
- 未写正文
- 目标：确认 6 域数量、边界、顺序、术语、遗漏

## 发现与修正

1. **SS-1 术语漂移**
   - 执行计划里的 `ShardingRouter` 已不适合作为现行主战场名称
   - 修正为 `ShardingStandardRoutingEngine` + route engine/validator 家族

2. **SS-2 需双层结构**
   - 仅写 `ShardingSQLRewriteContextDecorator` 不够
   - 必须同时覆盖 `infra/rewrite` 抽象层

3. **SS-3 原计划过窄**
   - 归并至少包含 group-by stream/memory、order-by、pagination、show/ddl 结果族
   - 修正为 `infra/merge` 抽象 + sharding feature 具体 merged result 家族

4. **SS-4 的 LEAF 不成立**
   - 当前主战场只证实 `Snowflake` 与 `UUID` 两个内置实现
   - `LEAF` 仅见测试 fixture 残留，不进入主线

5. **SS-5 需要 router + filter**
   - 不能只写 `ReadwriteSplittingDataSourceRouter`
   - 必须纳入 standard/qualified 子路由和 `ReadDataSourcesFilter`

6. **SS-6 应聚焦 Encrypt 主线**
   - `encrypt` 与 `mask` 是两个 feature
   - 若维持 6 域上限，主线应写 encrypt，mask 作为旁枝边界说明

## 当前状态
- 已完成：`pass1-notes.md`
- 已完成：`pass2-q1.md` ~ `pass2-q6.md`
- 已完成：`completeness-questions.md`
- 已完成：`outline.md`
- 已完成：`domain-plan-review.md`
- 未进入正文写作

## 第二轮收束结论
- 6 域数量保留
- 6 域边界已全部与真实源码主战场对齐
- 依赖顺序已稳定为：`SS-1 → SS-2 → SS-3`，其后并列插件域 `SS-4/SS-5/SS-6`
- 当前已从“粗糙候选计划”提升到“规划阶段收束完成”，不再需要继续拆域或改标题

## 审查结论
- 下一步若继续，应在用户明确允许后进入正文写作；在此之前不再继续扩张规划范围
