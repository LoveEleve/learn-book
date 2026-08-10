# T-1 §2 Container 层次 — 5 级树 + ReadWriteLock + addChild 级联

> 依赖 §1 | 🔴 Deep | 5 KP | [模式: Composite]

**读者处境**: 知道所有容器共享 Lifecycle 状态机。但 "Server 怎么知道它有一个 Service?" "Engine 怎么知道 defaultHost 是哪个?" ——容器之间的**双向引用**是树结构的基础。不理解 addChild 的锁机制和级联启动，就不理解为什么 `server.start()` 一行代码能启动全部 5 层容器。

### 1. children HashMap + ReadWriteLock — 容器树的线程安全骨架

场景: 生产环境——一个管理请求通过 JMX 调用 `host.addChild(newContext)` 的同时——另一个 HTTP 请求通过 `host.findChild("/app")` 查找 Context——两个线程同时操作 children Map。

源码路径: `ContainerBase.java:154-155` — `HashMap<String,Container> children` + `ReentrantReadWriteLock childrenLock`。`ContainerBase.java:447-460` — `getParent()`/`setParent(Container)` — 双向引用: 父持有 children.put(child.getName(), child)，子持有 parent 字段。

关键设计: **ReadWriteLock 而非 ConcurrentHashMap**——为什么? `addChildInternal()`(L575-604) 的写入需要两步原子操作: 1) `child.setParent(this)` 建立反向引用 2) `children.put()`。如果 `setParent` 在锁外——另一个线程在 `setParent` 后、`put` 前调用 `findChild(name)`——返回 null 但 child.getParent() 已经指向 this——数据不一致。ReadWriteLock 保证"写锁内双向引用一次性建立"的原子性。

→ 实现规范: `Engine`/`Host`/`Context`/`Wrapper` 四个接口 → Servlet 规范 2.3 §9 容器模型。规范定义了四层抽象(Engine/Host/Context/Wrapper)，Tomcat 实现为四个 Standard* 类，通过 ContainerBase 统一树管理。

数据流: 写线程: `childrenLock.writeLock().lock()`→姓名查重→`child.setParent(this)`→`children.put()`→`writeLock.unlock()`→`fireContainerEvent(ADD_CHILD_EVENT)`→`child.start()`(锁外)。读线程: `childrenLock.readLock().lock()`→`children.get(name)`→`readLock.unlock()`。读写分离——多读者互不阻塞——只有写入时阻塞所有读。

### 2. addChild 的三层语义 — 不仅仅是 put

场景: 运维人员通过 Tomcat Manager 应用动态部署了一个新的 Context——`host.addChild(context)`。他不需要手动调 `context.start()`——addChild 在正确的时机自动启动子容器。

源码路径: `ContainerBase.java:566-604`。`addChild()` 安全检查 → `addChildInternal()`。三步: 1) lock+验证+建立引用 2) fire `ADD_CHILD_EVENT`(监听器——如 MapperListener——在此事件中更新路由表) 3) **锁外** `child.start()`——注释明确写 "locking the children object can cause problems elsewhere"(L596)。自动启动条件: `getState().isAvailable() || STARTING_PREP` 且 `startChildren=true`。

关键设计: **Why lock outside?** `child.start()` 内部会触发 `ContainerBase.startInternal()`——它又去 `children[].start()` 遍历子容器。如果子容器也有子容器——addChild 的写锁内再获得 children 的写锁 = 自旋死锁。**多级容器树的锁不能传递**——每层容器有自己的 childrenLock——外层锁必须在外层释放后才能进入内层。

架构意图: **Composite 模式** — Container 自身是 Container 类型且持有 `Container[] children`，形成递归树。`addChild`/`findChild`/`removeChild` 是树的增删查操作。`start()` 调用是树的深度优先遍历——父 start→children[].start→孙 start—... [模式: Composite]

数据流: `host.addChild(context)`→`addChildInternal`→writeLock→`context.setParent(host)`→`children.put("/app", context)`→writeLock.unlock→`fireContainerEvent(ADD_CHILD_EVENT, context)`→MapperListener 收到→`mapper.addHost()` 更新路由表→`context.start()`→`Context.startInternal()`→遍历 context 的 children(Wrappers)→`wrapper.start()`→每个 Wrapper 的 `initInternal`/`startInternal`...→全部完成后 context STARTED→host 继续。

### 3. parent/children 类型约束 — Container 树不能插错层

场景: 有人在测试时往 Engine 里加了一个 Wrapper——`engine.addChild(wrapper)`。这在逻辑上是错的——Engine→Host→Context→Wrapper 是固定的四层结构。但 Java 类型系统不会阻止——Engine、Host、Context、Wrapper 都实现 Container 接口。

源码路径: 各容器的 `addChild()` 覆写——通过 `instanceof` 检查类型: `StandardEngine.addChild()` 要求 Host 类型; `StandardHost.addChild()` 要求 Context 类型; `StandardContext.addChild()` 要求 Wrapper 类型。`StandardWrapper`(L1340) 是末级容器——**不能覆写 addChild**——因为 Wrapper 没有子容器。

关键设计: **为什么不是泛型 `Container<Host> extends Container`?** 因为 `ContainerBase` 是抽象基类——它需要在统一的 `HashMap<String,Container>` 中管理子容器——泛型会导致类型擦除后仍可用 `addChild(Container)`——`instanceof` 运行时检查是唯一真正的防护。

架构意图: **Chain of Responsibility 预留** — `ContainerBase` 持有 `Pipeline pipeline` 字段和 `Valve basic` 字段(L75-77)——这是为 T-3 Pipeline 预留的。容器层次是请求路由的骨架，Pipeline 是请求处理的骨架——两者在 ContainerBase 交汇。当前只讲容器树，但每个容器的 `invoke(Request, Response)` 方法最终调 pipeline 的第一个 Valve——这是下一篇文章的桥。

数据流: `StandardEngine.addChild(child)`→`instanceof Host`→true→`super.addChild(child)`→`ContainerBase.addChild()`→...建立 parent→返回。若 `instanceof Host`=false→throw `IllegalArgumentException`——编译期通过、运行时拦截。

→ 引出 §3 Server+Service — 容器树内部连通了——但谁创建这棵树？`new StandardServer()` 怎么关联到 `new StandardService()`？顶部两层的职责和顺序反转又是什么？
