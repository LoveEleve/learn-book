# Pass 2 闭环笔记 Q5: FlowRule 的 grade / strategy / controlBehavior 如何组合

## 验证过程

- `grade` 决定观测量：QPS 或并发线程数 (`FlowRule.java:52-57`；`RuleConstant.FLOW_GRADE_QPS` / `FLOW_GRADE_THREAD`)。
- `strategy` 决定从哪个节点视角取数：
  - `DIRECT`：当前 origin 或当前资源 cluster node
  - `RELATE`：按 `refResource` 找另一个资源的 `ClusterNode`
  - `CHAIN`：要求 `refResource` 等于当前 context 名，然后使用当前 `DefaultNode` (`FlowRuleChecker.java:87-106, 108-136`)
- `controlBehavior` 不改变“看哪个节点”，只改变 `FlowRule` 创建的 rater/controller 行为：
  - default 直接拒绝
  - warm-up 预热
  - rate-limiter 匀速排队
  - warm-up-rate-limiter 组合模式
- `limitApp` 再决定规则是否对当前 origin 生效：指定 origin、default、other 三种匹配路径 (`FlowRuleChecker.java:108-136`)。

## 结论
一条 FlowRule 实际是四个正交选择：看什么指标(`grade`)、从哪个调用关系取节点(`strategy/refResource`)、对谁生效(`limitApp`)、超限后怎么处理(`controlBehavior`)。Checker 负责前两类匹配，rater/controller 负责最后的流控行为。