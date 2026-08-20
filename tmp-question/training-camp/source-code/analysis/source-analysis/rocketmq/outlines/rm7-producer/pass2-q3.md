# 闭环笔记 q3: 故障策略 — 7 档映射 + 三级过滤

## 假设
延迟→不可用时间分段映射; 队列选择按可用性过滤。

## 验证过程
- **7 档映射** (MQFaultStrategy:29-30): latencyMax {50,100,550,1800,3000,5000,15000}ms → notAvailableDuration {0,0,2000,5000,6000,10000,30000}ms — **延迟越高, broker 冷却越久**
- **computeNotAvailableDuration** (L180-187): 从高到低找 ≥ 档位 → 对应冷却; 无匹配 → 0
- **updateFaultItem** (L173-178): **isolation (发送失败/网络异常) → 固定 10000ms** (L174); 普通延迟 → 映射冷却; reachable 维度 (5.x 双维度: 延迟冷却 + 可达性)
- **selectOneMessageQueue** (L145-170): sendLatencyFaultEnable 时**三级过滤**:
  1. **availableFilter** (可用+可达) → 2. **reachableFilter** (仅可达 — 冷却中也可试) → 3. 兜底 (全队列)
  - 关闭时单 brokerFilter (排除上次 broker)
- **startDetector** (5.x): ServiceDetector 服务探测 (定时探活, reachableFlag 恢复 L73-75)

## 代码类型
Algorithmic (故障策略)

## 跨域关联
- RM-8 (消费): 无关
- RM-12 (HA): broker 故障面

## 结论
故障策略 = 延迟分段冷却 (7 档) + isolation 固定 10s + 双维度 (延迟/可达); 选择三级降级 (可用→可达→兜底); 5.x 服务探测自动恢复可达。
源码位置: MQFaultStrategy.java:29-30,145-187; LatencyFaultToleranceImpl.java:36-126
