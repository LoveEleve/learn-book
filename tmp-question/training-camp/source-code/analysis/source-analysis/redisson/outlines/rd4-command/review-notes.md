# RD-4 命令执行流水线 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 二次 REVIEW (07 换维度: 结构/事实/锚点/补漏/跨域)

| 轮 | 维度 | 发现 | 修复 |
|:--:|:--|:--|:--|
| R1 | 结构 (反写) | 三篇全含 概念依赖链/核心悬念/叙事顺序 ✅ | 通过 |
| R2 | 事实核验 | ① blocking 命令重试路径 (L288-303) 大纲正确 ✅ ② EVALSHA_RO 递归传 mappedScript (非原始) 未写 | 篇2-S3 补 "递归传 mappedScript 避免二次映射" |
| R3 | **锚点密度 (第三次复发)** | 裸行号 12 处 (L278-375/L609-649 等); file:line 仅 01=3/02=1/03=3 (🔴需≥8) | **全部补文件名 → 01=8/02=9/03=9** ✅ (与 RD-1/RD-3 同型 — 已列为最高优先级铁律) |
| R4 | 完整补漏 | 同步 API get() 包装未展开 (RFuture→blocking) | 篇1 线程模型节补 get()/getInterrupted() 双入口 |
| R5 | **跨域违规** | `[[r16-transaction]]` 引用未产出域 (Redis 无 r16) | 改 `[[r20-server]]` (服务端命令处理对照, 真实存在) |

> ⚠️ R3/R5 是两次复发: 裸行号在 RD-1→RD-3→RD-4 三次出现; 引用未产出域在 REDISSON-PLAN 修过后 RD-4 又犯。**已写入手册级铁律**: ①写大纲时行号直接带文件 ②引用前 must `[ -d ]` 核验目标。下次 (RD-2) 必须先自查再呈现。

## 六层深审 (Pass 0-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **重点机制 (eval NOSCRIPT 自愈)** | evalAsync 双错误降级 (EVALSHA_RO unknown + NOSCRIPT auto-load) — 脚本缓存失效自动恢复是 RD-4 高价值机制 | 篇3-S1 完整展开 |
| 2 | 命令适配 | resp3 查表 (ServiceManager:689) vs async 内 SORT_RO 降级 — 两层协议适配 | 篇2 拆 3 节 |
| 3 | SORT_RO SUPPORTED static 语义 | static final AtomicBoolean 进程级缓存 — 一次失败永降 (无法感知升级) | 篇2 负面空间标注 |
| 4 | 缓存失效钩子 | async() 只对 String param + write 触发 — 条件式低成本 | 篇3-S3 |
| 5 | noRetry 语义 | evalWriteNoRetryAsync — 续期幂等禁重试防放大 | 篇3-S1 + RD-2 衔接 |
| 6 | completeness ❌ 回填 | Q3 线程模型 (回调线程) 未覆盖 | 篇1 新增 "4. 线程模型" 节 |
| 7 | 批/事务边界 | batch=pipeline 非 transaction 需明示 | 篇3-S2 + 负面空间 |
| 8 | 通过项 | 全锚点行号 awk 验证: CommandAsyncService 690-731/578-659/717-727 / RedisExecutor 122-230/278-375/232-276 / ServiceManager 686-705/713 / CommandBatchService 53-327 / NodeSource 28-51 ✅ | 记录 |

## 07 五维度

### 维度1 功能正确性
- async 汇聚三重装配 (resp3/SORT_RO/evict) 实证
- 重试分型: 连接/写可重试, 响应不重试 — 防重复副作用
- Lua 双自愈 (EVALSHA_RO 降级 + NOSCRIPT reload) 闭环

### 维度2 性能
- 三定时器共用 responseTimeout (3s), 排障文案带指标
- countPendingTasks 量化 EventLoop 积压
- EVALSHA 省脚本字节 vs NOSCRIPT 探测成本

### 维度3 内存
- keysCopy/paramsCopy 对称 free (evalAsync L619-648) — 防泄漏
- free(params) 每参数 ReferenceCountUtil.safeRelease (RedisExecutor:381-385) — ByteBuf 安全

### 维度4 一致性/并发
- static AtomicBoolean 能力缓存 — 进程级一致
- evictClientSideCaching 写完成回调 — 就近失效
- CompletableFuture 链 + EventLoop 线程模型 (补节)

### 维度5 边界/安全
- attempts 有界 (默认 4) 防雪崩
- noRetry 幂等契约
- shutdown 快速失败 RedissonShutdownException
- blocking 命令取消 forceFastReconnectAsync

## 完成状态

- [x] Pass 0-3: command/ 12 文件 + 8 闭环 + 知识规划 + 3 篇大纲 + harness 9/9
- [x] 二次 REVIEW (R1-R5) 修复 4 项 (fact/锚点/补漏/跨域)
- [x] completeness 50 问 + ❌ 1 回填
- [x] 铁律更新: 裸行号三次复发 → 大纲自查优先; 引用核验 must ls