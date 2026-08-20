# SW-3E Hierarchy Compiler — Pass 2 问题收敛

> 模块: `oap-server/analyzer/hierarchy`
> 日期: 2026-08-18

## Q1: grammar 声明的 `>= <=` 是否真的贯通到 visitor 与 codegen
最初没有贯通。

实证：
- grammar 有 `condGte / condLte`
- `HierarchyRuleModel.CompareOp` 与 `HierarchyRuleClassGenerator.generateComparison(...)` 也有 `GTE / LTE`
- 但 `HierarchyRuleScriptParser.ConditionVisitor` 初版缺少 `visitCondGte(...)` 与 `visitCondLte(...)`

后果：
- parser 产出的 if condition 为 `null`
- codegen 生成 `if ()`，Javassist 编译时报语法错误

结论：这是本轮确认并修复的真实缺陷。

## Q2: `&& || !` 是否完整可用
当前源码表面支持，且 `ConditionVisitor` 已实现：
- `visitCondAnd`
- `visitCondOr`
- `visitCondNot`

本轮新增测试确认至少以下组合可用：
- `u.name != l.name && true`

但要注意：
- 对 `!false && expr` 这类混合优先级组合，若没有精确断言 AST 形状，容易误判 visitor 行为
- 当前官方规则并未依赖复杂逻辑组合

结论：逻辑运算已具备基本能力，但尚未达到像 MAL/LAL 那样的大规模 DSL 覆盖度。

## Q3: `else if / else` grammar 已声明，是否有真实规则和测试覆盖
当前真实 YAML 规则没有使用 `else if`，现有测试也未覆盖。

结论：
- 该分支目前更多是语法预留能力
- 后续若 hierarchy 规则开始使用 `else if`，需要补专门测试验证 flatten 逻辑

## Q4: 运行时空值/NPE 是否有保护
没有额外保护。

`HierarchyDefinitionService.MatchingRule.match(...)` 直接执行生成后的 `matcher.apply(upper, lower)`；若规则表达式里对空 `shortName` 做字符串方法调用，异常会向上传播给 `HierarchyService`。

结论：
- 编译器当前不做空值防护包装
- 运行时空值鲁棒性取决于规则表达式本身

## Q5: provider / runtime 的 class name、source file、debug 输出契约
已确认：
- provider 使用 SPI 首个实现
- class name 由 `yamlSource + lineNo + classNameHint` 组合生成，必要时去重
- `SourceFile` 会写入形如 `(hierarchy-definition.yml:88)rule.java`
- `SW_DYNAMIC_CLASS_ENGINE_DEBUG` 打开时输出 `.class` 到 `hierarchy-rt/`

结论：调试锚点和类命名契约比较清晰，便于后续收敛与定位。
