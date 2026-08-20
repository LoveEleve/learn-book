# SW-3E Hierarchy Compiler — Outline

> 模块: `oap-server/analyzer/hierarchy`
> 日期: 2026-08-18

## 1. 域定位
`SW-3E` 不是 hierarchy relation 的运行时服务，而是一个小型 DSL 编译器：
- 输入：`hierarchy-definition.yml` 中的 rule expression
- 中间态：`HierarchyRuleModel` AST
- 输出：`BiFunction<Service, Service, Boolean>` 动态类

它为 `server-core` 的 `HierarchyDefinitionService` 提供“规则表达式 -> 可执行 matcher”的编译能力。

## 2. 链路分层
- `HierarchyDefinitionService`：读取 YAML、发现 SPI provider、组装层级规则
- `CompiledHierarchyRuleProvider`：把多个 rule expression 批量编译成 `BiFunction`
- `HierarchyRuleScriptParser`：ANTLR4 解析为 AST
- `HierarchyRuleClassGenerator`：AST -> Java method body -> Javassist class
- `HierarchyRulePackageHolder`：JDK 16+ 动态加载锚点

## 3. 支持的语言特性
当前 grammar/visitor/codegen 组合支持：
- 比较：`== != > < >= <=`
- 逻辑：`&& || !`
- 控制流：`if / else / return`
- 字面量：string / number / boolean
- 算术：`+ -`
- method chain：`u.shortName.substring(...)`
- field access：`u.name`, `l.shortName`

字段访问会在 codegen 阶段转 getter：
- `name -> getName()`
- `shortName -> getShortName()`
- 其他字段按 JavaBean 规则推导

## 4. 已确认的真实缺陷与修复
本轮发现并修复：
- `HierarchyRuleParser.g4` 已声明 `condGte` / `condLte`
- `HierarchyRuleClassGenerator.generateComparison(...)` 已实现 `GTE / LTE`
- 但 `HierarchyRuleScriptParser.ConditionVisitor` 初版遗漏 `visitCondGte(...)` / `visitCondLte(...)`

影响：
- 解析 `>=` / `<=` 时生成 `null` condition
- codegen 输出非法 `if ()`
- Javassist 编译失败

修复后：
- `>=` / `<=` 已从 grammar -> AST -> codegen 全链路贯通

## 5. 运行时语义
- provider 在启动时 fail-fast 编译全部 hierarchy rules
- 编译失败会阻止对应规则构建
- 生成类实现 `apply(Object, Object)`，内部强转成 `Service`
- `MatchingRule.match(...)` 不包异常；表达式内部 NPE/字符串调用错误会直接上抛

## 6. 当前测试覆盖
原有：
- parser happy path 5 个
- class generator happy path + 基础异常 path 8 个

本轮新增：
- `HierarchyRuleScriptParserAdvancedTest`
- `HierarchyRuleClassGeneratorAdvancedTest`

新增覆盖：
- `>=`
- `<=`
- `&&`
- `!=`
- 高级条件的 AST / codegen 回归

## 7. 与外域关系
- `SW-2`：SPI/ServiceLoader 装配语义
- `SW-3E`：只负责编译 hierarchy rule
- `SW-4/5`：不涉及 query/storage 本身
- `server-core` 的 `HierarchyService` / `HierarchyQueryService` 是消费方，不并入本域

## 8. 回归命令
定向新增测试：
```bash
./mvnw -pl oap-server/analyzer/hierarchy -am -Dtest=HierarchyRuleScriptParserAdvancedTest,HierarchyRuleClassGeneratorAdvancedTest -Dsurefire.failIfNoSpecifiedTests=false test
```

完整模块回归：
```bash
./mvnw -pl oap-server/analyzer/hierarchy -am test
```

## 9. 收敛结论
`SW-3E` 已达到可交接收敛状态：
- 编译链边界清晰
- grammar / visitor / codegen 的责任已拆清
- 已确认并修复一个真实缺陷（`>= <=` visitor 缺失）
- 关键扩展分支已补回归测试

剩余前向引用：
- `HierarchyService` 的运行时匹配副作用留在消费侧域交叉引用
- `else if` 的预留语法仍缺真实业务规则覆盖
