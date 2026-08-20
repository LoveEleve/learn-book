# SCC-9 配置加密 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. 双解密路径 (绑定期/环境期) 的分工? 各自服务谁?
2. failOnError 在两处 (BindHandler/EnvironmentDecrypt) 的字段来源差异?
3. FailsafeTextEncryptor 的 delegate 模式解决什么问题?
4. COLLECTION_PROPERTY 正则为什么用 "[" 前缀匹配防误伤?
5. decrypted 源与密文源并存时, 优先级怎么定?

## B. 源码实证 (5)

6. TextEncryptorBindHandler.onSuccess 的判断条件? (grep L55-60)
7. AbstractEnvironmentDecrypt 的 PropertyVisitor 去重逻辑? (grep L62)
8. RsaEncryptionConfiguration 和 Vanilla 的装配差异? (grep L80-110)
9. EnvironmentDecryptApplicationInitializer 的防重复守卫? (grep L80-81)
10. FailsafeTextEncryptor 的 delegate 怎么设置? (grep TextEncryptorUtils:57-70)

## C. 推理深挖 (5)

11. 密钥在 application.properties 而 bootstrap 阶段不可得 — 为什么 Failsafe 是优雅解?
12. 索引属性 foo[0] 一族解密 — 为什么"一族任一解密则全族入源"?
13. {cipher} 值在绑定期被 BindHandler 解了, 环境期的源里还是密文 — 两个值并存矛盾吗?
14. failOnError=false 返回 "" — 业务侧怎么发现配置错了?
15. 为什么解密是"线性扫描所有属性"不做索引? (负面空间)

## D. 跨域扩展 (5)

16. {cipher} vs Jasypt 的 ENC() 前缀设计对照?
17. Failsafe delegate vs Spring Security Crypto 的延迟初始化模式?
18. 解密源 addAfter(decrypted) (SCC-1) vs Nacos 配置解密的时机?
19. 双路径解密 vs 统一后处理 (只做环境期) 的设计取舍?
20. 如果做密钥轮换, 应该改哪层? (KeyProperties vs TextEncryptor 装配)
