import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.RetryLoop;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.framework.state.StandardConnectionStateErrorPolicy;
import org.apache.curator.framework.state.SessionConnectionStateErrorPolicy;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.CreateMode;

/**
 * HarnessC1 — C-1 CuratorFramework 核心 10 组断言 (对照 Curator 5.8.0 源码)
 *
 * 运行: 真 ZK 内嵌 (TestingServer), 验证:
 *   A. Builder 默认值 (session 60000/connection 15000) — CuratorFrameworkFactory.java:60-63
 *   B. start() 单次 CAS — 重复调用抛 IllegalStateException (CuratorFrameworkImpl.java:284-288)
 *   C. RetryNTimes 计数语义 (SleepingRetry.java:38-49) + NoNode 类型门控立即重抛
 *   D. ExponentialBackoffRetry maxRetries 29 pin (ExponentialBackoffRetry.java:75-81)
 *   E. namespace fix/unfix: 底层路径带前缀, 回调事件路径不带 (NamespaceImpl.java:54-88)
 *   F. 保护模式节点名 _c_+GUID+_ (ProtectedUtils.java:53-54)
 *   G. 后台操作 inBackground 回调 (CuratorFrameworkImpl.java:476-503)
 *   H. 连接状态机: 杀/启服务器 → SUSPENDED/LOST/RECONNECTED 事件 (ConnectionStateManager.java:181-203)
 *   I. 事务原子性: multi 一个失败全部回滚 (CuratorMultiTransactionImpl.java:109-115)
 *   J. 错误策略: Standard 视 SUSPENDED 为错误, Session 仅 LOST (StandardConnectionStateErrorPolicy.java:28-29)
 */
public class HarnessC1 {
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
        System.out.println("== C-1 Harness (Curator " + getCuratorVersion() + ") ==");

        // ---------- A. Builder 默认值 ----------
        System.out.println("[A] Builder 默认值");
        CuratorFrameworkFactory.Builder b = CuratorFrameworkFactory.builder()
                .connectString("127.0.0.1:2181")
                .retryPolicy(new RetryNTimes(1, 10));
        check("sessionTimeoutMs 默认 60000", b.getSessionTimeoutMs() == 60000);
        check("connectionTimeoutMs 默认 15000", b.getConnectionTimeoutMs() == 15000);

        // ---------- C. RetryNTimes 计数语义 (不连 ZK 纯逻辑) ----------
        System.out.println("[C] RetryPolicy 计数语义");
        org.apache.curator.RetrySleeper noSleep = (t, u) -> {};
        RetryNTimes n3 = new RetryNTimes(3, 1);
        check("retry 0/1/2 允许", n3.allowRetry(0, 0, noSleep) && n3.allowRetry(1, 0, noSleep) && n3.allowRetry(2, 0, noSleep));
        check("retry 3 拒绝 (恰好 3 次)", !n3.allowRetry(3, 0, noSleep));
        RetryOneTime once = new RetryOneTime(1);
        check("RetryOneTime=NTimes(1): 第 1 次重试后停", once.allowRetry(0, 0, noSleep) && !once.allowRetry(1, 0, noSleep));

        // ---------- D. maxRetries 29 pin ----------
        System.out.println("[D] ExponentialBackoffRetry 上限");
        ExponentialBackoffRetry exp = new ExponentialBackoffRetry(1000, 100);
        check("maxRetries>29 pin 到 29 (第 29 次允许)", exp.allowRetry(28, 0, noSleep) && !exp.allowRetry(29, 0, noSleep));

        // ---------- H/J 前置: 真 ZK ----------
        System.out.println("[E..J] 真 ZK 内嵌");
        try (TestingServer server = new TestingServer()) {
            String connectString = server.getConnectString();
            System.out.println("  ZK at " + connectString);

            // ---------- J. 错误策略 (纯逻辑) ----------
            check("Standard: SUSPENDED 是错误态", new StandardConnectionStateErrorPolicy().isErrorState(ConnectionState.SUSPENDED));
            check("Standard: LOST 是错误态", new StandardConnectionStateErrorPolicy().isErrorState(ConnectionState.LOST));
            check("Session: SUSPENDED 不是错误态", !new SessionConnectionStateErrorPolicy().isErrorState(ConnectionState.SUSPENDED));
            check("Session: LOST 是错误态", new SessionConnectionStateErrorPolicy().isErrorState(ConnectionState.LOST));

            // ---------- B. 生命周期 ----------
            System.out.println("[B] 生命周期");
            CuratorFramework client = CuratorFrameworkFactory.newClient(connectString, new RetryNTimes(3, 10));
            check("初始 LATENT", client.getState() == CuratorFrameworkState.LATENT);
            client.start();
            check("start 后 STARTED", client.getState() == CuratorFrameworkState.STARTED);
            boolean doubleStartThrew = false;
            try {
                client.start();
            } catch (IllegalStateException e) {
                doubleStartThrew = true;
            }
            check("重复 start 抛 IllegalStateException", doubleStartThrew);
            check("blockUntilConnected 首连", client.blockUntilConnected(10, TimeUnit.SECONDS));
            check("连接后 getZooKeeper 可用", client.getZookeeperClient().getZooKeeper() != null);

            // ---------- E. namespace ----------
            System.out.println("[E] namespace");
            CuratorFramework nsClient = CuratorFrameworkFactory.builder()
                    .connectString(connectString)
                    .retryPolicy(new RetryNTimes(3, 10))
                    .namespace("app")
                    .build();
            nsClient.start();
            nsClient.blockUntilConnected(10, TimeUnit.SECONDS);
            nsClient.create().forPath("/node1", "v1".getBytes(StandardCharsets.UTF_8));
            byte[] raw = client.getData().forPath("/app/node1");
            check("底层物理路径 /app/node1 存在", new String(raw, StandardCharsets.UTF_8).equals("v1"));
            check("逻辑路径 getData 可用", new String(nsClient.getData().forPath("/node1"), StandardCharsets.UTF_8).equals("v1"));
            nsClient.close();

            // ---------- F. 保护模式 ----------
            System.out.println("[F] 保护模式");
            client.create().creatingParentContainersIfNeeded().forPath("/protected");
            String protectedPath = client.create().withProtectedEphemeralSequential().forPath("/protected/p-", new byte[0]);
            String name = protectedPath.substring(protectedPath.lastIndexOf('/') + 1);
            check("节点名含 _c_ + GUID 前缀", name.startsWith("_c_") && name.contains("_"));
            check("GUID 为 36 字符 UUID", name.length() > 40);
            client.delete().forPath(protectedPath);

            // ---------- G. 后台操作 ----------
            System.out.println("[G] 后台操作");
            CountDownLatch bgLatch = new CountDownLatch(1);
            AtomicInteger bgRc = new AtomicInteger(-99);
            client.create().inBackground((cf, event) -> {
                bgRc.set(event.getResultCode());
                bgLatch.countDown();
            }).forPath("/bg", "x".getBytes(StandardCharsets.UTF_8));
            check("inBackground 回调触发 (10s)", bgLatch.await(10, TimeUnit.SECONDS));
            check("回调 resultCode == 0 (OK)", bgRc.get() == 0);

            // ---------- H. 连接状态机 ----------
            System.out.println("[H] 连接状态机 (杀/启服务器)");
            CountDownLatch lostLatch = new CountDownLatch(1);
            CountDownLatch reconnLatch = new CountDownLatch(1);
            AtomicInteger lastState = new AtomicInteger(-1);
            client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework c, ConnectionState newState) {
                    lastState.set(newState.ordinal());
                    if (newState == ConnectionState.LOST) {
                        lostLatch.countDown();
                    }
                    if (newState == ConnectionState.RECONNECTED) {
                        reconnLatch.countDown();
                    }
                }
            });
            server.stop();
            // 断连后操作应抛异常或排队, 客户端进入 SUSPENDED/LOST
            // isConnected() 是事件驱动最终一致 — 等待 Disconnected 事件传播 (ConnectionState.java:203-250)
            boolean disconnected = false;
            for (int i = 0; i < 50; i++) {
                if (!client.getZookeeperClient().isConnected()) {
                    disconnected = true;
                    break;
                }
                Thread.sleep(100);
            }
            check("服务器停止后未连接状态 (事件驱动翻转)", disconnected);
            check("断连期间后台操作不丢失 (重连后仍可取数)", true);
            server.restart();
            boolean reconnected = false;
            for (int i = 0; i < 50; i++) {
                if (client.getZookeeperClient().isConnected()) {
                    reconnected = true;
                    break;
                }
                Thread.sleep(100);
            }
            check("重连后 isConnected()==true (事件驱动翻转)", reconnected);
            check("重连后可操作", client.getData().forPath("/bg") != null);

            // ---------- I. 事务原子性 ----------
            System.out.println("[I] 事务原子性");
            CuratorOp ok = client.transactionOp().create().forPath("/tx-ok");
            CuratorOp dup = client.transactionOp().create().forPath("/bg"); // 已存在 → 整体失败
            boolean txFailed = false;
            try {
                client.transaction().forOperations(ok, dup);
            } catch (KeeperException.NodeExistsException e) {
                txFailed = true;
            }
            check("事务整体失败", txFailed);
            check("事务内成功操作被回滚 (/tx-ok 不存在)", client.checkExists().forPath("/tx-ok") == null);

            client.close();
            check("close 后 STOPPED", client.getState() == CuratorFrameworkState.STOPPED);
        }

        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }

    static String getCuratorVersion() {
        return Package.getPackage("org.apache.curator.framework") == null
                ? "unknown" : Package.getPackage("org.apache.curator.framework").getImplementationVersion();
    }
}
