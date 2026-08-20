# Java GC 专题 — 知识点规划

> 方法论: knowledge-planning/methodology/01-03 | TOC-only, 04 跳过
> 主题: Java GC 垃圾回收 | 2 本书 | N=2

---

## 01 提取 + 02 深度 + 03 聚类

### 书籍

| # | 书名 | 章数 |
|---|------|:---:|
| 1 | 深入探索JVM垃圾回收 | 16 |
| 2 | 新一代垃圾回收器ZGC设计与实现 | 10 |

---

### 🔴 Deep (12 项)

| Knowledge Point | Pri | 说明 |
|----------------|:---:|------|
| GC 算法基础 (复制/标记清除/压缩/分代回收) | P1 | 算法框架 |
| GC 根 (强根/弱根/JVM根构成) | P3 | 可达性分析基础 |
| 安全点 (解释/编译/本地/JVM并发4种) | P3 | Stop-The-World |
| 三色标记法 + SATB/增量标记 | P3 | 并发标记核心 |
| CardTable + 写屏障 (跨代引用管理) | P3 | 分代GC关键 |
| CMS 8 阶段 | P3 | 经典并发回收 |
| G1 (Region/Young+Mixed/SATB/Humongous) | P3 | 默认GC |
| ZGC 多视图映射 + Colored Pointers + Load Barrier | P1 | 核心创新 |
| ZGC 回收 10 阶段 [B1=概述, B2=源码级] | P1 | 并发转移 |
| GC 触发时机 + Full GC 降级 | P3 | 降级场景 |
| GC 日志解读 (G1/ZGC/Xlog) | P1 | 调优必备 |
| GC 选型与调优 (停顿/堆/并行/NewRatio) | P1 | 选代选择 |

### 🟡 Working (12 项)

Serial GC/Parallel GC/G1 NUMA/Shenandoah GC/ZGC 线程模型/ZGC 读写屏障/ZGC 发展与展望(分代 ZGC JDK21)/Metaspace 元数据内存管理/GC 生产参数/OpenJ9 Balanced + Metronome/ARM AArch64/JVM调试(GDB/HSDB)

### 🟢 Surface (4 项)

复制算法比较/标记栈溢出/Cassandra YCSB/ARM鲲鹏章节

---

### 聚类 — 3 组

```
A(GC理论基础:6) → B(GC算法族:7) → C(GC工程:3)
```

**A GC 理论基础**: GC算法→GC根→安全点→三色标记+SATB→CardTable+写屏障→触发机制+Full GC
**B GC 算法族**: Serial→Parallel→CMS→G1→Shenandoah→ZGC(Colored Pointers/Load Barrier/10阶段)
**C GC 工程**: 日志解读→选型调优→生产参数

---

### Summary

| 深度 | 计数 |
|------|:---:|
| 🔴 Deep | 12 |
| 🟡 Working | 12 |
| 🟢 Surface | 4 |
| **Total** | **28** |

教学: A→B→C
