# hq33 浏览器监督器(Browser Supervisor)— 产品②"浏览器监督"蓝本

> 项目:Hermes(tools/browser_supervisor.py 1,518 行 + browser_cdp_tool.py + tests 4 文件 36 用例)
> 假设:浏览器对话框/框架检测需持久监督——Hermes 用每 task_id 一个 CDP 监督器,输出经双通道(browser_snapshot 合并/browser_dialog 响应)到达 agent,是"外部设备监督"的样本(域发现 v8:不在工具 schema 中)。
> 结论:✅ 成立——每 task 监督器/快照合并/对话框响应/运行时评估/脱敏/注册表全具备,产品②"浏览器监督"直接蓝本。

---

## 一、架构全景:不在 schema 的监督器

```
┌────────────────────────────────────────────────────────────┐
│ CDPSupervisor(289):每 Hermes task_id 一个(可达 CDP 时)    │
│   ——持久监督:对话框 + 框架树状态                           │
├────────────────────────────────────────────────────────────┤
│ 输出双通道(不在工具 schema):                              │
│   1. browser_snapshot 合并监督器状态进返回载荷              │
│   2. browser_dialog 工具响应对话框(respond_to_dialog)      │
├────────────────────────────────────────────────────────────┤
│ 状态:                                                    │
│   PendingDialog(161)/DialogRecord(188)/FrameInfo(217)/     │
│   ConsoleEvent(250)/SupervisorSnapshot(260)                │
├────────────────────────────────────────────────────────────┤
│ 生命周期:start(353)/stop(395)/snapshot(427)/               │
│   respond_to_dialog(445)/evaluate_runtime(507)             │
│ 注册表:_SupervisorRegistry(1425,get/get_or_start/stop/     │
│   stop_all)                                                │
│ 脱敏:_redact_cdp_error_text/_redact_supervisor_text(41/60) │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:每 task 监督器(持久 CDP)

**位置**:`browser_supervisor.py:289-353`(CDPSupervisor/start)

```
一个 CDPSupervisor 每 Hermes task_id(有可达 CDP 时)
——持久监督:对话框 + 框架树可观察状态

start(timeout=15)/stop(timeout=5):生命周期
snapshot():状态快照(对话框/框架)
```

**正确性价值**:每 task 持久监督——浏览器状态跨工具调用可观察。

**产品④映射**:外部设备监督——每任务持久观察器(浏览器/设备)。

## 设计 2:输出双通道(不在 schema)

**位置**:`browser_supervisor.py:1-15`(模块头)+ `445`(respond_to_dialog)

```
监督器不在 agent 工具 schema——输出经双通道:
1. browser_snapshot 合并监督器状态进返回载荷
2. browser_dialog 工具响应挂起对话框(respond_to_dialog)

设计规格:website/docs/developer-guide/browser-supervisor.md
```

**正确性价值**:不在 schema(零 schema 占用)——状态经现有工具合并,响应经专用工具。

**产品④映射**:监督器接入模式——不在工具面,snapshot 合并 + 响应专用工具。

## 设计 3:状态模型(结构化)

**位置**:`browser_supervisor.py:161-288`

```
PendingDialog(161):挂起对话框
DialogRecord(188):已记录对话框
FrameInfo(217):框架树信息
ConsoleEvent(250):控制台事件
SupervisorSnapshot(260):完整快照(对话框+框架)

——监督器状态全结构化(agent 可读)
```

**正确性价值**:状态结构化——agent 读快照理解浏览器状态。

**产品④映射**:监督器状态模型——结构化快照(可被 agent 消费)。

## 设计 4:注册表 + 脱敏

**位置**:`browser_supervisor.py:1425`(_SupervisorRegistry)+ `41-60`(脱敏)

```
_SupervisorRegistry:get(1425)/get_or_start(1441)/stop(1485)/stop_all(1492)
  ——每 task 监督器生命周期管理

脱敏:_redact_cdp_error_text/_redact_supervisor_text
  ——CDP 错误/监督器文本脱敏(错误信息不进明文)
```

**正确性价值**:注册表统一管理(每 task 生命周期);脱敏(错误/文本安全)。

**产品④映射**:监督器注册表 + 脱敏——统一生命周期 + 输出安全。

---

## 三、与四项目对比(外部监督)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes browser_supervisor |
|------|----|----------|----------|-----|---------------------------|
| 外部监督 | — | — | — | — | **CDP 持久监督(每 task)** |
| 接入 | — | — | — | — | **不在 schema(snapshot 合并)** |
| 状态 | — | — | — | — | **对话框/框架/控制台结构化** |
| 响应 | — | — | — | — | **专用工具响应对话框** |
| 注册表 | — | — | — | — | **每 task 生命周期管理** |

**结论**:产品"外部设备监督"参考 = Hermes browser_supervisor(持久监督 + 双通道接入 + 结构化状态)。**与 hq25 工具搜索桥同理:监督器不在 schema,经现有工具合并状态**。

---

## 四、面试弹药

1. **"不在工具 schema"**:监督器零 schema 占用——状态经 browser_snapshot 合并,响应经 browser_dialog
2. **"每 task 持久监督"**:每 task_id 一个 CDPSupervisor——浏览器状态跨工具调用可观察
3. **"对话框响应"**:respond_to_dialog——挂起对话框可响应(非只读监督)
4. **"结构化快照"**:对话框/框架/控制台事件——agent 读快照理解状态
5. **"脱敏"**:CDP 错误文本脱敏——错误信息不进明文

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 每 task 监督器 | 持久观察(跨调用) |
| 双通道接入 | 不在 schema(snapshot 合并 + 响应工具) |
| 结构化状态 | 对话框/框架/控制台 |
| 注册表 + 脱敏 | 生命周期统一 + 输出安全 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_browser_supervisor.py(7)+ test_browser_supervisor_healthcheck.py(2)+ test_browser_cdp_override.py(15)+ test_browser_cdp_tool.py(12)= 36 用例
> 位置:CDPSupervisor :289 / start :353 / respond_to_dialog :445 / _SupervisorRegistry :1425 / 双通道(browser_snapshot/browser_dialog)
> 规格:website/docs/developer-guide/browser-supervisor.md
