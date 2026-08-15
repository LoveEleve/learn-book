# Reasonix 域发现 v47 补充(续扫第三十三轮:subagent_store/project_index/migrate)— 2026-08-14

> 承接:v46。本轮:subagent_store(949)/bot/project_index(831)/config/migrate(827)。
> 结论:生命周期验证(销毁检查/父会话探针)与迁移标记确认,无新域。

---

## 一、v47 深化确认

### subagent_store(949)

| 设计 | 要点 |
|------|------|
| **生命周期验证** | WithDestroyedChecker(父会话销毁检查)/WithParentSessionProbe(父会话存在探针)——**子代理元数据引用的有效性验证** |
| **SubagentRun.Release** | 运行释放(确定性清理) |
| **EphemeralSubagentRun** | 临时运行(只给 system prompt) |
| **解码错误** | subagentMetaDecodeError(带 Unwrap) |

### project_index(831)

| 设计 | 要点 |
|------|------|
| **bot 项目/会话索引** | buildProjectIndex(项目)/buildSessionIndex(会话)+ 收集器(collector.add 去重映射)——**bot 路由的索引构建** |

### config/migrate(827)

| 设计 | 要点 |
|------|------|
| **legacy 迁移** | legacyConfig/legacyMCPServer/legacyQQConfig → MigrateLegacyIfNeededForRoot;凭证迁移 |
| **MCP 升级迁移标记** | mcpGlobalMigrationMarkerPath(迁移只跑一次——标记防重复) |

---

## 二、关键设计(通用价值)

1. **"引用有效性验证"**:子代理元数据引用的父会话有效性(销毁/存在检查)——**元数据引用的完整性**(与知识库引用完整性同思想)
2. **"迁移标记"**:mcpGlobalMigrationMarkerPath——**迁移幂等性**(只跑一次)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v46 | — | 102 | 102 |
| v47 | subagent_store/project_index/migrate | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 config/provider_presets(1,062)/recovery/rules(1,011)/bot/feishu(1,010)——按需收尾。
