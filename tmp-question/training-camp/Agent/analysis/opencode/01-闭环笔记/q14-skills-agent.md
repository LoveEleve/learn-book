# q14 — Skills + Agent(深度版:远程发现安全 + 权限过滤)

> 域:技能/agent | 文件:core/src/skill/discovery.ts(213)+ guidance.ts(76,q2 已读)+ skill.ts(109)+ agent.ts + opencode/src/agent/(agent.ts/subagent-permissions)+ core/test/(skill-discovery/skill/agent).test.ts
> review 轮次:2 轮(源码全文 + 测试抽查)

---

## 假设

技能系统分三层:发现(远程仓库拉取,安全下载器)、指导(Context Source,权限过滤)、工具(正文加载,权限检查)。远程发现的安全模型是重点:从不可信 URL 拉文件必须有路径穿越/跨域/编码攻击防护。

## 验证

### 1. 发现安全(设计 1:六重校验)

```ts
// discovery.ts:15-53
isSafeSegment:非空 / 非 ./.. / 无 / \ \0          ← 单段名
isSafeRelativePath:非空 / 无 \ \0 ? # / 非 URL / 
  非 posix+win32 绝对路径 / 每段 decodeURIComponent 后再次校验  ← 双重编码攻击防护
// pull 内校验(discovery.ts:116-151):
1. skill.name 必须 isSafeSegment
2. files 必须含 SKILL.md 或 {name}.md(技能必须有主文件)
3. root = resolve(sourceRoot, name);FSUtil.contains(sourceRoot, root) 且 ≠ root  ← 目录穿越
4. 每个文件 isSafeRelativePath + new URL 可解析 + resource.origin === source.origin  ← 跨域防护
5. destination = resolve(root, file);FSUtil.contains(root, destination) 且 ≠ root
6. 任一文件校验失败 → 整个技能丢弃(全有或全无)
```

### 2. 版本化原子更新(设计 2:staging → rename → 回滚)

```ts
// discovery.ts:153-205
版本匹配 → 增量下载(并发 8)
版本变化 → 原子替换:
  staging 下载(全成功才继续,并发 4)
  → 写 .opencode-version
  → uninterruptible:rename(root → backup) + rename(staging → root)
  → rename 失败 → 回滚(backup → root)
  → ensuring 清理 staging
// HTTP:retryTransient 2 次 + 指数退避 200ms + jitter(discovery.ts:76-83)
```

**设计要点**:远程技能更新像软件包管理器(原子替换 + 回滚 + 版本文件)——半更新状态不存在。

### 3. 指导源(设计 3:权限过滤,q2 已详述)

```ts
// guidance.ts:46-68
SkillV2.available(skills, agent):按 agent 权限过滤
仅列 name + description;正文/位置只通过权限检查的 skill 工具暴露(CONTEXT.md:122)
空列表 + skill 权限 deny → SystemContext.empty(不注入空指导)
```

### 4. Skill 工具(设计 4:正文加载 + 权限)

```ts
// skill.ts(109 行)+ tool/skill.ts:通过工具加载技能正文(非注入),
// 权限检查在工具层(skill action)
```

### 5. Agent 系统(设计 5:默认 build + 权限继承)

```ts
// agent.ts:State.Transformable(Draft 编辑器:list/get/default/update/remove)
// select/resolve:按 id 或默认;defaultID = "build"
// subagent-permissions:子 agent 权限(sub agent 从父继承 + 自己的)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 发现六重校验(段/路径/穿越/跨域/编码) | discovery.ts:15-53,116-151 | ②外部技能安全 |
| 2 | 版本化原子更新(暂存/替换/回滚) | discovery.ts:153-205 | ②更新一致性 |
| 3 | 指导源权限过滤(只列名+描述) | guidance.ts:46-68 | ①技能可见性 |
| 4 | 正文走工具(权限检查点) | skill.ts + tool/skill.ts | ②权限边界 |
| 5 | agent 默认 build + 权限继承 | agent.ts + subagent-permissions | ②权限模型 |

## 面试弹药

- "双重编码攻击防护":decodeURIComponent 后再校验段——%2e%2e 等编码穿越无效
- "全有或全无":任一文件校验失败 → 整个技能丢弃(不装半个技能)
- "原子替换 + 回滚":staging → rename → 失败回滚——远程技能更新像包管理器
- "技能正文不注入只加载":guidance 只列名+描述,正文经权限检查的工具加载——最小暴露

## 待深挖

- [ ] subagent-permissions 细节
- [ ] skill 工具的权限断言(source 结构)
- [ ] opencode/src/skill/index.ts(354)的 V1 技能发现对比
