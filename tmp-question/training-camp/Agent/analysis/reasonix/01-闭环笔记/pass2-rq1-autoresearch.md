# 闭环笔记 RQ1:AutoResearch TaskSpec — 规格书三合一蓝本

> 域:internal/autoresearch/(task.go 125 行 + schema.go 52 行 + summary.go 95 行 + store.go 659 行)
> 日期:2026-08-14
> 假设 → 验证 → 结论,全部带 file:line

---

## 假设

AutoResearch 虽然只是"历史归档的只读读取器",但其 TaskSpec 数据结构 + Summary 验收算法是产品规格书/验收器的完整蓝本。

## 验证过程

### 1. 任务规格书 TaskSpec(task.go:33-40)— 产品①规格书的数据模型

```go
type TaskSpec struct {
  TaskID            string
  Goal              string
  Scope             []string
  NonGoals          []string
  AllowedOperations AllowedOperations   // write/network/publish
  SuccessCriteria   []SuccessCriterion
}
```

**映射到产品 6 维规格书**:
| TaskSpec 字段 | 产品维度 |
|--------------|---------|
| Goal | 目标 |
| Scope | 范围 |
| **NonGoals** | **边界排除(非目标)** |
| **AllowedOperations** | **权限边界** |
| **SuccessCriteria** | **验收判据** |

### 2. 成功标准 = 验收判据 + 证据链(task.go:26-31)— 产品③核心

```go
type SuccessCriterion struct {
  ID          string
  Description string
  Required    bool     // 必选/可选
  EvidenceIDs []string // 证据引用!
}
```

**每个判据自带 evidence_ids**——验收 = 检查证据是否满足判据。**这是产品③验收器的直接数据模型。**

### 3. 验收算法(summary.go:31-54)— 产品③的核心逻辑

```go
for _, criterion := range task.Spec.SuccessCriteria {
    count := countAcceptedEvidence(criterion.EvidenceIDs, accepted)  // 证据计数
    status := "satisfied"
    if criterion.Required && count == 0 {
        status = "open"    // 必选判据无证据 → 未满足
    }
    if status == "open" { openCriteria = append(...) }
}
```

**验收 = 证据匹配 + 必选判据检查**:
- evidence_ids → 在 accepted findings 中计数
- required 判据计数为 0 → open(未满足)
- 产出 open_criteria 列表(未满足的判据)

### 4. 下一步行动决策(summary.go:74-82)— 交接/恢复自动化

```go
func nextRequiredAction(progress Progress) string {
  if Status == Blocked:        return "resolve blocker before continuing"
  if StaleCount >= 4:          return "ask for the smallest external input needed"
  if StaleCount >= 2:          return "make a structural pivot before continuing"
  return "continue with the next evidence-producing step"
}
```

**停滞/阻塞的自动决策链**:
- blocked → 先解阻塞
- stale ≥ 4 → **问人**(最小外部输入)——agent 卡死自动请求人类
- stale ≥ 2 → **结构性转向**(pivot)——方向错误自动换方向
- 否则 → 继续

### 5. 停滞检测指标(Progress, task.go:42-50)— #24 收敛性的量化答案

```go
type Progress struct {
  Status           string  // running/blocked/complete/stopped/invalid
  Iteration        int
  CurrentDirection string
  StaleCount       int     // 停滞计数:同一方向重复尝试
  PivotCount       int     // 转向计数:方向切换次数
  BlockedReason    string
}
```

**stale_count/pivot_count = "agent 死循环"的量化检测**:
- stale_count 高 = 在重复尝试同一方向(没有进展)
- pivot_count 高 = 频繁换方向(可能乱窜)
- **PivotRequired = StaleCount >= 2**(summary.go:66)——停滞 2 次强制转向

### 6. Finding = 闭环笔记结构(task.go:59-68)

```go
type Finding struct {
  ID        string
  Kind      string   // opaque,历史值 round-trip
  Summary   string
  Source    string   // command/file/manual(溯源!)
  Command   string   // 来源命令
  Paths     []string // 相关文件
  Accepted  bool     // 是否被采纳
  CreatedAt time.Time
}
```

**映射到闭环笔记**:
- Summary = 结论
- Source(command/file/manual) = 证据溯源
- Paths = 源码位置
- Accepted = 验收状态

### 7. 校验(schema.go)— 规格书合法性检查

```
validateTaskSpec:task_id 必填/必须匹配目录/goal 必填/criterion id 必填且唯一/description 必填
validateProgress:status 枚举合法/iteration/stale/pivot 非负/updated_at 必填
```

**规格书有硬校验**——非法规格书在写入时就被拒绝。

### 8. 状态枚举(task.go:5-11)

```
running / blocked / complete / stopped / invalid
```
五种任务状态——"invalid" 是显式状态(规格书校验失败)。

### 9. 只读定位说明(store.go:11-14)

> "Package autoresearch is a read-only compatibility reader for historical archives. New Goal runs never create or mutate these directories."

**重要**:当前 Goal 系统不写这些目录了(新系统在 control/goal_*),但**数据结构本身是历史实践的沉淀**——数据模型的价值不因读写位置改变。

### 10. ResumeFromGoalText 中断恢复(store.go:107-117)— review 新增

```go
func (s *Store) ResumeFromGoalText(goal string) (*Task, bool, error) {
    taskID, found, err := ExplicitTaskID(goal)  // 从 goal 文本提取任务路径
    ...
    task, err := s.LoadTask(taskID)             // 加载任务
}
```

**产品④"从目标文本恢复任务"的直接机制**:
- 用户说"继续分析 JVM"(goal 含任务路径)→ 提取 → 加载 → 恢复
- **这就是 #21 交接自动化的代码级答案**:恢复 = 从文本定位任务,不是人写交接文档

**ExplicitTaskID 的安全解析**(store.go:122-141):
- 从 goal 提取 `.reasonix/autoresearch/<task-id>/` 路径
- 多路径组件/非法 id → 报错(不是普通文本)
- validateTaskID:防 `..` 路径穿越 + `/\` 拒绝(store.go:455)

### 11. tailJSONLLines 增量读取(store.go:514-519)— review 新增

```
JSONL 反向分块读尾部:limit>0 只读最后 N 行(不重扫 append-only 日志)
limit<=0 全扫(accepted-evidence 需要全量)
```

**性能设计**:per-turn 读取不重扫整个日志——append-only 长任务日志的增量读取。产品知识库"读最近 N 条"直接抄。

### 12. 安全读取模型(os.Root)— review 新增

- os.Root 约束(Go 1.24 的 chroot 式路径隔离):所有读取限制在归档根内
- fileencoding.DecodeToUTF8:历史文件编码转换
- 扫描器缓冲 4MB:长 JSONL 行不截断(store.go:498)

### 13. 新旧系统桥接 = epoch 保护的迁移(goal_legacy.go:183 行)— review 第三轮新增

**验证**:当前 Goal 系统保留 `AutoResearchTaskID`(goal.go:127)作为 legacy 兼容——历史任务可在新系统继续,数据结构价值确认。

**迁移的严谨设计**(goal_legacy.go):
```
epoch 单调递增保护:所有迁移操作检查 continuationEpoch
  → 防止"过期恢复覆盖新状态"(与 Fencing Token 同哲学)

fail-closed 原则:迁移失败 → Goal 保持 blocked
  → 直到下一次 resume 提交持久化迁移

clearLegacyTaskID:新状态持久化后才清除旧身份
  → 两阶段提交的持久化顺序(先新后旧)

resumeLegacyArchive:恢复时重置 budgetClass/turnsLimit
  → 恢复即全新运行(不继承旧预算)
```

**产品映射**:产品"知识库版本迁移"同样需要 epoch 保护——防止旧版本数据覆盖新版本。这是"规格书/知识库升级"的正确性保障。

### 14. 测试即行为契约(store_test.go)— review 第四轮新增

测试揭示了 3 个关键行为契约:

**契约 1:验收严格性**(TestSummaryReportsMissingCriteria,store_test.go:225-240)
- 无证据时必选判据全部报 open(2/2 未满足)——**验收不宽容**

**契约 2:路径安全 fail-closed**(TestResumeFromGoalText,store_test.go:242-271)
- `../escape` / `/extra` / `\extra` / 空路径 → **全部拒绝**
- 不匹配的任务路径 → 普通文本(不报错)
- **路径逃逸测试集 = 安全边界的完整定义**

**契约 3:只读保证**(TestListSummariesAndSummaryAreReadOnly,store_test.go:273-299)
- 读取操作后 hashTree + modTimes 对比 → **读取器绝不写文件**

**测试即契约文档**:这些测试定义了读取器的行为边界,比注释更权威。产品验收器/知识库的测试应同样"定义边界"。

## 代码类型

Data Model + Algorithm(验收算法)

## 跨域关联

- → 对照:control/goal_*(当前 Goal 系统,新实现)
- ← 依赖:无(纯数据读取)

## 结论

核心可抄设计 13 个:
1. **TaskSpec 数据模型**(goal/scope/non_goals/allowed_operations/success_criteria)→ 产品①规格书
2. **SuccessCriterion + EvidenceIDs** → 验收判据 + 证据链
3. **验收算法**(证据计数 + 必选判据检查)→ 产品③核心逻辑
4. **nextRequiredAction 决策链**(blocked/stale≥4问人/stale≥2转向)→ 自动恢复
5. **stale_count/pivot_count** → #24 死循环量化检测
6. **Finding 结构**(source 溯源 + accepted)→ 闭环笔记
7. **规格书硬校验**(schema.go)→ 非法规格书拒绝
8. **五状态枚举**(含 invalid)→ 任务状态机
9. **ResumeFromGoalText**(goal 文本 → 任务恢复)→ #21 交接自动化代码答案
10. **tailJSONLLines 增量读取** → append-only 日志不重扫
11. **os.Root 安全模型** → 归档读取路径隔离
12. **epoch 保护迁移**(新旧系统桥接)→ 知识库版本迁移正确性
13. **测试即行为契约**(fail-closed 边界/只读保证/验收严格性)→ 测试定义边界

## 产品映射

| 设计 | 抄/改/弃 | 怎么用 |
|------|---------|--------|
| TaskSpec 数据模型 | ✅ 抄 | 产品规格书 schema(goal/non_goals/allowed_operations/success_criteria) |
| 验收算法 | ✅ 抄 | 验收器:evidence_ids 计数 + required 检查 |
| nextRequiredAction | ✅ 抄 | 分析卡死自动决策(问人/转向/继续) |
| stale/pivot 计数 | ✅ 抄 | 死循环检测(#24) |
| Finding 结构 | ✅ 抄 | 闭环笔记数据模型 |
| 硬校验 | ✅ 抄 | 规格书合法性检查 |
| ResumeFromGoalText | ✅ 抄 | "继续分析 JVM"自动恢复任务 |
| 增量读取 | ✅ 抄 | 知识库日志尾部读取 |
| os.Root 隔离 | ✅ 抄 | 归档读取安全 |
| epoch 保护迁移 | ✅ 抄 | 知识库 schema 升级防旧覆盖新 |
| 测试即契约 | ✅ 抄 | 验收器/知识库的边界测试 |

## 面试问答弹药

- **Q**:怎么定义"任务完成"?→ A:SuccessCriteria 数组,每个判据 required + evidence_ids——验收 = 必选判据的证据计数非零
- **Q**:agent 卡死怎么检测?→ A:stale_count(停滞)>= 2 强制转向,>= 4 问人类——量化阈值
- **Q**:怎么防 agent 乱换方向?→ A:pivot_count 计数 + PivotRequired 逻辑——停滞才转向,不是随便转
- **Q**:规格书怎么保证合法?→ A:schema.go 硬校验——task_id 唯一/goal 必填/判据 id 唯一
- **Q**:自动恢复决策?→ A:nextRequiredAction——blocked 解阻塞/stale≥4 问人/stale≥2 转向/否则继续
- **Q**:中断怎么恢复?→ A:ResumeFromGoalText——goal 文本提取任务路径 → 加载任务 → 继续
- **Q**:长日志怎么读?→ A:tailJSONLLines 反向分块读尾部,不重扫 append-only 日志
- **Q**:系统升级数据格式怎么迁移?→ A:epoch 保护——迁移操作检查 continuationEpoch,防旧状态覆盖新状态;先写新后清旧
- **Q**:怎么保证读取器不写文件?→ A:只读契约测试——hashTree + modTimes 对比,读取后文件树不变
