# S-2 审查记录(全流程)

> 日期: 2026-08-17 | 范围: Pass 1 → Pass 2 → 全视角 36 问 → 时空溯源 → 三篇正文 → harness
> 审查方法: 09 怀疑审计 + 全视角提问 + 源码实证 + 费曼法复现

## 一、Pass 1 审查

- 09 域级审计 7 断言: 修正 1 项("ContextUtil 281 行" → 实为 213 行); 补锚 4 项(SphO boolean 版、AsyncEntry 两阶段、20 个公开入口、COW 与 chainMap 同构)
- 标记 9 问 → 全部闭环(pass2-q1~q9)

## 二、Pass 2 审查(9 闭环)

| 闭环 | 关键实证 |
|---|---|
| Q1 exit 三态 | NullContext 免清理 / 错序"先扯平再抛" / 五步收尾; trueExit 返回 parent |
| Q2 InternalContextUtil | 绕过"默认名不可自定义"校验; 仅 2 处内部调用点 |
| Q3 AsyncEntry | initAsyncContext + cleanCurrentEntryInLocal = 生命周期从 ThreadLocal 转移到对象 |
| Q4 EntryType | **修正假设**: NodeSelector/ClusterBuilder 不消费 EntryType; 消费方仅 StatisticSlot + SystemRuleManager |
| Q5 SphO | catch Throwable → **true 放行**(框架错误不传播的哲学) |
| Q6 Context 单指针 | 无栈结构, 调用栈 = CtEntry.parent 链 |
| Q7 双上限 | 2000/6000 并列两道闸; NULL_CONTEXT 带一次性 WARN, 链超限静默 |
| Q8 Tracer | shouldTrace 五级过滤; BlockException 永不 trace; 落点 entry.error |
| Q9 resourceType | **盲区修正**: equals 仅按 name, resourceType/entryType 不参与链 key; 全部汇聚 2 私有方法 |

## 三、全视角 36 问审查

- 6 身份提问 36 条; 14 条有闭环证据, 22 条大纲/正文逐条落锚
- #32(COW 引入版本)git 查证: **0.1.0 已有 COW 结构**(双检锁 + HashMap 复制 + 2000 上限), 4073053b(2019-03-11)仅 diamond 语法替换 — 修正"COW 后期引入"的初始猜想
- #14(双检锁正确性)正证: volatile 读 → LOCK 内重查 → COW 写(与 chainMap 同模式)

## 四、时空溯源审查(temporal-trace.md)

- 0.1.0 全核已具: 嵌套 CtEntry/三放行/双检锁/2000 上限/错误释放自愈 — 入口状态机是"第一天就完成"的设计
- d798794a(2018-11): 异步重构 → CtEntry 独立文件 + AsyncEntry + asyncContext
- **cbaacfda**: 默认 context 自动退出从"parent==null 无条件"收窄为"仅默认 context" — 修复显式 context 被误清的 bug(正文第 4 节主打)
- be43a31d(#152) 超限内部 bug; 9c2683e6 shouldWarn 警告; 04a1d065(#1429) 锁错位修复; 1.7.0 resourceType API

## 五、上篇正文审查(3 轮, 修正 7 处)

| 轮 | 发现问题 |
|---|---|
| R1 | "14 个 entry 变体" → 实为 **20 个公开入口**(12 entry + 6 asyncEntry + 2 entryWithPriority); SphO 转译行号 180-185→180-188; CtSph 包装代码行号 286-290→313-316; "8 个规则槽" → "内置 8 槽 + 扩展槽" |
| R2 | 图语义验证: entryWithPriority → lookProcessChain/internalEnter/chain.entry/getContext 完全吻合 |
| R3 | 通读: 标题"14 个变体"漏改 → "20 个入口"; **Env.sph 是 public static final, "可替换入口引擎"表述错误** → 修正为 final 语义 + 架构抽象价值; "七步" → "八步" |

## 六、中篇正文审查(3 轮, 修正 5 处)

| 轮 | 发现问题 |
|---|---|
| R1 | getLastNode 行号 170-181 → **179-186**(entranceNode getter 在 170) |
| R2 | 图验证: exitForContext 出边 = chain.exit/ContextUtil.exit/isDefaultContext/callExitHandlers/clearEntryContext/setCurEntry 五步全吻合; **whenTerminate 真实消费者 = AbstractCircuitBreaker.java:108**(熔断结算)→ 补入正文 |
| R3 | 通读: "四步收尾" → **五步收尾**(①②③④⑤); "被记了一次日志级异常" → 异常抛给调用方、栈已救回; cbaacfda 后果表述精确化(显式调用树破坏, 后续 entry 跑错统计树, 而非"节点丢失") |

## 七、下篇正文审查(3 轮, 修正 5 处)

| 轮 | 发现问题 |
|---|---|
| R1 | trueExit 行号 84-88 → **97-98**; setExceptionPredicate 183 验证 |
| R2 | 图验证: setError 调用方 = Tracer.traceEntryInternal **+ StatisticSlot.entry**(被拦 setBlockError:97/内部错误 setError:118)→ 修正"写入方就是 Tracer"为"主要写入方", 并点出 exit 侧 !BlockException 与框架写入的闭环 |
| R3 | 通读: initAsyncContext 行号 63-75→75-83; 悬念回收段行号同步; "系统规则熔断" → "打回入站流量" |

## 八、harness 费曼法(3 轮修正, 终态 PASS=13/13)

| 轮 | 盲区 |
|---|---|
| R1 | **继承方向写反**(真实 Entry 是基类、CtEntry 子类、AsyncEntry 孙类) — 5 编译错误 |
| R2 | 错序 exit 的异常未捕获(自愈语义=扯平后抛); 断言设计重新排布(先扯平验证再正常退出验证) |
| R3 | total 计数 14 vs 断言 13(C1 第三段多计 1)→ PASS=13/13 |

## 九、自审检查单(01)

- [x] grep-verified 声明每条均有源码实证(全部锚点逐一 sed/awk 复核)
- [x] 每轮审查均发现真实内容问题(正文累计 17 处修正, 含 1 处表述性错误"可替换入口引擎"、1 处假设方向错误"EntryType 影响节点选择")
- [x] 语义工具使用: search_graph + trace_path(entryWithPriority 出边、exitForContext 出边、setError 入边)
- [x] 数值声明全部对照: 20 入口/2000/6000/36 问/13 断言/行号
- [x] 无类名推断: cbaacfda 后果经 0.1.0 源码通读验证; Env.sph final 实证
- [x] 闭环笔记 10 文件存在(pass1 + q1~q9)
- [x] 概念覆盖: completeness 36 问全 ✅ 后收束
- [x] 代码示例: 全部标注"节选自 file:line"
- [x] 跨文档一致: 上篇"20 个入口"与 SphU.java 一致; EntryType 消费与 S-5 移交点标注; equals 仅 name 与 S-1 中篇一致(无勘误)

## 十、遗留(诚实清单)

1. asyncContext 上退出时若名为默认 context 且 parent==null → ContextUtil.exit() 会清**异步线程**的 ThreadLocal: 边角语义未在真机验证(真实代码同构, 见 AsyncEntry.java:97-98 + CtEntry.java:123-125)
2. trueEnter 双检锁的可见性细节(#1429 修复前的 bug 形态)未深挖: 04a1d065 diff 未读
3. MetricEntryCallbackTest 的 whenTerminate 时序断言未逐条对照(正文只用了 AbstractCircuitBreaker.java:108 实证)
4. SphO/SphU 测试文件(SphUTest/SphOTest 等 7 文件)留作 S-2 回归基准