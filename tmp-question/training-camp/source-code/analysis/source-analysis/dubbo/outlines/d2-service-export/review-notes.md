# D-2 服务导出 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "ServiceConfig.export()—Protocol.export—Netty 启动" — **export 6 层链实证** (export → doExport → doExportUrls → 1Protocol → exportUrl → doExportUrl, ServiceConfig:326-993) | 大纲 §1 |
| 2 | **补充锚点** | **scope 三态** (L650-690): NONE/LOCAL/REMOTE + **本地始终可导** (injvm port 0) — 执行计划未提 | 大纲 §2 |
| 3 | **补充锚点** | **RegisterTypeEnum 3 值** (AUTO/MANUAL/NEVER) + registerType 修正 (REGISTER_KEY=false → MANUAL) | 大纲 §1 |
| 4 | **语义标注** | **protocolSPI = Protocol 自适应** (L188) — **D-1 自适应 + Wrapper 织入的消费面** (export 时挂监听/过滤) | 大纲 §4 |
| 5 | **补充锚点** | **serverMap 缓存 + reset**: 同地址多服务共享 server; 已存在 → **server.reset (override 支持)** (DubboProtocol:377-405) | 大纲 §3 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (ServiceConfig 188,326,577-690,730-770,978-1030 / DubboProtocol 346-430) | 记录 |

## 07 五维度

### 维度1 功能正确性
- export 链 (harness A/D)
- scope 三态 (harness B)
- server 缓存 (harness C)

### 维度2 性能
- serverMap 缓存 (同地址复用)
- 双检锁
- exporters CopyOnWrite

### 维度3 内存
- exporterMap/exporterMap
- registerType 分组

### 维度4 一致性
- scope 决策
- registerType 修正
- reset override

### 维度5 负面空间 (已写入大纲 6 条)
- 不热发布/不按需懒加载/不多租户/不服务降级/不端口协商/不导出回滚

## 结论
D-2 锚点 ~30 处验证, 8 闭环完成, harness 12/12 (A-D 4 面), **数字穷举 1 + 语义标注 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 D-1 ✅; 引出 D-3/D-4/D-5 ✅; 对照 ZK Netty ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (链/scope/server/invoker) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **EXT_PROTOCOL 附加协议** (L660-685): 主协议外追加 (IS_PU_SERVER/IS_EXTRA 标记) — 多协议导出面 | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (链/scope/server/invoker) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (EXT_PROTOCOL #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 链完整 | 6 层渐进 — 配置→URL→invoker→protocol ✅ | 通过 |
| V2 | 本地通道 | injvm port 0 — 同 JVM 引用 ✅ | 通过 |
| V3 | server 复用 | 同地址共享 — 端口不重复 ✅ | 通过 |
| V4 | override | reset — 动态配置生效 ✅ | 通过 |
| V5 | 元数据发布 | publishServiceDefinition — 服务发现面 ✅ | 通过 |
| V6 | 注册可控 | registerType — 注册模式细分 ✅ | 通过 |
| V7 | 延迟导出 | delay — 启动节奏控制 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **optimizeSerialization** (DubboProtocol:373): export 时序列化优化 (SerializationOptimizer) — 性能面 | 大纲 §3 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (optimizeSerialization), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (unexport 链/延迟导出实现/executor 隔离/回调服务/metadata 附加)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | unexport 链? | unexported 标志 + exporters 遍历 unexport (L253) — 销毁面 | 通过 (验证) |
| T2 | 延迟导出? | delay 参数 → 定时导出 (L350-356 注释) | 通过 (验证) |
| T3 | executor 隔离? | processServiceExecutor (L667-690): **ISOLATION 模式才生效** | 通过 (验证) |
| T4 | 回调服务? | IS_CALLBACK_SERVICE + STUB_EVENT (DubboProtocol:351-360) — 回调导出面 | 通过 (验证) |
| T5 | metadata 附加? | serviceMetadata.getAttachments().putAll(map) (L640-641) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 销毁完整 | unexport 遍历 — 资源释放 ✅ | 通过 |
| V2 | 延迟可控 | delay — 节奏面 ✅ | 通过 |
| V3 | executor 隔离 | ISOLATION 模式 — 隔离面 ✅ | 通过 |
| V4 | 回调支持 | callback service — 双工面 ✅ | 通过 |
| V5 | 元数据随 URL | attachments — 服务元数据 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **回调服务导出面** (T4): IS_CALLBACK_SERVICE + STUB_EVENT (L351-360) — 消费者反向导出 (双工通信) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 export 链 (6 层/registerType/延迟) — 可写 ✅
- §2 scope 三态 (三态/本地/远程/EXT_PROTOCOL) — 可写 ✅
- §3 DubboProtocol (exporter/server 缓存/reset/回调/优化) — 可写 ✅
- §4 invoker 元数据 (proxyFactory/Delegate/Wrapper) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (回调服务导出)。核心认知: **export 6 层链** (自适应 Protocol 消费 D-1) + **scope 三态 + serverMap 复用 reset** + **EXT_PROTOCOL 多协议 + 回调双工**。harness 12/12 全过。大纲经修复后反写测试全过。
