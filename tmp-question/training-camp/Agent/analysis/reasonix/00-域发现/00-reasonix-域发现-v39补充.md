# Reasonix 域发现 v39 补充(续扫第二十五轮:evidence Receipt 全集)— 2026-08-14

> 承接:v38。本轮:internal/evidence/(receipt.go + evidence.go 剩余)。
> 结论:**Receipt 17 字段全模型 + SerialTodo 确定性推进确认**——③验收器证据层的核心数据结构。

---

## 一、v39 深化确认(evidence)

### Receipt(17 字段)

| 字段 | 设计要点 |
|------|---------|
| ToolName/Args/Profile | 工具身份 |
| Success/Command/Step/StepProof | 成功/命令/步骤/步骤证明 |
| **Read/Write/Mutation** | 三类效果分类 |
| **OutputBytes** | **非零才算读**(head -n 0/</dev/null 永不计数)——"读了"的可证伪定义 |
| **OutputDigest** | **有界身份**:区分真变化 vs 精确重复,不保留内容——**变化的指纹化** |
| **ExitCode 指针** | 失败测试运行与工具干净报告可区分(0 ≠ 未设置)——**工具报告 ≠ 真相** |
| **Verification** | 主机分类(Verification* 值) |

### SerialTodo(确定性推进)

| 设计 | 要点 |
|------|------|
| **AdvanceSerialTodo** | 仅 in_progress 可推进;子步骤未完不推进;**Level 1 完成 → 下一个 pending 晋升 in_progress**;孤儿子步骤回退为普通步;列表保持一个 current |
| **validateSerialTodos** | 串行 todo 合法性验证 |
| **收据查询族** | HasWriteOrCommandSince/HasSuccessfulCommand/HasCompletedReview/HasFailedCommand/TouchedPaths |

---

## 二、关键设计(通用价值)

1. **"OutputBytes 非零才算读"**:可证伪的读定义——**证据的严格判定**
2. **"OutputDigest 指纹不保留内容"**:变化检测不存内容——**隐私与正确性兼得**
3. **"ExitCode 区分工具报告与真相"**:工具说成功 ≠ 测试通过——**证据的独立性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v38 | — | 102 | 102 |
| v39 | evidence Receipt 全集 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 goaleval/plancontract 源码验证(rq2/rq3 覆盖后)、cli 剩余——按需收尾。
