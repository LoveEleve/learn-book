# ALI-A3 Nacos 服务发现+注册 — 知识规划 (KP)

> 🔴 A | 模块: spring-cloud-starter-alibaba-nacos-discovery (37) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 发现双通道 | NacosDiscoveryClient:59-75 | 成功写穿缓存 / 失败容错 or 抛异常 |
| 2 | 不对称失败 | NacosDiscoveryClient:77-90 | getServices 失败默认空列表不抛 |
| 3 | 双段过滤 | NacosServiceDiscovery:58/89 | 服务端 selectInstances(true) + 客户端 enabled/healthy |
| 4 | 六键元数据 | NacosServiceDiscovery:98-106 | nacos.* 命名空间 + 用户 putAll |
| 5 | 注册仪式 | NacosServiceRegistry:59-89 | registerInstance + failFast 双分支 |
| 6 | 状态翻转 | NacosServiceRegistry:127-154 | UP/DOWN = enabled 翻转重注册 |
| 7 | 状态反查 | NacosServiceRegistry:156-175 | getAllInstances ip+port 双匹配 |
| 8 | 端口仲裁 | NacosAutoServiceRegistration:56-61 | port<0 用 WebServer 端口 + Assert |
| 9 | 写穿缓存 | ServiceCache:55-67 | unmodifiableList + 缺 key 空 |
| 10 | 命名服务单例 | NacosServiceManager:86-95 | volatile 双检 + shutDown 复位 |

## 02 高频坑

1. failure-tolerance-enabled 默认 false — 不开容错 Nacos 挂了直接抛
2. getInstances 抛异常 vs getServices 返回空 — 不对称
3. 缓存不实时 — "depends on getServices/getInstances invoke"
4. setStatus 只认 UP/DOWN — 其他状态 warn 忽略
5. getManagementRegistration=null — Nacos 不注册管理实例
6. 元数据 secure 键控制协议 (http/https)
7. NamingService 可 shutDown 重建, 非静态

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| SPI 实现 | DiscoveryClient 四方法 / ServiceRegistry 五方法 (SCC-3/4) |
| 容错 | failureToleranceEnabled / ServiceCache 写穿 / emptyList 兜底 |
| 转换 | Instance→ServiceInstance / 元数据六键 / secure 回读 |
| 状态机 | UP/DOWN 翻转 / enabled 标志 / ip+port 反查 |
| 并发 | ConcurrentHashMap / volatile 双检 / unmodifiableList |
| 生命周期 | WebServer 端口仲裁 / @EventListener restart / shutDown |

## 04 跨域桥接

- ← SCC-3/SCC-4: 契约实现方实证 (DiscoveryClient/ServiceRegistry SPI 落地)
- → ALI-A4: selectInstances 结果被 NacosLoadBalancer 消费
- → ALI-A5: NacosWatch 订阅与缓存更新 / HeartBeatPublisher
- → Nacos 5.8: namingService 客户端面 (selectInstances/registerInstance 内核)
- → 面试: "Nacos 服务发现/注册怎么实现" — 双通道 + 仪式链 + 状态语义
