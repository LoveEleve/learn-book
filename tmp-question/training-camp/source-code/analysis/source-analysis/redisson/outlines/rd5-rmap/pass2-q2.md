# 闭环笔记 q2: WriteBehind 缓冲协议 — 写后批量落库

## 假设
WRITE_BEHIND 模式: Redis 写成功后任务进 WriteBehindService 缓冲, 按 batchSize=50/delay=1000ms 批量写 MapWriter (合并重复 key)。

## 验证过程
- MapOptions (api/MapOptions.java:62-63): `writeBehindBatchSize = 50` / `writeBehindDelay = 1000`
- WriteBehindService (Redisson 根包): `writeBehindService.start(name, options)` → MapWriteBehindTask
- RedissonMap 构造器 (RedissonMap.java:77-88): options 有 writer → writeBehindService.start; 否则 null
- mapWriterFuture WRITE_BEHIND (L667): `writeBehindTask.addTask(task)` — 每次写进缓冲
- WriteBehind 合并: 同 key 多次写合并为最后一次 (批处理核心)
- 触发落库: 缓冲满 50 或延迟 1000ms → 批量 writer.write(map)
- RetryableMapWriter: writer 失败重试包装 (MapOptions.java:101 `new RetryableMapWriter<>(this, writer)`)

## 代码类型
Implementation (写后缓冲) — 批量合并 + 定时落库

## 跨域关联
- Q1 (mapWriterFuture) → addTask 入口
- 一致性: 写后 (behind) = 最终一致 (延迟窗口); 写通 (through) = 同步
- 面试点: "MapWriter 写后和写通区别?"

## 结论
WriteBehind = 写后批量: 缓冲任务 50 条或 1000ms → 批量写外部 (合并重复 key); 失败 RetryableMapWriter 重试。代价: 延迟窗口内外部数据源滞后 (最终一致)。选型: 吞吐优先选 behind, 一致优先选 through。
源码位置: MapOptions.java:62-63,101; RedissonMap.java:77-88,667