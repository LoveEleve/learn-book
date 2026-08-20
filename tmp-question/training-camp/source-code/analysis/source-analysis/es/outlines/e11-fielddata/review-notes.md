# E-11 FieldData — 六层深审 REVIEW 记录 (2026-08-14)

> 审查方法: 07 五维度 + 逐锚点 awk/sed 核对 + 裸行号扫描 + 行号上限检查 + 跨域引用核验
> **结论: 深审通过 (3 处行号偏差 + 18 裸行号根治)**

## 第一层: 锚点验证 (写后即验, 抓 3 处偏差)

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 | GlobalOrdinalsBuilder 断路器检查点 (L66-68) | **L65** | -1 |
| 2 | 01 大纲 | addWithoutBreaking (L76) | **L74** | -2 |
| 3 | 02 大纲 | "40%" (L86) / overhead 1.03 (L92) / loadGlobalDirect (L148) | **L83 / L89 / L144** | -3/-3/-4 |

## 第二层: 机制实证 (全过)

- load/loadDirect/loadGlobal 契约 (IndexFieldData.java:59,64,250-252) ✅
- GlobalOrdinalsBuilder 五步 (逐段 load L51-52 → FilterTermsEnum L59-72 → OrdinalMap L72 → ramBytesUsed L73 → addWithoutBreaking L74) ✅
- 断路器: FIELDDATA limit 40% (L83) + overhead 1.03 (L89) + addEstimateBytesAndMaybeBreak (CircuitBreaker.java:84) ✅
- fielddata 默认 false (TextFieldMapper.java:249) / docValues 默认 true (KeywordFieldMapper.java:143) ✅
- 排序 comparator 4 源 + converter/loadDocValues/getNumericDocValues ✅
- 缓存契约 clear/clear(fieldName) (IndexFieldDataCache.java:30,35) ✅

## 第三层: 编造检查 (零)

- 5.0 迁移: doc_values 默认值变化 commit (7290b2dc916) 实证 ✅
- fielddata 现代默认 false (TextFieldMapper.java:249) 实证 ✅

## 第四层: 覆盖缺口 (3 项 — completeness ⚠️ 已回补)

- 01-L2 loadDirect 绕过缓存 ("possibly cached" 注释) ✅
- 02-L2 CircuitBreakingException 运维 (调 limit/降并发/查高基数) ✅
- 02-L2 40% 是 JVM heap ✅

## 第五层: 裸行号

- 修复前 19 处 → 修复后 **0 残留**
- 行号上限: 8 文件全 OK (GlobalOrdinalsBuilder 130 / GlobalOrdinalsIndexFieldData 276 / Comparator 163 / CircuitBreaker 128 等)

## 第六层: 跨域引用核验

- redisson/rd6-localcachedmap ✅ / redis/r23-evict ✅
- E-7/E-12/E-2 内部衔接 ✅
- 全部 [ -d ] 验证; 对照声明含"同词+一句摘要" ✅

---

## 第二轮复审 (REVIEW-2, 2026-08-14) — 自查修复后复核

> 目的: 验证自查修复精度 + 抓方法体内行号偏移 (前 6 域经验延续)

### 发现 3 处新偏差

| # | 文件 | 原写 | 实测 | 类型 |
|:--:|---|---|---|---|
| 1 | 01 大纲 L25 | FilterTermsEnum 包装 (L59-72) / 断路器检查点 (L66-68) | **L59-71 / L65** | -1~-3 |
| 2 | 01 大纲 L50 | 逐段 load (51-52) / 断路器检查点 (66-68) / addWithoutBreaking (76) | **51-53 / 65 / 74** | 锚点清单残留 |
| 3 | pass2-q1 L36 | FIELDDATA (L31) / REQUEST (L39) / IN_FLIGHT (L44) 裸行号 | 补 CircuitBreaker.java 前缀 | 格式 |

### 已验证正确 (20+ 项)

- IndexFieldData load L59 / loadDirect L64 / loadGlobal L250 / loadGlobalDirect L252 ✅
- GlobalOrdinalsBuilder build L39 / 逐段 load L52-53 / FilterTermsEnum L59-71 / 断路器检查 L65 / OrdinalMap L72 / ramBytesUsed L73 / addWithoutBreaking L74 ✅
- 断路器: FIELDDATA limit L81 / "40%" L83 / overhead 1.03 L89 / CircuitBreaker PARENT L26 / FIELDDATA L31 / REQUEST L39 / IN_FLIGHT L44 / addEstimateBytesAndMaybeBreak L84 ✅
- loadGlobalDirect 消费点 L144 ✅
- comparator L40/43/75/86 / cache L30/35 / GlobalOrdinalsIndexFieldData L44/83/122 ✅
- TextFieldMapper fielddata 默认 false L249 / Keyword docValues 默认 true L143 ✅

### 根因与根治 (确认收敛)

- **根因**: 上一轮 sed 只修了 L25 主行, 漏了 L50 锚点清单 + pass2-q1 的混合格式 — 证明"批量替换必须逐文件复核"仍是关键
- **确认**: 三遍验证闭环持续收敛 — E-11 本轮仅 3 处 (前几域 6-10 处)
- 🟡 B 无 harness 符合方案 B 预期; 行号上限 5 文件全 OK; 裸行号零残留; 跨域 3 个 [ -d ] 通过
