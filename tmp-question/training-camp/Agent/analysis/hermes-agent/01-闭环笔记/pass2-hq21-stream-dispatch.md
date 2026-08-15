# hq21 流式事件分发(Stream Dispatch)— 产品②"执行事件路由"蓝本

> 项目:Hermes(gateway/stream_dispatch.py 132 行 + gateway/stream_events.py 事件族 + gateway/platforms/base.py:3310 渲染钩子)
> 假设:agent 发 typed 事件、adapter 决定如何投递——Hermes 用薄同步路由器把"事件产生"与"平台渲染"解耦,是"执行事件分发"的样本。
> 结论:✅ 成立——typed 事件族/薄路由器无平台知识/adapter 决定吃或渲染/渲染钩子默认兼容全具备;⚠ 接线状态诚实标注(见下)。

---

## 一、架构全景:接缝设计(agent 发事件,adapter 决定投递)

```
┌────────────────────────────────────────────────────────────┐
│ 事件产生(agent 侧):typed 事件族(stream_events.py)          │
│   MessageChunk/MessageStop/Commentary(文本流)               │
│   ToolCallChunk/ToolCallFinished(工具进度)                  │
│   LongToolHint/GatewayNotice(长工具提示/网关通知)           │
├────────────────────────────────────────────────────────────┤
│ 路由(GatewayEventDispatcher 薄同步路由器):                 │
│   无平台知识 + 无 asyncio——agent worker 线程可调           │
│   持 adapter + sink(流消费者)+ 渠道级呈现设置              │
│   每个事件经 adapter 渲染钩子 → sink/进度队列              │
├────────────────────────────────────────────────────────────┤
│ 投递(adapter 决定):                                        │
│   render_message_event:映射到流消费者原语                   │
│     (Telegram DM 原生草稿/他处编辑就地)                    │
│   format_tool_event:可返回 None = 吃事件(不能渲染工具 chrome│
│     的平台);渲染行入同一进度队列(不再双独立路径竞争)       │
└────────────────────────────────────────────────────────────┘
```

**⚠ 接线状态(诚实标注,2026-08-15 深度 review 发现)**:`GatewayEventDispatcher` 定义存在 + adapter 渲染钩子(render_message_event/format_tool_event)存在 + 测试存在,但 **dispatcher 全仓无生产实例化点**(grep 仅定义 + 测试)——"Tobi 要的接缝"已建、渲染钩子已定义,但生产路径尚未经 dispatcher 接线(或已由其他路径直接消费钩子)。产品抄此模式时注意:接缝设计是目标,接线需自行完成。

---

## 二、设计 1:typed 事件族(结构化而非字符串)

**位置**:`stream_events.py:44-140`(MessageChunk/MessageStop/Commentary/ToolCallChunk/ToolCallFinished/LongToolHint/GatewayNotice)

```
事件族(6+1):
- 文本流:MessageChunk(delta)/MessageStop(final 标志)/Commentary(旁白)
- 工具进度:ToolCallChunk(工具名)/ToolCallFinished(完成)
- 提示/通知:LongToolHint(长工具)/GatewayNotice(网关通知)

语义:typed 事件解耦"发生了什么"与"怎么显示"——事件产生者不知道
  平台渲染细节(与域发现 v32"typed 事件流"验证一致)
```

**产品④映射**:执行事件结构化为 typed 事件——"发生了什么"与"怎么显示"解耦(与 OpenCode 事件族同哲学)。

## 设计 2:薄路由器(无平台知识 + 无 asyncio)

**位置**:`stream_dispatch.py:40-94`(GatewayEventDispatcher)

```
dispatch(event):路由单事件,永不 raise 进 agent worker 线程
  (presentation 不能破坏 agent 循环——try/except 兜底)

路由分派:
- MessageChunk/MessageStop/Commentary → sink 非 None → adapter.render_message_event
  (sink None = 流禁用 → 丢弃,最终响应仍走正常发送路径)
- ToolCallChunk → tool_mode != off + enqueue 非 None → adapter.format_tool_event
  (adapter 返回 None = 吃事件)→ enqueue_tool_line(同进度队列)
- ToolCallFinished → 默认无 chrome(与今天一致——网关只渲染 started)
- LongToolHint → on_long_tool 钩子(网关拥有"该不该在这里显示"决策)
- GatewayNotice → on_notice 钩子
```

**正确性价值**:
1. 薄 + 同步——worker 线程可调,不引入 asyncio 竞态
2. 永不 raise——呈现失败不破坏 agent 循环
3. adapter 可吃事件(无法渲染工具 chrome 的平台)——渲染能力适配

**产品④映射**:执行事件路由——薄路由器 + 永不 raise + adapter 可吃(平台能力适配)。

## 设计 3:渲染钩子默认兼容(adapter 决定)

**位置**:`base.py:3310-3360`(render_message_event/format_tool_event)

```
render_message_event 默认(映射到流消费者原语,1:1 保留今天行为):
  MessageChunk → sink.on_delta(text)
  MessageStop → 非 final → sink.on_segment_break()
    (中间 stop = 段断;终 stop 由网关 finish() 信号,不经此)
  Commentary → sink.on_commentary(text)

format_tool_event(adapter 覆盖):返回 None = 吃事件

默认重现今天行为;adapter 覆盖原生渲染(Telegram 草稿/编辑就地)
```

**正确性价值**:默认实现 = 行为不变;adapter 覆盖 = 平台原生——"扩展不破坏默认"。

**产品④映射**:平台渲染钩子——默认兼容 + adapter 覆盖;渲染与事件产生分离。

## 设计 4:new 模式去重(工具变化才报)

**位置**:`stream_dispatch.py:85-107`(new 模式)

```
tool_mode("all"/"new"/"verbose"/"off"):
- "new" 模式:仅工具变化时报告(_last_tool 记录,同工具跳过)
  ——不重复刷同一工具的进度行
- "off":工具行全不渲染
- preview_max_len(0 = verbose 模式无上限)
```

**正确性价值**:工具进度去重——"new" 模式只在工具切换时报告,防同工具重复轰炸。

**产品④映射**:进度渲染的模式化(全部/新/详细/关)+ 去重。

---

## 三、与四项目对比(执行事件)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes stream_dispatch |
|------|----|----------|----------|-----|------------------------|
| 事件化 | 事件发布 | event Kind 全集 | LLMEvent→SessionEvent 12 映射 | typed 事件 | **typed 事件族(6+1)** |
| 路由 | — | — | 投影器 | Surface 投影 | **薄同步路由器(adapter 决定)** |
| 渲染 | — | — | — | 工具 UI 渲染意图 | **adapter 钩子(可吃事件)** |
| 默认兼容 | — | — | — | — | **默认 = 今天行为,覆盖 = 原生** |
| 模式 | — | — | — | — | **all/new/verbose/off + 去重** |

**结论**:产品"执行事件分发"参考 = Hermes stream_dispatch(薄路由器 + adapter 决定 + 可吃事件)+ OpenCode 事件族(结构化)+ dsh Surface(模型/人类视图分离)。**⚠ 抄接缝模式时须自行完成生产接线(本组件当前无实例化点)**。

---

## 四、面试弹药

1. **"Tobi 要的接缝"**:agent 发 typed 事件,adapter 决定投递——事件产生与平台渲染解耦
2. **"adapter 可吃事件"**:无法渲染工具 chrome 的平台 → format_tool_event 返回 None——渲染能力适配,不强行渲染
3. **"永不 raise 进 worker 线程"**:presentation 失败不能破坏 agent 循环(try/except 兜底)
4. **"new 模式去重"**:仅工具变化时报告进度——同工具不重复轰炸
5. **"默认 = 今天行为"**:渲染钩子默认 1:1 保留,adapter 覆盖原生渲染——扩展不破坏默认

---

## 五、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| typed 事件族 | "发生了什么"与"怎么显示"解耦 |
| 薄路由器 | 无平台知识 + 无 asyncio + 永不 raise |
| 渲染钩子 | 默认兼容 + adapter 覆盖(可吃事件) |
| new 模式去重 | 工具进度不重复轰炸 |
| 接线状态 | ⚠ 接缝已建,生产接线需自行完成 |

> 覆盖设计数:4(设计 1-4)
> 测试契约:test_stream_events.py(5 用例:delta 流到 sink/中间 stop 段断/旁白流/默认 chrome/新模式去重)
> 位置:dispatcher :40 / 事件族 stream_events.py:44-140 / 渲染钩子 base.py:3310/3331
> ⚠ 接线状态:dispatcher 无生产实例化点(2026-08-15 深度 review 实证)
