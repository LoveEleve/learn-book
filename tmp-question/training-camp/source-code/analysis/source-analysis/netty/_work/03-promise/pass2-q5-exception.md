## Loop Note: Q5 — Promise 异常传播链

**Hypothesis**: Promise.setFailure 通过 CauseHolder 包装异常 → notifyListeners 通知所有监听器 → 监听器检查 future.isSuccess()/cause() → 调用方处理失败。get() 调用 await() 后检查 result → throw ExecutionException 包裹原始 cause。

**Verification**:
- `DefaultPromise.setFailure(cause)` (`line 123-127`): `setFailure0(cause)` → setValue0(new CauseHolder(cause))
- `CauseHolder` (`DefaultPromise` 内部类) — 包装 Throwable + 可选简化栈
- `cause()` (`line 172-188`): `cause0(result)` → `result instanceof CauseHolder` → 展开包装。`CANCELLATION_CAUSE_HOLDER` 特殊处理: 懒创建 `LeanCancellationException` (不填栈)
- `LeanCancellationException` (`line 155-168`): `fillInStackTrace()` → 使用预先捕获的全局 CANCELLATION_STACK — 避免每次取消创建完整栈
- `setSuccess(null)` → `result=SUCCESS` (singleton Object) — 不是 null
- `setSuccess(value)` → `result=value` — 直接存 value（非 null 时）
- `get()` (`line 349-365`): `await()` → check result → `SUCCESS/UNCANCELLABLE` → null; `CauseHolder` → `CancellationException` or `ExecutionException(cause)`; else → `(V) result`

**Code type**: Implementation

**设计权衡**: SUCCESS = new Object() 单例 (line 57)。把所有成功存储统一为空 singleleton——V 结果在用户存入的非 null 值里，null 表示 Void 结果。CauseHolder 包装异常，CANCELLATION_CAUSE_HOLDER 额外携带 LeanCancellationException（不填完整栈）。setSuccess0 对 null 特殊处理——确保 isSuccess() 能区分 SUCCESS 标记和 null Void 值。

**结论**: Promise 的异常传播 = CauseHolder 包装 + notifyListeners 通知 + listener.cause() 检查 + get() → ExecutionException。LeanCancellationException 避免取消操作填完整栈 (高频操作)。source: DefaultPromise.java:55-61,123-188,349-365
