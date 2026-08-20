# SCC-9 配置加密 — 知识规划 (KP)

> 域: SCC-9 | 级别: 🟡 | 方案: B | 大纲: outlines/scc9-encryption/outline.md (6 节)

## §01 域定位

配置加密 = {cipher} 前缀的双路径解密。绑定期 (TextEncryptorBindHandler Binder 钩子) + 环境期 (AbstractEnvironmentDecrypt 后处理) + 双条件加密器装配 (RSA/对称) + Failsafe delegate 占位。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| bootstrap/TextEncryptorBindHandler.java | 86 | Binder 钩子解密 + failOnError | 1,2 |
| bootstrap/encrypt/AbstractEnvironmentDecrypt.java | 196 | 环境期解密 + COLLECTION_PROPERTY + 去重 | 1,4 |
| bootstrap/encrypt/EncryptionBootstrapConfiguration.java | 147 | 加密器双条件装配 | 3 |
| bootstrap/encrypt/EnvironmentDecryptApplicationInitializer.java | — | 解密初始化器 + decryptedBootstrap 源 | 5 |
| bootstrap/encrypt/TextEncryptorUtils.java | — | RSA 工厂 + Failsafe delegate | 6 |
| context/encrypt/EncryptorFactory.java | — | 对称加密器工厂 | 6 |
| bootstrap/encrypt/KeyProperties + RsaProperties | — | 密钥配置面 | 3,6 |

## §05 闭环要点 (Pass 2 内化)

### q1 双解密路径
绑定期 (Binder onSuccess) 服务 @ConfigurationProperties / 环境期 (decrypt 后处理) 服务任意 Environment 读取; {cipher} 前缀两处同构。

### q2 TextEncryptor 装配
RsaEncryptionConfiguration (KeyCondition + RsaSecretEncryptor 类) vs Vanilla (无 bouncycastle); Failsafe 兜底。

### q3 failOnError 语义
两处同构独立字段: true 抛 IllegalStateException / false 返回 ""。

### q4 索引属性
COLLECTION_PROPERTY 正则 + "[" 前缀精确匹配 + PropertyVisitor 去重 + 一族处理。

## §06 负面空间 (6 条)

不做算法内建 / 不做密钥轮换 / 不做加密属性索引 / 不做部分解密 / 不做结果持久化 / 不做审计日志

## §07 交叉引用

- ← SCC-1 Bootstrap (bootstrap 阶段解密, insertPropertySources addAfter) + SCC-8 刷新重跑 decrypt
- → SCC-13 NamedContextFactory (配置面收束)
- 另见: Jasypt / Spring Security Crypto / Nacos 配置解密
