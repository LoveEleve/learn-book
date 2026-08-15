# OpenCode 参考架构 — 源码学习 Agent 的设计蓝图

> 项目:anomalyco/opencode(v1.18.18 tag)
> 生成:2026-08-14
> 输入:53 份闭环笔记(Q1-Q53,278 设计)+ 域发现 v10(53 域)
> 用途:从 OpenCode 提取"源码学习 Agent"的四组件设计蓝图——抄什么/改什么/弃什么
> 前置:对照 Pi 参考架构(Pi-参考架构.md)+ Reasonix 参考架构(reasonix-参考架构.md)

---

## 一、OpenCode 架构一句话

> **OpenCode = 事件溯源执行的(收件箱驱动的)、Context Epoch 管理上下文的、权限显式的、可托管输出的 Agent 引擎。**

四个定语对应四个核心域:事件溯源(EventV2+投影器)、收件箱(SessionInput)、上下文(Epoch+Compaction)、权限(PermissionV2+工具)。

**与 Pi/Reasonix 的本质差异**:
- Pi:内存循环 + 事件发布,循环状态在内存
- Reasonix:长跑 + checkpoint,计划契约驱动
- **OpenCode:循环状态在 DB(收件箱),上下文=状态(Epoch),事件=真相(投影器同事务)**——这是最接近"可重放执行引擎"的架构

---

## 二、产品四组件 ← OpenCode 设计映射总表

| 产品组件 | OpenCode 抄什么 | OpenCode 改什么 | 关键笔记 |
|---------|----------------|----------------|---------|
| ① 对齐模块 | SystemContext 代数、Context Epoch、指令源、Question 批量提问、技能指导源 | 6 维盘问规格书 = Epoch 基线 + 时间序更新 | Q2/Q5/Q27 |
| ② 执行引擎 | 双层循环、收件箱、事件溯源、压缩、工具结算、权限、协调器、崩溃恢复 | 章节驱动 + 验收挂点(替代 steer 续跑) | Q3/Q4/Q6/Q7/Q8/Q11 |
| ③ 自动验收器 | 事件发布管线、强制工具结构化输出、Step 快照对、错误契约 | 章节 conformance(从事件审计演化) | Q3/Q9/Q13/Q16 |
| ④ 书级知识库 | EventV2、投影器、7 段摘要模板、版本化事件、公开/内部分层 | 目录按"书籍/章节"组织 + 重放即真相 | Q1/Q4/Q6/Q9/Q48 |

---

## 三、组件① 对齐模块(6 维盘问)

### 抄自 OpenCode

| OpenCode 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **SystemContext 代数**(六字段源 + 四态 reconcile) | system-context/index.ts:32-39,79-80 | 6 维盘问 = 6 个 Context Source(对象/目标/深度/画像/边界/产出);每维独立加载/比较/渲染 |
| **Unavailable = stale-while-revalidate** | system-context/index.ts:198-206,283-291 | 盘问维度暂时拿不到(如"产出形式待定")→ 保留旧值,不打扰 |
| **空渲染拒绝**(requireText) | system-context/index.ts:309-312 | 规格书任何维度渲染空 → 缺陷(防空白规格书) |
| **Context Epoch 基线不可变** | context-epoch.ts:40-78 | 规格书 = 基线;用户中途改目标 = 时间序更新消息(不重写基线) |
| **指令源聚合**(AGENTS.md 全文件一个值) | instruction-context.ts:40-74 | 领域骨架清单 = 一次性聚合源(任何变化发完整替换) |
| **Question 批量提问 + Deferred** | question.ts:93-141 | 三档提问实现(ask/Deferred/reject);批量提问一次问完 |
| **技能指导源**(权限过滤只列名+描述) | skill/guidance.ts:46-68 | 骨架清单只列能力名,正文按需加载 |
| **reconcile 四态代数** | system-context/index.ts:218-280 | 对齐状态机:Unchanged(没变)/Updated(改了)/Replacement(不兼容)/Blocked(拿不到) |

### 产品设计(改)

```
★ 规格书三合一(关键设计,Pi 版继承):
  同一份 spec 同时是:
  - Epoch 基线(SystemContext 组合,不可变)
  - 收件箱第一条输入(admit 进 SessionInput)
  - 验收器契约源(章节 conformance 基准)
  一份 spec 三处消费——避免"三份规格书漂移"

★ 对齐中断处理:
  - 用户中途改目标 → reconcile Updated → 时间序系统消息(不重写基线)
  - 盘问维度 unavailable → 保留旧值(Blocked 时不构造不完整规格书)
  - 用户拒绝提问 → RejectedError → 对齐终止(可重试)

★ 入口自检源(review 轮 4 补,对齐 A 档):
  - 版本/对象 = 指令源聚合(instruction-context:版本文件 + AGENTS.md 聚合为 core/instructions 源)
  - 环境事实 = builtins 源(core/environment:目录/平台/版本,加载失败不影响其他源)
  - 配置 = 三来源合并(global < 项目 < .opencode,打开时读一次)
  —— 入口点不自报,从环境源派生(q2/q28/q50)
```

---

## 四、组件② 执行引擎

### 抄自 OpenCode(核心骨架)

| OpenCode 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **双层循环**(shouldRun/needsContinuation) | runner/llm.ts:383-406 | 产品执行主干;外层章节队列,内层章节步骤 |
| **持久化收件箱驱动循环**(hasPending 查 DB) | runner/llm.ts:387-405 | 循环状态在 DB——崩溃后 resume 从 durable 继续(产品 7 卡点"敢一直跑"的答案) |
| **admit/promote 分离**(先落盘再执行) | session/input.ts:41-81,245-287 | 章节任务 admit(队列)+ promote(执行);resume:false = 只排队 |
| **runTurn 7 步** | runner/llm.ts:173-348 | 章节步骤的确定性执行序(location 校验/epoch/promotion/request/stream/settle) |
| **压缩 = 异常转移**(TurnTransitionError) | runner/llm.ts:152-166,369-381 | 上下文超预算 → 压缩 → 弹回 turn 开始;主循环零改动 |
| **7 段摘要模板** | compaction.ts:16-55 | 章节交接摘要(Objective/Work State/Next Move/Relevant Files) |
| **unsettled 兜底矩阵**(7 场景) | runner/llm.ts:295-345 | 任何失败路径工具都有终态(成功/失败),不悬挂 |
| **崩溃恢复**(先失败化 running 工具) | runner/llm.ts:119-139 | 重跑前失败化残留——副作用不静默重放 |
| **工具不透明定义 + 结算七步** | tool/tool.ts:69-76 + registry.ts:50-82 | 产品工具契约(单 executor + stale rejection) |
| **输出托管**(bounded preview + 托管文件) | tool-output-store.ts:138-174 | 超大输出保头保尾 + 指针,模型上下文不爆炸 |
| **权限三件套**(规则/ask-assert/级联) | permission.ts:76-218 | 边界权限预授权(deny 永远赢 + always 级联) |
| **RunCoordinator**(每 key 串行 + wake 合并) | run-coordinator.ts:24-104 | 章节执行调度(同章节排队,章节间并行) |
| **Eager 工具执行 + await 全部结算** | runner/llm.ts:250-271 | 工具并行 + 结算屏障 |
| **max-steps 禁工具** | runner/llm.ts:202-213 | 章节步数上限最后一步强制文本 |
| **CAS 写入**(writeIfUnchanged) | file-mutation.ts:144-157 | 文件写防覆盖(章节产出) |
| **apply_patch 预检后应用 + 部分失败报告** | tool/apply-patch.ts:85-189 | 章节补丁的三阶段(解析/预检/应用) |

### 产品设计(改)

```
学习模式:只读工具集(read/grep/glob/ripgrep)——分析不动代码
写书模式:+write/edit/apply_patch(带 CAS + 部分失败报告)
模式切换 = 权限规则集切换(PermissionV2 三来源,deny 优先)

★ 章节验收挂点:
  每章完成 → 事件发布管线产生 Step.Ended(快照对 + files diff)
  → 验收器消费(组件③)
  —— Pi 用 shouldStopAfterTurn 钩子,OpenCode 用事件驱动(更干净)

★ 失败循环(D17):
  步骤失败 → 工具失败进上下文(failUnsettledTools 语义)
  → LLM 自纠 → 重验;连续失败 → 停下问人(Question 批量提问)
```

---

## 五、组件③ 自动验收器

### 抄自 OpenCode

| OpenCode 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **事件发布管线**(LLMEvent → SessionEvent 12 映射) | publish-llm-event.ts:239-409 | 每步过程全事件化(文本/推理/工具输入/调用/结算)——验收可回放 |
| **工具 5 状态机**(inputEnded/called/settled/...) | publish-llm-event.ts:55-66 | 任何工具调用有确定性终态——验收"这步干了啥" |
| **generateObject 强制工具** | llm.ts:80-144 | 验收器结构化输出(全协议统一,不用 provider JSON mode) |
| **Step 快照对**(开始/结束 files diff) | runner/llm.ts:217,318-336 | "这步改了哪些文件"可审计(章节覆盖证据) |
| **错误契约**(域错误 vs 基础设施) | CONTEXT.md:150-153 | 验收失败 = tagged 域错误(可判别),非 Error 类身份 |
| **录制测试**(cassette + 回放 + 密钥检测) | http-recorder/cassette.ts:69-70 | 验收器测试基建(不泄漏 key) |
| **操作化错误**(operation 字段) | snapshot.ts:18-22 | 验收错误带操作名(哪个环节失败) |

### 产品设计(改)

```
★ 章节 conformance(从事件审计演化):
  章节完成事件流 → 对照规格书检查:
  1. 覆盖:章节要求的 KP 是否有对应 Step(快照对文件变化 + 事件)
  2. 深度:结构化输出(generateObject)是否满足规格书 schema
  3. 读者对象:产出文件是否面向目标读者(写书模式)
  不过 → 失败事件进上下文(LLM 自纠)→ 重验
  连续失败 → Question 批量提问(降级回交互)

★ 审计链路(2026-08-15 review 轮 4 补):
  runID(每次运行 8 位随机)贯穿:日志(文件+stderr)/OTLP 资源属性/事件
  ——全书分析过程可关联:验收时重放事件流 = 完整审计轨迹
  (q31:opencode.run/service.instance.id 共享 runID;结构化日志 key=value 扁平化)

★ 元验收(D17 自证):
  验收器本身 = 契约测试(EventV2 的 44 契约模式)
  ——"检查器也被检查"(http-recorder 的录制回放)
```

---

## 六、组件④ 书级知识库

### 抄自 OpenCode(理论基础)

| OpenCode 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **事件溯源**(聚合 seq + 投影器同事务) | event.ts:236-352 | 知识库 = 追加写事件日志;投影失败 = 不提交(重放即真相) |
| **双游标收件箱**(admitted/promoted) | session/input.ts + schema.gen.ts:158-166 | 章节结论 admit(草稿)/promote(定稿)——双游标可重放 |
| **投影器**(事件 → 消息,seq=事件序) | projector.ts:193-209 | 章节视图 = 投影(任意时点重建) |
| **durable/live-only 边界**(28+4) | session-event.ts:448-512 | 增量(delta)live-only,定稿(Ended)durable——只重放定稿 |
| **版本化事件**(type.version) | schema/event.ts:94-96 | 知识库 schema 演进(新版本定义 + 旧版本重放解码) |
| **7 段摘要模板**(跨 session 交接) | compaction.ts:16-55 | 交接文档 = 摘要(Objective/Work State/Next Move) |
| **历史过滤规则**(compaction + baseline 双重) | history.ts:24-53 | 章节视图 = 最近 N 章 + 基线后的更新——重放不重算 |
| **重放幂等/分叉检测** | event.ts:262-302 | 交接重放:同 seq 不同内容 = 报错(不静默覆盖) |
| **owner 栅栏**(多节点预留) | event.ts:525-532 | 多设备同步扩展点 |
| **公开/内部事件分层** | event-manifest.ts:57-82 | 书籍公开事件面 vs 内部事件——读者只承诺公开面 |

### 产品设计(改)

```
★ 书级组织(改):
  聚合 = sessionID → 章节聚合(每章一个 aggregate?或全书一个)
  推荐:全书一个聚合(seq 全序 = 书籍时间线),章节 = type 过滤
  —— SessionEvent 的 aggregate 是 sessionID,产品是 bookID

★ 跨 session 交接(产品 7 卡点 #21 的自动化答案):
  交接不是写文档——事件日志本身就是交接:
  新 session 打开 → 重放日志到 lastSeq → 投影恢复章节状态
  → compaction 摘要(7 段模板)注入上下文
  —— OpenCode 的 baseline+历史过滤正好实现

★ 结论防矛盾(D16):
  章节结论 = 投影定稿(durable End 事件)
  重放校验:同 seq 不同内容 → 分叉报错
  —— "结论不可静默修改"(与 Reasonix 的 subject 冲突同目标,实现更强)
```

---

## 七、D17 落地性检查(5 问)

### 1. 数据怎么流动(三合一原则)

```
规格书(1 份)三处消费:
- Epoch 基线(SystemContext 组合,不可变)——执行引擎的初始上下文
- 收件箱第一条输入(SessionInput.admit)——对齐产物入队
- 验收器契约源(章节 conformance)——验收基准
答案:同数据在系统上下文(基线)、收件箱(任务)、验收器(契约)三处,由同一事件流驱动,无漂移。
```

### 2. 时机怎么定

```
- 验收时机:章节 Step.Ended 事件(快照对 + files diff 就绪时)——不是每轮,是每章
- 压缩时机:request 预算超限(compactIfNeeded)+ provider 溢出(compactAfterOverflow,仅一次)
- 上下文更新:safe provider-turn boundary(输入提升后、工具结算后)——绝不同步推送
- 基线重建:压缩完成 / 会话搬家(epoch 终点)——切换模型不重建
```

### 3. 失败怎么循环

```
- 工具失败 → 失败事件进上下文(Tool.Failed)→ LLM 自纠 → 重验          [OpenCode 原样]
- 权限拒绝 → DeclinedError → 中断执行(不是重试)                       [OpenCode 原样]
- 权限纠正 → CorrectedError(带反馈)→ 进上下文 → 模型修正再试          [OpenCode 原样]
- 压缩溢出 → 一次恢复(二次 = 终止失败,防循环)                         [OpenCode 原样]
- 崩溃 → 重跑前失败化 running 工具 → 从 durable 收件箱继续            [OpenCode 原样]
- 连续验收失败 → Question 批量提问(降级回交互)                        [★产品新增,非 OpenCode 原样]
```

> ★ review 轮 8 溯源:前 5 条均有源码证据(工具失败 publish-llm-event.ts ×5 / DeclinedError llm.ts:149 / CorrectedError permission.ts ×5 / "cannot recover another overflow" llm.ts:361 / "Tool execution interrupted" llm.ts ×4)。
> 语义偏差说明:第 6 条在 OpenCode 是"用户 dismiss 提问 → RejectedError → 中断执行"(question.ts:35-39 + runner 测试 2815),不是"连续失败后自动降级"。产品的"连续失败 → 停下问人"是新增行为,复用 Question 机制但触发条件是验收失败计数——**抄机制,不抄触发**。

### 4. 大库怎么查

```
- 章节历史:历史过滤规则(compaction 后 + baseline 后更新)——不重放全量
- 分页:不透明游标(base64url 完整查询,q21)——翻页稳定
- 事件检索:readAggregate(after + limit)按聚合 seq 分页
- 结论查询:投影表(session_message 唯一 seq 索引)——顺序 = 事件顺序
- 全书检索:公开事件面(ServerDefinitions)+ 有限 history 端点(limit ≤ 100)
```

### 5. 系统怎么自证

```
- 验收器自证:EventV2 44 契约测试 + 录制测试(http-recorder cassette)——检查器被检查
- 重放自证:确定性(同 seq 同内容 = 无操作;不同 = 分叉报错)
- 状态自证:State 可重放转换(transform/reload 从 initial 重放)
- 并发自证:KeyedMutex 锁内 CAS(测试:并发写只成功一次)
- 迁移自证:V1→V2 shadow bridge(老事件合成新收件箱记录,测试覆盖)
```

---

## 八、产品决策清单(OpenCode 独有贡献)

| # | 决策 | 来源 | 理由 |
|---|------|------|------|
| 1 | 循环状态放 DB(收件箱) | Q3/Q4 | 崩溃恢复零成本,resume = 重放收件箱 |
| 2 | 上下文 = 状态(Epoch 基线 + 时间序更新) | Q5 | provider 缓存命中 + 变更可审计 |
| 3 | 变化不改基线(追加时间序消息) | Q5 | 成本(缓存)+ 正确性(审计)双赢 |
| 4 | 工具输出托管(保头保尾 + 指针) | Q8 | 上下文保护 + 完整可查 |
| 5 | 权限 deny 永远赢 + 级联 | Q7 | 安全默认 + 少打扰 |
| 6 | 无沙箱(授权层而非隔离层) | Q7 | 架构决策(与 MiniMax JD 的隔离需求对比) |
| 7 | 摘要模板 7 段(交接规格书) | Q6 | 给"另一个 agent 续跑"的结构化规格;★改用途:OpenCode 原用于上下文压缩(compaction.ts:16-46),产品用于跨 session 交接文档 |
| 8 | 事件 = 版本化契约 | Q1 | schema 演进不破坏重放 |
| 9 | 快照 = git 树(不发明 CAS) | Q16 | 复用 git 对象库 |
| 10 | 双运行时迁移(V1→V2) | Q12/Q34 | ★产品不适用:产品从零开始,无 V1 包袱——决策改为"架构演进原则"(事件契约先行,新能力用版本化事件而非双轨) |

> ★ review 轮 9 验证:10 条决策来源笔记均有效;修正 2 条(7 改用途标注、10 改为产品不适用)。相互矛盾检查:无(1 与 4 互补、2/3 与 7 互补、5/6 与 8 无冲突)。

---

## 九、弃用清单(产品不抄)

| OpenCode 设计 | 弃用原因 |
|--------------|---------|
| V1 内存循环 + 处理器三态 | 被 V2 事件溯源取代(AGENTS.md 明确禁止 bridge legacy loop) |
| V1 部件级写 API(updatePart 等) | 直接写存储,无投影确定性 |
| 模型家族提示词分发(prompt/*.txt) | 产品面向 JVM 等源码,非多模型家族差异 |
| 插件 Zod 兼容边界 | 产品插件用统一 Tool.make(V2 方式) |
| MCP 集成(完整) | 产品 MVP 不需要(学习场景);保留为扩展点 |
| GitLab workflow 审批 | 特定外部模型集成 |
| 云工作区/控制面同步 | 产品 MVP 单机(owner 栅栏留作扩展) |

---

## 十、与 Pi/Reasonix 的合并要点

| 维度 | Pi | Reasonix | OpenCode | 产品取 |
|------|----|----------|----------|--------|
| 循环 | 内存双层 | 长跑+checkpoint | **DB 收件箱双层** | OpenCode(可重放) |
| 上下文 | 压缩 5 重保护 | 7 标题摘要 | **Epoch 基线+时间序更新** | OpenCode(缓存+审计) |
| 验收 | 钩子+契约 | 独立审查器 | **事件管线+快照对** | OpenCode 事件 + Reasonix 独立审查 |
| 知识库 | 事件溯源 | subject 冲突 | **双游标+投影器+版本化** | OpenCode 全套 |
| 规格书 | 提示词模板 | TaskSpec 三合一 | **Epoch 基线+批量提问** | 合并(Pi 三合一 + OpenCode 基线) |

> ★ review 轮 6 核对:上表与 Pi/Reasonix 参考架构事实一致(双层循环/规格书三合一/独立审查器四隔离/7 标题摘要/subject 冲突均已核对源码位置)。
> 关联缺口:合并表"规格书"一行对应第十二章缺口 B(规格书 = 事件)——Pi 三合一的三处消费在 OpenCode 里是三个不同机制,产品必须用规格书事件族统一。

---

## 十一、覆盖对账表(53 笔记 → 蓝图去向)★ review 轮 2 补充
> v1 版只显式引用 18/53 笔记;对账发现 35 份未标映射。下表补齐,保证 278 设计都有去向(四组件/弃用/支撑)。

| 笔记 | 设计数 | 蓝图去向 |
|------|:--:|---------|
| q1 event-v2 | 11 | ④核心(事件溯源三支柱) |
| q2 system-context | 7 | ①核心(代数/四态) |
| q3 session-runner | 7 | ②核心(双层循环/兜底) |
| q4 session-input | 8 | ②核心(收件箱)+④(双游标) |
| q5 context-epoch | 8 | ①核心(基线/时间序更新) |
| q6 compaction | 8 | ②核心(双触发/7 段模板)+④(交接摘要) |
| q7 permission | 7 | ②核心(权限三件套) |
| q8 tools | 6 | ②核心(不透明定义/结算/托管) |
| q9 projector | 7 | ④核心(事件→消息投影) |
| q10 session-facade | 5 | ④支撑(历史过滤规则/分页) |
| q11 run-coordinator | 7 | ②核心(调度/wake 合并) |
| q12 v1-legacy | 7 | 弃用(被 V2 取代) |
| q13 llm | 6 | ②支撑(route 四轴/generateObject→③) |
| q14 skills-agent | 5 | ①支撑(骨架清单只列名)+②(发现安全) |
| q15 acp | 4 | 弃用(外部集成协议,MVP 不需要) |
| q16 snapshot | 6 | ③核心(Step 快照对)+②(回滚) |
| q17 shared-packages | 5 | ②支撑(Flock/KeyedMutex) |
| q18 policy-location | 4 | ②支撑(路径安全/声明式策略) |
| q19 builtin-tools | 5 | ②核心(统一四步权限序列/CAS/部分失败报告) |
| q20 mcp | 6 | 弃用(MVP 学习场景不需要;保留扩展) |
| q21 protocol-server | 5 | ④支撑(公开契约分层/不透明游标) |
| q22 database | 4 | ④支撑(双运行时/迁移日志) |
| q23 message-translation | 4 | ②核心(七类型翻译/模型一致性元数据) |
| q24 move-plugin | 4 | ②支撑(搬家=变更集)+插件弃用 |
| q25 file-mutation-git | 4 | ②核心(CAS/仓库级锁) |
| q26 remaining-tools | 5 | ②支撑(三类权限资源模式) |
| q27 question-command-state | 3 | ①核心(批量提问)+②(State) |
| q28 config | 4 | ①支撑(三来源合并/V1 迁移) |
| q29 v1-message-model | 5 | 弃用(媒体矩阵等被 V2 简化) |
| q30 server-wiring | 5 | ④支撑(双路由面/生命周期) |
| q31 observability | 4 | ③核心(OTLP/runID/录制密钥检测) |
| q32 location-services | 4 | ②支撑(34 服务按目录/LayerMap) |
| q33 v1-tool-registry | 5 | 弃用(模型过滤/插件兼容被 V2 取代) |
| q34 v1-llm | 5 | 弃用(双运行时被单路径取代) |
| q35 project-workspace | 4 | ②支撑(项目 ID 三源) |
| q36 v1-session-lsp | 4 | 弃用(部件 API/LSP 工具) |
| q37 effect-runtime | 4 | ②支撑(Runner 四态/Bridge) |
| q38 v1-compaction | 5 | 弃用(破坏性擦除 vs checkpoint) |
| q39 aisdk-catalog | 4 | ②支撑(钩子链/请求体修复) |
| q40 filesystem-ripgrep | 5 | ②支撑(隐私保护清单/搜索原语) |
| q41 auth-credential | 4 | ④支撑(0600/每集成一凭证) |
| q42 control-plane-account | 4 | 弃用(云同步,MVP 单机) |
| q43 message-schema | 5 | ④核心(8 类型联合/工具 4 态) |
| q44 agent-permissions | 5 | ②核心(.env 保护/子 agent 继承) |
| q45 codegen-server | 4 | ④支撑(契约生成/认证三层) |
| q46 provider-peripheral | 6 | ②支撑(Schema 降级/后台任务) |
| q47 layer-node | 5 | ②支撑(声明式依赖树/双 tag) |
| q48 event-manifest | 4 | ④核心(公开/内部分层) |
| q49 core-util | 4 | ②支撑(Token 估算/Wildcard) |
| q50 v1-internals | 7 | 弃用(隐式指令/Flag 细节) |
| q51 patch-parser | 5 | ②核心(四级宽容匹配/derive) |
| q52 session-files-state | 6 | ④支撑(Info 映射/Todo/Reference) |
| q53 v1-tools-system | 3 | 弃用(模型家族提示词/MCP 资源工具) |

**统计**:②执行 24 份 / ④知识库 12 份 / ①对齐 6 份 / ③验收 2 份 / 弃用 12 份(交叉计)
**结论**:核心产品组件(①②④)覆盖 42 份;弃用 12 份均为 V1/外部集成——与蓝图弃用清单一致,无漏抄。

---

## 十二、落地缺口清单(产品需新增的设计)★ review 轮 5 补充

> 审查结论:OpenCode 的 28 个 durable 事件 **全部是 session 级**(aggregate 固定 "sessionID",session-event.ts:38-43),无"章节"概念。产品直接把 OpenCode 事件族当"章节事件"用会缺三件事:

| # | 缺口 | 证据 | 产品设计 |
|---|------|------|---------|
| A | **无章节完成事件** | 28 durable 事件无 chapter 类(Step.Ended 是步骤级) | 新增 `book.next.chapter.completed`(durable,携带规格书 ID + 覆盖 KP + 验收结果)或复用 Step.Ended + 事件类型扩展 |
| B | **规格书不是事件** | 收件箱存 Prompt(session_input),基线存 SystemContext——两者不同数据 | 新增规格书事件族:`spec.admitted`(对齐完成入队)/`spec.promoted`(章节执行开始)/`spec.amended`(用户改目标=时间序更新)——规格书 = 事件溯源的一等公民 |
| C | **聚合粒度** | durable.aggregate 固定 "sessionID"(schema 层硬编码) | 决策:全书一个聚合(seq 全序 = 书籍时间线,章节 = type 过滤)vs 每章一个聚合(需把 aggregate 泛化)——推荐前者(重放顺序=书籍顺序,跨章节一致性最强) |
| D | **章节上下文过滤** | 历史过滤规则只认 compaction/baseline(history.ts:24-53) | 增加"章节边界"过滤:每章开始时 compaction 式摘要(7 段模板),章节视图 = 本页 KP 相关事件 |

**说明**:A/B/C/D 不是"抄不到"的缺陷,而是"OpenCode 没做章节概念"——产品必须自己加。这也证明 D17 第 5 问(自证)的价值:审查发现缺口,蓝图才能落地。
