# 域 AP-5: 输出与格式 — 知识规划

> 源码路径: src/flameGraph.cpp(231行) + src/flightRecorder.cpp(1558行) + src/jfrMetadata.cpp(333行) + src/otlp.cpp + src/writer.cpp/h + src/converter/(Java 51 文件: convert 16 + jfr 9 + jfr/event 17 + proto 1)
> 源码量: C++ ~6 文件 ~2500 行 + Java 51 文件 ~3000 行
> 提取日期: 2026-08-10(v1 深读提取——范围规划声明已逐文件验证)
> 前置域: AP-4(采样存储——输出读 collectSamples)

## 01 逐源提取

| Source File | Inferred Mechanism | Confidence |
|------------|-------------------|------------|
| flameGraph.cpp:17 | **模板二进制嵌入**: `INCBIN(FLAMEGRAPH_TEMPLATE, "src/res/flame.html")`——HTML 模板编译期嵌入二进制,运行零文件依赖 | High |
| flameGraph.cpp:67-112 | **调用树构建**: `Node`/`Trie` 树(:67-82)——按栈 ID 合并成树;`_minwidth` 阈值(:111,`_root._total * _minwidth / 100`)过滤极细帧;`dump`(:109)输出 | High |
| flameGraph.cpp:149-157 | **帧输出**: `printFrame`——HTML 帧绘制;`[inlined]`/`[interpreted]` 标记判定(:152,`_inlined < _total`) | Medium |
| flightRecorder.cpp:237-332 Recording | **JFR 记录体**: `Recording`(:237)持有并发缓冲(CONCURRENCY_LEVEL :244);构造时写**头部区**: `writeHeader/writeMetadata/writeRecordingInfo/writeSettings/writeOsCpuInfo/writeJvmInfo/writeSystemProperties/writeNativeLibraries`(:295-312) | High |
| flightRecorder.cpp:1473-1535 recordEvent | **事件写入分发**: `recordEvent(lock_index, tid, call_trace_id, event_type, event)`(:1473)——按类型分发(executionSample/allocSample/lockSample...);**per-thread 时间戳计数更新**(:1476-1482,sample_counter) | High |
| flightRecorder.cpp:1311-1467 start/JfrSync | **JFR 启动与同步**: `start`(:1311,输出文件校验)→ `startMasterRecording`(:1404,**调用 JDK 自己的 JFR 开始录制**,`--jfrsync` 模式);`JfrSync` 类反射调用(`CallStaticVoidMethod` :1454) | High |
| jfrMetadata.cpp:41-155 | **元数据流式定义**: type/field 链式 API(:41-155,`type("jdk.types.FrameType"...).field(...)` 流式构建)——JFR 类型系统的声明式描述 | High |
| otlp.cpp:12-43 | **OTLP protobuf 编码**: `startMessage(ProfilesData::dictionary)`(:12)/`field(Link::span_id,...)`(:25)——**pprof 格式**(ProfilesData/Dictionary/Function/Location 表)手工 protobuf 编码 | High |
| writer.cpp:44-48 | **文件输出**: `FileWriter::open`(`O_WRONLY|O_TRUNC|O_CREAT, 0644` :44)——统一写抽象 | Low |
| converter/JfrConverter.java | **JFR 解析器(Java)**: 解析 JFR 事件流 → 内部模型——离线转换的入口 | High |
| converter/JfrToFlame.java | **JFR→火焰图转换**: 把 JFR 事件还原为火焰图/调用树——服务器采集、本地渲染的工作流落点(AP-0 篇 3) | High |
| converter/(JfrToHeatmap/JfrToOtlp/Arguments...) | **转换家族**: 热力图(淘汰)/OTLP/参数解析——converter 是"格式翻译器"集合 | Medium |

*12 个知识点*

---

## 02 聚合

### P1 — 系统级共识 (≥5 文件)

| KP | 出现文件 | 说明 |
|----|---------|------|
| 输出管线(采样→树→格式) | flameGraph, flightRecorder, otlp, writer, converter | 同一份采样数据,N 种格式 |

### P2 — 局部重要 (2-4 文件)

| KP | 出现文件 |
|----|---------|
| JFR 双轨(自研写 + JDK 同步) | flightRecorder, jfrMetadata, JfrSync |
| 调用树构建 | flameGraph(Trie), callTraceStorage(AP-4 去重) |

### P3 — 孤立或专项 (1 文件)

| KP | 文件 |
|----|------|
| OTLP | otlp.cpp |
| 文件写抽象 | writer.cpp |
| 转换家族 | converter/(Java) |

---

## 03 深度分类

### 🔴 Deep (教学核心)

| KP | 为什么 |
|----|------|
| 自研 JFR(Recording 头部区+事件分发) | "不依赖 JDK jdk.jfr 模块写 JFR"的实现——与 OpenJDK 域 32 对照 |
| JfrSync 双轨(与 JDK JFR 同步) | `--jfrsync` 的机制: async-profiler 事件并入 JDK JFR 录制 |
| 事件写入分发(recordEvent) | 信号内并发缓冲追加,后台落盘——不阻塞采样 |
| 调用树构建(Trie+minwidth) | 火焰图"合并+过滤"的实现——AP-0 读法的源码依据 |
| OTLP protobuf 手工编码 | 无依赖写 protobuf——pprof 格式 |

### 🟡 Working (理解即可)

| KP | 为什么 |
|----|------|
| 元数据流式定义 | JFR 类型系统声明方式 |
| 模板二进制嵌入(INCBIN) | 零文件依赖部署 |
| converter 转换家族 | 离线工作流(AP-0 已用) |
| JFR 解析器(JfrConverter) | 解析 JFR 事件流的 Java 入口——离线转换的第一步 |
| JFR→火焰图(JfrToFlame) | 服务器采 jfr、本地渲染的工作流落点 |

### 🟢 Surface (了解)

| KP | 为什么 |
|----|------|
| 帧输出细节 | 渲染细节 |
| 文件写抽象 | 工具性 |

---

## 04 聚类 — 教学顺序与文章拆分

> 教学主线: 采样数据怎么变成人看的图/机器读的格式。

### 依赖图

```
01 火焰图 HTML                     ← 无前置(读 collectSamples)
  ├─ 02 JFR 录制器                 ← 无前置(独立输出轨)
  └─ 03 OTLP 与 converter          ← 依赖 01/02 (格式转换)
```

### 教学顺序

```
01 flameGraph(Trie 树/minwidth/模板嵌入)
  → 02 flightRecorder(Recording 头部区/recordEvent 分发/JfrSync)
    → 03 otlp(pprof protobuf) + writer + converter(Java 离线)
```

### 文章拆分 (3 篇大纲)

| # | 大纲文件 | 主题 | 覆盖 KP |
|:--:|------|------|------|
| 1 | 01-flamegraph-html.md | 火焰图 HTML | Trie 树/minwidth/INCBIN/printFrame |
| 2 | 02-jfr-recorder.md | JFR 录制器 | Recording 头部区/recordEvent 分发/JfrSync/元数据 |
| 3 | 03-otlp-converter.md | OTLP 与离线转换 | otlp protobuf/writer/converter 家族 |

### 关键悬念设计

| 悬念 | 解答 |
|------|------|
| "火焰图的合并节点哪来的?" | Trie 树 + minwidth 过滤(01 篇) |
| "不依赖 JDK 怎么写 JFR?" | 自研 Recording + 元数据(02 篇) |
| "--jfrsync 怎么把事件并进 JDK 的 JFR?" | JfrSync 反射同步(02 篇) |
