
---

## 第三轮复审 (REVIEW-3, 2026-08-15) — 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛
- 四行双链 + 桥链 01→02→03 + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛
- 12/9/8 (🔴A ≥8 全达标); 上限抽查 (GroupCoordinatorConfig:193/OffsetMetadataManager:1039) OK

### R3 维度3 (规划断言复核): **1 发现 (规划错误), 大纲修正** ⚠️⚠️
- **发现**: 规划断言 "RangeAssignor(默认)" — 4.1.2 KIP-848 内置列表首位 = **UniformAssignor** (GroupCoordinatorConfig.java:187-188), 注释 GroupCoordinatorConfig.java:L193 "first one is the default"
- **修正**: 02-L2 补 "KIP-848 默认 Uniform (GroupCoordinatorConfig.java:L187-193)"; Range 默认仅适用旧协议时代 — 规划第 4 处错误 (淘汰表笔误/两态 reset/Classic 混淆/默认 assignor)
- OffsetExpirationCondition 存在性验证 ✅ (OffsetMetadataManager.java:1032-1039)

### R4 维度4 (横切: epoch 语义): 0 发现, 收敛
- memberEpoch 5 处覆盖 (增量核心: -1/-2 离组/递增版本)

### R5 维度5 (负面+开篇): 1 发现, 修复
- 01/02 篇负面 0 → 补 (不做无协调广播/不做客户端算分配); 03 篇原有"不是"已覆盖
- 开篇词 5

### 内容深度轮: 0 发现
- 反写测试: 12 场景 ✅
- r29-pubsub 对照核验: PUBLISH/SUBSCRIBE 在 RM... 在 r29 outline/pass1 实证 ✅ (编造教训未重犯)
- 双协议/增量/KIP-848 语义与源码一致 ✅

### 收敛判定
R1/R2/R4 + 内容深度轮收敛; R3 抓 1 规划断言错误 (默认 assignor, 规划第 4 处错误) + R5 补负面 — 修复后三形态零残留。K-6 深度收官完成。

---

## 第四轮复审 (REVIEW-4, 2026-08-15) — 07 五维度深度收官

### R1 维度1 (桥+结构): 0 发现, 收敛
- 四行双链 + 桥链 + 零反模式

### R2 维度2 (锚点密度): 0 发现, 收敛
- 12/10/8 (🔴A ≥8 全达标); 上限 OK

### R3 维度3 (规划断言复核): **1 发现, 1 修复** ⚠️
- **发现**: 规划 R2 断言 streams 面 (StickyTaskAssignor/CopartitionedTopicsEnforcer) 未覆盖 — 核验: CopartitionedTopicsEnforcer 存在 (streams/topics/CopartitionedTopicsEnforcer.java:39) 但属 streams 子包; 消费组 assignor/ 无 Sticky (grep 实证)
- **修复**: 02-L2 补 streams 面 (Copartitioned L39) + 明确 Sticky 是 streams 专用 (我的"streams 侧"表述正确 ✅)

### R4 维度4 (横切: 记录/状态机): 0 发现, 收敛
- 记录/CoordinatorRecord/__consumer_offsets 11 处 — 用日志存状态语义全覆盖

### R5 维度5 (负面+开篇): 0 发现, 收敛
- 负面声明 1/1/2; 开篇词 6

### 内容深度轮: 0 发现
- 反写测试: 12 场景 ✅
- harness 审计: 9 处对照锚点 + 17/17 复验 ALL PASS ✅
- 双协议/增量/Uniform 默认/Copartitioned 归属与源码一致 ✅

### 收敛判定
R1/R2/R4/R5 + 内容深度轮收敛; R3 抓 1 覆盖缺口 (streams 面) — 修复后三形态零残留。K-6 深度收官完成。
