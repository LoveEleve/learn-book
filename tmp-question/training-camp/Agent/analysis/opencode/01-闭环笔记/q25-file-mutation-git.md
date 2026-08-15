# q25 — FileMutation + Git(深度版:CAS 写入 + 仓库操作锁)

> 域:②执行(文件/版本控制,共享包) | 文件:core/src/file-mutation.ts(207)+ git.ts(987)+ core/test/(file-mutation/git).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

FileMutation = 文件写操作的唯一入口(create/write/CAS/remove),CAS 依赖"同进程锁内 比较+写入"的原子性;Git = 命令封装 + 仓库级互斥(7 组操作)。apply_patch 的 CAS 语义建立在 FileMutation 上(q19)。

## 验证

### 1. 五操作(设计 1:create/write/BOM/CAS/remove)

```ts
// file-mutation.ts:54-65
create:flag "wx"(排他)→ AlreadyExists → TargetExistsError;NotFound → ensureDir 重试
write:覆盖写(含 dirs)
writeTextPreservingBom:保留现有 UTF-8 BOM + 至多一个 BOM(splitBom/joinBom,file-mutation.ts:175-187)
writeIfUnchanged:读当前 → sameBytes(expected)→ 写;不等 → StaleContentError
remove:NotFound → existed=false(幂等)
```

### 2. CAS 原子性(设计 2:锁内比较+写入)

```ts
// file-mutation.ts:74-82
locks = KeyedMutex.makeUnsafe<string>()
withTargetLock(target):locks.withLock(target.canonical)(Effect.uninterruptible(effect))
// 注释: "Conditional writes compare and write under the same process-local lock
//         so cooperating OpenCode mutations do not overwrite changes made from the same stale content"
```

**设计要点**:CAS 的"检查-写入"在 KeyedMutex(同 key 串行)内完成——进程内多工具并发写同一文件安全;uninterruptible 防检查后写前被中断。

### 3. Git 封装(设计 3:七组 + 仓库级锁)

```ts
// git.ts:64-171
repo:discover(向上找 .git + rev-parse 解析)/clone/create(seed 种子化)
remote:get-url
history:head/branch/defaultRemoteBranch/rootCommits
sync:fetchRemotes/fetchBranch/checkoutRemoteBranch/resetHard
change:capture(patch)/apply/discard(index: preserve|reset, untracked: preserve|remove)  ← move-session 用
worktree:create/remove/list
index:refresh(范围刷新,最大未跟踪字节)/ignored
tree:capture(TreeID)/write/files/diff/preview/restore/checkout  ← snapshot 用
// 锁:locks.withLock(repository.gitDirectory)(git.ts:180-182)——按仓库串行
```

**设计要点**:所有 git 操作按 gitDirectory 加 KeyedMutex——同一仓库的操作不会并发交错;git 命令通过 AppProcess.run 执行(非 spawn 裸奔)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 五写操作(create 排他/BOM 保留/CAS/幂等 remove) | file-mutation.ts:54-65 | ②文件写契约 |
| 2 | CAS = KeyedMutex 锁内比较+写入 + uninterruptible | file-mutation.ts:74-82 | ②并发安全 |
| 3 | Git 七组操作 + 仓库级锁 | git.ts:64-171,180-182 | ②版本控制抽象 |
| 4 | change 捕获/应用/丢弃(move 基石) | git.ts:100-113 | ②搬家=变更传输 |

## 面试弹药

- "CAS 在锁内才成立":比较和写入同临界区 + uninterruptible——进程内并发工具写同一文件不会互相覆盖
- "create 用 wx 排他":已存在 → TargetExistsError;目录缺失 → ensureDir 重试——创建语义精确
- "git 仓库级互斥":按 gitDirectory 加锁——fetch/reset/apply 不会交错
- "discard 双参数语义":index preserve|reset + untracked preserve|remove——搬家重置的精确控制

## 待深挖

- [ ] git tree capture 的未跟踪字节限制/忽略逻辑(内部实现)
- [ ] git change capture 的 patch 格式(ChangeSet 品牌)
