# D-3 服务引用 — Pass 2 闭环 Q1: 引用装配面 (配置→URL→invoker 双路径)

> 入口: ReferenceConfig.get(boolean check) | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 消费者配置怎么变成可调用的 invoker? 直连和注册中心两条路分别怎么走?**

## 机制链 (已实证)

```
get(boolean check)                          ReferenceConfig.java:228-248
├── destroyed → IllegalStateException       (引用销毁后不可用)
├── Deployer.prepare()/start()              (3.x 生命周期: Spring 托管 vs 兼容旧 API)
└── init(check)                             L332-404
    ├── lock 双检锁 (initialized && ref != null)
    ├── refresh()                           3.x 配置刷新 (AbstractConfig.java:718, dubbo-common!)
    ├── NATIVE_STUB 自动检测 (DubboStub 继承)  — 3.x 原生 stub 面
    ├── ServiceRepository.registerService → ServiceDescriptor + ConsumerModel
    ├── appendConfig() → referenceParameters  L431
    ├── ref = createProxy(referenceParameters)  L489-523
    │   ├── 双路径:
    │   │   ├── url 非空 → parseUrl (L605, 直连 peer-to-peer / 注册中心地址)
    │   │   └── 否则 → aggregateUrlFromRegistry (L633, 注册中心聚合)  ← 主流
    │   │       ├── ConfigValidationUtils.loadRegistries (注册中心配置→URL)
    │   │       ├── monitorUrl 附加 + injvm 标记 + REFER_KEY 参数绑定
    │   │       └── urls 空 && shouldJvmRefer (L854) → injvm URL 兜底 (localhost:0)
    │   │           ← 本地始终可引 (与 D-2 本地始终可导对称!)
    │   ├── createInvoker()  L668-714 (见 q3)
    │   └── proxyFactory.getProxy(invoker, generic)  (见 q4)
    └── check → checkInvokerAvailable(0)    L716-767
        ├── shouldCheck() 开关
        ├── invoker.isAvailable() 轮询等待 (deadline)
        └── 不可用 → IllegalStateException "No provider available for the service"
```

## 关键设计 (why)

1. **双路径设计**: 直连 (url 参数) 用于调试/点对点; 注册中心路径是生产主流 — 配置只需注册中心地址, provider 动态发现
2. **injvm 兜底**: 本地始终可引, 与 D-2 本地始终可导组成对称设计 — 同 JVM 内部引用不走网络
3. **双检锁 + initialized 标志**: get 可重入, 只初始化一次 (与 D-2 export 的懒加载对称)
4. **checkInvokerAvailable 启动即检查**: 失败快速失败 (fail fast), 避免运行时才发现无 provider
5. **3.x ConsumerModel/ServiceDescriptor**: 消费侧也有完整服务模型注册 (服务自描述面, D-11 关联)

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| get 入口 + destroyed 检查 | ReferenceConfig.java:228-248 |
| init 9 步 | ReferenceConfig.java:332-404 |
| appendConfig | ReferenceConfig.java:431 |
| createProxy 双路径 | ReferenceConfig.java:489-523 |
| parseUrl (直连) | ReferenceConfig.java:605 |
| aggregateUrlFromRegistry (注册中心) | ReferenceConfig.java:633-663 |
| createInvoker 三分支 | ReferenceConfig.java:668-714 |
| shouldJvmRefer (injvm 兜底) | ReferenceConfig.java:854 |
| checkInvokerAvailable | ReferenceConfig.java:716-767 |
| refresh (3.x 配置刷新) | dubbo-common AbstractConfig.java:718 |

## 负面空间 (Q1 面)

- 不做异步引用 (get 同步返回, 检查同步轮询)
- 不做引用热更新 (initialized 后不变, 销毁需重建)
- 不缓存 invoker 于全局 (每 ReferenceConfig 一个)
