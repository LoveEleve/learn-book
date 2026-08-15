# q35 — Project/Workspace/Repository(深度版:项目身份解析)

> 域:项目服务 | 文件:core/src/project.ts(136)+ project/(schema/directories/copy/copy-strategies/sql)+ workspace.ts(短)+ repository.ts(214)+ repository-cache.ts(259)
> review 轮次:2 轮(源码全文核心)

---

## 假设

项目身份(ID)是会话归属的核心:resolve 从目录解析出稳定 ID(git remote > 本地缓存 > root commit)。非 git 目录 = ID.global。Repository 抽象处理"外部引用"(本地路径/git URL + 分支)的解析与缓存。

## 验证

### 1. 项目 ID 三源(设计 1:remote > cached > root)

```ts
// project.ts:110-122 resolve:
repo = git.repo.discover(input)
非 git → { id: ID.global, directory: 根, vcs: undefined }
previous = cached(commonDirectory/opencode 文件)
id = remote(repo) ?? previous ?? root(repo)
// remote(project.ts:73-103):git remote get-url → 规范化(URL 或 SCP 格式)→ "git-remote:{host}/{path}" 哈希
//   归一化:去掉 file: 协议、.git 后缀、斜杠、host 小写
// root(project.ts:105-108):git rootCommits[0] → ID(无 remote 时用 root commit 作为身份)
// commit(124-126):写回缓存(桥接方法,老服务迁移期)
```

**设计要点**:同一远程仓库的多克隆共享 ID(remote 归一化);无远程用 root commit(同一仓库同身份);缓存为 previous(迁移)。

### 2. 目录管理(设计 2:project/directories)

```ts
// project/directories.ts:项目 → 目录列表(多目录项目)
// copy.ts + copy-strategies.ts:项目复制策略
// sql.ts:project 表(id/worktree/vcs/sandboxes)
```

### 3. Workspace(设计 3:作用域标识)

```ts
// workspace.ts:Workspace.ID(品牌 schema)——会话可归属 workspace(控制面)
// 省略 = 隐式本地;显式 = 预留未来 placement 语义(AGENTS.md)
```

### 4. Repository 抽象(设计 4:引用解析)

```ts
// repository.ts:26-135
Reference = FileReference | RemoteReference(解析 input)
parseRemote:git URL/SSH → RemoteReference{owner, repo, url}
validateBranch:分支名校验
cachePath(root, reference, branch):缓存路径
cacheIdentity:引用 → 缓存标识(哈希)
same:引用相等
// repository-cache.ts(259):仓库缓存(克隆/刷新/过期)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 项目 ID 三源(remote > cached > root)+ 非 git 归 global | project.ts:110-122 | ②项目身份 |
| 2 | 目录管理 + 复制策略 | project/directories + copy | ②多目录 |
| 3 | Workspace 标识(隐式/显式) | workspace.ts | ②作用域 |
| 4 | Repository 引用解析 + 缓存 | repository.ts + repository-cache.ts | ②外部依赖 |

## 面试弹药

- "remote 归一化是跨克隆身份":同一 GitHub 仓库多克隆 → 同一 project ID(host 小写 + 去 .git)
- "无 remote 用 root commit":无远程仓库以根提交为身份——同一仓库不分裂
- "非 git = ID.global":无版本控制目录全局身份(会话仍可用,权限按目录)
- "缓存是迁移桥":previous 来自 opencode 文件(老服务)——core 接管后移除

## 待深挖

- [ ] project/copy-strategies(复制策略细节)
- [ ] repository-cache 的刷新策略
- [ ] project-directories.test.ts 契约
