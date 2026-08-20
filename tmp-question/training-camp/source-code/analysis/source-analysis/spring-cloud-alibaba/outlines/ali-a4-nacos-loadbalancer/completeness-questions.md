# ALI-A4 NacosLoadBalancer 权重 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. LoadBalancerAlgorithm 的 serviceId 与 DEFAULT_SERVICE_ID 如何路由到具体算法?
2. 权重随机为什么采用"继承 nacos-client Balancer"而非重新实现?
3. 选择流水线的过滤顺序 (cluster→IP→用户) 为什么这样排? 换序影响?
4. clusterName 为空时为什么 warn "A cross-cluster call occurs"?
5. IPv6 双栈的"回退"逻辑在什么条件下触发?

## B. 源码实证 (6)

6. DEFAULT_SERVICE_ID 的值? (grep LoadBalancerAlgorithm)
7. getHostByRandomWeight3 从 metadata 读哪两个键? (grep NacosBalancer:69-70)
8. 算法 Map 的 putIfAbsent 语义在哪? (grep NacosLoadBalancerClientConfiguration)
9. cluster 过滤用什么元数据键? (grep NacosLoadBalancer:149)
10. 空列表返回什么 Response? (grep L136-139)
11. NacosLoadBalancer 实现自哪个接口? (grep L51)

## C. 推理深挖 (5)

12. 两个自定义算法 Bean 声明同一 serviceId 会怎样? putIfAbsent 后谁生效?
13. 权重从哪来? nacos.weight 谁写入? (回链 A3 hostToServiceInstance)
14. filterInstanceByIpType 中 "Provider has no IPv6, should use IPv4" 分支什么条件?
15. DefaultResponse vs EmptyResponse 对调用方 (BlockingLoadBalancerClient) 的语义差异?
16. 为什么 getInstanceResponse 的 catch 返回 null 而非 EmptyResponse? 调用方怎么处理 null?

## D. 跨域扩展 (4)

17. NacosLoadBalancer vs SCC-7 RoundRobinLoadBalancer: 结构上的同构与差异?
18. 本域的算法插槽 vs SCC-12 的六种扩展策略: 定位差异?
19. getLazyProvider vs SCC-13 ClientFactoryObjectProvider: 懒加载语义?
20. nacos-client 的 Balancer.getHostByRandomWeight 内核 vs Nacos 5.8 域的权重计算: 归属?
