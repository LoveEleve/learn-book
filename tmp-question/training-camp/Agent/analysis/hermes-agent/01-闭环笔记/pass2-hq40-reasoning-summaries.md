# hq40 推理摘要边界修复(Reasoning Summaries)— 产品②"流式边界"蓝本

> 项目:Hermes(agent/reasoning_summaries.py 67 行,小而聚焦)
> 假设:推理摘要模型流式边界——chat wire 无 summary_index → 从 delta 开 bold 标题信号重推导边界(域发现 v11:vercel/ai#6742 同问题)。是"流式边界修复"的样本。

---

## 一、设计:bold 标题信号重推导

**位置**:`reasoning_summaries.py:1-67`

```
问题:推理摘要模型(gpt-5.x 族/Responses API relay 到 chat wire)
  不逐 token 流式思维链——每个*完成*摘要部件发一个 reasoning_content
  delta,每个以 bold markdown 标题开头:
    {"delta": {"reasoning_content": "**Investigating likely culprit PRs**"}}

边界双形态(reasoning_summaries.py:11):
  Responses API 用 summary_index 分隔部件(有显式 index)
  chat wire 无 summary_index → 必须从"delta 开 bold 标题"信号重推导边界
  (vercel/ai#6742 同问题)

修复:检测 delta 开头 bold 标题 → 识别摘要部件边界
  ——"从 delta 开 bold 标题信号重推导边界"
  (**** 连续星号既非 bold 关闭也非打开——边界检测避坑,:20)

★ 富消息回显复合键(rich_sent_store.py:35):chat_id:message_id 复合键
  ——回显索引按(聊天,消息)定位
```

**正确性价值**:无 index 时用内容信号重推导边界——跨 wire 兼容(chat wire 无 summary_index);有 index 用 index(Responses API 双形态)。

**产品④映射**:流式边界修复——无显式索引时内容信号重推导(与 hq36 温度契约同族:provider 差异内化)。

> 测试契约:test_reasoning_summaries.py(关联)
> 位置:reasoning_summaries.py(67 行)
