# q16 — Snapshot(深度版:git 树内容寻址 + 选择性恢复)

> 域:②执行(快照)+ 回滚 | 文件:core/src/snapshot.ts(266)+ session/revert.ts(121)+ core/src/pty/+ pty-ticket
> review 轮次:2 轮(源码全文 + 测试抽查)

---

## 假设

Snapshot 不是自研内容寻址存储,而是**复用 git 的对象库做快照**:每个快照 = 一个 Git TreeID,差异/恢复/预览全部走 git tree 操作。这是"不发明轮子"的经典工程决策。

## 验证

### 1. 存储设计(设计 1:git 树作为快照)

```ts
// snapshot.ts:94-122
gitDirectory = global.data/snapshot/{projectID}/{hash(worktree)}  ← 私有 git 仓库(每项目每 worktree)
repository():HEAD 存在 → 打开;否则 git.repo.create({ worktree, gitDirectory, seed: source })
  —— seed = 源仓库(从已有仓库种子化,快速建立历史)
capture(snapshot.ts:129-144):
  enabled = git 仓库 && config snapshots !== false
  git.tree.capture({ scopes: [location 相对路径], ignores: source, maximumUntrackedFileBytes: 2MB })
  → ID = Git.TreeID
  → 失败仅 logWarning(尽力而为,非致命)
```

**设计要点**:快照 ID 就是 git tree 哈希——内容寻址天然去重;ignore 规则复用源仓库(git index ignored)。

### 2. 六操作(设计 2:capture/files/diff/preview/restore/checkout)

```ts
// snapshot.ts:43-82 Interface
files(from, to):两树差异路径(不加载内容)+ 过滤 ignored
diff(from, to, context, paths):unified diff
preview(files: Map<path, ID>):选择性恢复的预览(不修改工作树,生成 diff)
restore(files: Map<path, ID>):选择性恢复;缺失于选中树的路径 = 删除;map 外不动
checkout(snapshot):整个树切换(全量)
// plan(snapshot.ts:178-187):路径穿越检查(FSUtil.contains(worktree, absolute)),越界 → Error
```

### 3. 错误契约(设计 3:operation 化)

```ts
// snapshot.ts:18-22
Error = { operation: "capture"|"files"|"diff"|"preview"|"restore", message, cause }
// failure(snapshot.ts:250-257):已有同 operation 错误透传,否则包装
// noopLayer(238-248):全空实现(未启用时注入)
```

### 4. 与 Runner 集成(设计 4:Step 快照对)

```ts
// runner/llm.ts:217,318-336
startSnapshot = snapshots.capture()(provider turn 前)
endSnapshot = snapshots.capture()(step settlement 时)
files = snapshots.files({from: start, to: end})  ← Step.Ended 事件携带
// 用途:每步的"改了哪些文件"可审计/可回滚
```

### 5. Revert(设计 5:stage/clear/commit)

```ts
// session/revert.ts(121 行)+ session.ts:433-453
stage: { session, messageID, files? } → 用该消息关联的快照构建 Revert.State
clear: 清除回滚状态
commit: 应用回滚(事件驱动:revert.staged/cleared/committed 投影)
```

### 6. PTY(设计 6:简述)

```ts
// core/src/pty/:PTY 抽象 + pty-ticket;PTY Environment(server 层合并 host overlay + Core 强制 TERM/OPENCODE_TERMINAL)
// CONTEXT.md:138:"PTY creation merges caller values, then the host overlay, then Core-forced terminal invariants"
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | git 树内容寻址(私有仓库 + seed) | snapshot.ts:94-122 | ②快照基础设施(不发明轮子) |
| 2 | 六操作(capture/files/diff/preview/restore/checkout) | snapshot.ts:43-82 | ②回滚/审计 |
| 3 | operation 化错误 + noopLayer | snapshot.ts:18-22,238-248 | ②失败语义 |
| 4 | Step 快照对(开始/结束 files diff) | runner/llm.ts:217,318-336 | ③每步可审计 |
| 5 | revert 三操作(事件驱动) | session/revert.ts | ②用户回滚 |
| 6 | PTY env 合并顺序(调用方→host→Core 强制) | CONTEXT.md:138 | ②终端隔离 |

## 面试弹药

- "快照 = git tree":内容寻址天然去重 + ignore 规则复用 + seed 从源仓库——比自研 CAS 少一个量级的工程
- "快照尽力而为":capture 失败仅 logWarning(非致命)——快照是增强不是契约
- "选择性恢复":files map 指定路径;缺失 = 删除;map 外不动;preview 先看 diff 再动
- "每步快照对":provider turn 前后各拍一次,Step.Ended 携带 changed files——"这步改了啥"可审计

## 待深挖

- [ ] git.ts 的 tree capture/restore 实现(最大未跟踪字节限制逻辑)
- [ ] revert 状态机(stage→commit 的边界)
