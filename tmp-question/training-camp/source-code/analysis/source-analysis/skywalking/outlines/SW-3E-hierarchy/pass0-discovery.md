# SW-3E Hierarchy Compiler — Pass 0 发现

> 模块: `oap-server/analyzer/hierarchy`
> 生产 Java: **5**（主源码）
> 语法文件: **2** ANTLR4 grammar
> 测试 Java: **4**（本轮后）
> 日期: 2026-08-18

## 1. 域职责
- 从 `hierarchy-definition.yml` 的 `auto-matching-rules` 读取层级匹配规则表达式
- 用 `ANTLR4` 把表达式解析成 `HierarchyRuleModel` AST
- 用 `Javassist` 生成 `BiFunction<Service, Service, Boolean>` 实现类
- 通过 `HierarchyDefinitionService.HierarchyRuleProvider` SPI 提供给 `server-core`

这不是 hierarchy 运行时匹配本身，而是 **规则编译器**。

## 2. 主链
`HierarchyDefinitionService.loadProvider()`
→ `CompiledHierarchyRuleProvider.buildRules(...)`
→ `HierarchyRuleClassGenerator.compile(ruleName, expression)`
→ `HierarchyRuleScriptParser.parse(expression)`
→ `HierarchyRuleModel`
→ `generateApplyMethod(...)`
→ `CtClass.toClass(HierarchyRulePackageHolder.class)`
→ 返回 `BiFunction<Service, Service, Boolean>`

## 3. 输入语言边界
grammar 已声明支持：
- `== != > < >= <=`
- `&& || !`
- `if / else / return`
- 字符串、数字、布尔字面量
- method chain / field access
- `+ -`

目标对象固定是 `org.apache.skywalking.oap.server.core.query.type.Service`，字段访问本质上映射到 getter：
- `u.name -> u.getName()`
- `l.shortName -> l.getShortName()`

## 4. 当前规则现实
测试脚本 `test-hierarchy-definition.yml` 当前只用到 4 条规则：
- `name`
- `short-name`
- `lower-short-name-remove-ns`
- `lower-short-name-with-fqdn`

因此已有官方测试主要覆盖：
- 简单相等比较
- method chain
- substring / lastIndexOf / concat
- block body + return false fallback

## 5. 首轮质疑点
- Q1: grammar 声明的 `>= <=` 是否真的贯通到 visitor 与 codegen
- Q2: `&& || !` 的条件组合是否有完整 AST visitor 与执行覆盖
- Q3: `else if / else` grammar 已声明，是否有真实规则和测试覆盖
- Q4: 运行时空值/NPE 是否由编译器保护，还是直接透传到 `HierarchyService`
- Q5: provider / runtime 对 rule line、class name、debug 输出的契约是否稳定

## 6. 已发现的真实问题
首轮深审已证实：
- grammar 支持 `>= <=`，但 `HierarchyRuleScriptParser.ConditionVisitor` 最初缺少 `visitCondGte` / `visitCondLte`
- 结果是 AST condition 变成 `null`，进一步在 codegen 阶段生成非法 `if ()` 代码

该问题已在本轮源码中修复，并通过新增测试回归。
