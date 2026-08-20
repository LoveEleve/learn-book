# ALI-A1 Nacos Config 配置加载 — 六层深审

> 深审标准: 缺陷档案 #1~#15 (事实/编造/路径推断/跨项目转移/覆盖率/跨层/文字锚/篇节/表格/数字/范围/待确认/一次一域/代码块/漂移)

## 审 1: 事实错误 (类名/路径/行号) — 3 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划文档称 A-1 核心类含 NacosConfigDataLoader 在 spring-alibaba-nacos-config — 实为 ConfigData 轨 (services SPI), **PropertySourceLocator 轨核心在 starter 模块** | 大纲明确双轨分属两模块 |
| 2 | NacosConfigDataResource 的 equals/hashCode 含 Log 字段 — 深审初稿未提 | 大纲 NacosItemConfig 五元组补注 |
| 3 | NacosConfigManager 的 ConfigService 是 **static** (L35) — 规划未提, 常被误认为实例字段 | 大纲 §4 明示"类级持有" |

## 审 2: API/实现路径编造 — 0 (全部源码行号支撑)

## 审 3: 文件名/目录名推断 — 2 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "NacosConfigDataMissingEnvironmentPostProcessor 是检查配置缺失" | 实测: 继承 commons ConfigDataMissingEnvironmentPostProcessor, **bootstrap/legacy 时跳过** (L25-29) + ORDER = ConfigDataEnvironmentPostProcessor.ORDER+1000 |
| 2 | "NacosConfigBootstrapConfiguration 装配全部核心 Bean" | 实测: 只有 SmartConfigurationPropertiesRebinder (兼容用); 核心 Bean 在 core 模块的 NacosConfigBootstrapConfiguration (同名双类不同模块!) — **易混淆陷阱** |

## 审 4: 跨项目概念转移 — 0 (对照 SCC-1 已验证 PropertySourceLocator 契约同源)

## 审 5: 覆盖率 (大纲 7 节 vs 源码面) — 0 缺漏

覆盖: 双轨制/URI 解析/三级递进/单例/快照/配置类/装配面 — 7 大机制全量。

## 审 6: 跨层一致性 (KP/大纲/问题集数字) — 0 (首次交付, 数字核对: 17/17 harness)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| Repository 静态污染: locate 三次调用后 size=3, 后续 collect 断言 size==2 失败 | 实证 **Repository 是进程级全局** (真实语义) — 修正断言为 size==5 |
| addFirst 存 dataId 而非整源 | 修正 harness 命名语义 (大纲 §3 addFirstPropertySource 语义未变) |

## 结论: 6 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | Locator:87-94 (prefix 三选) / 112-122 (三级递进+高优先注释) / Manager:35 (static service) / Snapshot:47-52 (读后即删) / Resolver:62+75-77 (PREFIX/order=-1) / Loader:105-112 (PROFILE_SPECIFIC+issue#2455) 全精确 | 通过 ✅ |
| 2 | 锚点漂移 | **MissingEP bootstrap 跳过原写 L25-29 (实际 import 区)** → 实证 L51 + ORDER L41 | **已修正** outline |
| 3 | 跨域一致 | refreshCount 节流 ↔ A2 REFRESH_COUNT 联动; 快照 put(写入方 A2) ↔ getAndRemove(消费方 A1) 闭环 | 通过 ✅ |
| 4 | 数字自洽 | 三轨/三级/三策略/MAX 100 全文一致 | 通过 ✅ |
