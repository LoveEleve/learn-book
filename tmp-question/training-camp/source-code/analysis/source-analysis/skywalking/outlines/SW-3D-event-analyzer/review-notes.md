# SW-3D Event Analyzer — Review Notes

> 日期: 2026-08-18
> 轮次: 3 轮

## Review 1 — 边界复核
核对了：
- module/provider/service 注册链
- receiver 到 analyzer 的入口位置
- listener factory 的唯一来源
- `RecordStreamProcessor` 仅为 build 出口，不在本域展开

结论：Pass 0 的“单 listener 薄链路”判断成立，没有隐藏 fan-out。

## Review 2 — 语义质疑
重点质疑：
1. 双无效时间是否会被补成零跨度 event
2. 单边有效时间是否被 analyzer 擅自修正
3. `startTime > endTime` 是否有隐藏纠偏
4. source/parameters/message/type/layer 的空值和默认值语义

结果：
- 只有“双无效时间”会被 service 层回填
- 其余异常/边界值基本透传到 listener 映射逻辑
- 未发现隐藏清洗或顺序修正

## Review 3 — 测试与实现交叉
新增测试后执行定向 reactor：
```bash
./mvnw -pl oap-server/analyzer/event-analyzer -am -Dtest=EventAnalyzerServiceImplTest,EventRecordAnalyzerListenerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：**6/6 PASS**

过程中唯一暴露的问题是测试初版误把 protobuf `Type` 当成 `Event.Type` 内部枚举；修正为顶层 `org.apache.skywalking.apm.network.event.v3.Type` 后通过。这是测试编写错误，不是源码缺陷。

## 最终判断
- 当前域无已知源码 bug
- 本轮新增测试已覆盖最关键边界
- 交接风险主要在跨域理解，而不是本域内部复杂度

## 剩余风险
- `RecordStreamProcessor` 单例及 TTL/DAO 行为未在本域验证
- receiver 层对 layer 的协议约束只做了源码交叉，未在本域编写联动测试
