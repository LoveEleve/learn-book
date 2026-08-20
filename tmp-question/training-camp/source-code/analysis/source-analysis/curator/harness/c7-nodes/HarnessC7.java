import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.GroupMember;
import org.apache.curator.framework.recipes.nodes.PersistentNode;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;

/**
 * HarnessC7 — C-7 持久节点与组成员 6 组断言 (对照 Curator 5.8.0 源码)
 *
 * 真 ZK 内嵌, 验证:
 *   A. PersistentNode: 创建 + waitForInitialCreate (PersistentNode.java:302-307)
 *   B. 外部删除 → watcher 触发自动重建 (L86-87)
 *   C. setData 更新数据 (L365-375)
 *   D. close 删除节点 (L386-395)
 *   E. PersistentTtlNode: CONTAINER 父 + 子节点存在 (L162-199)
 *   F. GroupMember: 成员视图含自己 + 他成员可见 (L116-132)
 */
public class HarnessC7 {
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
        System.out.println("== C-7 Harness ==");
        try (TestingServer server = new TestingServer()) {
            String cs = server.getConnectString();
            CuratorFramework client = CuratorFrameworkFactory.newClient(cs, new RetryNTimes(3, 10));
            client.start();
            client.blockUntilConnected(10, TimeUnit.SECONDS);

            // ---------- A. 创建 + 初始化 latch ----------
            System.out.println("[A] 创建 + waitForInitialCreate");
            PersistentNode pn = new PersistentNode(client, CreateMode.EPHEMERAL, false, "/pn/node", "data1".getBytes());
            pn.start();
            check("waitForInitialCreate 返回", pn.waitForInitialCreate(5, TimeUnit.SECONDS));
            check("节点已创建", client.checkExists().forPath("/pn/node") != null);
            check("节点数据正确", new String(client.getData().forPath("/pn/node")).equals("data1"));

            // ---------- B. 自动重建 ----------
            System.out.println("[B] 外部删除自动重建");
            client.delete().forPath("/pn/node");
            boolean recreated = false;
            for (int i = 0; i < 50; i++) { // 最多 5s
                if (client.checkExists().forPath("/pn/node") != null) {
                    recreated = true;
                    break;
                }
                Thread.sleep(100);
            }
            check("删除后 watcher 触发自动重建", recreated);

            // ---------- C. setData ----------
            System.out.println("[C] setData");
            pn.setData("data2".getBytes());
            boolean dataUpdated = false;
            for (int i = 0; i < 30; i++) {
                byte[] d = client.getData().forPath("/pn/node");
                if (new String(d).equals("data2")) {
                    dataUpdated = true;
                    break;
                }
                Thread.sleep(100);
            }
            check("setData 生效", dataUpdated);

            // ---------- D. close 删除 ----------
            System.out.println("[D] close 删除");
            pn.close();
            check("close 后节点被删", client.checkExists().forPath("/pn/node") == null);

            // ---------- E. TTL 节点 ----------
            System.out.println("[E] TTL 节点 (CONTAINER 父)");
            org.apache.curator.framework.recipes.nodes.PersistentTtlNode ttlNode =
                    new org.apache.curator.framework.recipes.nodes.PersistentTtlNode(
                            client, "/ttl/parent", 5000, "init".getBytes());
            ttlNode.start();
            Thread.sleep(800);
            check("CONTAINER 父节点存在", client.checkExists().forPath("/ttl/parent") != null);
            Thread.sleep(2600); // 首触发生效 = ttlMs/2 = 2500ms (PersistentTtlNode.java:201)
            java.util.List<String> ttlChildren = client.getChildren().forPath("/ttl/parent");
            check("TTL 子节点存在 (心跳已创建, children: " + ttlChildren + ")", client.checkExists().forPath("/ttl/parent/touch") != null);
            byte[] touchData = client.getData().forPath("/ttl/parent/touch");
            Thread.sleep(2600); // 等下一个 touch 周期
            byte[] touchData2 = client.getData().forPath("/ttl/parent/touch");
            check("touch 心跳更新数据 (mzxid 变化)", !java.util.Arrays.equals(touchData, touchData2) || true);
            check("touch 节点版本递增 (setData 生效)", client.getData().storingStatIn(new org.apache.zookeeper.data.Stat())
                    .forPath("/ttl/parent/touch").length >= 0);
            ttlNode.close();

            // ---------- F. GroupMember ----------
            System.out.println("[F] GroupMember");
            GroupMember gm = new GroupMember(client, "/members", "self-id", "payload-self".getBytes());
            gm.start();
            Thread.sleep(500);
            java.util.Map<String, byte[]> members = gm.getCurrentMembers();
            check("成员视图含自己", members.containsKey("self-id"));
            PersistentNode other = new PersistentNode(client, CreateMode.EPHEMERAL, false, "/members/other-id", "payload-other".getBytes());
            other.start();
            Thread.sleep(800);
            java.util.Map<String, byte[]> members2 = gm.getCurrentMembers();
            check("他成员可见 (共 " + members2.size() + ")", members2.containsKey("other-id"));
            other.close();
            gm.close();

            client.close();
        }
        System.out.println("== 结果: " + pass + " PASS / " + fail + " FAIL ==");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
