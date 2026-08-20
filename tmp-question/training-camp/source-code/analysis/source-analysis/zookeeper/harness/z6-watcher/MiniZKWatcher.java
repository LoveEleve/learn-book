import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * MiniZKWatcher — Z-6 Watcher 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. 双向注册: watchTable (path→watchers) + watch2Paths (watcher→paths) 一致
 *      (WatchManager.java:50-52,75-110)
 *   B. 一次性触发: STANDARD 触发即移除; PERSISTENT_RECURSIVE 保持
 *      (WatchManager.java:161-170)
 *   C. 双向清理: removeWatcher(watcher) 全清 + 空路径删除
 *      (WatchManager.java:113-132)
 *   D. 位图压缩: watcher bit id + BitHashSet — contains O(1)
 *      (WatchManagerOptimized.java:61-64)
 *
 * 纯内存模拟, 保留核心判定数学与控制流。
 */
public class MiniZKWatcher {

    enum Mode { STANDARD, PERSISTENT, PERSISTENT_RECURSIVE }

    static class Watcher {
        final int id;
        final java.util.List<String> fired = new java.util.ArrayList<>();  // 触发记录 (可重复)
        Watcher(int id) { this.id = id; }
    }

    /** 简化 WatchManager: 双向 + 触发移除 */
    public static class WatchManager {
        final Map<String, Set<Watcher>> watchTable = new HashMap<>();      // path → watchers
        final Map<Watcher, Map<String, Mode>> watch2Paths = new HashMap<>(); // watcher → path+mode

        public void addWatch(String path, Watcher w, Mode mode) {
            watchTable.computeIfAbsent(path, k -> new HashSet<>()).add(w);
            watch2Paths.computeIfAbsent(w, k -> new HashMap<>()).put(path, mode);
        }

        /** B: 触发 — 对照 WatchManager:161-170 */
        public Set<Watcher> trigger(String path) {
            Set<Watcher> fired = new HashSet<>();
            // 简化: 只触发路径自身 (递归父路径面略)
            Set<Watcher> watchers = watchTable.get(path);
            if (watchers == null) return fired;
            for (Watcher w : new HashSet<>(watchers)) {
                Mode mode = watch2Paths.get(w).get(path);
                fired.add(w);
                w.fired.add(path);                if (mode == Mode.STANDARD) {
                    // 触发即移除
                    watchers.remove(w);
                    watch2Paths.get(w).remove(path);
                    if (watch2Paths.get(w).isEmpty()) watch2Paths.remove(w);
                }
                // PERSISTENT/PERSISTENT_RECURSIVE 保持
            }
            if (watchers.isEmpty()) watchTable.remove(path);
            return fired;
        }

        /** C: 全清 — 对照 WatchManager:113-132 */
        public void removeWatcher(Watcher w) {
            Map<String, Mode> paths = watch2Paths.remove(w);
            if (paths == null) return;
            for (String p : paths.keySet()) {
                Set<Watcher> list = watchTable.get(p);
                if (list != null) {
                    list.remove(w);
                    if (list.isEmpty()) watchTable.remove(p);
                }
            }
        }
    }

    /** D: 位图 — 对照 watcherBitIdMap + BitHashSet */
    public static class BitMap {
        final Map<Watcher, Integer> idMap = new HashMap<>();
        final Map<Integer, Set<Watcher>> bitSet = new HashMap<>();
        int nextBit = 0;

        int add(Watcher w) {
            Integer bit = idMap.get(w);
            if (bit == null) {
                bit = nextBit++;
                idMap.put(w, bit);
            }
            return bit;
        }
        boolean contains(Watcher w) { return idMap.containsKey(w); }
        int size() { return idMap.size(); }
    }

    public static void main(String[] args) {
        // --- A: 双向注册 ---
        WatchManager wm = new WatchManager();
        Watcher w1 = new Watcher(1);
        Watcher w2 = new Watcher(2);
        wm.addWatch("/a", w1, Mode.STANDARD);
        wm.addWatch("/a", w2, Mode.PERSISTENT_RECURSIVE);
        assertTrue(wm.watchTable.get("/a").size() == 2 && wm.watch2Paths.size() == 2,
            "双向注册 2 watcher × 1 path");
        System.out.println("[A] 双向注册 1/1 OK");

        // --- B: 一次性触发 ---
        Set<Watcher> fired = wm.trigger("/a");
        assertTrue(fired.size() == 2, "双 watcher 都触发");
        assertTrue(!wm.watch2Paths.containsKey(w1) && wm.watch2Paths.containsKey(w2),
            "STANDARD 移除 / PERSISTENT_RECURSIVE 保持");
        assertTrue(wm.watchTable.get("/a").size() == 1, "watchTable 剩 1 (PERSISTENT)");
        wm.trigger("/a");  // 再触发
        assertTrue(w2.fired.size() == 2, "PERSISTENT 可重复触发");
        System.out.println("[B] 一次性触发 + 持久保持 4/4 OK");

        // --- C: 双向清理 ---
        WatchManager wm2 = new WatchManager();
        Watcher w3 = new Watcher(3);
        wm2.addWatch("/x", w3, Mode.STANDARD);
        wm2.addWatch("/y", w3, Mode.STANDARD);
        wm2.removeWatcher(w3);
        assertTrue(!wm2.watch2Paths.containsKey(w3) && wm2.watchTable.isEmpty(),
            "全清: watch2Paths 空 + watchTable 空路径删除");
        System.out.println("[C] 双向清理 1/1 OK");

        // --- D: 位图 ---
        BitMap bm = new BitMap();
        Watcher w4 = new Watcher(4);
        Watcher w5 = new Watcher(5);
        int b4 = bm.add(w4), b5 = bm.add(w5);
        assertTrue(b4 == 0 && b5 == 1 && bm.contains(w4) && bm.contains(w5) && bm.size() == 2,
            "位图 id 分配 0/1 + contains O(1)");
        int b4again = bm.add(w4);
        assertTrue(b4again == 0 && bm.size() == 2, "重复 add 同 watcher 幂等");
        System.out.println("[D] 位图压缩 2/2 OK");

        System.out.println("MiniZKWatcher 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
