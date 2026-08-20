import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniSnapshot — J-3 快照核心逻辑极简复现 (harness)
 *
 * 纯内存模拟, 对照 SOFAJRaft 1.4.1 源码验证:
 *   A. 冻结语义: 快照任务与 apply 同队列串行, 快照点 = 当时 lastApplied (FSMCallerImpl.java:201-210)
 *   B. temp 原子改名: 崩溃残留 temp 清理, 正式目录只有完整快照 (LocalSnapshotStorage.java:247-261)
 *   C. 截断三分支: term==0 全截 / term 匹配截到上次快照点+1 / term 不匹配 reset (LogManagerImpl.java:661-682)
 *   D. 引用计数: 传输中快照不删, 归零才物理删 (LocalSnapshotStorage.java:199-206,262-270)
 *   E. 硬链接复用: 相同 checksum 文件不重复下载 (LocalSnapshotCopier.java:254-328)
 */
public class MiniSnapshot {

    /** A. 冻结: 模拟 FSM 队列串行 (COMMITTED 与 SNAPSHOT 严格有序) */
    static class FSMSim {
        long lastApplied = 0;
        Long snapshotPoint = null;

        void apply(long to) {
            lastApplied = to; // 批量 apply
        }

        void snapshot() {
            snapshotPoint = lastApplied; // 队列串行: 快照点 = 当前 lastApplied
        }
    }

    /** B. 原子改名 + D. 引用计数 */
    static class SnapshotStore {
        final Map<Long, Integer> refs = new HashMap<>(); // index -> refCount
        final List<String> tempDirs = new ArrayList<>();
        boolean moveFailed = false;

        void writeTemp(String name) {
            tempDirs.add(name);
        }

        boolean commit(long index) {
            if (moveFailed) {
                tempDirs.clear(); // 崩溃: temp 清理
                return false;
            }
            refs.put(index, 1);
            tempDirs.clear(); // 原子改名后 temp 空
            return true;
        }

        void ref(long index) {
            refs.put(index, refs.getOrDefault(index, 0) + 1);
        }

        void unref(long index) {
            int c = refs.getOrDefault(index, 0) - 1;
            if (c <= 0) {
                refs.remove(index); // 归零物理删除
            } else {
                refs.put(index, c);
            }
        }

        boolean exists(long index) {
            return refs.containsKey(index);
        }
    }

    /** C. 截断三分支 (LogManagerImpl.java:661-682 语义) */
    static long[] truncatePolicy(long lastIncludedIndex, long lastIncludedTerm,
                                 long lastLogTerm, long savedLastSnapshotIndex) {
        if (lastIncludedTerm == 0) {
            // term==0: 快照领先日志 → 全截
            return new long[] {lastIncludedIndex + 1, -1}; // -1 = 不 reset
        }
        if (lastIncludedTerm == lastLogTerm) {
            // term 匹配 → 只截到上次快照点+1 (保守)
            return new long[] {savedLastSnapshotIndex + 1, -1};
        }
        // term 不匹配 → reset 整体重建
        return new long[] {lastIncludedIndex + 1, 1};
    }

    /** E. 硬链接复用 (filter 语义) */
    static class Copier {
        int downloaded = 0;
        int reused = 0;

        void copyFile(String name, String localChecksum, String remoteChecksum) {
            if (localChecksum != null && localChecksum.equals(remoteChecksum)) {
                reused++; // Files.createLink 复用 (L305)
            } else {
                downloaded++;
            }
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.冻结", "B.原子改名", "C.截断三分支", "D.引用计数", "E.硬链接复用"};
        boolean[] r = new boolean[5];

        // ---------- A. 冻结 ----------
        {
            FSMSim fsm = new FSMSim();
            fsm.apply(100);
            fsm.apply(200);
            fsm.snapshot(); // 队列串行: apply 全部完成才执行快照
            fsm.apply(300); // 快照后继续 apply
            r[0] = fsm.snapshotPoint == 200;
            System.out.println("[A] 冻结: 快照点=" + fsm.snapshotPoint + " (快照后 lastApplied=" + fsm.lastApplied + ")");
        }

        // ---------- B. 原子改名 ----------
        {
            SnapshotStore store = new SnapshotStore();
            store.writeTemp("partial-1");
            store.writeTemp("partial-2");
            store.commit(1);
            boolean afterOk = store.tempDirs.isEmpty() && store.exists(1);
            // 崩溃场景: move 失败 → temp 清理, 无正式快照
            SnapshotStore crash = new SnapshotStore();
            crash.moveFailed = true;
            crash.writeTemp("partial-x");
            crash.commit(5);
            boolean afterCrash = crash.tempDirs.isEmpty() && !crash.exists(5);
            r[1] = afterOk && afterCrash;
            System.out.println("[B] 原子改名: 成功提交=" + afterOk + " 崩溃清理=" + afterCrash);
        }

        // ---------- C. 截断三分支 ----------
        {
            // ① term==0: 快照领先日志 → 全截
            long[] c1 = truncatePolicy(100, 0, 0, 0);
            // ② term 匹配: 上次快照点 50, 本次 100 → 只截到 51 (保留 51-100)
            long[] c2 = truncatePolicy(100, 5, 5, 50);
            // ③ term 不匹配: 日志 term 4 ≠ 快照 term 5 → reset
            long[] c3 = truncatePolicy(100, 5, 4, 50);
            r[2] = c1[0] == 101 && c1[1] == -1 && c2[0] == 51 && c3[1] == 1;
            System.out.println("[C] 截断: 全截到" + c1[0] + " 保守截到" + c2[0] + " term不匹配reset=" + (c3[1] == 1));
        }

        // ---------- D. 引用计数 ----------
        {
            SnapshotStore store = new SnapshotStore();
            store.commit(1);
            store.ref(1); // 传输中
            store.unref(1); // 传输完
            boolean kept = store.exists(1); // ref 归零前的另一 ref 仍保留
            store.unref(1); // 归零 → 物理删
            boolean deleted = !store.exists(1);
            r[3] = kept && deleted;
            System.out.println("[D] 引用计数: 传输后仍保留=" + kept + " 归零删除=" + deleted);
        }

        // ---------- E. 硬链接复用 ----------
        {
            Copier c = new Copier();
            c.copyFile("data-1", "abc", "abc"); // 相同 → 复用
            c.copyFile("data-2", "abc", "xyz"); // 不同 → 下载
            c.copyFile("data-3", null, "abc");  // 本地无 → 下载
            r[4] = c.reused == 1 && c.downloaded == 2;
            System.out.println("[E] 硬链接: 复用=" + c.reused + " 下载=" + c.downloaded);
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
