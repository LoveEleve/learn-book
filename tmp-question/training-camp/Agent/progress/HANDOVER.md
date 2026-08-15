# 交接文档 — Agent 源码分析项目(权威进度)

> **本文件是 Agent 项目的唯一权威进度文档。** 接手前请**完整阅读**本文,再动任何文件。
> 最后更新:2026-08-15(OpenCode + deepseek-harness 完成,详见 HANDOVER-session003.md)
> 分支:git 仓库(父仓 /data/workspace/source-code/book/成长之路,分支 fresh,远端 git@github.com:LoveEleve/learn-book.git)——Agent/ 资产已入库(fe02281,已推送)

---

## 一、任务背景

**最终目标**:做出一个"源码学习 Agent"——输入"帮我分析 JVM 源码",对齐后自主跑完,产出一本好书。

**三条线的关系(重要)**:
```
核心目标:做出来(能跑、能产出书级作品)——产品是主线
支撑手段:方法论(工程规范,防编造/防跑偏)——产品是其质量保障系统
副产品:面试弹药(作品 + 失败案例 + 架构理解)——顺带
```

**产品定位澄清**:产品是**通用的源码学习 Agent**,不是"agent 分析器"。分析对象是任意源码(JVM/Redis/Netty/Docker 等)。4 个 agent 项目(Pi/Reasonix/Hermes/OpenCode)只是**第一批自举样本**(练手验证方法论 + 抄架构 + 沉淀领域骨架)。

---

## 二、方法论框架(先读这些)

```
Agent/
├── progress/
│   ├── methodology-v2-decisions.md      ← 方法论决策总纲(D1-D18 + 支柱 4)★必读
│   ├── jd-cross-analysis.md             ← 10 份 JD 交叉分析
│   ├── source-code-learning-agent-problems.md ← 25 问题 + 产品需求清单(#21/#24/#25)
│   └── HANDOVER-session00X.md          ← 各 session 详细交接(003 最新)
├── jd/                                  ← 10 份 JD 原文
├── methodology/zh/                      ← 正式方法论文件(尚未写!)
├── prompt/zh/                           ← 自约束契约(尚未写!)
├── skills/zh/                           ← 快速参考(尚未写!)
├── analysis/                            ← 各项目分析产出
│   ├── pi/                              ← Pi 分析完成(127 域)
│   ├── reasonix/                        ← Reasonix 分析完成(102 域)
│   ├── hermes-agent/                    ← Hermes 分析完成(81 域,参考架构待写)
│   ├── opencode/                        ← OpenCode 分析完成(53 域 278 设计 + 参考架构)
│   └── deepseek-harness/                ← dsh 分析完成(51 域 126 设计 + 参考架构)
├── harness/                             ← 微缩复现(未开始)
└── progress/                            ← 交接 + 进度
```

### 方法论决策(D1-D17)要点

| 决策 | 内容 |
|------|------|
| D1-D9 | 双锚点域发现/能力域/面试弹药/动态轨/跨项目对比/JD权重/评测Tier0/模型侧/MVP切入点 |
| D10-D11 | Linux 唯一基准 + 平台标签检查 |
| D12-D14 | 三档提问(A自答/B推荐/C决策)/入口自发现/推荐带证据 |
| D15 | 理论层检查(分布式/状态机/契约测试) |
| D16 | 正确性问题检查(断言/不变量/契约/错误语义) |
| D17 | 参考架构落地性检查(数据流/时机/失败循环/大库/自证) |

### 已确认的产品设计(方法论决策文档 §5.4)

**正确性三支柱**:
1. 事件溯源可重放性(知识库 = 追加写日志,重放 = 真相)
2. 契约检查(验收器 = 章节 conformance)
3. 状态机不变量(分析状态机合法迁移)

---

## 三、源码与环境位置

```
4 个 agent 项目源码:/data/workspace/agents/
├── pi/            ← earendil-works/pi(main,5,663 commits)
├── reasonix/      ← esengine/DeepSeek-Reasonix(main-v2,5,453 commits)
├── hermes-agent/  ← NousResearch/hermes-agent(main)
└── opencode/      ← anomalyco/opencode(v1.18.18 tag)

MCP 索引(全部已建):
  data-workspace-agents-pi(18,004 节点)
  data-workspace-agents-reasonix(63,482 节点)
  data-workspace-agents-hermes-agent(107,360 节点)
  data-workspace-agents-opencode(30,601 节点)

其他资产:
  原方法论蓝本:/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/analysis/talk-method/source-code-analysis/(00-09 + prompt + skills)
  用户历史分析:source-code/ 下大量 Java 生态分析(microsphere/spring 等)
```

---

## 四、当前状态(2026-08-15,5 项目分析全部完成)

### 方法论:✅ 决策完成(D1-D18),正式文件未写

- progress/methodology-v2-decisions.md 是决策总纲(含产品设计 §5.4 正确性三支柱 + 支柱 4 质量门禁工程化)
- **D18 新增(2026-08-14)**:穷尽性检查 = 文件体量排序核对,禁止按目录感觉扫描(Pi 假收敛证伪后沉淀)
- **methodology/zh/ 正式文件(00-13)尚未产出** ← 待办

### JD 分析:✅ 完成(10 份)

- 10 份 JD 入库(DS/Kimi/GLM/MiniMax/字节/腾讯×2/美团×3)
- 交叉分析完成(岗位画像 + 覆盖矩阵 + 优先级)

### 问题清单:✅ 完成(25 问题 + 交接协议)

- source-code-learning-agent-problems.md
- 含 #21 会话交接(用户亲历,双文档协议)、#24 收敛性验证、**#25 规范约束失效(2026-08-14 新增,自举项目 Hermes 分析时发生)**
- #25 核心:质量门禁必须工程化,禁止依赖 LLM 自觉遵守文档规范(交接文档写了 review 纪律,agent 仍批量跳过)→ 支柱 4

### Pi 分析:✅ 完成(127 域 + 8 份闭环笔记)

```
analysis/pi/
├── 00-域发现/00-pi-域发现.md        ← 88 域(v1-v9)+ v10-v32 续扫至 127 域 + 40+ 契约深化
│   └── 00-pi-域发现-v7补充.md        ← 体量排序复测(假收敛证伪,59→75)
│   └── 00-pi-域发现-v8补充.md        ← 子目录深扫(Fencing 实现/seq 全序,→85)
│   └── 00-pi-域发现-v9补充.md        ← 测试契约层(Q9/Q10 已答,→88)
│   └── 00-pi-域发现-v10~14补充.md    ← auth/工具加载/搜索/server/存储/遥测/rpc/资源仲裁/OAuth(→117)
│   └── 00-pi-域发现-v15~19补充.md    ← agent 门面/双层循环/tui 原生层/server/测试契约族(→126)
│   └── 00-pi-域发现-v20~28补充.md    ← 测试全覆盖 + 核心实现层内部(settings/resource-loader/model/composer/sdk/compaction)(→127)
│   └── 00-pi-域发现-v29~32补充.md    ← harness 实现层/agent-harness 主体/工具对比(Q1 闭环)/agent-session 验证
├── 01-闭环笔记/(8 份)
│   ├── q1-agent-session(13 设计)
│   ├── q2-session-manager(17)
│   ├── q3-context-compaction(16)
│   ├── q4-agent-loop(14)
│   ├── q5-tools(14)
│   ├── q6-skills(13)
│   ├── q7-distributed-theory(无编号设计,6 个理论发现)
│   └── q8-reducer-event-sourcing(8 设计)  ← 2026-08-14 补漏:12 种损坏原因/Fencing 实现/seq 全序
├── 02-文章/pi-参考架构.md           ← 完成(v3,含 D17 落地性补全 + reducer 损坏检测补行)
└── 03-harness/(未开始)
```

### Reasonix 分析:✅ 完成(102 域 + 9 份闭环笔记)

```
analysis/reasonix/
├── 00-域发现/00-reasonix-域发现.md   ← 41 域(v1-v5)+ v6-v54 补漏/深化至 102 域 + 80+ 契约深化
│   └── 00-reasonix-域发现-review补漏.md ← 体量排序复测(假收敛证伪:controller.go 6,276 行漏 5 轮)
│   └── 00-reasonix-域发现-v7补充.md     ← 策略/契约/存储层(TaskPolicy/TaskContract/TaskIntent)
│   └── 00-reasonix-域发现-v8补充.md     ← 验收报告层(GapKind 8 分类/Claim 分离)
│   └── 00-reasonix-域发现-v9~14补充.md  ← 执行正确性/boot/bot/extension/sandbox/支撑层(→101)
│   └── 00-reasonix-域发现-v15~29补充.md ← taskmonitor/control/checkpoint 验证/acp/能力/目录/shellsafe/pluginpkg/eventwire/支撑层(→102 + 40+ 深化)
│   └── 00-reasonix-域发现-v30~36补充.md ← agent 核心大文件/agentpreset 矩阵/event Kind 全集/permission/compact/MCP OAuth(→102 + 50+ 深化)
│   └── 00-reasonix-域发现-v37~42补充.md ← plugin 宿主/hook 系统/recovery Gate/evidence Receipt/验证族(→102 + 60+ 深化)
│   └── 00-reasonix-域发现-v43~50补充.md ← provider 规范化/技能钉/execute_one 门控链/失败分类/适配族(→102 + 70+ 深化)
│   └── 00-reasonix-域发现-v51~54补充.md ← jobs 管理器核心/收件箱磁盘/chat_tui 消费端/workers(→102 + 80+ 深化)
├── 01-闭环笔记/(9 份)
│   ├── rq1-autoresearch(13 设计) ... rq7-checkpoint(14 设计)
│   ├── rq8-controller(7 设计)  ← 2026-08-14:transport-agnostic 会话驱动(②执行引擎内核)
│   └── rq9-completion(6 设计)  ← 2026-08-14:验收模型(③验收器终极蓝本:Claim/GapKind 8/Verdict 四态)
├── 02-文章/reasonix-参考架构.md      ← 完成(v2,含 D17 4 缺口补全)
│   ⚠ 待检查:是否引用 Controller 模式 + TaskContract/TaskPolicy + GapKind?(v6-v29 补漏后需核对)
└── 03-harness/(未开始)
```

### Hermes 分析:✅ 全部完成(81 域 + 41 笔记 262 设计 + 参考架构 v3)★ 2026-08-15

```
analysis/hermes-agent/
├── 00-域发现/00-hermes-域发现.md    ← 80 域(v1-v11)+ v12-v46 深化(81 域 + 70+ 契约深化,排除面全确认)
│   └── 00-hermes-域发现-v12~26补充.md ← 顶层大文件对账(gateway/run.py 29K 等 17 个 >5K 文件)
│   └── 00-hermes-域发现-v27~46补充.md ← GoalGate/验证守卫族/区间全覆盖/moa_trace/tui_gateway/cron 蓝图/启动安全/排除复核/transports/契约测试面
├── 01-闭环笔记/(41 份,262 设计)  ← 2026-08-15 全域闭合:原 7 份 + 补充 34 份(每份深度 review)
│   ├── hq1-hq7(87 设计)         ← 核心 7 份:记忆/技能/会话库/压缩/主循环/委派/验证
│   ├── hq8-hq18(71 设计)        ← 补充 11 份:回合租约/交付账本/生命周期账本/检查点/背景审查/工具结果/文件协调/审批/ESTOP/秘密作用域/目标审查器
│   └── hq19-hq43(104 设计)      ← 补充 25 份:缩放至零/停滞/流式分发/输出上限/自仓库/学习图谱/工具搜索桥/监控/洞察/MoA/辅助客户端/平台抽象/授权/进程注册表/浏览器/批量/MCP/SWE/Cron/插件/CLI/小件 4
├── 02-文章/hermes-参考架构.md      ← 完成(v3:41 笔记 262 设计全对账 + 81 域全闭合 + 决策 20 条 + 弃用调和)★ 2026-08-15
└── 03-harness/                        ← 未开始
```

### 未开始(确认状态)

- 产品 MVP(未开始)
- 方法论正式文件(methodology/zh/ 空)
- harness 微缩复现(harness/ 空)

### OpenCode 分析:✅ 完成(53 域 + 53 份闭环笔记 278 设计 + 参考架构)★ 2026-08-15
```
analysis/opencode/
├── 00-域发现/00-opencode-域发现.md   ← 53 域 + 9 共享包(v1-v10 review:包级核对/共享包/D15 理论层/D16 正确性/测试覆盖/JD 覆盖)
├── 01-闭环笔记/(53 份,278 设计)
│   ├── q1-q13:V2 核心(EventV2/SystemContext/Runner/Input/Epoch/Compaction/Permission/Tools/Projector/Facade/Coordinator/V1/LLM)
│   ├── q14-q18:技能/ACP/快照/共享包/Policy
│   ├── q19-q24:内置工具/MCP OAuth/协议层/Database/消息翻译/Move+Plugin
│   ├── q25-q30:FileMutation+Git/剩余工具/Question+State/Config/V1 消息/Server 接线
│   ├── q31-q36:可观测/Location 路由/V1 工具注册表/V1 LLM/项目服务/V1 会话+LSP
│   ├── q37-q42:effect 运行时/V1 压缩/AISDK/文件系统/认证/控制面
│   ├── q43-q46:消息 schema/Agent+子权限/生成器+中间件/Provider+外围
│   └── q47-q53:layer-node/事件清单/util/V1 内部/补丁解析/会话小文件/V1 工具桥
├── 02-文章/opencode-参考架构.md      ← 完成(四组件映射 + D17 5 问全有答案 + 产品决策清单 10 条 + 弃用清单 + 三项目合并表)
└── 03-harness/(未开始)
```

### DeepSeek Harness 分析:✅ 完成(51 域 + 26 份闭环笔记 126 设计 + 参考架构)★ 2026-08-15

```
analysis/deepseek-harness/            ← 源码已拉(deepseek-ai/deepseek-harness,pnpm 11.7,60,780 节点 MCP 索引)
├── 00-域发现/00-deepseek-harness-域发现.md ← 51 域 + 10 核心域(v1-v10 review:图谱/源码/测试契约/对账)
├── 01-闭环笔记/(26 份,126 设计)
│   ├── q1-q6:Session 日志/AgentLoop/Tools/Cordis/LLM/Profile-Bundle
│   ├── q7-q12:能力缝/Session 持久化/Interaction/Sandbox/三缝样本/SDK-API-Typert
│   ├── q13-q19:Guard-Goal-Jobs/Settings-Credentials/支撑域/Repair-Terminal-LSP/投影/Web-Hooks/LLM 适配
│   └── q20-q26:Session 查询/Inbox 工具调用/Code-Mode-E2B/Agent 生命周期/子代理 Provider/Hooks-Compaction/调度-Settings
├── 02-文章/deepseek-harness-参考架构.md ← 完成(四组件映射 + D17 5 问 + 决策清单 12 条 + 弃用 7 项 + 覆盖对账 + 缺口 4 项)
└── 03-harness/(未开始)

核心特征:一切皆插件(vendored Cordis)+ 能力缝三角色 + 模型可见⟺已记录(运行时不变量)
产品价值:四组件插件化极端样本;重复提醒=收敛性检测;每调用沙箱政策;writer 决定版本 bump
测试契约:734 测试文件 23 万行,11 份核心笔记已补行为契约(追加不变性/JSON 严格性/HMR/continuation 70+/压力决策)
```

---

## 五、接手须知(给下一个 AI)

### 必读文件最小集合

```
/data/workspace/source-code/book/成长之路/tmp-question/training-camp/Agent/
├── progress/HANDOVER.md   ← 本文件(权威进度——先读)
├── progress/HANDOVER-session003.md ← 最新详细交接(2026-08-15,OpenCode+dsh)
├── progress/HANDOVER-session002.md ← Hermes/Pi/Reasonix 追挖 + 方法论 D18/支柱4
├── progress/HANDOVER-session001.md ← session001 详细交接(参考)
├── progress/methodology-v2-decisions.md   ← 方法论决策总纲(先读,D18 + 支柱 4)
├── progress/jd-cross-analysis.md          ← JD 分析
├── progress/source-code-learning-agent-problems.md ← 问题清单(#21/#24/#25)
├── analysis/pi/02-文章/pi-参考架构.md      ← Pi 蓝本(参考)
├── analysis/reasonix/02-文章/reasonix-参考架构.md ← Reasonix 蓝本(参考)
├── analysis/opencode/02-文章/opencode-参考架构.md ← OpenCode 蓝本(参考)
└── analysis/deepseek-harness/02-文章/deepseek-harness-参考架构.md ← DS 蓝本(参考)
```

### JD 分析核心结论(下个 session 快速参考)

```
10 份 JD(DS/Kimi/GLM/MiniMax/字节/腾讯×2/美团×3)
关键共识:评测 8/10、可观测 7/10、上下文管理 6/10、Sandbox 5/10、MCP 5/10、Skills 5/10
岗位画像:
  DS=懂模型的 Agent 研究员 / Kimi=懂工程的 Agent 工程师
  GLM=懂数据的 Agent 评测专家 / MiniMax=懂安全的 Agent 基建专家
  字节=懂平台的 Agent 运行时专家 / 腾讯A=懂业务的 Agent 系统专家
  腾讯B=懂质量的 Agent 可观测专家 / 美团=懂效果的 Agent Harness 工程师
面试准备优先级:Context Engineering 最厚 → Reasonix 必吃透 → 评测+可观测双核心
```

### 问题清单核心(#21/#24/#25 速记)

```
#21 会话交接:上下文满→写交接文档→新 session→读文档继续
  ——用户已手工验证,产品④"交接=日志"是自动化答案
#24 收敛性验证:agent 说"完成"不可信,只有 review 循环可信
  ——"完成"= 连续 N 轮无新增 + 覆盖对账,不是 agent 主观声明
#25 规范约束失效(2026-08-15 实证):交接文档写了 review 纪律,agent 仍批量跳过
  ——质量门禁必须工程化,禁止依赖 LLM 自觉遵守文档规范(→ 支柱 4)
  ——域发现层面:#24 加深 = agent 连 review 循环都会跳过,穷尽性靠体量排序对账(D18)
```

### 核心纪律

1. **产品优先**:面试是副产品,产品(源码学习 Agent)是主线
2. **能自查的绝不问用户**(D12/D13);问必带推荐+证据(D14)
3. **Linux 唯一基准**(D10);平台标签检查(D11)
4. **结论必须有 file:line**(信任基础设施)
5. **每轮 review 的含金量取决于打开哪个层面**——深挖"被多个域依赖的共享包"
6. **收敛性**:连续 N 轮无新增才算完成;覆盖检查用"设计数对账"不是"感觉全了"
7. **D17 落地性检查**:参考架构产出后必须过 5 问(数据流/时机/失败循环/大库/自证)
8. **支柱 4(2026-08-15)**:质量门禁必须程序化强制——每份笔记写完 review 再推进,用户确认后再写下一份
9. **D18(2026-08-15)**:穷尽性 = 文件体量排序 × 已打开清单对账,禁止按目录感觉扫描

### 下一步决策(用户确认)

1. **讨论 Agent 产品** ← **下一步(下个 AI Session 讨论,本 session 不讨论)**:输入 = 5 份参考架构(Pi/Reasonix/OpenCode/dsh/Hermes)+ 5 项目闭环笔记
2. **Hermes 参考架构** ✅ 完成(2026-08-15:v3,41 笔记 262 设计全对账 + 81 域全闭合)
3. **跨项目沉淀**(5 项目参考架构合并 → 产品最终架构决策文档)——产品讨论后开工
4. **方法论正式文件**(methodology/zh/ 00-13)——产品的前置质量规范
5. **产品 MVP**(对齐模块 + 知识库日志 + 验收器)——产品讨论定案后开工
6. **git**:Agent/ 资产已入库(fe02281 已推送);Hermes 41 笔记 + 参考架构 v3 待本次提交

---

## 六、git 约定(本目录)

- **Agent/ 已在 git 仓库**——/data/workspace/source-code/book/成长之路(分支 fresh,远端 git@github.com:LoveEleve/learn-book.git)
- **2026-08-15 已入库**:fe02281(236 文件,四项目分析资产 + JD + progress),已推送
- 提交规则:只提交 Agent/ 相关文件,不碰其他项目未提交改动;提交前 git status 确认
- 分析产出(闭环笔记/域发现/参考架构)随 session 及时提交,防丢失

---

## 七、交接约定(切换 session 时)

上下文满了切换 session 时,按此流程:

1. **写新的 `progress/HANDOVER-session00X.md`** 记录本 session 完成单元、产出、未决问题
2. **更新 `HANDOVER.md`** 的"当前状态"(§四)
3. 下个 session 接手:先读 HANDOVER.md → 方法论决策 → 继续未完成单元

**本次移交的教训(重要)**:
- 交接文档本身也需要深度 review(#24 实证)——本 session 写了 2 份交接文档,review 后修正了设计数(84→87/107→100)、补了 git 约定、补了 review 明细、补了交接约定
- **交接文档里的事实(数字/路径)必须可验证**——修正设计数靠 grep 核对
- 用户可随时中断纠正("不对""重新讲")——这是安全阀,不是违例
