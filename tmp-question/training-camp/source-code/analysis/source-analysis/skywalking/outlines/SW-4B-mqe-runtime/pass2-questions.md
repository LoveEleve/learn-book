# SW-4B MQE grammar/runtime — Pass 2 问题收敛

> 模块: `oap-server/mqe-grammar` + `mqe-rt`
> 日期: 2026-08-18

## Q1: MQE 的核心复杂度主要在哪里
主要不在 grammar，而在 runtime operation 对 `ExpressionResult` 形态的组合语义。

尤其是：
- `SINGLE_VALUE`
- `TIME_SERIES_VALUES`
- `SORTED_LIST`
- `RECORD_LIST`
- labeled / unlabeled
- `emptyValue`
- `boolResult`

## Q2: many↔one / one↔many 时 single-side empty 是否正确传播
初版不正确。

实证：
- `LROp.many2OneBinaryOp(...)` 只判断 many-side `mqeValue.isEmptyValue()`
- `LROp.one2ManyBinaryOp(...)` 也只判断 many-side `mqeValue.isEmptyValue()`
- single result 若为 empty，会继续把其 doubleValue 参与运算

后果：
- binary/compare 在 empty scalar 与非空 many 组合下，结果被错误标成非空

本轮已修复为：
- many-side 或 single-side 任一为 empty，都传播 empty

## Q3: 该缺陷影响哪些 operation
凡是通过 `LROp.doLROp(...)` 走 many↔one / one↔many 路径的 operation 都受影响，至少包括：
- `BinaryOp`
- `CompareOp`
- `BoolOp` 的同层组合路径间接受益于同一底层语义一致性

## Q4: 当前官方测试为何没发现它
原有测试覆盖了：
- 空 many result
- 不同 result type 组合
- 基本 arithmetic / compare / bool 结果

但没有直接构造“single result 本身是 empty，而 many result 非空”的场景。

本轮新增 `LROpEdgeCaseTest` 后才把问题打出来。

## Q5: 修复后如何验证无回归
已验证：
```bash
./mvnw -pl oap-server/mqe-rt -am -Dtest=LROpEdgeCaseTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/mqe-rt -am test
```

结果：通过。

## Q6: 剩余风险主要在哪
剩余风险不再是明显空值传播漏洞，而是：
- baseline / relabels / aggregateLabels 这类更上层操作的边界语义
- labeled 集合对齐规则的进一步极端输入
- grammar 能力与 query consumer 对 MQE 错误信息的耦合
