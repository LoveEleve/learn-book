# q10 — Sandbox/Subprocess(深度版:每调用政策 + 升级阶梯 + 方言检测)

> 域:②执行契约(隔离) | 文件:packages/sandbox/(sandbox 452/sandbox-local/sandbox-policy/sandbox-windows-acl)+ native/landlock-run + subprocess/(subprocess 428/subprocess-local)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Sandbox = 能力缝:消费者 spawn 前包装 argv(ctx.sandbox)。政策 **PER CALL**(不固定 provider)。升级 = 严格更宽阶梯 + user-approval 通道(一切执行前 fail-closed)。后端方言检测(每后端的拒绝签名)。

## 验证

### 1. 模式与政策(设计 1:三模式 + 每调用)

```ts
// sandbox/src/index.ts:29-69:
SandboxMode = 'read-only' | 'workspace-write' | 'danger-full-access'
  read-only: 仅必需 sink(/dev/null);workspace-write: +workspace + 后端临时区
  网络/进程可见性在词汇外
SandboxPolicy 携带 PER CALL(不固定 provider):
  "two consumers may confine under different policies at the same instant
   (bash under read-only while a confined child agent needs its state directory writable)"
  "an approved escalated retry is a NEW call with a wider policy"
  Defaulting/resolution 是 consumer 边界的显式步骤;provider 视政策为完全指定
SandboxExecutionPolicy:mode + workspaceRoot + sessionId?(后端按会话键状态)
```

**产品启示**:②学习模式(只读)vs 写书模式(workspace-write) = 每调用政策切换——比 OpenCode 的"授权层"多一层"文件效果隔离"。

### 2. 升级阶梯(设计 2:严格更宽 + 执行时检查)

```ts
// escalation.ts:
WIDER_MODES: 'read-only' → [workspace-write, danger-full-access];'workspace-write' → [danger-full-access]
  ——严格更宽阶梯(只能升,不能跳/横)
"Checked at EXECUTION, never baked into a tool schema"
  ——schema 枚举 = ESCALATION_TARGETS(registry-global);有效模式 = per-call truth
approveEscalation:有序 fail-closed 序列——解析 sandbox_permissions 请求通过 user-approval 通道,
  在一切执行前
// 结构函数形状(EscalationAsk)而非审批服务类型:工具层闭包 ctx.approval.request(...)
//   ——本包不依赖审批/agent 包(依赖方向干净)
```

### 3. ConfinedArgv(设计 3:包装 + 方言)

```ts
// sandbox/src/index.ts:95-134:
ConfinedArgv = { argv(包装后), enforcement: 'full'|'partial', denialSignatures, runnerFailureRules }
enforcement:'partial' = 活跃后端/旧内核不能管辖每项文件效果;要求绝对边界的调用不得视为 full
denialSignatures:每后端拒绝方言(EROFS bwrap 只读绑定 / EACCES Landlock / EPERM Seatbelt)
  ——消费者按自己的后端匹配,不用跨后端并集(并集会声称后端从不产生的拒绝)
runnerFailureRules:退出码从不证明 runner 失败;匹配致命 stderr 行(信息行排除后)
  ——runner 失败 = 命令从未运行;denial = 限制工作并阻止了它
SANDBOX_UNAVAILABLE:无可用后端 → provider fail-closed(区分缺隔离与命令失败)
```

### 4. 后端(设计 4:landlock + windows)

```ts
// native/landlock-run:Landlock 原生(landlock-run 启动器,packages 结构)
// sandbox-local:本地后端;sandbox-windows-acl:Windows ACL(每 live session/workspace 随机私有临时目录 + SID)
// subprocess:消费者包装 argv 前经 sandbox(架构:spawn 前包装)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三模式 + 每调用政策 | sandbox/src/index.ts:29-69 | ②文件效果隔离 |
| 2 | 严格升级阶梯 + 执行时检查 | escalation.ts | ②权限升级 |
| 3 | ConfinedArgv(方言签名 + runner 规则) | index.ts:95-134 | ②失败归因 |
| 4 | fail-closed(SANDBOX_UNAVAILABLE) | index.ts:124 | ②安全默认 |
| 5 | Landlock 原生 + Windows ACL | native + windows-acl | ②平台 |

## 面试弹药

- "政策 per-call 不固定 provider":同一时刻 bash 只读 + 子 agent 写状态目录——每调用政策,升级 = 新调用
- "严格更宽阶梯":只能升不能跳——升级路径受控
- "方言签名不是并集":bwrap 报 EROFS,Landlock 报 EACCES——按后端匹配,并集声称不存在的拒绝
- "退出码从不证明 runner 失败":runner 失败 = 命令没跑;denial = 限制生效——归因精确
- "升级在一切执行前 fail-closed":approveEscalation 经 user-approval 通道,工具层闭包审批服务(依赖方向干净)

## 待深挖

- [ ] landlock-run 的原生实现
- [ ] sandbox-local 的 bwrap/seatbelt 集成
- [ ] sandbox-policy 的默认策略解析
