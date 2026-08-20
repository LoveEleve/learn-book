# Dubbo 完整卷重新规划

> 目标：不要把 Dubbo 误写成“注册中心 + 远程调用 + SPI”三段粗粒度框架，而是严格按《源码范围规划复盘方法论》重新审视 Apache Dubbo，把它从“一个大而杂的 Java RPC 框架”重组为一套清晰的知识域地图。  
> 分析对象：`Apache Dubbo 3.3.7-SNAPSHOT`  
> 仓库路径：`/data/workspace/source-code/code/spring/dubbo`

---

## 一、先给结论：Dubbo 不能按包结构平推，必须按运行时机制重组

Dubbo 的源码规模和组织方式，跟 grpc-java 很不一样。

grpc-java 的主干较窄：一次调用的心智图基本可以围绕 `Stub / ClientCall / ServerCall / Resolver / LB / Transport` 逐层展开，再向外补 service config、credentials、xDS、生产排障等机制层。

Dubbo 不行。

它的问题不是“模块太多”这么简单，而是：

- 核心运行时被拆散在 `dubbo-config`、`dubbo-rpc`、`dubbo-cluster`、`dubbo-registry`、`dubbo-remoting` 里
- `dubbo-common` 里藏着并不“common”的东西，例如 `ExtensionLoader`、`ScopeModel`
- `dubbo-config` 并不是“配置层”，而是 export/refer 生命周期的总入口
- `dubbo-registry` 不是纯外部接入，而是 consumer 侧动态地址、directory、override 的核心桥
- `dubbo-plugin` 体量很大，但其中大量模块是可选适配，不适合作为 baseline 主线

所以，如果按包结构写，很容易得到一种“每个模块都介绍了一点，但读者还是不知道一次 Dubbo 调用到底怎么活起来、怎么选 provider、怎么失败重试、怎么被路由和扩展改写”的伪完整感。

Dubbo 必须像 grpc-java 一样，按**机制主线**重组，而不是按目录平推。

---

## 二、为什么 Dubbo 特别容易被写散

### 1. `dubbo-config` 的命名会误导你

很多人会先把 `dubbo-config` 当成“配置对象模块”，以为它属于外围。但实际上 `DubboBootstrap`、`ServiceConfig`、`ReferenceConfig` 恰恰是 runtime 生命周期的入口。也就是说：

- provider 是从这里 export 的
- consumer 是从这里 refer 的
- 应用级/模块级 scope 也是从这里开始组织的

如果把它放到“装配层后讲”，整个主干就会倒着写。

### 2. `dubbo-common` 的命名会再次误导你

`dubbo-common` 听起来像工具库，但核心 SPI 机制 `ExtensionLoader`、模型隔离 `ScopeModel`、URL、URL 参数规则都在这里。

它不是杂项，是 Dubbo 最底层的“框架 DNA”。

### 3. `dubbo-rpc` 和 `dubbo-remoting` 容易被混成一篇

两者关系很近，但不能合并。

- `dubbo-rpc` 解决的是 invocation 语义：`Invoker`、`Protocol`、exporter、proxy、filter
- `dubbo-remoting` 解决的是传输与交换语义：client/server、exchange、dispatcher、codec、线程派发

如果把两者混成“远程调用实现”一篇，最后会失去两个最关键的边界：

- 框架如何抽象“调用”
- 调用如何真正被送上网络

### 4. `dubbo-registry` 不是外围组件，而是 consumer 主线的一部分

很多源码分析会把注册中心写成“外部依赖接入”。对 Dubbo 来说，这不够准确。

`RegistryProtocol`、`RegistryDirectory`、notify/update、override/configurator、address 刷新都直接影响 consumer 的可调用目标集合。这不是外围，这就是调用路径的一部分。

### 5. 插件很多，但不该一开始就吃

`dubbo-plugin` 下有大量 REST、security、QoS、serialization、tracing、servlet、native、MCP 等模块。

这些模块很重要，但在完整卷规划里应当属于：

- 平台 / 生态变体层
- 生产 / 观测层
- 安全 / 策略扩展层

如果在 baseline 阶段就试图全吃，整卷会失焦。

---

## 三、重新按方法论审计：Dubbo 应拆成哪些知识域

下面不按目录分，而按机制分。

### A. 主干运行时层

这是 Dubbo 的第一性问题：

- 一个服务如何 export 出去
- 一个引用如何 refer 进来
- 一次 consumer 调用如何穿过 directory / router / loadbalance / cluster / invoker / protocol / remoting 到达 provider
- provider 如何再从 network request 回到业务实现

这一层如果没立住，后面 registry、config-center、metadata、triple、metrics 都会漂。

建议拆出：

#### D-MAIN-1 Bootstrap、ScopeModel 与应用生命周期
回答：
- `DubboBootstrap` 在整个系统中是什么位置
- 为什么 3.x 要引入 `ApplicationModel / ModuleModel / ScopeModel`
- export/refer 生命周期从哪里开始

#### D-MAIN-2 `ServiceConfig` / `ReferenceConfig`：服务暴露与引用主线
回答：
- provider 怎样 export
- consumer 怎样 refer
- URL、配置合并、scope 和延迟初始化怎样进入主流程

#### D-MAIN-3 `Invoker` / `Protocol` / Exporter / Proxy / Filter：Dubbo 的窄腰
回答：
- Dubbo 如何抽象“调用”
- 为什么 `Invoker` 是核心
- `Protocol.export()` / `Protocol.refer()` 如何成为 provider / consumer 的对称入口
- filter 链怎样挂上去

#### D-MAIN-4 Consumer 侧流量主线：Directory、Router、LoadBalance、Cluster
回答：
- provider 列表怎样变成可选 invokers
- routing 在哪一层做
- loadbalance 在哪一层做
- failover/failfast/forking/broadcast 这些 cluster 语义如何改变一次调用

#### D-MAIN-5 Remoting / Exchange / Dispatcher：网络与线程派发主线
回答：
- invocation 怎样变成请求报文
- client/server 怎样建立连接
- request-response / heartbeat / one-way 怎样承载
- 线程派发策略怎样影响 provider 侧执行

#### D-MAIN-6 Provider 侧调用回落：请求怎样回到业务实现
回答：
- server request 怎样经过 codec、exchange handler、protocol、exporter 回到目标 bean
- async / sync / one-way 怎样分流

### B. 集成层

Dubbo 的真实用户常常不是直接 new `ReferenceConfig`，而是通过 Spring、Spring Boot、配置文件、注解、自动装配进入。

如果不补这层，整卷会偏内部实现视角，缺真实使用入口。

建议拆出：

#### D-INT-1 Spring / Spring Boot 接入桥
- `@DubboService` / `@DubboReference` 怎样进入 bootstrap
- auto-configuration 如何创建 export/refer 生命周期

#### D-INT-2 配置合并与外部化来源
- application/module/provider/consumer/service/reference 配置怎样叠加
- properties / YAML / 系统属性 / 环境变量怎样进入 URL

### C. 协议与扩展层

这是 Dubbo 区别于 grpc-java 的一个巨大特点：SPI / adaptive 扩展不是附加件，而是基础设施。

建议拆出：

#### D-EXT-1 `ExtensionLoader` 与 Adaptive 机制
- `@SPI`
- `@Adaptive`
- wrapper extension
- activate extension
- 文件式扩展声明怎样进入运行时

#### D-EXT-2 URL 语义与参数驱动扩展选择
- Dubbo 如何用 URL 参数驱动 extension 选择
- 为什么 URL 是“配置 + 路由 + 扩展选择”的统一载体

#### D-EXT-3 协议实现对照：Dubbo2、Triple、Injvm
- `dubbo-rpc-dubbo`
- `dubbo-rpc-triple`
- `dubbo-rpc-injvm`
- 它们如何共享 `Protocol` 抽象，又在 wire path 上分叉

#### D-EXT-4 Serialization / Codec 边界
- Hessian2 / Kryo / Fastjson2 / Protobuf / Triple codec 等如何挂接

### D. 注册发现与控制面层

Dubbo 在注册发现方面比 grpc-java 更丰富，也更历史包袱重。

建议拆出：

#### D-CTRL-1 `RegistryProtocol` 与 `RegistryDirectory`
- registry 如何进入 consumer 主线
- notify/update/override 如何影响可调用 invokers

#### D-CTRL-2 Service Discovery / Application-Level Discovery / Migration
- 接口级地址发现与应用级服务发现的差异
- migration 逻辑如何在运行时切换

#### D-CTRL-3 Config Center / Dynamic Config / Override
- 动态配置如何影响 provider / consumer 行为

#### D-CTRL-4 Metadata Report
- metadata 在 Dubbo 中承担什么角色
- 为什么它不是简单的附属存储

### E. 生产与诊断层

Dubbo 的生产层比 grpc-java 更复杂，因为它有 cluster、router、registry、remoting、config-center 多层交互。

建议拆出：

#### D-PROD-1 调用失败与集群容错排障
- failover / failfast / forking / broadcast 线上怎么判因
- timeout / retry / mock / provider unavailable 怎么区分

#### D-PROD-2 Router、Directory、LoadBalance 排障
- “有 provider 但就是没选中”
- 路由规则、黑白名单、标签路由、条件路由的症状

#### D-PROD-3 连接、线程派发与 remoting 问题
- client/server 长连接
- heartbeat
- dispatcher 线程模型
- 队列堆积 / provider 假死

#### D-PROD-4 Registry / Config Center / Metadata 失配问题
- “注册中心有地址但调用不到”
- “配置已推送但行为没变”
- “metadata 不一致导致调用异常”

### F. 平台 / 生态变体层

这是后续扩展层，不建议放在 baseline 第一批。

可作为后续专题：

- REST / servlet / gateway 接入
- `dubbo-qos`
- metrics / tracing / observability
- native / test / mcp 等插件

---

## 四、Dubbo 更合理的整卷结构建议

### 第一组：主干运行时卷
1. DubboBootstrap、ScopeModel 与应用生命周期
2. ServiceConfig、ReferenceConfig 与 export/refer 主线
3. Invoker、Protocol、Exporter、Proxy 与 Filter 窄腰
4. Directory、Router、LoadBalance、Cluster consumer 流量主线
5. Remoting、Exchange、Dispatcher 与网络/线程派发
6. Provider 侧请求如何回到业务实现

### 第二组：集成层卷
7. Spring / Spring Boot 接入桥
8. 配置合并、外部化与 URL 生成

### 第三组：协议与扩展层卷
9. ExtensionLoader、Adaptive 与 Dubbo SPI 机制
10. URL 语义与扩展选择
11. Dubbo2、Triple、Injvm 协议对照
12. Serialization 与 Codec 边界

### 第四组：注册发现与控制面卷
13. RegistryProtocol、RegistryDirectory 与地址更新主线
14. Service Discovery / Migration 机制
15. Config Center / Dynamic Override
16. Metadata Report 体系

### 第五组：生产与诊断卷
17. 调用失败、超时、集群容错排障
18. Router、Directory、LoadBalance 选路诊断
19. Remoting、连接与线程派发问题
20. Registry / Config / Metadata 失配排障

### 第六组：平台与生态变体卷（按需）
21. REST / servlet / gateway
22. QoS / metrics / tracing
23. 生态插件与平台特化

---

## 五、最合理的第一批正文优先级

如果直接开始写 Dubbo，不要一上来吃 registry、config center 或 Triple。

第一批最合理的是 4 篇：

### 优先级 A：必须先立住
1. **DubboBootstrap、ScopeModel 与应用生命周期**
2. **ServiceConfig、ReferenceConfig 与 export/refer 主线**
3. **Invoker、Protocol、Exporter、Proxy 与 Filter 窄腰**
4. **Directory、Router、LoadBalance、Cluster consumer 流量主线**

原因：这 4 篇立住后，读者才能真正回答：

- Dubbo 是怎样活起来的
- 一次服务暴露怎样发生
- 一次 consumer 调用怎样流动
- 集群容错和路由是在哪一层起作用

### 优先级 B：主干立住后最值得补
5. **Remoting、Exchange、Dispatcher 与网络/线程派发**
6. **ExtensionLoader、Adaptive 与 Dubbo SPI 机制**
7. **RegistryProtocol、RegistryDirectory 与地址更新主线**

### 优先级 C：高阶与控制面层
8. Triple / Dubbo2 协议对照
9. Service Discovery / Migration
10. Config Center / Metadata / 生产排障

---

## 六、最终结论

Dubbo 不能按包结构平推，也不能只围绕“注册中心 + SPI + 调用链”三段粗粒度来写。

更合理的做法是：

- 先承认 `dubbo-config`、`dubbo-common`、`dubbo-registry` 这些模块的名字会误导人
- 再按主干运行时、集成层、协议扩展层、控制面层、生产层重组知识域
- 最后用 4 篇主干 baseline 先把 Dubbo 的“生命线”立住，再往外扩展

**所以，下一步最合理的动作不是直接写某篇正文，而是：在这份规划基础上，开始第一篇 `DubboBootstrap、ScopeModel 与应用生命周期` 的 rewrite plan。**