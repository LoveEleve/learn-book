# hq23 自仓库保护(Self-Repo Guard)— 产品②"自保护"蓝本

> 项目:Hermes(tools/self_repo_guard.py 722 行 + tools/terminal_tool.py:2902 接入)
> 假设:agent 在自己源码 checkout 里开发时,git 操作可能重写支撑本进程的 checkout(模块版本漂移危害)——Hermes 检测并拦截 worktree 变异操作,是"自保护"的完整样本。
> 结论:✅ 成立——worktree 变异清单/反混淆 shell 解析/别名解析递归/heredoc 处理/git worktree 专项/gh 桥全具备,产品②"自保护"直接蓝本。

---

## 一、架构全景:为什么需要自仓库保护

```
问题:agent 开发自己源码时,git checkout/switch/rebase/merge/pull 等
     会重写支撑本进程的 checkout——模块版本漂移:运行时加载的 .py 与
     磁盘上不一致,进程内状态不可信

┌────────────────────────────────────────────────────────────┐
│ 接入(terminal_tool.py:2902):env_type=="local" 才检查       │
│   ——远端后端(ssh/docker)够不到本 checkout,不检查           │
│   → detect_self_repo_git_mutation(command, cwd)            │
│   → 命中 → 返回 blocked(exit_code=1 + 引导消息)            │
├────────────────────────────────────────────────────────────┤
│ 检测(self_repo_guard.py):                                 │
│   worktree 变异子命令集 + 反混淆 shell 解析                │
│   (引号/heredoc/命令替换/别名展开/cd 追踪)                 │
│   + git worktree remove/move 专项 + gh pr checkout 桥      │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:变异子命令分类(危险 vs 安全)

**位置**:`self_repo_guard.py:20-36`(集合)+ `512-536`(_mutates_worktree)

```
_WORKTREE_MUTATIONS:checkout/switch/rebase/merge/pull/restore/clean/
  cherry-pick/revert/bisect(bisect 反复 checkout 运行根——正是本守卫的
  模块版本漂移危害)

危险形式细分(已知内建也分类):
- reset:仅 --hard/--merge/--keep(缩写 --h 前缀也匹配)算变异;--soft 安全
- stash:action 非 list/show/create/store/drop/clear 即变异(默认 push)
- clean:非 --dry-run(-n 缩写)即变异
- restore:仅 --staged(-S)安全(不动 worktree);--worktree 或两者皆无 = 变异

_KNOWN_GIT_BUILTINS(~50):reset/stash/clean/restore 只在安全形式进此集
  ——"listing them here only prevents a pointless `git config --get
  alias.<sub>` subprocess for `stash list`/`reset --soft`/`clean -n`/
  `restore --staged`,which agent dev sessions run constantly"
```

**正确性价值**:危险/安全形式精确分类——stash list/reset --soft 等常跑安全形式零开销(不查别名);危险形式必拦。

**产品④映射**:自保护的危险操作分类——同命令不同形式精确区分(安全形式不误拦,危险形式必拦)。

## 设计 2:反混淆 shell 解析(检测前规整)

**位置**:`self_repo_guard.py:128-303`(shell 上下文/作用域)+ `423-453`(heredoc 掩码)+ `643-695`(_find_mutation)

```
复用 approval.py 解析族(_bash_exec_payload/_deobfuscate_shell_word/
  _iter_shell_command_starts/_read_shell_word)——同一解析器族

关键结构:
- _scope_keys:shell 作用域追踪(引号/$(/backtick/子 shell 嵌套)——每命令起点
  记录其作用域,cd 只在同作用域传播
- _mask_heredocs:heredoc 体掩码(防假 delimiter);execute_as_shell 的
  heredoc 体递归检测(脚本喂给 shell)
- _find_mutation(递归,深度上限 _MAX_RECURSION=4):
  cd/pushd 追踪(cwd_by_scope 按作用域)
  git → _inspect_git;gh/hub → _inspect_github_cli;shell -c → 递归
```

**正确性价值**:检测面对混淆命令(引号/heredoc/命令替换/多 shell 段)——作用域追踪 + cd 传播让"cd /elsewhere && git checkout"正确判定目标。

**产品④映射**:自保护的解析前置——作用域追踪 + heredoc 掩码 + cd 传播(防"换个目录绕过")。

## 设计 3:git 别名解析(递归展开)

**位置**:`self_repo_guard.py:582-625`(_inspect_git)

```
_inspect_git(executable, args, cwd, env, root, depth):
  - _git_target_and_subcommand:-C/--work-tree/-c alias. 解析目标与子命令
  - worktree 子命令:名字受害者作参数,不经 cwd 检查(_inspect_git_worktree
    专项,remove/move 指向运行根即拦——任意目录)
  - 目标不在运行根内 → None(不管他人仓库)
  - 变异子命令 → 拦;已知内建安全 → 放行
  - 未知子命令 → 查别名(内联 -c alias. 或 git config --get):
    alias 以 "!" 开头 → 递归 shell 检测(alias 到 shell 命令)
    否则 alias 展开递归(深度上限 4)——"别名指向别名/变异组合"不逃逸
```

**正确性价值**:别名解析递归(alias → git 变异组合)→ 绕过别名不逃逸;"!" 前缀别名走 shell 检测;深度上限防循环别名。

**产品④映射**:自保护的别名解析——别名指向变异组合不逃逸,递归有界。

## 设计 4:git worktree 专项 + gh 桥

**位置**:`self_repo_guard.py:551-564`(_inspect_git_worktree)+ `628-640`(_inspect_github_cli)

```
git worktree remove/move 指向运行根 → 拦(从任意目录)
  ——worktree 命令名字受害者作参数,cwd 检查不适用(专项)

gh/hub pr checkout → 拦(检出 PR = checkout 变异;仅 -R/--repo/--hostname
  选项消耗后匹配 ["pr","checkout"])
```

**正确性价值**:worktree 专项(参数指向非 cwd 检查)+ gh 桥(工具链等价物不逃逸)。

**产品④映射**:自保护的等价路径覆盖——原生 git 命令 + 工具链桥(gh)+ 专项参数模式。

## 设计 5:非绕过性接入 + 引导消息

**位置**:`terminal_tool.py:2902-2920`(接入)+ `self_repo_guard.py:716-722`(_block_message)

```
接入语义:
- env_type=="local" 才检查(远端够不到本 checkout)
- 命中 → blocked(exit_code=1 + error 消息)——非绕过性(不能 sudo/env 包装绕过,
  因为解析器剥 wrapper;不能换目录绕过,因为 cd 追踪)

_block_message:
  "Blocked: `X` would rewrite Hermes's live source checkout (root) and can
  mix module versions in this running process. Use a separate worktree or
  temporary clone. To change this checkout, stop Hermes, run the command
  externally, then restart Hermes."
```

**正确性价值**:非绕过(解析器剥 wrapper/追踪 cd)+ 引导替代方案(worktree/临时 clone)。

**产品④映射**:自保护的引导消息——拦截后告诉正确做法(不是只说不)。

---

## 三、与四项目对比(自保护)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes self_repo_guard |
|------|----|----------|----------|-----|------------------------|
| 自保护 | — | — | — | — | **检测重写运行 checkout 的 git 操作** |
| 分类 | — | — | — | — | **变异/安全形式精确分类** |
| 解析 | — | — | — | — | **复用 approval 解析族 + 作用域追踪** |
| 别名 | — | — | — | — | **递归展开(深度 4)** |
| 桥 | — | — | — | — | **gh/hub pr checkout + worktree 专项** |

**结论**:产品"自保护"参考 = Hermes self_repo_guard 全案。**与 hq15 approval 同解析族(复用);与文件状态(hq14)不同面:一个防"改自己支撑代码",一个防"并发写冲突"**。

---

## 四、面试弹药

1. **"模块版本漂移"**:checkout/switch 重写运行 checkout——运行时加载的 .py 与磁盘不一致,进程内状态不可信
2. **"安全形式零开销"**:stash list/reset --soft/clean -n/restore --staged 常跑——列入已知内建免别名查询;危险形式精确分类
3. **"别名绕过不逃逸"**:alias 指向变异组合 → 递归展开(深度 4);"!" 前缀别名走 shell 检测
4. **"cd 追踪"**:cd /elsewhere && git checkout 正确判定目标——作用域追踪 + cd 传播
5. **"非绕过"**:env_type==local 才查;解析器剥 sudo/env wrapper——远端够不到本 checkout 不查
6. **"引导替代"**:拦截后告诉用 worktree/临时 clone——不是只说不

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 变异分类 | 危险/安全形式精确区分 |
| 反混淆解析 | 作用域追踪 + heredoc 掩码 + cd 传播 |
| 别名递归 | 别名指向变异组合不逃逸 |
| worktree/gh 专项 | 等价路径全覆盖 |
| 非绕过接入 | 本地才查 + 引导替代方案 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:test_self_repo_guard.py(43 用例:变异分类/别名/heredoc/作用域/gh 桥/worktree 专项)+ test_terminal_self_repo_guard.py(7:接入非绕过性)
> 位置:detect_self_repo_git_mutation :698 / _find_mutation :643 / _inspect_git :582 / _mutates_worktree :512 / 接入 terminal_tool.py:2902
> 常量:_WORKTREE_MUTATIONS=10(实测)/_KNOWN_GIT_BUILTINS=45(实测)/_MAX_RECURSION=4
