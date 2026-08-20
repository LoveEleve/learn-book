# 闭环笔记 q7: RSS 统计面 — /proc 解析与 fork CoW 评估

## 假设
zmalloc_get_rss 多平台实现 (Linux /proc/self/stat / macOS kvm / 兜底 used_memory), private_dirty 解析 smaps 用于 fork CoW 报告 (R-8 持久化依赖)。

## 验证过程
- get_proc_stat_ll (zmalloc.c:496-531): 打开 /proc/self/stat → strrchr(')') 跳过 pid+进程名 → 按空格跳字段 → 取第 i 字段 (RSS=24)
- zmalloc_get_rss (L533-646 多分支): Linux /proc/self/stat → page*RSS; macOS kvm_getprocs; 其他 → **fallback: 返回 used_memory** (碎片率恒 1, 注释明说)
- WARNING 注释 (L472-476): "not designed to be fast" — 慢速精确 vs RedisEstimateRSS (快速估算, server.c)
- **private_dirty (fork CoW 评估)**: smaps 解析 (L700+), 消费方 childinfo.c:70 — `cow = zmalloc_get_private_dirty(-1)` → "Fork CoW for %s: current %zu MB..." 日志 + peak/average 统计, 带 CHILD_COW_DUTY_CYCLE 节流 (读数昂贵)
- jemalloc 统计面: mallctl 查询 allocated/active/resident/retained/muzzy (L640-1004) — INFO memory 的 allocator_* 字段; frag_smallbins (小 bin 碎片逐 bin 计算, R-18 关联)

## 代码类型
Implementation (平台抽象) + Algorithmic (smaps 解析)

## 跨域关联
- R-8 (RDB/AOF fork COW 报告) → childinfo.c 是持久化子进程的内存报告面
- R-18 (defrag 碎片) → frag_smallbins 是碎片数据源
- R-20 (INFO memory) → 全部统计字段的展示端
- R-23 (evict 内存状态) → used_memory/rss 度量

## 结论
统计面分层: used_memory (快, 原子记账) / RSS (慢, /proc 解析, 有快速估算替代) / private_dirty (更慢, smaps, 专供 fork CoW 报告且节流) / jemalloc mallctl (分配器内部视角)。每层服务不同消费方, 性能与精度取舍显式化。
源码位置: zmalloc.c:496-646,700+; childinfo.c:60-80
