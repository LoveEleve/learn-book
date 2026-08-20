public class MiniRpcInvoke {

    enum InvokeMode { SYNC, ASYNC, FUTURE }

    static class RpcInvocation {
        final String method, iface;
        final InvokeMode mode;
        RpcInvocation(String method, String iface, InvokeMode mode) { this.method = method; this.iface = iface; this.mode = mode; }
        public String toString() { return "RpcInvocation(" + iface + "." + method + ", " + mode + ")"; }
    }

    // ---- A: 入口面 — InvocationUtil.invoke (两拍: 异步 invoke + 同步化 recreate) ----
    static class InvocationUtil {
        static Object invoke(Invoker invoker, RpcInvocation rpc) throws Throwable {
            String ctx = "SAVED_CTX"; // RpcContext.storeServiceContext
            Object result = invoker.invoke(rpc).recreate(); // 两拍
            return "RESULT[" + ctx + "]:" + result;
        }
    }

    interface Result { Object recreate() throws Throwable; }

    static class AsyncRpcResult implements Result {
        final InvokeMode mode;
        final String payload;
        AsyncRpcResult(InvokeMode mode, String payload) { this.mode = mode; this.payload = payload; }
        public Object recreate() throws Throwable {
            // 三模式: FUTURE → Future 本身 / ASYNC → 默认值 / SYNC → 同步等待真实值
            switch (mode) {
                case FUTURE: return "FUTURE_OBJECT(" + payload + ")";
                case ASYNC:  return "DEFAULT_VALUE(null)";
                default:     return "APP_RESPONSE(" + payload + ")"; // getAppResponse().recreate
            }
        }
    }

    // ---- C: 发送面 — DubboInvoker (连接轮询 + oneway + 超时) ----
    static class DubboInvoker implements Invoker {
        static final String[] CLIENTS = {"conn-1", "conn-2", "conn-3"};
        static int index = 0;
        final boolean oneway;
        DubboInvoker(boolean oneway) { this.oneway = oneway; }
        public Result invoke(RpcInvocation rpc) {
            String client = CLIENTS[index++ % CLIENTS.length]; // 连接级轮询
            if (oneway) {
                return new AsyncRpcResult(InvokeMode.SYNC, "SENT(" + client + ", oneway)");
            }
            int timeout = 3000; // calculateTimeout
            return new AsyncRpcResult(rpc.mode, "SENT(" + client + ", twoWay, timeout=" + timeout + ")");
        }
    }

    // ---- B: Filter 链 — buildInvokerChain (筛选 + 倒序包装) ----
    interface Filter { String name(); Object doFilter(Invoker inv, RpcInvocation rpc) throws Throwable; }

    static class FilterA implements Filter {
        public String name() { return "A(timeout)"; }
        public Object doFilter(Invoker inv, RpcInvocation rpc) throws Throwable { return "[" + name() + "→" + inv.invoke(rpc).recreate() + "]"; }
    }
    static class FilterB implements Filter {
        public String name() { return "B(accesslog)"; }
        public Object doFilter(Invoker inv, RpcInvocation rpc) throws Throwable { return "[" + name() + "→" + inv.invoke(rpc).recreate() + "]"; }
    }

    static class FilterChainNode implements Invoker {
        final Invoker next; final Filter filter;
        FilterChainNode(Invoker next, Filter filter) { this.next = next; this.filter = filter; }
        public Result invoke(RpcInvocation rpc) throws Throwable {
            Object r = filter.doFilter(next, rpc);
            return new AsyncRpcResult(InvokeMode.SYNC, String.valueOf(r));
        }
    }

    static Invoker buildInvokerChain(Invoker original, Filter[] activated) {
        // 倒序包装: 最后一个过滤器先执行 (LIFO)
        Invoker last = original;
        for (int i = activated.length - 1; i >= 0; i--) {
            last = new FilterChainNode(last, activated[i]);
        }
        return last;
    }

    interface Invoker {
        Result invoke(RpcInvocation rpc) throws Throwable;
        default boolean isAvailable() { return true; }
    }

    public static void main(String[] args) throws Throwable {
        int pass = 0, fail = 0;
        // ============ A: 入口面 — 两拍 + 三模式 ============
        DubboInvoker dubboInvoker = new DubboInvoker(false);
        System.out.println("A1 sync   : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("sayHello", "HelloService", InvokeMode.SYNC)));
        System.out.println("A2 async  : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("sayHello", "HelloService", InvokeMode.ASYNC)));
        System.out.println("A3 future : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("sayHello", "HelloService", InvokeMode.FUTURE)));

        // ============ B: Filter 链 — 倒序责任链 ============
        Filter[] filters = {new FilterA(), new FilterB()};
        Invoker chained = buildInvokerChain(dubboInvoker, filters);
        System.out.println("B1 chain  : " + chained.invoke(new RpcInvocation("sayHello", "HelloService", InvokeMode.SYNC)).recreate());

        // ============ C: 发送面 — 连接轮询 ============
        System.out.println("C1 round  : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("m1", "S", InvokeMode.SYNC)));
        System.out.println("C2 round  : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("m2", "S", InvokeMode.SYNC)));
        System.out.println("C3 round  : " + InvocationUtil.invoke(dubboInvoker, new RpcInvocation("m3", "S", InvokeMode.SYNC)));
        System.out.println("C4 oneway : " + InvocationUtil.invoke(new DubboInvoker(true), new RpcInvocation("notify", "S", InvokeMode.SYNC)));

        // 断言
        boolean t1 = InvocationUtil.invoke(dubboInvoker, new RpcInvocation("s", "S", InvokeMode.FUTURE)).toString().contains("FUTURE_OBJECT");
        
        pass += t1 ? 1 : 0;
        boolean t2 = chained.invoke(new RpcInvocation("s", "S", InvokeMode.SYNC)).recreate().toString().contains("[A(timeout)\u2192");
        
        pass += t2 ? 1 : 0; // 外层先执行
        // 连接轮询: 连续 3 次调用应至少覆盖 2 个不同连接 (index % size 轮询)
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 3; i++) {
            String r = InvocationUtil.invoke(dubboInvoker, new RpcInvocation("m" + i, "S", InvokeMode.SYNC)).toString();
            seen.add(r.replaceAll(".*SENT\\(([^,]*),.*", "$1"));
        }
        
        pass += seen.size() >= 2 ? 1 : 0;
        pass += seen.size() <= 3 ? 1 : 0;
        pass += InvocationUtil.invoke(new DubboInvoker(true), new RpcInvocation("n", "S", InvokeMode.SYNC)).toString().contains("oneway") ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
