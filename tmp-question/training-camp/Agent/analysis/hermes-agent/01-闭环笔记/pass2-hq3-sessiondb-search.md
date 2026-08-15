# hq3 会话存储与搜索(SessionDB + FTS5)— 产品④"大库规模知识库"蓝本

> 项目:Hermes(hermes_state.py 11,605 行 + hermes_state_search.py 2,492 行 + hermes_state_common.py 675 + hermes_state_schema.py 1,297 + hermes_state_portability.py 714 + tools/session_search_tool.py 1,161)
> 假设:Hermes 会话存储解决了"大库规模"问题(WAL 并发/崩溃自愈/FTS5 检索/跨进程锁),是产品④知识库规模化的参考(远超 Pi/Reasonix 轻量存储)。
> 结论:✅ 成立——GB 级库的完整工程:读写并发、自愈分级、全文检索降级链、锚定视图。

---

## 一、架构全景:SQLite 的工程极限运用

```
┌────────────────────────────────────────────────────────────┐
│ 写入:_execute_write(BEGIN IMMEDIATE + jitter 重试)          │
│   例行 20s / 转录 60s / 心跳 0.5s                           │
└──────────────┬─────────────────────────────────────────────┘
               │ WAL 模式(跨进程读写并发)
┌──────────────▼─────────────────────────────────────────────┐
│ 读取:_read_ctx(有界连接池 8 + permit 信号量)                │
└──────────────┬─────────────────────────────────────────────┘
               │ FTS5 trigger 同步 + 增量 merge
┌──────────────▼─────────────────────────────────────────────┐
│ 搜索:BM25 + 排序 + 锚定视图(FTS5→CJK→trigram→LIKE 降级链)  │
└──────────────┬─────────────────────────────────────────────┘
               │
┌──────────────▼─────────────────────────────────────────────┐
│ 自愈:连接重开/FTS fail-open/租约锁/离线修复管线/零值隔离    │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:写路径 — BEGIN IMMEDIATE + jitter 重试

**位置**:`hermes_state.py:3533-3703`(_execute_write)

1. **BEGIN IMMEDIATE**(非 DEFERRED):事务开始即取写锁,竞争立即暴露
2. **时间预算而非尝试次数**(#74478):`_WRITE_PATIENCE_S=20` 例行/`_TRANSCRIPT_WRITE_PATIENCE_S=60` 转录/`_ACTIVITY_WRITE_PATIENCE_S=0.5` 心跳
3. **随机 jitter 退避**:20-150ms 起步,2s 后 250ms-1s——打破 SQLite 确定性退避的 convoy 模式
4. **错误分类处理**:
   - locked/busy → 重试
   - "no more rows available" → 消息级匹配(跨版本异常类不同)
   - "file is not a database" → 连接重开自愈(one-shot)
   - FTS 损坏 → 运行时重建(one-shot)→ 再失败 fail-open 分离索引
5. **周期性维护**:每 50 写 PASSIVE checkpoint;每 1000 写增量 FTS merge

**关键 1a:PASSIVE checkpoint 取代 TRUNCATE(#45383)**
- TRUNCATE 在大库(65K+ 页)独占锁 I/O 压力损坏 B-tree
- PASSIVE 不阻塞并发写、不截断 WAL(journal_size_limit 限界)

**产品④映射**:知识库日志写入并发——时间预算分级 + jitter 破 convoy。

## 设计 2:读路径 — 有界连接池 + 信号量

**位置**:`hermes_state.py:2803-2860` + `_get_read_conn`/_read_ctx

**教训驱动(#69678/#69567)**:
- 旧方案 per-thread 连接 → Starlette worker 每线程占一连接+两 fd → 256 RLIMIT_NOFILE 后 EMFILE,进程活着但全部失败,supervisor 重启-on-exit 不触发
- 新方案:有界 LIFO 池(`_READ_POOL_MAX=8`)+ **permit 信号量限制峰值描述符**(池只限空闲集)+ 非阻塞获取(拿不到降级写者锁)+ `_read_permit_exhausted` 可观测计数 + **时间戳回退**(EMFILE 瞬态,永久标志会把网关共享 SessionDB 全局降级)
- WAL 下读者借只读连接,永不排队在写者后

**产品④映射**:知识库检索并发隔离——读写分离 + 有界池 + 可观测降级。

## 设计 3:压缩锁 = 租约(跨进程)

**位置**:`hermes_state.py:5619-5750`

```
- holder + expires_at(TTL 默认 300s)
- 过期透明回收;结构化 holder 本地 pid 死 → 立即回收(不等满 TTL)
- 实现:单事务 DELETE-expired + INSERT OR IGNORE + SELECT 确认(原子)
- 释放 holder 检查 + 幂等
- fail open:锁子系统坏 → 跳过压缩(安全)
```

**正确性边界**:`_COMPRESSION_BUSY_WAIT_S=5`——压缩锁是**正确性边界**(非忙信号),超预算仍拒绝(防压缩旋转中落地 stale 回合,分裂 session 血缘)。测试:`test_compression_lease_blocks_non_owner_but_allows_owner_flush`。

**产品④映射**:知识库维护操作(章节压缩/索引重建)跨进程互斥——Fencing Token 思想的 SQLite 版。

## 设计 4:搜索路由降级链

**位置**:`hermes_state_search.py:1704-1790` + `_describe_search_path`

```
fts5(标准 BM25)→ fts_cjk(CJK bigram)→ trigram(3+ 汉字)→ like_scan(兜底)
```

- `_refresh_fts_stale_state()` 驱动(FTS stale → 自动降级 LIKE)
- **查询消毒** `_sanitize_fts5_query`(防 FTS5 语法注入)
- **排序归一**:sort 白名单(newest/oldest),其他回落到 rank-only
- **rewound 行默认排除**(用户收回的);**compaction 归档行默认包含**(#38763——压缩≠删除,压缩前全文依然可搜)
- **LIKE 兜底编译**:FTS 布尔子集(OR 分组/NOT 否定)+ 转义
- **慢查询日志**:>1000ms 记录 routing path("下次回归是一个 grep")

**产品④映射**:全书检索降级链 + 压缩归档章节依然可搜(压缩≠删除)。

## 设计 5:增量 FTS merge(大库维护取舍)

**位置**:`hermes_state_search.py:2423-2492`

**背景**:原 `'optimize'` 全量重写,单事务持写锁 9-18s(10GB 库)→ 耗尽竞争者重试耐心。

**新协议**:
- 正数 merge rank = 输出页近似预算 → 每条命令毫秒级
- `usermerge` 降到 2(默认 4 → 低层级永不合并,碎片索引不收敛)
- 每条命令独立隐式事务 → 命令间写锁释放,竞争进程可交错
- no-progress 信号:`total_changes` 增量 < 2
- 每 1000 写触发,每索引 ≤4 命令 × 500 页

**产品④映射**:知识库索引维护必须增量/有界/可交错——任何全量重写在 GB 级库是故障源。

## 设计 6:自愈与 fail-open(正确性分级)

**位置**:`hermes_state.py:3660-3900` + `1934-1996`

```
自愈链(写路径):
1. "file is not a database" → _reconnect_after_notadb(one-shot 重开重试)
2. FTS 损坏(trigger 同步写入时)→ _try_runtime_fts_rebuild(one-shot)
3. 不可修复 → _enter_fts_fail_open:
   - 原子:写 stale 标记 + 删全部 FTS trigger + commit
   - 顺序承载:trigger 移除后新行产生未知缺口 → 别的进程绝不能先重装 trigger 而不重建全行
   - 效果:规范表继续写,搜索临时 LIKE,下次打开重建
```

**失败语义**:`classify_persistence_error` + 明确消息("不是磁盘/权限损坏,是另一进程持锁超时——库本身健康")——**错误消息帮操作者免于错误排查方向**。

**离线修复**:preflight_db_writability(写前预检)+ repair_state_db_schema(备份+修复)+ `_cross_process_repair_lock`(防并发修复)+ 零值库隔离(quarantine 非删除)。

**产品④映射**:知识库自愈分级——主数据优先于派生索引(fail-open 分离 FTS)。

## 设计 7:锚定视图(检索上下文轻量加载)

**位置**:`hermes_state_search.py:975-1095`

```
一个 FTS5 命中 → 三切片:
- window:锚点周围 ±5(角色过滤,锚点本身永远保留)
- bookend_start:会话头 3 条 user/assistant(不与 window 重叠)
- bookend_end:会话尾 3 条

价值:长 session 任意位置命中 → 单次调用得"目标(开头)+解析(结尾)"
```

- 空内容消息跳过(tool-call-only 不挤占散文)
- 角色过滤可禁用

**产品④映射**:全书检索命中章节自动带"章首目标 + 章尾结论"。

## 设计 8:schema 单一真相源(SCHEMA_SQL)

**位置**:`hermes_state_common.py:250-442` + `hermes_state_schema.py`

```
- SCHEMA_SQL 是唯一真相源(Beets/sqlite-utils 模式):增列 → 启动 reconcile
- 表:sessions(含 handoff_state/pinned/archived/压缩故障计数)/
  messages(active/compacted 双标记)/session_model_usage/compression_locks/
  async_delegations(含 delivery_state 账本)/gateway_routing/state_meta
- _ensure_schema 在 scratch 库执行 SCHEMA_SQL 派生期望列 → 差异增列
- 版本迁移:SCHEMA_VERSION 阶梯迁移
```

**产品④映射**:知识库 schema 单一真相源 + 启动 reconcile——"增列自动生效"。

## 设计 9:AsyncSessionDB 门面

**位置**:`hermes_state.py:11591-11605`

```
class AsyncSessionDB:
    def __getattr__(self, name):
        async def _offloaded(*args, **kwargs):
            return await asyncio.to_thread(attr, *args, **kwargs)
```

- 泛型转发:每个调用 to_thread 卸载——阻塞 SQLite 永不冻结事件循环
- 审计确认无方法返回活 cursor/generator(防跨线程游标泄漏)

**产品④映射**:同步存储 + 异步门面,避免重写存储层。

## 设计 10:会话搜索工具(检索产品化)

**位置**:`tools/session_search_tool.py:848`(session_search)+ `_get_anchored_view`

- 血缘解析(_resolve_lineage:压缩父子/分支)
- 存储状态标注(_annotate_rebuild_status)
- 召回排序(_order_for_recall)
- 跨 profile 检索(_resolve_profile_db)

**产品④映射**:全书检索的产品化入口(血缘/状态/跨库)。

---

## 三、与 Pi/Reasonix 对比(存储决策输入)

| 维度 | Pi | Reasonix | Hermes |
|------|----|----------|--------|
| 存储 | SQLite(branch/lane/facts) | BlobStore/checkpoint | SQLite + WAL + FTS5 |
| 写入并发 | 单进程 | 单进程 | **跨进程(网关多 profile)** |
| 崩溃恢复 | findOpenOperations 三态 | 双阶段 checkpoint | **连接重开 + FTS fail-open + 离线修复** |
| 检索 | 无 | 内存 BM25 | **FTS5 + CJK + trigram + LIKE 降级链** |
| 规模 | 会话级 | 会话级 | **10GB 级(merge 协议为证)** |
| 正确性理论 | 事件溯源/不变量 | fail-closed | **租约锁 + fail-open 分级 + 原子修复顺序** |

**结论**:产品④存储层组合——Pi 事件溯源语义 + Hermes WAL/有界读池/租约/增量索引/自愈 + FTS5 降级链/锚定视图。

---

## 四、面试弹药

1. **"读路径用信号量上限而非池大小"**:池限空闲集,permit 限峰值描述符——EMFILE 根因是每线程一连接
2. **"TRUNCATE 在大库损坏 B-tree"**:65K+ 页独占锁 I/O 压力(#45383);PASSIVE + journal_size_limit
3. **"增量 merge 取代 optimize"**:9-18s 写锁 vs 毫秒级;usermerge 降到 2 让碎片收敛;no-progress 信号防空转
4. **"压缩锁是正确性边界"**:等待 5s 超预算仍拒绝(防分裂血缘)——租约 TTL + pid 死检
5. **"fail-open 的原子顺序"**:先 stale 标记+删 trigger+commit——顺序承载性
6. **"压缩归档行默认包含在搜索里"**:压缩≠删除,压缩前全文依然可搜(#38763)

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|----------|
| 时间预算写分级 | 关键写 60s/例行 20s/心跳 0.5s |
| jitter 重试 | 多进程写防 convoy |
| PASSIVE checkpoint | 大库维护不独占锁 |
| 有界读池 + permit | 检索并发防 fd 耗尽 |
| 压缩租约锁 | 章节压缩/索引重建互斥 |
| FTS 降级链 | 全书检索三级 + 慢查询日志 |
| 增量 merge | 索引维护有界可交错 |
| 自愈链 + fail-open | 主数据优先派生索引 |
| 锚定视图 | 命中带章首目标+章尾结论 |
| SCHEMA 单一真相源 | 增列自动生效 |
| Async 门面 | 同步存储+异步门面 |
| 搜索工具产品化 | 血缘/状态/跨库检索 |

> 覆盖设计数:14(设计 1-10 + 1a/2 子设计)
