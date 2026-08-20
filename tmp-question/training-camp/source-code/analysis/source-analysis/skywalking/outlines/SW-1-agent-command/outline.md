# SW-1 Agent Command Model — outline 收敛版

> 当前范围: `apm-protocol/apm-network` Java command model + 初始化后的 26 个通用协议 proto
> harness: `MiniSW1` **9/9 PASS** | 日期: 2026-08-17

## 一、准确域名
SW-1 不是全仓所有协议的总域，而是：

> **Agent Command Model + 通用采集 Protocol Boundary**

OAP 内部专用 proto 继续按真实消费模块归入 SW-2/SW-4/SW-7。

## 二、Command envelope
- `BaseCommand` 统一写入 `command` + `SerialNumber`
- `Serializable.serialize()` 返回 `Command.Builder`
- `Deserializable.deserialize(Command)` 只存在于需要反向解析的 command
- `CommandDeserializer` 对 4 个 command 做字符串分派：Profile / ConfigurationDiscovery / AsyncProfiler / Pprof
- 其他 command 是单向 OAP→agent 控制 command，只要求 serialize:
  - EBPF profiling
  - Continuous profiling policy/report
  - TraceIgnore
- unknown command → `UnsupportedCommandException`

## 三、协议边界
- submodule 初始化后，`apm-protocol/apm-network` 可见 26 个 proto
- Maven protobuf/grpc codegen 成功，apm-network 编译成功
- 生成后 apm-network 编译源数量为 335（含 generated protobuf/grpc Java）
- 全仓仍有 53 个 proto；OAP receiver/query/exporter/configuration 专用协议不能全部归入 SW-1

## 四、序列化验证
- Profile round-trip ✅
- ConfigurationDiscovery round-trip ✅
- AsyncProfiler round-trip ✅
- Pprof round-trip ✅
- EBPF serialize ✅
- Continuous policy serialize ✅
- Continuous report serialize ✅
- TraceIgnore serialize ✅
- unknown command reject ✅

## 五、边界结论
- command model 是控制面 envelope，不等同于 telemetry data plane
- `CommandDeserializer` 的 4 项分派不是遗漏，而是反向解析职责边界
- command 的真正消费方在 OAP server-core / receiver plugins，后续域继续追消费链
- generated protocol Java 不手工改，只作为 proto 契约的生成结果验证