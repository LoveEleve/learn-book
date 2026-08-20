# 闭环笔记 q8: 复用三件 — sdsclear / SDS_NOINIT / 导出分配器

## 假设
三个小机制共同服务"避免重复分配": sdsclear 清空留缓冲 (AOF 复用), SDS_NOINIT 免 memset (大缓冲), 导出分配器让宿主 (Lua) 与 SDS 分配器互通。

## 验证过程
- **sdsclear (sds.c:200-203)**: `sdssetlen(s, 0); s[0]='\0'` — **len 归零但 alloc 不动** — 注释 L196-199: "all the existing buffer is not discarded but set as free space so that next append operations will not require allocations"
  - 消费: aof.c:1216 `sdsclear(server.aof_buf)` — **AOF 写缓冲每轮清空复用** (append 到 flush 的循环)
- **SDS_NOINIT (sds.c:14,97-98)**: `if (init==SDS_NOINIT) init = NULL;` — 跳过 memset (L99-100 的 memset 分支), 内容不初始化
  - 消费: aof.c / config.c / networking.c / object.c — 大缓冲创建免清零 (要立即覆盖)
- **导出分配器 (sds.h:256-258)**: `sds_malloc/sds_realloc/sds_free` — 注释 L252-255: "Sometimes the program SDS is linked to may use a different set of allocators, but may want to allocate or free things that SDS will respectively free or allocate"
  - 场景: Lua (R-30) 等宿主代码分配了 SDS 会释放的块, 或反之 — 必须用同一分配器
- 关联: 分配器宏 (sds_malloc 内部 = s_malloc_usable 族, sds.c 头部) — 与 R-33 的宏重映射同源

## 代码类型
Glue (生命周期优化) + Interface (分配器互通)

## 跨域关联
- R-8 (aof.c:1216 aof_buf sdsclear) → 每轮复用
- R-30 (Lua 宿主) → 导出分配器动机
- R-28 (networking 大缓冲) → SDS_NOINIT 免清零

## 结论
复用三件: sdsclear (AOF 写缓冲每轮复用, 免分配) / SDS_NOINIT (大缓冲免 memset, 创建即覆盖) / 导出分配器 (宿主与 SDS 分配器互通, 防"谁分配谁释放"错配)。都是"省一次操作"的微观优化, 但支撑 Redis 高吞吐的写路径。
源码位置: sds.c:200-203,97-98; sds.h:256-258; aof.c:1216
