# hq41-43 网关运行态小件(排空控制/富消息回显/渠道目录)— 产品②"部署与平台边缘"蓝本

> 项目:Hermes(gateway/drain_control.py 370 行 + gateway/rich_sent_store.py 83 行 + gateway/channel_directory.py 658 行)
> 假设:网关运行态小件——外部排空标记/富消息回显索引/渠道目录。三个小而聚焦的部署/平台边缘设计,产品②"部署面"样本。

---

## 一、hq41 排空控制(Drain Control)

**位置**:`drain_control.py:1-370`

```
外部排空标记契约(dashboard → gateway):
  safe-shutdown 计划 Task 2.2(decisions.md Q-B,option A):
  "Restart/drain is driven only by the gateway reacting to its own inputs:
  slash commands, process signals, and file markers it writes"
  ——无 HTTP 控制通道(刻意)——用 marker 文件 + epoch 防跨实例误判

dashboard begin/cancel-drain 端点 ↔ 运行中 gateway 经 marker 文件通信
```

**正确性价值**:无 HTTP 控制通道(攻击面小)——marker + epoch 防跨实例误判。

**产品④映射**:部署控制面——marker 文件契约(无 HTTP 通道)+ epoch 防误判。

> 测试契约:tests/gateway/test_drain*.py(关联)

## 二、hq42 富消息回显索引(Rich Sent Store)

**位置**:`rich_sent_store.py:1-83`

```
问题:Telegram 不回显 rich message 内容(回复 reply_to_message 时
  api_kwargs 为 None)——回复 launchd 简报/富发送时无法定位原文

修复:发送时记住 message_id → text,回复时按 message_id 查回显

本地索引(Bot API 10.1 sendRichMessage)
```

**正确性价值**:回显索引——平台不回显内容时本地记录(回复可定位)。

**产品④映射**:平台边缘适配——回显缺失时本地索引(与 hq30 平台差异内化同族)。

> 测试契约:tests/gateway/test_rich_sent_store*.py(关联)

## 三、hq43 渠道目录(Channel Directory)

**位置**:`channel_directory.py:1-658`

```
渠道目录:每平台可达渠道缓存(~/.hermes/channel_directory.json,5 分钟刷新)
send_message 工具读此文件:
  action="list"(列渠道)
  解析人类友好渠道名 → 数字 ID

——"send_message 名称解析"基础(域发现 v11)
```

**正确性价值**:渠道缓存(5 分钟刷新)——名称解析/列出可用渠道。

**产品④映射**:消息目标解析——渠道目录缓存(名称→ID)。

> 测试契约:tests/gateway/test_channel_directory*.py(关联)
> 位置:channel_directory.py:build_channel_directory(缓存构建,run.py 启动调用)

---

## 三、与四项目对比(部署/平台边缘)

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes 三件 |
|------|----|----------|----------|-----|-------------|
| 排空 | — | — | — | — | **marker + epoch(无 HTTP)** |
| 回显 | — | — | — | — | **本地索引(平台缺失)** |
| 渠道 | — | — | — | — | **缓存目录(名称解析)** |

**结论**:产品"部署/平台边缘"参考 = Hermes 三件(marker 契约/回显索引/渠道目录)。**均属域发现排除边缘(具体平台/部署细节),产品 MVP 参考即可**。

---

## 四、产品映射汇总

| 设计 | 产品②用法 |
|------|---------|
| 排空 marker 契约 | 部署控制(无 HTTP + epoch) |
| 富消息回显索引 | 平台缺失本地补 |
| 渠道目录缓存 | 目标名称解析 |

> 覆盖设计数:3(三文件各 1 设计)
> 位置:drain_control.py / rich_sent_store.py / channel_directory.py(共 1,111 行)
