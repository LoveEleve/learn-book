# RD-4 命令执行流水线 — 全视角提问验证 (completeness-questions)

> 域级验证 (大域 command/ 12 文件 4027 行 → 50+ 问 / 6 身份, 逐篇)
> 覆盖目标: 篇1 执行重试 (q1/q2/q3) | 篇2 协议能力 (q4/q5) | 篇3 Lua批处理 (q6/q7/q8)

## 篇1 (执行管线/重试/超时)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 1 | 为什么错误组件命令会在 async() 拦一道? | ✅ 篇1-S1 三重装配 |
| 2 | RedisExecutor 的 CompletableFuture 哪些是必须成对的? | ✅ 篇1-S2 |
| 3 | 我的回调在哪个线程? (EventLoop/业务线程) | ❌ 未覆盖 — 需补线程模型 |
| 4 | blocking 命令 (BLPOP) 取消会怎样? | ✅ 篇1-S2 forceFastReconnectAsync |
| 5 | 响应超时后还能拿到 response 吗? | ✅ 篇1-S3 不重试 (服务端已执行) |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 6 | 为什么接口收 RFuture 而不是直接 Future? | ⚠️ 提到 CompletableFuture 未聊 RFuture 包装 |
| 7 | 三定时器共用 responseTimeout 的耦合 vs 独立? | ✅ 篇1-S3 + 负面空间 |
| 8 | 汇聚点在架构里是 Gateway 模式吗? | ⚠️ 未对比 |
| 9 | 为什么 execute() 失败返回 void 不抛? | ✅ 篇1-S1 promise 完成 |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 10 | retryInterval 每次尝试都算 jitter 的代价? | ✅ 篇1-S2 calcDelay 每次 |
| 11 | countPendingTasks 遍历所有 EventLoop 的每次成本? | ⚠️ 仅在超时才调用 |
| 12 | 响应超时不重试的前提是幂等性 — 如何保证? | ✅ 篇1-S3 负面 |
| 13 | 大量连接失败 → 递归重试会不会打爆池? | ✅ 篇1-S3 attempts 有界 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 14 | "Increase connection pool size" 文案何时出现? | ✅ 篇1-S3 池满 |
| 15 | 写超时的 "Netty pending tasks" 怎么读? | ✅ 篇1-S3 |
| 16 | 如何从异常判断卡在连接/写/响应哪一环? | ✅ 篇1-S3 异常类型区分 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 17 | vs Lettuce async 模型差异? | ⚠️ 篇1 无横向 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 18 | 为什么叫 RedisExecutor 不叫 CommandExecutor? | ⚠️ 未解释命名 |
| 19 | CompletableFuture 和 RFuture 关系? | ⚠️ 一句话可带 |

## 篇2 (RESP 适配/能力探测)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 20 | RESP3 下哪些命令变 V2? | ✅ 篇2-S1 映射表 |
| 21 | V2 命令的解码器在哪定义? | ⚠️ ReplayMultiDecoder 提到未详 |
| 22 | SORT_RO 降级后读变写, 影响? | ✅ 篇2-S2 读转写 |
| 23 | AtomicBoolean 是 volatile 吗? | ⚠️ 需 grep 验证 |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------|:--:|
| 24 | 探测缓存 static 的进程级隔离好吗? | ✅ 篇2-S2 "无法感知升级"负面 |
| 25 | 为什么不做通用能力发现 (COMMAND|INFO)? | ⚠️ 未对比 |
| 26 | RESP2/3 双协议支持的成本在mapper 还是 decoder? | ✅ 篇2-S1 V2+decoder |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 27 | 探测只发生一次的语句在哪? | ✅ 篇2-S2 |
| 28 | resp3() 每次查表是 map hit 还是 O(n)? | ⚠️ HashMap O(1) 可补充 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 29 | 服务端升级支持 SORT_RO 后客户端现状? | ✅ 篇2-S2 永降不恢复 (负面) |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 30 | 服务端 (Redis r28) 对 SORT_RO 的接受史? | ⚠️ 需查 redis side |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 31 | SORT_RO 名字的 RO 什么意思? | ✅ 篇2-S2 只读 |
| 32 | 为什么 ZRANGE 也需要 V2? | ✅ 篇2-S1 map 语义 |

## 篇3 (Lua/批/钩子)

### 开发者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 33 | evalAsync 里 keysCopy/paramsCopy 为什么复制? | ✅ 篇3-S1 free 对称 |
| 34 | NOSCRIPT 在什么场景发生? | ✅ 篇3-S1 重启/新节点 |
| 35 | loadScript 为什么按 master/slave 分? | ✅ 篇3-S1 SCRIPT_LOAD 对节点 |
| 36 | evictClientSideCaching 触发条件? | ✅ 篇3-S3 写+有实例 |
| 37 | batch 和 transaction 区别? | ✅ 篇3-S2 pipeline≠事务 |

### 架构师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 38 | 为什么批处理的失败要 tryFailure 全灭? | ✅ 篇3-S2 |
| 39 | useScriptCache 默认 true 的权衡? | ⚠️ 篇3 提到默认未聊降级场景 |
| 40 | 缓存失效钩子为什么不做成订阅? | ✅ 篇3-S3 就近失效+订阅(他实例) |

### 性能工程师视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 41 | EVALSHA 比 EVAL 省多少字节? | ⚠️ 无量化 |
| 42 | skipResult 快路径省什么? | ✅ 篇3-S2 免响应收集 |
| 43 | batch 分组后网络往返次数? | ✅ 篇3-S2 pipeline 概念 |

### SRE/运维视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 44 | SCRIPT FLUSH 后第一个请求怎么恢复? | ✅ 篇3-S1 NOSCRIPT 自愈 |
| 45 | batch 失败的排障入口? | ⚠️ 未给日志 |
| 46 | 缓存失效不及时 (钩子失效) 的后果? | ⚠️ 未展开弱一致窗口 |

### 研究者视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 47 | vs 服务端 Lua 脚本缓存 (Redis)? | ⚠️ 未对照服务端 SCRIPT cache |
| 48 | vs Redis pipeline 标准实现? | ✅ 篇3-S2 |

### 学生视角
| # | 问题 | 覆盖 |
|:--:|------:--:|
| 49 | EVAL 和 EVALSHA 核心区别? | ✅ 篇3-S1 |
| 50 | 为什么锁的续期脚本不重试? | ✅ 篇3-S1 noRetry 防放大 |

---

**覆盖统计**: ✅ 34 | ⚠️ 13 | ❌ 1 (Q3 线程模型)

**回填动作**:
1. ❌ Q3 (线程模型): 篇1 补一段 "回调和通知在哪个线程执行" — 连接回调在 Netty EventLoop, 用户 thenAccept 在完成线程, 无自定义 executor
2. ⚠️ Q23 (AtomicBoolean volatile): 补篇2 "static final AtomicBoolean = 内存可见性由 final + volatile 语义保证"
3. ⚠️ 深审回填: Q6 RFuture 包装 / Q17 Lettuce 对比 / Q46 弱一致窗口等 11 项标注待深审