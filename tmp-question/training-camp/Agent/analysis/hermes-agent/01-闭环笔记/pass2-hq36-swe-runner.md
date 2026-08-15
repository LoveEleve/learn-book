# hq36 SWE 评测 runner(Mini SWE Runner)— 产品③"评测"蓝本

> 项目:Hermes(mini_swe_runner.py 732 行 + tests/test_mini_swe_runner.py 等 2 用例 + trajectory 管线兼容)
> 假设:SWE 评测需跑真实任务并产 Hermes 轨迹格式——Hermes 的 MiniSWERunner 是"评测 runner"的样本(域发现 v7:local/docker/modal 环境;batch JSONL;与 trajectory_compressor 兼容;严格采样契约温度处理)。
> 结论:✅ 成立——三环境/轨迹格式兼容/批处理/温度契约/采样严格全具备,产品③"评测基建"直接蓝本。

---

## 一、架构全景:SWE 评测 + Hermes 轨迹格式

```
┌────────────────────────────────────────────────────────────┐
│ 环境(create_environment,117):local/docker/modal           │
├────────────────────────────────────────────────────────────┤
│ MiniSWERunner(157):单任务执行                             │
│   ——产出 Hermes-Agent 轨迹格式                            │
├────────────────────────────────────────────────────────────┤
│ 兼容管线:                                                 │
│   batch_runner.py(批处理)+ trajectory_compressor.py(压缩)  │
│   ——"outputs trajectories in the Hermes-Agent format       │
│     compatible with batch_runner.py and                    │
│     trajectory_compressor.py"                              │
├────────────────────────────────────────────────────────────┤
│ 温度契约:_effective_temperature_for_model(43)             │
│   ——"严格采样契约模型温度处理"(域发现 v7)                 │
├────────────────────────────────────────────────────────────┤
│ CLI:main(630):--task/--env local                          │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:三环境抽象

**位置**:`mini_swe_runner.py:117`(create_environment)

```
create_environment:local/docker/modal 三环境
  ——评测可在本地或隔离环境跑(与 hq15 沙箱同族)

CLI:--env local(环境选择)
```

**正确性价值**:环境抽象(本地/容器/云)——评测隔离可选。

**产品④映射**:评测环境抽象——本地/隔离可切换。

## 二、设计 2:轨迹格式兼容(管线接续)

**位置**:`mini_swe_runner.py:1-25`(模块头)

```
"outputs trajectories in the Hermes-Agent format compatible with
batch_runner.py and trajectory_compressor.py"
  ——评测输出可经批处理/压缩管线(batch JSONL + 压缩兼容)

严格采样契约:_effective_temperature_for_model(43)
  ——温度按模型契约处理(评测可复现)
```

**正确性价值**:轨迹格式统一——评测输出直接进批处理/压缩管线;温度契约(复现)。

**产品④映射**:评测输出管线兼容——统一轨迹格式 + 采样严格。

## 设计 3:CLI + 批处理

**位置**:`mini_swe_runner.py:630`(main)

```
python mini_swe_runner.py --task "..." --env local
  ——单任务;batch JSONL 提示词文件(域发现 v7)
```

**正确性价值**:CLI 面(单任务/批)——评测自动化入口。

**产品④映射**:评测 CLI——任务 + 批 + 环境。

---

## 三、与四项目对比(评测)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes mini_swe_runner |
|------|----|----------|----------|-----|------------------------|
| 评测 runner | conformance | e2ebench | 录制测试 | readtool A/B | **SWE 轨迹格式 runner** |
| 环境 | — | — | — | sandbox | **local/docker/modal** |
| 管线 | — | — | — | — | **批处理/压缩兼容** |
| 采样 | — | 温度 0 | — | — | **模型温度契约** |

**结论**:产品③"评测"参考 = Hermes mini_swe_runner(SWE 轨迹格式 + 三环境)+ dsh readtool(真实 A/B)+ Pi conformance。**Hermes 是任务级评测,readtool 是工具级评测——两层**。

---

## 四、面试弹药

1. **"轨迹格式统一"**:SWE 输出 Hermes 格式——兼容批处理/压缩管线(评测→训练数据链路)
2. **"严格采样契约"**:模型温度按契约处理——评测可复现
3. **"三环境"**:local/docker/modal——评测隔离可选
4. **"批 JSONL"**:批量提示词文件——评测自动化

---

## 五、产品映射汇总

| 设计 | 产品③用法 |
|------|---------|
| 三环境 | 评测隔离可选 |
| 轨迹格式兼容 | 评测→管线接续 |
| 温度契约 | 评测可复现 |
| CLI + 批 | 评测自动化 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_mini_swe_runner.py + test_minisweagent_path.py(2 用例,较小)
> 位置:MiniSWERunner :157 / create_environment :117 / _effective_temperature_for_model :43 / main :630
> 兼容:batch_runner.py/trajectory_compressor.py 管线;--env local/docker/modal
