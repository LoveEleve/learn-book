# Hermes 域发现 v35 补充(续扫第二十四轮:plugins 内部复核)— 2026-08-14

> 承接:v34。本轮:plugins/ 内部复核(此前排除的插件族)。
> 结论:**同构排除确认正确**(平台适配器 10K+ 行同构),openviking 记忆插件确认契约实例。

---

## 一、v35 确认(plugins 复核)

| 插件 | 体量 | 结论 |
|------|:--:|------|
| platforms/(telegram 10,542/discord 10,522/slack 9,611/feishu 5,895/matrix 5,423/…) | 大 | **同构适配器**(同一 BasePlatformAdapter 契约,排除正确) |
| memory/(openviking 5,243/hindsight 2,440/honcho 已深挖) | 大 | **MemoryProvider 契约实例**(排除正确;honcho 作契约样本已深挖) |
| kanban/dashboard/plugin_api(2,967) | 中 | 看板仪表盘插件 API |
| 其他(context_engine/observability 等) | — | 契约实例 |

### openviking 独特点(契约多样性样本)

- 自动记忆抽取(6 类)/**分层上下文 L0(~100 tokens)/L1(~2k)/L2(全量)**/语义检索(层级目录)/viking:// URI 文件系统式浏览/资源摄取(URL/文档/代码)

**价值**:与 honcho 对比显示 MemoryProvider 契约的多样性(不同后端的能力差异),但契约接口一致——**契约抽象的正确性证明**。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v34 | — | 81 | 81 |
| v35 | plugins 内部复核 | +0(排除确认) | **81**(确认) |

> **Hermes 续扫 24 轮完成**:顶层(>2,500)/中层(1,000-2,500)/抽查(600-1,000/400-600)/tests 契约/plugins 内部全部覆盖——源码实现面 + 契约面全覆盖。
> 剩余:500 以下小文件(支撑细节)/平台适配器(同构)/i18n(数据)——**程序化收敛达成**。
