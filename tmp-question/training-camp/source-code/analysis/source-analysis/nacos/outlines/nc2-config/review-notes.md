# NC-2 ConfigService 配置客户端 — 六层深审

> 深审标准: 缺陷档案 #1~#15

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划把 ConfigRpcTransportClient 当独立类 | 09 审计确认: **ClientWorker 内部类** (ClientWorker.java:639) — 大纲 §3 明示 |
| 2 | 规划未提 fuzzyWatch (ConfigFuzzyWatchGroupKeyHolder) | 大纲 §6 补: 3.x 模糊监听面 (545 行独立类) |

## 审 2: API/实现路径编造 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "server 返回 null → snapshot 兜底" | 实测: **server 正常返回 null (无配置) 直接 return; snapshot 只在 NacosException 时兜底** — harness 场景修正 + 大纲 §1 精确化 |
| 2 | 初稿假设 "ConfigChangeHandler 简单解析" | 实测: parserList **按 type 找第一个 isResponsibleFor 的解析器** (L65-73), 内置 Properties/Yml — harness 差异比较修正 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | "ConfigChangeHandler 在 config/impl" | 实测确认 (config/impl/ConfigChangeHandler.java:46) ✅ |

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "监听通知链 = 简单回调" | 实测五步: fillContext → ClassLoader 切换 → 过滤链 → receiveConfigInfo → (变更监听器) parseChangeData+ConfigChangeEvent — 与 ALI-A2 的 RefreshEvent 链分层精确化 |

## 审 5: 覆盖率 — 0 缺漏 (三路/监听/信号量/通知链/解析/模糊 7 面全覆盖)

## 审 6: 跨层一致性 — 0 (harness 11/11)

## 深审自抓缺陷 (harness)

| 缺陷 | 本质 |
|:--|:--|
| 第 4 步 NPE | 场景设计错: 把"服务端无配置"当 snapshot 触发条件 — 实证修正为"仅异常兜底" (真实语义) |
| 解析器断言失败 | 模拟缺差异比较 (old vs new) — 修正为 PropertiesChangeParser 语义 (新增/修改才产出) |

## 结论: 6 项修正, 全部落盘大纲/harness; 锚点重 grep 验证通过
