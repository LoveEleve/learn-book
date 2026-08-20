# MI-7 MeterFilter — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, 收敛后全量验证)

### ① 机制: 锚点有效性/因果链/闭环
- [x] 全文件通读: MeterFilter.java 481 行 (L29-481) / MeterFilterReply.java 3 值 / NamingConvention.java 全 / MeterRegistryConfigValidator.java 全 / PropertyValidator.java 6 种 / Validated.java 核心 / MeterRegistryConfig.java
- [x] 消费闭环: filter 在注册流程 3 处消费 (mapId L677-684 / accept L785-794 / configure L723-729) — MI-1 已实证
- [x] maxExpected merge 方向因果: builder 作 this → filter 强制 (P1, harness 实证)
- [x] maximumAllowableTags 双通道委托: accept+configure 各自 delegate (P2)
- [x] 校验链闭环: 构造 → requireValid → orThrow → ValidationException

### ② 语义: 术语精确
- [x] "NEUTRAL 继续 / ACCEPT 短路 / DENY 拒绝" — 精确 (3 值枚举)
- [x] "前缀追加不覆盖" (commonTags) — harness 实证
- [x] "导出侧 vs 注册侧" (NamingConvention) — **harness 打脸修正** (getConventionName 变换, getName 原始)
- [x] "filter 链 > meter builder > registry default" 优先级精确

### ③ 架构/拓扑: 前向引用
- [x] MI-7 拓扑第 2 位正确: 只依赖 MI-1 (MeterRegistry 消费点), 无前向引用
- [x] DistributionStatisticConfig 引用 (MI-3 域) — 后向引用 (已有), 合法
- [x] 交叉引用: Search.acceptFilter (MI-1 查询) → MeterFilter — 后向

### ④ 引用形式: 方法名 + 行号
- [x] 全部锚点格式 "方法 L行号" — 实读验证

### ⑤ 数字穷举
- [x] 静态工厂穷举: 15 个全部列出并归类 (无遗漏)
- [x] 校验调用点穷举: 3 处 (Simple/Push/Dropwizard)
- [x] PropertyValidator 6 种穷举
- [x] 16 断言 harness 全绿

### ⑥ 排除审查
- [x] InvalidReason (MISSING/MALFORMED/OTHER) — 归类 E7 未展开细节, 边界声明
- [x] DurationValidator — 边界 (PropertyValidator 内部)

### ⑦ 全面性
- [x] 接口 3 默认方法 + 15 工厂 + 4 预置 convention + 校验链 — 全覆盖
- [x] 与 MI-1 的双向联动 (消费点/acceptFilter) 明确

## 结论
MI-7 收敛。遗留: InvalidReason 细节归校验扩展 (非核心); DurationValidator 边界。
## 审查轮次: 第二轮 (2026-08-17, 锚点穷举/官方测试交叉/harness 补强)
- [x] 锚点穷举: 15 工厂 + 3 默认方法定位, 修正 7 处 (acceptNameStartsWith L297 / maxExpected L308-331+L346-362 / minExpected L365-401+L403-421 / forMeters L425 / 3 默认方法 L449/L466/L477)
- [x] 官方测试交叉: MeterFilterTest 17 个全对照 — 补 mapThenAccept 语义 + maximumAllowableTags 2 边界; NamingConventionTest 6 个 (snakeCase/camelCase/upperCamelCase)
- [x] camelCase 细节: 首段保留 + 后续段首字符大写 (已有大写保留) L66-100
- [x] **harness 补强 16→19, 打脸 2 次**:
  - 重复注册幂等: maximumAllowableTags 已存在值重复注册返回**同一实例** (MI-1 幂等语义) — 断言修正 count 累计 2.0
  - map→accept 顺序: accept 看到 map 后 id + ACCEPT 短路跳过 deny — 实证官方 mapThenAccept
- [x] 19/19 全绿

## 收敛判定 (第二轮终)
- 锚点: 全部精确定位修正
- 机制: 顺序语义 (map→accept) + 幂等 + 短路 — 全实证
- harness: 19/19, 累计打脸 2 次全修正

## 审查轮次: 第三轮 (2026-08-17, 实现细节实读/跨域一致性/数字统一)
- [x] maximumAllowableMetrics **两阶段语义**: 先查 size>max→DENY (过注册保护), add 后再查 → 恰好第 max 个 NEUTRAL 通过, 第 max+1 个 DENY (harness 边界实测通过)
- [x] maximumAllowableTags 内部: observedTagValues CHM keySet; 前缀不匹配→null→NEUTRAL 不计数; 值新→size>=max→onMaxReached.accept/configure 双通道
- [x] 谓词 3 工厂实现: denyUnless (真→NEUTRAL 继续/假→DENY), accept (真→ACCEPT 短路), deny (真→DENY) — 与 MI-1 消费点 L785-794 交叉一致 (DENY→false/ACCEPT→true 短路/全 NEUTRAL→true)
- [x] replaceTagValues exceptions: 逐值 equals 匹配保留原值 (L122-140)
- [x] **数字统一修正 2 处**:
  - 静态工厂 15→**20** (maxExpected/minExpected 各 3 重载 Duration/long/double 共 6, 漏 6 个工厂)
  - PropertyValidator 6→**10 种** (漏 getString L107/getUrlString L112/getUriString L135/getStringMap L158)
- [x] 前向引用纪律: DistributionStatisticConfig (MI-3 域) 引用均经 harness 实证 (getMaximumExpectedValue 断言), 合法
- [x] Validated and L79 / required L110-112 锚点复验

## 收敛判定 (第三轮终)
- 工厂穷举: 20/20 全列
- 语义: 两阶段上限逻辑/过注册保护 — 实读
- 数字: 20 工厂 / 10 validator / 3 校验调用点 — 全对

## 审查轮次: 第四轮 (2026-08-17, 暗路径机制实读/harness 实证)
- [x] commonTags "不覆盖" 真正机制: Tags.concat = Tags.of(common).and(idTags) → **sorted-merge dedupe** (Tags.java merge L123-148, 注释 "In case of key conflict prefer tag from other set" L147-148) — meter 自身 tag 覆盖 common tag; 修正"前缀追加"措辞
- [x] 校验链触发实证: SimpleMeterRegistry(SimpleConfig, Clock) 构造 (L52-56 requireValid) + 非法 step "bogus" → ValidationException 实测抛出 (harness 12 号)
- [x] 过程教训: 编译错误被 2>/dev/null 吞 → 误跑旧 class (19 PASS 假象); 修正流程 (编译必须检查错误输出)
- [x] harness 20/20 全绿 (20 断言, 打脸累计 2 次)

## 收敛判定 (第四轮终)
- 机制级: commonTags dedupe / 校验链触发 — 全实证
- harness: 20/20
- 流程教训已固化: javac 错误输出必须可见

## 审查轮次: 第五轮 (2026-08-17, 终审: 残留扫描/复杂度/数字闭环)
- [x] 残留扫描: outline 零旧锚点; pass0 清零 2 处 (PropertyValidator 6→10 种 / forMeters L428→L425)
- [x] 复杂度: 唯一循环在 ignoreTags (per-tag × tagKeys 线性扫描, 小集合, 已知可接受成本) — 记录
- [x] 数字闭环: 20 工厂 (grep 穷举) / 10 PropertyValidator / 3 校验调用点 / 3 默认方法 / 4 预置 convention / harness 20/20 (22 check 调用含 12 号互斥二选一, 自洽)
- [x] 语义终核: ACCEPT 短路 / DENY 拒绝 / NEUTRAL 继续; map→accept 顺序; dedupe 机制; 两阶段上限; filter 链 > builder > default; 导出侧 convention — 全实证

## 收敛判定 (第五轮终, 5 轮审查全部通过)
MI-7 MeterFilter 域: 无遗留问题。5 轮维度: ① 07 七维 ② 锚点穷举+官方测试 ③ 实现细节+数字统一 ④ 暗路径机制 ⑤ 终审残留清零。
harness 20/20, 打脸累计 2 次 (snakeCase 导出侧 / 重复注册幂等) 全修正。

## 审查轮次: 第六轮 (2026-08-17, 官方测试断言逐条对照 + 行为边界补测)
- [x] 官方 17 测试断言逐条对照 (assertThat 逐行 L60-295), 发现 5 个未覆盖语义并补 harness:
  - renameTag 前缀不匹配 → 原样不动 (L103-106)
  - replaceTagValues 函数只对匹配 key 调用
  - maximumAllowableTags 超限 + onMaxReached=accept() → 放行 (L247: onMaxReached 返回值决定)
  - forMeters 非匹配: accept 不委托 + map/configure 原样 (官方 forMeters 测试 L278-295 三断言)
  - minExpected 完全未测 → 补 (Summary 100<999 覆盖)
- [x] **harness 打脸第 3 次**: "同 key 2 值" 断言 FAIL → Tags 不可变 sorted+deduplicated, 同 key 不可能 2 值; 修正为 2 meter × 1 次调用
- [x] configure 类型筛选: maxExpected 对 GAUGE 无效 (config 原样) — 18 号
- [x] harness 20→30/30 全绿

## 审查轮次: 第七轮 (2026-08-17, 组合语义/注册路径终走查)
- [x] 组合语义终验: ACCEPT 短路跳过 deny (mapThenAccept) / NEUTRAL 链继续 / DENY 拒绝 — 三态全实证
- [x] 注册路径走查: mapId→accept→configure 全链在 MI-1 outline 与 MI-7 outline 双向一致 (L677-684/L785-794/L723-729)
- [x] forMeters 委托完整性: accept/map 全委托实证, configure 委托 (官方 configurePrimaryMeters L294-295: 非匹配返回原 config)
- [x] 文档一致性: outline 六节结构完整, 锚点与 pass0 同步 (20 工厂/10 validator/3 调用点/30 harness)

## 收敛判定 (第七轮终, 7 轮审查全部通过)
MI-7: 无遗留问题。7 轮维度: ① 07 七维 ② 锚点穷举+官方测试 ③ 实现细节+数字统一 ④ 暗路径机制 ⑤ 终审残留清零 ⑥ 官方断言逐条对照 ⑦ 组合语义终走查。
harness 30/30, 打脸累计 3 次全修正。
