# 第四个月：架构层（130h）

> **目标**：系统掌握 Java 框架原理、容器与云原生核心技术、系统架构设计方法论，打通从代码到部署的完整技术链路，建立"分布式 → 框架 → 云原生 → 架构设计"的学习闭环。
> **核心理念**：前三个月建好了根基（OS/JVM/数据），第四个月是"架构能力层"——理解框架底层原理、掌握容器化部署、能设计高并发高可用系统架构。
> **月末产出**：能讲清楚 Netty 线程模型/Spring Bean 生命周期/Tomcat 类加载器、能独立部署 Docker 容器和 K8s Pod、能完成高并发/高可用/亿级流量的系统架构设计、能写 gRPC 服务端和客户端、算法刷题起步。

>
> **本规划依据**：[学习大纲.md](../../学习大纲.md) 第四阶段（架构层 43本：分布式系统 + 架构设计 + 容器云原生 + Java框架）+ [4个月面试冲刺路线.md](../../4个月面试冲刺路线.md)

---

## 第四个月要攻克什么？

```
第四个月的核心命题：架构能力 —— 框架原理 + 容器部署 + 系统设计

    ┌──────────────────────────────────────────────────────┐
    │                    第四个月：架构层                     │
    ├──────────────┬──────────────┬──────────────┬──────────┤
    │  Java 框架    │  容器云原生   │  系统架构设计  │ 算法起步  │
    │   (7本)      │   (6本)      │   (8本)      │          │
    ├──────────────┼──────────────┼──────────────┼──────────┤
    │  Netty 线程   │  Docker 原理  │  高并发架构   │ 排序/查找  │
    │  Spring IoC  │  K8s 调度    │  高可用设计   │ 链表/树   │
    │  Tomcat 类加  │  gRPC 四种   │  亿级流量    │  DP/回溯  │
    │  载器模型     │  调用模式    │  系统设计实战  │          │
    └──────────────┴──────────────┴──────────────┴──────────┘
```

**四条主线并行推进**：
- **主线1（Java框架）**：Netty 线程模型 → Spring IoC/AOP/Bean 生命周期 → Tomcat 类加载器与连接器架构
- **主线2（容器云原生）**：Docker 核心原理（Namespace/Cgroups/UnionFS）→ K8s 核心概念（Pod/Deployment/Service）→ gRPC + Service Mesh
- **主线3（系统架构设计）**：高并发架构 → 高可用架构 → 亿级流量 → 系统设计实战（秒杀/短链/Feed流/IM/排行榜）
- **主线4（算法起步）**：排序/查找 → 链表/树 → DP/回溯 → 哈希/图（为第五个月算法冲刺打基础）

**四条主线的交汇点**：
- Netty 的 Reactor 模式 ← → 第一个月第四周 epoll/Reactor → Docker 的 Namespace/Cgroups ← → 第一个月第二周内核
- Spring 的类加载器 ← → 第二个月第六周 JVM 类加载 → Tomcat 的类加载器 ← → 打破双亲委派
- K8s 的 etcd（Raft）← → 第二个月第八周 Raft 协议 → gRPC 的 HTTP2 ← → 第一个月第三周 TCP 协议栈
- 系统设计的数据层 ← → 第三个月 MySQL/Redis → 分布式设计 ← → 第三个月第十一周 CAP/Raft

---

## 四周内容总览（与学习大纲完全对齐）

| 周次 | 主题 | 核心产出 | 目录 | 对应学习大纲 |
|------|------|---------|------|------------|
| **第13周** | Java 框架（Netty/Spring/Tomcat） | 能讲清楚 Netty EventLoopGroup→EventLoop→Channel 模型、Spring Bean 生命周期、Tomcat 类加载器模型 | [第十三周/](./第十三周/) | 4.4 Java框架（Netty核心 + Spring核心 + Tomcat架构） |
| **第14周** | 系统架构设计 | 能完成高并发/高可用/亿级流量系统架构设计、能独立完成 5 个系统设计题、掌握安全架构(OAuth/JWT/TLS/OWASP)、10种高频设计模式、软件测试基础(JUnit5/Mockito/TDD) | [第十四周/](./第十四周/) | 4.2 架构设计（书103/104/106核心）+ 4.1 分布式系统深入 + 工程素养（安全/设计模式/测试） |
| **第15周** | 容器云原生（Docker/K8s/gRPC） | 能独立部署 Docker 容器和 K8s Pod、能写 gRPC 服务端和客户端、能讲清楚容器化原理 | [第十五周/](./第十五周/) | 4.3 容器与云原生（Docker原理 + K8s核心 + gRPC/ServiceMesh） |
| **第16周** | 架构总结 + 算法起步 | 能画出完整的架构图、能讲清楚分布式系统的核心设计模式、算法刷题起步、掌握技术写作与分享能力 | [第十六周/](./第十六周/) | 架构综合 + 算法起步（排序/查找/链表/树） + 技术写作与分享 |

---

## 完整书籍章节映射（来自学习大纲.md）

### 第13周：Java 框架（Netty/Spring/Tomcat）

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 4.4 Java框架 | **Netty核心** | 第1-5章（Reactor/EventLoop/Pipeline） | EventLoopGroup→EventLoop→Channel 线程模型、Pipeline 责任链模式（入站/出站）、零拷贝（FileRegion/CompositeByteBuf/DirectBuffer）、ByteBuf 内存管理（堆内/堆外/池化） |
| 4.4 Java框架 | **Spring核心** | 第1-4章（IoC容器/Bean生命周期/AOP） | IoC 容器启动流程（Resource→BeanDefinition→BeanFactory→ApplicationContext）、Bean 生命周期（实例化→属性填充→初始化→销毁）、循环依赖解决（三级缓存）、AOP 实现（JDK动态代理 vs CGLIB） |
| 4.4 Java框架 | **Tomcat架构** | 第1-3章（类加载器/连接器） | 类加载器模型（Common/Catalina/Shared/Webapp ClassLoader）、破坏双亲委派的原因、连接器架构（Endpoint→Processor→Adapter）、NIO 在 Tomcat 中的使用 |

**多书交叉印证**（学习大纲推荐）：
- Netty 线程模型：Netty核心 第1-3章（实现视角）+ 第一个月第四周 Reactor 模式（理论基础）+ Linux epoll 源码（内核视角）
- Spring IoC/Bean生命周期：Spring核心 第1-4章（容器视角）+ 第二个月第六周 JVM 类加载器（对比视角）
- Tomcat 类加载器：Tomcat架构 第1-2章（Tomcat视角）+ JVM 类加载双亲委派（对比视角）+ Spring 破坏双亲委派（工程视角）

### 第14周：系统架构设计

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 4.2 架构设计 | **书103/104/106（架构设计核心）** | 高并发/高可用/可扩展/安全性 | 高并发架构（读写分离/缓存策略/异步化/限流熔断）、高可用架构（冗余/故障转移/容灾/降级）、可扩展架构（微服务拆分/服务化/中台）、安全性（认证/授权/加密/审计） |
| 4.1 分布式系统 | **分布式深入** | CAP权衡/一致性模型/分布式事务 | CAP 在系统设计中的 trade-off、最终一致性方案（消息队列+本地消息表/Saga）、分布式锁（Redis/ZooKeeper/etcd）、分布式 ID 生成 |
| 4.2 架构设计 | **系统设计实战** | 秒杀/短链/Feed流/IM/排行榜 | 秒杀系统（前端限流→接入层→业务层→数据层）、短链系统（Base62编码+布隆过滤器+缓存）、Feed流（推拉结合+Timeline模型）、IM系统（长连接+消息队列+离线消息）、排行榜（Redis ZSet 跳表实现） |

**多书交叉印证**：
- 系统设计：架构设计核心 第1-3章（方法论视角）+ 第三个月第十一周 分布式理论（CAP视角）+ 第三个月第十周 Redis/MySQL（数据层视角）

### 第15周：容器云原生（Docker/K8s/gRPC）

| 学习大纲来源 | 书籍 | 核心章节 | 学习大纲学习重点 |
|-------------|------|---------|----------------|
| 4.3 容器与云原生 | **Docker原理** | 第1-3章（Namespace/Cgroups/UnionFS） | Namespace 六大隔离（PID/NET/MNT/UTS/IPC/USER）、Cgroups 资源限制（CPU/内存/IO）、UnionFS 分层镜像原理、Docker 和虚拟机的区别 |
| 4.3 容器与云原生 | **K8s核心** | 第1-4章（Pod/Deployment/Service/调度） | Pod 概念与设计原因（共享网络/存储命名空间）、Deployment 滚动更新/回滚、Service 负载均衡（ClusterIP/NodePort/LoadBalancer）、调度器流程（预选→优选→绑定）、etcd 与 Raft 的关系 |
| 4.3 容器与云原生 | **gRPC/ServiceMesh** | 第1-2章（gRPC 四种模式/HTTP2） | gRPC 四种调用模式（Unary/ServerStreaming/ClientStreaming/Bidirectional）、HTTP2 多路复用与 gRPC 的关系、Protobuf 序列化、Service Mesh Sidecar 模式（Envoy/Istio） |

**多书交叉印证**：
- Docker 原理：Docker原理 第1-3章（容器视角）+ 第一个月第二周 Namespace/Cgroups（内核视角）+ Docker 官方文档（实践视角）
- K8s 核心：K8s核心 第1-4章（编排视角）+ 第三个月第十一周 CAP/Raft（分布式视角）+ K8s 官方文档（实践视角）
- gRPC：gRPC 第1-2章（RPC视角）+ 第一个月第三周 TCP/HTTP2（网络视角）+ Istio 文档（ServiceMesh视角）

### 第16周：架构总结 + 算法起步

| 学习大纲来源 | 内容 | 核心产出 |
|-------------|------|---------|
| 架构综合 | 完整架构图绘制、分布式设计模式总结 | 能画出完整系统架构图、能讲清楚常用设计模式（代理/适配器/观察者/策略/责任链） |
| 算法起步 | 排序（快排/归并/堆排序）、查找（二分查找）、链表（反转/环检测）、树（遍历/LCA）、哈希表（冲突解决） | 能手写基础算法、能分析复杂度 |

---

## 必须掌握的面试话题（第四月）

### Java 框架（面试高频考点 —— 工程深度）

1. **Netty 线程模型**：EventLoopGroup → EventLoop → Channel 三者关系？一个 EventLoop 绑定多个 Channel，一个 Channel 的所有 IO 事件由同一个 EventLoop 处理（无锁化设计）？bossGroup 和 workerGroup 的分工？Pipeline 的入站（ChannelInboundHandler）和出站（ChannelOutboundHandler）区别？

2. **Netty 零拷贝**：Netty 的零拷贝有哪几种实现（FileRegion/CompositeByteBuf/DirectBuffer）？和 OS 零拷贝（sendfile/mmap）的区别？堆外内存的优势和风险？

3. **Spring Bean 生命周期**：从实例化到销毁的完整链路（实例化→属性填充→Aware→BeanPostProcessor before→InitializingBean→init-method→BeanPostProcessor after→使用→DisposableBean→destroy-method）？循环依赖的三级缓存如何解决（singletonObjects/earlySingletonObjects/singletonFactories）？

4. **Spring AOP**：JDK 动态代理和 CGLIB 代理的区别？什么时候用 JDK 代理（基于接口），什么时候用 CGLIB（基于类）？Spring 如何选择（proxyTargetClass）？

5. **Tomcat 类加载器模型**：Common/Catalina/Shared/Webapp ClassLoader 的层级关系？为什么破坏双亲委派（Web 应用隔离、类版本冲突）？Webapp ClassLoader 的加载顺序（Bootstrap→System→Webapp 自己的/WEB-INF/classes 和 /WEB-INF/lib）？

### 容器云原生（面试高频考点 —— 工程广度）

6. **Docker 核心原理**：Namespace（PID/NET/MNT/UTS/IPC/USER 六种隔离）？Cgroups（CPU/内存/IO 限制）？UnionFS（分层镜像，写时复制）？Docker 和虚拟机的区别（共享内核 vs 独立 OS、启动速度、资源利用率）？

7. **K8s 核心概念**：Pod 是什么？为什么需要 Pod（共享网络/存储命名空间、是调度的最小单位）？Deployment 的滚动更新和回滚机制？Service 的四种类型（ClusterIP/NodePort/LoadBalancer/ExternalName）？

8. **K8s 调度流程**：预选（过滤不符合条件的节点）→ 优选（对符合条件的节点打分）→ 绑定（选择得分最高的节点）？调度策略（NodeName/NodeSelector/NodeAffinity/PodAffinity/Taints/Tolerations）？

9. **gRPC 四种调用模式**：Unary（一问一答）、Server Streaming（一问多答）、Client Streaming（多问一答）、Bidirectional Streaming（双向流）？HTTP2 如何支持这些模式（多路复用、Server Push）？

10. **Service Mesh**：Sidecar 模式解决了什么问题（服务治理与业务代码解耦）？Istio 的架构（Envoy 数据面 + Pilot 控制面）？和 API 网关的区别？

### 系统架构设计（面试必考 —— 区分度话题）

11. **高并发架构**：读写分离（主从复制、读写分离中间件）、缓存策略（Cache Aside/Read Through/Write Through/Write Behind）、异步化（消息队列削峰填谷）、限流熔断（令牌桶/漏桶/滑动窗口/Sentinel/Hystrix）

12. **高可用架构**：冗余（多副本/多活）、故障转移（主从切换/选主）、容灾（同城双活/两地三中心）、降级（服务降级/功能降级/熔断降级）

13. **系统设计题 — 秒杀系统**：前端限流（按钮置灰/验证码/答题）→ 接入层（Nginx 限流/WAF）→ 业务层（Redis 预减库存+MQ 异步下单）→ 数据层（分库分表/读写分离）→ 如何保证不超卖（Redis Lua 原子操作+数据库唯一索引）

14. **系统设计题 — Feed 流**：推模式（发帖时推送到所有粉丝的 Timeline，读取快写入慢）vs 拉模式（读取时拉取关注列表最新帖子，写入快读取慢）→ 推拉结合（大V用拉，普通用户用推）→ 热 Feed 缓存（Redis ZSet 按时间排序）

### 工程素养（面试加分项 —— 体现广度与工程成熟度）

15. **安全架构**：OAuth 2.0 授权码模式流程（为什么分两步比直接返回 token 安全）？JWT 三段结构（Header/Payload/Signature）和 Session 的优劣对比？TLS 1.3 握手需要几次 RTT（1-RTT vs TLS 1.2 的 2-RTT）？OWASP Top 5（SQL注入/XSS/CSRF/SSRF/不安全反序列化）的防御方案？

16. **设计模式**：JDK 动态代理 vs CGLIB 的区别？代理和装饰器模式的本质区别？Netty Pipeline 用了什么模式（责任链，入站出站双向）？Spring Event 用了什么模式（观察者）？策略模式和状态模式的区别？

17. **软件测试**：测试金字塔各层的投入比例（单元70%/集成20%/E2E 10%）？JUnit5 参数化测试怎么用？Mockito 中 @Mock 和 @InjectMocks 的区别？什么时候不该 Mock？TDD 的 Red-Green-Refactor 循环？

---

## 关键工具掌握

| 工具 | 用途 | 第几周 | 对应学习大纲建议 |
|------|------|--------|----------------|
| IntelliJ IDEA | Netty/Spring/Tomcat 源码阅读与调试 | 第13周 | Java框架专题 |
| Docker Desktop / Colima | Docker 容器构建与运行 | 第14周 | Docker原理 |
| minikube / kind | 本地 K8s 集群搭建 | 第14周 | K8s核心 |
| protoc + grpc 插件 | gRPC 代码生成与服务开发 | 第14周 | gRPC |
| draw.io / Excalidraw | 系统架构图绘制 | 第15周 | 架构设计 |
| LeetCode | 算法刷题起步 | 第16周 | 算法基础 |
| JMeter / wrk | 压力测试与性能验证 | 第15周 | 系统设计验证 |

---

## 前置知识检查表（进入第四个月需要掌握）

| 知识领域 | 你需要先掌握的内容 | 参考来源 |
|---------|-----------------|---------|
| **Reactor 模式 + epoll** | 事件驱动 IO、Reactor 多线程模型、epoll_create/epoll_ctl/epoll_wait | 第一个月 第4周 |
| **JVM 类加载器** | 双亲委派模型、findClass/loadClass 区别、打破双亲委派的场景 | 第二个月 第6周 |
| **JVM JMM** | happens-before、volatile、synchronized 锁升级 | 第二个月 第6周 |
| **Linux Namespace/Cgroups** | PID/NET/MNT 等六种 Namespace、Cgroups v1/v2 资源控制 | 第一个月 第2周 |
| **TCP 协议栈 / HTTP2** | TCP 三次握手/四次挥手、HTTP2 多路复用/Server Push/HPACK | 第一个月 第3周 |
| **CAP 定理 / Raft** | CAP 三个字母含义、Paxos vs Raft、Raft 三子问题 | 第三个月 第11周 + 第二个月 第8周 |
| **MySQL/Redis 基础** | B+树索引、MVCC、Redis 数据结构与持久化 | 第三个月 第9-10周 |
| **分布式理论** | 一致性模型、分布式事务方案对比、CAP 权衡 | 第三个月 第11周 |

---

## 与第一、二、三个月的衔接说明

**第四个月是"架构能力层"——站在前三个月的根基上，掌握框架原理、容器化部署和系统架构设计**：

| 前三个月（根基） | → | 第四个月（架构层） | 关联说明 |
|-----------------|---|-------------------|---------|
| 第一个月第四周：Reactor/epoll | → | 第13周 Netty 线程模型 | Netty 的 EventLoop 是对 epoll Reactor 模式的工业级实现 |
| 第二个月第六周：JVM 类加载器 | → | 第13周 Spring/Tomcat 类加载器 | Spring 和 Tomcat 都打破了双亲委派，理解 JVM 类加载机制是理解它们的前提 |
| 第二个月第六周：JMM | → | 第13周 Spring Bean 生命周期 | Spring Bean 的三级缓存解决循环依赖需要 JMM 保证可见性 |
| 第一个月第二周：Namespace/Cgroups | → | 第14周 Docker 原理 | Docker 容器 = Namespace 隔离 + Cgroups 限制 + UnionFS 文件系统 |
| 第一个月第三周：TCP/HTTP2 | → | 第14周 gRPC | gRPC 基于 HTTP2 的多路复用和 Server Push |
| 第二个月第八周：Raft 协议 | → | 第14周 K8s etcd | K8s 的 etcd 使用 Raft 协议保证分布式一致性 |
| 第三个月第9-10周：MySQL/Redis | → | 第15周 系统设计数据层 | 系统设计中的数据存储方案依赖 MySQL 索引和 Redis 数据结构 |
| 第三个月第11周：CAP/Raft/分布式 | → | 第15周 系统设计分布式层 | 系统设计中的一致性方案依赖分布式理论 |
| 第三个月第12周：系统设计 | → | 第15周 系统设计实战 | 第三个月的设计方法论在本月实战中深化 |

---

## 第四月综合自测（面试模拟）

完成四周学习后，用以下问题做一次完整的面试模拟。每题准备 3 分钟脱稿回答。

### Java 框架（6题）
1. Netty 的 EventLoopGroup → EventLoop → Channel 模型如何工作？bossGroup 和 workerGroup 的分工是什么？（基础）
2. Netty Pipeline 的责任链模式如何实现？入站和出站的区别？Netty 的零拷贝有哪几种实现？（进阶）
3. Spring Bean 的完整生命周期是什么？循环依赖的三级缓存如何解决？（基础）
4. Spring AOP 的 JDK 动态代理和 CGLIB 代理的区别？什么时候用哪个？（基础）
5. Tomcat 的类加载器模型是什么？为什么破坏双亲委派？（进阶）
6. Tomcat 的连接器架构（Endpoint→Processor→Adapter）如何工作？（进阶）

### 容器云原生（5题）
7. Docker 的三个核心原理（Namespace/Cgroups/UnionFS）分别解决什么问题？Docker 和虚拟机有什么区别？（基础）
8. K8s 的 Pod 是什么？为什么需要 Pod？Deployment 如何实现滚动更新？（基础）
9. K8s 的调度流程是怎样的？预选和优选阶段分别做了什么？（进阶）
10. gRPC 的四种调用模式分别是什么？HTTP2 如何支持这些模式？（基础）
11. Service Mesh 的 Sidecar 模式解决了什么问题？Istio 的架构是怎样的？（进阶）

### 系统架构设计（5题）
12. 画出秒杀系统的完整架构图，标注关键组件和技术选型，如何保证不超卖？（基础）
13. 画出 Feed 流系统的推拉结合架构，大V和普通用户的策略有什么不同？（基础）
14. 高并发架构的核心策略有哪些（读写分离/缓存/异步/限流）？各适用什么场景？（基础）
15. 高可用架构的核心策略有哪些（冗余/故障转移/容灾/降级）？各解决什么问题？（基础）
16. CAP 定理在系统设计中如何权衡？举例说明 AP 和 CP 系统的场景选择？（进阶）

### 分布式深入（3题）
17. 分布式事务有哪些常见方案（2PC/3PC/TCC/Saga/本地消息表）？各适用什么场景？（进阶）
18. 分布式锁有哪些实现方式（Redis/ZooKeeper/etcd）？各有什么优缺点？（基础）
19. 分布式 ID 生成有哪些方案（UUID/雪花算法/号段模式）？各有什么特点？（基础）

### 算法基础（3题）
20. 徒手写出快速排序（含 partition 函数），分析时间复杂度和空间复杂度？（基础）
21. 徒手写出二分查找的三种变体（精确查找/左边界/右边界），说明各自使用场景？（基础）
22. 徒手写出反转链表（迭代+递归），分析时间和空间复杂度？（基础）

---

## 学习方法提醒

1. **先骨架后细节**：Java 框架先理解整体架构（Netty 的线程模型、Spring 的 IoC 容器、Tomcat 的连接器架构），再深入实现细节
2. **动手验证**：Netty 写一个简单的聊天服务器、Spring 写一个完整的 CRUD 应用、Docker 写一个 Dockerfile、K8s 部署一个应用、gRPC 写服务端和客户端
3. **关联回顾**：学 Netty 时回顾第4周的 Reactor 模式、学 Spring/Tomcat 时回顾第6周的类加载器、学 Docker 时回顾第2周的 Namespace/Cgroups
4. **费曼学习法**：学完后用自己话讲一遍，讲不出来 = 没学会
5. **面试导向**：每个主题学完后问自己"面试官会怎么问这个问题"——Java 框架和容器云原生是 Java 后端面试的核心考点
6. **系统设计要画图**：用 draw.io 或白板画架构图，面试中画图能力是基本功
7. **算法每天 3-5 题**：第16周开始算法刷题，不求多但求每天坚持，为第五个月冲刺打基础

---

## 月末产出检查清单

- [ ] 能讲清楚 Netty EventLoopGroup→EventLoop→Channel 模型和 Pipeline 责任链
- [ ] 能讲清楚 Spring Bean 完整生命周期和三级缓存解决循环依赖
- [ ] 能讲清楚 Tomcat 类加载器模型和破坏双亲委派的原因
- [ ] 能独立部署 Docker 容器，编写 Dockerfile 和 docker-compose.yml
- [ ] 能独立部署 K8s Pod，创建 Deployment 和 Service
- [ ] 能写 gRPC 服务端和客户端（四种调用模式）
- [ ] 能完成秒杀/短链/Feed流/IM/排行榜 5 个系统设计题的架构设计
- [ ] 能讲清楚高并发/高可用/可扩展的核心策略
- [ ] 能手写排序（快排/归并/堆排序）、查找（二分查找）、链表（反转）
- [ ] 能画出完整的系统架构图并标注技术选型
- [ ] 能画出 OAuth 2.0 授权码流程图并解释每步安全性，对比 JWT vs Session
- [ ] 能解释 TLS 1.3 握手过程并说出比 TLS 1.2 的改进点
- [ ] 能列举 OWASP Top 5 攻击和对应的防御方案
- [ ] 能一句话说清 10 种高频设计模式的本质并找到项目中的实际应用
- [ ] 能写出规范的 JUnit5 单元测试，能用 Mockito 隔离外部依赖
- [ ] 能写出技术方案文档（背景→目标→方案对比→详细设计→风险→排期）
- [ ] 能用 STAR 框架准备 1 篇技术文章和 1 个技术分享

---

## 项目主线：短链服务 M4-架构里程碑

> 继续向核心实战项目推进：[贯穿项目-短链服务](../贯穿项目-短链服务/README.md)

### 本月项目��务

本月进行项目架构升级——**Netty 高性能接入层 + 微服务化 + Docker/K8s 容器化部署**。

| 任务 | 对应学习 | 产出 |
|------|---------|------|
| Netty 实现高性能网关（BossGroup+WorkerGroup） | 第13周 Netty核心 | Netty网关代码 |
| API/Admin/Stats 三服务拆分 | 第14周 系统设计 | 微服务代码 |
| Docker Compose 本地一键部署 | 第15周 Docker原理 | docker-compose.yml |
| Kubernetes 生产部署配置（Deployment/HPA/Ingress） | 第15周 K8s核心 | K8s YAML文件 |

### 学以致用

- **Netty Reactor**（第13周）→ BossGroup=Acceptor, WorkerGroup=Poller, Pipeline=Handler链
- **Spring IoC**（第13周）→ Bean生命周期管理在微服务中的应用
- **Docker Namespace/Cgroups**（第15周）→ 容器隔离和资源限制如何保障服务稳定
- **K8s HPA**（第15周）→ 基于CPU/内存指标的自动扩缩容

### 里程碑产出

- [ ] 完整架构设计文档（架构图 + 技术选型说明）
- [ ] Docker Compose 本地全栈部署
- [ ] K8s 生产部署配置 + CI/CD Pipeline

详见：[M4-架构里程碑.md](../贯穿项目-短链服务/M4-架构里程碑.md)
