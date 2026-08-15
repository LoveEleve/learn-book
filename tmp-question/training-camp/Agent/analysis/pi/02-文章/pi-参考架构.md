# Pi 参考架构 — 源码学习 Agent 的设计蓝图

> 项目:earendil-works/pi(main,5,663 commits)
> 生成:2026-08-14
> 输入:7 份闭环笔记(Q1-Q7,1667 行)+ 域发现(59 域)
> 用途:从 Pi 提取"源码学习 Agent"的四组件设计蓝图——抄什么/改什么/弃什么

---

## 一、Pi 架构一句话

> **Pi = 双层循环(内层工具/外层队列)驱动的、事件溯源持久化的、可压缩上下文的、可插拔工具的 Agent 引擎。**

四个定语对应四个核心域:循环(agent-loop)、持久化(session)、上下文(compaction)、工具(tools)。

---

## 二、产品四组件 ← Pi 设计映射总表

| 产品组件 | Pi 抄什么 | Pi 改什么 | 关键笔记 |
|---------|----------|----------|---------|
| ① 对齐模块 | steer/followUp 双队列、三档提问、prompt 模板 | 加"6 维规格书"产出 | Q1/Q6 |
| ② 执行引擎 | 双层循环、钩子契约、错误三层分层 | 加"章节验收挂 shouldStopAfterTurn" | Q4/Q1 |
| ③ 自动验收器 | 契约检查、错误消息=反馈、工具预设分级 | 从 conformance 演化"章节 conformance" | Q5/Q7 |
| ④ 书级知识库 | 事件溯源、CustomEntry、压缩三段式、分支 | 目录按"书籍/章节"组织 | Q2/Q3/Q7 |

---

## 三、组件① 对齐模块(6 维规格书)

### 抄自 Pi

| Pi 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **steer/followUp 双队列** | agent-session.ts:1343-1408 | 对齐中:steer=立即纠正,followUp=排队待办 |
| **提示词模板系统** | harness/prompt-templates.ts(262 行) | 6 维盘问模板(对象/目标/深度/画像/边界/产出) |
| **技能清单注入+按需读取** | system-prompt.ts:63-67 | 领域骨架清单进 prompt,内容按需读 |
| **三档提问**(D12) | 方法论决策 | A 自答/B 推荐确认/C 用户决策 |
| **静态工厂 continueRecent** | session-manager.ts:1557 | "上次分析到哪,继续对齐" |
| **约束采样(结构化输出)** | ai/api/constrained-sampling.ts(277 行) | 规格书强制 JSON Schema |
| **双调用路径(/skill: + 模型自动)** | agent-session.ts:1309-1333 | 用户显式/模型自动触发骨架 |
| **reload 热重载 + resetLeaf 回退** | agent-session.ts:2610 / session-manager.ts:1372 | 用户中途改主意/对齐错了回退 |

### 产品设计(改)

```
对齐产物 = 6 维规格书(执行契约):
  1. 对象/版本/入口点(agent 自查:A 档)
  2. 目标(写书/面试/自用:C 档)
  3. 深度分层(🔴 A/B/C/D:B 档,按画像推荐)
  4. 学习者画像(B 档:引导式科普+确认)
  5. 边界排除(Linux 基准/平台过滤:B 档)
  6. 产出形式(书/大纲/笔记:C 档)
规格书 = 结构化 schema(constrained-sampling 强制 JSON)

★ 规格书三合一(关键设计):
  同一份 spec 同时是:
  - 执行引擎的初始上下文(执行契约)
  - 验收器的契约来源(章节 conformance 基准)
  - 知识库的第一条日志(spec 条目)
  一份 spec 三处消费——避免"三份规格书漂移"

★ 对齐中断处理:
  - 用户中途改目标 → reload 热重载(配置变化不丢状态)
  - 对齐结果不对 → resetLeaf 回退重来
```

---

## 四、组件② 执行引擎

### 抄自 Pi(核心骨架)

| Pi 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **双层循环**(内层工具/外层 follow-up) | agent-loop.ts:155-275 | 产品执行主干 |
| **while(_handlePostAgentRun) 自主续跑** | agent-session.ts:1063-1075 | "一直跑直到无事可做" |
| **钩子契约**(不抛异常+handleRunFailure 兜底) | types.ts:149-293 + agent.ts:502 | 扩展点双层防线 |
| **shouldStopAfterTurn 钩子** | types.ts:222 | **章节完成判定点** |
| **prepareNextTurn 钩子** | types.ts:229 | 分析中切换模型 |
| **错误三层分层**(overflow→压缩/可恢复→重试/致命→放弃) | agent-session.ts:2645 | 错误处理骨架 |
| **immediate 错误结果模式** | agent-loop.ts:600-668 | 失败进上下文,LLM 自纠正 |
| **截断全失败安全** | agent-loop.ts:374-406 | 防残缺参数 |
| **动态工具注册(addedToolNames)** | agent-loop.ts:787 | 自进化机制 |
| **工具预设分级(只读/可写)** | tools/index.ts:138-166 | 学习模式 vs 写书模式 |
| **单状态机多 I/O 壳**(modes 共享) | agent-session.ts:1-14 | 一个核心多 UI |
| **压缩判定 5 重保护**(模型一致性/边界/污染/失败上限) | agent-session.ts:1962-2053 | 防误触发 |
| **溢出恢复失败上限**(compact+retry 仅一次) | agent-session.ts:2001-2012 | 防死循环(收敛性) |
| **内存移除+历史保留双态** | agent-session.ts:2015-2020 | 失败结论可审计 |
| **关键路径强校验+辅助路径弱校验** | agent-session.ts:409-469 | 验收请求设计 |
| **工具钩子拦截**(beforeToolCall) | agent-session.ts:479-491 | 验收器挂载点 |
| **reload 热重载不丢状态** | agent-session.ts:2610-2635 | 配置变更继续分析 |
| **abort 异步等待完全停止** | agent-session.ts:1550-1554 | 用户打断语义 |

### 产品设计(改)

```
学习模式:只读工具集(read/grep/find/ls)——分析不动代码
写书模式:+write/edit——产出书籍文件
模式切换 = setActiveToolsByName + system prompt 重建
章节完成 = shouldStopAfterTurn 返回 true(验收器判据)
```

---

## 五、组件③ 自动验收器

### 抄自 Pi

| Pi 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **契约测试(conformance)** | session/testing/conformance.ts(1016 行) | 章节验收 = 规格书契约检查 |
| **错误消息=反馈** | edit-diff.ts:257-293 | 验收失败信息指导 LLM 怎么改 |
| **系统 prompt 动态组装** | system-prompt.ts:79-138 | 验收模式 prompt 与工具集匹配 |
| **输出防护(output-guard)** | core/output-guard.ts | 输出合法性检查 |
| **技能清单按工具能力条件注入** | system-prompt.ts:63-67 | 无 read 不注入技能清单 |
| **usage 优先/估算兜底双轨** | compaction.ts:216-244 | 精确计量不重复统计 |
| **工具 partial 更新流** | agent-loop.ts:670-711 | 长工具实时反馈 |

### 产品设计(改)— 收敛性验证(#24 的代码级答案)

```
验收器 = 章节 conformance:
  1. 规格书契约检查(覆盖 KP/深度/读者对象/证据链 file:line)
  2. 失败 → 错误消息带"为什么+怎么改"
  3. 连续 N 轮验收无新增问题 → 收敛 → 章节完成
  4. 收敛判定 = 覆盖率达标 + 两轮无新增(不是"agent 说完成了")
  5. 审计:验收结果写入知识库事件日志(可重放)

★ 验收与执行循环的交互(关键设计):
  验收时机 = 每章完成时(不是每轮——每轮验收成本太高)
  验收失败 → 失败消息作为 steer 注入 → LLM 下一轮修正 → 重新验收
  (复用 Pi 的 steer 机制,验收器和执行引擎共用同一循环)

★ 验收器的测试(元验收):
  验收器本身也是代码 → 需要契约测试
  "章节 conformance 套件"测试验收器是否漏检
  (Pi 的 conformance.ts 测存储后端,产品用同样的思路测验收器)
```

---

## 六、组件④ 书级知识库

### 抄自 Pi(核心蓝图)

| Pi 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **JSONL 追加写 + 版本迁移** | session-manager.ts | 知识库存储基础 |
| **8 种条目 + CustomEntry 不参与上下文** | session-manager.ts:46-120 | 结论/证据/章节结构存储 |
| **压缩三段式(摘要+尾巴+新增)** | compaction.ts:420-454 | 章节上下文重建 |
| **增量摘要更新(SUMMARIZATION/UPDATE)** | compaction.ts:529+ | 章节摘要演进不重写 |
| **分支三操作** | session-manager.ts:1360-1505 | 走错路回退+单线导出 |
| **label 书签** | session-manager.ts:110-115 | 结论标记(结论/证据/待验证) |
| **Fencing Token + Lease** | writer-leases.ts | 多进程写知识库防冲突 |
| **事件溯源 + seq 全序** | session-sequences.ts | 可重放/审计/增量读 |
| **归约损坏检测(12 种)** | reducer.ts validateRecordLog | **重放 ≠ 真相,重放+验证才是**——12 种损坏原因枚举(reducer.ts:12-31)作知识库重放校验器;corrupt() 必须拒绝而非修复 |
| **恢复三态(0 空闲/1 挂起/2 损坏)** | session/types.ts | 分析中断恢复 |
| **延迟落盘(无效会话不写)** | session-manager.ts:1015 | 无效探索不写库 |
| **坏行/孤儿容错** | session-manager.ts:503-556, 1310 | 库文件损坏恢复 |
| **事件流水线持久化**(message_end 自动落库) | agent-session.ts:610-670 | 知识库自动记录 |
| **retainedTail 内嵌**(摘要自带尾巴) | context.ts:75-80 | 章节摘要带最近结论 |
| **deferred 消息过滤** | context.ts:72 | 中间推理不污染 |
| **摘要专用 system prompt(防跑偏)** | compaction.ts:424 | 防摘要跑偏(#24) |
| **摘要请求缓存隔离** | compaction.ts:110-114 | 不污染主链路缓存 |
| **对话序列化 + 2000 字符截断** | compaction/utils.ts:91-131 | 摘要输入规范 |
| **精确+模糊匹配 / 错误消息=反馈** | edit-diff.ts:206-293 | 工具容错+反馈驱动 |

### 产品设计(改)

```
目录组织:cwd 编码 → 书籍/章节 编码
  book/
    book-{id}.jsonl          ← 全书日志(追加写)
    chapters/{ch}.jsonl      ← 每章独立(createBranchedSession 演化)
条目类型扩展:
  conclusion(结论:KP + file:line 证据链)
  evidence(证据:源码位置)
  spec(规格书:对齐产物)
  acceptance(验收结果)
label 语义:
  "结论"/"证据"/"待验证"/"已验收"
交接 = 日志本身:
  新 session 加载 = 读日志尾部 + 规格书 + 验收状态
  (不再需要手写交接文档——#21 的自动化)

★ 全书级检索(关键设计,Pi 的 search-backend 被 v1 遗漏):
  Pi 有 search-backend.ts(FTS5 全文搜索)——跨章节引用靠它
  产品:书级检索 = FTS5 索引全书日志
  "第 20 章要引用第 3 章结论" = 检索"结论"label 条目 → 读对应 evidence
  检索结果按"结论→证据→验收状态"链式返回,不返回原始对话
```

---

## 七、正确性三支柱(§5.4 的落地映射)

| 支柱 | 落地位置 | 验证方式 |
|------|---------|---------|
| 事件溯源可重放性 | 知识库日志 | "重放日志 = 相同章节状态"测试 |
| 契约检查 | 自动验收器 | 章节 conformance 套件 |
| 状态机不变量 | 分析状态机 | 合法迁移表 + 守卫 |

---

## 八、Pi 的"弃"清单(产品不做)

| Pi 设计 | 弃的原因 |
|---------|---------|
| TUI 视觉层/components | 产品 CLI 优先 |
| 40+ provider 生态 | 只留 2-3 个 provider |
| OAuth 设备码/PKCE 全套 | 简化 API key |
| 图片处理族(image-*) | 源码分析不需要 |
| 终端原生层(stdin/keys) | 非核心 |
| export-html | 书用 Markdown 输出 |
| 扩展系统全量 | 简化 hooks |

---

## 九、Pi 参考架构的价值总结

1. **四组件全部有 Pi 源码证据**——不是凭空设计,是"抄架构"
2. **正确性有理论支柱**——可重放性/契约检查/不变量
3. **#21 交接自动化**——知识库日志 = 交接,人写文档的活被消掉
4. **#24 收敛性有代码答案**——验收器 = 连续 N 轮无新增
5. **面试弹药**:能讲"我从 Pi 抄了 Fencing Token/事件溯源/双层循环,因为 XXX"

## 九.5 Review 记录(收敛性)

| 轮次 | 发现 | 状态 |
|:--:|------|------|
| v1 | 初始架构(引用 ~25 设计) | 完成 |
| v2 | review:补全 Q1 的 8 个遗漏 + Q2/Q3 的 6 个 + Q6 条件注入 + 约束采样 | 完成 |
| v3 | 产品角度 review:规格书三合一/验收-执行循环交互/元验收/全书检索(search-backend)/对齐中断处理(reload+resetLeaf) | 完成 |

**覆盖检查**:Q1(16)✅ Q2(17)✅ Q3(16)✅ Q4(14)✅ Q5(14)✅ Q6(13)✅ Q7(6)✅
**遗漏率**:v1 约 30%(源码设计遗漏)→ v2 归零(源码覆盖)→ v3 发现产品设计层遗漏 5 个(不是源码遗漏,是"蓝图本身缺设计决策")

**v3 的元教训**:
- 源码覆盖度 review(v2)和产品设计 review(v3)是**两种不同的 review**
- v2 查"Pi 的设计有没有漏抄",v3 查"蓝图本身能不能落地"
- **产品蓝图必须回答:数据怎么流动(spec 三合一)、时机怎么定(验收时机)、失败怎么循环(steer 回注)、大库怎么查(全书检索)、验收器怎么自证(元验收)**

## 十、下一步

- [ ] 用此蓝图进入 Reasonix 分析(验证/补充 Go 版设计)
- [ ] 或直接开始产品 MVP(对齐模块 + 知识库日志原型)
