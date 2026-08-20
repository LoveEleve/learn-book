# 闭环笔记 q4: 时间事件 — 无序链表与周期重排

## 假设
时间事件 = 无序单链表 (O(N) 找最早), 递归安全 (refcount), 迭代安全 (maxId), 返回 ms 重排 when。

## 验证过程
- 注册 (ae.c:200-221): nextId 递增 (L204) → when = getMonotonicUs() + ms×1000 (L210) → 头插 (L215-219)
- 删除 (L223-234): 标记 AE_DELETED_EVENT_ID 惰性删 (L228) — 不立即摘链 (可能正在执行)
- **usUntilEarliestTimer** (L245-258): O(N) 扫全部找最早 (注释 L239-244: 有序插入/跳表是可选优化, Redis 不需要)
- **processTimeEvents** (L261-325):
  - maxId = timeEventNextId-1 (L267) — 本次迭代前创建的才处理 (L302-305, 防事件内新注册被本迭代处理)
  - 惰性删 (L273-295): refcount 非 0 不释放 (L278-281, 递归调用中)
  - 到期 (L307-321): refcount++ 保护 (L311-313) → timeProc 调用 → 返回 AE_NOMORE (-1) → 标记删除; 否则 when += retval×1000 (L316-317, 周期重排)
- 单调时钟 getMonotonicUs (monotonic.h) — 不受系统时间跳变影响 (NTP)
- serverCron 注册 (server.c:2757): aeCreateTimeEvent(1, serverCron) — 每 1ms 检查, 回调返回 1000/hz 重排 (R-20 已交付)

## 代码类型
Mechanism (定时器管理)

## 跨域关联
- R-20 (serverCron 周期) / R-23 (evictionTimeProc) / R-2 (io threads 无关)

## 结论
时间事件 = 无序链表 + 惰性删除 + 双重防重入 (maxId 防新事件, refcount 防递归释放)。周期事件靠回调返回 ms 重排 when — 比有序结构简单, 事件数少时 O(N) 可接受 (注释明示权衡)。
源码位置: ae.c:200-325
