# JVM 核心 — 知识点规划

> 方法论: knowledge-planning/methodology/01-03 | TOC-only, 04 跳过
> 主题: Java JVM 运行时 | 3 本书 | N=3 | [Books 1-2 overlap on JVM fundamentals — frame-level P1 not visible]

---

## 01 提取 + 02 深度 + 03 聚类

### 书籍

| # | 书名 | 章数 |
|---|------|:---:|
| 1 | 揭秘Java虚拟机 (JVM设计原理与实现) | 10 |
| 2 | JVM规范 (JVMS 17) | 7 + 附录 |
| 3 | Java性能权威指南 (Scott Oaks) | 12 |

---

### 🔴 Deep (14 项)

| Knowledge Point | Pri | 说明 |
|----------------|:---:|------|
| JVM 运行时数据区 (PC/堆/栈/方法区/Native栈) | P3 | 内存模型基础 |
| Class 文件结构 (魔数/常量池11种/access_flags) | P1 | 两书覆盖 |
| 常量池解析 (constantPoolOop/符号引用→直接引用) | P3 | 加载核心 |
| oop-klass 模型 (oop体系/klass体系/handle体系) | P3 | HotSpot核心 |
| 栈帧结构 (局部变量表/操作数栈/动态链接) | P1 | 执行基础 |
| 类加载机制 (双亲委派/初始化/验证/准备/解析5阶段) | P1 | 加载全链路 |
| 字节码执行引擎 (取指令/译码/栈顶缓存/模板解释器) | P3 | 执行核心 |
| vtable + invokevirtual (静态分派 vs 动态分派) | P3 | 多态实现 |
| 对象分配 (TLAB/栈上分配/逃逸分析/指针碰撞) | P3 | 分配机制 |
| JIT 编译 (分层编译/hotspot计数/OSR/内联/逃逸分析) | P3 | 编译优化 |
| GC 基础 (分代假设/算法/堆大小/元空间) | P3 | 内存管理 |
| GC 算法 (Parallel/G1/CMS/Serial + 选择) | P3 | 回收器 |
| 类加载器分类 (Bootstrap/Ext/App/自定义 + 线程上下文) | P1 | 加载器体系 |
| <clinit> + <init> + 对象初始化完整时序 | P3 | 初始化链 |

### 🟡 Working (12 项)

| Knowledge Point | 说明 |
|----------------|------|
| JVM 字节码指令集 (200+条) | 规范级 |
| 字节码验证器 (Type Checking + Inference) | 类加载 |
| 方法属性解析 (Code/LVT/LVTT) | 调试 |
| 字段重排补白/private 继承 | 内存布局 |
| 镜像类 + 静态字段 | 加载 |
| GC 工具箱 (JFR/jstat/jcmd/jmap) | 工具 |
| JMH 基准测试 | 性能 |
| OS 性能分析 (CPU/IO/网络) | 监控 |
| GraalVM + AOT 编译 | 新 |
| Java 9+ Modules and Layers | 模块加载 |
| Heap 调优 + Native Memory (OOM/NMT/JNI泄漏) | 生产 |
| <clinit>/<init> 详细时序 | 初始化 |

### 🟢 Surface (6 项)

| 项 | 说明 |
|----|------|
| B2 Ch3 "Compiling for JVM" (Java源→Class) | 🟢 交叉 |
| JVM 规范异常表/限制 | 规范层 |
| 29种 Attr 详解 | 规范层 |
| 指令集格式化 | 手册级 |
| 大端/小端验证 | 背景 |
| 吞吐/延迟概念 | 理念 |

---

### 聚类 — 5 组

```
A(Class文件:5) → B(类加载+oop-klass:6) → C(执行引擎:4) → D(JIT:4) → E(GC算法:5)
```

**A Class 文件与结构**: Class结构→常量池→字段详情→类型识别→指令集
**B 类加载与 oop-klass**: 类加载5阶段→oop-klass模型→常量池解析→<clinit>/<init>→镜像→验证
**C 执行引擎**: 栈帧结构→引擎(取指/译码)→vtable+invokevirtual→对象分配(TLAB/逃逸)
**D JIT 编译**: 分层编译→内联→OSR→GraalVM/AOT
**E GC 算法**: GC算法族→Parallel/G1/CMS→调优参数→JMH→JFR

---

### Summary

| 深度 | 计数 |
|------|:---:|
| 🔴 Deep | 14 |
| 🟡 Working | 12 |
| 🟢 Surface | 6 |
| **Total** | **32** |

教学: A→B→C→D→E
