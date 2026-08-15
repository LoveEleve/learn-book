# q24 — Subagent Providers(深度版:7 提供者光谱 + continuation 契约)

> 域:②执行引擎(子任务) | 文件:packages/subagent/(subagent-acp 587/subagent-claude-code 604/subagent-codex 705/subagent-dsh-sdk 375/subagent-fork-in-process 124/subagent-in-process-driver 405/subagent-spawn-in-process 94)+ subagent/subagent/tests/continuation.spec.ts(2523)
> review 轮次:3 轮(源码结构 + 能力面 + continuation 测试契约 70+)

---

## 假设

Subagent = 能力缝:7 提供者从"进程内最便宜"到"委托外部产品",共享 SubagentProvider 接口(capabilities + inheritsParentContext + start)。能力声明(CO 声明)决定协议面。**continuation 测试契约(70+)揭示 Activation/接纳/排水/终端结果语义**。

## 验证

### 1. 提供者光谱(设计 1:7 档)

| Provider | 行数 | 能力 | 父上下文 | 本质 |
|---------|:--:|------|:--:|------|
| spawn-in-process | 94 | outputSchema/depthLimit/toolFilter/persona | 否 | 同 context 新鲜子 Agent |
| fork-in-process | 124 | — | ? | 进程内 fork |
| in-process-driver | 405 | 共享驱动 | 否 | 一次性运行驱动 |
| dsh-sdk | 375 | NO_START_CAPABILITIES | 否 | 进程外 JSON-RPC |
| acp | 587 | — | 否 | 委托 ACP |
| claude-code | 604 | NO_START_CAPABILITIES | 否 | 委托 Claude Code |
| codex | 705 | — | 否 | 委托 Codex |

### 2. continuation 测试契约(设计 2:Activation/接纳/排水/终端)★ review 轮 3

```ts
// tests/continuation.spec.ts(70+ 契约,关键):
1. 接纳(171-310):inbox 接纳时双身份返回(不等 turn/日志);无 prepareContinuable 能力 → 无 id 拒绝;
   持久化未配置 → 同步拒绝;保留子 id + 预 turn 描述符;abort 前/发布后回滚;
   深度上限拒绝;未声明组合字段省略;tool filter/persona 记录并重施
2. 冷恢复(367-440):不发明描述符未声明的模型路由;fork 前缀后续 turn 编号;persona 重施
3. Activation 生命周期(440-754):运行中同 Activation 入队(单 FIFO);已结算子冷恢复;
   初始 provider 注销后冷恢复;等待 Activation 唤醒而非冷恢复;非 durable 直接父拒绝;
   无支持描述符不可恢复;未知 id 不可用
4. 森林/排水(754-1050):manager teardown child-first 全森林;父森林排水不殃及兄弟;
   排水保留 continuable 根;中间一次性 Agent 离注册表后找作用域后代;排水开始拒新物化/交付;
   无自动重放(已接纳未记录消息);冷恢复后重查父存活
5. 终端结果(1157-1330):子自身终止原因(非 teardown 成功);本 epoch 输出(子仍活时捕获);
   空 usage-only 消息后保留早期文本;重施 epoch 未开 turn 无前一答案;handle/pre-disposal 失败独立保留
6. 丢弃/拒绝(1385-1470):接纳后丢弃消息释放;旧 id 后期窗口丢弃释放;pre-step 拒绝报告为拒绝;
   激活保持(已接纳消息仍在 inbox)
7. 报告/通知(1528-1840):父收到子结束报告(未问也报);子已自报仍交付;策略拒绝交付 = declined 非 finished;
   首 step 前失败/停止/祖先中断 = stopped;无法 durable 释放则扣留结果;空闲父一个普通 turn 看通知;
   忙父批量通知为一步;维持父保持活直到读通知
```

**设计要点**:continuation 是"可恢复子任务"的完整状态机——接纳/回滚/排水/报告每步有契约;终端结果区分 finished/stopped/declined/refused。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 7 档光谱(同 context → 委托外部) | subagent/* | ②子任务架构 |
| 2 | capabilities 声明契约 | 各 provider | ②能力协商 |
| 3 | Activation 状态机(接纳/回滚/排水) | continuation.spec:171-1050 | ②子任务生命周期 |
| 4 | 终端结果四态(finished/stopped/declined/refused) | continuation.spec:1157-1470 | ③验收结果 |
| 5 | 报告/通知语义(未问也报/批量) | continuation.spec:1528-1840 | ②父-子通信 |

## 面试弹药

- "能力声明决定协议面":spawn 全开 vs 外部 NO_START_CAPABILITIES——父能强制多少由能力表定
- "冷恢复不发明模型路由":描述符未声明的路由不猜测——恢复确定性
- "排水不殃及兄弟":每父森林独立排水——隔离
- "终端结果语义精确":finished/stopped/declined/refused——每个结局有触发路径
- "未问也报":子结束主动报告父——无需轮询

## 待深挖

- [ ] in-process-driver 的完整驱动逻辑
- [ ] claude-code 的 hooks 集成细节
- [ ] codex 的委托协议
