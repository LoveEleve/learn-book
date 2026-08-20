import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.InterProcessMultiLock;
import org.apache.curator.framework.recipes.locks.Lease;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;

/**
 * HarnessC3 — C-3 分布式锁 8 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. 互斥: 持锁者存在时 acquire(timeout) 超时返回 false (LockInternals.java:223-271)
 *   B. 可重入: 同线程 3 次 acquire 2 次 release 仍持有 (InterProcessMutex.java:197-220, 132-144)
 *   C. 公平 FIFO: 释放顺序 = 获取顺序 (序号全序)
 *   D. 超时清理: 超时失败后参与者节点数归零
 *   E. 会话崩溃释放: 客户端 close 后另一客户端可获取 (ephemeral 语义)
 *   F. 读写锁: 多读者并存 / 写者互斥 / 写→读降级
 *   G. 信号量: maxLeases=2 → 第 3 个 acquire 超时; 归还后可取 (InterProcessSemaphoreV2)
 *   H. MultiLock: 部分失败逆序释放已获 (InterProcessMultiLock.java:104-112)
 */
public class HarnessC3 {
    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            pass++;
            System.out.println("  PASS " + name);
        } else {
            fail++;
            System.out.println("  FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== C-3 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A. 互斥 ----------
            System.out.println("[A] 互斥");
            InterProcessMutex mutex = new InterProcessMutex(client, "/lock/a");
            mutex.acquire();
            Thread other = new Thread(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignore) {
                }
            });
            other.start();
            InterProcessMutex mutex2 = new InterProcessMutex(client, "/lock/a");
            boolean got = mutex2.acquire(1, TimeUnit.SECONDS);
            check("持锁时他人 acquire(1s) 超时 false", !got);
            mutex.release();
            boolean got2 = mutex2.acquire(2, TimeUnit.SECONDS);
            check("释放后可获取", got2);
            mutex2.release();

            // ---------- B. 可重入 ----------
            System.out.println("[B] 可重入");
            InterProcessMutex rm = new InterProcessMutex(client, "/lock/b");
            rm.acquire();
            rm.acquire();
            rm.acquire();
            check("3 次 acquire 后持有", rm.isOwnedByCurrentThread());
            rm.release();
            rm.release();
            check("2 次 release 后仍持有 (本地计数)", rm.isOwnedByCurrentThread());
            rm.release();
            check("3 次 release 后释放", !rm.isOwnedByCurrentThread());
            boolean otherGot = new InterProcessMutex(client, "/lock/b").acquire(2, TimeUnit.SECONDS);
            check("完全释放后他人可获", otherGot);

            // ---------- C. 公平 FIFO ----------
            System.out.println("[C] 公平 FIFO (按 ZK 创建序)");
            // 公平性语义: 获取顺序 = 顺序节点创建顺序 (StandardLockInternalsDriver createsTheLock)
            // 逐线程错开启动, 保证创建顺序确定
            InterProcessMutex fair = new InterProcessMutex(client, "/lock/fair");
            fair.acquire();
            int n = 5;
            CountDownLatch go2 = new CountDownLatch(1);
            List<Integer> order = new ArrayList<>();
            List<Thread> waiters = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                final int id = i;
                Thread t = new Thread(() -> {
                    InterProcessMutex m = new InterProcessMutex(client, "/lock/fair");
                    try {
                        m.acquire();
                        synchronized (order) {
                            order.add(id);
                        }
                        Thread.sleep(50);
                        m.release();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                t.start();
                waiters.add(t);
                Thread.sleep(200); // 逐线程错开, 让第 i 个线程的创建请求先到达 ZK
            }
            Thread.sleep(500);
            fair.release(); // 释放头锁, 让 5 个等待者依次进入
            for (Thread t : waiters) {
                t.join(15000);
            }
            boolean fifo = true;
            for (int i = 0; i < order.size(); i++) {
                if (order.get(i) != i) {
                    fifo = false;
                }
            }
            check("5 线程按创建序 FIFO (0,1,2,3,4 实际 " + order + ")", fifo && order.size() == 5);

            // ---------- D. 超时清理 ----------
            System.out.println("[D] 超时清理");
            InterProcessMutex lockD = new InterProcessMutex(client, "/lock/d");
            lockD.acquire();
            InterProcessMutex lockD2 = new InterProcessMutex(client, "/lock/d");
            boolean d2got = lockD2.acquire(500, TimeUnit.MILLISECONDS);
            check("超时失败", !d2got);
            check("超时后参与者只剩 1 个", lockD.getParticipantNodes().size() == 1);
            lockD.release();

            // ---------- E. 会话崩溃释放 ----------
            System.out.println("[E] 会话崩溃释放");
            CuratorFramework c2 = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            c2.start();
            c2.blockUntilConnected(10, TimeUnit.SECONDS);
            InterProcessMutex crashLock = new InterProcessMutex(c2, "/lock/crash");
            crashLock.acquire();
            c2.close(); // 直接关连接 — 会话由 ZK 服务端回收
            boolean crashGot = false;
            for (int i = 0; i < 80; i++) { // 最多等 8s
                InterProcessMutex m = new InterProcessMutex(client, "/lock/crash");
                if (m.acquire(100, TimeUnit.MILLISECONDS)) {
                    crashGot = true;
                    m.release();
                    break;
                }
                Thread.sleep(100);
            }
            check("客户端关闭后锁被服务端回收", crashGot);

            // ---------- F. 读写锁 ----------
            System.out.println("[F] 读写锁");
            InterProcessReadWriteLock rwl = new InterProcessReadWriteLock(client, "/lock/rw");
            rwl.readLock().acquire();
            boolean r2 = rwl.readLock().acquire(1, TimeUnit.SECONDS);
            check("多读者并存", r2);
            boolean w = rwl.writeLock().acquire(500, TimeUnit.MILLISECONDS);
            check("读者存在时写者被拒", !w);
            rwl.readLock().release();
            rwl.readLock().release();
            boolean w2 = rwl.writeLock().acquire(2, TimeUnit.SECONDS);
            check("读者全走后写者可获", w2);
            boolean r3 = rwl.readLock().acquire(1, TimeUnit.SECONDS);
            check("写者持有时可再获读锁 (降级前奏)", r3);
            rwl.readLock().release();
            rwl.writeLock().release();
            boolean r4 = rwl.readLock().acquire(2, TimeUnit.SECONDS);
            check("写者释放后读者可获", r4);
            rwl.readLock().release();

            // ---------- G. 信号量 ----------
            System.out.println("[G] 信号量");
            InterProcessSemaphoreV2 sem = new InterProcessSemaphoreV2(client, "/lock/sem", 2);
            Lease l1 = sem.acquire();
            Lease l2 = sem.acquire();
            check("2 租约已取", l1 != null && l2 != null);
            long start = System.currentTimeMillis();
            Lease l3 = null;
            try {
                l3 = sem.acquire(500, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                l3 = null;
            }
            check("第 3 个 acquire 超时失败", l3 == null);
            l2.close();
            l3 = sem.acquire(2, TimeUnit.SECONDS);
            check("归还后可再取", l3 != null);
            l1.close();
            l3.close();

            // ---------- H. MultiLock ----------
            System.out.println("[H] MultiLock");
            InterProcessMultiLock ml = new InterProcessMultiLock(
                    client, java.util.Arrays.asList("/lock/ml1", "/lock/ml2", "/lock/ml3"));
            InterProcessMutex blocker = new InterProcessMutex(client, "/lock/ml2");
            blocker.acquire();
            boolean mlGot = ml.acquire(500, TimeUnit.MILLISECONDS);
            check("MultiLock 部分失败返回 false", !mlGot);
            InterProcessMutex probeMl1 = new InterProcessMutex(client, "/lock/ml1");
            boolean ml1Free = probeMl1.acquire(2, TimeUnit.SECONDS);
            check("失败后 ml1 已释放 (可被他人获取)", ml1Free);
            probeMl1.release();
            blocker.release();
            boolean mlGot2 = ml.acquire(3, TimeUnit.SECONDS);
            check("全可用时 MultiLock 成功", mlGot2);
            ml.release();
            check("MultiLock 释放后 ml2 可获", new InterProcessMutex(client, "/lock/ml2").acquire(2, TimeUnit.SECONDS));

            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
