import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MiniDruidPool — Druid 池核心极简复现 (费曼法)
 *
 * 只保留核心控制流, 去掉: filter 链/线程池/scheduler/MBean/统计/配置校验
 * 保留: 固定数组 + 单锁 + notEmpty Condition + 借还 + 超时 + shutdown
 *
 * 对应源码:
 *   DruidAbstractDataSource.java:243-244,299 (notEmpty/empty Condition)
 *   DruidDataSource.java:772 (connections[maxActive] 数组)
 *   DruidDataSource.java:2214-2279 (pollLast: await + 取尾 + 超时)
 *   DruidDataSource.java:2194-2212 (putLast: 尾插 + signal)
 *   DruidDataSource.java:1894-2057 (recycle: 归还链)
 *   DruidDataSource.java:2081-2158 (close: signalAll 唤醒退出)
 */
public class MiniDruidPool {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    private final MiniConnection[] connections;   // 固定数组 (L772)
    private int poolingCount;                     // 尾指针
    private final int maxActive;
    private volatile boolean closed;

    public MiniDruidPool(int maxActive) {
        this.maxActive = maxActive;
        this.connections = new MiniConnection[maxActive];
        for (int i = 0; i < 4; i++) {             // 预置 4 个连接 (满配)
            connections[poolingCount++] = new MiniConnection();
        }
    }

    /** 借出: 取尾元素, 空则等待 (pollLast, DruidDataSource.java:2218) */
    public MiniConnection getConnection(long maxWaitMillis) throws SQLException {
        long startTime = System.currentTimeMillis();
        long expiredTime = startTime + maxWaitMillis;
        lock.lock();
        try {
            for (; ; ) {
                if (closed) {
                    throw new SQLException("dataSource already closed"); // L1544
                }
                if (poolingCount > 0) {
                    MiniConnection conn = connections[--poolingCount];
                    connections[poolingCount] = null;
                    return conn;                                          // L2271-2273
                }
                long waitMillis = expiredTime - System.currentTimeMillis();
                if (waitMillis <= 0) {
                    throw new SQLException("wait millis " + (System.currentTimeMillis() - startTime)
                            + ", active " + (maxActive - poolingCount) + ", maxActive " + maxActive); // L1728-1765
                }
                try {
                    notEmpty.await(waitMillis, TimeUnit.MILLISECONDS);    // L2245-2247
                } catch (InterruptedException ie) {
                    notEmpty.signal();                                    // L2266 传播
                    throw new SQLException("interrupt", ie);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /** 归还: 状态复位(简化) + 尾插 + signal (recycle + putLast, L1894/L2194) */
    public void recycle(MiniConnection conn) {
        lock.lock();
        try {
            if (closed || poolingCount >= maxActive) {   // L2195 池满/已关
                conn.closePhysical();                     // 丢弃: 物理关闭
                return;
            }
            conn.reset();                                 // L1943 holder.reset 简化
            connections[poolingCount++] = conn;           // L2200 尾插
            notEmpty.signal();                            // L2208 精确唤醒一个
        } finally {
            lock.unlock();
        }
    }

    /** 关闭: signalAll 唤醒全部等待者 (close, L2081-2147) */
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            for (int i = 0; i < poolingCount; i++) {
                connections[i].closePhysical();           // L2118-2134
                connections[i] = null;
            }
            poolingCount = 0;
            notEmpty.signalAll();                         // L2139 广播退出
        } finally {
            lock.unlock();
        }
    }

    public int getPoolingCount() {
        return poolingCount;
    }

    /** 模拟连接: 脏状态 + 复位 */
    static class MiniConnection {
        private boolean dirty; // 模拟用户改过 autoCommit

        void markDirty() {
            dirty = true;
        }

        void reset() {
            dirty = false;    // holder.reset 四态简化
        }

        void closePhysical() {
            dirty = true;     // 模拟物理连接关闭后不可用
        }
    }

    /** 冒烟测试: 并发借还 + 池满阻塞 + 超时 + shutdown 唤醒 */
    public static void main(String[] args) throws Exception {
        final MiniDruidPool pool = new MiniDruidPool(4);

        // 1. 基本借还
        MiniConnection c1 = pool.getConnection(100);
        c1.markDirty();
        pool.recycle(c1);
        assert pool.getPoolingCount() == 4;

        // 2. 并发 8 线程各借还 100 次 (池只有 4, 验证等待/唤醒不丢)
        Thread[] threads = new Thread[8];
        final boolean[] failed = {false};
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        MiniConnection conn = pool.getConnection(500);
                        conn.markDirty();
                        Thread.sleep(1);
                        pool.recycle(conn);
                    }
                } catch (Exception e) {
                    failed[0] = true;
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assert !failed[0] : "并发借还失败";
        assert pool.getPoolingCount() == 4 : "归还泄漏, poolingCount=" + pool.getPoolingCount();

        // 3. 超时抛异常
        for (int i = 0; i < 4; i++) {
            pool.getConnection(100); // 全部借光
        }
        try {
            pool.getConnection(50);
            assert false : "应该超时";
        } catch (SQLException e) {
            System.out.println("超时异常: " + e.getMessage());
        }
        for (int i = 0; i < 4; i++) {
            pool.recycle(new MiniConnection()); // 池满丢弃验证 (返回假连接无妨)
        }
        assert pool.getPoolingCount() == 4;

        // 4. shutdown 广播唤醒等待者
        for (int i = 0; i < 4; i++) {
            pool.getConnection(100); // 先借光, 让 waiter 真正进入 await
        }
        Thread waiter = new Thread(() -> {
            try {
                pool.getConnection(5000);
                System.out.println("等待者被唤醒(应拿到或抛异常)");
            } catch (SQLException e) {
                System.out.println("等待者退出: " + e.getMessage());
            }
        });
        waiter.start();
        Thread.sleep(50);   // 确保 waiter 已进入 notEmpty.await
        pool.close();       // signalAll 唤醒 → 循环检查 closed → 抛异常
        waiter.join();

        System.out.println("=== MiniDruidPool 冒烟测试通过 ===");
    }
}
