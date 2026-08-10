# T-1 容器+Lifecycle 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | `LifecycleBase.start()` 内部先调 `setState(STARTING_PREP)` 再调 `startInternal()` — 如果子类在 `startInternal` 中抛异常，状态会变成什么？为什么不是回滚到 `INITIALIZED`？ | §1.1 |
| 2 | ContainerBase 的 `children` 为什么用 HashMap+ReadWriteLock 而非 ConcurrentHashMap？`addChild` 的写锁内为什么需要两步原子操作？ | §2.1 |
| 3 | `addChildInternal()` 的 `child.start()` 为什么在写锁**外**执行？如果放在写锁内会发生什么？ | §2.2 |
| 4 | `StandardEngine.addChild()` 怎么确保只接受 Host 类型？如果传了 Wrapper 会发生什么？ | §2.3 |
| 5 | `server.await()` 的 port=-1 模式为什么不用 ServerSocket 等待？它是怎么退出阻塞的？ | §3.1 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 6 | 为什么 Lifecycle 用 Template Method 而不是让每个子类重写 start()？如果子类自己管状态转换 — 会有什么风险？ | §1.1 |
| 7 | Engine 的 `setDefaultHost` 为什么**直接**调 Mapper 而不是通过 MapperListener 的 ContainerEvent？其他 Host 的 addChild 为什么可以异步？ | §4.1 |
| 8 | Service.stopInternal() 为什么必须 connectors.gracefulClose → pause → engine.stop → connectors.stop？如果把 engine.stop 放在 connectors.stop 之后会怎样？ | §3.2 |
| 9 | Lifecycle 状态机有 11 态 — 但每个组件的 `initInternal` 实现差异很大(ContainerBase 启动 children, StandardServer 启动 services)。Template Method 怎么适应这些差异？ | §1.3 |
| 10 | Tomcat 的容器层次(Server→Engine→Host→Context→Wrapper) 和 Servlet 规范的容器模型(Engine/Host/Context/Wrapper) 有什么不同？Server 和 Service 是 Tomcat 独有还是规范定义的？ | §4 规范映射表 |
| 11 | 5 层容器的启动是递归还是线程池并行？如果 Host 有 100 个 Context — `startInternal` 怎么处理这 100 个启动才不串行阻塞？ | §1.3 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 12 | `server.start()` 一行代码 — 为什么能启动全部 5 层容器？调用链是怎么传下去的？ | §3.3 |
| 13 | Lifecycle 的 13 种事件中 — `CONFIGURE_START` 和 `START` 有什么区别？为什么要分开？ | §1.2 |
| 14 | Context 和 Wrapper 的关系 — 是一个 Web 应用对应一个 Context、一个 Servlet 对应一个 Wrapper 吗？ | §4.3-§4.4 |
| 15 | 容器的 parent/children 双向引用 — 如果 child.getParent() 和 parent.findChild(name) 在不同线程执行 — 怎么保证一致性？ | §2.1 |

## 覆盖: 15 问 / 3 身份 / 100%
