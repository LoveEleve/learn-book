# SCC-9 配置加密 — {cipher} 的两种解密: 绑定期钩子与环境期后处理

> 前置: [[SCC-1-Bootstrap]] (bootstrap 阶段解密) + [[SCC-8-RefreshEndpoint]] (刷新重跑 decrypt) | 引出: [[SCC-13-NamedContextFactory]] (配置面收束) | 对照: Jasypt + Spring Security Crypto
> 🟡 B | 方案 B (标准) | 闭环: q1(双解密路径) q2(TextEncryptor 装配) q3(failOnError 语义) q4(索引属性)
> Pass 2 闭环: q1(BindHandler vs EnvironmentDecrypt) q2(双条件装配) q3(两处同构) q4(COLLECTION_PROPERTY)

**读者处境**: 配置中心的密码是 `{cipher}xxxxx`, 应用怎么解出明文? 解密失败 (密钥不对) 会怎样? `{cipher}` 前缀在哪两层被处理? 加密的 List/Map 属性怎么解密?

### 1. 双解密路径 — 绑定期钩子 vs 环境期后处理

场景: {cipher} 前缀在哪被识别? 为什么有两条路径?
源码路径:
- **路径 A: 绑定期** — TextEncryptorBindHandler (bootstrap/TextEncryptorBindHandler.java:36-84): **AbstractBindHandler.onSuccess 钩子** (L55-56) — @ConfigurationProperties 绑定成功时, 值以 {cipher} 开头 → decrypt (L55-60)
- **⚠ 绑定期注册机制 (Boot 3 新架构)**: TextEncryptorBindHandler 经 **BootstrapRegistry 注册为 BindHandler** (TextEncryptorConfigBootstrapper, BootstrapRegistryInitializer L21): TextEncryptor 注册 (TextEncryptorUtils.register L85-97, RSA/对称/Failsafe 三选一 L80-87) + **BindHandler 注册** (L90-97); promote 到 beanFactory 带 **isLegacyBootstrap 检查** (L48-52, legacy bootstrap 环境跳过 — SCC-1 双轨制联动)
- **路径 B: 环境期** — AbstractEnvironmentDecrypt.decrypt (bootstrap/encrypt/AbstractEnvironmentDecrypt.java:67-105): 遍历 EnumerablePropertySource → 逐属性检查 {cipher} → 产出 **decrypted 源** (L44 DECRYPTED_PROPERTY_SOURCE_NAME); EnvironmentDecryptApplicationInitializer 把它加进 Environment (L86-90)
- ENCRYPTED_PROPERTY_PREFIX = "{cipher}" (AbstractEnvironmentDecrypt.java:49 / TextEncryptorBindHandler.java:43)
- 分工: **绑定期服务 @ConfigurationProperties, 环境期服务任意 Environment 读取** (含 bootstrap 拉取的远程配置)
关键设计 (q1): **两条解密路径覆盖两种消费方式** — @ConfigurationProperties 用 Binder (绑定期钩子), 直接 environment.getProperty() 用环境期后处理; 前缀识别两处同构 ({cipher} 常量); 绑定期解密在"值进入 Bean"时, 环境期解密在"值进入 Environment"时。 [模式: 双路径同构]

### 2. TextEncryptorBindHandler — Binder 钩子的 failOnError

场景: 绑定 {cipher} 值时, 失败怎么办?
源码路径:
- onSuccess (L55-60): result 是 String 且以 {cipher} 开头 → decrypt(name, result)
- decrypt (L62-84): 剥前缀 (L63) → textEncryptor.decrypt → **成功返回明文** / 失败 → **keyProperties.isFailOnError() 决定** — true 抛 IllegalStateException (L79-81) / false 返回 "" (L82)
- 持有: TextEncryptor + KeyProperties 双注入 (L39-40)
关键设计 (q3): **failOnError 是"配置事故"的开关** — true = 密钥错直接启动失败 (快速暴露), false = 空串静默 (容忍); 与 AbstractEnvironmentDecrypt 的 failOnError **同构但独立字段** (KeyProperties vs AbstractEnvironmentDecrypt 字段)。 [模式: 失败策略开关]

### 3. TextEncryptor 装配 — 双条件 RSA vs 普通

场景: 加密器怎么选? RSA 还是对称?
源码路径:
- **⚠ 装配双轨**: **新路径** — TextEncryptorConfigBootstrapper (BootstrapRegistryInitializer, Boot 3) 经 TextEncryptorUtils.register (L77) 注册 TextEncryptor/BindHandler 到 BootstrapRegistry (§1 已述); **旧路径** — EncryptionBootstrapConfiguration (L48, @ConditionalOnClass(TextEncryptor)) 传统 @Bean; 两轨共用 TextEncryptorUtils (createTextEncryptor/register 桥梁)
- EncryptionBootstrapConfiguration (encrypt/EncryptionBootstrapConfiguration.java:48): @ConditionalOnClass(TextEncryptor) + KeyProperties + **FailsafeTextEncryptor 兜底** (L64, 无 TextEncryptor Bean 时)
- **RsaEncryptionConfiguration** (L80-95): @Conditional(KeyCondition) + @ConditionalOnClass(RsaSecretEncryptor + ASN1Sequence) → **RSA 加密器** (TextEncryptorUtils.createTextEncryptor)
- **VanillaEncryptionConfiguration** (L99-110): KeyCondition + **@ConditionalOnMissingClass(ASN1Sequence)** (无 bouncycastle) → **普通加密器** (EncryptorFactory + key.getSalt())
- 密钥面: KeyProperties (key/salt/failOnError) + RsaProperties (RSA 密钥)
关键设计 (q2): **加密器选择 = 类存在性 + KeyCondition** — 有 RSA 库用 RSA (非对称), 无则对称 (EncryptorFactory 哈希派生); KeyCondition 检查 key 配置存在; Failsafe 兜底防"无加密器启动崩"。 [模式: 双条件装配]

### 4. AbstractEnvironmentDecrypt — 环境期解密的"索引属性"特判

场景: 加密的 `foo[0]=xxx` 怎么处理? 为什么有 COLLECTION_PROPERTY?
源码路径:
- **COLLECTION_PROPERTY 正则** (L33): `(\\S+)?\\[(\\d+)\\](\\.\\S+)?` — 匹配索引属性 foo[0] / list[0].name
- decrypt 主循环 (L67-105): EnumerablePropertySource 遍历 → **PropertyVisitor 去重** (L74, 同属性名多源只解一次) — **去重存储用 SystemEnvironmentPropertySource** (L172-177, 注释明示"覆盖 relaxed-binding 场景" — DB_URL 与 db.url 视为同属性) → 索引属性走 **getPropertyValues** (L141-165, `matchingName + "["` 前缀精确匹配 L144, 防 fooBar[0] 误匹配) → 非索引走 getPropertyValue
- **只收集解密过的值** (L80/L86): 无 {cipher} 的属性不进 decrypted 源
- 产出: decrypted 源 (AbstractEnvironmentDecrypt.java:44 DECRYPTED_PROPERTY_SOURCE_NAME="decrypted") → SCC-1 insertPropertySources 特判 addAfter (SCC-1 已讲)
关键设计 (q4): **索引属性的"整族处理"** — foo[0]/foo[1] 作为一族检查, 一族任一解密则全族入 decrypted 源; "[" 前缀匹配防同前缀误伤; PropertyVisitor 跨源去重防重复解密。 [模式: 族处理 + 去重]

### 5. EnvironmentDecryptApplicationInitializer — 解密初始化器与防重复

场景: 解密在什么时机执行? 会重复解密吗?
源码路径:
- EnvironmentDecryptApplicationInitializer (encrypt/EnvironmentDecryptApplicationInitializer.java:45-97): extends AbstractEnvironmentDecrypt — 初始化器
- **DECRYPTED_BOOTSTRAP_PROPERTY_SOURCE_NAME = "decryptedBootstrap"** (L51) — bootstrap 解密源
- **"No reason to decrypt bootstrap twice"** (L80-81): 已含 decryptedBootstrap 源 → 跳过 — 防重复 (SCC-1 已提)
- **双场景** (L78-98): ① bootstrap 源解密 (L80-90, **防重复守卫 + decryptedBootstrap 源**) ② **removeDecryptedProperties 先清旧解密源** (L93) ③ 主环境解密 → decrypted 源 (L95-98)
- 装配: EncryptionBootstrapConfiguration L61-64 (无 TextEncryptor Bean 用 Failsafe)
关键设计 (q2): **解密是"Environment 准备期的一次性后处理"** — 产出解密源后原密文源仍在 (双源并存), 解密源优先级更高 (SCC-1 insertPropertySources addAfter); 防重复守卫保证只解一次。 [模式: 一次性后处理]

### 6. TextEncryptorUtils 与 EncryptorFactory — 加密器工厂面

场景: 加密器具体怎么构造? 密钥还没就绪怎么办?
源码路径:
- TextEncryptorUtils (encrypt/TextEncryptorUtils.java:41-...): decrypt 静态工具 + createTextEncryptor (RSA) + **FailsafeTextEncryptor**
- **FailsafeTextEncryptor delegate 模式** (L57-70): 初始无密钥时创建 Failsafe → **后续 setDelegate 替换真实加密器** (Javadoc 明示: 密钥可能在 application.properties, bootstrap 阶段不可得; delegate 允许延迟设置)
- EncryptorFactory (context/encrypt/EncryptorFactory.java): 对称加密器工厂 — key.getSalt() 派生
- KeyProperties: key/salt/failOnError 三字段 (bootstrap/encrypt/KeyProperties.java)
关键设计 (q3): **Failsafe 是"占位 + 延迟替换"** — bootstrap 阶段密钥未就绪时 Failsafe 兜底 (解密返回原值不崩), 密钥就绪后 setDelegate 换成真实加密器; 这是"两阶段密钥可用性"的优雅解 (bootstrap 早于 application.properties 加载)。 [模式: 占位委托]

## 代码类型
Architecture (安全面) + Security (加密/解密)

## 负面空间 — 配置加密刻意不做的事

- **不做加密器内建算法**: TextEncryptor 用 Spring Security Crypto (RSA/对称), 无自研算法
- **不做密钥轮换**: 密钥静态配置 (KeyProperties), 无自动轮换 (对比 KMS)
- **不做加密属性索引**: 解密是线性扫描所有属性, 无索引加速
- **不做部分解密**: {cipher} 只支持整值, 不支持 "abc{cipher}x" 混合
- **不做解密结果持久化**: decrypted 源内存态, 重启重新解密
- **不做审计日志**: 解密事件无专门审计 (仅 debug 日志)

→ 引出: 配置面怎么被各客户端隔离消费? → SCC-13 NamedContextFactory
