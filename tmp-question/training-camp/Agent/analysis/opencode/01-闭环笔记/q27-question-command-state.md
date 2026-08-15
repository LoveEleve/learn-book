# q27 — Question + Command + State(深度版:提问机制 + 可重放状态)

> 域:②执行(交互/配置) | 文件:core/src/question.ts(153)+ command.ts(64)+ state.ts(128)+ core/test/(question/command/state).test.ts
> review 轮次:2 轮(源码全文)

---

## 假设

Question = 权限 ask 的同构机制(Deferred 等待 + 事件通知),但支持**多问题批量**与选项;Command = 斜杠命令的 State 化存储;State = "可重放转换"抽象(transform 注册/移除/reload 重建)——插件与配置的共享状态机。

## 验证

### 1. Question(设计 1:批量提问 + Deferred)

```ts
// question.ts:93-110 ask:
id = ID.ascending + Deferred.make → pending.set → Event.Asked 发布 → Deferred.await
// reply(112-126):Event.Replied → Deferred.succeed(answers)
// reject(128-141):Event.Rejected → Deferred.fail(RejectedError)
// list:查看挂起
// 与 Permission 的差异:多问题批量(ReadonlyArray<Info>→ReadonlyArray<Answer>)+ 显式 reject 操作
// Location 归属(70-74 注释):"The Location layer map must materialize this layer once per embedded Location
//   so replies cannot settle another Location's deferred request"
```

**测试证据**(q3 测试 2815 "interrupts runner continuation when a question is dismissed"):RejectedError → runner 中断(与权限 DeclinedError 同路径)。

### 2. Command(设计 2:State 化存储)

```ts
// command.ts:22-47
Interface extends State.Transformable<Draft>
Draft:list/get/update/remove(update 不存在时创建默认 {name, template: ""})
// 斜杠命令(/init 等):命令模板 + 参数展开(见 q12 V1 command 处理)
```

### 3. State(设计 3:可重放转换)

```ts
// state.ts:24-43,61-128
Transformable = { transform, reload }
transform(update):注册转换(Scope 关闭移除)+ 立即应用 + reload 重建
reload:replay 所有活跃转换到 initial → finalize → 可见
batch:批量转换合并为一次 reload(CurrentBatch Reference 去重)
// 用途:agent/command/plugin 配置的声明式修改——"可重放的转换"= 增量配置的确定性重建
```

**设计要点**:State 是"配置即函数"的实现——每个 transform 是纯增量,reload = 从 initial 重放全部——保证确定性(与事件溯源同哲学)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | Question 批量 ask/Deferred/reject + Location 归属 | question.ts:93-141 | ②人机交互 |
| 2 | Command State 化(transform 修改) | command.ts:22-47 | ②命令系统 |
| 3 | State 可重放转换(transform/reload/batch) | state.ts:24-128 | ④确定性重建(与事件溯源同哲学) |

## 面试弹药

- "Question 与 Permission 同构":Deferred + 事件 + Location 归属——但批量提问 + 显式 reject
- "State = 配置即函数":transform 增量注册,reload 从 initial 重放——确定性(不保存中间态)
- "batch 合并 reload":多次转换一次重建(CurrentBatch Reference)——性能 + 原子

## 待深挖

- [ ] question schema 的 Option/Prompt/Tool 结构
- [ ] state.test.ts 的边界契约
- [ ] V1 question(schema/question-v1)对比
