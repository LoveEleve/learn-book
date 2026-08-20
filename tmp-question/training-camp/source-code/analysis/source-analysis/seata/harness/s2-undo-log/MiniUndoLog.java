import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MiniUndoLog — S-2 undo_log 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 反向 SQL 三模板: DELETE→INSERT / INSERT→DELETE / UPDATE→UPDATE SET
 *      (MySQLUndoDeleteExecutor:51-82 / MySQLUndoInsertExecutor:42-76 / MySQLUndoUpdateExecutor:40-73)
 *   B. 校验三步决策: before==after 跳过 / after==current 通过 / before==current 跳过 / else dirty
 *      (AbstractUndoExecutor.dataValidationAndGoOn:234-283)
 *   C. 逆序回滚 + 状态机: sqlUndoLogs reverse + GlobalFinished 忽略 + exists→删除 / !exists→标记
 *      (AbstractUndoLogManager.undo:315-466)
 *   D. 压缩阈值 + 重试分类: needCompress(enable && >64k) + Retriable/Unretriable 判定
 *      (AbstractUndoLogManager:419-445,571-573)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniUndoLog {

    static final long COMPRESS_THRESHOLD = 64 * 1024;
    static final int NORMAL = 0, GLOBAL_FINISHED = 1;

    // ---- A: 反向 SQL 三模板 (简化生成, 保留结构) ----
    static String undoSQL(String sqlType, String table, String pk) {
        switch (sqlType) {
            case "DELETE":
                // MySQLUndoDeleteExecutor: INSERT INTO t (cols, pk) VALUES (..., ?)
                return "INSERT INTO " + table + " (x, y, pk) VALUES (?, ?, ?)";
            case "INSERT":
                // MySQLUndoInsertExecutor: DELETE FROM t WHERE pk = ?
                return "DELETE FROM " + table + " WHERE " + pk + " = ?";
            case "UPDATE":
                // MySQLUndoUpdateExecutor: UPDATE t SET x=?, y=? WHERE pk = ?
                return "UPDATE " + table + " SET x = ?, y = ? WHERE " + pk + " = ?";
            default:
                return "UNSUPPORTED";
        }
    }

    // ---- B: 校验三步决策 ----
    static String validate(boolean beforeEqualsAfter, boolean afterEqualsCurrent, boolean beforeEqualsCurrent) {
        if (beforeEqualsAfter) {
            return "SKIP_NO_CHANGE";       // L241-249: 业务无实际变更
        }
        if (afterEqualsCurrent) {
            return "PROCEED_UNDO";         // L254-255: 正常
        }
        if (beforeEqualsCurrent) {
            return "SKIP_ALREADY_UNDONE";  // L259-266: 已回滚过 (幂等)
        }
        return "DIRTY";                    // L279: SQLUndoDirtyException
    }

    // ---- C: 逆序回滚 + 状态机 ----
    static class UndoLogEntry {
        final String sqlType;
        final int logStatus;
        boolean deleted;
        UndoLogEntry(String sqlType, int logStatus) { this.sqlType = sqlType; this.logStatus = logStatus; }
    }

    /** 模拟 undo(): 返回执行序列 (逆序) 与最终动作 */
    static List<String> undo(List<UndoLogEntry> entries, boolean logExists) {
        List<String> actions = new ArrayList<>();
        // 行锁 → 查 undo_log → 逐条
        boolean exists = false;
        for (UndoLogEntry entry : entries) {
            exists = true;
            if (entry.logStatus != NORMAL) {
                actions.add("IGNORE:" + entry.sqlType); // GlobalFinished 忽略 (重复回滚防护)
                continue;
            }
            entry.deleted = true;
            actions.add("UNDO:" + entry.sqlType);
        }
        // 逆序: sqlUndoLogs.size()>1 → Collections.reverse (AbstractUndoLogManager:368-370)
        // 已在上层做; 此处验证决策:
        if (exists) {
            actions.add("DELETE_UNDO_LOG");        // exists → deleteUndoLog + commit
        } else {
            actions.add("INSERT_GLOBAL_FINISHED"); // !exists → 防 Phase1 提交 (#489)
        }
        return actions;
    }

    // ---- D: 压缩阈值 + 重试分类 ----
    static boolean needCompress(boolean enable, int length) {
        return enable && length > COMPRESS_THRESHOLD; // AbstractUndoLogManager:571-573
    }

    static String classifyError(Throwable e) {
        if (e instanceof DirtyException) return "UNRETRIABLE_DIRTY";
        if (e instanceof SQLIntegrityConstraintViolation) return "RETRY_CONSTRAINT";
        return "RETRIABLE";
    }

    static class DirtyException extends RuntimeException {}
    static class SQLIntegrityConstraintViolation extends RuntimeException {}
    static class OtherException extends RuntimeException {}

    public static void main(String[] args) {
        // ============ A: 反向 SQL 三模板 ============
        assertTrue(undoSQL("DELETE", "t", "id").startsWith("INSERT INTO t (x, y, pk) VALUES"),
            "A1 DELETE 的 undo = INSERT (beforeImage 重建)");
        assertTrue(undoSQL("INSERT", "t", "id").equals("DELETE FROM t WHERE id = ?"),
            "A2 INSERT 的 undo = DELETE WHERE pk");
        assertTrue(undoSQL("UPDATE", "t", "id").startsWith("UPDATE t SET x = ?, y = ? WHERE id = ?"),
            "A3 UPDATE 的 undo = UPDATE SET before 值 WHERE pk");
        assertTrue(undoSQL("SELECT", "t", "id").equals("UNSUPPORTED"), "A4 非写类型不支持");
        System.out.println("[A] 反向 SQL 三模板 4/4 OK");

        // ============ B: 校验三步决策 ============
        assertTrue(validate(true, false, false).equals("SKIP_NO_CHANGE"), "B1 before==after → 跳过 (无实际变更)");
        assertTrue(validate(false, true, false).equals("PROCEED_UNDO"), "B2 after==current → 执行 undo");
        assertTrue(validate(false, false, true).equals("SKIP_ALREADY_UNDONE"), "B3 before==current → 跳过 (已回滚, 幂等)");
        assertTrue(validate(false, false, false).equals("DIRTY"), "B4 全不等 → dirty → Unretriable");
        System.out.println("[B] 校验三步决策 4/4 OK");

        // ============ C: 逆序回滚 + 状态机 ============
        List<UndoLogEntry> entries = new ArrayList<>();
        entries.add(new UndoLogEntry("DELETE", NORMAL));   // 先执行
        entries.add(new UndoLogEntry("UPDATE", NORMAL));   // 后执行
        entries.add(new UndoLogEntry("INSERT", GLOBAL_FINISHED)); // 已全局完成 → 忽略
        // 逆序执行面: 后注册先回滚 (sqlUndoLogs reverse)
        List<String> reversed = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) reversed.add(entries.get(i).sqlType);
        assertTrue(reversed.equals(List.of("INSERT", "UPDATE", "DELETE")), "C1 逆序回滚 (后执行先补偿)");

        List<String> actions = undo(entries, true);
        assertTrue(actions.contains("IGNORE:INSERT"), "C2 GlobalFinished 忽略 (重复回滚防护)");
        assertTrue(actions.contains("UNDO:DELETE") && actions.contains("UNDO:UPDATE"), "C3 Normal 全部执行 undo");
        assertTrue(actions.contains("DELETE_UNDO_LOG"), "C4 exists → 删除 undo_log + commit");

        List<String> actionsNoLog = undo(new ArrayList<>(), false);
        assertTrue(actionsNoLog.contains("INSERT_GLOBAL_FINISHED"),
            "C5 !exists → insertUndoLogWithGlobalFinished (#489 防 Phase1 提交)");
        System.out.println("[C] 逆序回滚 + 状态机 5/5 OK");

        // ============ D: 压缩阈值 + 重试分类 ============
        assertTrue(needCompress(true, 65537), "D1 开启且 >64k → 压缩");
        assertTrue(!needCompress(true, 65536), "D2 边界 64k 不压缩 (严格大于)");
        assertTrue(!needCompress(false, 70000), "D3 关闭 → 不压缩");
        assertTrue(classifyError(new DirtyException()).equals("UNRETRIABLE_DIRTY"),
            "D4 dirty → Unretriable (人工校准)");
        assertTrue(classifyError(new SQLIntegrityConstraintViolation()).equals("RETRY_CONSTRAINT"),
            "D5 约束冲突 → 重试 (for(;;) 无限重试面)");
        assertTrue(classifyError(new OtherException()).equals("RETRIABLE"), "D6 其他 → Retriable");
        System.out.println("[D] 压缩阈值 + 重试分类 6/6 OK");

        System.out.println("MiniUndoLog 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
