# 交接文档 — Agent 项目 Session 详细交接(2026-08-15,OpenCode + DeepSeek Harness)

> 本文件记录 2026-08-15 本 session 的详细过程、方法、发现与教训。
> 承接:HANDOVER.md(权威进度)+ HANDOVER-session002.md(另一侧:Hermes/Pi/Reasonix 追挖)
> 本 session 完成:**OpenCode 全量分析 + DeepSeek Harness 全量分析 + 双参考架构 + 环境准备(5 项目可运行)+ 双 MCP 索引**
> 本 session 未做:Agent 产品讨论(用户明确:留到下一个 AI Session)

---

## 一、本 session 时间线(按顺序)

### 阶段 1:OpenCode 分析(承接 session001 待办 #2)

1. **启动**:用户确认由我分析 opencode(另一侧在弄 harness);读 HANDOVER.md + 方法论决策总纲
2. **Pass 0**:AGENTS.md/CONTEXT.md(225 行,会话运行时概念词典)/specs/v2/(session/tools/provider-model 等)——设计文档极全,域地图清晰
3. **Pass 1**:包级全量扫描(core/opencode/schema/protocol/server/llm/plugin 等 32 包)
4. **域发现 v1-v10**:10 轮 review(包级全量核对补 10 域 → 共享包 S1-S9 → D15 理论层 → D16 正确性 → 测试覆盖 → 一致性修复)
5. **闭环笔记 q1-q53(278 设计)**:8 轮深挖,每份 2-3 轮 review 起,核心域含测试契约(session-runner 44 契约/event 44 契约/permission 11 契约等)
6. **参考架构 v2**:四组件映射 + D17 5 问 + 覆盖对账表(53 笔记全映射)+ 落地缺口清单 4 项(A-D)+ 6 轮 review(设计数对账 274→278 修正)

### 阶段 2:环境准备(用户任务)

7. **deepseek-harness 拉取**:git clone git@github.com:deepseek-ai/deepseek-harness.git(用户提供地址)
8. **5 项目依赖安装**:
   - pi: npm install(336 包)
   - reasonix: go mod download(含 go1.26.6 工具链)
   - hermes-agent: npm install(1294 包,需先升 npm 12.0.2)+ uv sync
   - opencode: bun install(4715 包;bun 1.3.14 新装)
   - deepseek-harness: pnpm install(925 包;pnpm 11.7.0 新装)
9. **双 MCP 索引**:opencode(已建)+ deepseek-harness(新建 60,780 节点,持久化 artifact)

### 阶段 3:DeepSeek Harness 分析

10. **Pass 0**:AGENTS.md(一切皆插件/vendored Cordis/能力缝三角色)/architecture.md(129 行,Turn flow 权威)
11. **Pass 1**:219 叶子包全扫描
12. **域发现 v1-v10**:图谱验证 + 核心源码验证(session/agent-loop)+ 引用验证 + 对账
13. **闭环笔记 q1-q26(126 设计)**:6 轮深挖
14. **测试契约轮**(用户质疑"内容怎么这么少"触发):发现 734 测试文件 23 万行一份没读 → 补 11 份核心笔记契约
    (追加不变性/JSON 严格性/HMR/continuation 70+/压力决策/时间精确/定界符中和)
15. **参考架构**:四组件映射 + D17 5 问 + 决策清单 12 条 + 弃用 7 项 + 覆盖对账 + 缺口 4 项
16. **收尾对账**:设计数 110→125→144 各轮有误,最终程序化统计 = **126**(以域发现 v10 为准)

### 阶段 4:交接 + 收尾

17. **HANDOVER.md 同步**(OpenCode/dsh 状态已更新)
18. **本交接文档**(session003)
19. **用户决策**:下一步 = 讨论 Agent 产品,**在下一个 AI Session 讨论,不在本 session**

---

## 二、本 session 完成单元(产出清单)

### OpenCode 分析(analysis/opencode/)

| 目录 | 产出 |
|------|------|
| 00-域发现/ | 00-opencode-域发现.md(53 域 + 9 共享包,v1-v10 review) |
| 01-闭环笔记/ | **53 份,278 设计**(q1-q53) |
| 02-文章/ | **opencode-参考架构.md(12 章)**:四组件映射 + D17 5 问 + 决策清单 10 条 + 弃用 7 项 + 三项目合并表 + 覆盖对账表 + 落地缺口 4 项(A-D) |
| 03-harness/ | 空 |

### DeepSeek Harness 分析(analysis/deepseek-harness/)

| 目录 | 产出 |
|------|------|
| 00-域发现/ | 00-deepseek-harness-域发现.md(51 域 + 10 核心,v1-v10 review) |
| 01-闭环笔记/ | **26 份,126 设计**(q1-q26,11 份核心含测试契约) |
| 02-文章/ | **deepseek-harness-参考架构.md(12 章)**:四组件映射 + D17 5 问 + 决策清单 12 条 + 弃用 7 项 + 合并要点 + 覆盖对账 + 缺口 4 项 |
| 03-harness/ | 空 |

### 环境与基础设施

| 项 | 状态 |
|----|------|
| deepseek-harness 源码 | ✅ 已拉(47f943859b) |
| 5 项目依赖 | ✅ 全部可运行(pi/reasonix/hermes/opencode/dsh) |
| 工具链 | bun 1.3.14 / pnpm 11.7.0 / uv 0.12.5 / npm 12.0.2(升级) |
| MCP 索引 | opencode(30,601)+ deepseek-harness(60,780)均可用 |

---

## 三、方法论运用记录

### OpenCode(10 轮 review 的层面)

```
轮 1:包级全量核对(补 10 域:Policy/LocationMutation/GlobalBus/Command/Repository/Shell/Process/Global/ProjectV2/opencode-effect)
轮 2:共享包/依赖检查(补 S1-S9:Location/Database/FSUtil/Wildcard/Snapshot/Token/Flock/KeyedMutex/SyncFence)
轮 3-4:D15 理论层(Fencing/文件锁/KeyedMutex/日志全序/快照隔离/乐观并发/双阶段)+ D16 正确性(9 机制)
轮 5:测试覆盖核对(全域有对应测试)
轮 6:内部一致性修复
轮 7-10:参考架构 6 轮 review(设计数对账 274→278/覆盖对账表/源码引用 8/8/产品缺口 2 处/落地缺口 A-D/失败路径标注)
```

### dsh(用户"内容这么少"纠正后补深)

```
用户对比:OpenCode 30,601 节点 → 53 笔记;dsh 60,780 节点(2 倍)→ 当时仅 26 笔记(1/3)
根因:①没读测试契约(734 文件 23 万行)②核心文件没读完 ③域发现 review 不足 ④无 D15/D16 ⑤漏跨项目线索(pi-ai 依赖)
补救:测试契约轮 ×2(11 份核心笔记补行为契约)+ 引用验证 + 对账
```

### 关键方法:D18 体量排序对账(另一侧沉淀,本 session 借鉴)

- OpenCode 域发现用了"包级全量核对"而非体量排序——但核心文件行数(>400 行)均有核对
- dsh 收尾用程序化设计数统计(python 脚本)而非感觉——**发现 v5-v9 各轮汇总误差,最终 126**

---

## 四、关键发现汇总(产品蓝图核心)

### OpenCode 独有贡献(进 opencode-参考架构)

| 组件 | 贡献 |
|------|------|
| ② 执行 | **循环状态在 DB(收件箱,可重放)**/异常转移压缩(改循环零污染)/unsettled 兜底矩阵/崩溃先失败化 |
| ④ 知识库 | 事件溯源三支柱(版本化事件/投影器同事务/重放四级校验)/owner 三级栅栏/收件箱双游标 |
| ① 对齐 | Context Epoch(基线不可变+时间序更新)/SystemContext 四态代数/Unavailable=stale-while-revalidate |
| ③ 验收 | 事件发布管线(12 映射)/Step 快照对/错误契约(域 vs 基础设施) |

### dsh 独有贡献(进 deepseek-harness-参考架构)

| 组件 | 贡献 |
|------|------|
| ② 执行 | **一切皆插件(能力缝三角色)**/每调用沙箱政策/融合分派(subject 与 scope 不分歧)/repeat 3/5/8 收敛检测 |
| ④ 知识库 | **模型可见 ⟺ 已记录(运行时不变量)**/Surface 双视图(append/replace 阴影)/writer 决定版本 bump |
| ① 对齐 | Profile/Bundle 组合(规格书=顶层 patch)/审批政策 ask\|never/指令投影(两提交边界) |
| ③ 验收 | 恢复指导语义(只读/幂等才重试)/压缩压力决策(统一测量+工具对不可分) |

### 跨 5 项目合并全景(产品架构定论输入)

```
循环:OpenCode DB 收件箱双层 + dsh InboxTarget 双队列
上下文:OpenCode Epoch 基线 + dsh profile 覆盖模型
验收:OpenCode 事件管线 + dsh 收敛检测 + Reasonix 独立审查器 + Hermes GoalGate
知识库:dsh 不变量 + OpenCode 双游标 + Reasonix subject 冲突 + Pi reducer 损坏检测
规格书:Reasonix TaskSpec + OpenCode Epoch 基线 + dsh patch 覆盖
```

---

## 五、教训清单

### 用户纠正记录

1. **"先删掉全部,从域发现重新开始"**(OpenCode 初期):我批量写了 9 份闭环笔记未经用户 review 域清单——用户要求域发现经 review 后才写笔记
2. **"内容怎么这么少"**(dsh 收尾期):用户对比 OpenCode/dsh 体量,发现 dsh 分析深度不足(测试契约/核心文件未读)——**体量对比是深度自查的镜子**
3. **"对账错误"**(收尾):v5-v9 设计数汇总有误差,程序化统计才得 126——**数字必须可验证**

### 分析教训

1. **域发现必须先于笔记,且经 review**:用户两次纠正同类问题(批量/跳过 review)
2. **测试契约是行为契约的黄金证据**:dsh 734 测试文件 23 万行,核心契约全在测试里(追加不变性/HMR/continuation)
3. **体量对比自查**:新项目分析时对照已分析项目(节点数/笔记数)评估深度
4. **设计数必须程序化对账**:手写汇总会漂移(110/125/144 均错,126 才真)

---

## 六、未决问题与待办

### 下一步(用户明确决策)

- [ ] **讨论 Agent 产品**(下个 AI Session)——本 session 不讨论;输入 = 5 项目参考架构齐备

### 分析待办(其余项目)

- [ ] Hermes 参考架构(另一侧 session002 待办,唯一缺口)
- [ ] 跨项目沉淀(5 项目参考架构合并 → 产品最终架构决策文档)

### 产品 MVP 待办

- [ ] 对齐模块(6 维盘问 + 三档提问 + 规格书 schema)
- [ ] 书级知识库日志(subject 冲突 + 事件溯源)
- [ ] 验收器(独立审查器 + 验收算法)
- [ ] OpenJDK 验证闭环

### 其他待办

- [ ] 方法论正式文件(methodology/zh/ 00-13)
- [ ] prompt/zh/self-constraint-prompt.md + skills/zh/01-快速参考.md
- [ ] harness 微缩复现(harness/ 空)

---

## 七、接手须知(下个 AI 第一件事)

1. **必读**:HANDOVER.md(权威)→ 本文件 → HANDOVER-session002.md → methodology-v2-decisions.md(支柱 4/D18 铁律)
2. **状态速览**:
   - **5 项目分析全部完成**:Pi(127 域)/Reasonix(102 域)/Hermes(81 域,参考架构未写)/OpenCode(53 域 278 设计)/dsh(51 域 126 设计)
   - 参考架构:Pi ✅ / Reasonix ✅ / OpenCode ✅ / dsh ✅ / **Hermes ❌(唯一缺口)**
   - 环境:5 项目全部可运行(pi=npm/reasonix=go/hermes=npm+uv/opencode=bun/dsh=pnpm)
3. **下一步 = 讨论 Agent 产品**(用户明确,不在本 session 讨论):输入 = 4 份参考架构 + 5 项目闭环笔记
4. **git 待提交**:本 session 资产(OpenCode 53 笔记 + dsh 26 笔记 + 双参考架构 + 本交接)未提交;
   `git status` 当前 47 个文件(含非 Agent 项目的 M 状态文件,如 source-code/analysis/** 等,禁止混入);
   提交规则:只暂存 Agent/ 路径下的文件(`git add tmp-question/training-camp/Agent/...`),不碰其他项目未提交改动
5. **铁律**:
   - 质量门禁必须程序化(支柱 4)——不批量跳过 review
   - 穷尽性 = 体量排序对账(D18)——"感觉到底"不可信
   - 设计数程序化统计(本 session 教训:手写汇总漂移,110/125/144 均错,126 才真)
6. **交接更新**:本 session 结束时同步 HANDOVER.md §四(已同步)+ 本文件
