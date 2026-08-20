# ALI-A1 Nacos Config 配置加载 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. Nacos Config 为什么需要 ConfigData 新轨 + Bootstrap 旧轨双机制? 各自解决什么时代的问题?
2. `nacos:dataId?group=...&refreshEnabled=...&preference=...` 的 URI 是怎么被拆成五元组的? dataId 为什么只能一段?
3. locate() 的三级递进 (默认→后缀→profile) 为什么能保证 profile 配置优先级最高?
4. NacosPropertySourceRepository 为什么用 ConcurrentHashMap 存 PropertySource? dataId+group 复合 key 解决什么?
5. NacosSnapshotConfigManager 的"读后即删"语义是什么? 为什么不是常驻缓存?

## B. 源码实证 (5)

6. NacosConfigDataLocationResolver 的 PREFIX 和 getOrder() 分别是什么? (grep L62/L75-77)
7. loadProperties 为什么优先从 bootstrapContext 取 NacosConfigDataLoadProperties? (grep L85-105)
8. NacosConfigManager.getInstance 的双检锁在哪几行? ConfigService 为什么是 static? (grep L35/L49-60)
9. loadNacosPropertySource 的刷新节流条件是什么? (grep L164-169)
10. NacosConfigProperties 的 timeout/fileExtension/group 默认值各是多少? (grep L117/125/129)

## C. 推理深挖 (5)

11. 如果 `spring.config.import=nacos:` 配了但 Nacos 不可达且 optional=false, 会发生什么?
12. preference=REMOTE 时 getOptions 为什么加 PROFILE_SPECIFIC? 它怎么改变加载顺序?
13. 快照容量上限 100 的实现为什么"理论上永不触发"? 触发时丢哪个?
14. ConfigData 轨和 Bootstrap 轨同时启用时, 同一 dataId 会被加载两次吗? 谁保证幂等?
15. NacosConfigDataMissingEnvironmentPostProcessor 的 ORDER 为什么是 ConfigDataEnvironmentPostProcessor.ORDER+1000?

## D. 跨域扩展 (5)

16. NacosPropertySourceLocator vs Commons SCC-1 的 PropertySourceLocator SPI: 谁定义契约谁实现?
17. ConfigData 轨的 ConfigDataResource vs NacosPropertySource: 两种数据载体差异?
18. 本域的快照容灾 vs Nacos 5.8 的本地快照 (LocalSnapshot/FailoverReactor) 谁更底层?
19. 刷新节流 (refreshCount != 0 && !isRefreshable) 与 ALI-A2 的动态刷新是什么关系?
20. 双轨制 vs Commons SCC-1 的 bootstrap 双轨 (bootstrapEnabled/useLegacyProcessing): 机制怎么呼应?
