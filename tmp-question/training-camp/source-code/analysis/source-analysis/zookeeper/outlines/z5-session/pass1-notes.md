# Z-5 Session — Pass 1 探索笔记

> 域: Z-5 Session | 🔴 A 方案 | 2026-08-15
> 源码: SessionTrackerImpl (359) + SessionTracker + ExpiryQueue + SessionImpl (内嵌) + ZooKeeperServer (超时钳制/本地会话) + LearnerSessionTracker | ZooKeeper 3.9.5

## 调用图

```
客户端请求 (Z-7) → ZooKeeperServer → Prep.checkSession (校验)
  → touchSession (有效) → updateSessionExpiry → ExpiryQueue.update (桶迁移)

过期面: SessionTrackerImpl.run (单线程循环)
  → getWaitTime → sleep → poll (到期桶)
  → setSessionClosing (isClosing) + expirer.expire (→ killSession → ephemeral 清扫 Z-3)

sessionId: createSession → nextSessionId.getAndIncrement (initializeNextSessionId: 时间戳+serverId 位结构)
```

## 基本元素分解

1. **结构**: sessionsById + ExpiryQueue (expiryMap 桶 + elemMap) + SessionImpl 三态
2. **分桶数学**: roundToNextInterval + update 迁移 + poll
3. **过期循环**: 单线程 + setSessionClosing + expirer.expire
4. **touch/check + id 生成**: touchSession/checkSession + initializeNextSessionId + 超时钳制 + 本地会话

## 标记问题 (20 问)

1. sessionsById 结构? (CHM)
2. ExpiryQueue 双 map? (expiryMap 桶 + elemMap)
3. roundToNextInterval? (向上取整)
4. update 迁移? (旧桶删除)
5. poll 语义? (到期桶)
6. 三态? (active/closing/expired)
7. touch 拒绝? (无效/closing)
8. 过期循环? (单线程 sleep-poll)
9. expire 动作? (expirer → killSession)
10. removeSession? (三处清理)
11. sessionId 位结构? (serverId<<56 + 时间)
12. CONTAINER 特值? (跳过)
13. 超时钳制? (2×tick/20×tick)
14. checkSession? (过期/移动异常)
15. checkGlobalSession? (未知→过期)
16. 本地会话? (localSessionEnabled)
17. 升级? (首次写)
18. LearnerSessionTracker? (follower 侧)
19. 宽限期? (向上取整余量)
20. 桶数上限? (maxTimeout/interval)

## 时空溯源 (代码内注释锚)

- L41-42: 分桶宽限期注释 (3.4 面)
- L104-106: CONTAINER_EPHEMERAL_OWNER 特值 (3.6 面)
- 本地会话: 3.5+ (KIP-xxx 本地会话特性)
- LearnerSessionTracker: 3.5+ 本地会话配套

## 大域拆分判断

Z-5 = SessionTrackerImpl + ExpiryQueue + 超时面; 单篇 🔴 A (8 闭环 q1-q4 + 验证); 会话协议 (Connect/Close 包) 归 Z-7

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划) | 验证 | 结论 |
|:--|:--|:--|
| "ExpiryQueue分桶+touchSession+SessionImpl三态" | 全实证 (ExpiryQueue L35-140 / touchSession L179-194 / SessionImpl L56-78) | **接受** ✅ |
| 数字: 超时钳制 | min=2×tick / max=20×tick (默认 -1 时) | **补充** ✅ |
| 数字: sessionId 位 | serverId<<56 + 时间戳<<24>>>8 | **补充** ✅ |
| 数字: 桶粒度 | tickTime (expirationInterval) | **补充** ✅ |
