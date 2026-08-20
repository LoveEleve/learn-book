# 闭环笔记 q2: 注册 — 版本兼容 + 冲突仲裁

## 假设
注册 = 幂等 upsert; 同地址冲突/版本冲突有仲裁。

## 验证过程
- **入口校验** (DefaultRequestProcessor:223-281): **crc32 body 校验** (bodyCrc32 ≠ 0 时, L331-342) + **版本分支**: V3_0_11+ → RegisterBrokerBody (压缩 topic 配置 + filterServer 列表); 老版本 → 仅 TopicConfig (L239-247)
- **注册主流程** (RouteInfoManager:226-409):
  1. clusterAddrTable 补 brokerName (L243-244)
  2. registerFirst 判定 (L246-253)
  3. enableActingMaster 兼容 (isOldVersionBroker = enableActingMaster==null, L255-257)
  4. **minBrokerId 变化检测** (L261-269)
  5. **同 IP:PORT 去重**: 主从切换时同地址只保留一条 (L273)
  6. **stateVersion 仲裁**: 旧记录 stateVersion > 新 → 拒绝注册 + 清 brokerLiveTable (防僵尸复活顶掉新主, L276-292)
  7. 空 topic 配置+已注册校验 (L294-298)
  8. brokerAddrsMap.put (L300)
  9. **isPrimeSlave 判定**: 非旧版 + 非主 + brokerId == 最小 → topic 表更新时**擦写权限** (L305-307,343-347)
  10. topic 表增量: registerFirst 或 DataVersion 变化 (L338-350) + deleteTopicWithBrokerRegistration 删消失 topic (L320-336)
  11. **topicQueueMappingInfoTable 更新** (L352-362)
  12. brokerLiveTable upsert (心跳超时 = 请求头 heartbeatTimeoutMillis, 默认 2min, L366-373)
  13. filterServerTable (L378-384)
  14. 从库注册 → 返回 master 地址 (L386-396)
  15. minBrokerId 变化 → notify (L398-401)
- **从库响应**: haServerAddr + masterAddr (DefaultRequestProcessor:270-271)

## 代码类型
Implementation (幂等 upsert + 仲裁)

## 跨域关联
- RM-5 (Broker): registerBrokerAll 发起 (10s 初/[10s,60s])
- RM-1 (协议): 103 请求码 / extFields zone
- RM-12 (HA): stateVersion/acting master/controller 协同 (交叉)

## 结论
注册 = 六表 upsert + 四类仲裁 (地址去重/stateVersion/prime slave 权限/DataVersion 增量); 心跳超时随注册请求携带。
源码位置: DefaultRequestProcessor.java:223-281,331-342; RouteInfoManager.java:226-409
