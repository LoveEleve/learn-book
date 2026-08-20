# SW-4B MQE grammar/runtime — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 结构复核
确认：
- `SW-4B` 是 grammar + visitor + operation 解释执行链
- 核心不在代码生成，而在 `ExpressionResult` 形态组合
- `LROp` 是多个 operation 的共享底层骨架，应优先怀疑

## Review 2 — 边界测试质疑
重点质疑 many↔one / one↔many 时 single-side empty 的传播。

新增 `LROpEdgeCaseTest` 后，直接打出失败：
- empty scalar + many 在 `BinaryOp` 路径下错误返回非空结果
- empty scalar + many 在 `CompareOp` 路径下同样错误返回非空结果

结论：这是本轮最核心的真实缺陷。

## Review 3 — 修复与回归
修复：
- `LROp.many2OneBinaryOp(...)`
- `LROp.one2ManyBinaryOp(...)`

改为：
- many-side 或 single-side 任一 empty -> 传播 empty

验证：
```bash
./mvnw -pl oap-server/mqe-rt -am -Dtest=LROpEdgeCaseTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/mqe-rt -am test
```

结果：通过，完整 `mqe-rt` 回归 **28/28 PASS**。

## 最终判断
- 当前已无已知 defect
- 新增测试已锁住这类空值传播回归
- 文档、源码、测试三者一致

## 剩余风险
- baseline / relabels / aggregateLabels 的更极端语义组合仍可继续探测
- consumer 层如何消费 MQE 错误信息，留待 `SW-4C` 交叉引用
