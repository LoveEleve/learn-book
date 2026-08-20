import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.barriers.DistributedBarrier;
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier;
import org.apache.curator.framework.recipes.queue.DistributedDelayQueue;
import org.apache.curator.framework.recipes.queue.DistributedIdQueue;
import org.apache.curator.framework.recipes.queue.DistributedPriorityQueue;
import org.apache.curator.framework.recipes.queue.DistributedQueue;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.recipes.queue.QueueSerializer;
import org.apache.curator.framework.recipes.queue.SimpleDistributedQueue;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;

/**
 * HarnessC4 — C-4 队列与屏障 9 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. FIFO: put 5 消费 5 顺序保持 (DistributedQueue runLoop L457-490 + 序号排序)
 *   B. 消费回调: QueueConsumer.consumeMessage 触发次数
 *   C. flushPuts: 后台 put 后 flushPuts 等待全部提交 (L240-257)
 *   D. 优先级: 低优先级先出 (名字编码 L186-192)
 *   E. 延迟队列: 延迟未到不消费 (getDelay L75-83)
 *   F. IdQueue: put(id) + remove(id) (L168-183)
 *   G. SimpleDistributedQueue: offer/take 同步阻塞 (L171-208)
 *   H. DistributedBarrier: set→阻塞→remove→放行 (L101-125)
 *   I. DistributedDoubleBarrier: N 成员 enter→ready→leave (L260-292)
 */
public class HarnessC4 {
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
        System.out.println("== C-4 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A/B. FIFO + 消费回调 ----------
            System.out.println("[A/B] FIFO + 消费回调");
            List<String> consumed = new ArrayList<>();
            QueueConsumer<String> consumer = new QueueConsumer<String>() {
                @Override
                public void consumeMessage(String message) throws Exception {
                    synchronized (consumed) {
                        consumed.add(message);
                    }
                }

                @Override
                public void stateChanged(CuratorFramework c, ConnectionState newState) {}
            };
            DistributedQueue<String> q = QueueBuilder.builder(client, consumer, new QueueSerializer<String>() {
                @Override
                public byte[] serialize(String item) {
                    return item.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(byte[] bytes) {
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            }, "/queue/a").buildQueue();
            q.start();
            for (int i = 0; i < 5; i++) {
                q.put("msg-" + i);
            }
            Thread.sleep(1500);
            synchronized (consumed) {
                check("消费 5 条", consumed.size() == 5);
                boolean fifo = true;
                for (int i = 0; i < 5; i++) {
                    if (!consumed.get(i).equals("msg-" + i)) {
                        fifo = false;
                    }
                }
                check("FIFO 顺序 (" + consumed + ")", fifo);
            }
            q.close();

            // ---------- C. flushPuts ----------
            System.out.println("[C] flushPuts");
            DistributedQueue<String> q2 = QueueBuilder.builder(client, consumer, new QueueSerializer<String>() {
                @Override
                public byte[] serialize(String item) {
                    return item.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(byte[] bytes) {
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            }, "/queue/c")
                    .putInBackground(true)
                    .buildQueue();
            q2.start();
            q2.put("x1");
            q2.put("x2");
            boolean flushed = q2.flushPuts(5, TimeUnit.SECONDS);
            Thread.sleep(500);
            synchronized (consumed) {
                check("flushPuts 后后台 put 全部提交 (消费端可见 x1/x2: " + consumed + ")", flushed && consumed.contains("x1") && consumed.contains("x2"));
            }
            q2.close();

            // ---------- D. 优先级 ----------
            System.out.println("[D] 优先级");
            List<String> prio = new ArrayList<>();
            QueueConsumer<String> pConsumer = new QueueConsumer<String>() {
                @Override
                public void consumeMessage(String message) throws Exception {
                    synchronized (prio) {
                        prio.add(message);
                    }
                }

                @Override
                public void stateChanged(CuratorFramework c, ConnectionState newState) {}
            };
            DistributedPriorityQueue<String> pq = QueueBuilder.builder(client, pConsumer, new QueueSerializer<String>() {
                @Override
                public byte[] serialize(String item) {
                    return item.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(byte[] bytes) {
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            }, "/queue/pq")
                    .buildPriorityQueue(0);
            pq.start();
            pq.put("low", 10);
            pq.put("high", 0);
            pq.put("mid", 5);
            Thread.sleep(1500);
            synchronized (prio) {
                check("优先级顺序 high,mid,low (" + prio + ")", prio.equals(List.of("high", "mid", "low")));
            }
            pq.close();

            // ---------- E. 延迟队列 ----------
            System.out.println("[E] 延迟队列");
            List<String> delayed = new ArrayList<>();
            QueueConsumer<String> dConsumer = new QueueConsumer<String>() {
                @Override
                public void consumeMessage(String message) throws Exception {
                    synchronized (delayed) {
                        delayed.add(message);
                    }
                }

                @Override
                public void stateChanged(CuratorFramework c, ConnectionState newState) {}
            };
            DistributedDelayQueue<String> dq = QueueBuilder.builder(client, dConsumer, new QueueSerializer<String>() {
                @Override
                public byte[] serialize(String item) {
                    return item.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(byte[] bytes) {
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            }, "/queue/dq")
                    .buildDelayQueue();
            dq.start();
            dq.put("later", System.currentTimeMillis() + 2000);
            dq.put("now", System.currentTimeMillis());
            Thread.sleep(800);
            synchronized (delayed) {
                check("延迟 2s 的消息 800ms 内未消费", !delayed.contains("later"));
                check("立即到期消息已消费", delayed.contains("now"));
            }
            Thread.sleep(2000);
            synchronized (delayed) {
                check("2s 后延迟消息被消费", delayed.contains("later"));
            }
            dq.close();

            // ---------- F. IdQueue ----------
            System.out.println("[F] IdQueue (producer-only, consumer=null)");
            DistributedIdQueue<String> iq = QueueBuilder.builder(client, null, new QueueSerializer<String>() {
                @Override
                public byte[] serialize(String item) {
                    return item.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }

                @Override
                public String deserialize(byte[] bytes) {
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            }, "/queue/iq").buildIdQueue();
            iq.start();
            iq.put("a", "id-a");
            iq.put("b", "id-b");
            Thread.sleep(500); // 等节点创建 (无 consumer, 不会消费)
            int removed = iq.remove("id-a");
            check("remove(id-a) 成功 (返回 " + removed + ")", removed == 1);
            Thread.sleep(300);
            check("剩余 1 节点 (id-b)", client.getChildren().forPath("/queue/iq").size() == 1);
            iq.close();

            // ---------- G. SimpleDistributedQueue ----------
            System.out.println("[G] SimpleDistributedQueue");
            SimpleDistributedQueue sq = new SimpleDistributedQueue(client, "/queue/sq");
            sq.offer("hello".getBytes());
            byte[] head = sq.peek();
            check("peek 可见", new String(head).equals("hello"));
            byte[] taken = sq.take();
            check("take 取出", new String(taken).equals("hello"));
            check("take 后队列空", sq.peek() == null);

            // ---------- H. DistributedBarrier ----------
            System.out.println("[H] DistributedBarrier");
            DistributedBarrier barrier = new DistributedBarrier(client, "/barrier/b1");
            barrier.setBarrier();
            Thread bThread = new Thread(() -> {
                try {
                    barrier.waitOnBarrier();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            bThread.start();
            Thread.sleep(600);
            check("屏障关闭时 waitOnBarrier 阻塞", bThread.isAlive());
            barrier.removeBarrier();
            bThread.join(10000);
            check("removeBarrier 后放行", !bThread.isAlive());

            // ---------- I. DistributedDoubleBarrier ----------
            System.out.println("[I] DistributedDoubleBarrier");
            int n = 3;
            List<Thread> members = new ArrayList<>();
            CountDownLatch entered = new CountDownLatch(1);
            CountDownLatch allLeft = new CountDownLatch(1);
            AtomicInteger readyCount = new AtomicInteger(0);
            for (int i = 0; i < n; i++) {
                Thread t = new Thread(() -> {
                    try {
                        DistributedDoubleBarrier db = new DistributedDoubleBarrier(client, "/barrier/db", n);
                        db.enter();
                        if (readyCount.incrementAndGet() == 1) {
                            entered.countDown();
                        }
                        Thread.sleep(300);
                        db.leave();
                        if (readyCount.incrementAndGet() >= n * 2) {
                            allLeft.countDown();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                t.start();
                members.add(t);
            }
            check("全部成员 enter 后才放行 (ready)", entered.await(10, TimeUnit.SECONDS));
            check("全部成员 leave 后完成", allLeft.await(15, TimeUnit.SECONDS));
            for (Thread t : members) {
                t.join(5000);
            }
            check("成员线程全部结束", members.stream().noneMatch(Thread::isAlive));
            check("leave 后 ready 节点被清理", client.checkExists().forPath("/barrier/db/ready") == null);

            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
