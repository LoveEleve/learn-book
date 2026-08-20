# SCC-9 配置加密 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | **语义重大** | TextEncryptorBindHandler 经 **BootstrapRegistry 注册为 BindHandler** (TextEncryptorConfigBootstrapper, Boot 3 新架构) — TextEncryptorUtils.register (L77) + 三选一 (RSA/对称/Failsafe L80-87); promote 带 **isLegacyBootstrap 检查** (L48-52, SCC-1 双轨联动) | 已修 §1 |
| 7 | **装配双轨** | 新路径 (BootstrapRegistry Bootstrapper) vs 旧路径 (EncryptionBootstrapConfiguration @Bean) 并存, 共用 TextEncryptorUtils 桥梁 | 已修 §3 |
| 8 | 细节补强 | PropertyVisitor 去重用 **SystemEnvironmentPropertySource** (L172-177, relaxed-binding: DB_URL 与 db.url 同属性) | 已修 §4 |
| 9 | 细节补强 | EnvironmentDecryptApplicationInitializer **removeDecryptedProperties 先清旧解密源** (L93) — 刷新时防旧值残留 | 已修 §5 |
| 10 | 验证通过 | 双路径 startsWith 整值检查 (L56/L155) / 负面空间"不做部分解密"成立 / SCC-1 DECRYPTED 特判衔接 (L200-201) | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | TextEncryptorBindHandler 类 **L36** + onSuccess **L55-60** (大纲写 L41-47) + failOnError **L79-81** (写 L66-69) | 已修 |
| 2 | 锚点漂移 | AbstractEnvironmentDecrypt: COLLECTION_PROPERTY **L39** (写 L33) + DECRYPTED 源 **L44** (写 L37) + 前缀 **L49** (写 L39-40) + 主循环 **L67** (写 L49-105) | 已修 |
| 3 | 锚点漂移 | EncryptionBootstrapConfiguration 类 **L48** (写 L30-147) + Failsafe **L64** (写 L54-61) + Rsa **L80** (写 L81-95) | 已修 |
| 4 | **机制遗漏** | **FailsafeTextEncryptor delegate 模式** (TextEncryptorUtils.java:57-70): bootstrap 阶段密钥未就绪 → Failsafe 占位 → setDelegate 延迟替换 — "两阶段密钥可用性"优雅解, 大纲原只说"兜底" | 已补 §6 |
| 5 | 验证通过 | EnvironmentDecryptApplicationInitializer L45/L51/L80-81 / Vanilla L99 / 嵌套 Failsafe L143 | 通过 |

## 锚点密度统计

- file:line 锚点数: **25+** (🟡B 标准 ≥4 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (算法内建/密钥轮换/索引/部分解密/结果持久化/审计日志)
- [x] 每条有对照物 (KMS/Spring Security Crypto)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 ({cipher} 解密/失败处理/两层识别/集合属性)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (双路径 → 装配 → 索引处理 → 初始化器)
- [x] 边界交代: 双路径分工/failOnError 两处同构/Failsafe delegate

## 方法论教训

- **"兜底"类要深挖** — FailsafeTextEncryptor 表面是兜底, 实际是 delegate 占位模式 (延迟替换); 空实现类可能承载设计决策
- **锚点必须落在声明而非注释区** — AbstractEnvironmentDecrypt 的 L33 是注释, 正则实际在 L39 — 与上轮 RefreshScopeLifecycle 同类教训
