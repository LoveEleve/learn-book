# R-30 Lua+Functions — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **编造修正** | 大纲 q1/q3 "7 个 SCRIPT_FLAG (+ NO_REPLICATE 等)" — **错误**: scripts_flags_def 仅 **5 个 shebang 可写标志** (script.c:16-22: no-writes/allow-oom/allow-stale/no-cluster/allow-cross-slot-keys); script.h:60-65 宏 6 个 (EVAL_COMPAT_MODE 不可写, 无 shebang 默认); **无 NO_REPLICATE** | 大纲 §1 + pass2-q1 修正 |
| 2 | **机制精确化** | **os 库 = 精简到仅 os.clock**: Redis 补丁 vendored loslib.c (deps/lua/src/loslib.c:240-243 sandbox_syslib "Only a subset is loaded currently, for sandboxing concerns"); 测试实证 os.execute/remove/rename 报 "attempt to call field" (scripting.tcl:675-679); 非"白名单受限"而是"补丁裁剪" | 大纲 §2 + pass2-q2 修正 (HANDOFF "无 io/os 库" 最终精确: io 无 + os 仅 clock) |
| 3 | **表述精确化** | EVALSHA→EVAL 传播重写: eval.c:468-471 注释保留 2.6-3.2 全量传播语义; **7.0 effects 时代 body 缓存主要 = EVALSHA 可用性 + 旧版兼容** (当前传播=命令级 effects); 原大纲表述"body 缓存是传播前提"过度 | 大纲 §1 重写 |
| 4 | **补充锚点** | **redis.set_repl() 仍有效** (script.c:536-544 scriptSetRepl — 3.2 API 保留, 运行时改 repl_flags); replicate_commands 空操作实证 (scripting.tcl:1396-1404 "replicate_commands is the default on Redis Function / can be issued anywhere now") | 大纲 §3 补注 |
| 5 | 验证 | luaCallFunction 钩子 LUA_MASKCOUNT **100000 指令** (script_lua.c:1613-1615) / KEYS+ARGV 注入临时解只读锁 (L1619-1632) / LRU_LIST_LENGTH=500 (eval.c:527) / 仅 EVAL 淘汰 (L530-531) — 全命中 | 记录 |
| 6 | 行号验证 | 全函数 28 锚点 + 跨文件 12 处 grep 穷举 (eval.c 98-957 / script.c 16-640 / script_lua.c 96-1660 / functions.c 163-1118 / function_lua.c 64-414 / config.c 3201 / commands.def 11095-11102 / deps/lua loslib.c 240-243 + lua.h 19) | 记录 |

## 07 五维度

### 维度1 功能正确性
- EVALSHA 快速失败 (sha 长度≠40) / 缓存未命中 NOSCRIPT
- scriptCall 六重验证链逐行对照
- KILL 写门槛 (SCRIPT_WRITE_DIRTY) 与 pcall 免疫 (MASKLINE)
- FUNCTION LOAD 双 ctx 回滚路径
- 类型转换 14 函数与 RESP3 全谱对照

### 维度2 性能
- 指令计数钩子 100000 (超时检查摊薄)
- registry 直接调用 (避免重复编译)
- LRU 500 上限防缓存无限增长
- 双 ctx 加载 O(1) 指针交换

### 维度3 内存
- lua_scripts_mem 记账 (sha+body) + evalMemory 三构成
- jemalloc arena 绑定 (luaEnvInit — R-18 交叉)
- SCRIPT FLUSH ASYNC (lazyfree)

### 维度4 一致性
- 确定性随机 (math.random 替换) — 主从重放
- effects 传播 — 命令级精确重放
- 脚本内 SELECT 隔离 (script_client 独立 DB)
- replicate_commands 空操作 + set_repl 保留 (API 兼容)

### 维度5 负面空间 (已写入大纲 7 条)
- 不迁移 5.4/不多引擎/不完整沙箱/不写后撤销/不续跑/不统一传播/不磁盘缓存

## 结论
R-30 全部锚点 ~40 处验证, 6 闭环完成, **编造修正 1 + 机制精确化 1 + 表述精确化 1 + 补充锚点 1**。怀疑审计 2/6 修正已入 pass1-notes (原子性=超时降级 / os 库非"无")。推断 3 处显式标注。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1 维度1: 叙事桥 + 结构完整性

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 R-20/R-16/R-28/R-33/R-21 (序号均 < 31) 已讲 ✅; 引出 R-31 (未来 OK); 对照 R-16/R-18 ✅; 读者处境场景化 (原子性疑问/缓存存代码原因/io 进不来 os 能进/传播方式) ✅; 六结构元素齐备 ✅ | 记录 |

## R2 维度2: 锚点密度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 大纲 ~40 锚点 (file:line) — 🟡B ≥4 ⏫; 含 deps/lua 补丁锚点 ✅ | 记录 |

## R3 维度3: 前向引用

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | 前置声明无未来域 (R-31 仅引出) ✅ | 记录 |

## R4 维度4: 横切关注点覆盖

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 通过项 | 传播 (effects/EVALSHA 重写) / 复制 (确定性随机) / 内存 (arena+记账+LRU) / 集群 (跨槽验证) / ACL (scriptCall 复查) / 事务 (MULTI 传递) / 持久化 (FUNCTION DUMP + AOF) / 过期 (ALLOW_STALE) — 八横切全覆盖 ✅ | 记录 |

## R5 维度5: 负面空间 + 开篇质量

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | 通过项 | 负面空间 7 条 ✅; 开篇场景化 ✅ (原子性悖论/缓存动机/沙箱双面) | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **反写测试发现** | 大纲 §4 "busy 模式可执行命令面" 引 redis.conf L1574-1580 "possibly some other commands" — **未穷举实际允许面** (哪些命令在 busy 期间可执行未源码验证) — 需标注为"以 SCRIPT KILL/FUNCTION KILL/SHUTDOWN 为主, 其余为受限面 (源码验证不足, 标注)" | 大纲 §4 标注推断 |
| 13 | 通过项 | 其余 ~35 句机制描述逐句对源码一致 ✅ (sha1hex/注册名/LRU 500/仅 EVAL 淘汰/shebang 5 标志/EVAL_COMPAT 兼容路径/六重验证/effects/replicate_commands 空操作/超时链/KILL 写门槛/MASKLINE 免疫/双 ctx 回滚/register_function/DUMP-RESTORE/14 类型转换/DB 隔离/命令版本) | 记录 |

## 二次 REVIEW 汇总
07 五维度轮换 + 内容深度共 **1 处标注修复** (busy 命令面 #12), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | funcname 长度 | "f_"(2) + sha(40) + NUL = 43 — eval.c:562-566 数组 [43] ✅ | 通过 |
| V2 | LRU 淘汰时机 | 创建时循环 (L533-541): ≥500 淘汰最老直到 <500; 执行后重排 (L610-615) — "LRU" 实际是"最近使用重排的 FIFO" (无访问时间戳, 执行即重排) ✅ | 通过 |
| V3 | EVAL_COMPAT 语义 | 无 shebang → 旧行为: 只查 stale (L239-245) + 随机性等老规则 (脚本内非确定性命令报错面保留) ✅ | 通过 |
| V4 | SCRIPT_WRITE_DIRTY 时机 | scriptCall L597-600: 仅命令验证通过且 CMD_WRITE → 置位 — 被拒的写命令不算脏 (不可 KILL 判定保守) ✅ | 通过 |
| V5 | KILL 后 pcall 免疫 | luaMaskCountHook L1551-1555: 改 MASKLINE 后 luaError — 即使 pcall 捕获, 下一条指令再触发 → 永续终止 ✅ | 通过 |
| V6 | 双 ctx 回滚完备性 | libraryJoin: 冲突在"链接"前全检测 (L337-360); 链接后无失败路径 (L363-375); 回滚只含已卸载旧库 (L380-389) — 无中间态 ✅ | 通过 |
| V7 | 函数标志继承 | register_function flags (function_lua.c:221-262) → functionInfo; FCALL 时 scriptPrepareForRun 用函数 flags (fcallCommandGeneric L609+) — 函数级 > 库级 ✅ | 通过 |
| V8 | 编译超时钩子 | luaEngineLoadHook (function_lua.c:64-81): LUA_MASKLINE 每行检查 duration>timeout → luaError — 编译期长循环可终止 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 14 | **补充锚点** | 大纲 §1 未提 **stat_evictedscripts** (LRU 淘汰统计, eval.c:537) 与 INFO 面 (evicted_scripts) — 淘汰可观测性 | 大纲 §1 补注 |
| 15 | 精确化 | 大纲 §1 "LRU 淘汰" 表述可更精确: 无访问时间戳, 执行即重排的 **FIFO+重排** (LRU 语义近似) — V2 实证 | 大纲 §1 修正 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **2 处** (补充锚点 1 / 精确化 1), 全部修复。锚点逐句 re-grep 无行号偏移。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查五个存疑点 (busy 拒绝点/非确定性命令/随机种子来源/FUNCTION 传播/luaGC), 并做反写测试。

## 追查过程 (五个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | busy 模式到底拒绝什么? | server.c:4155-4175: `isInsideYieldingLongCommand() && !CMD_ALLOW_BUSY` → -BUSY (scriptIsEval → slowevalerr / 否则 slowscripterr L4167-4169); **CMD_ALLOW_BUSY 穷举 = 13 处 MAKE_CMD 12 命令** (auth/discard/hello/quit/reset/replconf/shutdown/script-kill/function-kill/function-stats/multi/watch/unwatch); **事务命令豁免** (L4156-4160 注释 PR #7022) | 发现 2 (大纲 §4 升级) |
| T2 | 脚本内非确定性命令 (SPOP/TIME)? | t_set.c:968 `rewriteClientCommandVector(c,3,shared.srem,...)` — **SPOP 被重写为 SREM**; 测试 L1475 "Using side effects is not a problem with command replication" (redis.call('time')) + L74 "scripts rewriting client->argv" | 发现 5 (effects 确定性的另一支柱) |
| T3 | math.random 的种子/动机? | rand.c:1-6 注释: 动机 = **跨系统一致性** (libc rand() 同种子跨架构不保证同序列); rand48 静态状态**进程级推进无每次播种** (redisSrand48 仅 math.randomseed 调用); 测试 "seeded randomly" = 状态自然推进 + is_eval 分支 replicate_commands 调用为 3.2 残留 | **认知修正 (发现 1)** |
| T4 | 主从一致性的真实保证? | effects 传播: 从库执行传播的命令流**不执行脚本** (脚本内随机只影响主库本地决策); 非确定性命令重写 (SPOP→SREM) 保证命令流确定 | 发现 1 落点 |
| T5 | luaGC 周期? | LUA_GC_CYCLE_PERIOD = **50 命令** (script_lua.c:1705, 注释: 50 命令+50 步收集) | 发现 3 (大纲 §6 补充) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | busy 事务豁免必要性 | MULTI 已入队命令若被拒 → pipeline 事务半执行 (注释 L4156-4160 PR #7022) — 事务命令 CMD_ALLOW_BUSY 的动机闭环 ✅ | 通过 |
| V2 | -BUSY 双文案 | scriptIsEval() → slowevalerr ("SCRIPT KILL") / 否则 slowscripterr ("FUNCTION KILL") — 用户指引正确 ✅ | 通过 |
| V3 | 随机状态推进 | 两次 EVAL math.random: 静态 x[]/a[]/c 状态连续 next() → 结果必不同 (无重置); "随机 now" 测试断言 a!=b ✅ | 通过 |
| V4 | SPOP 重写等价性 | SPOP (删除随机元素) → SREM (删除指定元素) — 脚本内传播的命令流从"随机删除"变为"删除实际元素" (主库已算好), 从库重放确定 ✅ | 通过 |
| V5 | EVAL_COMPAT 随机语义 | 测试 L1451: is_eval 分支调用 replicate_commands() 才"get real randomization" — 3.2 语义残留 (replicate_commands 现空操作), 7.0 后无条件真随机 ✅ | 通过 |
| V6 | GC 周期设计 | luaGC 每 50 命令 — 脚本长跑时内存回收有界 ✅ | 通过 |
| V7 | evicted_scripts INFO | server.c:5887 "evicted_scripts:%lld" — LRU 淘汰可观测 ✅ | 通过 |
| V8 | 双缓存传播面 | body 缓存: EVALSHA 可用性 (NOSCRIPT 快速失败) + 旧版兼容 — 与"从库不执行脚本"不矛盾 (effects 面无重写需求) ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **认知修正** | 大纲 §2 "math.random 确定性替换 — 主从/AOF 重放一致性" **归因错误**: rand.c:1-6 动机 = **跨系统一致性** (libc rand() 跨架构不保证); 主从一致性 = **effects 传播 + 非确定性命令重写** (从库不执行脚本, 只执行传播命令流; SPOP→SREM t_set.c:968); rand48 静态推进无每次播种 | 大纲 §2 重写 + q2 同步 |
| 17 | **锚点补充** | 大纲 §4 busy 命令面从"未验证标注"升级: **CMD_ALLOW_BUSY 12 命令穷举** (auth/discard/hello/quit/reset/replconf/shutdown/script-kill/function-kill/function-stats/multi/watch/unwatch) + **事务豁免 PR #7022** (server.c:4156-4160) | 大纲 §4 重写 |
| 18 | 锚点补充 | luaGC 周期 LUA_GC_CYCLE_PERIOD=50 (script_lua.c:1705) 缺失 | 大纲 §6 补充 |

## 反写测试 (只读大纲能否写文章)

- §1 缓存: sha1/双缓存/LRU 500/FIFO+重排/淘汰统计 — 可写 ✅
- §2 沙箱: 库装载面/os 仅 clock/全局锁/白名单/**跨系统随机 (修正后)** — 可写 ✅
- §3 运行时: 标志验证/六重验证/effects/set_repl — 可写 ✅
- §4 超时: 钩子链/busy 模式/**12 命令面+事务豁免 (修正后)**/KILL 门槛/pcall 免疫 — 可写 ✅
- §5 Functions: 双 ctx 原子/引擎抽象/register_function/编译超时 — 可写 ✅
- §6 转换: 14 类型/**GC 周期 (补充后)**/四命令族 — 可写 ✅
- 负面空间 7 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑点全实证 (T1-T5); 推理验证 8 项全过 (V1-V8); **新发现 3 处全部修复** — 其中 **#16 认知修正最有价值** (math.random 动机归因: 跨系统一致性而非主从重放; 主从一致的真实支柱 = effects 传播+SPOP→SREM 重写)。大纲经修复后反写测试全过。
