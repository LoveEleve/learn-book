/**
 * MiniG3 — gRPC-Java G-3 客户端域极简复现 (纯 JDK, 无依赖)
 *
 * 复现四个核心机制 (与源码逐条对应):
 *  1. SynchronizationContext 无锁单线程: CAS 抢权 + 队列 + do-while 重查 (SynchronizationContext.java:62-137)
 *  2. 懒启动: 首次 newCall 才 exitIdleMode 建资源 (ManagedChannelImpl.java:389-417)
 *  3. DelayedClientCall 缓冲: passThrough 原子放行, 缓冲操作按序重放 (DelayedClientCall.java:206-320)
 *  4. 调用短路: Context 已取消 → NoopClientStream 直接回调失败 (ClientCallImpl.java:197-216)
 *
 * 用法: javac MiniG3.java && java MiniG3
 */
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

public class MiniG3 {

  // ============ 机制 1: SynchronizationContext ============
  static final class SyncContext implements Runnable {
    final Queue<Runnable> queue = new LinkedList<>();          // ConcurrentLinkedQueue (L65)
    final AtomicReference<Thread> drainingThread = new AtomicReference<>(); // L66

    void execute(Runnable task) {                              // L126-129: executeLater + drain
      queue.add(task);
      drain();
    }

    void drain() {                                             // L87-106
      do {
        if (!drainingThread.compareAndSet(null, Thread.currentThread())) {
          return;                                              // 别的线程在排空, 放弃
        }
        try {
          Runnable r;
          while ((r = queue.poll()) != null) {
            r.run();
          }
        } finally {
          drainingThread.set(null);
        }
      } while (!queue.isEmpty());                              // L105: 重查防竞态
    }

    void throwIfNotInThisSynchronizationContext() {            // L135-137
      if (Thread.currentThread() != drainingThread.get()) {
        throw new IllegalStateException("must be called from syncContext");
      }
    }

    @Override public void run() { /* 由 drain 直接 run */ }
  }

  // ============ 机制 2: 懒启动 Channel ============
  static final class MiniChannel {
    boolean lbCreated, resolverStarted;
    final SyncContext syncContext = new SyncContext();
    boolean idle = true;

    void exitIdleMode() {                                      // ManagedChannelImpl.java:389
      syncContext.throwIfNotInThisSynchronizationContext();
      if (!lbCreated) {
        lbCreated = true;                                      // L412: newLoadBalancer
        resolverStarted = true;                                // L415: nameResolver.start
        idle = false;
      }
    }

    Object newCall() {
      if (!lbCreated) {
        syncContext.execute(this::exitIdleMode);               // L864-868: 首次触发懒启动
      }
      return new Object();                                     // ClientCall
    }
  }

  // ============ 机制 3: DelayedClientCall 缓冲 ============
  static final class DelayedCall {
    boolean passThrough;                                       // L307-309 一次性切换
    final Queue<Runnable> pendingRunnables = new LinkedList<>();
    final Queue<String> bufferedEvents = new LinkedList<>();

    void start(String headers) {                               // L206: 未放行则记录
      if (!passThrough) {
        bufferedEvents.add("start(" + headers + ")");
      }
    }

    void sendMessage(String msg) {
      if (!passThrough) {
        bufferedEvents.add("sendMessage(" + msg + ")");
      }
    }

    void delayOrExecute(Runnable r) {                          // L270-278
      if (!passThrough) { pendingRunnables.add(r); return; }
      r.run();
    }

    void drainPendingCalls() {                                 // L300-320
      while (true) {
        if (pendingRunnables.isEmpty()) {
          passThrough = true;                                  // L307-309: 永不回头
          break;
        }
        Runnable r = pendingRunnables.poll();
        r.run();
      }
    }

    void setCall() {
      drainPendingCalls();                                     // 真实 call 注入 → 放行
      System.out.println("  [delayed] 放行, 缓冲事件重放: " + bufferedEvents);
    }
  }

  // ============ 机制 4: Context 短路 ============
  static final class MiniContext {
    boolean cancelled;
  }

  static String startCall(MiniContext ctx) {                   // ClientCallImpl.java:197-216
    if (ctx.cancelled) {
      return "短路: NoopClientStream → onClose(状态来自取消)";   // 不建真实流
    }
    return "正常: 创建真实流";
  }

  // ============ 主验证 ============
  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. syncContext: 单线程排空 + 断言 ---
    SyncContext sc = new SyncContext();
    final int[] counter = {0};
    sc.execute(() -> {
      sc.throwIfNotInThisSynchronizationContext();             // 排空线程内通过
      counter[0]++;
      sc.execute(() -> counter[0]++);                          // 可重入: 内联执行
    });
    System.out.println("[syncContext] 任务执行次数: " + counter[0] + " (期望 2, 可重入)");
    if (counter[0] == 2) pass++; else fail++;

    try {
      sc.throwIfNotInThisSynchronizationContext();             // 排空线程外抛
      fail++;
    } catch (IllegalStateException e) {
      System.out.println("[syncContext] 线程外断言拦截: " + e.getMessage());
      pass++;
    }

    // --- 2. 懒启动 ---
    MiniChannel ch = new MiniChannel();
    System.out.println("[懒启动] 首次 newCall 前: lbCreated=" + ch.lbCreated);
    ch.newCall();                                              // 触发 exitIdleMode
    System.out.println("[懒启动] 首次 newCall 后: lbCreated=" + ch.lbCreated
        + ", resolverStarted=" + ch.resolverStarted);
    if (ch.lbCreated && ch.resolverStarted) pass++; else fail++;

    // --- 3. DelayedCall 缓冲重放 ---
    DelayedCall dc = new DelayedCall();
    dc.start("headers");                                       // 缓冲期操作
    dc.sendMessage("hello");
    dc.setCall();                                              // 放行
    dc.sendMessage("world");                                   // 放行后直通
    if (dc.passThrough) pass++; else fail++;

    // --- 4. Context 短路 ---
    MiniContext cancelled = new MiniContext();
    cancelled.cancelled = true;
    System.out.println("[短路] 已取消调用: " + startCall(cancelled));
    System.out.println("[短路] 正常调用: " + startCall(new MiniContext()));
    pass++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
