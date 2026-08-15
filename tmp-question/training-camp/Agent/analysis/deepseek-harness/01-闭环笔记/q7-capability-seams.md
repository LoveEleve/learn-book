# q7 — 能力缝模式(深度版:三包结构 + fs/shell/sandbox 样本)

> 域:②执行引擎(扩展模式) | 文件:packages/(shell/fs/sandbox/subprocess/terminal)+ docs/capability-seams.md + docs/architecture.md §Capability seams
> review 轮次:2 轮(源码结构 + 事件面)

---

## 假设

能力缝 = 可换能力,三角色:**Service Definition**(接口)/ **Service Provider**(实现)/ **Consumer**(常用 = 模型工具)。一个包可合并角色,但单角色不是缝。换 provider 换整个产品(fs/subprocess 共享执行世界)。

## 验证

### 1. 三包结构(设计 1:每能力一缝)

```ts
// shell 缝:
shell/           ← Service Definition(ctx.shell)
bash-local + bash-sandbox + pwsh-local + pwsh-sandbox ← Providers(2 后端 × 2 模式)
tool-bash + tool-bash-persistent + tool-pwsh ← Consumers(模型工具)
shell-env       ← 环境补充
// fs 缝:
fs/             ← Service Definition(ctx.fs)
fs-local + fs-sandbox ← Providers(共享执行世界)
tool-fs + tool-fs-search + tool-str-replace-editor ← Consumers
fs-observation-policy ← 观察政策
// sandbox/subprocess/terminal 同构
```

**产品启示**:产品②的"工具/权限/沙箱/存储"都可做成能力缝——学习模式(只读 provider)vs 写书模式(写 provider)= provider 切换,不碰 Consumer。

### 2. 缝事件(设计 2:fs 样本)

```ts
// fs/fs/src/index.ts:58-76:
'fs/write-intent'(waterfall): 写意图决策(target, actor, next)→ FsWriteIntent | undefined
'fs/edit-intent'(waterfall): 编辑意图(带版本)
'fs/observed'(emit): 观察通知(target, observation, actor)
// ——政策/适配器挂在缝事件上,不 import loop(architecture.md:59)
// shell 缝:shell 后端通过 ctx.subprocess spawn(架构:shell 是 subprocess 的 Consumer)
```

### 3. 换 provider 换产品(设计 3:共享执行世界)

```ts
// architecture.md:102:
"Filesystem and subprocess providers share one execution world,so pointing them
 at a remote sandbox moves Bash,PTY,and LSP with them,with no provider forks"
// subagent:从全新子 agent 到委托另一产品,同一接口(8 providers)
// sandbox:消费者 spawn 前包装 argv(architecture.md:118)
```

### 4. 缝完整性规则(设计 4:三角色契约)

```ts
// packages/AGENTS.md:
"Design Service Definitions for all current Consumers"
"keep tool-schema,Loader,UI,transport,and provider-specific behavior in the Consumer or provider;
 do not let one Consumer dictate the service contract"
// 反模式:一个内部调用者的公开服务方法 → 传私有能力闭包
// 完整性:新能力 = 设计三角色,不是单角色
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三包结构(Def/Provider/Consumer) | shell/fs/sandbox 各包 | ②可替换能力 |
| 2 | 缝事件(fs/write-intent 等) | fs/fs/src/index.ts:58-76 | ②政策挂点 |
| 3 | 共享执行世界(远程沙箱迁移全家) | architecture.md:102 | ②架构杠杆 |
| 4 | 三角色完整性规则 | packages/AGENTS.md | ②契约纪律 |

## 面试弹药

- "换 provider 换整个产品":fs/subprocess 共享执行世界——指向远程沙箱,Bash/PTY/LSP 一起迁移,零 provider fork
- "消费者不主导契约":tool-schema/UI/transport 行为在 Consumer——一个 Consumer 不能dictate 服务契约
- "缝事件不 import loop":政策/适配器挂 fs/*、tools/* 事件——无循环依赖
- "单角色不是缝":Service Definition 单独 = 接口文件,不是能力

## 待深挖

- [ ] fs-local vs fs-sandbox 的实现差异
- [ ] shell 的 request/spec 分离模板
- [ ] sandbox 的 escalation/roots(提升/根限制)
