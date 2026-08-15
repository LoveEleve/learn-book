# hq34 批量 runner 断点续跑(Batch Runner)— 产品②"批处理恢复"蓝本

> 项目:Hermes(batch_runner.py 1,330 行 + tests/test_batch_runner_checkpoint/durability + integration 16 用例)
> 假设:批量跑 agent 提示词需并行 + 检查点断点续跑——Hermes 的 BatchRunner 是"批处理恢复"的样本(域发现 v10:completed_prompts 索引 + 按内容匹配恢复)。
> 结论:✅ 成立——并行批处理/工具统计聚合/检查点续跑/内容匹配恢复/成功才标记全具备,产品②"批量任务"直接蓝本。

---

## 一、架构全景:并行批 + 检查点

```
┌────────────────────────────────────────────────────────────┐
│ 批处理(multiprocessing 并行):                              │
│   _process_batch_worker(400):批 worker                    │
│   BatchRunner(529):批量管理(checkpointing + 统计)         │
├────────────────────────────────────────────────────────────┤
│ 检查点续跑:                                               │
│   checkpoint.json(617)/_load_checkpoint(690)/              │
│   _save_checkpoint(原子写 + lock)                          │
│   ★ _scan_completed_prompts_by_content(734):索引对不上时   │
│     按提示词实际内容扫描恢复(失败条目跳过重试)             │
├────────────────────────────────────────────────────────────┤
│ 统计:                                                    │
│   _extract_tool_stats(125)/_normalize_tool_stats(71)/      │
│   _extract_reasoning_stats(208)——跨批工具/推理统计聚合    │
├────────────────────────────────────────────────────────────┤
│ 语义:                                                    │
│   仅成功保存才标记完成(508——失败条目 resume 重试)         │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:并行批处理(多进程)

**位置**:`batch_runner.py:400`(_process_batch_worker)+ `529`(BatchRunner)

```
_multiprocessing 并行批(数据集加载/分批)
_process_batch_worker:批 worker(批号/数据/输出目录/完成集/配置)

统计聚合:
  _extract_tool_stats(消息 → 工具统计)/_normalize(跨批归一)
  _extract_reasoning_stats(推理统计)
```

**正确性价值**:并行批 + 跨批统计聚合(工具/推理用量统一)。

**产品④映射**:批量任务并行化——多进程 + 统计聚合。

## 设计 2:检查点续跑(成功才标记)

**位置**:`batch_runner.py:617`(checkpoint_file)+ `508-525`(标记语义)+ `690-733`(加载/保存)

```
checkpoint.json(atomic_json_write + lock 保护)
_load_checkpoint/_save_checkpoint:检查点读写

★ 成功语义(508-514):
  "Only mark as completed if successfully saved (failed prompts can be
  retried on resume)"——仅成功保存标记;失败条目 "will retry on resume"
  (不把失败当完成)
```

**正确性价值**:成功才标记(失败可重试)——检查点不污染;原子写 + 锁。

**产品④映射**:批处理恢复——成功语义 + 原子检查点。

## 设计 3:按内容匹配恢复(索引失效兜底)

**位置**:`batch_runner.py:734-780`(_scan_completed_prompts_by_content)

```
★ 内容匹配恢复(域发现 v10 核心):
  "Scan all batch files and extract completed prompts by their actual
  content"——completed_prompts 索引对不上时,按提示词实际内容扫描
  ——索引漂移/损坏时仍能恢复(失败条目跳过重试)

用法:python batch_runner.py ... --resume
```

**正确性价值**:恢复双保险——索引优先 + 内容扫描兜底(索引损坏不丢进度)。

**产品④映射**:批处理恢复兜底——内容匹配扫描(索引失效仍可续)。

## 设计 4:CLI 入口

**位置**:`batch_runner.py:1156`(main)

```
python batch_runner.py --dataset_file=data.jsonl --batch_size=10
  --run_name=my_run [--resume] [--distribution=image_gen]
```

**正确性价值**:CLI 面完整(--resume/分布/数据集/批大小)。

**产品④映射**:批处理 CLI——数据集/批大小/续跑/分布。

---

## 三、与四项目对比(批处理)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes batch_runner |
|------|----|----------|----------|-----|---------------------|
| 并行批 | — | — | — | — | **多进程并行** |
| 检查点 | — | checkpoint | — | — | **原子检查点 + 成功标记** |
| 恢复 | — | 双阶段 | — | — | **索引 + 内容扫描双保险** |
| 统计 | — | — | — | — | **跨批工具/推理聚合** |

**结论**:产品"批量任务"参考 = Hermes batch_runner(并行批 + 检查点 + 内容恢复)。**与 trajectory 管线兼容(域发现 v7:S WE runner 共享轨迹格式)**。

---

## 四、面试弹药

1. **"成功才标记"**:仅成功保存标记完成——失败条目 resume 重试(不把失败当完成)
2. **"内容匹配恢复"**:索引对不上 → 按提示词实际内容扫描——索引损坏不丢进度
3. **"原子检查点"**:atomic_json_write + lock——检查点永不半写
4. **"跨批统计聚合"**:工具/推理用量统一——批间可比

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 并行批 | 多进程批处理 |
| 检查点 | 原子 + 成功标记 |
| 内容恢复 | 索引失效兜底 |
| CLI | 数据集/批/续跑/分布 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_batch_runner_checkpoint + test_batch_runner_durability + integration/test_batch_runner(16 用例)
> 位置:BatchRunner :529 / _process_batch_worker :400 / _load_checkpoint :690 / _scan_completed_prompts_by_content :734 / main :1156
> 用法:--dataset_file/--batch_size/--run_name/--resume/--distribution
