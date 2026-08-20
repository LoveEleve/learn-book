# 闭环笔记 q3: 死阈值数学 — 70s/10s + 动态延迟

## 假设
重试有双阈值: MAX_RETRY_TIMEOUT (默认 -1 永不) + RETRY_DEAD_THRESHOLD (70s/10s); 调度频率动态。

## 验证过程
- **阈值定义** (DefaultValues:383-390): **RETRY_DEAD_THRESHOLD = 70s** / **END_STATE_RETRY_DEAD_THRESHOLD = 10s**
- **timeToDeadSession** (GlobalSession:222-228, S-1 实证): **active 会话 70s / 已结束 (active=false) 10s** — 剩余 = 阈值 - (now - beginTime)
- **isRetryTimeout** (DefaultCoordinator:572-574): **timeout >= 0 (ALWAYS_RETRY_BOUNDARY=0) && now - beginTime > timeout** — **MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 默认 -1L → 永不超时** (S-3 实证) — 重试终止靠 dead 阈值
- **动态延迟** (S-3 实证): 无会话 → **70s**; 有 → 排序后首个 timeToDeadSession>0 驱动 **delay = max(剩余, period)** — **最早到期会话决定下次调度**
- **调度链**: rollbackingSchedule/committingSchedule/endSchedule 递归自调度 (syncProcessing) + distributedLock 防重 (S-3)
- **END 10s 语义**: 终态会话 (Committed/Rollbacked/...) 快速清理 (10s 内) — 防止终态滞留

## 代码类型
Implementation (阈值数学)

## 跨域关联
- S-1: timeToDeadSession/clean (本域消费)
- S-3: 调度面 (本域是数学面)
- S-6: 与钩子无关 (纯服务端)

## 结论
双阈值: MAX=-1 永不超时 (靠 dead) + 70s/10s 动态死线; 调度频率由最早到期会话驱动。
源码位置: DefaultValues.java:383-390; GlobalSession.java:222-228; DefaultCoordinator.java:572-574
