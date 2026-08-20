# SW-4A OAL compiler/runtime — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 边界复核
确认：
- `SW-4A` 只覆盖 OAL compile/runtime
- `oal-grammar` 和 `oal-rt` 必须一起看
- 真正关键链路是 parser -> enricher -> generator，而不是单看 parser

## Review 2 — 高风险点盘点
重点检查了：
- `>= <= like in contain not contain`
- decorator
- map attribute
- nested boolean accessor
- source/function arg cast
- production scripts / runtime generation

结果：这些点都已有实现和测试交叉，暂未发现“语法声明有、实现缺”这一类明显 bug。

## Review 3 — 回归与一致性复查
执行：
```bash
./mvnw -pl oap-server/oal-rt -am test
```

结果：`oal-grammar` + `oal-rt` 回归通过，`oal-rt` **98/98 PASS**。

同时复核了：
- parser 不是通用递归表达式引擎，而是固定表达式集合
- enricher 是 parser 与 generator 的关键桥梁
- generator 会产出 metrics / builder / dispatcher 三类生成物

## 最终判断
- 当前未发现可复现源码 defect
- 文档、源码、官方测试三者一致
- 本域可视为完成第一阶段收敛

## 剩余风险
- production scripts 未覆盖到的少数极端语义组合
- FreeMarker 模板与 metadata 约束之间的隐性耦合
- 若后续继续深挖，应优先做 targeted probing，而不是无依据扩改
