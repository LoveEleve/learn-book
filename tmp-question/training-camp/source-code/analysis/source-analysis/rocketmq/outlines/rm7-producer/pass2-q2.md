# 闭环笔记 q2: 发送主链 — sendDefaultImpl 重试循环

## 假设
SYNC 重试 1+2 次; 异常分类故障更新; 超时逐次扣减。

## 验证过程
- **主链** (sendDefaultImpl L733-880):
  - timesTotal = **SYNC ? 1+retryTimes(2) : 1** (L760) — 异步/单向不重试 (调用方负责)
  - 重试循环: lastBrokerName 传递 → **resetIndex (重试时重置轮询索引)** (L766-768) → selectOneMessageQueue → **timeout 扣减** (costTime, L782-786) → sendKernelImpl
  - **异常分类故障更新** (L794-840):
    - MQClientException → updateFaultItem(false, true) + continue (重发)
    - **RemotingException → 隔离 (true, true)** 或 startDetectorEnable 时置不可达 (true, false) (L806-817)
    - **MQBrokerException → 不可达 (true, false)** + retryResponseCodes 集合判定: 可重试码 continue / 否则抛 (L824-839)
    - InterruptedException → 抛 (不重试)
  - **SEND_OK 检查**: retryAnotherBrokerWhenNotStoreOK → 存储未确认换 broker (L789-793)
- **brokersSent**: 记录已试 broker (避免重复)

## 代码类型
Algorithmic (重试编排)

## 跨域关联
- RM-1 (remoting): 三调用
- RM-6 (过滤): 无关

## 结论
重试编排 = 仅 SYNC (1+2); 异常三分类 (客户端/网络/broker) 决定故障更新维度与是否继续; 超时预算逐次扣减; retryResponseCodes 白名单控制 broker 异常重试。
源码位置: DefaultMQProducerImpl.java:733-880
