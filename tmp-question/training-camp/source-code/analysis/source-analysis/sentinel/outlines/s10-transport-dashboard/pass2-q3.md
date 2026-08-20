# Pass 2 闭环笔记 Q3: Dashboard 规则查询与推送链

## 验证过程

- 以 `FlowControllerV2` 为例：
  - 读规则：controller 调 `DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider.getRules(app)`，拿到远端机器当前规则后写入本地 `InMemoryRuleRepositoryAdapter` (`FlowControllerV2.java:52-72`)。
  - 改规则：controller 校验 request body 后写 repository，再调 `publishRules(app)` (`FlowControllerV2.java:95-155`)。
- `publishRules(app)` 从 repository 取出该 app 全部规则，再交给 `DynamicRulePublisher.publish(app, rules)` (`FlowControllerV2.java:176-179`)。
- `FlowRuleApiProvider` 的默认实现：
  - 从 `AppManagement` 取该应用所有机器
  - 选健康且最近心跳最新的机器
  - 通过 `SentinelApiClient.fetchFlowRuleOfMachine` 拉规则 (`FlowRuleApiProvider.java:31-47`)。
- `FlowRuleApiPublisher` 的默认实现：
  - 遍历该 app 的所有健康机器
  - 逐台调用 `sentinelApiClient.setFlowRuleOfMachine(...)` 推规则 (`FlowRuleApiPublisher.java:31-48`)。

## 结论

Dashboard 的规则链路是：controller 接受请求 → repository 保存规则副本 → provider 从机器拉当前规则 / publisher 把规则推回机器。默认 provider/publisher 走 Sentinel command API，因此 Dashboard 既是规则编辑器，也是 client 规则同步协调者。