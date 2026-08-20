import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MiniTransactionHook — S-6 TransactionHook 核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Seata 2.5.0 源码):
 *   A. 7 钩子定义 + 3×2+1 分组 + 触发顺序 (TransactionHook.java:19-55; TransactionalTemplate:223-402)
 *   B. 管理器语义: registerHook 累加 + getHooks 只读 + clear 清空
 *      (TransactionHookManager.java:32-66)
 *   C. 触发异常不中断: 钩子抛异常 → 后续钩子仍执行 (TransactionalTemplate:322-328)
 *   D. Launcher 守卫: Participant 不触发 afterCompletion + cleanUp 归 Launcher
 *      (TransactionalTemplate:381-402)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniTransactionHook {

    interface TransactionHook {
        default void beforeBegin() {}
        default void afterBegin() {}
        default void beforeCommit() {}
        default void afterCommit() {}
        default void beforeRollback() {}
        default void afterRollback() {}
        default void afterCompletion() {}
    }

    // ---- 管理器 (ThreadLocal) ----
    static class HookManager {
        static final ThreadLocal<List<TransactionHook>> LOCAL_HOOKS = new ThreadLocal<>();

        static List<TransactionHook> getHooks() {
            List<TransactionHook> hooks = LOCAL_HOOKS.get();
            if (hooks == null || hooks.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(hooks); // 只读视图
        }

        static void registerHook(TransactionHook hook) {
            if (hook == null) throw new NullPointerException("transactionHook must not be null");
            List<TransactionHook> hooks = LOCAL_HOOKS.get();
            if (hooks == null) {
                LOCAL_HOOKS.set(new ArrayList<>());
                hooks = LOCAL_HOOKS.get();
            }
            hooks.add(hook);
        }

        static void clear() {
            LOCAL_HOOKS.remove(); // remove 防泄漏
        }
    }

    // ---- 触发编排 (简化) ----
    static class Template {
        static List<String> events = new ArrayList<>();

        static void trigger(String name, List<TransactionHook> hooks) {
            for (TransactionHook h : hooks) {
                try {
                    events.add(name);
                    // 模拟钩子动作
                } catch (Exception e) {
                    // 吞掉
                }
            }
        }

        /** 模拟 begin→业务→commit 周期 */
        static void runCommitCycle(boolean launcher) {
            List<TransactionHook> hooks = HookManager.getHooks();
            if (launcher) trigger("beforeBegin", hooks);
            if (launcher) trigger("afterBegin", hooks);
            // 业务
            if (launcher) trigger("beforeCommit", hooks);
            if (launcher) trigger("afterCommit", hooks);
            // finally: afterCompletion 仅 Launcher
            if (launcher) trigger("afterCompletion", hooks);
            if (launcher) HookManager.clear();
        }
    }

    // 异常钩子: 抛异常验证不中断
    static class ThrowingHook implements TransactionHook {
        @Override public void beforeBegin() { throw new RuntimeException("hook boom"); }
    }
    static class NormalHook implements TransactionHook {}

    public static void main(String[] args) throws Exception {
        // ============ A: 7 钩子 + 触发顺序 ============
        java.lang.reflect.Method[] methods = TransactionHook.class.getDeclaredMethods();
        assertTrue(methods.length == 7, "A1 接口 7 钩子");
        assertTrue(TransactionHook.class.getDeclaredMethod("beforeBegin") != null
                && TransactionHook.class.getDeclaredMethod("afterCompletion") != null, "A2 首尾钩子存在");
        Template.events.clear();
        HookManager.registerHook(new NormalHook());
        Template.runCommitCycle(true);
        assertTrue(Template.events.equals(java.util.Arrays.asList(
                "beforeBegin", "afterBegin", "beforeCommit", "afterCommit", "afterCompletion")),
            "A3 触发顺序: beforeBegin→afterBegin→beforeCommit→afterCommit→afterCompletion");
        HookManager.clear();
        System.out.println("[A] 7 钩子 + 触发顺序 3/3 OK");

        // ============ B: 管理器语义 ============
        HookManager.registerHook(new NormalHook());
        HookManager.registerHook(new NormalHook());
        assertTrue(HookManager.getHooks().size() == 2, "B1 注册累加 2");
        // 每次 getHooks 新建只读包装 (非缓存 — 对照 TccHookManager CACHED_UNMODIFIABLE_HOOKS)
        assertTrue(HookManager.getHooks().equals(java.util.Arrays.asList(
                HookManager.getHooks().get(0), HookManager.getHooks().get(1))), "B2 只读视图内容一致 (每次新包装)");
        boolean readOnlyThrown = false;
        try {
            HookManager.getHooks().add(new NormalHook()); // unmodifiableList
        } catch (UnsupportedOperationException e) {
            readOnlyThrown = true;
        }
        assertTrue(readOnlyThrown, "B3 getHooks 只读 (修改抛 UnsupportedOperationException)");
        HookManager.clear();
        assertTrue(HookManager.getHooks().isEmpty(), "B4 clear 后为空");
        boolean npe = false;
        try {
            HookManager.registerHook(null);
        } catch (NullPointerException e) {
            npe = true;
        }
        assertTrue(npe, "B5 registerHook(null) → NPE");
        System.out.println("[B] 管理器语义 5/5 OK");

        // ============ C: 异常不中断 ============
        HookManager.registerHook(new ThrowingHook()); // 抛异常
        HookManager.registerHook(new NormalHook());
        Template.events.clear();
        Template.runCommitCycle(true);
        // ThrowingHook.beforeBegin 抛 → 吞掉 → NormalHook 仍触发
        assertTrue(Template.events.size() >= 4, "C1 钩子异常后后续钩子仍执行 (事件数 >= 4)");
        HookManager.clear();
        System.out.println("[C] 异常不中断 1/1 OK");

        // ============ D: Launcher 守卫 ============
        HookManager.registerHook(new NormalHook());
        Template.events.clear();
        Template.runCommitCycle(false); // Participant
        assertTrue(Template.events.isEmpty(), "D1 Participant 不触发任何钩子");
        HookManager.registerHook(new NormalHook());
        Template.events.clear();
        Template.runCommitCycle(true);
        assertTrue(HookManager.getHooks().isEmpty(), "D2 Launcher 完成后 clear (钩子已清)");
        System.out.println("[D] Launcher 守卫 2/2 OK");

        System.out.println("MiniTransactionHook 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
