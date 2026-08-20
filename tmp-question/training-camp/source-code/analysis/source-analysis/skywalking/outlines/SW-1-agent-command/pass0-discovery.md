# SW-1 Agent Protocol / Data Model — Pass 0 发现

> 仓库: `/data/workspace/source-code/code/spring/skywalking`
> HEAD: `b272da3 Prepare for release 10.4.0`
> 日期: 2026-08-17

## 1. 首轮数字穷举
- `apm-protocol/` Java 文件：**17**（其中 main 16、test 1）
- `apm-protocol/apm-network/src/main/proto` 子模块初始化后可见 proto：**26**
- 仓库全局 proto 文件：**53**
  - `apm-protocol`：26
  - `oap-server`：44（与 apm-protocol 有交叉来源，含 receiver/query/exporter/config 等）
  - `test/`：9

## 2. 重要边界修正
初始计划把 `apm-protocol/` 视作完整“协议总域”仍然过宽。初始化 submodule 后确认：

- `apm-protocol/apm-network` 现在包含：
  - Java command model（16 个 main Java）
  - 26 个 proto/gRPC 数据采集协议源
- 但仓库全局仍有大量 proto 分布在 OAP receiver/query/exporter/configuration/test 等模块
- 因此 SW-1 可以承担“通用采集协议 + command envelope”的主入口，但不能把 OAP 内部 query/exporter/fetcher/配置 proto 全部吞并；它们仍应按真实消费模块拆回后续域

## 3. 当前可见 Java command model
- `BaseCommand`：command name + 序列化/反序列化契约
- `CommandDeserializer`：按 `command.getCommand()` 字符串分派到静态 deserializer
- 已知分派命令：
  - `ProfileTaskCommand`
  - `ConfigurationDiscoveryCommand`
  - `AsyncProfilerTaskCommand`
  - `PprofTaskCommand`
- 未知 command → `UnsupportedCommandException`
- 其他 command 类型：Continuous Profiling policy/report、EBPF extension config、TraceIgnore 等，需继续逐个检查它们是否进入 CommandDeserializer 主分派链

## 4. 初步风险点
- `CommandDeserializer` 是字符串分派表，新增 command 必须同步注册，否则运行时直接 Unsupported
- command model 与 proto/generated message 的边界当前不完整，不能仅凭 apm-protocol 目录推断完整 wire contract
- profiling command 与 configuration discovery command 可能属于控制面，而不是普通 telemetry data plane
- Java command 的 `Serializable/Deserializable` 是 SkyWalking 自定义 JSON/command payload 契约，需要与 OAP receiver 消费方交叉验证

## 5. Pass 1 待验证问题
- Q1: 16 个 main Java 文件中，哪些 command 真正被 OAP receiver 消费，哪些只是扩展/兼容模型
- Q2: `CommandDeserializer` 的 4 个显式分派项是否覆盖全部可执行 command，其他 command 是否由不同通道处理
- Q3: `BaseCommand` 的序列化格式、字段兼容和未知字段行为
- Q4: `ProfileTaskCommand` / `AsyncProfilerTaskCommand` / `PprofTaskCommand` 的控制面生命周期
- Q5: 全仓 53 个 proto 应如何拆回 SW-2/SW-3/SW-4/SW-7，避免协议重复归属
- Q6: 当前 apm-protocol 子模块是否应初始化后补读；在未初始化前不能宣称 protocol 域完整

## 6. Pass 0 结论
SW-1 不能暂时命名为“完整 Agent Protocol / Data Model”。当前准确名称是：

> **SW-1 可见 Agent Command Model + Protocol Boundary Audit**

完整协议模型待确认子模块状态并按 OAP 消费模块拆分。