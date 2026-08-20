# D-2 服务导出 — export 6 层链与 server 复用

> 项目: Dubbo | 🔴 Deep / 1 篇 | ServiceConfig(1202)+DubboProtocol(661)+Protocol SPI+ExporterManager
> 基线: DUBBO-PLAN D-2 (生命周期) — 前置: **D-1 (Protocol 自适应+Wrapper 织入)** — 展开 6 层链→scope 三态→server 复用→invoker 元数据

---

## §0.8

- 🔴 Deep，1篇 — 入口(**ServiceConfig.export(RegisterTypeEnum) L325**) → 6 层链(**export→doExport→doExportUrls→1Protocol→exportUrl→doExportUrl, 配置→URL→invoker→protocol 渐进 L326-993**) → scope 三态(**L650-690: NONE/LOCAL/REMOTE; 本地始终可导 injvm port 0**) → 注册模式(**RegisterTypeEnum AUTO/MANUAL/NEVER L; REGISTER_KEY=false→MANUAL**) → 协议装配(**protocolSPI=Protocol 自适应 L188; EXT_PROTOCOL 附加协议 L660-685[IS_PU_SERVER/IS_EXTRA]**) → server 复用(**DubboProtocol openServer L346-375: serverMap 缓存同地址共享+server.reset override L377-405**) → 优化面(**optimizeSerialization L373; 回调服务导出 IS_CALLBACK_SERVICE+STUB_EVENT L351-360**) → invoker 元数据(**proxyFactory.getInvoker+serviceMetadata.getAttachments().putAll L640-641**)
- 设计模式: [模式: 门面链+SPI 自适应+缓存复用+双工回调]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ServiceConfig.java:325 | 入口 | **export(RegisterTypeEnum)** — 3.x 带注册模式参数 | High |
| ServiceConfig.java:326-993 | 链 | **export 6 层链**: export→doExport→doExportUrls→1Protocol→exportUrl→doExportUrl — 配置→URL→invoker→protocol 渐进 | High |
| ServiceConfig.java:650-690 | scope | **scope 三态**: NONE/LOCAL/REMOTE — **本地始终可导** (injvm port 0) | High |
| ServiceConfig.java:188 | 协议 | **protocolSPI = Protocol 自适应实例** (D-1 消费: export 挂监听/过滤) | High |
| ServiceConfig.java:660-685 | 附加 | **EXT_PROTOCOL 附加协议**: 主协议外追加 (IS_PU_SERVER/IS_EXTRA 标记) — 多协议导出 | High |
| DubboProtocol.java:346-375 | server | **openServer: serverMap 缓存** — 同地址多服务共享 server | High |
| DubboProtocol.java:377-405 | reset | **已存在 server → server.reset** — override 动态配置生效 | High |
| DubboProtocol.java:373 | 优化 | **optimizeSerialization** — export 时序列化优化 (SerializationOptimizer, D-10 挂钩) | High |
| DubboProtocol.java:351-360 | 回调 | **回调服务导出**: IS_CALLBACK_SERVICE + STUB_EVENT — 消费者反向导出 (双工) | High |
| ServiceConfig.java:640-641 | 元数据 | **serviceMetadata.getAttachments().putAll(map)** — 元数据随 URL | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 导出是单管线机制 (6 层链) + 三决策面 (scope/registerType/server 复用) — 1篇按"链→scope→server→invoker 元数据"展开; D-1 自适应 Protocol 消费 (导航), 引出 D-3 引用对称面/D-4 调用链/D-5 注册中心。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | export 6 层链 (配置→URL→invoker→protocol) | 🔴 | **为什么🔴**: 导出主线 |
| P1-2 | scope 三态 + 本地始终可导 | 🔴 | **为什么🔴**: 服务可见性语义 |
| P1-3 | serverMap 缓存 + reset override | 🔴 | **为什么🔴**: 端口/配置管理 |
| P1-4 | protocolSPI 自适应 + RegisterType 三值 | 🔴 | **为什么🔴**: 协议/注册模式决策 |
| P2-1 | EXT_PROTOCOL 附加协议 | 🟡 | **为什么🟡**: 多协议场景 |
| P2-2 | 回调服务导出 (双工) | 🟡 | **为什么🟡**: 双向通信面 |
| P2-3 | optimizeSerialization + 元数据附件 | 🟡 | **为什么🟡**: 优化/元数据面 |
| P3-1 | executor ISOLATION 隔离 | 🟢 | **为什么🟢**: 隔离面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **6 层导出链** | 🔴 | 主线 |
| B | **scope+注册模式** | 🔴 | 可见性决策 |
| C | **server 复用+reset** | 🔴 | 资源管理 |
| D | **invoker 元数据+优化面** | 🟡 | 支撑面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | export 6 层链 | 配置→URL→invoker→protocol 渐进: export(RegisterTypeEnum) 入口 → doExport 校验 → doExportUrls 组装 URL → 1Protocol 协议选择 → exportUrl 单协议导出 → doExportUrl 构建 invoker+exporter — 6 层数字穷举实证 | ServiceConfig.java:326-993 |
| q2 | scope 三态 | NONE 不导出/LOCAL 仅本地/REMOTE 远程; **本地始终可导** (injvm port 0) — 与 D-3 引用侧本地兜底对称 | ServiceConfig.java:650-690 |
| q3 | server 复用 | serverMap 缓存: 同地址多服务共享 server (端口不重复); 已存在 → server.reset (override 动态配置生效); 双检锁 | DubboProtocol.java:346-405 |
| q4 | 注册模式 | RegisterTypeEnum AUTO/MANUAL/NEVER; REGISTER_KEY=false → MANUAL (只导出不注册) | ServiceConfig.java |
| q5 | 协议自适应 | protocolSPI = Protocol 自适应实例 — D-1 消费面; EXT_PROTOCOL 主协议外追加 (IS_PU_SERVER/IS_EXTRA) | ServiceConfig.java:188,660-685 |
| q6 | 回调双工 | IS_CALLBACK_SERVICE + STUB_EVENT — 消费者反向导出回调服务 (双工通信) | DubboProtocol.java:351-360 |
| q7 | 元数据 | serviceMetadata.getAttachments().putAll — 服务元数据随 URL (D-11 挂钩) | ServiceConfig.java:640-641 |

→ 引出 D-3 服务引用 (对称面): 本地始终可引 vs 本地始终可导; D-4 调用链: 导出 invoker 是调用目标; D-5 注册中心: registerType 决定注册行为。
