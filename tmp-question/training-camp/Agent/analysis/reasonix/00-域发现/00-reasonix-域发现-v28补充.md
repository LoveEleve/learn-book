# Reasonix 域发现 v28 补充(续扫第十四轮:memory store/retrieval v2/migration)— 2026-08-14

> 承接:v27。本轮:internal/memory/store(838)/retrieval/v2/migration(587)。
> 结论:Volatility 遗忘速度与代码符号分词确认——④知识库/检索的高价值细节。

---

## 一、v28 深化确认

| 设计 | 位置 | 要点 |
|------|------|------|
| **记忆存储模型** | memory/store.go:26-99 | **Scope 与 Type 独立**(调用者自选归属);Memory(Volatility **遗忘速度**——"多快老化";未设置 → 类型默认);ArchivedMemory;StoreFor(userDir,cwd) |
| **归档语义** | :190-297 | Archive/Delete 都走归档(archiveInDir/archivePath 带时间戳)——**删除 = 归档可恢复** |
| **retrieval v2 代码分词** | retrieval/v2.go:18-61 | **splitCodeSymbol**(代码符号拆分——camelCase/snake_case 分词);TokensV2 |
| **FieldedDoc 分字段排名** | :89-169 | RankV2(查询 × 分字段文档)+ SortHitsDesc |
| **迁移/救援** | migration/migration.go:17-184 | **LegacyRescue(旧数据救援)**/LegacySessionImport/LegacyMemorySources——旧版本数据导入 |

---

## 二、关键设计(通用价值)

1. **"Volatility 遗忘速度"**:每条记忆声明老化速度(未设置 → 类型默认)——**知识保鲜的字段化**(与 Hermes 技能 stale、Pi facts 无此维度对比——Reasonix 独有)
2. **"代码符号分词"**:代码检索的分词优化——**检索领域适配**
3. **"删除 = 归档"**:删除走归档带时间戳——**可恢复性**(与 Hermes curator 归档同族)

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v27 | — | 102 | 102 |
| v28 | memory/retrieval v2/migration | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 appidentity/i18n/notify、ablation、store——支撑层最后收尾。
