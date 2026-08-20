# D-3 服务引用 — Pass 2 闭环 Q4: 代理生成与可用性面

> 入口: proxyFactory.getProxy / checkInvokerAvailable | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 消费者拿到的是什么样的代理? 调用时怎么进入 invoker? 启动检查怎么保证可用性?**

## 机制链 (已实证)

```
createProxy 收尾                              ReferenceConfig.java:489-523
├── publishServiceDefinition(consumerUrl, serviceModel)   ← 消费侧元数据发布 (D-11 钩子)
└── proxyFactory.getProxy(invoker, generic)   L522
    ├── proxyFactory = ExtensionLoader(ProxyFactory).getAdaptiveExtension()  L189 (D-1 自适应注入)
    ├── @SPI(value="javassist", scope=FRAMEWORK)   ProxyFactory.java:29
    ├── SPI 实现族 (internal/org.apache.dubbo.rpc.ProxyFactory):
    │   javassist=JavassistProxyFactory (默认) / jdk=JdkProxyFactory
    │   stub=StubProxyFactoryWrapper (Wrapper 织入, D-1) / nativestub=StubProxyFactory (3.x native stub)
    └── 代理处理器 = InvokerInvocationHandler   proxy/InvokerInvocationHandler.java:34-100
        ├── Object 方法特判: toString/$destroy/hashCode/equals
        ├── 业务方法 → RpcInvocation 构建:
        │   serviceModel + methodName + interface + protocolServiceKey + 参数类型 + args
        ├── ConsumerModel 挂载 (CONSUMER_MODEL/METHOD_MODEL)  — 3.x 方法模型
        └── InvocationUtil.invoke(invoker, rpcInvocation)  ← 进入调用链 = D-4 起点!

启动可用性检查                             ReferenceConfig.java:716-767
├── shouldCheck() 开关 (默认 true)
├── invoker.isAvailable() → 不可用则轮询等待 (deadline = startTime + timeout)
└── 超时仍不可用 → IllegalStateException "No provider available for the service"
```

## 关键设计 (why)

1. **代理 = 门面**: 用户拿到接口代理, 内部 handler 把方法调用翻译成 RpcInvocation — 接口调用与网络解耦
2. **javassist 默认 + jdk 备选 + wrapper 织入**: 代理工厂本身是 SPI (D-1 微内核自举) — proxyType URL 参数可切 (init 里 NATIVE_STUB 自动检测)
3. **Object 方法拦截**: 代理要"像"真实接口 (toString/hashCode/equals/$destroy) — 避免 Object 方法误走 RPC
4. **ConsumerModel 挂载 + METHOD_MODEL**: 3.x 在调用前就把方法模型绑定到 invocation — D-4 过滤器可用方法级元数据
5. **启动即检查**: check=true 时 get() 同步等待 provider 可用 — 失败快速失败 (fail fast)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| proxyFactory 自适应注入 | ReferenceConfig.java:189 |
| getProxy (generic 分支) | ReferenceConfig.java:522 |
| ProxyFactory @SPI 默认 javassist | dubbo-rpc-api ProxyFactory.java:29 |
| SPI 实现族 (jdk/javassist/stub/nativestub) | dubbo-rpc-api resources META-INF/dubbo/internal/org.apache.dubbo.rpc.ProxyFactory |
| InvokerInvocationHandler | dubbo-rpc-api proxy/InvokerInvocationHandler.java:34-100 |
| RpcInvocation 构建 + ConsumerModel 挂载 | 同上 L72-88 |
| InvocationUtil.invoke → D-4 桥 | 同上 L88 (桥接点!) |
| checkInvokerAvailable 轮询 | ReferenceConfig.java:716-767 |

## 负面空间 (Q4 面)

- 不做代理缓存复用 (每 ReferenceConfig 一个代理, destroy 后不可再 get)
- 不做非接口代理 (必须接口, 无接口代理模式 — JDK 代理限制)
- 不做调用级容错 (代理只做翻译; 容错在 ClusterInvoker, D-7)
