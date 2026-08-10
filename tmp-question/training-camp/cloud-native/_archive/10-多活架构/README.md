# 10 多活架构（Week 21-23 · 8课时）

## 学完能干什么
掌握异地多活从理论到各层落地的完整方案。训练营含金量最高的模块。

## ⭐ ZoneLocator 核心抽象（多活的灵魂）

**文件**: `cloud-native-code/stage-4/microsphere-multiactive/`

**ZoneLocator 接口**: `supports(Environment)` / `locate(Environment)`

**4 种实现（按 Order 优先级 5→20）**：

| 实现 | Order | 方式 |
|------|:-----:|------|
| EcsContainerMetadataFileZoneLocator | 5 | 读 ECS 元数据文件 JSON |
| EcsTaskMetadataEndpointV4ZoneLocator | 10 | HTTP GET ECS Task Endpoint v4 |
| Ec2AvailabilityZoneEndpointZoneLocator | 15 | HTTP GET EC2 元数据 API |
| DefaultZoneLocator | 20 | Spring 属性 `microsphere.availability.zone` |

**CompositeZoneLocator**: 组合模式，按 Order 遍历，第一个 `supports()=true` 返回 `locate()` 结果

**ZonePreferenceFilter 7 步过滤**:

| 步骤 | 条件 | 行为 |
|:----:|------|------|
| 1 | entities ≤ 1 | 直接返回 |
| 2 | enabled=false | 返回全部 |
| 3 | preferenceEnabled=false | 返回全部 |
| 4 | zone 空白/"default" | 忽略 |
| 5 | 有失效区域 | 过滤该区域 entities |
| 6 | 上游未就绪 | 返回全部 |
| 7 | 同区域匹配 | 返回同区域子集 |

## 📖 逐层多活

### 第 1-2 课：多活基础 + ZoneLocator

**读**: `stage-4/docs/01.` + `07.` + `08.`（全文）

**动手**:
```bash
cd cloud-native-code/stage-4/microsphere-multiactive && mvn compile
# 重点看: ZonePreferenceFilter.filter() 7步逻辑
# ZoneLocator 的 4 种平台实现
```

### 第 3 课：负载均衡 + RPC 多活

**LoadBalancer 同区域优先**: `ZonePreferenceServiceInstanceListSupplier` 继承 `DelegatingServiceInstanceListSupplier`
**Dubbo Router SPI**: 自定义 Router 读取 ZoneContext，优先路由同区域 Provider
**UnionDiscoveryClient**: 排除自身 + CompositeDiscoveryClient 后合并所有 DiscoveryClient 结果，`getServices()` 用 LinkedHashSet 去重

**动手**:
```bash
cd cloud-native-code/stage-4/microsphere-multiactive/microsphere-multiactive-spring-cloud
# 看 ZonePreferenceServiceInstanceListSupplier
# 看 CustomizedLoadBalancerClientConfiguration
```

### 第 4 课：Gateway + MySQL 多活

**Gateway 区域路由**：`WebEndpointMappingGlobalFilter` + `ServiceInstancePredicate`
**MySQL Multi-Host**: LOADBALANCE / FAILOVER / REPLICATION 三种 JDBC URL 模式
**Binlog 方案**: Alibaba Canal / Maxwell's Daemon / mysql-binlog-connector-java

### 第 5-6 课：Redis + 动态 JDBC 多活

**Redis 跨区域命令复制完整链路**（训练营经典设计）:
```
RedisTemplate.set(key, value)
  → InterceptingRedisConnectionInvocationHandler.invoke() 拦截
  → EventPublishingRedisCommandInterceptor.afterExecute()
  → publishEvent(RedisCommandEvent) Spring 事件
  → KafkaProducerRedisCommandEventListener → KafkaTemplate.send()
  → 异地 KafkaConsumerRedisReplicatorConfiguration 消费
  → RedisCommandReplicator 反射调用的目标方法
```

**DynamicJdbcConfig JSON 驱动切换**：创建独立 Spring 子上下文 → refresh → 原子切换 delegate → 优雅关闭旧上下文

### 第 7 课：Feign 动态刷新（新增）

**8 步数据流**: EnvironmentChangeEvent → FeignClientConfigurationChangedListener.resolveChangedClient() → FeignComponentRegistry.refresh() → DecoratedFeignComponent.refresh()（delegate=null）→ 下次调用从 NamedContextFactory 懒加载新实例

**动手**:
```bash
cd cloud-native-code/stage-4/microsphere-spring-cloud/microsphere-spring-cloud-openfeign
# 看 AutoRefreshCapability.enrich() → DecoratedRetryer/DecoratedEncoder 等
# 看 FeignComponentRegistry.refresh() 的配置键→组件类型映射
```

## 自检清单
- [ ] 能画 ZonePreferenceFilter 的 7 步过滤流程图
- [ ] 理解 Redis 跨区域命令复制的完整链路
- [ ] 知道 DynamicJdbcConfig 怎么实现运行时 DataSource 切换
- [ ] 能解释 Feign 动态刷新的 8 步数据流
- [ ] 理解 UnionDiscoveryClient 的合并逻辑
