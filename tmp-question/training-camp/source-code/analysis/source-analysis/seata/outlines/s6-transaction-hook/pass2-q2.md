# 闭环笔记 q2: 钩子管理器 — ThreadLocal 栈 + 只读视图

## 假设
TransactionHookManager 用 ThreadLocal 维护当前线程钩子列表; 对外只读。

## 验证过程
- **存储** (TransactionHookManager:32-33): **ThreadLocal<List\<TransactionHook\>> LOCAL_HOOKS** — 线程隔离
- **getHooks** (L35-47): 空 → **Collections.emptyList()**; 非空 → **Collections.unmodifiableList (只读视图)** — 触发方不能修改列表 (并发修改防护)
- **registerHook** (L49-61): **null → NullPointerException**; lazy 创建 ArrayList; **追加 (注册序)** — 触发按注册序
- **clear** (L63-66): LOCAL_HOOKS.remove() — 全清
- **生命周期**: 业务前注册 → 事务周期触发 → Launcher 完成时 cleanUp 清除 (S-1) — **ThreadLocal 无泄漏** (remove 而非 set(empty))

## 代码类型
Implementation (管理器)

## 跨域关联
- S-1: getCurrentHooks 消费 (L404-406)
- S-5: ThreadLocal 上下文 (同线程模型)

## 结论
管理器 = ThreadLocal 列表 + 只读视图 + lazy 注册; clear 用 remove 防泄漏。
源码位置: TransactionHookManager.java:32-66
