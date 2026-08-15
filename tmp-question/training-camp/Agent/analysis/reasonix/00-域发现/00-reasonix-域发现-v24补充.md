# Reasonix 域发现 v24 补充(续扫第十轮:extension 剩余 — validate/publish/runtimeplan)— 2026-08-14

> 承接:v23。本轮:internal/extension/(protocol/validate 419/publish 417/runtimeplan 372)。
> 结论:**SubgraphKind 7 类子图分类(增量重建)**与 PublishGate(代际排空)确认——扩展运行时的高价值机制。

---

## 一、v24 深化确认(extension 剩余)

| 设计 | 位置 | 要点 |
|------|------|------|
| **SubgraphKind 7 类** | runtimeplan.go:11-37 | **子图分类**(None/InterceptorOnly/ProviderOnly/UIOnly/MCPOnly/Sidecar/Full)——**重建跳过未受影响的工作**(tools/prompt/interceptors/UI/MCP) |
| **PublishGate 代际门** | publish.go:13-133 | Publish(代际)/**BeginDrain(排空)**/IsStale/IsDraining/**AdmitNewWork**(代际拒绝新工作)/WithDrainTTL |
| **协议校验** | protocol/validate.go:17-199 | **17 个拦截点枚举**(InterceptEvent)→ 类型名键控;**必填字段递归校验**(decodeAndValidate/validateRequiredJSON/validateNestedRequired);ensureJSONEOF(尾随数据拒绝) |

---

## 二、关键设计(通用价值)

1. **"子图分类 → 增量重建"**:SubgraphKind 让 Rebuild 跳过未受影响的工作——**增量构建的依赖分类**(与 Reasonix boot RebuildFrom 只排空旧代同族,但更细)
2. **"代际排空门"**:PublishGate.BeginDrain/AdmitNewWork——**排空期间拒绝新工作**(与 Hermes drain_control、Reasonix scale-to-zero 排空同族)
3. **"必填字段递归校验 + 尾随拒绝"**:协议消息的严格校验——**协议严格性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v23 | — | 102 | 102 |
| v24 | extension 剩余 | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 appidentity/i18n/notify 支撑、cli 剩余(complete 849/transcript 790)——按需。
