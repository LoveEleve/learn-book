# Reasonix 域发现 v29 补充(续扫第十五轮:支撑层收官)— 2026-08-14

> 承接:v28。本轮:ablation/store/filelock——支撑层最后收尾。
> 结论:ablation 6 模块开关确认(评测方法论的基础设施),无新域;本轮后支撑层基本扫完。

---

## 一、v29 深化确认(支撑层)

| 设计 | 位置 | 要点 |
|------|------|------|
| **消融 6 模块** | ablation/ablation.go | Evidence/Planner/Subagent/Retrieval/Compaction/FullFold 可关——**评测把 solve rate 变化归因到单个子系统**(基准方法论的工程化) |
| **remote 文件名规则** | store/remote.go | RemoteWorkspaceSlug(路径→slug 无碰撞)+ serve-<slug>.{json/token/log/port/pid/lock}——**远程运行的文件布局** |
| **filelock 双模式** | filelock/filelock.go | Acquire(本地+外部超时)/TryAcquire(非阻塞)/canonicalLockPath——**锁抽象** |

---

## 二、关键设计(通用价值)

1. **"消融 = 归因"**:逐个关闭子系统测 solve rate——**"哪个组件贡献多少"的评测方法**(与 Hermes readtool A/B、Reasonix e2ebench 类级边际效用同族——评测方法论三项目共证)
2. **"slug 无碰撞"**:路径→slug 的确定性且无碰撞——**命名正确性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v28 | — | 102 | 102 |
| v29 | ablation/store/filelock | +0(深化 1 设计) | **102**(深化) |

> **Reasonix 续扫 15 轮完成(102 域 + 40+ 契约深化)**:internal/ 96 包全部覆盖(含 taskmonitor/acp/checkpoint 验证/config/guardian/capability/installsource/catalog/shellsafe/pluginpkg/extension/eventwire/memory 细节),源码实现面全覆盖。
> 剩余:desktop 前端(排除)/i18n 消息(数据)/providers 适配细节(同构)——按产品价值已到边际。
