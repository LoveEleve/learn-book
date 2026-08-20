# SW-1 Agent Command Model — Pass 2 问题收敛

## P1: CommandDeserializer 覆盖范围
- 显式 deserialize 分派只有 4 个可反序列化 command：Profile / ConfigurationDiscovery / AsyncProfiler / Pprof
- ContinuousProfilingPolicy/Report、EBPF、TraceIgnore 仅实现 serialize，不实现 Deserializable
- 结论：不是遗漏分派，而是它们是 OAP→agent 下发单向 command；CommandDeserializer 是 agent→OAP 或接收 command 的反向解析入口，不能按“所有 NAME 都必须出现”误判

## P2: apm-protocol 与 OAP 消费边界
- Profile/AsyncProfiler/Pprof command 在 server-core CommandService 创建，在各自 receiver handler 发送
- ConfigurationDiscovery 由 configuration-discovery receiver 消费
- Continuous profiling policy/report 与 EBPF command 由对应 profiler/EBPF receiver 消费
- 结论：SW-1 只负责 command model；CommandService/receiver handler 属于后续 OAP 域，不前向吞并

## P3: 序列化契约
- command payload 通过 `org.apache.skywalking.apm.network.common.v3.Command` + `KeyStringValuePair` 承载
- `BaseCommand` 统一保存 command name + serial number
- 各 command 的 deserialize 以 key 字符串匹配字段；未知字段默认忽略，缺字段使用默认初始化值
- 结论：这是控制面 command envelope，不是 telemetry proto data model

## P4: proto 边界
- 初始阻塞是 `apm-protocol/apm-network/src/main/proto` 子模块未初始化；初始化后确认其中有 **26 个 proto**
- Maven 编译 `apm-network` 成功，生成代码后源文件总数从 16 升到 335（含 generated protobuf/grpc Java）
- 全仓其余 proto 仍大量分布在 OAP receiver/query/exporter/配置模块
- 结论：SW-1 应覆盖 `apm-protocol` 的通用采集协议与 command envelope；OAP 内部专用 proto 继续按真实消费方进入 SW-2/SW-4/SW-7

## P5: 规划修正
- SW-1 正式名称采用“Agent Command Model + Protocol Boundary Audit”
- 8 域候选中，完整 wire protocol 由后续 OAP 消费域承接

## 收敛判定
- SW-1 Pass 0/1/2 已闭环：数字穷举、command 分派、单向/双向 command 差异、消费边界、proto 边界均已核对。
- 尚未写 outline/harness；下一步应对 16 个 main Java command 做 serialize/deserial round-trip harness，再进行多轮 review。