# MI-1 MeterRegistry — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, 收敛后全量验证)

### ① 机制: 锚点有效性/因果链/闭环
- [x] 注册全链: registerMeterIfNecessary (L629-633 类型检查) → getOrCreateMeter (L671) → mapId (L682) → accept (L786) → configure+merge (L720-729) → create → 三 map 写入 (L739-741) — 全实读
- [x] 双重 ID 映射因果: 为何 preFilter 读无锁 (注释 L103-106 明示设计权衡 P1)
- [x] synthetic 级联闭环: HistogramGauges.synthetic → 注册时登记 (L732-736) → remove 级联 (L853-869)
- [x] stale 机制闭环: 迟 filter (L908-910 addAll) → 旧 id 标记 → 下次注册 unmark (L705)
- [x] 生命周期闭环: close → CAS → meter.close + detector.close; 关闭后 noop

### ② 语义: 术语精确
- [x] "map" 是 filter 的 ID 变换 (非并发 map) — 文档已明确
- [x] "noop 降级" 精确 (静默空操作 + count=0)
- [x] "ACCEPT 短路" 精确 (L792 立即返回 true)
- [x] harness 打脸修正: renameTag 只改 tag 不改 name — 文档§十已记录

### ③ 架构/拓扑: 前向引用
- [x] MI-1 是域根: 无前向引用声明 (Simple/Composite 均在本域)
- [x] 联动声明: HistogramGauges (MI-3) 被 Simple/Composite newTimer 调用 — **后向引用** (MI-3 先于 MI-1 存在的事实), 非前向声明, 允许
- [x] Search.acceptFilter 联动 MI-7 — 后向引用, 允许

### ④ 引用形式: 方法名 + 行号
- [x] 全部锚点格式 "方法 L行号", 写作时重 grep 确认

### ⑤ 数字穷举
- [x] simple 4 / composite 13 / step 13 / push 3 / cumulative 6 / noop 10 / search 4 / logging 3 文件 — 实测
- [x] 18 断言 harness 全绿 (17 首轮 + 1 修正后)
- [x] MeterRegistry 1337 行全文通读 (1-1337 分段实读)

### ⑥ 排除审查
- [x] Composite* 各实现类 (CompositeTimer 等 12 个) 未逐个深读 — **边界声明**: 机制 (CompositeMeterRegistry add/updateDescendants) 已验证; 各 Composite* 仅是转发器, 语义在 MI-2/MI-3 域 (其内部值语义) — 待对应域深读时验证
- [x] cumulative/ 6 文件: CumulativeCounter 等直接累加 (无步长) — MI-2 域内

### ⑦ 全面性
- [x] Q1-Q6 问题清单全闭环 (pass2-questions.md)
- [x] 40 文件反向扫描: 本域相关 (Metrics 323/HighCardinality 308/Logging 321) 全覆盖

## 结论
MI-1 收敛。遗留: Composite* 转发器细节归 MI-2/MI-3 验证; Step 家族细节归 MI-2 (StepCounter 已读) / MI-3 (StepTimer 直方图)。
## 审查轮次: 第二轮 (2026-08-17, 锚点穷举)
- [x] **20 处锚点穷举回源**: 14 精确 + 6 修正 (getOrCreateMeter L688-751 / mapId L677-684 / configure L723-729 / accept L785-794 / remove L838-859 / clear L871-875 / removeByPreFilterId L821-826 / close L1267-1280)
- [x] Composite onMeterAdded L69-72 / updateDescendants L209-239 / lock L195-207 — 修正
- [x] Search 精确定位: meterStream L256-268 / findOne L200-202 / findAll L320-323 / acceptFilter L215-225 — 修正
- [x] HCD: calculateThreshold L177-183 / 频率计数 L168-176 — 修正
- [x] 反模式自查: #5 (12 种真实 API 调用, 无模拟) / #4 (52 锚点全溯源) / #9 (机制描述主导) / #10 (版本明确 1.17.x merge) — 全通过

## 审查轮次: 第三轮 (2026-08-17, 排除验证 + harness 补强)
- [x] Composite* 转发器验证: AbstractCompositeMeter (children IdentityHashMap + firstChild 懒 noop) — 声明成立
- [x] PushMeterRegistry 深读: Semaphore(1) 互斥 + 随机初始延迟对齐步长 (L166-172) + scheduleAtFixedRate (L114) + close final publish (L140-145)
- [x] **harness 补强 18→22 断言, 打脸 2 次**:
  - 迟 filter 断言修正: 带新 tag 是新 originalId → 走新路径; 相同 id 重新注册 → 原实例 + unmarkStale (实证 stale 机制)
  - **STEP 空档归零实证**: add(2000) 跨 2 步长 → previous=noValue()=0 (实证 StepValue L58-62 语义); add(1000) 单步长 → previous=5.0
- [x] 22/22 全绿

## 审查轮次: 第四轮 (2026-08-17, 官方测试交叉验证)
- [x] MeterRegistryTest 25 测试全覆盖本 outline 声称 — 无一遗漏
- [x] **补 2 个机制** (测试揭示): 
  - multiplePreFilterIdsMapToSameId: replaceTagValues 多值→同 mapped id→同一 meter; meterToPreFilterIdMap 反向清全部 (removeByPreFilterId L821-826)
  - doNotCallFiltersWhenUnnecessary: preFilter 命中直接返回, 不调用 filters (L671-674)
- [x] removeMetersWithSynthetics / filterConfiguredAfterMeterRegistered / unchangedStaleMeterShouldBeUnmarked / registryCloseShouldCloseHighCardinalityTagsDetectorOnlyOnce — 均与 harness/outline 一致

## 审查轮次: 第五轮 (2026-08-17, 复杂度/一致性/残留收敛)
- [x] 复杂度检查: 最大循环嵌套深度 6 (getOrCreateMeter 注册路径 L723 区), mapId 深度 2 — 无意外热点
- [x] 数字统一: 18/18 → 22/22 (outline + PLAN)
- [x] 残留清零: 旧锚点 (L671-741/L786-794) 全部修正
- [x] 最终抽查 6 锚点精确命中
- [x] 37 个唯一行号引用全部回源 (R2-R7 累计)

## 收敛判定 (第五轮终)
- 锚点: 全部实测回源, 零残留
- 数字: 全部一致 (22/22 断言, 文件数实测)
- 机制: 注册/过滤/移除/生命周期/合成/查询全闭环, 官方测试 25 个全覆盖交叉验证
- harness: 22/22 全绿, 累计打脸 3 次 (renameTag/迟 filter 断言/STEP 空档) — 全部修正
- 无待解决项

## 审查轮次: 第六轮 (2026-08-17, 因果链/代际/harness 覆盖扩展)
- [x] R12 getMappedId 因果链: 唯一外部调用方 **MultiGauge L94** (快照行 ID 映射, MI-2 域) — outline 补注
- [x] R7 requireValid 校验链: MeterRegistryConfig.requireValid (L47-49) → validate().orThrow(); SimpleConfig.validate = checkAll(step, mode) (L58-60); MeterRegistryConfigValidator.checkAll (L36) — E7 配置校验联动 MI-7
- [x] R8 代际: HEAD = 1.17.x merge (shallow); 引用的 API 最高 @since 1.16.0 均在内; compatibleVersion=1.17.0 — 无代际问题
- [x] R9 harness 补覆盖 22→25: LoggingMeterRegistry (E10, start→定时 publish→sink 输出) + HighCardinalityTagsDetector (E11, 3>2 检测 + 频率计数)
- [x] **harness 打脸第 4 次**: Logging publish 场景 add(500) 跨 5 步长 → 空档归零 → count=0 无输出; 修正 add(100) 单步长 → 输出。**再次实证 StepValue 空档归零语义在 publish 链的传播**
- [x] 25/25 全绿

## 收敛判定 (第六轮终)
- 因果链: getMappedId/requireValid 调用方确认, 无悬空引用
- 代际: 无
- harness 覆盖: outline §四/E10/E11 声称全部实测
- 累计打脸 4 次全修正
