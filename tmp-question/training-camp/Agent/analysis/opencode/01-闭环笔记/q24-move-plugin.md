# q24 — MoveSession + Plugin(深度版:会话搬家 + 插件生命周期)

> 域:位置变更 + 扩展 | 文件:core/src/control-plane/move-session.ts(148)+ core/test/move-session.test.ts(235,3 契约)+ opencode/src/plugin/(loader 237/install/index/meta)+ packages/plugin/src/v2/(effect/* promise/* options)
> review 轮次:2 轮(源码全文 + 测试契约)

---

## 假设

MoveSession = "git patch 搬运":源变更 → capture → apply 到目标 → Moved 事件 → 源重置。全程 git 变更集操作(不复制文件)。搬家通过事件驱动 runner/epoch 响应(q5 的 epoch reset)。

## 验证

### 1. 前置校验(设计 1:同项目强制)

```ts
// move-session.ts:77-87
session 存在 → 目录相同 → 直接返回(幂等)
project.resolve(源) vs project.resolve(目标):
  不同 projectID → DestinationProjectMismatchError(拒绝跨项目搬家)
// moveChanges = input.moveChanges && 源目录 ≠ 目标目录
```

### 2. 搬运(设计 2:capture → apply → discard)

```ts
// move-session.ts:89-137
1. capture:git.change.capture({ repository: source, path }) → ChangeSet(失败 → CaptureChangesError)
2. apply:git.change.apply({ repository: destination, path, changes })(失败 → ApplyChangesError)
3. events.publish(Moved, { location, subdirectory })  ← 事件驱动后续(epoch reset/runner 中断)
4. discard:git.change.discard({ repository: source, index: "preserve", untracked: "remove" })
   (失败 → ResetSourceChangesError)
```

**测试证据**(move-session.test.ts):
- 54 "moves session changes to another project directory":tracked+untracked 文件到目标,源重置为 initial,untracked 删除;session.directory 更新
- 112 "moves within a checkout without transferring existing changes":嵌套目录搬家,现有变更不转移
- 164 "moves nested session changes without cleaning unrelated files":只处理会话目录范围

### 3. 事件驱动后续(设计 3:Moved 事件消费者)

```ts
// q5 已验证:context-epoch reset(SessionContextEpoch.reset)
// q3 已验证:runner location 校验(会话搬家后旧进程 runner 中断,epoch 清除,收件箱保留)
// 子目录:subdirectory 相对路径记录(嵌套会话)
```

### 4. Plugin 生命周期(设计 4:plan → resolve → load)

```ts
// loader.ts:15-45
Plan(配置规格)→ Resolved(解析:依赖/条目/兼容性)→ Loaded(加载结果)
resolve:可重试解析错误分类(install/entry/compatibility 三阶段)
load:attempt 包装(重试)
// plugin/index.ts + install.ts:插件安装
// meta.ts + shared.ts:插件元数据/共享运行时
// v2(effect/* promise/*):插件公开 API 双形态(Effect + Promise)
//   能力面:agent/aisdk/catalog/command/context/event/filesystem/integration/location/npm/path/plugin/reference/registration/skill
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 同项目强制 + 幂等 | move-session.ts:77-87 | ②位置变更安全 |
| 2 | git 变更集搬运(capture→apply→discard) | move-session.ts:89-137 | ②搬家=变更传输 |
| 3 | Moved 事件驱动(epoch reset/runner 中断) | move-session.ts:106 + q3/q5 | ④事件联动 |
| 4 | 插件 plan→resolve→load + 错误分类 | loader.ts:15-45 | ②扩展生命周期 |

## 面试弹药

- "搬家 = git 变更集,不复制文件":capture→apply→discard,untracked 删除,index preserve——迁移可审计可回滚
- "跨项目搬家拒绝":projectID 不匹配 → 错误——会话绑定项目,不漂移
- "Moved 事件 = 联动枢纽":epoch reset + runner 中断 + 收件箱保留——一切响应都是事件消费者

## 待深挖

- [ ] git.ts 的 change.capture/apply/discard 实现
- [ ] plugin install/meta 细节
- [ ] v2 plugin 的 registration/skill 能力
