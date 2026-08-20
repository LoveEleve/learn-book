# R-20 server 骨架 — completeness-questions

## 开发者视角

1. redis-server 从执行到监听端口, 经历了哪些阶段?
2. 为什么哨兵要先于配置文件解析?
3. initServer 都初始化了什么?键空间为什么分片?
4. serverCron 每 tick 做什么?什么任务 5 秒一次?
5. 客户端多了 serverCron 会变快吗?hz 上限多少?
6. 122 个命令的定义在哪里?CONFIG SET 失败会怎样?
7. beforeSleep 和 serverCron 的分工?
8. watchdog 怎么检测卡死?

## 架构师视角

9. main 的依赖序 (OOM→seed→哨兵→配置→initServer) — 每步为什么必须在前?
10. kvstore 键空间分片 (cluster 14bit) — 设计动机?与单 dict 的差异?
11. 时间分级 (每 tick/100ms/1s/5s) — 任务的频度依据?
12. hz 自适应配额 (200/clock tick) — 为什么翻倍而非线性?上限 500 的依据?
13. 生成式命令表 (commands.def) — 单一来源的价值?双字典的 rename 免疫?
14. beforeSleep 的 AOF flush 时机 — 为什么必须在事件处理前?
15. 配置宏 DSL — 五合一注册的工程收益?回滚机制?
16. watchdog 与信号处理的配合?

## 学生视角

17. serverCron 的一次完整执行: 从 aeCreateTimeEvent 到返回?
18. hz=10 时 run_with_period(1000) 多久触发一次?
19. 命令从 commands.def 到 lookupCommand 的路径?
20. 启动顺序里, 共享对象和事件循环谁先?为什么?
