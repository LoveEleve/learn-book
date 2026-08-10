# 内功修炼——书籍目录录制阶段

**Fact (2026-08-03)**: 内功修炼TOC录制阶段——21本完整目录已记录，覆盖6大维度。用户通过粘贴完整目录来一本本录入，不写内容。

> 目录路径: `/data/workspace/source-code/book/MinerU/tmp-question/内功修炼/`

## 21本分类

| 维度 | 数量 | 书目 |
|------|:---:|------|
| **Linux内核底层** | 5 | 深入理解Linux进程与内存、深入Linux内核架构与底层原理、图解Linux内核(6.x)、Linux技术内幕、Linux-UNIX系统编程手册 |
| **操作系统理论** | 2 | 现代体系结构上的UNIX系统(SMP/MESI)、现代操作系统原理与实现(ChCore实验) |
| **CPU性能分析** | 3 | 高性能软件-CPU性能分析与源码调优(TMA/PEBS)、深入理解软件性能(KUtrace)、性能之巅(BrendanGregg) |
| **内存/文件系统** | 2 | The-Linux-Memory-Manager(folio/OOM)、文件系统技术内幕(Ext2→CephFS→S3) |
| **网络/可观测** | 5 | 趣味网络图解(Wireshark)、图解Linux网络编程、深入理解Linux网络(收发包/epoll/百万并发)、深入浅出TCP-IP图解、Kubernetes网络系统原理(CNI动手实现) |
| **eBPF全栈** | 4 | BPF之巅(BrendanGregg)、深入理解eBPF与可观测性、eBPF开发指南(BTF/CO-RE/Golang)、深入理解eBPF与可观测性 |

## 工作模式
- 用户粘贴目录→创建 `{BookName}/{BookName}-目录.md`
- 纯目录记录，不写内容
- 每本独立子文件夹，与MySQL项目模式一致

**Why**: MySQL 14本完成后，用户开启新主题——构建计算机基础功底的全景书单。覆盖从CPU硬件到Kubernetes网络的完整技术栈。

**How to apply**: 继续接受目录录入。后续用户可能开启"先规划再写"模式（类似分布式书籍重写），但目前仅为TOC录制阶段。
