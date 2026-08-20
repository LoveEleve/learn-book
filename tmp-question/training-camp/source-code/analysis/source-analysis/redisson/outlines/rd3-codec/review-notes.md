# RD-3 Codec 序列化体系 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 0 | **二次 REVIEW (07 五维度, 补测新维)** | RD-1 已测 07 R1-R5; RD-3 换维度补测: R1 反写 (闭环↔大纲覆盖) / R2 锚点密度 / R3 跨域一致性 / R4 深度反写 / R5 事实核查 | 见下方 R1-R5 明细 |
| R1-1 | 反写覆盖 | 8 闭环关键点全部被 3 篇大纲利用 (三池化全参/跨包证据/失败归还) — 无断层 | 通过 ✅ |
| R2-1 | **锚点密度 (系统性)** | 裸行号 19 处: `(L214-235)`/`(L105)` 等无文件名; 带文件名仅 01=4/02=2/03=0 (🔴A 需 ≥8) | **全部补文件名: 01=13/02=15/03=10** ✅ (同 RD-1 教训复发 — 已记录为铁律) |
| R3-1 | **跨域补链** | 并行 AI 新产出 r28-networking (RESP); RD-3 篇1 命令衔接正好对齐 | 篇1 header + 正文补 [[r28-networking]] 双向对话 |
| R4-1 | 深度缺口 | Kryo 池满 obtain 阻塞语义未写明 | 篇2 补 "(池满阻塞等待归还, 1024 并发 writer 触顶, 标注推断)" |
| R5-1 | **事实核查** | 篇3 只说"V2 重构"未提旧 `LZ4Codec` 并存 — 实测旧版在 (net.jpountz.lz4), V2 换 commons-compress | 篇3 补两版并存说明 (算法同, 库不同) |
| 1 | **结构发现 (REVIEW 替代 09)** | Codec 接口不在 org.redisson.codec 而在 **client/codec/Codec.java:30** — 双包结构 (协议面 vs 实现面) | pass1 + 篇1-S1 展开 |
| 2 | 跨包证明 | StringCodec implements JsonCodec — 分层非隔离铁证 | 篇1-S1 |
| 3 | 机制实证 (harness) | CompositeCodec value=null NPE 契约 → MiniCodecTest 9/9 PASS | harness |
| 4 | 默认值根因链 | 默认 Kryo5 = 4.0.0 Jackson optional | 篇2-S1 |
| 5 | 失败归还协议 | encoder out.release vs decoder 池不回收 — 精确差异 | 篇2-S1 |
| 6 | completeness ❌ 回填 | Q15/Q31/Q50 (迁移/灰度/选型表) | 篇3+篇1 负面空间+选型表 |
| 7 | 通过项 | 全锚点行号 awk 验证 ✅ | 记录 |

## 二次 REVIEW 结论 (05/06 合规)

- [x] R1-R5 每轮新维度 (反写/锚点/跨域/深度/事实), 无同维度重复
- [x] 锚点密度达标 (≥8): 13/15/10
- [x] 跨域补链 r28-networking (06 §3 新发现回流)
- [x] 发现真实问题 4 项 (裸行号系/池满语义/LZ4 两版/补链) — 非格式扫描
- [ ] 完成状态: 待用户确认后更新 HANDOFF-REDISSON (RD-3 二次 REVIEW ✅)