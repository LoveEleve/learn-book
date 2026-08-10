## Loop Note: Q1 — Future/Promise 读写分离

**Hypothesis**: Future 和 Promise 不是"同一个接口的不同实现"——是故意分为读者接口和写者接口。Channel 拿到只读 Future，EventLoop 拿到可写 Promise。分离防止 Channel 误改异步结果。

**Verification**:
- `Future.java:26` — `interface Future<V> extends java.util.concurrent.Future<V>` — 只读: `isSuccess()`, `cause()`, `addListener()`, `await()`, `sync()`
- `Promise.java:21` — `interface Promise<V> extends Future<V>` — 可写: `setSuccess(V)`, `setFailure(Throwable)`, `trySuccess(V)`, `tryFailure(Throwable)`, `setUncancellable()`
- `DefaultPromise.java:37` — `class DefaultPromise<V> extends AbstractFuture<V> implements Promise<V>` — 同时实现两个接口
- Channel.write() 返回 `ChannelFuture` (extends Future) — Channel 拿到的是只读面
- EventLoop 内部持有 `ChannelPromise` (extends Promise) — EventLoop 拿到的是可写面

**Code type**: Interface Design

**设计权衡**: JDK Future 没有读写分离——同一个 FutureTask 实例既是结果读取器也是结果写入器。Netty 的分离让 Channel 在完成写操作后不能意外修改结果。EventLoop 完成后调用 `promise.setSuccess()` → Channel 的 Future 自动可见结果。

**Conclusion**: 读写分离 = EventLoop 写、Channel 读、双方不互相干扰。Promise 是可写的，Future 是只读的——DefaultPromise 是唯一同时实现两者的类。source: Future.java:26, Promise.java:21, DefaultPromise.java:37
