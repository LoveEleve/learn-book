import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;

/**
 * HarnessC5 — C-5 缓存与监听 8 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. NodeCache: 单节点增/改/删均触发 nodeChanged (NodeCache.java:250-300)
 *   B. PathChildrenCache NORMAL: CHILD_ADDED/UPDATED/REMOVED 事件 + getCurrentData (L660-703)
 *   C. BUILD_INITIAL_CACHE: 启动前已存在的子节点立即可见 (L322-325)
 *   D. POST_INITIALIZED_EVENT: INITIALIZED 事件只发一次 (L705-744)
 *   E. TreeCache: 子树 NODE_ADDED; 删父节点级联 NODE_REMOVED (L301-333)
 *   F. CuratorCache: NODE_CREATED/CHANGED/DELETED + get() (L231-245)
 *   G. CuratorCache SINGLE_NODE_CACHE: 只缓存单节点 (L54-59)
 *   H. 旧监听器桥接: forPathChildrenCache 包装
 */
public class HarnessC5 {
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
        System.out.println("== C-5 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A. NodeCache ----------
            System.out.println("[A] NodeCache");
            client.create().creatingParentContainersIfNeeded().forPath("/cache/node", "v0".getBytes());
            NodeCache nc = new NodeCache(client, "/cache/node");
            AtomicInteger ncCount = new AtomicInteger(0);
            nc.getListenable().addListener(() -> ncCount.incrementAndGet());
            nc.start();
            Thread.sleep(300);
            client.setData().forPath("/cache/node", "v1".getBytes());
            client.setData().forPath("/cache/node", "v2".getBytes());
            Thread.sleep(500);
            check("改 2 次触发 2 次 nodeChanged (实际 " + ncCount.get() + ")", ncCount.get() == 2);
            check("getCurrentData 反映最新值", new String(nc.getCurrentData().getData()).equals("v2"));
            client.delete().forPath("/cache/node");
            Thread.sleep(500);
            check("删除后 nodeChanged 再触发", ncCount.get() >= 3);
            check("删除后 getCurrentData null", nc.getCurrentData() == null);
            nc.close();

            // ---------- B. PathChildrenCache ----------
            System.out.println("[B] PathChildrenCache");
            client.create().creatingParentContainersIfNeeded().forPath("/pc");
            List<PathChildrenCacheEvent.Type> pEvents = new CopyOnWriteArrayList<>();
            PathChildrenCache pcc = new PathChildrenCache(client, "/pc", true);
            pcc.getListenable().addListener((c, event) -> pEvents.add(event.getType()));
            pcc.start(PathChildrenCache.StartMode.NORMAL);
            Thread.sleep(300);
            client.create().forPath("/pc/c1", "d1".getBytes());
            Thread.sleep(400); // 等 CHILD_ADDED 完成 getDataAndStat + 挂 data watcher
            client.setData().forPath("/pc/c1", "d2".getBytes());
            Thread.sleep(500);
            client.delete().forPath("/pc/c1");
            Thread.sleep(500);
            check("CHILD_ADDED 事件", pEvents.contains(PathChildrenCacheEvent.Type.CHILD_ADDED));
            check("CHILD_UPDATED 事件", pEvents.contains(PathChildrenCacheEvent.Type.CHILD_UPDATED));
            check("CHILD_REMOVED 事件", pEvents.contains(PathChildrenCacheEvent.Type.CHILD_REMOVED));
            pcc.close();

            // ---------- C. BUILD_INITIAL_CACHE ----------
            System.out.println("[C] BUILD_INITIAL_CACHE");
            client.create().creatingParentContainersIfNeeded().forPath("/pc2/a", "x".getBytes());
            client.create().forPath("/pc2/b", "y".getBytes());
            PathChildrenCache pcc2 = new PathChildrenCache(client, "/pc2", true);
            pcc2.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
            check("启动即见 2 个子节点", pcc2.getCurrentData().size() == 2);
            pcc2.close();

            // ---------- D. POST_INITIALIZED_EVENT ----------
            System.out.println("[D] POST_INITIALIZED_EVENT");
            client.create().creatingParentContainersIfNeeded().forPath("/pc3/x", "1".getBytes());
            client.create().forPath("/pc3/y", "2".getBytes());
            PathChildrenCache pcc3 = new PathChildrenCache(client, "/pc3", true);
            AtomicInteger initCount = new AtomicInteger(0);
            pcc3.getListenable().addListener((c, event) -> {
                if (event.getType() == PathChildrenCacheEvent.Type.INITIALIZED) {
                    initCount.incrementAndGet();
                }
            });
            pcc3.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            Thread.sleep(1000);
            check("INITIALIZED 触发一次 (实际 " + initCount.get() + ")", initCount.get() == 1);
            check("初始数据含 2 子节点", pcc3.getCurrentData().size() == 2);
            pcc3.close();

            // ---------- E. TreeCache ----------
            System.out.println("[E] TreeCache");
            client.create().creatingParentContainersIfNeeded().forPath("/tree");
            List<TreeCacheEvent.Type> tEvents = new CopyOnWriteArrayList<>();
            TreeCache tc = new TreeCache(client, "/tree");
            tc.getListenable().addListener((c, event) -> tEvents.add(event.getType()));
            tc.start();
            Thread.sleep(500);
            tEvents.clear(); // 清掉启动时根节点自身的 NODE_ADDED
            client.create().forPath("/tree/n1", "1".getBytes());
            Thread.sleep(400); // 等 n1 的 children watcher 就位
            client.create().forPath("/tree/n1/leaf", "2".getBytes());
            Thread.sleep(500);
            long addedCount = tEvents.stream().filter(e -> e == TreeCacheEvent.Type.NODE_ADDED).count();
            check("子树 NODE_ADDED ×2 (实际 " + addedCount + ", 事件: " + tEvents + ")", addedCount == 2);
            check("find 到叶子", tc.getCurrentData("/tree/n1/leaf") != null);
            client.delete().deletingChildrenIfNeeded().forPath("/tree/n1");
            Thread.sleep(500);
            check("删父节点后 find 空 (级联)", tc.getCurrentData("/tree/n1") == null && tc.getCurrentData("/tree/n1/leaf") == null);
            tc.close();

            // ---------- F. CuratorCache ----------
            System.out.println("[F] CuratorCache");
            List<CuratorCacheListener.Type> ccEvents = new CopyOnWriteArrayList<>();
            CuratorCache cc = CuratorCache.build(client, "/cc");
            cc.listenable().addListener(CuratorCacheListener.builder()
                    .forCreatesAndChanges((oldData, data) -> ccEvents.add(CuratorCacheListener.Type.NODE_CREATED))
                    .forDeletes(oldData -> ccEvents.add(CuratorCacheListener.Type.NODE_DELETED))
                    .forInitialized(() -> {})
                    .build());
            cc.start();
            Thread.sleep(300);
            client.create().creatingParentContainersIfNeeded().forPath("/cc/item", "v".getBytes());
            Thread.sleep(500);
            check("NODE_CREATED 事件", ccEvents.contains(CuratorCacheListener.Type.NODE_CREATED));
            check("get() 返回 Optional", cc.get("/cc/item").isPresent()
                    && new String(cc.get("/cc/item").get().getData()).equals("v"));
            client.delete().forPath("/cc/item");
            Thread.sleep(500);
            check("NODE_DELETED 事件", ccEvents.contains(CuratorCacheListener.Type.NODE_DELETED));
            check("删除后 get() 空", cc.get("/cc/item").isEmpty());
            cc.close();

            // ---------- G. SINGLE_NODE_CACHE ----------
            System.out.println("[G] SINGLE_NODE_CACHE");
            client.create().creatingParentContainersIfNeeded().forPath("/sn");
            client.create().forPath("/sn/child");
            CuratorCache single = CuratorCache.build(client, "/sn", CuratorCache.Options.SINGLE_NODE_CACHE);
            AtomicInteger singleEvents = new AtomicInteger(0);
            single.listenable().addListener(CuratorCacheListener.builder()
                    .forAll((t, o, d) -> singleEvents.incrementAndGet())
                    .build());
            single.start();
            Thread.sleep(500);
            singleEvents.set(0); // 清掉启动时根节点自身的 NODE_CREATED
            client.create().forPath("/sn/child2"); // 子节点变化不应触发单节点缓存
            Thread.sleep(500);
            check("子节点变化不触发单节点缓存事件 (实际 " + singleEvents.get() + ")", singleEvents.get() == 0);
            single.close();

            // ---------- H. 桥接 ----------
            System.out.println("[H] 旧监听器桥接");
            AtomicInteger bridgeCount = new AtomicInteger(0);
            CuratorCache bridgeCache = CuratorCache.build(client, "/bridge");
            bridgeCache.listenable().addListener(CuratorCacheListener.builder()
                    .forPathChildrenCache("/bridge", client, (c, event) -> bridgeCount.incrementAndGet())
                    .build());
            bridgeCache.start();
            Thread.sleep(300);
            client.create().creatingParentContainersIfNeeded().forPath("/bridge/b1", "1".getBytes());
            Thread.sleep(500);
            check("PathChildrenCache 监听器经 bridge 收到事件", bridgeCount.get() >= 1);
            bridgeCache.close();

            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
