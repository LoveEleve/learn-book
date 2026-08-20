# 闭环笔记 q3: serverCron 时间分级 — run_with_period 100ms/1s/5s

## 假设
serverCron 每 hz (默认 10/s) 执行, 任务按 run_with_period 分级: 每 tick (高频) / 100ms / 1s / 5s — 昂贵任务降频。

## 验证过程
- 每 tick (server.c:1273-1300): watchdog 检查 → hz 自适应 → lruclock 更新 → cronUpdateMemoryStats → clientsCron → databasesCron (R-22 主动过期) → updateDictResizePolicy (R-3 COW) → updatePausedActions → stopThreadedIOIfNeeded
- run_with_period(100) (L1303): 时间缓存更新等
- run_with_period(5000) (L1361,1377): 统计日志 (内存/客户端数)
- run_with_period(1000) (L1404): receiveChildInfo (子进程 COW 报告, R-8)
- RDB 检查 (每 tick): dirty >= changes → BGSAVE; AOF 重写触发
- run_with_period(1000) (L1462): tracking 表 resize (R-17)
- replicationCron (L1479-1481): 100ms (正常) / 1000ms (无从)
- run_with_period(100) (L1485): 内存抽样 (maxmemory 预估)
- BGSAVE scheduled (L1513+)
- modulesCron (100ms)
- **返回 1000/server.hz** (L1538) — 下一次调度的毫秒数 (hz=10 → 100ms)
- watchdog (L1279): watchdog_period 时 scheduleSignal — 卡死检测

## 代码类型
Algorithmic (时间分级调度) — 周期引擎

## 跨域关联
- R-22 (databasesCron 主动过期) → 每 tick
- R-8 (receiveChildInfo) → 1s
- R-17 (tracking resize) / R-14 (sentinel) / R-15 (clusterCron) → 各频
- R-2 (aeCreateTimeEvent) → 宿主

## 结论
时间分级: 每 tick 是"必须快"的任务 (过期/COW 策略), 100ms 是常规轮询 (复制/模块), 1s 是统计/COW 报告, 5s 是日志。返回 1000/hz 控制下一轮间隔 — 频率可调 (hz 1-500)。
源码位置: server.c:1273-1540
