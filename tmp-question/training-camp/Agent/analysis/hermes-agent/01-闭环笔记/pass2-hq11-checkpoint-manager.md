# hq11 文件系统检查点(Checkpoint Manager)— 产品②"章节回滚"蓝本

> 项目:Hermes(tools/checkpoint_manager.py 1,953 行 + agent/tool_executor.py 写前拦截 + agent/conversation_loop.py new_turn + agent/agent_init.py 接线 + gateway/run.py 自动维护)
> 假设:文件变更前的自动快照 = 章节回滚的基础;Hermes 用单一共享 git 影子仓库(跨项目对象去重)实现透明快照,是"写前自动检查点"的完整样本。
> 结论:✅ 成立——影子仓库架构/回合去重/写前拦截/回滚可撤销/保守孤儿判定/容量治理全具备,产品②"章节回滚"直接蓝本。

---

## 一、架构全景:为什么需要透明检查点

```
这不是工具——LLM 永远看不到它。透明基础设施,由 checkpoints 配置或 --checkpoints CLI 开关控制。

┌────────────────────────────────────────────────────────────┐
│ 接入:写前拦截(tool_executor.py:736-753)                    │
│   write_file/patch → _ensure_file_checkpoint(路径解析→项目根→快照)│
│   terminal(破坏性命令)→ 快照 cwd                            │
├────────────────────────────────────────────────────────────┤
│ 周期:new_turn() 每回合重置去重(每目录每回合至多一张快照)     │
├────────────────────────────────────────────────────────────┤
│ 存储:~/.hermes/checkpoints/store/ 单一共享 bare 仓库        │
│   refs/hermes/<hash16> 每项目分支尖                        │
│   indexes/<hash16> 每项目 git index(并发隔离)              │
│   projects/<hash16>.json 元数据(workdir/created/last_touch/卷身份)│
├────────────────────────────────────────────────────────────┤
│ 维护:prune_checkpoints(孤儿/过期/容量)+ 自动 .last_prune   │
└────────────────────────────────────────────────────────────┘
```

**v1→v2 教训**:v1 每工作目录一个影子仓库,同项目多个 worktree 各存 ~40MB(~500MB 总量重复存同一批 blob)。v2 单一共享仓库——git 内容寻址对象库跨项目/跨回合去重,新 worktree 成本近零。

**与 Reasonix checkpoint 的差异**:Reasonix 每轮快照(编辑前状态 + MsgIndex 边界)+ 回滚事务;Hermes 用 git 对象库做存储——"不发明 CAS,复用 git 对象库"(与 OpenCode 决策 #9"快照 = git 树"同哲学)。

---

## 二、设计 1:写前拦截(什么触发快照)

**位置**:`agent/tool_executor.py:736-753`

```
if function_name in {"write_file", "patch"} and agent._checkpoint_mgr.enabled:   # tool_executor.py:734
    _ensure_file_checkpoint(agent, function_name, function_args, effective_task_id)
if function_name == "terminal" and agent._checkpoint_mgr.enabled:                 # :747
    if _is_destructive_command(command):  # agent/tool_dispatch_helpers.py:92   # :748
        ensure_checkpoint(cwd, f"before terminal: {command[:60]}")

_ensure_file_checkpoint(路径解析关键):
  - 相对路径按任务实时 cwd 解析(_resolve_path_for_task)——文件工具对相对路径的解析
    与 Hermes 进程 cwd 可不同(尤其 Docker),必须先走同一路径管线
  - get_working_dir_for_path:向上找项目标记(.git/pyproject.toml/package.json/
    Cargo.toml/go.mod/Makefile/pom.xml/.hg/Gemfile)→ 项目根快照
```

**正确性价值**:
1. 快照目标 = 文件工具将要变更的同一路径(解析管线一致,防快错目录)
2. 写文件必快照;terminal 只对破坏性命令快照(启发正则)
3. LLM 不可见——不占工具 schema、不占上下文

**产品④映射**:章节写书前自动快照——写前拦截点是"变更前的真相"捕获点。

## 设计 2:回合去重(每目录每回合至多一张)

**位置**:`checkpoint_manager.py:741-743`(new_turn)+ `772-775`(去重)

```
new_turn():_checkpointed_dirs.clear()——回合开始重置
ensure_checkpoint:abs_dir in self._checkpointed_dirs → skip;否则 add 后 _take

接入:conversation_loop.py:1721 每轮迭代 new_turn()
```

**正确性价值**:批量工具调用(如 write 10 文件)只取一张快照——快照成本有界;但跨回合每次都新快照(每回合变化可回滚)。

**产品④映射**:章节内多次写 → 一章一张基线快照 + 增量;回合 = 快照粒度。

## 设计 3:单一共享影子仓库 + git 隔离

**位置**:`checkpoint_manager.py:239-276`(_git_env)+ `421-487`(_init_store)

```
存储:store/(bare)+ refs/hermes/<hash16> + indexes/<hash16> + projects/<hash16>.json

_git_env 隔离策略(关键):
  GIT_DIR=store + GIT_WORK_TREE=workdir + GIT_INDEX_FILE=每项目 index
  GIT_CONFIG_GLOBAL=<os.devnull>——忽略 ~/.gitconfig(用户 commit.gpgsign=true/
    signing hooks/credential helpers 会破坏后台快照,更糟:pinentry GUI 窗口
    会话中弹出)
  GIT_CONFIG_SYSTEM=<os.devnull> + GIT_CONFIG_NOSYSTEM=1(老 git 兜底)
  保留 HOME(不重写——改了会改变隔离变量要隐藏的 .gitconfig 位置)

_init_store:
  git init --bare 不能用 _run_git(它总设 GIT_WORK_TREE,bare 拒绝)
    → 裸 subprocess 只带隔离 env
  每 store 配置:user.email=hermes@local/user.name=Hermes Checkpoint/
    commit.gpgsign=false/tag.gpgSign=false/gc.auto=0
  info/exclude 写 DEFAULT_EXCLUDES(50 条:依赖/缓存/venv/VCS/二进制/媒体/密钥/日志)
```

**正确性价值**:
1. 用户的 git 配置永不泄漏进影子仓库(gpgsign/pinentry 是真实事故类)
2. 每项目独立 index → 并发项目不竞争同一 index
3. exclude 让快照聚焦源码,不吃依赖/产物/密钥

**产品④映射**:知识库快照存储 = 复用 git 对象库 + 全隔离配置——"不发明 CAS,复用 git"(OpenCode 决策 #9 同哲学)。

## 设计 4:快照算法(seed index → add → diff → commit-tree → update-ref)

**位置**:`checkpoint_manager.py:998-1130`(_take)

```
1. _init_store + _touch_project(last_touch 更新,created_at 保留)
2. 大小守卫:_dir_file_count > 50,000 跳过(防巨型目录拖慢)
3. 每项目 index:已有 → read-tree ref 尖(防 stale 路径累积);无 → 清空新建
4. git add -A(带 index_file,timeout×2)
5. 超限文件过滤:_drop_oversize_from_index(max_file_size_mb 默认 10MB,
   ls-files -z → stat → rm --cached,200 一批)
6. 变更检测:diff-index --cached --quiet ref(rc=1 = 有变化)→ 无变化跳过
7. write-tree → commit-tree(-p 父, --no-gpg-sign)→ update-ref(带旧值条件更新)
8. _prune(max_snapshots=20)+ _enforce_size_cap(500MB)

关键细节:
- 比较用 ref 尖而非 HEAD——bare 仓库 HEAD 指向不存在的分支,diff HEAD
  永远"全新文件"
- update-ref 带旧值(ref_commit)条件更新——CAS,防并发覆盖
- commit 消息 = reason("before write_file"/"before terminal: ..."/"pre-rollback snapshot")
```

**正确性价值**:
1. 无变化不提交(垃圾快照零成本)
2. CAS 更新防多进程并发写同一项目 ref
3. 大文件过滤(数据集/权重/日志)让快照只吃源码

**产品④映射**:章节快照算法——基线 + 增量 + 无变化跳过 + 大文件排除。

## 设计 5:回滚可撤销(pre-rollback snapshot)

**位置**:`checkpoint_manager.py:919-974`(restore)

```
restore(working_dir, commit_hash, file_path=None):
  1. _validate_commit_hash(防 git 参数注入:禁 '-' 开头 + 4-64 hex)
  2. _validate_file_path(防路径穿越:相对路径 + resolve 后必须在 workdir 内)
  3. cat-file -t 验证 commit 存在
  4. ★ 先取 pre-rollback snapshot("pre-rollback snapshot (restoring to X)")——撤销的撤销
  5. git checkout <hash> -- <target>(index_file,timeout×2)
  6. ★ index 恢复:read-tree ref(重置暂存树回项目最后快照)
     ——diff/restore 都用 add -A 暂存当前态比较;不重置则 index 与 ref 漂移,
       下次 _take 的 seed 基线错(把已比较过的差异当新变化)
  7. 返回 restored_to/reason/directory

输入验证测试:
- test_restore_rejects_argument_injection('--patch' 等)
- test_restore_rejects_invalid_hex_chars
- test_restore_file_path_confined_to_working_dir('../x' 穿越)
```

**正确性价值**:**回滚本身可回滚**——回滚前先快照当前状态,"撤销的撤销"(与 Reasonix Undo 同哲学,更简单:git 对象天然支持)。

**产品④映射**:章节回滚 = 先存"回滚前"快照再 checkout——错误回滚可恢复。

> ★ review 补深(2026-08-15 深度 review 发现):
> - **session_diff 是近似不是账本**(checkpoint_manager.py:897-900 诚实声明):
>   快照是持久 per-project ref,最旧*保留*快照可能早于本会话(或修剪后晚于真实起点)
>   ——"Hermes 改了什么"是近似,"精确会话账本"需要独立记录。产品章节 diff
>   同样要声明近似性,不能假装精确。
> - **index 恢复是 diff/restore 的正确性关键**(见设计 5 步骤 6):
>   add -A 暂存比较后必须 read-tree ref 重置,否则下次 _take 的 seed 基线错。

## 设计 6:孤儿保守判定(挂载卸载 ≠ 删除)

**位置**:`checkpoint_manager.py:1383-1468`(_workdir_is_observably_gone)+ `490-520`(_volume_evidence)

```
问题:Path.exists() False = 目录被删 OR 存储没挂上(拔盘/断 VPN/容器缺 bind-mount/
     Windows 离线映射盘)。孤儿清理删整个项目快照史——把"瞬态挂载状态"当
     "删除" = 无人值守丢弃用户恢复点。

三步佐证(缺一不判孤儿):
1. 父目录必须存在(父也不在 = 卷不在 = 什么都不知道)
2. 父目录身份必须匹配(st_dev/st_ino 与项目存活时记录的卷身份一致;
   卸载 = 挂载点显示 underlay 目录,不同 (dev,ino) → 是卷分离不是删除)
3. 身份确认的父目录必须携带信息(空目录 = 卸载挂载点的签名;
   非空 = 观察到有内容但不含项目;ismount = 卷确在附着且项目确不在)

_volume_evidence:项目存活时记录父目录 (st_dev, st_ino) 到 projects/<hash>.json;
  st_dev/st_ino 为零(Windows 无文件 ID 的文件系统/网络共享)→ 不记录
  → 无身份记录的旧元数据 + require_parent_identity=True → 保守不判孤儿

真正被遗弃的项目仍被 stale 规则回收(基于 last_touch,不做文件系统探测)
```

**正确性价值**:孤儿判定是"必须能正面观察到删除"的强标准——"不确定绝不删除";真正废弃由 retention 兜底。与 delivery/lifecycle 的"_owner_alive 保守"同哲学。

**产品④映射**:知识库孤儿结论/章节的删除判定——挂载/同步态歧义不删除,过期兜底。

## 设计 7:容量治理(三保险)

**位置**:`checkpoint_manager.py:1178-1331`(_prune/_enforce_size_cap)+ `1483-1757`(prune_checkpoints)+ `1760-1824`(maybe_auto_prune)

```
运行时:
- _prune:每项目保留最近 max_snapshots=20 个 commit,重链 + reflog expire + gc --prune=now
  (v1 的 _prune 是 no-op——只有 log 视图受限,loose 对象无限累积;v2 真正重写 ref)
- _enforce_size_cap:总容量 > 500MB → 按 ref 轮询丢最旧,每项目至少留 1 张,上限 20 轮防病态循环

维护(prune_checkpoints):
- 孤儿(三步佐证)+ stale(last_touch > retention_days=7)
- orphan_allowlist:预览确认绑定——预览后新变孤儿的跳过(防"用户确认前消失"被扫掉)
- 容量 pass:孤儿/stale 后仍超 → 逐项目丢最旧
- legacy-* 归档同 retention 清理;clear-legacy 手动清
- 自动:gateway 启动 maybe_auto_prune(24h 间隔 .last_prune 标记,幂等)
  ★ gateway 自动 sweep delete_orphans 故意永不启用(run.py:6646)——启动时 workdir
    缺失有歧义(删除 vs 卷未挂),无人值守不删孤儿;孤儿清理只经用户显式
    `hermes checkpoints prune`
```

**正确性价值**:
1. 每项目保留上限 + 总容量上限双保险
2. 自动维护保守(孤儿留给用户显式);显式清理可预览绑定(allowlist)
3. gc 后修复 bare 仓库 refs/branches 目录(git 2.34+ 要求,缺了 git add 全失败)

**产品④映射**:知识库容量治理——每域保留上限 + 总量上限 + 自动维护保守化 + 预览绑定删除。

## 设计 8:透明性纪律(LLM 不可见 + 永不抛)

**位置**:`checkpoint_manager.py:10-11`(不是工具)+ `749-781`(ensure_checkpoint)+ 各处 try/except

```
- 不是工具——LLM 从不看到它(工具 schema 零占用)
- ensure_checkpoint 永不抛:git 不可用(which 探测懒缓存)→ 禁用;目录过宽
  (根/home)→ 跳过;异常 → 记录返回 False(非致命)
- 所有 git 调用 _run_git 返回 (ok, stdout, stderr) 不抛
- gateway 自动维护失败 → debug 日志跳过
```

**产品④映射**:可靠性基础设施的透明性——不在模型可见面、失败不打断主流程(与 delivery/lifecycle "主数据优先"同族)。

---

## 三、与四项目对比(章节回滚)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes checkpoint |
|------|----|----------|----------|-----|-------------------|
| 快照触发 | — | 每轮编辑前 | Step 快照对(git 树) | — | **写前拦截(write/patch/破坏性 terminal)** |
| 存储 | — | BlobStore(SHA-256) | git 树 | — | **单一共享 git bare 仓库(跨项目去重)** |
| 回滚 | 分支三操作 | 回滚事务(意图持久化+双阶段) | git 恢复 | — | **git checkout + pre-rollback 快照(撤销的撤销)** |
| 粒度 | — | 每轮 MsgIndex 边界 | 每 Step | — | **每回合每目录一张(去重)** |
| 孤儿判定 | — | — | — | — | **三步佐证(父存在/身份匹配/携带信息)** |
| 容量 | — | Blob GC | — | — | **20 张/项目 + 500MB 总量 + 自动保守化** |
| 隔离 | — | — | — | — | **GIT_CONFIG 全隔离(gpgsign/pinentry 事故类)** |

**结论**:产品"章节回滚"参考 = Hermes checkpoint(写前自动快照 + 共享仓库 + 可撤销回滚 + 保守孤儿)+ Reasonix 回滚事务(多文件原子性)+ OpenCode Step 快照对(验收证据)。**Hermes 与 OpenCode 同"复用 git"哲学,但 Hermes 是自动透明的(Hermes 每回合、OpenCode 每 Step 显式)**。

---

## 四、面试弹药

1. **"透明不是工具"**:LLM 永远看不到检查点——可靠性基础设施不占 schema/上下文
2. **"单一共享仓库省 500MB"**:v1 每 worktree 一个影子仓库重复存 blob;git 内容寻址跨项目去重,新 worktree 近零成本
3. **"gpgsign 事故类"**:用户 ~/.gitconfig 的 commit.gpgsign/signing hooks 会破坏后台快照,更糟 pinentry GUI 会话中弹出——GIT_CONFIG_GLOBAL=devnull 全隔离
4. **"回滚可回滚"**:pre-rollback snapshot = 撤销的撤销(git checkout 前先快照)
5. **"孤儿三步佐证"**:拔盘 ≠ 删除——父存在 + (st_dev,st_ino) 身份匹配 + 携带信息;不确定绝不删,stale 兜底
6. **"gateway 自动维护永不删孤儿"**:启动时 workdir 缺失有歧义,无人值守删孤儿 = 数据丢失;孤儿清理只经用户显式命令 + allowlist 预览绑定

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 写前拦截 | 章节写书前自动快照(write/patch/破坏性 terminal) |
| 回合去重 | 章节内多次写 → 一章一张基线快照 |
| 共享仓库 + git 隔离 | 复用 git 对象库 + 全配置隔离(不发明 CAS) |
| 快照算法 | 基线 + 增量 + 无变化跳过 + CAS + 大文件过滤 |
| pre-rollback 快照 | 章节回滚可撤销(撤销的撤销) |
| 孤儿三步佐证 | 挂载/同步歧义不删除,stale 兜底 |
| 容量三保险 | 每项目 20 张 + 总量 500MB + 自动维护保守化 |
| 透明性纪律 | LLM 不可见 + 永不抛 + 失败不打断 |

> 覆盖设计数:8(设计 1-8)
> 测试契约:tests/tools/test_checkpoint_manager.py(40 用例:共享仓库/项目哈希/去重/回滚/输入验证/隔离/gpgsign/孤儿/retention/allowlist)+ test_checkpoints_prune.py(预览绑定)+ test_tool_executor_checkpoint_paths.py(路径解析)
> 接入点:tool_executor.py:734(write/patch)/748(terminal 破坏性)/conversation_loop.py:1721(new_turn)/agent_init.py:1626(接线)/gateway/run.py:6644(自动维护)
