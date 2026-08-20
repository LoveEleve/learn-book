# SCC-13 NamedContextFactory — 知识规划 (KP)

> 域: SCC-13 | 级别: 🔴 (Hub) | 方案: A | 大纲: outlines/scc13-named-context/outline.md (6 节)

## §01 域定位

NamedContextFactory = Spring Cloud 配置隔离的基石。双 Map (contexts/configurations) + 双检懒创建 + registerBeans 三段 (精确/default. 前缀/基础设施) + 含祖先查找。被 LoadBalancerClientFactory (SCC-6) 和 FeignClientFactory (5.6) 消费。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| named/NamedContextFactory.java | 274 | 子上下文工厂核心 | 1,2,3,4 |
| named/ClientFactoryObjectProvider.java | 120 | 延迟 ObjectProvider | 4 |
| 消费方: LoadBalancerClientFactory (SCC-6) | — | 每服务 LoadBalancer 隔离 | 5 |

## §05 闭环要点 (Pass 2 内化)

### q1 双 Map 懒创建
contexts/configurations 分离 + getContext 双检锁 (L119-126) + destroy 全清。

### q2 registerBeans 三段
精确匹配 (L147-150) + default. 前缀全局默认 (L152-154) + PropertyPlaceholder+defaultConfigType 基础设施 (L158)。

### q3 父环境复用
buildContext: propertySourceName 注入 (L185-187) + setParent (L189) + 类加载器用 parent (L169-173, issue 修复) + AOT 分支 (L174-181)。

### q4 多形态查找
getInstance 单类型/泛型/注解 + IncludingAncestors 祖先回退 + ClientFactoryObjectProvider 延迟。

## §06 负面空间 (6 条)

不做缓存淘汰 / 不做配置热更新 (H2 实证) / 不做跨上下文共享 / 不做子上下文通信 / 不做懒配置校验 / 不做动态增删客户端

## §07 交叉引用

- ← SCC-1 Bootstrap (父子上下文对照) + Spring 上下文
- → SCC-3 服务发现 + SCC-6 LoadBalancer (LoadBalancerClientFactory) + 5.6 OpenFeign (FeignClientFactory)
- 另见: Spring HierarchicalBeanFactory
