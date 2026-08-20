# G-4 Pass 1 扫描笔记 — 负载均衡

> 日期: 2026-08-16 | 版本: 1.83.1 | 🟡 B | 模块: api/LoadBalancer (1626) + PickFirst 族 (202/915) + InternalSubchannel (897) + AutoConfiguredLoadBalancerFactory (218) + util 全家族 (RoundRobin 182/MultiChild 414/OutlierDetection 1165/RandomSubsetting/GracefulSwitch) + services 对照 (HealthCheckingLoadBalancerFactory 511)

## 继承树/调用图

```
ManagedChannelImpl (G-3) exitIdleMode → loadBalancerFactory.newLoadBalancer(lbHelper)
  ├── AutoConfiguredLoadBalancerFactory (41, extends LoadBalancerProvider) — 默认策略选择
  │     ├── PickFirstLoadBalancer (40) → PickFirstLeafLoadBalancer (60) — 1.83 两代
  │     └── (按 service config / provider 注册表找策略)
LoadBalancer (111, api):
  ├── Helper (L1036, 抽象) — createSubchannel/updateBalancingState
  ├── SubchannelPicker (L453) — 调用时选 Subchannel
  ├── Factory (L1558)
InternalSubchannel (72) — 连接管理: start/requestConnection/地址更新/健康状态
util 家族: RoundRobinLoadBalancer (182)/MultiChildLoadBalancer (414, 基类)/OutlierDetection (1165)/RandomSubsetting/GracefulSwitch
services 对照: HealthCheckingLoadBalancerFactory (511)
```

## 基本元素分解

1. **LoadBalancer API**: Helper (回调能力) + SubchannelPicker (选址) + 状态上报 (updateBalancingState)
2. **AutoConfiguredLoadBalancerFactory**: 默认策略 (PickFirst) + provider 注册表委托
3. **PickFirst 两代**: PickFirstLoadBalancer (旧, 202) → PickFirstLeafLoadBalancer (新, 915, 双地址列表/端口复用?)
4. **InternalSubchannel**: 连接生命周期管理 (transports/地址变更/重连)
5. **util 家族**: RoundRobin/MultiChild 基类/OutlierDetection (离群熔断)/RandomSubsetting

## 标记问题 (6)

1. **Q1 LoadBalancer API 面**: Helper/SubchannelPicker/updateBalancingState 三方协作 — 状态怎么上报?调用时怎么选?
2. **Q2 AutoConfigured 默认策略**: defaultIsPickFirst (测试 L130) — 策略选择链 (service config → provider)?
3. **Q3 PickFirst 两代**: PickFirstLoadBalancer vs PickFirstLeafLoadBalancer 区别 (1.83 演进)?shuffle 测试 (L163) 是什么?
4. **Q4 InternalSubchannel**: 连接生命周期 (createSubchannel→start→地址更新→关闭) + 健康状态报告
5. **Q5 RoundRobin 与 MultiChild**: RoundRobin 182 行怎么实现轮询?MultiChild 基类 (414) 提供什么?
6. **Q6 OutlierDetection**: 离群检测 (1165) — EEDF 排分/熔断语义?

## 已读测试

- PickFirstLoadBalancerTest: pickAfterResolved (L139)/pickAfterResolved_shuffle (L163)/pickAfterResolved_noShuffle (L187)
- AutoConfiguredLoadBalancerFactoryTest: newLoadBalancer_isAuto (L123)/defaultIsPickFirst (L130)
