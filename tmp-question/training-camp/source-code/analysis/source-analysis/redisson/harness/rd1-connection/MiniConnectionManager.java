import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * MiniConnectionManager — RD-1 主类+连接管理 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 Redisson 4.6.2 源码):
 *   A. eager/lazy 汇聚 + 模式工厂 (ConnectionManager.create 5 分支, ConnectionManager.java:89-111;
 *      ConfigSupport.getConfig 优先级 L844-859)
 *   B. lazyConnect 单飞锁: CAS latch 定所有者 + 异常检测放行重试 + volatile connectingThread 防自死锁
 *      (MasterSlaveConnectionManager.lazyConnect L190-227; connectingThread L70)
 *   C. AsyncSemaphore permit 池容量协议: init 精确消费/失败归还, borrow 借 free 队列 (ConnectionsHolder.java:44-59)
 *
 * 机制复现, 非完整实现 — 无网已足证协议。
 */
public class MiniConnectionManager {

    // ===== A. 模式工厂: 5 分支 if-else (ConnectionManager.java:89-111) =====
    public enum Mode { SINGLE, MASTER_SLAVE, SENTINEL, CLUSTER, REPLICATED }

    // 优先级固定: MasterSlave > Single > Sentinel > Cluster > Replicated (ConfigSupport.getConfig L844-859)
    static Mode detectMode(boolean[] flags) {
        if (flags[0]) return Mode.MASTER_SLAVE;
        if (flags[1]) return Mode.SINGLE;
        if (flags[2]) return Mode.SENTINEL;
        if (flags[3]) return Mode.CLUSTER;
        if (flags[4]) return Mode.REPLICATED;
        throw new IllegalArgumentException("server(s) address(es) not defined!"); // L104-107
    }

    // ===== B. 单飞锁: 完整复现 lazyConnect (L190-227 语义压缩) =====
    private final AtomicReference<CompletableFuture<Void>> lazyConnectLatch = new AtomicReference<>();
    volatile boolean initialized;
    private volatile Thread connectingThread;   // L70: volatile 保证身份可见性

    // 连接重入计数: 记录 connect() 实际执行次数 (验证"只连一次")
    final AtomicInteger connectCalls = new AtomicInteger();
    private final boolean failFirstConnect;

    public MiniConnectionManager(boolean failFirstConnect) {
        this.failFirstConnect = failFirstConnect;
    }

    public void lazyConnect() {
        if (initialized) return;                                       // L191-193 快速路径
        if (Thread.currentThread() == connectingThread) return;        // L197-199 重入防护

        CompletableFuture<Void> newFuture = new CompletableFuture<>();
        if (!lazyConnectLatch.compareAndSet(null, newFuture)) {         // L201 CAS 定所有者
            CompletableFuture<Void> cur = lazyConnectLatch.get();
            if (cur.isCompletedExceptionally()) {                       // L204 失败检测 → 尝试替换
                if (!lazyConnectLatch.compareAndSet(cur, newFuture)) {
                    lazyConnectLatch.get().join();                      // L206 又一次输掉 → 等别人
                    return;
                }
            } else {
                lazyConnectLatch.get().join();                          // L210 等别人连完
                return;
            }
        }

        connectingThread = Thread.currentThread();                      // L217
        try {
            connectInternal();                                          // L219
            newFuture.complete(null);
        } catch (Exception e) {
            newFuture.completeExceptionally(e);                         // L222 失败完成 → 供重试检测
            initialized = false;
            throw e;
        } finally {
            connectingThread = null;                                    // L225
        }
    }

    private void connectInternal() {
        connectCalls.incrementAndGet();
        if (failFirstConnect && connectCalls.get() == 1) {
            throw new RuntimeException("simulated connect failure");
        }
        initialized = true;
    }

    // ===== C. permit 池容量: ConnectionsHolder 语义 (L44-59) =====
    static class ConnectionsHolder {
        private final ConcurrentLinkedDeque<String> free = new ConcurrentLinkedDeque<>();
        private final Semaphore permits;
        private final int poolMax;

        ConnectionsHolder(int poolMax) {
            this.poolMax = poolMax;
            this.permits = new Semaphore(poolMax); // AsyncSemaphore(poolMaxSize, group)
        }

        int freePermits() {
            return permits.availablePermits();
        }

        // initConnections(minimumIdleSize): 每建一连接消费一 permit, 失败必须归还 (ConnectionsHolder.java:141)
        boolean initConnections(int count) {
            for (int i = 0; i < count; i++) {
                if (!permits.tryAcquire()) {
                    rollbackInitialized(i); // 失败 → 归还已消费的, 保证 counter 精确
                    return false;
                }
                free.add("conn-" + i);
            }
            return true;
        }

        private void rollbackInitialized(int upTo) {
            for (int i = 0; i < upTo; i++) {
                free.poll();
                permits.release(); // 精确配对: 消费过的必须归还
            }
        }

        String acquire() {          // ConnectionsHolder.acquireConnection L224
            String c = free.poll(); // 先借 free
            if (c != null) return c;
            if (!permits.tryAcquire()) return null; // 池满 → 等/失败
            return "conn-new";
        }

        void release(String c) {
            if (c != null) {
                free.add(c);        // 归还回队
                permits.release();  // + 释放 permit
            }
        }
    }
}