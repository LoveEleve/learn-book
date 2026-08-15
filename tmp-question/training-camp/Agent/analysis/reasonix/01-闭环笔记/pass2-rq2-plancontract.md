# 闭环笔记 RQ2:PlanContract — 产品①规格书完整数据模型

> 域:internal/plancontract/(plan.go 301 行 + order.go 155 行 + diff.go 141 行 + render.go 123 行 + project.go 33 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

PlanContract 是"计划的数据模型",核心哲学"计划是数据不是 prose"。它的 Plan/Step/Criterion/Verification 结构与 Normalize/Validate/Ordered/Diff 操作,是产品①规格书的完整蓝本。

## 验证过程

### 1. 核心哲学(doc.go:5-14)— 规格书的第一原则

> "The plan is data, not prose. A planner that writes markdown forces the host to guess at structure... every guess is a heuristic that fails silently. Here those are fields."

**两条规则**:
1. **Identity 是 host 分配的**:ID/Revision 由 host 盖章,planner 不提交——审批门能 diff 而不是重新播种
2. **证据不模糊**:VerifiedFiles(实际读过)vs CandidateFiles(推断)——猜测不能当事实

### 2. Plan 数据结构(plan.go:17-25)— 规格书 schema

```go
type Plan struct {
  ID               string   `json:"-"`  // host 分配!
  Revision         int      `json:"-"`
  Objective        string
  Assumptions      []Assumption   // 未验证前提 + Confirm(最便宜的验证方法)
  NonGoals         []string
  Steps            []Step
  RequiresApproval bool
}
```

**关键设计**:
- **ID/Revision 带 json:"-"**:planner 无法提交身份——身份只能 host 给
- **Assumption.Confirm**:每个假设带"最便宜的验证方法"——**假设可验证性**

### 3. Step 结构(plan.go:37-47)— 执行单元

```go
type Step struct {
  ID / ParentID    // 两层结构:phase + sub-step
  Title
  DependsOn        // 兄弟依赖(advisory,投影成顺序不是门控)
  VerifiedFiles    // 实际读过的路径
  CandidateFiles   // 推断的路径
  Acceptance       []Criterion    // 验收标准!
  Verification     []Verification // 命令级验证!
  Risks            []string
}
```

**这是产品②执行契约的完整结构**——每个步骤自带:
- 验收标准(Acceptance)
- 命令级验证(Verification:command + expect)
- 风险(Risks)
- 证据分离(Verified vs Candidate)

### 4. Criterion 与 Verification(plan.go:52-64)— 验收/验证契约

```go
type Criterion struct {
  ID         string `json:"-"`  // host 分配,可作证据 key
  Text       string
  Regression bool   // 必须保持通过的行为
  Optional   bool   // 可选,永不阻塞
}

type Verification struct {
  Command string  // 空 = 接受任何 delivery-verification 命令
  Expect  string  // 通过长什么样
}
```

**产品③验收器直接抄**:
- Regression = "回归必须通过"(现有行为不能破坏)
- Optional = "可选不阻塞"
- Verification.Command/Expect = "怎么验证 + 通过标准"

### 5. Normalize/Validate(plan.go:69-240)— 修复与拒绝分离

```
Normalize(永不失败):修剪/去空/分配 ID/修复 parent 和 dependency 引用
  → assignStepIDs(plan_step_01)/assignCriterionIDs(c1/c2...)
  → repairParents(两层扁平化)/repairDependencies(去环)

Validate(拒绝缺陷):
  → objective 必填 / steps 非空 / MaxSteps=50
  → step id 唯一 / 依赖环检测(sortSiblings)
  → 错误全部收集(errors.Join),planner 一轮修完
```

**产品映射**:**规格书也分"可修复(Normalize)vs 可拒绝(Validate)"**——能修的自动修,不能修的明确拒绝,错误一次给全。

### 6. Ordered 单排序源(order.go:1-35)— 防漂移

```
Ordered():phase 按依赖排序 + 每个 phase 的子步骤排序
Render 和 ProjectTodos 都读它——"用户审批的列表"和"host 播种的任务列表"永远一致
```

**产品映射**:产品"用户看的章节计划"和"agent 执行的任务列表"必须读同一个 Ordered——**防两处不一致**(D17 数据流一致性)。

### 7. Diff 版本差异(diff.go)— 审批门的基础

```
Diff{Objective变化, Added, Removed, Changed, Preserved}
按 ID 配对(不是按位置/标题)——能区分"移动"和"替换"
StepChange.Fields:标注哪些字段变了("标题重写"vs"验收标准重写"风险不同)
```

**产品映射**:规格书修改审批 = diff——用户看到"这次改了哪些步骤、哪些验收标准",不用重读全计划。

### 8. Render/ProjectTodos 链路(render.go + project.go)— review 新增

**Render(用户看到的)**:
- 只有步骤是列表项(phase 编号/sub-step 缩进)——**解析任务列表的 reader 只看到步骤**
- **RequiresApproval 故意不渲染**(render.go:12-14):它是 host 的路由请求,不是计划内容
- **continuation() 剥离列表标记**(render.go:35-49):planner 把假设写成 bullet 不能混入步骤列表——**注入防护**
- 证据/检查渲染为**无标记续行**(renderDetail):不干扰任务列表解析

**ProjectTodos(执行器看到的)**:
- 计划 → 串行任务列表(phase 0 级/sub-step 1 级)
- **TodoItem.StepID**:完成状态携带步骤 ID——**重计划时重命名/插入不丢完成状态**
- NormalizeSerialTodos + ValidateSerialTodos:投影前校验,失败返回 nil(fail-closed)

**renderDetail 的验收渲染哲学**(render.go:87-96):
```go
// The id is rendered because a proof has to cite it: a criterion the
// executor cannot name is one it cannot satisfy.
fmt.Fprintf(b, "%saccept [%s]: %s\n", indent, c.ID, c.Text)
```
**"证明必须能引用判据 ID"**——执行器不能命名的判据是无法满足的判据。**这是证据链的设计哲学**:验收标准 ID 必须可被证据引用。

### 9. 测试即契约(5 个测试文件)— review 新增

关键行为契约:
```
TestNormalizeIsIdempotent        → Normalize(Normalize(p)) == Normalize(p)(幂等!)
TestRenderListItemsMatchTheProjectedTodos → 渲染列表 = 投影任务列表(一致性)
TestOrderedKeepsEveryStepThroughADependencyCycle → 环中步骤不丢失
TestProjectTodosCarriesStableStepIdentity      → 重计划完成状态不丢
TestValidateRejectsRawDuplicateIDs             → 原始重复 ID 拒绝
TestCompareParesByIdentityNotPosition          → diff 按身份不按位置
```

**产品启示**:规格书/计划系统的测试应同样定义:
- 幂等性(Normalize 两次结果相同)
- 一致性(用户视图 = 执行列表)
- 容错(环中不丢步骤)
- 身份稳定(重计划不丢状态)

### 10. NeedsApproval:扩张才审批(diff.go:84-99)— review 第三轮新增

```go
func (d Diff) NeedsApproval() bool {
  if d.Objective != nil || len(d.Added) > 0 { return true }
  for _, change := range d.Changed {
    if grew(Risks) || grew(Acceptance) || grew(VerifiedFiles) || grew(CandidateFiles) {
      return true
    }
  }
  return false
}
```

**审批语义(TestNeedsApprovalOnlyOnExpansion 定义)**:
```
需要审批(扩张):+ 新步骤/新目标/新风险/新判据/更宽候选文件面
不需要审批(收窄):- 删步骤/改标题/重排序
```

**这是产品审批门的黄金设计**:
- **范围收敛(收窄)不需打扰用户**——删除/改词/重排自动通过
- **范围扩张必须用户点头**——新增必须重新确认
- 精准语义:目标变或新增,或任何列表字段"增长"才需要审批

**产品映射**:规格书修改审批 = NeedsApproval 语义——"章节计划扩大范围要用户确认,收窄自动通过"。

### 11. 身份稳定:相同标题的步骤保持不同身份(order_test:63-90 + project_test:54-77)— review 第四轮新增

**TestProjectTodosCarriesStableStepIdentity 的核心注释**:
```go
// The plan already knows each step's identity; dropping it at the projection
// boundary is what forces completion attribution back onto title and index.
```

**测试用 `{"change the API", "change the API"}`(故意相同标题)验证**:
- StepID 必须唯一——相同标题步骤保持不同身份
- 完成状态归因到 ID,不是标题/索引
- 投影边界不丢身份

**TestProjectTodosAlwaysSatisfiesTheSerialContract**(project_test:33-53):5 种畸形计划(flat/单 phase/孤儿/深层嵌套/环)投影**永远满足串行契约**——健壮性测试。

**TestOrderedKeepsEveryStepOnUnnormalizedInput**(order_test:76-93):
> "Ordered must be total: a caller that skips Normalize gets the same steps, grouped the same way, never a silently shorter list."

**Ordered 是全程的**:跳过 Normalize 也不丢步骤——**不变量:输入 → 输出步骤数不变**。

**产品启示**:
- 任务/章节完成状态必须绑定 ID(不是标题/索引)——标题相同也区分
- 投影必须健壮(畸形输入不违反契约)
- Ordered/投影必须"全程"(不丢步骤)

## 代码类型

Data Model + 算法(Normalize/Validate/Ordered/Diff)

## 跨域关联

- → 被消费:render.go(渲染)、project.go(任务投影)、control/plan_seed(播种)
- ← 关联:autoresearch TaskSpec(更简单的规格书,plancontract 更完整)

## 结论

核心可抄设计 12 个:
1. **"数据不是 prose"哲学** → 规格书第一原则
2. **host 分配 Identity**(ID/Revision json:"-")→ 审批可 diff
3. **Assumption.Confirm** → 假设可验证性
4. **Step 自带 Acceptance/Verification/Risks** → 执行单元契约
5. **VerifiedFiles vs CandidateFiles** → 证据不模糊
6. **Normalize/Validate 分离** → 修复 vs 拒绝
7. **Ordered 单排序源** → 防用户视图和执行列表漂移
8. **Diff 按 ID 配对 + 字段级变更** → 审批门
9. **Render/ProjectTodos 一致性 + 注入防护 + 判据 ID 可引用** → 用户视图/执行列表/证据链
10. **测试定义幂等/一致/容错/身份稳定** → 规格书系统的测试契约
11. **NeedsApproval 扩张才审批**(收窄自动通过)→ 审批门黄金语义
12. **身份稳定 + 投影全程**(相同标题步骤区分/畸形输入不违约/不丢步骤)→ 完成状态绑定 ID

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| Plan 数据模型 | ✅ 抄 | 产品规格书 schema(Objective/Assumptions/NonGoals/Steps) |
| host 分配 ID | ✅ 抄 | 章节/任务 ID 由产品分配 |
| Assumption.Confirm | ✅ 抄 | 对齐阶段"假设 + 验证方法" |
| Step.Acceptance | ✅ 抄 | 章节验收标准 |
| Step.Verification | ✅ 抄 | "怎么验证 + 通过标准" |
| Verified/Candidate | ✅ 抄 | 证据分离(file:line 实际读 vs 推断) |
| Normalize/Validate | ✅ 抄 | 规格书自动修复 + 明确拒绝 |
| Ordered 单源 | ✅ 抄 | 用户视图 = 执行列表 |
| Diff | ✅ 抄 | 规格书修改审批 |
| Render 注入防护 | ✅ 抄 | 章节内容不能混入任务列表 |
| 判据 ID 可引用 | ✅ 抄 | 证据必须能引用验收标准 ID |
| **NeedsApproval 扩张语义** | ✅ 抄 | 范围扩张要确认,收窄自动过 |
| **身份绑定 ID** | ✅ 抄 | 章节完成状态绑定 ID 不绑标题 |

## 面试问答弹药

- **Q**:计划为什么用数据不用 prose?→ A:prose 逼 host 猜结构,每个猜测都是静默失败的启发式——字段化的计划让下游零猜测
- **Q**:怎么防止 planner 伪造身份?→ A:ID/Revision 带 json:"-",host 分配——planner 无法提交自己没得到的身份
- **Q**:怎么区分"读过"和"推断"的文件?→ A:VerifiedFiles(实际读)vs CandidateFiles(推断)——猜测不能当事实
- **Q**:计划有环怎么办?→ A:Normalize 修复依赖(repairDependencies 去环),修复不了的 Validate 拒绝
- **Q**:用户审批的计划和执行列表不一致怎么办?→ A:Ordered 是单排序源——Render 和 ProjectTodos 都读它,永不漂移
- **Q**:审批时怎么看到改动?→ A:Diff 按 ID 配对 + 字段级变更(Added/Removed/Changed/Fields)——"标题重写"和"验收重写"风险不同
- **Q**:验收标准 ID 为什么必须渲染?→ A:证明必须能引用判据——执行器不能命名的判据是无法满足的判据
- **Q**:渲染的任务列表会被污染吗?→ A:continuation() 剥离列表标记 + 证据渲染为无标记续行——只有步骤是列表项
- **Q**:计划改了什么时候要重新审批?→ A:NeedsApproval——目标变/新步骤/列表字段增长(扩张)要审批;删步骤/改标题/重排(收窄)自动过
- **Q**:两个步骤标题相同怎么办?→ A:StepID 唯一绑定——完成状态归因 ID 不归因标题/索引
