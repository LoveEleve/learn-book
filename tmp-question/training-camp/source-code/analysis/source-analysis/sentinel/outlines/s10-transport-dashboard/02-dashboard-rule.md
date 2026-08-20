# Dashboard 的规则链路

> S-10 中篇。本文讲 Dashboard 自身如何读取规则、保存规则，再通过 transport command API 推回机器。

## 悬念

Dashboard 不是直接改客户端内存里的规则。它先把规则视为一种“控制面数据”，在 controller、repository、provider、publisher 四层之间流转。为什么要这样分层？

## 一、controller 负责输入校验与用例编排

以 `FlowControllerV2` 为例：

- `GET /v2/flow/rules?app=xxx`：查规则
- `POST /v2/flow/rule`：新增规则
- `PUT /v2/flow/rule/{id}`：修改规则
- `DELETE /v2/flow/rule/{id}`：删除规则

controller 自身不实现规则同步，它主要做三件事：

1. 校验请求体（resource/limitApp/grade/controlBehavior 等）
2. 调 repository 保存或删除规则副本
3. 调 `publishRules(app)` 触发推送

例如新增规则：

```java
entity = repository.save(entity);
publishRules(entity.getApp());
```

## 二、repository 是 Dashboard 的本地规则副本

`InMemoryRuleRepositoryAdapter<FlowRuleEntity>` 是 dashboard 侧的本地规则仓库。它的角色不是配置中心，而是：

- 保存从机器拉回来的规则快照
- 作为 controller 的修改目标
- 在推送前提供“该 app 当前的完整规则集合”

所以 repository 是 Dashboard 的本地视图层。即使远端机器上才是真正生效规则，Dashboard 也需要一个本地副本来支持编辑、查询和页面展示。

## 三、provider:从机器拉规则

`FlowRuleApiProvider` 实现 `DynamicRuleProvider<List<FlowRuleEntity>>`：

1. 从 `AppManagement` 取该应用的所有机器
2. 过滤健康机器，按最近心跳倒序排序
3. 选“最近心跳最新的健康机器”
4. 调 `sentinelApiClient.fetchFlowRuleOfMachine(app, ip, port)` 拉规则

```java
MachineInfo machine = list.get(0);
return sentinelApiClient.fetchFlowRuleOfMachine(machine.getApp(), machine.getIp(), machine.getPort());
```

这里的默认语义不是“从配置中心读规则”，而是“从一台健康机器上拉当前规则作为该 app 的代表视图”。这也解释了为什么 controller 查询规则前会先调 provider，再把结果 `repository.saveAll(rules)`。

## 四、publisher:把规则推回所有健康机器

`FlowRuleApiPublisher` 实现 `DynamicRulePublisher<List<FlowRuleEntity>>`，它会：

1. 从 `AppManagement` 取该 app 的所有机器
2. 遍历健康机器
3. 对每台机器调用 `sentinelApiClient.setFlowRuleOfMachine(app, ip, port, rules)`

```java
for (MachineInfo machine : set) {
    if (!machine.isHealthy()) {
        continue;
    }
    sentinelApiClient.setFlowRuleOfMachine(app, machine.getIp(), machine.getPort(), rules);
}
```

所以 Dashboard 的规则推送是 fan-out：一套规则写给该应用下所有健康机器。

## 五、provider/publisher 的边界

provider 和 publisher 都是接口：

```java
public interface DynamicRuleProvider<T> {
    T getRules(String appName) throws Exception;
}

public interface DynamicRulePublisher<T> {
    void publish(String app, T rules) throws Exception;
}
```

这两个接口把 Dashboard 的“规则来源”和“规则落点”抽象掉了。默认实现是 `SentinelApiClient` 直连机器，但也可以替换为 Nacos/ZK/DB 等持久化规则中心。

## 六、SentinelApiClient:Dashboard 到机器的 command API 客户端

Dashboard 和 client 之间真正通信的不是 repository，而是 `SentinelApiClient`。provider 用它拉规则，publisher 用它推规则。

也就是说：

- repository 只在 Dashboard 内部
- `SentinelApiClient` 才是 Dashboard 与 client transport command port 的桥

这把 Dashboard 的页面/存储逻辑和 transport 通信逻辑分开了。

## 悬念回收

Dashboard 的规则链路是：

```text
controller
  -> provider 从健康机器拉当前规则
  -> repository 保存本地副本
  -> controller 修改/删除副本
  -> publisher 从 repository 取完整规则集
  -> SentinelApiClient fan-out 推到所有健康机器
```

这套分层的目的，是把“页面编辑”“本地副本”“远程读规则”“远程推规则”四件事拆开。Dashboard 不是单纯 UI，它是一个规则同步协调者。

## 锚点

- `FlowControllerV2.java:52-72`
- `FlowControllerV2.java:95-155`
- `FlowControllerV2.java:176-179`
- `FlowRuleApiProvider.java:31-47`
- `FlowRuleApiPublisher.java:31-48`
- `DynamicRuleProvider.java:18-20`
- `DynamicRulePublisher.java:18-28`
