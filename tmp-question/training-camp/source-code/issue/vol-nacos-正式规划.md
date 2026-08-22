# vol-nacos 正式规划（基于 Nacos 3.0.3）

## 一、规划前提

这份规划不是先拍一个篇数，再去硬塞主题，而是按两套材料交叉校准后得出的：

1. **主依据：Nacos 3.0.3 源码结构**  
   代码位置：`/data/workspace/source-code/code/spring/nacos`
2. **参考依据：已有专题拆解资料**  
   资料位置：`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/zsxq-zsxq/nacos`

其中：

- **源码决定主线和边界**
- **参考资料用于查漏补缺**
- 规划最终必须服从当前本地源码版本的真实结构，而不是服从旧文章的拆法

## 二、先给结论：不是 6 篇，也不建议硬压成 12 篇

经过重新探索后，`vol-nacos` 更合理的规划规模是：

- **主计划：16 篇**
- **完整版：17 篇**

如果硬压成 `12` 篇，会明显压扁以下真实 seam：

1. `naming` 的 `ephemeral` / `persistent` / `push-subscribe` / `client redo-failover`
2. `config` 的 `write` / `read` / `long polling` / `dump-reconcile`
3. `core` / `remote` / `cluster` / `auth` / `persistence` / `consistency`
4. `console/operator shell` 与 `client sdk` 这两条很容易被漏掉的运行面

所以这里不采用“先定 12 篇，再强行归并”的办法，而采用“按真实架构 seam 定篇，再看能否谨慎合并”的办法。

## 三、为什么是 16~17 篇：从源码结构反推 seam

### 1. 顶层模块说明 Nacos 不是只有 naming 和 config

从 `3.0.3` 顶层模块看，至少有这些真正影响源码分析的域：

- `core`
- `server`
- `client`
- `client-basic`
- `api`
- `common`
- `naming`
- `config`
- `consistency`
- `persistence`
- `auth`
- `console`
- `plugin`
- `plugin-default-impl`
- `prometheus`
- `k8s-sync`
- `maintainer-client`

这说明 Nacos 的源码现实不是“两个业务模块 + 一个 Raft”。

### 2. 参考资料也支持更细粒度拆分

你给的资料目录里，已经天然拆出了几条独立主线：

- naming client register / subscribe / failover / redo
- naming server push / cache / client / connection
- distro 一致性
- jraft / multi raft group
- config client publish / query / listener
- config server publish / query / cache / dump / notify
- grpc server / connection manager / server start

这进一步说明：

- `naming` 不能写成两篇
- `config` 不能写成两篇
- `transport + cluster + consistency` 不能合并成一篇笼统“集群架构”

## 四、正式篇章规划

下面采用 **6 组、16 篇主计划**。

---

# 第一组：总图与共享内核（2 篇）

这组负责建立 Nacos 的总地图，不直接进入 naming/config 细节。

## 01. Nacos 3.0.3 总图：模块、打包、启动装配与运行时骨架

### 角色定位

整卷入口篇。

### 要回答的问题

- Nacos 3.0.3 到底由哪些模块组成？
- `server` 为什么不是业务核心，而更像聚合打包层？
- `core`、`naming`、`config`、`client`、`console` 的职责边界在哪里？

### 为什么必须独立成篇

没有这篇，后面 naming/config/auth/consistency 每篇都会反复补背景，读者也会持续迷路。

---

## 02. Shared Kernel：core / sys / bootstrap 如何提供公共运行时底座

### 角色定位

共享基础设施篇。

### 要回答的问题

- `core` 到底不是“杂物间”，而是哪些能力的底座？
- request handler 注册、连接管理、RPC server、成员管理、事件与公共生命周期在哪里？
- naming 和 config 到底是如何站在同一套 kernel 上的？

### 为什么必须独立成篇

因为 `core` 是后面 naming/config/cluster/auth 都要反复引用的公共地板。

---

# 第二组：remote / cluster / auth（3 篇）

这组讲“服务怎么连起来、节点怎么协作、权限怎么切进去”。

## 03. Remote 模型：API 请求对象、RequestHandler、gRPC 长连接与服务端接收链

### 角色定位

协议与接入入口篇。

### 要回答的问题

- Nacos 3.x 的 remote 模型长什么样？
- SDK 请求是怎么落到服务端 RequestHandler 的？
- gRPC 长连接和早期 HTTP/长轮询模型在运行时是怎样并存的？

### 为什么必须独立成篇

因为 naming/config 虽然业务不同，但接入链路高度共享；把它们拆开前，先要建立共同的 remote spine。

---

## 04. Cluster 运行时：成员管理、节点通信、ServerMemberManager 与节点协作

### 角色定位

节点协作篇。

### 要回答的问题

- Nacos 服务器节点怎么知道彼此是谁？
- server-to-server RPC 怎么打？
- 什么时候是 client→server，什么时候是 server→server？

### 为什么必须独立成篇

因为这条线既不等于 naming，也不等于 config，更不等于 consistency，它是所有集群行为的共同外壳。

---

## 05. Auth 与 Plugin SPI：HTTP/gRPC 鉴权、资源解析、扩展点与插件装配

### 角色定位

认证与扩展边界篇。

### 要回答的问题

- 鉴权是怎么切进 naming/config/console/gRPC 的？
- `auth` 为什么不能只当成“登录校验”？
- 插件 SPI 在 Nacos 里是如何落地的？

### 为什么必须独立成篇

因为 auth 跨 naming/config/console，既是运行时横切，也是生产安全边界，压到其他篇里会被写薄。

---

# 第三组：Naming 主线（4 篇）

这组讲 Nacos 作为注册中心的主体。

## 06. Naming 领域模型：Service、Instance、Namespace、Cluster 与 ServiceManager

### 角色定位

模型总图篇。

### 要回答的问题

- naming 的核心对象到底有哪些？
- 服务、实例、命名空间、集群、客户端之间是什么关系？
- 为什么后面的临时实例和持久实例要分两条路？

### 为什么必须独立成篇

因为不先立对象模型，后面所有 register/beat/push/distro/persistent 都会失去坐标系。

---

## 07. Ephemeral 路径：临时实例注册、心跳、健康检查与快速收敛

### 角色定位

AP 风格 naming 主线篇。

### 要回答的问题

- 临时实例注册后，beat / health check / expire 是怎么跑的？
- client 心跳、server 健康检查、实例在线语义怎么闭环？
- 为什么这个模型天然偏 AP？

### 为什么必须独立成篇

因为临时实例是 Nacos naming 最常见的实际使用路径，值得单独讲透。

---

## 08. Persistent 路径：持久实例注册、非临时服务语义与 CP 轨

### 角色定位

CP 风格 naming 主线篇。

### 要回答的问题

- 持久实例为什么不能复用临时实例那套逻辑？
- persistent instance 注册为什么和 consistency / jraft 绑得更紧？
- naming 为什么不是单一 AP 注册中心？

### 为什么必须独立成篇

因为这是 Nacos 源码里一个最容易被“临时实例主线”遮掉、但架构上极关键的分裂点。

---

## 09. Naming 读路径：订阅、推送、ServiceInfo 更新、Failover 与 Redo

### 角色定位

client view 收敛篇。

### 要回答的问题

- 客户端订阅后，本地 `ServiceInfo` 怎么刷新？
- 服务端主动推送、客户端被动拉取、本地 failover、连接恢复 redo 是怎么协同的？
- 为什么 naming 的“注册成功”不等于“所有消费者视图都同步完成”？

### 为什么必须独立成篇

因为读路径和写路径是两套不同的收敛链，不能压在注册篇里一笔带过。

---

# 第四组：Config 主线（4 篇）

这组讲 Nacos 作为配置中心的主体。

## 10. Config 写路径：发布、持久化、灰度/加密/历史与变更发布

### 角色定位

config write path 篇。

### 要回答的问题

- 配置发布不只是写库，完整链路到底经过哪些对象？
- 灰度、加密、历史记录分别挂在哪一层？
- 一次 config publish 怎么变成后续通知的起点？

### 为什么必须独立成篇

因为配置写路径本身就足够复杂，强行和 query/listen 合并会损失因果链。

---

## 11. Config 读路径：查询、缓存、服务端结果组装与查询处理链

### 角色定位

config read path 篇。

### 要回答的问题

- 查询配置时服务端怎么从缓存、存储、规则层拿结果？
- 为什么 query path 不是 publish path 的镜像？
- 哪些语义属于 server read path，哪些属于 client cache path？

### 为什么必须独立成篇

因为读链有自己的一套 cache/decrypt/assembly 逻辑，不应该被 publish 篇吞掉。

---

## 12. Config 实时性核心：Long Polling、变更通知与客户端监听更新

### 角色定位

config near-real-time 篇。

### 要回答的问题

- Nacos config 为什么经典心智一直是 long polling？
- 监听、MD5 比较、阻塞等待、返回变更集合是怎么配合的？
- 3.x 里这条线和 RPC 化后的路径是什么关系？

### 为什么必须独立成篇

因为 long polling 是 Nacos config 最经典、也最应该源码化讲透的一条线。

---

## 13. Config 后台维护：Dump、Reconcile、Cache Warmup 与集群同步

### 角色定位

后台维护与一致性收束篇。

### 要回答的问题

- DumpService 到底在维护什么？
- 配置中心为什么除了 publish/query/listen，还需要后台 dump/reconcile？
- 这条线对故障恢复、热数据保持、集群一致性意味着什么？

### 为什么必须独立成篇

因为这条线是很多表面分析会漏掉的“后台现实”，但它恰恰是生产稳定性的关键。

---

# 第五组：存储与一致性（2 篇）

这组专门处理 Nacos 最容易被口号化的“AP + CP”。

## 14. Persistence 模式：Embedded、External MySQL、DynamicDataSource 与条件装配

### 角色定位

存储模式篇。

### 要回答的问题

- Nacos 到底有哪些存储模式？
- embedded 与 external datasource 的切换在哪里发生？
- config/naming/console 对存储模式的依赖各是什么？

### 为什么必须独立成篇

因为“配个 MySQL 就完了”的理解太粗了，而 persistence mode 是运维和源码都绕不过去的基础 seam。

---

## 15. AP / CP 双轨：Distro、JRaft、Consistency 抽象与为什么必须分裂

### 角色定位

一致性总图篇。

### 要回答的问题

- Nacos 为什么同时保留 AP 和 CP 两条轨？
- distro 和 jraft 分别服务什么业务语义？
- `consistency` 模块、`core` 中的 jraft wiring、naming persistent path 之间怎么连起来？

### 为什么必须独立成篇

因为这是 Nacos 架构最容易被一句“支持 AP/CP”糊弄过去、但实际最值得单独压成一篇的核心设计。

---

# 第六组：运维面与客户端现实（2 篇，或压成 1 篇）

## 16. Console / OpenAPI / 运维操作面：Namespace、State、Admin 工作流与 operator shell

### 角色定位

运维外壳篇。

### 要回答的问题

- console 到底只是 UI 壳，还是一个聚合 naming/config/auth/state 的 operator shell？
- namespace、server state、配置管理、实例管理在 console 里的组织方式说明了什么？
- 什么属于“业务能力”，什么属于“运维入口”？

### 为什么必须独立成篇

因为 console 不是边角料；它是运维理解系统行为的真实外壳。

---

## 17. Client SDK 现实：NacosConfigService、NacosNamingService、重连、Redo 与本地故障转移

### 角色定位

客户端运行时收束篇。

### 要回答的问题

- `NacosNamingService` 和 `NacosConfigService` 真正对应用隐藏了什么复杂度？
- 连接断开后 redo、listener、failover、local cache 是怎么自愈的？
- 为什么很多线上现象不能只看 server，必须回到 client sdk？

### 为什么建议单独成篇

因为大量真实问题都出在“server 看起来没问题，但 client 视图没有恢复”这类场景上。

### 如果必须压缩

可以把第 16、17 两篇压成 1 篇，形成 **16 篇主计划**；但完整版仍建议保留为 **17 篇**。

---

## 五、为什么不是 12 篇：查漏补缺结论

如果压成 12 篇，最容易缺的就是下面这些 seam：

1. **auth/plugin SPI** 会被吞进 remote 或 core，最后只剩一句“有鉴权”
2. **persistent naming path** 会被吞进 naming 总篇，失去 AP/CP 分裂的真实语义
3. **config dump/reconcile** 会被吞进 config 存储篇，后台维护链会消失
4. **persistence mode** 会被吞进 consistency，总结会太抽象，不够源码化
5. **console/operator shell** 会直接消失
6. **client sdk internals** 会直接消失，失去大量真实线上问题的解释力

所以 12 篇不是不可能，而是会明显牺牲完整性。

## 六、建议写作顺序

不建议一上来就写 naming client 或 config long polling，而建议按理解路径推进：

1. `01` 总图
2. `02` shared kernel
3. `03` remote 模型
4. `04` cluster 运行时
5. `06` naming 模型
6. `07` naming ephemeral
7. `08` naming persistent
8. `09` naming 读路径
9. `10` config 写路径
10. `11` config 读路径
11. `12` config long polling
12. `13` config dump / reconcile
13. `05` auth / plugin SPI
14. `14` persistence mode
15. `15` AP / CP 双轨总图
16. `16` console / operator shell
17. `17` client sdk 现实

这样排的原因是：

- 先立总骨架和 transport/kernel
- 再分别打通 naming 和 config 两条业务主线
- 最后再收 auth、存储、一致性、console、client reality 这些横切和收束主题

## 七、与 Spring Cloud Alibaba 的边界

由于另一个 AI 正在推进 Spring Boot / Spring Cloud Alibaba 相关内容，这里明确边界：

### 本卷不作为主体展开

- `spring.config.import` / ConfigData Resolver
- `PropertySourceLocator` / bootstrap 兼容桥
- `DiscoveryClient` / `ServiceRegistry` / `LoadBalancer` 适配
- Spring Environment / Refresh / Bean 刷新链

### 本卷只保留边界说明

- Spring Cloud Alibaba 本质上是 **Nacos client API 的 Spring 侧接入层**
- `vol-nacos` 的主体仍然应该围绕 **Nacos 自己的 client/server/runtime/consistency**
- Spring Cloud Alibaba 相关内容后续只做 cross-link，不占本卷主要篇幅

## 八、最终建议

### 正式目标

- **建议按 16 篇主计划推进**
- **有余力时保留 17 篇完整版**

### 最稳的表述

`vol-nacos` 不应该被规划成“命名服务几篇 + 配置中心几篇”的小册子，而应该被规划成：

- **共享内核**
- **remote / cluster / auth**
- **naming 主线**
- **config 主线**
- **persistence / consistency**
- **console / client reality**

六组能力的完整源码分析卷。

### 当前结论

- `6` 篇：明显不够
- `12` 篇：仍然偏紧，容易漏关键 seam
- **`16` 篇：主计划最稳**
- **`17` 篇：完整版更完整**
