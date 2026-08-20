# SW-3E Hierarchy Compiler — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 边界复核
确认了：
- 这是规则编译器，不是 hierarchy 运行时匹配服务
- `HierarchyDefinitionService` 是消费入口，`CompiledHierarchyRuleProvider` 是编译 SPI
- 真实业务规则当前只有 4 条，远小于 grammar 声明能力

结论：需要重点质疑“语法声明能力是否真的由 visitor/codegen 落实”。

## Review 2 — 语法/visitor/codegen 一致性审计
发现：
- grammar 有 `condGte / condLte`
- model 与 codegen 也有 `GTE / LTE`
- 但 parser visitor 缺少对应实现

通过新增失败测试实锤：
- `>=` / `<=` 会生成 `null` condition
- codegen 产出 `if ()`
- Javassist 编译报错

结论：这是本轮最核心的真实源码缺陷。

## Review 3 — 修复与回归
修复：
- 补 `visitCondGte(...)`
- 补 `visitCondLte(...)`

新增回归测试：
- `HierarchyRuleScriptParserAdvancedTest`
- `HierarchyRuleClassGeneratorAdvancedTest`

验证命令：
```bash
./mvnw -pl oap-server/analyzer/hierarchy -am -Dtest=HierarchyRuleScriptParserAdvancedTest,HierarchyRuleClassGeneratorAdvancedTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/analyzer/hierarchy -am test
```

结果：通过。

## 最终判断
- `SW-3E` 当前无遗留已知 defect
- 最关键的 grammar/visitor 漏实现问题已修复
- 文档、源码、测试三者已达到一致

## 剩余风险
- `else if` 仍缺真实规则覆盖
- 运行时空值异常仍按设计直接上抛，不属于本轮修复范围
