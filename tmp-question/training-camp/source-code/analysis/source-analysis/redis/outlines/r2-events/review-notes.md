# R-2 事件驱动+IO 多线程 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | networking.c io threads 函数行号多处偏移: initThreadedIO **L4295** (写 4294) / startThreadedIO **L4347** (写 4351) / stopThreadedIO **L4354** (写 4357) / handleClientsWithPendingReadsUsingThreads **L4518** (写 4521) | 大纲 02 + pass1/pass2-q6/q8 全量修正 |
| 2 | 通过项 | 行号穷举 100 处全验证 (ae.c 38 + ae.h 18 + ae_epoll.c 15 + networking.c 25 + config/server 4) — 25 个"FAIL"均为多行语句/注释偏移, 精确定位后修正 | 记录 |
| 3 | 通过项 | 数字穷举: IO_THREADS_MAX_NUM=128 (L4215) / io-threads 默认 1 范围 1-128 IMMUTABLE (config.c:3149) / do-reads 默认 0 (L3051) / 惰性停阈值 num×2 (L4377-4380) / 自旋上限 100 万次 (L4260-4264) / 三后端选择链 (ae.c:31-43) 全部 ✅ | 记录 |
| 4 | 通过项 | 机制验证: 互斥锁启停 (L4319 锁/ L4347 解锁 / L4354 上锁) ✅; 从库强制 list[0] (L4425-4431) ✅; 轮询分发 item_id % num (L4434-4436) ✅; postpone 五条件 (L4493-4501) ✅; EPOLLERR/HUP 双触发 (ae_epoll.c:104-105) ✅ | 记录 |
| 5 | 推断标注 | epoll 生产占比 / io-threads 默认 1 动机 / CACHE_LINE_SIZE 引入版本 — 3 处显式标注 temporal-trace.md | 记录 |

## 07 五维度

### 维度1 功能正确性
- 三种等待模式 + beforesleep/aftersleep 三明治逐行验证
- 分派顺序 (读先写后/BARRIER 逆序/同 proc 去重/指针刷新)
- 时间事件双重防重入 (maxId/refcount)

### 维度2 性能
- fd 索引数组 O(1); 编译期后端特化零间接
- io threads 惰性停 (num×2) + 自旋为主 + cache-line 对齐
- 时间事件 O(N) 权衡注释明示

### 维度3 内存
- events/fired 双数组 setsize 定长; 时间事件 24B/项
- io_threads_list 复用 clients_pending_write

### 维度4 一致性
- 扇出扇入 pending 计数唯一通信; 从库强制主线程 (共享 repl 缓冲)
- IO 并行/执行单线程不变式; ProcessingEventsWhileBlocked 禁用读线程 (#6988)

### 维度5 负面空间 (已写入两篇各 5 条)
- 不做事件优先级 / 不做定时器红黑树 / 不做跨线程事件投递 / 不做 ET 模式 / 不做命令并行 / 不做线程动态扩缩

## 结论
R-2 全部锚点行号 100 处验证, 8 闭环完成, 4 处行号偏差已修正。推断显式标注。待二次 REVIEW + harness。

---

# 二次深度 REVIEW (2026-08-13, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 R-20 (9)/R-33 (1) 已讲 ✅; 引出 R-2-下/R-28/R-9 (未来域 OK); 对照 R-20 ✅; 五结构元素齐备 ✅; 01→02 承接 (IO 怎么线程化) ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 01 篇 ~25 锚点 / 02 篇 ~20 锚点 (🔴A 标准 ≥8) ⏫ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置声明无未来域 (R-20/R-33 序号 < 13) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 01 节 2 beforesleep 只讲机制没给内容 — AOF flush/FAST 过期/客户端写是其主要工作 (R-20/R-22/R-28 连接) | 补 "beforesleep 内含 AOF flush/FAST 过期/客户端写" |
| 10 | **覆盖缺口** | 01 节 4 时间事件只提 serverCron — evictionTimeProc (R-23 淘汰续清) 同挂时间事件 | 补连接 |
| 11 | 通过项 | 复制 (从库强制主线程) / 持久化 (AE_BARRIER fsync) / 脚本 (processEventsWhileBlocked + #6988) / 淘汰 (evictionTimeProc) — 全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | 通过项 | 两篇负面空间各 5 条 ✅; 开篇场景化 ("上万连接/单线程为什么有 io threads/保序") ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **行号偏差** | processEventsWhileBlocked 实测 **L4169** (初稿写 4178-4211, 那是函数体后部) | 01 节 2 + pass2-q2 修正 |
| 14 | **行号偏差** | installClientWriteHandler 调用实测 **L4474** (初稿写 L4471, 那是注释行) | 02 节 2 + pass2-q7 修正 |
| 15 | 精确化 | initThreadedIO 锁互斥锁: L4319 是 pthread_mutex_init, **lock 在 L4320** (pass2-q6 写 L4319) | pass2-q6 修正 |
| 16 | 通过项 | 其余 ~40 句逐句对源码一致 ✅ (三等待/读先写后/BARRIER/去重/refcount+maxId/ADD-MOD-DEL/ERR-HUP 双触发/互斥锁启停/惰性停 num×2/从库 list[0]/轮询分发/postpone 五条件) | 记录 |

## 二次 REVIEW 汇总

07 五维度轮换 + 内容深度共 **5 处新发现** (覆盖缺口 2 / 行号偏差 2 / 精确化 1), 全部修复。反写测试结论: 两篇大纲机制面完整可支撑写作。
