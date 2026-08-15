# hq14 跨代理文件协调(File State Registry)— 产品②"并发写保护"蓝本

> 项目:Hermes(tools/file_state.py 332 行 + tools/file_tools.py 接入 + tools/delegate_tool.py 提醒 + 进程级单例)
> 假设:并发子代理(同进程同文件系统)碰同一文件会互相覆盖——Hermes 用进程级注册表跟踪"谁读过/谁最后写"防 stale 写,是"并发写保护"的完整样本。
> 结论:✅ 成立——读戳/最后写者/每路径锁/三分类 stale 检测/子代理提醒全具备,产品②"多 agent 并发写"直接蓝本。

---

## 一、架构全景:为什么需要文件状态注册表

```
问题:并发子代理碰同一文件 → 破坏性编辑。
     单 agent 路径重叠检查(_should_parallelize_tool_batch)抓"同一批内重叠";
     本模块抓跨代理案例:子代理 B 写了一个 A 已读的文件,A 下次写会用
     stale 内容覆盖 B 的修改。

┌────────────────────────────────────────────────────────────┐
│ 进程级单例 FileStateRegistry(tools/file_state.py:59)       │
│  per-agent 读戳:{task_id: {path: (mtime, read_ts, partial)}}│
│  全局最后写者:{path: (task_id, write_ts)}                  │
│  每路径 threading.Lock(read→modify→write 临界区)           │
├────────────────────────────────────────────────────────────┤
│ 三个公开钩子(file 工具调用):                              │
│  record_read(task_id, path, partial)——read_file 后        │
│  note_write(task_id, path)——write_file/patch 成功后       │
│  check_stale(task_id, path)——write_file/patch 前(警告)    │
│  lock_path(path)——锁整个 read→modify→write 块             │
├────────────────────────────────────────────────────────────┤
│ 提醒:writes_since(子代理完成时 parent 的"我读过的被改了"提醒)│
└────────────────────────────────────────────────────────────┘
```

**与 _read_tracker 的区别**:file_tools.py 的 _read_tracker 是 per-task 的(连续读循环检测,不同关注点);本模块是跨 agent 的。

---

## 二、设计 1:读戳(partial 语义)

**位置**:`file_state.py:93-112`(record_read)+ `44-48`(ReadStamp)

```
ReadStamp = (mtime, read_ts, partial)
partial=True 条件(read_file 接入,tools/file_tools.py:1964):
  offset > 1(窗口读)或结果 truncated(大文件 limit 未覆盖全)
  → 部分视图后的写应警告,让模型全量重读

record_read:
  mtime 缺失 → os.path.getmtime(失败 → return,不记录)
  _reads[task_id][path] = stamp;超 _MAX_PATHS_PER_AGENT(4096)按插入序丢最旧
```

**正确性价值**:
1. 读戳带 mtime——不靠"读过就安全",靠"读过时的版本"
2. partial 标记——窗口读/截断读 ≠ 全量读,写前应重读全量

**产品④映射**:知识库读取版本记录——"读过哪个版本"比"读过没有"精确;部分读取必须标记。

## 设计 2:最后写者(全局视图)

**位置**:`file_state.py:114-140`(note_write)

```
note_write(task_id, path):
  _last_writer[path] = (task_id, now);cap 4096
  ★ 写者自己的读戳同时刷新:(mtime, now, False)
    ——写 = 隐式读(agent 现在知道当前内容,后续自写不误报)

写后接入(tools/file_tools.py:2252):写成功才 note_write(错误不刷新)
```

**正确性价值**:"写即读"防自写误报——连续写同一文件不触发 false stale。

**产品④映射**:知识库"写入者刷新自己视图"——写完即知当前态,不因自己写而警告自己。

## 设计 3:stale 检测三分类(按严重度)

**位置**:`file_state.py:142-215`(check_stale)

```
三分类(按严重度):
1. 兄弟子代理修改(after 本 agent 最后读):
   last_writer.tid != task_id 且 writer_ts > read_ts
   → "modified by sibling subagent X at T — after your last read at R.
      Re-read the file before writing."
   (若从未读:该代理从未读但兄弟写 → "never read it" 变体)
2. 外部/未知修改(mtime 漂移):
   current_mtime != read_mtime → "modified since you last read it on disk
   (external edit or unrecorded writer). Re-read."
3. 部分读:
   stamp.partial → "last read with offset/limit pagination (partial view).
   Re-read the whole file before overwriting it."
   ——★ 与 mtime 检查**独立**(两个 if 非 elif,file_state.py:195-206):
     文件没变但你只读过窗口视图,写前仍应全量重读
   (优先级:兄弟写(1) > mtime 漂移(2) > partial(3)——每个都 return 短路)
4. (3b) 从未读 + 无写记录 → 净新文件/首次触碰,交给现有逻辑(None)
   ——文件不存在(mtime OSError)→ 创建不 stale(None)

返回 None = 安全;不 raise——调用者决定阻止还是警告

⚠ 测试缺口:partial 警告路径无专门测试(grep tests/tools/test_file_state_registry.py
无 partial 用例)——三分类中唯一缺测的一类,产品实现须补。
```

**正确性价值**:
1. 兄弟写 > mtime 漂移 > partial——按信息价值排序,最具体者赢
2. 净新文件/不存在不警告(创建语义)
3. 警告而非阻止(模型决定)——与 GoalGate"门不判定"同哲学

**产品④映射**:知识库并发写检测——"谁在我读后改了"是最高价值警告,且警告驱动重读。

## 设计 4:每路径锁(临界区串行化)

**位置**:`file_state.py:70-90`(lock_path)+ `66-67`(双锁)

```
lock_path(path):per-path threading.Lock,同路径串行,异路径并行
  ——read→modify→write 全块包锁(写前 check + 写 + 写后 note 都在锁内)

双元锁:
  _meta_lock 守卫 _path_locks dict(锁创建)
  _state_lock 守卫 _reads + _last_writer(数据)

接入(tools/file_tools.py:2227):
  with file_state.lock_path(_resolved):
      cross_warning = check_stale(...)
      stale_warning = _check_file_staleness(...)   # per-task 层
      cwd_warning = _path_resolution_warning(...)  # 工作树 cwd bug
      effective_warning = cross_warning or stale_warning or cwd_warning
      → 三警告优先级:跨代理 > per-task > cwd

测试:
- test_lock_path_serializes_same_path(同路径串行)
- test_lock_path_is_per_path_not_global(异路径并行)
```

**正确性价值**:
1. 临界区内 check+write+note 原子——锁内判定锁内更新
2. 三警告合并单一 _warning 字段(跨代理最具体)
3. 元锁/状态锁分离——锁创建不阻塞数据读

> ★ review 补深(2026-08-15 深度 review 发现):
> - **patch 多文件锁排序获取(防死锁)**:file_tools.py:2347-2352 用 ExitStack 按
>   _resolved_paths 排序序 enter_context 每路径锁——多线程各持一锁等另一锁的
>   循环等待 = 死锁,排序获取破循环(经典锁序纪律)。单路径退化为一把锁,
>   空列表 no-op。
> - **patch 双模式**:replace(单文件 old/new)与 patch(V4A 补丁,重写头到
>   绝对路径——相对头会按 shell cwd 重解析落地到别处,worktree-cwd bug);
>   多文件警告 " | " 连接(不止单 _warning)。
> - **patch 后清理**:成功补丁重置连续失败计数器(_reset_patch_failures——
>   未来同路径失败从零开始升级)。

**产品④映射**:知识库写临界区——check→write→note 原子;多警告合并最高信息价值;
**多文件操作必须排序取锁(死锁纪律)**。

## 设计 5:子代理完成提醒(writes_since)

**位置**:`file_state.py:218-242`(writes_since)+ `tools/delegate_tool.py:2946/2983`

```
writes_since(exclude_task_id, since_ts, paths):
  → {writer_task_id: [paths]}——since_ts 后、非本人、在我读过的路径上的写

delegate_tool 使用(三个调用点):
1. 父代理提醒(delegate_tool.py:2946):writes_since(parent_task_id, wall_start,
   parent_reads_snapshot)→ 有 → 追加:
   "[NOTE: subagent modified files the parent previously read — re-read
   before editing: <paths[:8]>...]"
   ——检查"任意非父 task_id 的写"(含嵌套 orchestrator→worker 链的传递写)
2. 子代理可观测载荷(delegate_tool.py:2983):known_reads(child)[:40] +
   writes_since("", wall_start, [])(exclude 空串 = 不过滤)→ 过滤出本子代理
   自己的写 [:40] → TUI overlay 细节窗 + accordion(可选字段,缺失优雅降级)
3. 父代理读快照(delegate_tool.py:2533):known_reads(parent_task_id)
   ——委派时快照父读过的路径,供 #1 的 parent_reads_snapshot 用
```

**正确性价值**:子代理返回时主动提醒父代理"你读过的文件被改了"——在编辑前预防,不是编辑时才发现。

**产品④映射**:委派结果回注的知识提醒——"你的依赖变了"随结果送达(与 hq6 委派摘要预算互补)。

## 设计 6:容量上限 + 熔断开关

**位置**:`file_state.py:50-56`(上限)+ `269-271`(_disabled)+ `280-291`(_cap_dict)

```
- _MAX_PATHS_PER_AGENT = 4096 / _MAX_GLOBAL_WRITERS = 4096
  ——长会话不无限累积状态;溢出按插入序丢最旧(dict 保序 PY>=3.7)
- HERMES_DISABLE_FILE_STATE_GUARD=1 → 全部方法 no-op
  (每次调用重读 env,测试可 monkeypatch)
- record_read/note_write 的 mtime 失败 → 静默 return(不炸)
- check_stale 不 raise;调用者决定

测试:
- test_kill_switch_env_var(熔断)
- test_net_new_file_no_warning(净新文件)
```

**正确性价值**:
1. 状态有界(4096/agent + 4096 全局)——防无限累积
2. 熔断开关——异常环境可整体关闭守护
3. 失败静默——守护层失败不打断主流程

**产品④映射**:知识库守护层的有界性 + 可熔断性——状态有界、失败静默、可整体关。

---

## 三、与四项目对比(并发写保护)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes file_state |
|------|----|----------|----------|-----|-------------------|
| 并发写检测 | — | — | file-mutation CAS | — | **读戳 + 最后写者三分类** |
| 临界区 | — | 事务 Prepare/Commit | KeyedMutex 锁内 CAS | — | **每路径锁(同路径串行异路径并行)** |
| 写前警告 | — | NeedsApproval | CAS 失败重试 | — | **三分类警告(兄弟写/mtime/partial)** |
| 写后刷新 | — | — | — | — | **note_write 刷新自己读戳(写即读)** |
| 委派提醒 | — | — | — | — | **writes_since 随子代理结果送达** |
| 熔断 | — | — | — | — | **env 开关全 no-op** |

**结论**:产品"多 agent 并发写"参考 = Hermes file_state(读戳/最后写者/每路径锁/委派提醒)+ OpenCode file-mutation CAS(写防覆盖)+ Reasonix 事务(原子发布)。**Hermes 是"写前检测 + 警告驱动",OpenCode 是"写时 CAS",Reasonix 是"事务回滚"——三层互补**。

---

## 四、面试弹药

1. **"单代理路径重叠 vs 跨代理"**:_should_parallelize_tool_batch 抓同批内重叠;file_state 抓跨子代理(进程级单例)——两个守护补不同面
2. **"写即读"**:note_write 刷新自己读戳——连续自写不误报 false stale
3. **"三分类按信息价值"**:兄弟写 > mtime 漂移 > partial——最具体的警告赢,但都驱动"重读"
4. **"警告不是阻止"**:check_stale 返回警告,调用者决定——模型读到警告自纠(与 GoalGate 门不判定同哲学)
5. **"partial 读是半信息"**:offset>1 或截断 = 窗口视图——写前应全量重读(写会用残缺视图覆盖)
6. **"子代理完成提醒"**:writes_since 在委派结果里带"你读过的被改了"——编辑前预防而非编辑时才发现

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 读戳(partial 语义) | 知识库读取版本记录(读过哪个版本) |
| 最后写者 | 全局写视图(谁改了) |
| 三分类 stale | 并发写检测按信息价值排序 |
| 每路径锁 | 临界区串行化(同路径/异路径并行) |
| writes_since 提醒 | 委派结果带"依赖变了" |
| 容量上限 + 熔断 | 守护层有界 + 可关 + 失败静默 |

> 覆盖设计数:6(设计 1-6)
> 测试契约:test_file_state_registry.py(7:读后检查/兄弟写 stale/同路径串行/异路径并行/熔断/兄弟写经 handler 表面化/净新文件)+ test_file_staleness.py(per-task 关联)
> 接入点:file_tools.py:1965(record_read)/2227(lock_path)/2230(check_stale)/2252(note_write)/2355+2367+2425(patch 路径)/delegate_tool.py:2946+2983(writes_since)
> 常量:_MAX_PATHS_PER_AGENT=4096/_MAX_GLOBAL_WRITERS=4096/HERMES_DISABLE_FILE_STATE_GUARD 熔断
