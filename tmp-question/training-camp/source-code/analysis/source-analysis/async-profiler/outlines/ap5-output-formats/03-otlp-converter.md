# 03. 机器读的格式与离线转换 — OTLP 与 converter

> 🟡 Working | 12 KP 中的 3 个(otlp protobuf/writer/converter)
> 读者处境: 火焰图给人看,JFR 给 JMC 看——那监控平台(OTLP)和"服务器采样、本地渲染"的工作流呢?

### 1. "手工编码 protobuf" — OTLP/pprof 输出

场景: 采样数据要上报到 Prometheus/OTLP 兼容平台——需要 pprof 格式的 protobuf。

- `Otlp::`(otlp.cpp:9): `_otlp_buffer.startMessage(ProfilesData::dictionary)`(:12)/`field(Link::span_id, zero_ids, 8)`(:25)——**手写 protobuf 编码**(无 protobuf 库依赖)
- pprof 格式: ProfilesData → Dictionary/Mapping/Function/Location 表(:17-43,逐个 startMessage)
- [protobuf: 字段 = tag(wire type + field number)+ 值;手工编码 = 按规范拼字节——与 JFR 手写同哲学: 不引依赖,自写编码]

关键设计: **"格式即协议,自写即可控"**: OTLP 与 JFR 一样手写——项目哲学贯穿: 无外部依赖,二进制格式自己编码。pprof 的"表结构"(函数表/位置表)与火焰图 Trie 是同一信息的两种视图。

### 2. "统一写抽象" — Writer 家族

场景: 文件/内存/回调——输出目标多样。

- `Writer`(writer.h:13,抽象基类)/`FileWriter::open`(writer.cpp:44,`O_WRONLY|O_TRUNC|O_CREAT, 0644`)/内存 writer/CallbackWriter(asprof_execute 的回调输出,AP-1 篇 1)
- 所有输出格式(flameGraph/flightRecorder/otlp)都写 `Writer&`——**输出目标与格式解耦**

关键设计: **"格式 × 目标"的笛卡尔积消解**: 3 格式 × 3 目标 = 9 组合,用 Writer 抽象降到 3+3——新增输出目标只加一个 Writer 实现。

### 3. "服务器采样,本地渲染" — converter(Java)

场景: 生产服务器没浏览器(AP-0 篇 3 的离线工作流)——JFR 文件拿回本地怎么变成图?

- `JfrConverter.java`(converter/one/convert/): 解析 JFR 事件流 → 内部模型(调用栈/计数)
- `JfrToFlame.java`: 还原成火焰图;`JfrToHeatmap`(淘汰)/`JfrToOtlp`/`JfrConverter`——**转换家族**
- 工作流: 服务器 `-o jfr` 采集 → 本地 `java -cp converter.jar jfr2flame dump.jfr dump.html`(AP-0 篇 3)
- [Java: converter 是纯 Java(JDK 自带)——解析 JFR 不需要 native;与 C++ 采样器"采集/渲染分离"(AP-0 篇 3 §2)]

关键设计: **采集与渲染的物理分离**: C++ 侧(无依赖)只采集,Java 侧(富生态)只渲染——部署形态决定了这种分工: 目标机最小化,分析机最大化。converter 也是"JFR 格式正确性"的第二读者(JMC 是第一读者)。

---

跨域桥: 数据源 = 上一篇(JFR 录制器)+ AP-4(CallTraceStorage);C API 回调 = AP-1(asprof_execute);离线工作流 = AP-0 篇 3;JFR 规范 = OpenJDK 域 32。

**OpenJDK 关联**: 无强关联(JFR 解析为 Java 生态);可对照 [域 32 JFR — outlines/32-jfr/](事件流格式)。
