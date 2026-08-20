# SW-4C1 GraphQL query boundary — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 结构复核
确认：
- GraphQL 插件的关键边界在 provider / handler / resolver 三层
- 大量 resolver 是薄 façade
- 如果试图逐个 resolver 机械扫，会被数量淹没而收效很低

## Review 2 — 代表性抽样
抽样深读：
- `GraphQLQueryProvider`
- `GraphQLQueryHandler`
- `MetricsExpressionQuery`
- `MetadataQueryV2`
- `LogTestQuery`

结论：
- `MetricsExpressionQuery` 是 GraphQL 插件里少数真正带 query execution 逻辑的 resolver
- `MetadataQueryV2` 是标准 façade 模式
- `LogTestQuery` 是 debug/tooling 边界

## Review 3 — Shared seam 补测
新增：
- `AsyncQueryUtilsTest`
- `MetadataQueryV2Test`

验证：
```bash
./mvnw -pl oap-server/server-query-plugin/query-graphql-plugin -am -Dtest=AsyncQueryUtilsTest,MetadataQueryV2Test -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/server-query-plugin/query-graphql-plugin -am test
```

结果：通过。

过程中暴露的问题都来自新增测试本身：
- 未使用 import
- TTLDefinition 类型名假设错误
- Mockito 严格模式下的多余 stub

这些都已修正，不属于生产代码缺陷。

## 最终判断
- 当前未发现 GraphQL 插件生产代码缺陷
- shared seam 已有基本回归覆盖
- 文档、源码、测试已达到一致

## 剩余风险
- `MetricsExpressionQuery` 仍可作为后续 GraphQL→MQE 交叉点继续深挖
- provider 显式 schema 注册是未来装配回归的高风险点
