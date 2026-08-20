# SCC-1 Bootstrap 上下文 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 7 | **结构失准** | "三守卫"把 filterListeners 当第三守卫 — 真实: 入口双守卫 (L99/L103) + apply 双保护 (BootstrapMarkerConfiguration 防重复 L291-293 / filterListeners 是 apply 内部 L195) | 已修 |
| 8 | **语义重大** | insertPropertySources 三策略漏 **addBefore 分支**: 真实四分支 — !allowOverride\|\|(!overrideNone&&overrideSystemProperties)→addFirst / overrideNone→addLast / !overrideSystemProperties→addAfter(system) / **overrideSystemProperties→addBefore(system)** (L222-224) | 已修 |
| 9 | **交叉纠错** | ParentContextApplicationContextInitializer 上轮判"编造" — **误判**: 它是 Boot 依赖类 (import org.springframework.boot.builder L34), 规划写它是正确的; 真编造仅 3 个 (NoopCircuitBreaker/RefreshListener/RefreshScopeBeanPostProcessor 全仓库零引用) | SCC-PLAN 审计表已纠 |
| 10 | 负面空间精确化 | "不缓存远程配置"准确, 但 **bootstrap.yml 解析有进程内 loadDocumentsCache** (L317/630) — 需区分本地文件缓存 vs 远程配置缓存 | 已补 |
| 11 | 解密面补强 | EnvironmentDecryptApplicationInitializer **"No reason to decrypt bootstrap twice" 防重复守卫** (L81) + addBootstrapDecryptInitializer 注册链 (L299/310) | 已补 |
| 12 | 验证通过 | locateCollection 语义 (null→empty/Composite 过滤 null/单个 List.of) / 入口双守卫 / addFirst 反转保序 / DECRYPTED 特判 L200-201 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | context.setId("bootstrap") 实际 **L203** (大纲写 L201) | 已修 |
| 2 | 锚点漂移 | addAncestorInitializer 实际 **L205** (大纲写 L204) | 已修 |
| 3 | 锚点拆分 | configName 解析 **L107** 与 spring.config.name 注入 **L151** 是两处 (大纲写"注入 L107") | 已修 |
| 4 | 锚点修正 | location/additional-location 注入 **L159-162** (大纲写 L147-149 是解析) | 已修 |
| 5 | 锚点修正 | firstToBeCreated 是 import L27/断言 L35, 非 L28 定义 | 已修 |
| 6 | 验证通过 | Listener L77/L87/L97-104 / PropertyUtils L48-54 / findBootstrapContext L121 / PSBC L68/L107/L184 / 测试 L29/L33 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniBootstrap 12/12 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 前置条件 | 微缩守卫 1 只支持显式 enabled, 真实还有 MARKER_CLASS 双保险 (PropertyUtils.java:37-38) — 测试环境需显式注入 enabled 属性 | 测试补 bootstrapConfig 源 — 实证守卫语义 |
| H2 | 断言语义 | keep-system 断言期望 "sys" 覆盖 remote — 实际微缩 Environment 顺序 (application 最前) 使 resolve 命中 local; 真实 Boot 中 systemEnvironment 优先级高于 application | 断言改为"remote 排最后不覆盖" — 排序语义验证正确 |

## 锚点密度统计

- file:line 锚点数: **30+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 6 项检查 5 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (应用 Bean/配置中心内建/缓存/运行时刷新/locator 顺序契约/热更新)
- [x] 每条有对照物 (Nacos 快照/SCC-2 RefreshScope)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (bootstrap.yml 加载/配置中心注入/优先级实现/双轨制)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (事件 → 三守卫 → 子上下文 → Locator → 排序仲裁)
- [x] 边界交代: 双轨制/三守卫/三插入策略/Composite 展开

## 方法论教训

- **"L107 注入" 拆分为解析与注入两处** — 行号必须对应真实语句, 不能把"变量声明"当"操作"
- **微缩 harness 的守卫语义要对照真实双保险** — MARKER_CLASS 是微缩版没建模的真实机制, 测试前置条件要显式
- **排序断言要基于微缩模型自身语义** — 不能把真实 Boot 的 systemEnvironment 优先级假设进微缩版
