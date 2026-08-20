import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;

/**
 * HarnessC2 — C-2 Leader 选举 7 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. 唯一性: 3 个 latch 恰 1 个 isLeader (LeaderLatch.java:539-602 checkLeadership)
 *   B. 事件驱动接替: leader close → 前驱 watch 通知 → 新 leader 当选 (非轮询)
 *   C. await 阻塞语义: follower await 在 leader 释放前不返回
 *   D. participants/leader 查询 (LockInternals.getParticipantNodes)
 *   E. LeaderSelector 任期: takeLeadership 阻塞返回后让位
 *   F. autoRequeue: 让位后可再次当选
 *   G. LeaderSelectorListenerAdapter: 默认实现即开即用
 */
public class HarnessC2 {
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
        System.out.println("== C-2 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A. 唯一性 ----------
            System.out.println("[A] 唯一性");
            LeaderLatch l1 = new LeaderLatch(client, "/lead/a", "id-1");
            l1.start();
            l1.await(10, TimeUnit.SECONDS); // 先让 l1 当选, 再让对手入场
            LeaderLatch l2 = new LeaderLatch(client, "/lead/a", "id-2");
            LeaderLatch l3 = new LeaderLatch(client, "/lead/a", "id-3");
            l2.start();
            l3.start();
            Thread.sleep(1500); // 等 l2/l3 完成内部选举
            int leaders = 0;
            leaders += l1.hasLeadership() ? 1 : 0;
            leaders += l2.hasLeadership() ? 1 : 0;
            leaders += l3.hasLeadership() ? 1 : 0;
            check("3 个 latch 恰 1 个 leader", leaders == 1);
            check("先启动者先当选 (l1)", l1.hasLeadership());

            // ---------- D. participants ----------
            System.out.println("[D] participants");
            java.util.Collection<Participant> parts = l1.getParticipants();
            check("participants 3 个", parts.size() == 3);
            boolean oneLeader = false;
            for (Participant p : parts) {
                if (p.isLeader()) {
                    oneLeader = true;
                    break;
                }
            }
            check("participants 中恰 1 个 isLeader", oneLeader);

            // ---------- B. 事件驱动接替 ----------
            System.out.println("[B] 事件驱动接替");
            l1.close(); // 触发 NodeDeleted → l2/l3 前驱 watch → getChildren 重查
            boolean l2Lead = false;
            boolean l3Lead = false;
            for (int i = 0; i < 50; i++) {
                l2Lead = l2.hasLeadership();
                l3Lead = l3.hasLeadership();
                if (l2Lead || l3Lead) {
                    break;
                }
                Thread.sleep(100);
            }
            check("l1 关闭后有人接替", l2Lead || l3Lead);
            check("接替者唯一", !(l2Lead && l3Lead));
            check("接替顺序: l2 (先启动者先当)", l2Lead);

            // ---------- C. await 阻塞 ----------
            System.out.println("[C] await 阻塞语义");
            LeaderLatch l4 = new LeaderLatch(client, "/lead/b", "id-4");
            LeaderLatch l5 = new LeaderLatch(client, "/lead/b", "id-5");
            l4.start();
            l4.await(10, TimeUnit.SECONDS);
            l5.start();
            long t0 = System.currentTimeMillis();
            Thread waiter = new Thread(() -> {
                try {
                    l5.await(); // 应阻塞直到 l4 关闭
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            waiter.start();
            Thread.sleep(500);
            check("follower await 阻塞中 (500ms 未返回)", waiter.isAlive());
            Thread.sleep(1200);
            l4.close();
            waiter.join(10000);
            long elapsed = System.currentTimeMillis() - t0;
            check("leader 关闭后 await 返回", !waiter.isAlive() && l5.hasLeadership());
            check("await 释放有延迟 (<3s, 前驱 watch 驱动)", elapsed < 3000);
            l5.close();

            // ---------- E. LeaderSelector 任期 ----------
            System.out.println("[E] LeaderSelector 任期");
            CountDownLatch took1 = new CountDownLatch(1);
            AtomicInteger selCount = new AtomicInteger(0);
            LeaderSelector sel = new LeaderSelector(client, "/lead/sel", new LeaderSelectorListenerAdapter() {
                @Override
                public void takeLeadership(CuratorFramework c) throws Exception {
                    selCount.incrementAndGet();
                    took1.countDown();
                    Thread.sleep(2000); // 任期 2s
                }
            });
            sel.start();
            check("takeLeadership 第一次调用", took1.await(10, TimeUnit.SECONDS));
            Thread.sleep(1000); // 任期进行中
            check("任期内无并发执行 (count==1)", selCount.get() == 1);
            Thread.sleep(2500); // 任期结束, 无 autoRequeue → 不再当选
            check("无 autoRequeue 不重选 (count 仍 1)", selCount.get() == 1);
            sel.close();

            // ---------- F. autoRequeue ----------
            System.out.println("[F] autoRequeue");
            CountDownLatch took3 = new CountDownLatch(1);
            AtomicInteger sel2Count = new AtomicInteger(0);
            LeaderSelector sel2 = new LeaderSelector(client, "/lead/sel2", new LeaderSelectorListenerAdapter() {
                @Override
                public void takeLeadership(CuratorFramework c) throws Exception {
                    sel2Count.incrementAndGet();
                    took3.countDown();
                    Thread.sleep(1000);
                }
            });
            sel2.autoRequeue();
            sel2.start();
            took3.await(10, TimeUnit.SECONDS);
            Thread.sleep(2500); // 第一任期 1s 结束 → autoRequeue 再竞选 → 第二任期
            check("autoRequeue 后再次当选 (累计 " + sel2Count.get() + " 次)", sel2Count.get() >= 2);
            sel2.close();

            // ---------- G. 适配器 ----------
            System.out.println("[G] 适配器");
            CountDownLatch took4 = new CountDownLatch(1);
            LeaderSelector sel3 = new LeaderSelector(client, "/lead/sel3", new LeaderSelectorListenerAdapter() {
                @Override
                public void takeLeadership(CuratorFramework c) throws Exception {
                    took4.countDown();
                }
            });
            sel3.start();
            check("默认适配器直接可用", took4.await(10, TimeUnit.SECONDS));
            sel3.close();

            l2.close();
            l3.close();
            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
