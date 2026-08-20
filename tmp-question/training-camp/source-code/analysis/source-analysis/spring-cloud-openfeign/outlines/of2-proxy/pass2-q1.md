# OF-2 代理创建与装配 — Pass 2 闭环 Q1: 入口与双路径

> 核心: getTarget 两条路径 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: BeanDefinition 触发后, 代理怎么创建? 有 url 和没 url 差在哪?**

## 机制链 (已实证)

```
getObject (L455) → getTarget (L465):
├── feignClientFactory = beanFactory.getBean(FeignClientFactory.class) — 子上下文工厂 (OF-7)
├── builder = feign(feignClientFactory)
├── 双路径:
│   ├── 路径 1: 无 url (L468-482):
│   │   ├── url = "http://" + name + cleanPath()
│   │   └── loadBalance(builder, factory, new HardCodedTarget(type, name, url)) (L427):
│   │       ├── client = getOptional(Client.class)
│   │       ├── client 存在 → builder.client + customizers + targeter.target
│   │       └── **client null → IllegalStateException
│   │           "Did you forget to include spring-cloud-starter-loadbalancer?"** (L449-451)
│   │           ← 生产陷阱: 无 LoadBalancer 依赖时启动即报错
│   └── 路径 2: 有 url (L484-500):
│       ├── url 前缀补全 (无 http:// 时)
│       ├── client = getOptional(Client.class)
│       ├── FeignBlockingLoadBalancerClient → unwrap:
│       │   └── ((RetryableFeignBlockingLoadBalancerClient) client).getDelegate() (L495-497)
│       │       ← "not load balancing because we have a url" — 有 url 时剥掉 LB 装饰
│       └── builder.client(client)
├── applyBuildCustomizers (L501)
└── targeter.target(this, builder, factory, resolveTarget(...)) (L502-503)
    ← Targeter 分支点 (OF-6 熔断装饰)
```

## 关键设计 (why)

1. **双路径 = 服务发现 vs 直连**: 无 url → loadBalance (从注册中心选实例); 有 url → 直连 (unwrap 剥 LB)
2. **生产陷阱显式化**: loadBalance 无 Client → IllegalStateException 明确提示缺 starter-loadbalancer — 配置错误早暴露
3. **unwrap 语义**: 有 url 时 FeignBlockingLoadBalancerClient 装饰无意义 → getDelegate 还原底层 Client
4. **Targeter 注入点**: targeter.target 是熔断装饰 (OF-6) 的接入点 — 代理创建与容错解耦

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| getObject/getTarget | FeignClientFactoryBean.java:455,465 |
| 路径 1 loadBalance + 生产陷阱 | FeignClientFactoryBean.java:427-451 |
| 路径 2 unwrap | FeignClientFactoryBean.java:484-500 |
| targeter.target 分支点 | FeignClientFactoryBean.java:502-503 |

## 负面空间 (Q1 面)

- 不做代理缓存 (每次 getObject 重建)
- 不做连接池预建 (Client 惰性)
- 不自动探测 url 合法性 (前缀补全后直连)
