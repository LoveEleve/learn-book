# q25 — Hook 协议 + Compaction-Basic(深度版:合并语义 + 压缩契约)

> 域:②执行(外部桥)+ ②上下文(压缩) | 文件:packages/(hooks/hook-protocol/src:codec/matcher/merge/detached/events)+(compaction/compaction-basic/src:config/region/summarizer/types + tests/compaction-basic.spec.ts 1880)
> review 轮次:3 轮(源码全文核心 + 压缩测试契约 30+)

---

## 假设

Hook 协议 = 外部 hook 输出的解码/匹配/合并(权限决策合并是核心:deny>ask>allow)。Compaction-Basic = 压缩 provider(压力决策 + 保留/摘要)。**压缩契约揭示压力阈值/保留切割/回退语义**。

## 验证

### 1. 合并语义(设计 1:deny > ask > allow)

```ts
// hooks/hook-protocol/src/merge.ts:15-62:
MergedHookOutcome = { decision(deny>ask>allow;none), reason(joined), stop, stopReason, additionalContext[], systemMessages[] }
rank:deny/block=3 > ask=2 > approve/allow=1 > none=0
mergeHookOutputs:折叠所有匹配 hook 输出;空列表 → 中性(决策 none)
```

### 2. Compaction-Basic 契约(设计 2:压力/保留/回退)★ review 轮 3

```ts
// tests/compaction-basic.spec.ts(30+ 契约,关键):
1. 配置(288-413):低摩擦服务默认;threshold/retention 独立覆盖;exact provider/model 政策覆盖 + 按模型比例;
   摘要目标对(inherit/clear/replace);公共值 + 压力政策不变量校验
2. 容量(486-545):无 durable routed 模型跳过(不用 AgentOptions fallback);未列模型经 provider 适配器计量;
   转发 turn 取消到主动模型元数据解析;同模型 id provider 切换后重解析;仅主动压力需容量(provider 确认溢出不需)
3. 压力决策(568-726):整个表面是不可分工具对 → 拒绝强制溢出;低于阈值无操作;高于阈值压缩定价头部;
   durable 路由请求包计数不进表面;无 compactable 范围 → 拒绝;统一测量(每压力/保留决策);
   收缩检查点仍高于阈值 → 限制重试;保留切割头部保留工具调用/结果配对;定价表面非当前位面拒绝;
   切割消耗唯一工具对 → 拒绝
4. 修剪/摘要(772-822):低于压力不机会主义修剪;修剪单清压力跳过 LLM 摘要;修剪不足才摘要;
   无可选插件保留原行为
5. 检查点(842):可重放检查点落地(framed + 精确源 seq + token 价格)
```

**设计要点**:压力决策以"统一测量"为原则;保留切割保工具配对(不撕裂调用/结果);回退链(修剪 → 摘要 → 拒绝)。

### 3. 解码/匹配(设计 3:codec + matcher)

```ts
// codec.ts:parseHookOutput(exitCode, stdout, stderr, expectedEventName?)
// matcher.ts:matchesMatcher/matcherDiagnostic;events.ts:HookInvocation/HookResultRecord + stderr 摘要(500)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 合并语义(deny>ask>allow + stop) | hooks/hook-protocol/merge.ts | ③多审查器合并 |
| 2 | 压力决策(统一测量 + 工具对不可分) | compaction-basic.spec:568-726 | ②压缩正确性 |
| 3 | 保留切割保工具配对 | compaction-basic.spec:704-734 | ②上下文完整性 |
| 4 | 回退链(修剪→摘要→拒绝) | compaction-basic.spec:772-822 | ②失败循环 |
| 5 | 可重放检查点(精确源 seq + 价格) | compaction-basic.spec:842 | ④可重放 |

## 面试弹药

- "最严格决策合并":deny > ask > allow——多 hook 权限单一化
- "统一测量":每压力/保留决策同一测量——决策一致性
- "保留切割保配对":工具调用/结果不撕裂——上下文完整性
- "回退链":修剪单清跳过 LLM,不足才摘要——成本最优
- "整个表面不可分拒绝":工具对不可分时不强制溢出——安全

## 待深挖

- [ ] region.ts 的区域选择算法
- [ ] summarizer 的摘要调用
- [ ] matcher 的匹配模式
