# 闭环笔记 q8: 就绪/监督/watchdog — 生产面

## 假设
启动尾部是生产就绪面: systemd 通知 (READY) / CPU 亲和 / OOM score; 运行期 watchdog 是"卡死检测"。

## 验证过程
- 就绪通知 (server.c:7230-7240): `redisCommunicateSystemd("READY=1\n")` — systemd 集成 (supervised_mode SYSTEMD); 从节点 "Waiting for MASTER <-> REPLICA sync"
- CPU 亲和 (L7250): `redisSetCpuAffinity(server.server_cpulist)` (L6783)
- OOM score (L7251): `setOOMScoreAdj(-1)` (L2232) — oom_score_adj 调整 (子进程用 CONFIG_OOM_BGCHILD, L6490)
- **watchdog** (L1281): `if (server.watchdog_period) watchdogScheduleSignal(...)` — serverCron 每 tick 检查; 卡死检测原理: 事件循环卡住 → cron 不来 → SIGALRM 由 OS 递达 → 信号处理器 dump 栈 (watchdog_period 配置 ms)
- 信号 (initServer L2593-2596): SIGHUP/SIGPIPE 忽略 + setupSignalHandlers (SIGTERM/SIGINT → 优雅关闭流程)
- maxmemory 警告 (L7245-7247): <1MB 配置提示

## 代码类型
Glue (生产运维面) — 部署集成

## 跨域关联
- R-8 (子进程 OOM score) → 后台面
- R-23 (maxmemory) → 警告面
- 运维面: systemd/cgroup/OOM killer 协作

## 结论
生产面 = 就绪协议 (systemd READY) + 资源策略 (CPU 亲和/OOM score) + 卡死检测 (watchdog: cron 缺席 → SIGALRM → 栈 dump)。main 尾部把"服务可用"的契约交给运维体系。
源码位置: server.c:1281,2232,6783,7230-7251
