# SW-3D Event Analyzer — Outline

> 模块: `oap-server/analyzer/event-analyzer`
> 生产 Java: **8**
> 测试: **2**（本轮补充）
> 日期: 2026-08-18

## 1. 域定位
`SW-3D` 负责把协议层 `org.apache.skywalking.apm.network.event.v3.Event` 归一化为 `server-core` 的 record 模型 `org.apache.skywalking.oap.server.core.analysis.record.Event`，并通过 `RecordStreamProcessor` 送入后续持久化/导出消费侧。

它不是 trace span 分析器，也不是 query/storage 域；当前是一个很薄的 `service -> analyzer -> listener -> record processor` 输出链。

## 2. 入口与边界
主入口：
- gRPC receiver / REST receiver 通过 `EventAnalyzerService` 调用 analyzer
- `EventAnalyzerServiceImpl.analyze(...)`
- `EventAnalyzer.analyze(...)`
- `EventRecordAnalyzerListener.parse(...)`
- `EventRecordAnalyzerListener.build(...)`
- `RecordStreamProcessor.getInstance().in(event)`

关键边界：
- receiver 层负责 layer 必填与协议入口约束
- `EventAnalyzerServiceImpl` 只负责“双无效时间”的兜底
- `EventRecordAnalyzerListener` 只负责 record 字段映射，不做时序合法性修复
- `RecordStreamProcessor` 是消费侧，按方法论不在本域深展开

## 3. 模块装配
`EventAnalyzerModuleProvider` 的职责很小：
- `prepare()` 中创建 `EventAnalyzerServiceImpl` 并注册 `EventAnalyzerService`
- `start()` 中只注册一个 `EventRecordAnalyzerListener.Factory`
- `requiredModules()` 只依赖 `CoreModule`

结论：
- 当前不存在复杂 provider 扇出
- listener 数量在本域当前为 1

## 4. 时间语义
`EventAnalyzerServiceImpl.analyze(...)` 的唯一归一化逻辑：
- 若 `startTime<=0 && endTime<=0`，则用当前时间回填两者
- 若只有一边有效，则保持原值，不补另一边
- 若 `startTime>endTime`，直接透传，不修复

`EventRecordAnalyzerListener.parse(...)` 的时间落桶规则：
- 优先使用 `startTime` 计算 `timeBucket/timestamp`
- 仅当 `startTime<=0 && endTime>0` 时回退到 `endTime`
- 两者都非正数时，不设置 `timeBucket/timestamp`

## 5. 字段映射语义
`EventRecordAnalyzerListener.parse(...)` 负责：
- `layer`: `Layer.nameOf(e.getLayer())`
- `uuid/name/type/message`
- `source.service/serviceInstance/endpoint` 经过 `NamingControl` 规范化
- `parameters` 非空时转 JSON 字符串
- `startTime/endTime`
- `timestamp/timeBucket`

已确认的负面空间：
- `source` 缺失时，不写 service/serviceInstance/endpoint
- `parameters` 空 map 时，不写 JSON
- `layer` 非法名字时映射为 `Layer.UNDEFINED`
- proto3 默认 `type` 未提供时，实际写入 `Normal`

## 6. 与外域关系
- `SW-2`：依赖模块系统注册 `EventAnalyzerService`
- `SW-3D`：只分析 event record 归一化
- `SW-5`：`RecordStreamProcessor`、TTL、DAO、落库语义属于后续存储域
- `SW-7`：receiver 的协议入口校验与 telemetry 计数在 transport 域交叉引用

## 7. 测试与回归
本轮新增：
- `EventAnalyzerServiceImplTest`
- `EventRecordAnalyzerListenerTest`

覆盖点：
- 双无效时间回填
- 单边有效时间保留
- listener factory 注册可见性
- source / naming / parameters / type / layer / timestamp / timeBucket 映射
- 未知 layer 与无正时间戳场景

已验证命令：
```bash
./mvnw -pl oap-server/analyzer/event-analyzer -am -Dtest=EventAnalyzerServiceImplTest,EventRecordAnalyzerListenerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：**6/6 PASS**

## 8. 收敛结论
`SW-3D` 当前已从 Pass 0 推进到可交接收敛状态：
- 边界清晰
- 单 listener 薄链路结论已证实
- 关键时间/空字段语义已落测试
- 未发现需要立即修复的源码缺陷

仍需保留的前向引用：
- `RecordStreamProcessor` 的 TTL/DAO/worker 行为留待 `SW-5`
- receiver 的 layer 校验与异常返回留待 `SW-7`
