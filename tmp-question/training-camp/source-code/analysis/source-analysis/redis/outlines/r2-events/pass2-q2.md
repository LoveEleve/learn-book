# 闭环笔记 q2: aeProcessEvents — 等待超时与主循环

## 假设
主循环 = 算等待时间 → beforesleep → poll → aftersleep → 分派 → 时间事件。

## 验证过程
- 入口 (ae.c:342-450):
  - 无任何事件类型 → 0 (L347)
  - **poll 条件** (L353-354): maxfd != -1 (有文件事件) 或 (TIME_EVENTS && !DONT_WAIT) — 只有时间事件也要 poll 来睡眠到点
- **三种等待** (L367-377):
  1. DONT_WAIT (参数或 eventLoop->flags, beforesleep 可改) → tv=0 立即返回
  2. TIME_EVENTS → usUntilEarliestTimer (L245-258, O(N) 扫链表) → tv=最早时间事件
  3. 否则 NULL → 无限等
- beforesleep (L359-360, AE_CALL_BEFORE_SLEEP 标志) → aeApiPoll (L380) → 无 FILE_EVENTS 时 numevents 清零 (L383-385) → aftersleep (L388-389)
- 返回 = 处理的文件+时间事件数 (L442,447)
- aeMain (L474-481): while(!stop) aeProcessEvents(AE_ALL_EVENTS|BEFORE|AFTER) — 唯一主循环
- processEventsWhileBlocked (networking.c:4169-4211): 4 次迭代 AE_FILE_EVENTS|DONT_WAIT — 阻塞中喂事件 (加载/脚本)
- aeSetDontWait (L90-95): beforesleep 内可全局改 flags

## 代码类型
Glue (主循环编排)

## 跨域关联
- R-20 (aeMain/initServer) / R-22 (beforeSleep 内 FAST 过期) / R-28 (networking 挂接)

## 结论
aeProcessEvents = **"睡眠到最早事件"的编排**: 有时间事件就 poll 到点, 没有就无限等; DONT_WAIT 由参数或 beforesleep 侧写。beforesleep/aftersleep 是每轮钩子 (R-20 的 AOF flush/FAST 过期都在 beforesleep)。
源码位置: ae.c:245-258,342-450,474-481; networking.c:4169-4211
