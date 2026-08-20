# Z-5 Session — completeness-questions (全视角提问验证)

## 开发者视角

1. 会话存在哪? (sessionsById CHM + ExpiryQueue 桶)
2. 过期时间怎么算? (roundToNextInterval 向上取整桶边界)
3. touch 干什么? (桶迁移续期)
4. 过期怎么清扫? (单线程循环 poll 桶 → setSessionClosing + expire)
5. sessionId 怎么生成? (时间戳+serverId 位结构)
6. 会话超时钳制? (min 2×tick / max 20×tick)
7. closing 状态? (过期中拒绝 touch)
8. 本地会话? (3.5+ follower 本地 + 升级)

## 架构师视角

9. 为什么分桶? (批量过期 — 单线程循环效率)
10. 宽限期意义? (向上取整 — 防临界抖动)
11. serverId 高 8 位? (跨服务器 id 唯一)
12. 单线程过期 vs 多桶? (无锁批量 — 复杂度 O(桶数))
13. 触摸语义? (任何请求 touch — 无专用心跳)
14. 对照 Redis? (惰性+定时 vs ZK 纯定时桶)
15. 会话迁移? (SessionMoved — 客户端重连他节点)
16. 本地会话动机? (follower 读面免全局广播)

## 学生视角

17. 什么是会话? (客户端与服务端的连接状态)
18. 什么是过期? (超时未活动 → 会话销毁)
19. 什么是触摸? (活动时刷新过期时间)
20. ephemeral 与会话关系? (会话过期 → 临时节点删除)
