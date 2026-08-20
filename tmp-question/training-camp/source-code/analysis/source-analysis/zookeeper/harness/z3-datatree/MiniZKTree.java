import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MiniZKTree — Z-3 DataTree 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. 扁平树 + 父节点锁: createNode 路径拆分 → synchronized(parent) → nodes.put
 *      (DataTree.java:433-483 createNode)
 *   B. 版本乐观锁: setData version 不匹配 → BadVersion 语义
 *      (DataTree.java:627+ setData)
 *   C. cversion/pzxid 单调保护: 仅当 parentCVersion > 现有才更新 (replay 防回退)
 *      (DataTree.java:470-478, 548-553)
 *   D. 分类维护 + 快照重建: ephemerals 登记/清理 + 反序列化父链重建
 *      (DataTree.java:484-494, 575-589, 1350-1388)
 *
 * 纯内存模拟, 保留核心判定数学与控制流。
 */
public class MiniZKTree {

    /** DataNode 简化 (对照 DataNode: data/stat/children) */
    public static class Node {
        byte[] data;
        long czxid, mzxid, pzxid;
        int cversion, version;
        long ephemeralOwner;      // -1 非临时 / >0 会话 / 特值容器与 ttl
        final Set<String> children = new HashSet<>();
        Node(byte[] data, long ephemeralOwner) {
            this.data = data; this.ephemeralOwner = ephemeralOwner;
        }
    }

    public static class Tree {
        final Map<String, Node> nodes = new HashMap<>();       // 扁平路径 → 节点
        final Map<Long, Set<String>> ephemerals = new HashMap<>();  // session → paths
        final Set<String> containers = new HashSet<>();
        final Set<String> ttls = new HashSet<>();

        public Node get(String path) { return nodes.get(path); }

        /** A: createNode (父锁语义 + 分类登记) */
        public String createNode(String path, byte[] data, long ephemeralOwner, int parentCVersion) {
            int lastSlash = path.lastIndexOf('/');
            String parentName = path.substring(0, lastSlash);
            String childName = path.substring(lastSlash + 1);
            Node parent = nodes.get(parentName);
            if (parent == null) throw new IllegalStateException("NoNode");
            synchronized (parent) {                       // 父节点锁
                if (nodes.containsKey(path)) throw new IllegalStateException("NodeExists");
                if (parentCVersion == -1) parentCVersion = parent.cversion + 1;
                // C: cversion 单调保护 (replay 防回退)
                if (parentCVersion > parent.cversion) {
                    parent.cversion = parentCVersion;
                    parent.pzxid = Long.MAX_VALUE;          // 简化: zxid 单调
                }
                Node child = new Node(data, ephemeralOwner);
                parent.children.add(childName);
                nodes.put(path, child);
                // D: 分类登记
                if (ephemeralOwner == Long.MIN_VALUE) containers.add(path);
                else if (ephemeralOwner == Long.MIN_VALUE + 1) ttls.add(path);
                else if (ephemeralOwner > 0) ephemerals.computeIfAbsent(ephemeralOwner, k -> new HashSet<>()).add(path);
                return path;
            }
        }

        /** B: setData 乐观锁 */
        public Node setData(String path, byte[] data, int version) {
            Node n = nodes.get(path);
            if (n == null) throw new IllegalStateException("NoNode");
            if (version != -1 && n.version != version) throw new IllegalStateException("BadVersion");
            n.data = data;
            n.version++;
            return n;
        }

        /** D: deleteNode (分类清理) */
        public void deleteNode(String path) {
            Node node = nodes.get(path);
            if (node == null) throw new IllegalStateException("NoNode");
            if (!node.children.isEmpty()) throw new IllegalStateException("NotEmpty");
            int lastSlash = path.lastIndexOf('/');
            Node parent = nodes.get(path.substring(0, lastSlash));
            parent.children.remove(path.substring(lastSlash + 1));
            nodes.remove(path);
            long owner = node.ephemeralOwner;
            if (owner == Long.MIN_VALUE) containers.remove(path);
            else if (owner == Long.MIN_VALUE + 1) ttls.remove(path);
            else if (owner > 0) {
                Set<String> paths = ephemerals.get(owner);
                if (paths != null) paths.remove(path);
            }
        }

        /** D: 快照反序列化父链重建 (路径序保证父先于子); 根双 key ("+"/) 对照 DataTree:288-289 */
        public void deserialize(String[] paths) {
            nodes.clear(); ephemerals.clear(); containers.clear(); ttls.clear();
            for (String path : paths) {
                if ("/".equals(path)) {
                    Node root = new Node(new byte[0], 0);
                    nodes.put("", root);       // 根 key "" (createNode parentName 语义)
                    nodes.put("/", root);      // 根 key "/" (快照序列化语义)
                    continue;
                }
                int lastSlash = path.lastIndexOf('/');
                Node parent = nodes.get(path.substring(0, lastSlash));
                if (parent == null) throw new IllegalStateException("Invalid Datatree: " + path);
                Node node = new Node(new byte[0], 0);
                parent.children.add(path.substring(lastSlash + 1));
                nodes.put(path, node);
            }
        }
    }

    public static void main(String[] args) {
        Tree t = new Tree();
        t.deserialize(new String[]{"/"});

        // --- A: createNode + 父锁 ---
        t.createNode("/a", new byte[]{1}, 0, -1);
        t.createNode("/a/b", new byte[]{2}, 0, -1);
        assertTrue(t.get("/a/b") != null && t.get("/a").children.contains("b"), "创建 /a/b 成功且父链正确");
        try {
            t.createNode("/a", new byte[]{1}, 0, -1);
            throw new AssertionError("[FAIL] 重复创建应 NodeExists");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().equals("NodeExists"), "重复创建 → NodeExists");
        }
        try {
            t.createNode("/x/y", new byte[]{1}, 0, -1);
            throw new AssertionError("[FAIL] 父缺失应 NoNode");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().equals("NoNode"), "父缺失 → NoNode");
        }
        System.out.println("[A] createNode 父锁/校验 3/3 OK");

        // --- B: 版本乐观锁 ---
        Node n = t.get("/a");
        n.version = 1;
        t.setData("/a", new byte[]{9}, 1);               // 版本匹配
        assertTrue(t.get("/a").version == 2, "setData 版本匹配 → version++");
        try {
            t.setData("/a", new byte[]{9}, 5);           // 版本不匹配
            throw new AssertionError("[FAIL] 版本不符应 BadVersion");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().equals("BadVersion"), "版本不符 → BadVersion");
        }
        System.out.println("[B] setData 乐观锁 2/2 OK");

        // --- C: cversion/pzxid 单调保护 ---
        Node parent = t.get("/a");
        int cvBefore = parent.cversion;
        long pzBefore = parent.pzxid;
        // 模拟 replay: 旧 cversion 事务重放 → 不得回退
        t.createNode("/a/c", new byte[]{3}, 0, cvBefore - 5);
        assertTrue(parent.cversion == cvBefore, "旧 cversion replay 不回退 (保持 " + cvBefore + ")");
        t.createNode("/a/d", new byte[]{4}, 0, -1);      // 正常 +1
        assertTrue(parent.cversion == cvBefore + 1, "新 create cversion +1");
        System.out.println("[C] cversion 单调保护 2/2 OK");

        // --- D: 分类维护 + 快照重建 ---
        long sess1 = 100L;
        t.createNode("/ep1", new byte[]{1}, sess1, -1);
        t.createNode("/ep2", new byte[]{1}, sess1, -1);
        assertTrue(t.ephemerals.get(sess1).size() == 2, "ephemeral 登记 2 条");
        t.deleteNode("/ep1");
        assertTrue(t.ephemerals.get(sess1).size() == 1, "delete 清理 ephemeral");
        Tree t2 = new Tree();
        t2.deserialize(new String[]{"/", "/a", "/a/b", "/a/c", "/a/d"});  // 父先子后
        assertTrue(t2.get("/a").children.size() == 3 && t2.get("/a/b") != null, "反序列化父链重建 3 子");
        try {
            new Tree().deserialize(new String[]{"/", "/a/b"});
            throw new AssertionError("[FAIL] 父缺失应 Invalid Datatree");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("Invalid Datatree"), "父缺失 → Invalid Datatree");
        }
        System.out.println("[D] 分类维护 + 快照重建 4/4 OK");

        System.out.println("MiniZKTree 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
