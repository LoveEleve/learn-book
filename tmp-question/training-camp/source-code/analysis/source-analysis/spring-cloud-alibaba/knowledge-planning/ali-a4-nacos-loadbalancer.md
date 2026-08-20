# ALI-A4 NacosLoadBalancer 权重 — 知识规划 (KP)

> 🟡 B | 模块: discovery/loadbalancer + balancer (8 文件, 610 行) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 算法插槽 | LoadBalancerAlgorithm (DEFAULT_SERVICE_ID) | 每服务可插拔算法 SPI |
| 2 | 默认权重随机 | DefaultLoadBalancerAlgorithm → NacosBalancer | LOWEST_PRECEDENCE 兜底 |
| 3 | 内核复用 | NacosBalancer:38 extends Balancer | nacos-client 权重随机零重写 |
| 4 | 元数据重建 | NacosBalancer:69-70 | nacos.weight/nacos.healthy 回读 |
| 5 | 空列表短路 | NacosLoadBalancer:136-139 | EmptyResponse + warn |
| 6 | cluster 隔离 | NacosLoadBalancer:142-155 | nacos.cluster 元数据匹配 |
| 7 | IPv6 双栈 | NacosLoadBalancer:86-112 | IPv6 优先 + IPv4 回退 |
| 8 | 过滤链 | NacosLoadBalancer:163-166 | ServiceInstanceFilter 按序 |
| 9 | 算法路由 | NacosLoadBalancer:170-176 | serviceId 查 Map, 无则默认 |
| 10 | 子上下文装配 | NacosLoadBalancerClientConfiguration:38-53 | LoadBalancerClientFactory + getLazyProvider |

## 02 高频坑

1. 权重在 metadata (nacos.weight) 不在 properties — 注册/选择两侧分离
2. clusterName 空会 warn 跨集群调用
3. 算法 Map putIfAbsent — 同 serviceId 先注册生效
4. catch 返回 null 不是 EmptyResponse
5. IPv6 需要实例 metadata 带 "IPv6" 键
6. 选择器在每服务子上下文 — 状态隔离

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 契约 | ReactorServiceInstanceLoadBalancer (SCC-7) / Response 族 (Default/Empty) |
| 策略 | LoadBalancerAlgorithm SPI / DEFAULT 兜底 / putIfAbsent 注册 |
| 过滤 | cluster 元数据 / IP 类型 / ServiceInstanceFilter 链 |
| 转换 | ServiceInstance↔Instance 双向 / 元数据桥接 / IPv6 替换 |
| 复用 | 继承 nacos-client Balancer / SCC-6 Supplier 链 / SCC-13 lazy provider |

## 04 跨域桥接

- ← ALI-A3: nacos.weight/healthy/cluster 元数据是 hostToServiceInstance 写的 (消费方实证)
- ← SCC-7/SCC-13: ReactorLoadBalancer 契约 + 子上下文装配
- → Nacos 5.8: Balancer.getHostByRandomWeight 内核归属
- → 面试: "Nacos 权重负载均衡怎么实现" — 元数据 + 继承内核 + 插槽
