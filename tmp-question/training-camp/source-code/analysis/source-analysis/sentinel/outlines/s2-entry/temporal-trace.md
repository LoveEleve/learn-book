# S-2 入口域 — 时空溯源 (0.1.0 → 1.8.9)

> 方法: git 逐 commit 比对 | 源码: sentinel (c92fea5d = 0.1.0 初始)

## 演进阶段图

```
0.1.0 (2018-06, c92fea5d)     入口全核已具: SphU/SphO/CtSph/Entry/EntryType/Tracer
  │                           ├─ CtEntry 为 CtSph 私有嵌套类
  │                           ├─ trueEnter: 双检锁 + COW + 2000 上限 全部已有
  │                           ├─ entryWithPriority 三放行 + catch Block→e.exit 已有
  │                           ├─ 错误释放"先扯平全栈再抛"已有
  │                           └─ parent==null → 无条件 ContextUtil.exit()
  │
d798794a (2018-11)            异步支持重构: Context/Entry 支持异步调用链
  │                           ├─ CtEntry 提为独立文件, exitForContext 参数化
  │                           ├─ AsyncEntry 诞生 (asyncContext 对象携带)
  │                           └─ Context 增加 async 标志 + newAsyncContext
  │
be43a31d (#152)               context 超限内部 bug 修复
cbaacfda                      默认 context 自动退出收窄: parent==null 且 isDefaultContext 才 exit
  │                           (0.1.0 是"parent==null 无条件退出" → 显式 context 曾被误清)
9c2683e6                      超限首警日志引入 (setNullContext + shouldWarn)
4073053b (2019-03-11)         COW 语法化 (diamond), 结构未变
04a1d065 (#1429)             ContextUtil/ClusterNode 锁错位 bug 修复
1.7.0 (2021-01)              entryWithType/asyncEntryWithType (resourceType 分类标签)
1.8.9 (现行)                  EXIT 前终态: 9 降级/自动退/异步全就位
```

## 关键差异表 (0.1.0 vs 1.8.9)

| 维度 | 0.1.0 | 1.8.9 | 引入节点 |
|---|---|---|---|
| CtEntry 位置 | CtSph 私有嵌套类 | 独立 CtEntry.java + AsyncEntry 子类 | d798794a |
| 错误释放 | 扯平全栈后抛 | 同 (消息更详细) | 0.1.0 已有 |
| 自动退出 | parent==null 无条件 | parent==null **且** isDefaultContext | cbaacfda 收窄 |
| 超限警告 | 无 | shouldWarn 一次性 WARN | 9c2683e6 |
| 异步 context | 无 | asyncContext 字段 + exitForContext(asyncContext) | d798794a |
| NullContext 免清理 | 无此分支 | 有 | d798794a 后 |
| whenTerminate 回调 | 无 | 有 | 1.x 中期 |
| resourceType API | 无 | entryWithType/asyncEntryWithType (@since 1.7.0) | 1.7.0 |
| 锁 | LOCK 双检 | 同 + #1429 锁错位修复 | 04a1d065 |

## 不变内核 (0.1.0 至今逐字未变)

- trueEnter 的 双检锁 + COW + MAX_CONTEXT_NAME_SIZE(2000) 三段结构 (c92fea5d 已有, 1.8.9 同)
- entryWithPriority 的 三放行顺序: NullContext → ON 开关 → chain==null (CtSph.java:120-141)
- 错误释放自愈语义: 先扯平调用栈再抛 ErrorEntryFreeException
- EntryType IN/OUT 二值 + 消费面 (StatisticSlot 全局入口 + SystemRuleManager 300 行)

## 回归基准

- SphUTest / SphOTest / CtSphTest / CtEntryTest / EntryTest / AsyncEntryTest / AsyncEntryIntegrationTest (7 文件)
- MetricEntryCallbackTest (whenTerminate/回调机制回归)