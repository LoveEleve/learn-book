# q10 — Sandbox/Subprocess(深度版:每调用政策 + 升级阶梯 + 测试契约)

> 域:②执行契约(隔离) | 文件:packages/sandbox/(sandbox 452/sandbox-local/sandbox-policy/sandbox-windows-acl + tests/escalation.spec + roots.spec + vocabulary.spec)+ native/landlock-run + subprocess/(subprocess 428/subprocess-local + tests/local.spec 404+)
> review 轮次:3 轮(源码全文核心 + 测试契约)

---

## 假设

Sandbox = 能力缝:消费者 spawn 前包装 argv(ctx.sandbox)。政策 **PER CALL**(不固定 provider)。升级 = 严格更宽阶梯 + user-approval 通道(一切执行前 fail-closed)。**测试契约揭示升级/根/失败语义**。

## 验证

### 1. 模式与政策(设计 1:三模式 + 每调用)

```ts
// sandbox/src/index.ts:29-69:
SandboxMode = 'read-only' | 'workspace-write' | 'danger-full-access'
SandboxPolicy 携带 PER CALL:两消费者可同时不同政策;批准升级重试 = 新调用更宽政策
SandboxExecutionPolicy:mode + workspaceRoot + sessionId?(后端按会话键状态)
```

### 2. 升级契约(设计 2:严格阶梯 + fail-closed 文本)★ review 轮 3

```ts
// tests/escalation.spec.ts(契约):
1. 阶梯(21-27):read-only → 两个更宽模式;workspace-write → 仅 full access;
   目标枚举 = 每会话可升级到的闭集(read-only 是地板)
2. 字段对(33-38):两字段都要,或两者带非空理由;单字段/空白理由拒绝
3. 标记(46-51):denial 标记命名模式;hint 标记命名家族主体
4. grants(77):返回请求模式,经 approver 带审计理由
5. fail-closed 文本(84-108):非拓宽请求不询问自带文本;缺审批服务/无 agent → 各自不同文本;
   非 grant 结局映射独特逐字文本(主体在拒绝中);闭联合外结局触发穷尽守卫
// tests/roots.spec.ts:符号链接解析(现有路径 realpath;无法解析原样保留——保守,直到存在);
//   read-only 不授予;workspace-write 授予工作区根 + 平台临时区(规范 + 去重)
// tests/vocabulary.spec.ts:词汇契约
```

**设计要点**:fail-closed 文本逐结局不同(可诊断);roots 保守(无法解析 = 不匹配直到存在);枚举闭集防越界。

### 3. Subprocess 契约(设计 3:生命周期/终结)★ review 轮 3

```ts
// tests/local.spec.ts(20+ 契约,关键):
1. 宿主退出终结(24-82):host-exit finalizer 在服务前监听器之前;活跃到正常 disposal 达静止;
   各宿主退出终结失败 contain 继续其他目标
2. 可执行解析(112-138):绝对/PATH 解析 + 查找取消;Windows 候选大小写不敏感覆盖
3. 终端(164-340):分配输入预校验;disposal 终结 + 加入 owned 终端;等每终端清理 + 聚合失败;
   单失败不包装;强制终结剩余目标后释放失败 disposal;顶层退出达静止后释放终端;
   自动清理失败的终端保留
4. 句柄(385-404):注册 ctx.subprocess + spawn 托管句柄;disposal 杀仍运行进程等退出;
   已结算进程离开 live 集(disposal 不重杀)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 三模式 + 每调用政策 | sandbox/src/index.ts:29-69 | ②文件效果隔离 |
| 2 | 严格升级阶梯 + 执行时检查 | escalation.ts + spec:21-27 | ②权限升级 |
| 3 | fail-closed 逐结局文本(可诊断) | escalation.spec:84-108 | ②失败语义 |
| 4 | roots 保守(不匹配直到存在) | roots.spec:20 | ②路径安全 |
| 5 | Subprocess 终结/清理契约 | local.spec:24-404 | ②进程生命周期 |

## 面试弹药

- "政策 per-call 不固定 provider":同一时刻 bash 只读 + 子 agent 写状态目录
- "fail-closed 文本逐结局不同":缺审批/无 agent/各拒绝都有独特文本——模型可诊断
- "roots 保守":无法解析 = 原样保留(匹配 nothing 直到存在)——防错误授权
- "disposal 不重杀已结算":settled 进程离开 live 集——幂等终结
- "单失败不包装":cleanup 失败聚合,不包装掩盖——错误保真

## 待深挖

- [ ] landlock-run 的原生实现
- [ ] sandbox-local 的 bwrap/seatbelt 集成
- [ ] sandbox-policy 的默认策略解析
