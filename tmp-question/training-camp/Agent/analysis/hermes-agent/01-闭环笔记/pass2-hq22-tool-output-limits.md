# hq22 工具输出上限(Tool Output Limits)— 产品②"输出防洪泛"蓝本

> 项目:Hermes(tools/tool_output_limits.py 110 行 + terminal_tool.py/file_operations.py 消费 + config.yaml tool_output 段)
> 假设:工具输出截断阈值必须可配置——Hermes 把散落两处的硬编码常量集中到单一 config 段,是"输出上限可配置化"的样本。
> 结论:✅ 成立——三阈值集中/防御式读取/进程缓存/默认值保持行为不变全具备(移植自 opencode PR #23770)。

---

## 一、架构全景:集中散落硬编码

```
移植:anomalyco/opencode PR #23770(feat(truncate): allow configuring tool
  output truncation limits)——OpenCode 硬编码 MAX_LINES=2000/MAX_BYTES=50K;
  Hermes 有两处硬编码:
    terminal_tool.py MAX_OUTPUT_CHARS=50000(终端 stdout/stderr 上限)
    file_operations.py MAX_LINES=2000/MAX_LINE_LENGTH=2000(read_file 分页/行长)

本模块集中到 tool_output config 段(默认值 = 原有硬编码 → 行为不变):
  max_bytes: 50000      # terminal 输出上限
  max_lines: 2000       # read_file 分页 + 截断上限
  max_line_length: 2000 # 行长上限(超 → "... [truncated]")
```

---

## 二、设计 1:三阈值集中(默认 = 原值)

**位置**:`tool_output_limits.py:39-41`(DEFAULT 常量)+ `82-88`(解析)

```
DEFAULT_MAX_BYTES = 50_000(terminal_tool.MAX_OUTPUT_CHARS)
DEFAULT_MAX_LINES = 2000(file_operations.MAX_LINES)
DEFAULT_MAX_LINE_LENGTH = 2000(file_operations.MAX_LINE_LENGTH)

消费点:
- terminal_tool.py:3371:MAX_OUTPUT_CHARS = get_max_bytes()
- file_operations.py:823:max_lines = get_max_lines()
- file_operations.py:1082/1461:max_line_length = get_max_line_length()
```

**正确性价值**:默认值 = 原有硬编码 → 未配置用户行为零变化(移植纪律)。

**产品④映射**:输出阈值的可配置化——集中单一 config 段,默认值保持行为不变。

## 设计 2:防御式读取(永不 raise)

**位置**:`tool_output_limits.py:48-56`(_coerce_positive_int)+ `59-89`(get_tool_output_limits)

```
_coerce_positive_int:非 int/<=0 → default(永不返回无效值)
get_tool_output_limits:
  - 配置缺失/非 dict → {}
  - 任何异常(缺文件/坏类型)→ section={}(回退默认)
  - "The limits reader is defensive: any error falls back to the built-in
    defaults so tools never fail because of a malformed config"
```

**正确性价值**:防御式读取——工具绝不因坏配置失败(与"可靠性层不打断主流程"同族)。

**产品④映射**:配置读取的防御式纪律——坏配置回退默认,工具永不因配置失败。

## 设计 3:进程级缓存

**位置**:`tool_output_limits.py:43-45`(_cached_limits)+ `70-72`(缓存检查)+ `92-95`(重置)

```
- 模块级缓存(首次调用填充)——避免每次工具调用重复 config 文件 I/O
- _reset_tool_output_limits_cache():测试/配置热重载后新读
```

**正确性价值**:进程缓存避免每工具调用磁盘 I/O;重置钩子供测试隔离。

**产品④映射**:配置读取缓存——热路径零 I/O + 显式重置钩子。

## 设计 4:快捷函数(调用点简单化)

**位置**:`tool_output_limits.py:98-110`(get_max_bytes/get_max_lines/get_max_line_length)

```
三快捷函数——terminal/file-ops 调用点只需单一上限,不拉整 dict
```

**正确性价值**:调用点最小化——单一上限需求用快捷函数。

---

## 三、与四项目对比(输出上限)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes tool_output_limits |
|------|----|----------|----------|-----|--------------------------|
| 截断阈值 | — | — | **MAX_LINES=2000/MAX_BYTES=50K 硬编码** | 工具结果修剪 | **三阈值集中可配置(移植 opencode #23770)** |
| 配置 | — | — | 硬编码 | 可配置 | **tool_output config 段(默认=原值)** |
| 防御 | — | — | — | — | **坏配置回退默认(永不 raise)** |
| 缓存 | — | — | — | — | **进程缓存 + 重置钩子** |

**结论**:产品"输出防洪泛"参考 = Hermes tool_output_limits(集中可配置)+ dsh 工具结果修剪(语义)+ hq13 三层防溢出(持久化)。**本模块是 L1 的工具内截断阈值集中化**。

---

## 四、面试弹药

1. **"移植纪律"**:opencode PR #23770 移植——默认值 = 原硬编码,未配置行为零变化
2. **"防御式读取"**:坏配置回退默认——工具绝不因 malformed config 失败
3. **"进程缓存"**:避免每工具调用磁盘 I/O;重置钩子供测试隔离
4. **"两处散落集中"**:terminal 50000 + file_operations 2000/2000 集中到 tool_output 段——单一真相源

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 三阈值集中 | 输出上限单一 config 段(默认=原值) |
| 防御式读取 | 坏配置回退默认(永不 raise) |
| 进程缓存 | 热路径零 I/O + 重置钩子 |
| 快捷函数 | 调用点最小化 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_tool_output_limits.py(10 用例:默认值/配置覆盖/坏类型回退/缓存/重置)+ 消费点回归(test_read_shell_line_clamp/test_terminal_output_transform_hook/test_terminal_truncation_spill)
> 位置:get_tool_output_limits :59 / 消费点 terminal_tool.py:3371 / file_operations.py:823,1082,1461
> 移植:opencode PR #23770;config 段:tool_output{max_bytes/max_lines/max_line_length}
