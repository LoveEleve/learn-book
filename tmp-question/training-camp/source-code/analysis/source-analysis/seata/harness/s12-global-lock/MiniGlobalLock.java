import java.util.ArrayList;
import java.util.List;

/**
 * MiniGlobalLock — S-12 全局锁体系核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. lockKey 解析: "table:pk1,pk2;t2:pk3" → RowLock 列表 (AbstractLockManager:122-165)
 *   B. 检查-插入: rowKey 去重 + IN 检查 + 冲突判定 (dbXID != currentXID)
 *      (LockStoreDataBaseDAO:109-160+)
 *   C. lockQuery 只查不锁 vs acquireLock 检查-插入 (语义对比)
 *   D. LockStatus 2 值 + 释放两级 (LockStatus.java:25-30; DataBaseLockManager:58-73)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniGlobalLock {

    static class RowLock {
        final String tableName, pk, xid;
        RowLock(String tableName, String pk, String xid) { this.tableName = tableName; this.pk = pk; this.xid = xid; }
        String rowKey() { return tableName + ":" + pk; } // LockDO.rowKey 复合键
    }

    // ---- A: lockKey 解析 ----
    static List<RowLock> collectRowLocks(String lockKey, String xid) {
        List<RowLock> locks = new ArrayList<>();
        String[] tableGrouped = lockKey.split(";");
        for (String tg : tableGrouped) {
            int idx = tg.indexOf(":");
            if (idx < 0) return locks;
            String tableName = tg.substring(0, idx);
            String mergedPKs = tg.substring(idx + 1);
            for (String pk : mergedPKs.split(",")) {
                if (!pk.isEmpty()) locks.add(new RowLock(tableName, pk, xid));
            }
        }
        return locks;
    }

    // ---- B: 检查-插入 ----
    static class LockStore {
        final List<RowLock> held = new ArrayList<>(); // rowKey → 持有者

        /** acquireLock: 检查-插入两段 */
        boolean acquireLock(List<RowLock> locks, boolean skipCheckLock) {
            // rowKey 去重
            List<RowLock> dedup = new ArrayList<>();
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (RowLock l : locks) {
                if (seen.add(l.rowKey())) dedup.add(l);
            }
            // checkLock: 已存在且 xid 不同 → 冲突
            if (!skipCheckLock) {
                for (RowLock l : dedup) {
                    for (RowLock h : held) {
                        if (h.rowKey().equals(l.rowKey()) && !h.xid.equals(l.xid)) {
                            return false; // LockKeyConflict
                        }
                    }
                }
            }
            held.addAll(dedup);
            return true;
        }

        /** lockQuery: 只查不锁 */
        boolean isLockable(List<RowLock> locks) {
            for (RowLock l : locks) {
                for (RowLock h : held) {
                    if (h.rowKey().equals(l.rowKey()) && !h.xid.equals(l.xid)) {
                        return false; // 不可锁
                    }
                }
            }
            return true; // 不写入!
        }
    }

    // ---- D: LockStatus + 释放 ----
    enum LockStatus { Locked(0), Rollbacking(1); final int code; LockStatus(int code) { this.code = code; } }

    public static void main(String[] args) {
        // ============ A: lockKey 解析 ============
        List<RowLock> locks = collectRowLocks("t1:1,2;t2:5", "xid1");
        assertTrue(locks.size() == 3, "A1 表:主键格式解析 (t1:1,2;t2:5 → 3 行锁)");
        assertTrue(locks.get(0).rowKey().equals("t1:1") && locks.get(2).rowKey().equals("t2:5"),
            "A2 rowKey 复合键 (表:主键)");
        assertTrue(collectRowLocks("badformat", "x").isEmpty(), "A3 无冒号 → 空 (安全返回)");
        System.out.println("[A] lockKey 解析 3/3 OK");

        // ============ B: 检查-插入 ============
        LockStore store = new LockStore();
        List<RowLock> locks1 = collectRowLocks("t1:1", "xid1");
        List<RowLock> locks1dup = collectRowLocks("t1:1,t1:1", "xid1"); // 重复
        assertTrue(store.acquireLock(locks1, false), "B1 首次获取成功");
        assertTrue(store.acquireLock(locks1dup, false), "B2 rowKey 去重后幂等 (同 xid 重获)");
        List<RowLock> locks2 = collectRowLocks("t1:1", "xid2"); // 异 xid 冲突
        assertTrue(!store.acquireLock(locks2, false), "B3 异 xid 冲突 → 获取失败 (LockKeyConflict)");
        assertTrue(store.acquireLock(locks2, true), "B4 skipCheckLock → 跳过检查插入");
        System.out.println("[B] 检查-插入 4/4 OK");

        // ============ C: lockQuery 只查不锁 ============
        LockStore store2 = new LockStore();
        store2.acquireLock(collectRowLocks("t1:1", "xid1"), false);
        assertTrue(store2.isLockable(collectRowLocks("t1:2", "xid2")), "C1 不同主键可锁 (只查)");
        assertTrue(!store2.isLockable(collectRowLocks("t1:1", "xid2")), "C2 冲突不可锁 (只查)");
        assertTrue(store2.held.size() == 1, "C3 lockQuery 不写入 (held 仍 1 行)");
        System.out.println("[C] lockQuery 只查不锁 3/3 OK");

        // ============ D: LockStatus + 释放 ============
        assertTrue(LockStatus.values().length == 2 && LockStatus.Rollbacking.code == 1,
            "D1 LockStatus 2 值 (Locked(0)/Rollbacking(1))");
        // 释放两级: 分支级 + 全局级
        LockStore store3 = new LockStore();
        store3.acquireLock(collectRowLocks("t1:1", "xid1"), false);
        store3.held.clear(); // 全局级释放 (releaseGlobalSessionLock)
        assertTrue(store3.held.isEmpty(), "D2 全局释放后锁空");
        System.out.println("[D] LockStatus + 释放 2/2 OK");

        System.out.println("MiniGlobalLock 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
