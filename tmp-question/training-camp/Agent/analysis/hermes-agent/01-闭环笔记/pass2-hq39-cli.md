# hq39 CLI 编排(CLI Orchestration)— 产品①"交互编排"蓝本

> 项目:Hermes(cli.py 19,269 行 + tests/cli/ 877 用例)
> 假设:交互 CLI 是 agent 主入口之一——Hermes 的 HermesCLI mixin 架构 + 斜杠命令分发 + busy_input_mode 是"交互编排"的样本(域发现 v10:Mixin 架构/process_command 斜杠分发/busy_input_mode 三模式)。
> 结论:✅ 成立——mixin 架构/斜杠注册表分发/三输入模式/配置加载/显示族全具备,产品①"交互入口"直接蓝本。

---

## 一、架构全景:CLI 编排

```
┌────────────────────────────────────────────────────────────┤
│ HermesCLI(4324):CLIAgentSetupMixin + CLICommandsMixin +    │
│   CLIBillingMixin(mixin 架构,god-file 分解运动)            │
├────────────────────────────────────────────────────────────┤
│ 斜杠命令分发:                                             │
│   process_command(10464):斜杠分发(经 commands.py 注册表    │
│     resolve_command 规范化 → 分发)                         │
│   ChatConsole(3993):交互控制台                            │
├────────────────────────────────────────────────────────────┤
│ 输入模式:busy_input_mode(interrupt/queue/steer 三模式)     │
│   ——agent 忙时用户输入行为                                │
├────────────────────────────────────────────────────────────┤
│ 配置:load_cli_config(409):硬编码默认 + 用户 YAML 合并      │
│ 显示族:show_banner/show_help/show_tools/show_history       │
│ 运行:run(15614):主循环                                    │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:Mixin 架构(god-file 分解)

**位置**:`cli.py:4324`(HermesCLI)

```
class HermesCLI(CLIAgentSetupMixin, CLICommandsMixin, CLIBillingMixin):
  ——mixin 架构:agent 设置/命令/计费分拆(域发现 v10:god-file 分解运动)
  ——与 hq5 turn_finalizer 同哲学(大文件拆 seam)

AIAgent(890)/get_tool_definitions(896)等延迟代理(模块级转发)
```

**正确性价值**:god-file 分解为 mixin——可维护性(与 turn_finalizer seam 同族)。

**产品④映射**:交互编排模块化——mixin 分拆(大文件 seam)。

## 设计 2:斜杠命令分发(注册表驱动)

**位置**:`cli.py:10464`(process_command)+ `hermes_cli/commands.py`(COMMAND_REGISTRY)

```
process_command(10464):斜杠命令分发
  ——经 commands.py COMMAND_REGISTRY(resolve_command 规范化别名→分发)
  ——注册表单源(CLI/gateway/帮助/自动补全全从它派生)

ChatConsole(3993):交互控制台(输入/回显)
```

**正确性价值**:注册表单源——别名解析/分发/帮助/补全一致(域发现 v9 单一真相源)。

**产品④映射**:交互命令分发——注册表驱动(单源)。

## 设计 3:busy_input_mode 三模式

**位置**:`cli.py:4404-4409`(busy_input_mode)+ 三模式实现

```
busy_input_mode(display 配置,默认 interrupt):
  interrupt:Enter 重定向当前运行(打断)
  queue:排队(agent 忙时输入入队)
  steer:纠偏(运行中 steering,与 /steer 衔接)

测试:test_busy_input_mode_command.py
```

**正确性价值**:忙时输入三语义(打断/排队/纠偏)——用户控制面。

**产品④映射**:交互忙时策略——打断/排队/纠偏(与 hq5 steer 衔接)。

## 设计 4:配置加载 + 显示族

**位置**:`cli.py:409`(load_cli_config)+ 7489-8568(显示族)

```
load_cli_config:硬编码默认 + 用户 YAML 合并(CLI 特有)
显示族:show_banner/show_help/show_tools/show_toolsets/show_config/
  show_history(7489-8568)
```

**正确性价值**:配置分层(默认+用户)+ 显示族完整(可发现性)。

**产品④映射**:交互入口可发现性——help/tools/toolsets/config/history 显示。

---

## 三、与四项目对比(交互编排)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes CLI |
|------|----|----------|----------|-----|------------|
| 交互入口 | TUI | chat_tui | TUI | — | **HermesCLI mixin** |
| 命令分发 | — | — | 斜杠注册表 | — | **COMMAND_REGISTRY 单源** |
| 忙时输入 | — | — | — | — | **interrupt/queue/steer** |
| 显示 | — | — | — | — | **help/tools/history 族** |

**结论**:产品①"交互入口"参考 = Hermes CLI(mixin + 注册表分发 + 三输入模式)。**产品 CLI 优先(域发现弃用 TUI/桌面具体实现,保留 CLI 交互面)**。

---

## 四、面试弹药

1. **"mixin 分解 god-file"**:HermesCLI = 三个 mixin(agent 设置/命令/计费)——与 turn_finalizer seam 同哲学
2. **"注册表单源"**:COMMAND_REGISTRY——CLI/gateway/帮助/补全全派生
3. **"busy_input_mode 三模式"**:interrupt/queue/steer——忙时输入用户控制
4. **"配置分层"**:硬编码默认 + 用户 YAML——CLI 特有配置
5. **"显示族可发现"**:help/tools/config/history——交互可发现性

---

## 五、产品映射汇总

| 设计 | 产品①用法 |
|------|---------|
| Mixin 架构 | 交互编排模块化 |
| 注册表分发 | 命令单源 |
| 三输入模式 | 忙时用户控制 |
| 配置分层 | 默认+用户 |
| 显示族 | 可发现性 |

> 覆盖设计数:5(设计 1-5)
> 测试契约:tests/cli/ 877 用例(busy_input_mode/bang_shell/bracketed_paste/branch/chat_q_exit 等 65 文件)
> 位置:HermesCLI :4324 / process_command :10464 / ChatConsole :3993 / load_cli_config :409 / run :15614 / busy_input_mode :4404
> 关联:commands.py COMMAND_REGISTRY 单源 / hq5 turn_finalizer seam 同哲学
