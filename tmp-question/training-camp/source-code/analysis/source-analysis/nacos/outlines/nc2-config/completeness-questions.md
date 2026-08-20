# NC-2 ConfigService 配置客户端 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. 三路读取的优先级与各自适用场景? failover 为什么是"用户维护"?
2. 每路都过 configFilterChainManager 的意义? 不对称 (get 过滤 / publish 加密)?
3. COW Map + 双检的组合解决什么并发问题?
4. listenExecutebell 信号量怎么驱动长轮询? 与"轮询"的本质区别?
5. 两种监听器 (Listener vs AbstractConfigChangeListener) 的回调差异?

## B. 源码实证 (6)

6. getConfigInner 的 NO_RIGHT 异常处理? (grep NacosConfigService:242-244)
7. addCacheDataIfAbsent 的双检在哪? (grep ClientWorker:381-402)
8. ALL_SYNC_INTERNAL 的值与语义? (grep ClientWorker:648-650)
9. 通知任务的 ClassLoader 切换逻辑? (grep CacheData:444-448)
10. ConfigChangeHandler 的解析器来源? (grep L51-53)
11. getServerStatus 的实现? (grep NacosConfigService:293-299)

## C. 推理深挖 (5)

12. failover 文件在什么场景下由谁创建? 服务端恢复后 failover 还优先吗?
13. listener 的 getExecutor 异步与内部线程同步: 阻塞监控的告警语义?
14. 服务端推送空内容 (配置删除) 时, checkListenerMd5 的判定?
15. 加密配置 (LocalEncryptedDataKeyProcessor) 在三条读取路的地位?
16. shutdown 后 cacheMap 标记 consistentWithServer=false 的意义?

## D. 跨域扩展 (4)

17. getConfigInner 三路 vs ALI-A1 的 Locator 三轨: 客户端容灾 vs 集成层加载?
18. ConfigChangeEvent vs ALI-A2 的 NacosConfigRefreshEvent: 变更事件的粒度差异?
19. 本域监听链 vs SCC-8 RefreshEventListener: 通知后谁触发刷新?
20. fuzzyWatch vs ALI-A2 的 @NacosConfigListener (精确 dataId): 模糊/精确两面的互补?
