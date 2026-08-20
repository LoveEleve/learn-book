# T-1 容器 + Lifecycle 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧多视角 15 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. Lifecycle 为什么用 Template Method 而不是让每个子类重写 start()? 如果子类自己管状态转换会有什么风险?
2. `LifecycleBase.start()` 内部先 setState(STARTING_PREP) 再调 startInternal() — 如果 startInternal 抛异常, 状态会变成什么? 为什么不是回滚到 INITIALIZED?
3. ContainerBase 的 children 为什么用 HashMap+ReadWriteLock 而非 ConcurrentHashMap? addChild 的写锁内为什么需要两步原子操作?
4. Service.stopInternal() 为什么必须按 connectors.gracefulClose → pause → engine.stop → connectors.stop 的顺序? 把 engine.stop 放最后会怎样?
5. server.await() 的 port=-1 模式为什么不用 ServerSocket 等待? 它是怎么退出阻塞的?

### B. 源码实证 (6)
6. LifecycleState 枚举有哪些态? 一共几个? (grep catalina/LifecycleState.java:23)
7. LifecycleBase.start() 的 synchronized 声明与 startInternal() 的 abstract 声明? (grep catalina/util/LifecycleBase.java:139/193)
8. children 字段的容器类型与锁类型? (grep catalina/core/ContainerBase.java:154 + L35-36)
9. StandardEngine.addChild() 怎么确保只接受 Host 类型? (grep catalina/core/StandardEngine.java)
10. await() 方法的实现位置? (grep catalina/core/StandardServer.java:508)
11. 13 种 LifecycleEvent 类型常量在哪定义? (grep catalina/Lifecycle.java)

### C. 推理深挖 (5)
12. Lifecycle 状态机 11 态 — 但每个组件的 initInternal 实现差异很大 (ContainerBase 启动 children, StandardServer 启动 services) — Template Method 怎么适应这些差异?
13. 5 层容器 (Server→Engine→Host→Context→Wrapper) 的启动是递归还是并行? Host 有 100 个 Context 时 startInternal 怎么处理才不串行阻塞?
14. Engine 的 setDefaultHost 为什么直接调 Mapper 而不是通过 MapperListener 的 ContainerEvent? 其他 Host 的 addChild 为什么可以异步?
15. addChildInternal() 的 child.start() 为什么在写锁外执行? 放在写锁内会发生什么 (死锁风险)?
16. 容器的 parent/children 双向引用 — child.getParent() 和 parent.findChild(name) 在不同线程执行怎么保证一致性?

### D. 跨域扩展 (4)
17. 本域 vs T-2 Connector: Connector 为什么也是 Lifecycle 组件? 它和 Container 的启动顺序谁先谁后?
18. 本域 vs T-10 集群: SimpleTcpCluster 继承 LifecycleMBeanBase — 集群组件的生命周期如何随 Server 级联?
19. 本域 vs openjdk: JVM 的 HotSpot VM 生命周期 (create→init→boot→start) 与 Tomcat Lifecycle 状态机有什么同构性?
20. 本域 vs Nacos: Nacos 的 ApplicationContext 启动与 Tomcat Lifecycle 的级联启动, 哪个更接近"状态机驱动"?