# 闭环笔记 q2: 分桶数学 — roundToNextInterval + 迁移

## 假设
过期时间向上取整到桶边界; touch 迁移桶。

## 验证过程
- **roundToNextInterval** (ExpiryQueue:53-55): `(time / expirationInterval + 1) * expirationInterval` — **向上取整** (now+timeout 落到下一桶边界)
- **update (touch)** (L84-103): newExpiryTime = roundToNextInterval(now + timeout) → **expiryMap.putIfAbsent(新桶, set) → set.add** → **elemMap.put(elem, newExpiryTime) 返回 prev → prevSet.remove (旧桶迁移)** (L94-100)
- **remove** (L63-74): elemMap.remove → 桶内 set.remove
- **poll** (L130+): nextExpirationTime 到期 → 取桶集合并推进 nextExpirationTime (+interval)
- **桶数上限注释** (L39-41): maxTimeout/expirationInterval

## 代码类型
Implementation (分桶 + 迁移)

## 跨域关联
- Z-3: 会话过期 → killSession (ephemeral 清扫)

## 结论
分桶 = roundToNextInterval 向上取整 (宽限期) + update 桶迁移 (O(1)) + poll 到期桶; 桶数有界。
源码位置: ExpiryQueue.java:39-55,63-103,130+
