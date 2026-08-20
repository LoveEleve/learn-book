# N-15-01 配置存储面 — PersistService 双实现族 (存储篇)

> 前置: [[NC-2-ConfigService]] (客户端) + [[NC-5-一致性]] (JRaft 落点) | 引出: N-15-02 (操作/查询链) + N-15-03 (缓存/转储)
> 🔴 A | 方案 A (全深度) | 闭环: q1(接口面) q2(双实现) q3(操作语义)

**读者处境**: 配置落库的持久化接口怎么组织? embedded (内嵌) 与 external (外置 DB) 双实现的差异?

### 1. 接口面 — ConfigInfoPersistService 族

场景: 配置持久化契约?
源码路径:
- **ConfigInfoPersistService** (repository/ConfigInfoPersistService.java:40): 核心接口 — **removeConfigInfo** (L163) + removeByIds (L176) + **removeConfigInfoAtomic** (L194, 原子删除) + **updateConfigInfo** (L215) + **updateConfigInfoCas** (L227, CAS 更新) + updateConfigInfoAtomic (L238)
- 族: ConfigInfoBetaPersistService (beta) / ConfigInfoGrayPersistService (灰度) / ConfigInfoTagPersistService (tag) / ConfigMigratePersistService (迁移)
- **ConfigRowMapperInjector** (repository/ConfigRowMapperInjector.java:46): 行映射注入
关键设计 (q1): **"接口族 = 场景分型"** — 主/灰度/beta/tag/迁移五个 persist 面; 原子/CAS 双写语义 (与服务端乐观锁对应 NC-2 的 publishConfigCas)。 [模式: 接口族]

### 2. 双实现族 — embedded vs external

场景: 内嵌与数据库怎么切换?
源码路径:
- **embedded/** (5): EmbeddedConfigInfoPersistServiceImpl (L99) + Beta/Gray/Tag/Migrate 变体 — 内嵌存储 (JRaft + RocksDB)
- **extrnal/** (5, 源码目录名原文如此拼写): ExternalConfigInfoPersistServiceImpl 等 — 外置数据库 (MySQL 等)
- 同名接口双实现 — 配置切换 (nacos.core.persistence 面)
- **EmbeddedConfigDumpApplyHook** (embedded/EmbeddedConfigDumpApplyHook.java:40): 内嵌转储钩子
关键设计 (q2): **"双实现 = 部署形态可切换"** — 单机/集群内嵌 (embedded) vs 独立 DB (external); 接口不变实现换。 [模式: 双实现族]

### 3. 操作语义 — 原子与 CAS

场景: 并发更新的语义保证?
源码路径:
- **removeConfigInfoAtomic** (L194): 原子删除 (条件删除)
- **removeConfigInfoByIdsAtomic** (L202): 批量原子
- **updateConfigInfoCas** (L227): CAS 更新 (比对旧内容)
- **ConfigOperateResult** (repository 面): 操作结果载体
关键设计 (q3): **"原子/CAS = 并发控制"** — 条件删除与 CAS 更新防并发覆盖; 与 NC-2 publishConfigCas 两端呼应。 [模式: 原子操作]

### 4. 测试与行为锚

场景: 持久化边界?
源码路径:
- 测试: ConfigInfoPersistServiceTest (config test)
- 锚: ConfigOperateResult 语义
关键设计 (q1): **"结果载体 = 操作可判定"** — 成功/失败/未变更三态。 [模式: 结果载体]
