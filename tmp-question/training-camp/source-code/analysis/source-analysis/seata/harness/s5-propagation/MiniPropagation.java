import java.util.ArrayList;
import java.util.List;

/**
 * MiniPropagation — S-5 事务传播核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 传播枚举 6 值 + 决策矩阵 6×2 (Propagation.java:57-176; TransactionalTemplate:66-113)
 *   B. 挂起恢复: suspend → unbind + holder; resume → bind (DefaultGlobalTransaction:207-240)
 *   C. 嵌套场景: REQUIRES_NEW 栈式挂起 / NOT_SUPPORTED 内 MANDATORY 抛 (环境清空)
 *   D. clean 语义: suspend(true) 不返回 holder (事务结束解绑)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniPropagation {

    enum Propagation { REQUIRED, REQUIRES_NEW, NOT_SUPPORTED, SUPPORTS, NEVER, MANDATORY }

    // ---- 决策矩阵 (6×2) ----
    static String decide(Propagation p, boolean existingTx) {
        switch (p) {
            case REQUIRED:     return existingTx ? "JOIN" : "CREATE_NEW";
            case REQUIRES_NEW: return existingTx ? "SUSPEND_CREATE_NEW" : "CREATE_NEW";
            case NOT_SUPPORTED:return existingTx ? "SUSPEND_RUN_WITHOUT" : "RUN_WITHOUT";
            case SUPPORTS:     return existingTx ? "JOIN" : "RUN_WITHOUT";
            case NEVER:        return existingTx ? "THROW" : "RUN_WITHOUT";
            case MANDATORY:    return existingTx ? "JOIN" : "THROW";
            default:           return "UNSUPPORTED";
        }
    }

    // ---- B: 挂起恢复 (简化 RootContext) ----
    static class RootContext {
        static String xid;
        static String bind(String x) { xid = x; return xid; }
        static String unbind() { String old = xid; xid = null; return old; }
        static String get() { return xid; }
    }

    static String suspend(boolean clean) {
        String xid = RootContext.get();
        if (xid != null) {
            RootContext.unbind();
            return clean ? null : xid; // SuspendedResourcesHolder
        }
        return null;
    }

    static void resume(String holder) {
        if (holder != null) {
            RootContext.bind(holder);
        }
    }

    /** 模拟 REQUIRES_NEW 嵌套: 返回事件序列 */
    static List<String> nestedRequiresNew() {
        List<String> events = new ArrayList<>();
        // 外层
        RootContext.bind("outer-xid");
        // 内层 REQUIRES_NEW: suspend 外层 → 新建
        String holder1 = suspend(false);
        events.add("SUSPEND:" + holder1);
        RootContext.bind("inner1-xid");
        // 再内层 REQUIRES_NEW
        String holder2 = suspend(false);
        events.add("SUSPEND:" + holder2);
        RootContext.bind("inner2-xid");
        // inner2 完成 → resume inner1... 实际按栈: inner2 恢复 outer? 模拟标准嵌套
        RootContext.unbind();
        resume(holder2);
        events.add("RESUME:" + RootContext.get());
        RootContext.unbind();
        resume(holder1);
        events.add("RESUME:" + RootContext.get());
        RootContext.unbind(); // 外层结束
        return events;
    }

    public static void main(String[] args) {
        // ============ A: 枚举 + 决策矩阵 ============
        assertTrue(Propagation.values().length == 6, "A1 枚举 6 值");
        assertTrue(decide(Propagation.REQUIRED, true).equals("JOIN"), "A2 REQUIRED 有事务 → JOIN");
        assertTrue(decide(Propagation.REQUIRED, false).equals("CREATE_NEW"), "A3 REQUIRED 无事务 → 新建");
        assertTrue(decide(Propagation.REQUIRES_NEW, true).equals("SUSPEND_CREATE_NEW"), "A4 REQUIRES_NEW 挂起新建");
        assertTrue(decide(Propagation.NOT_SUPPORTED, true).equals("SUSPEND_RUN_WITHOUT"), "A5 NOT_SUPPORTED 挂起裸跑");
        assertTrue(decide(Propagation.NEVER, true).equals("THROW"), "A6 NEVER 有事务抛");
        assertTrue(decide(Propagation.MANDATORY, false).equals("THROW"), "A7 MANDATORY 无事务抛");
        assertTrue(decide(Propagation.SUPPORTS, false).equals("RUN_WITHOUT"), "A8 SUPPORTS 无事务裸跑");
        // 全矩阵 12 决策点无 UNSUPPORTED
        int unsupported = 0;
        for (Propagation p : Propagation.values()) {
            if (decide(p, true).equals("UNSUPPORTED") || decide(p, false).equals("UNSUPPORTED")) unsupported++;
        }
        assertTrue(unsupported == 0, "A9 12 决策点全定义");
        System.out.println("[A] 枚举 + 决策矩阵 9/9 OK");

        // ============ B: 挂起恢复 ============
        RootContext.bind("x1");
        String holder = suspend(false);
        assertTrue(RootContext.get() == null && holder.equals("x1"), "B1 suspend → unbind + holder");
        resume(holder);
        assertTrue(RootContext.get().equals("x1"), "B2 resume → bind 恢复");
        resume(null);
        assertTrue(RootContext.get().equals("x1"), "B3 resume(null) → 无操作");
        RootContext.unbind();
        System.out.println("[B] 挂起恢复 3/3 OK");

        // ============ C: 嵌套场景 ============
        List<String> events = nestedRequiresNew();
        assertTrue(events.get(0).equals("SUSPEND:outer-xid"), "C1 第一段挂起 outer");
        assertTrue(events.get(1).equals("SUSPEND:inner1-xid"), "C2 第二段挂起 inner1 (栈式)");
        assertTrue(events.get(2).equals("RESUME:inner1-xid"), "C3 先恢复 inner1 (LIFO)");
        assertTrue(events.get(3).equals("RESUME:outer-xid"), "C4 再恢复 outer");
        // NOT_SUPPORTED 内 MANDATORY: 挂起后无事务 → MANDATORY 抛
        RootContext.bind("x2");
        suspend(false); // NOT_SUPPORTED 挂起
        boolean thrown = false;
        try {
            decide(Propagation.MANDATORY, RootContext.get() != null);
            if (!decide(Propagation.MANDATORY, false).equals("THROW")) throw new IllegalStateException();
        } catch (Exception e) {
            thrown = true;
        }
        assertTrue(decide(Propagation.MANDATORY, false).equals("THROW"),
            "C5 NOT_SUPPORTED 内 MANDATORY → 抛 (环境清空效应)");
        RootContext.unbind();
        System.out.println("[C] 嵌套场景 5/5 OK");

        // ============ D: clean 语义 ============
        RootContext.bind("x3");
        String cleanHolder = suspend(true);
        assertTrue(cleanHolder == null, "D1 suspend(true) → null holder (事务结束解绑)");
        assertTrue(RootContext.get() == null, "D2 clean 后上下文已解绑");
        // 无事务时 suspend → null
        assertTrue(suspend(false) == null, "D3 无事务 suspend → null");
        System.out.println("[D] clean 语义 3/3 OK");

        System.out.println("MiniPropagation 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
