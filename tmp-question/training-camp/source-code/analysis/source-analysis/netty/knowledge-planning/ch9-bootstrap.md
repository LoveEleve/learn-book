# Ch9 Netty Bootstrap — 知识规划

> 来源: 13 源文件 | ~1900 行 | transport/src/main/java/io/netty/bootstrap/
> 基线: Ch8 MemoryPool 提供了可复用的 ByteBuf — Ch9 回答 "如何组装成一个可运行的服务器"

---

## 01 提取 — 核心机制

### AbstractBootstrap.java (537行 — 17 KP)
- CRTP 泛型: `AbstractBootstrap<B extends AbstractBootstrap<B,C>, C extends Channel>` — Builder 自返回
- Fluent API: group/channel/handler/option/attr 全部返回 this
- doBind 三步: validate→initAndRegister→doBind0
- initAndRegister: channelFactory.newChannel→init(channel)→group().register
- 双重降级: register 失败→FailedChannel 兜底
- PendingRegistrationPromise: 异步等待 register 完成再 doBind0
- clone: 浅拷贝 config+深拷贝 options/attrs

### Bootstrap.java (348行 — 13 KP)
- connect: DNS resolve→doResolveAndConnect→doConnect
- 惰性 DNS: InetSocketAddress.createUnresolved 延迟解析
- ExternalAddressResolver: 避免 NoClassDefFoundError
- doConnect: 对称 doBind0 — EventLoop 线程投递
- 双重 clone: clone(group) + clone() 支持

### ServerBootstrap.java (312行 — 12 KP)
- 父子 EventLoop 分离: group(parentGroup, childGroup)
- childHandler/childOption/childAttr 独立存储(LinkedHashMap+ConcurrentHashMap)
- ServerBootstrapAcceptor: ChannelInitializer→accept→init child channel 六步
- childGroup 回退: childGroup==null → parentGroup
- exceptionCaught 限流恢复(#1328)

### ChannelInitializer (158行 — 6 KP, 已在 Ch7)
- handlerAdded 主线 + channelRegistered 兜底
- initMap 防重入 → initChannel 自毁

### ChannelFactory (3 KP)
- ReflectiveChannelFactory: clazz.getDeclaredConstructor().newInstance()
- bootstrap/ChannelFactory: void newChannel 回调式

### BootstrapConfig (8 KP)
- AbstractBootstrapConfig: 不可变快照 — group/channelFactory/options/attrs 只读

### ChannelInitializerExtension (9 KP)
- SPI 三级开关: none/serviceload/log
- 三级回调: clientListener/serverListener/serverChild
- WeakReference ClassLoader 缓存 + DCL 单例

---

## 02 深度分类

### 🔴 Deep
| KP | 为什么🔴 |
|----|---------|
| **doBind 三步状态机** | validate→initAndRegister→doBind0 — Bootstrap 的核心编排, 衔接 ChannelFactory/Channel/EventLoop 三大组件 |
| **ServerBootstrapAcceptor + child Channel 初始化六步** | 父 Channel accept→子 Channel 注册到 childGroup→注入 childHandler→fireChannelActive — Netty 服务端的完整 accept 流程 |

### 🟡 Working
| KP | 说明 |
|----|------|
| **CRTP 泛型 Fluent API** | `B extends AbstractBootstrap<B,C>` — 子类方法返回子类型 |
| **PendingRegistrationPromise** | register 异步 → doBind0 延迟到 register 完成 |
| **FailedChannel 降级** | register 失败时兜底 promise |
| **DNS 三级跳** | disableResolver→isResolved→resolve |

### 🟢 Surface
| KP | 放在哪 |
|----|-------|
| **BootstrapConfig 不可变快照** | 和 AbstractBootstrap 一起 |
| **ChannelInitializerExtension SPI** | 和 ChannelInitializer 一起 |
| **ReflectiveChannelFactory 反射** | 和 channel() 配置一起 |

---

## 03 聚类

### Cluster A: AbstractBootstrap 核心绑定 (7 KPs)
1. CRTP 泛型 + Fluent API(group/channel/handler/option/attr)
2. doBind 三步: validate→initAndRegister→doBind0
3. initAndRegister: channelFactory.newChannel→init→group().register
4. PendingRegistrationPromise: 异步等待
5. FailedChannel 降级
6. clone 深浅拷贝
7. AbstractBootstrapConfig 不可变快照

### Cluster B: 客户端+服务端绑定 (8 KPs)
1. Bootstrap.connect: resolve→doResolveAndConnect→doConnect
2. DNS 三级跳: disableResolver→isResolved→resolve
3. ServerBootstrap 父子 EventLoop 分离
4. childHandler/childOption/childAttr
5. ServerBootstrapAcceptor accept→init child channel 六步
6. childGroup 回退 parentGroup
7. ChannelInitializer 自毁 + initMap 防重入
8. ChannelInitializerExtension SPI 三级开关+回调

### 教学顺序: A → B
