import java.util.LinkedList;
import java.util.function.BiConsumer;

/**
 * MiniEntry — S-2 入口域微缩引擎 (费曼法验证)
 * 复刻 1.8.9 四个核心机制:
 *  A. Context 单 curEntry 指针 + CtEntry.parent 链 (隐式栈)
 *  B. exit 三态: NullContext 免清理 / 错序先扯平再抛 / 正常五步收尾
 *  C. 默认 context 联动退出 (isDefaultContext 收窄)
 *  D. ThreadLocal 生命周期 = 最外层 entry 生命周期
 */
public class MiniEntry {

    static final String DEFAULT_CTX = "sentinel_default_context";

    static class Context {
        final String name;
        Entry curEntry;
        final boolean async;
        Context(String name, boolean async) { this.name = name; this.async = async; }
        boolean isDefault() { return DEFAULT_CTX.equals(name); }
    }

    static class NullContext extends Context {
        NullContext() { super("<null>", false); }
    }

    static class ContextUtil {
        static final ThreadLocal<Context> holder = new ThreadLocal<>();
        static final NullContext NULL = new NullContext();

        static Context enter(String name) {
            if (DEFAULT_CTX.equals(name)) throw new IllegalStateException("default name not permitted!");
            return trueEnter(name);
        }

        static Context trueEnter(String name) {
            Context c = holder.get();
            if (c == null) { c = new Context(name, false); holder.set(c); }
            return c;
        }

        static Context get() { return holder.get(); }

        static void exit() {
            Context c = holder.get();
            if (c != null && c.curEntry == null) holder.set(null);
        }
    }

    static class Entry {
        Entry parent = null;
        Entry child = null;
        Context context;
        int exitCount = 0;

        Entry(Context context) {
            this.context = context;
            setUp(context);
        }

        void setUp(Context context) {
            if (context instanceof NullContext) return;
            this.parent = context.curEntry;
            if (parent != null) parent.child = this;
            context.curEntry = this;
        }

        void exit() { trueExit(); }

        Entry trueExit() {
            exitForContext(context);
            return parent;
        }

        void exitForContext(Context ctx) {
            if (ctx == null) return;
            if (ctx instanceof NullContext) return;              // 态1: 免清理
            if (ctx.curEntry != this) {                          // 态2: 错序自愈
                Entry e = ctx.curEntry;
                while (e != null) { e.exit(); e = e.parent; }
                throw new ErrorEntryFreeException("order error");
            }
            exitCount++;                                          // 态3: ① 槽链 exit(此处模拟)
            ctx.curEntry = parent;                                // ③ 栈顶回退
            if (parent != null) parent.child = null;
            if (parent == null && ctx.isDefault()) ContextUtil.exit(); // ④ 默认 context 自动退出
            this.context = null;                                  // ⑤ 防重复
        }
    }

    static class CtEntry extends Entry {
        CtEntry(Context c) { super(c); }
    }

    static class ErrorEntryFreeException extends RuntimeException {
        ErrorEntryFreeException(String m) { super(m); }
    }

    static class AsyncEntry extends CtEntry {
        Context asyncContext;
        AsyncEntry(Context c) { super(c); }
        void initAsync() { asyncContext = new Context(context.name, true); asyncContext.curEntry = this; }
        void cleanLocal() {
            if (context instanceof NullContext) return;
            Context orig = context;
            if (orig.curEntry == this) {
                orig.curEntry = parent;
                if (parent != null) parent.child = null;
            } else throw new IllegalStateException("bad async state");
        }
        @Override Entry trueExit() { exitForContext(asyncContext); return parent; }
    }

    public static void main(String[] args) {
        int pass = 0; int total = 0;
        String R = "sentinel_default_context";

        // ---- C1: 嵌套栈: A/B 入栈, 栈顶 = B, parent/child 双向 ----
        ContextUtil.enter("web");
        Context ctx = ContextUtil.get();
        Entry a = new Entry(ctx);
        Entry b = new Entry(ctx);
        total += 2;
        if (ctx.curEntry == b && b.parent == a && a.child == b) pass++;
        if (a.parent == null) pass++;
        try { a.exit(); } catch (ErrorEntryFreeException e1) { /* 错序:扯平 B 后抛异常 */ }
        total++;
        if (b.exitCount == 1) pass++;       // B 被强制 exit
        b.exit();                           // B 已退出过, 静默吸收
        a.exit();                           // 栈已扯平(curEntry==a), 现在正常退出
        total++;
        if (ctx.curEntry == null && a.exitCount == 1) pass++;       // a 正常退出, 栈空 else System.out.println("FAIL #4");

        // ---- C2: 正常退出顺序 + 默认 context 自动退出 ----
        ContextUtil.exit(); // 清掉 'web' 的显式 context
        ContextUtil.trueEnter(R);
        Context dctx = ContextUtil.get();
        Entry d = new Entry(dctx);
        d.exit();
        total += 2;
        if (ContextUtil.get() == null) pass++;          // 默认 context 最外层退出 → ThreadLocal 清空
        if (d.exitCount == 1) pass++;

        // ---- C3: 显式 context 不会被自动退出(cbaacfda 语义) ----
        ContextUtil.enter("web2");
        Context wctx = ContextUtil.get();
        Entry w = new Entry(wctx);
        w.exit();
        total++;
        if (ContextUtil.get() == wctx) pass++;          // 显式 context 保留!

        // ---- C4: NullContext 免清理 + 不挂栈 ----
        Context nc = ContextUtil.NULL;
        Entry ghost = new Entry(nc);
        total += 2;
        if (nc.curEntry == null) pass++;                // 不挂栈
        ghost.exit();                                   // 不抛
        if (ghost.exitCount == 0) pass++;

        // ---- C5: 异步两阶段: 摘除当前线程 + 在 asyncContext 上退出 ----
        ContextUtil.exit(); ContextUtil.trueEnter(R);
        Context actx = ContextUtil.get();
        AsyncEntry ae = new AsyncEntry(actx);
        ae.initAsync(); ae.cleanLocal();
        total += 2;
        if (actx.curEntry == null && ae.asyncContext.curEntry == ae) pass++;
        ae.exit(); // 在 asyncContext 上退出(当前线程栈不受影响)
        if (actx.curEntry == null && ae.exitCount == 1) pass++;

        // ---- C6: 重复 exit 静默吸收(context 置 null) ----
        ContextUtil.exit(); ContextUtil.trueEnter(R);
        Entry e6 = new Entry(ContextUtil.get());
        e6.exit(); e6.exit();
        total++;
        if (e6.exitCount == 1) pass++;

        // ---- C7: 释放顺序错乱后栈已救回, 后续调用正常 ----
        ContextUtil.exit(); ContextUtil.enter("web3");
        Context c7 = ContextUtil.get();
        Entry x = new Entry(c7); Entry y = new Entry(c7);
        try { x.exit(); } catch (ErrorEntryFreeException ignored) {}
        Entry z = new Entry(c7);   // 新 entry 正常挂栈
        total++;
        if (c7.curEntry == z) pass++;

        System.out.println("PASS=" + pass + "/" + total + (pass == total ? " (all green)" : " FAIL=" + (total - pass)));
    }
}
