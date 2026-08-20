# R-18 内存碎片 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 4.0 (2018) | **active defrag 引入** — redis.conf L2242 实证: "implemented by Oran Agra for Redis 4.0"; je_get_defrag_hint jemalloc 补丁同期; 4.0 发布稿 (2018-07): 在线碎片整理为头条特性之一 |
| 2020 | defrag.c 版权 **2020-Present** (L8) — 文件重组/RSAL 许可切换 (2020-03 Redis 6.0 RSALv2) |
| 6.0 | **active-defrag-max-scan-fields 引入** (大键延后, 推断) — 配置位于 6.0 配置族重构区; moduleDefragValue/RegisterDefragFunc (模块搬移 API) 6.0 同期 (模块 6.0 重大面) |
| 7.x (kvstore 时代) | kvstoreDictLUTDefrag + kvstoreDictScanDefrag (四阶段扫描适配 kvstore, L1158-1159); defragStage 数组抽象 (L1167-1179); whileBlockedCron 循环补齐 (L1586); dictDefragBucket 三态重构 (storedKey — R-3 单指针 entry 时代) |
| 7.4 (HFE 时代) | **ebDefragItem / hashTypeUpdateKeyRef / hashTypeGetMinExpire** (L750-758, L278-279) — HFE 时间桶引用同步; LISTPACK_EX 双搬 (L804-809); activeDefragHfieldDict (hfield 搬移 + ebucket) |

## 痕迹证据

- defrag.c:1-13: 模块自述 "scanning the keyspace and for each pointer we have, we can try to ask the allocator if moving it to a new address will help reduce fragmentation"
- defrag.c:30-32: je_get_defrag_hint 补丁注释
- defrag.c:46-48: no_tcache 动机注释 ("so that we don't get back the same pointers we try to free")
- defrag.c:383-385: 大键延后动机注释 ("prevent latency spikes")
- defrag.c:854-857: 小 bin 占比公式动机注释 ("if most of the memory usage is large bins, we may show high percentage")
- defrag.c:1036-1038: 只升不降动机注释
- defrag.c:1089-1090: fork 暂停注释 ("Defragging memory while there's a fork will just do damage")
- defrag.c:1108: "See activeExpireCycle for how timelimit is handled" — 与 R-22 同款预算模式
- defrag.c:1222-1223: defragOtherGlobals 同周期完成注释
- server.c:1578-1583: whileBlockedCron 多轮注释 (25% CPU 预算补齐)
- redis.conf:2242: Oran Agra / Redis 4.0 权威版本记录

## 推断标注

- "max-scan-fields 6.0" — 配置族位置推断, 无 release note 实证 (标注)
- "moduleDefragValue 6.0" — 模块 API 大版本推断 (标注)
- "dictDefragBucket 三态重构 7.x" — storedKey 与 kvstore 同代推断 (标注)
- 仓库浅克隆 (单 commit) 无法 git 考古 — 版本线依赖 redis.conf/版权/注释, 已逐条标注实证级别
