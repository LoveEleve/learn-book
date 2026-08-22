# Kafka-42 重写规划

> 题目：CheckPoint 机制——recovery-point-offset-checkpoint 与 log-start-offset-checkpoint 主链
> 状态：补充篇，覆盖 zsxq【原理分析系列第十二篇】CheckPoint 机制。按 Kafka v4 展开两个 checkpoint 文件的作用与写入/读取时机。

## 1. 读者困惑
- recovery point 存什么？
- log start offset checkpoint 存什么？
- 什么时候写、什么时候读？

## 2. 一句话顿悟
**recovery-point-offset-checkpoint 记录各分区最后一次成功 flush 的 offset，log-start-offset-checkpoint 记录各分区 logStartOffset；重启时从 recovery point 开始回放，避免全量重放。**

## 3. 关键证据
- storage LogManager.java:37（checkpoint 文件名）。
- core LogManager.scala（写入/读取）。

## 4. 误解清单
- "checkpoint 存消息内容"：只存 offset
- "重启总是全量重放"：从 recovery point 开始

## 5. 版本边界与字数预算
- 基线 v4.x；目标 4000~6000 字。