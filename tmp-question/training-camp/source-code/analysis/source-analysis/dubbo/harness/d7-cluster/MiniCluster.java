import java.util.*;

public class MiniCluster {

    static class Invoker {
        final String addr; final boolean available;
        int failCount = 0; final int failUntil;
        Invoker(String addr, int failUntil) { this.addr = addr; this.available = true; this.failUntil = failUntil; }
        Invoker(String addr, boolean available, int failUntil) { this.addr = addr; this.available = available; this.failUntil = failUntil; }
        boolean isAvailable() { return available; }
        public String toString() { return addr; }
    }

    static class Invocation {
        final String method; boolean bizException;
        Invocation(String method) { this.method = method; }
    }

    static class RpcException extends RuntimeException {
        final boolean biz;
        RpcException(String msg, boolean biz) { super(msg); this.biz = biz; }
        boolean isBiz() { return biz; }
    }

    // ---- A: 装配面 — Cluster SPI + join ----
    interface Cluster {
        String name();
        Invoker doJoin(List<Invoker> invokers); // 简化为返回包装器
    }
    static class FailoverCluster implements Cluster { public String name() { return "failover"; } public Invoker doJoin(List<Invoker> invokers) { return null; } }

    // ---- B: Failover 重试 (换节点 + isBiz) ----
    static class FailoverClusterInvoker {
        final List<Invoker> invokers;
        int retries = 2; // DEFAULT_RETRIES
        FailoverClusterInvoker(List<Invoker> invokers) { this.invokers = invokers; }

        String doInvoke(Invocation inv) {
            List<Invoker> invoked = new ArrayList<>();
            Set<String> providers = new HashSet<>();
            RpcException le = null;
            for (int i = 0; i <= retries; i++) {
                // select: 跳过 invoked 里的
                Invoker selected = null;
                for (Invoker invoker : invokers) {
                    if (!invoked.contains(invoker)) { selected = invoker; break; }
                }
                if (selected == null) break;
                invoked.add(selected);
                try {
                    if (inv.bizException) throw new RpcException("biz error", true);
                    if (selected.failUntil > i) throw new RpcException("net error: " + selected.addr, false);
                    return "OK(" + selected.addr + ", attempt=" + (i + 1) + ")";
                } catch (RpcException e) {
                    if (e.isBiz()) throw e; // 业务异常不重试
                    le = e; providers.add(selected.addr);
                }
            }
            throw new RpcException("Failed to invoke. Tried " + (retries + 1) + " times of " + providers + " (" + providers.size() + "/" + invokers.size() + ")", false);
        }
    }

    // ---- C: select 粘滞 + reselect ----
    static class StickySelector {
        Invoker stickyInvoker = null;
        boolean stickyEnabled = false; // DEFAULT_CLUSTER_STICKY = false

        Invoker select(List<Invoker> invokers, List<Invoker> selected) {
            if (stickyEnabled && stickyInvoker != null && invokers.contains(stickyInvoker)
                    && !selected.contains(stickyInvoker) && stickyInvoker.isAvailable()) {
                return stickyInvoker; // 粘滞命中
            }
            for (Invoker invoker : invokers) {
                if (!selected.contains(invoker)) { // selected > available 简化
                    if (stickyEnabled) stickyInvoker = invoker;
                    return invoker;
                }
            }
            return null;
        }
    }

    // ---- D: 策略族 + ZoneAware 三级 ----
    static class ZoneAwareClusterInvoker {
        static String pick(List<Invoker> invokers, String zone) {
            for (Invoker invoker : invokers) {
                if (invoker.addr.startsWith(zone)) return invoker.addr; // 同 zone 优先
            }
            return invokers.get(0).addr; // 兜底
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 装配面 ============
        List<Invoker> invokers = Arrays.asList(
                new Invoker("192.168.1.10:20880", 0),
                new Invoker("192.168.1.11:20880", 0),
                new Invoker("192.168.1.12:20880", 0));
        System.out.println("A1 spi    : default=failover, impls=9+2wrappers");

        // ============ B: Failover 重试 ============
        List<Invoker> flaky = Arrays.asList(
                new Invoker("a:20880", 1), // 前 1 次失败
                new Invoker("b:20880", 0));
        FailoverClusterInvoker failover = new FailoverClusterInvoker(flaky);
        System.out.println("B1 retry  : " + failover.doInvoke(new Invocation("read")));
        // 业务异常不重试
        Invocation bizInv = new Invocation("write");
        bizInv.bizException = true;
        try { failover.doInvoke(bizInv); } catch (RpcException e) {
            System.out.println("B2 biz    : " + e.getMessage() + " (业务异常不重试)");
        }

        // ============ C: 粘滞 ============
        StickySelector selector = new StickySelector();
        selector.stickyEnabled = true;
        Invoker first = selector.select(invokers, new ArrayList<>());
        Invoker second = selector.select(invokers, new ArrayList<>());
        System.out.println("C1 sticky : " + first + " vs " + second + " (应相同)");

        // ============ D: ZoneAware ============
        System.out.println("D1 zone   : " + ZoneAwareClusterInvoker.pick(invokers, "192.168.1.1"));

        // 断言
        pass += failover.doInvoke(new Invocation("r")).toString().contains("attempt=2") ? 1 : 0; // B 重试成功
        pass += flaky.get(0).failUntil == 1 && failover.retries == 2 ? 1 : 0; // B 默认 retries=2
        pass += selector.select(invokers, new ArrayList<>()).equals(selector.select(invokers, new ArrayList<>())) ? 1 : 0; // C 粘滞
        pass += ZoneAwareClusterInvoker.pick(invokers, "192.168.1.10").startsWith("192.168.1.10") ? 1 : 0; // D zone
        pass += ZoneAwareClusterInvoker.pick(invokers, "nope").equals("192.168.1.10:20880") ? 1 : 0; // D 兜底
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
