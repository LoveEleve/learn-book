import java.util.*;

public class MiniFeignProxy {

    // ---- A: 双路径 (无 url → LB / 有 url → unwrap) ----
    static class Client {
        String kind;
        Client(String kind) { this.kind = kind; }
        Client getDelegate() { return new Client("DELEGATE(" + kind + ")"); } // unwrap
    }

    static class FactoryBean {
        String url, name;
        Client client;

        String getTarget() {
            if (url == null || url.isEmpty()) {
                // 路径 1: 无 url → loadBalance
                if (client == null) {
                    throw new IllegalStateException(
                            "Did you forget to include spring-cloud-starter-loadbalancer?"); // L449-451 生产陷阱
                }
                return "PROXY[LB](" + name + "@" + client.kind + ")";
            }
            // 路径 2: 有 url → unwrap LB
            if (client instanceof LBClient) {
                client = ((LBClient) client).getDelegate(); // "not load balancing because we have a url"
            }
            return "PROXY[DIRECT](" + url + " via " + client.kind + ")";
        }
    }

    static class LBClient extends Client {
        LBClient() { super("FeignBlockingLoadBalancerClient"); }
    }

    // ---- B: 组件装配 (子上下文 + 继承开关) ----
    static class FeignClientFactory {
        final Map<String, Object> own = new HashMap<>();
        final Map<String, Object> parent = new HashMap<>();
        Object getInstance(String contextId, String type, boolean inherit) {
            if (inherit && !own.containsKey(type) && parent.containsKey(type)) {
                return parent.get(type); // 父上下文继承
            }
            return own.get(type);
        }
    }

    // ---- C: 超时三级优先级 (逐项 fallback) ----
    static class Options {
        final int connect, read; final boolean followRedirects;
        Options(int c, int r, boolean f) { connect = c; read = r; followRedirects = f; }
        public String toString() { return "Options(connect=" + connect + ", read=" + read + ", follow=" + followRedirects + ")"; }
    }

    static class OptionsFactoryBean {
        static Options create(Integer propConnect, Integer propRead, Boolean propFollow, Options defaults) {
            int connect = propConnect != null ? propConnect : defaults.connect;      // 逐项 fallback
            int read = propRead != null ? propRead : defaults.read;
            boolean follow = propFollow != null ? propFollow : defaults.followRedirects;
            return new Options(connect, read, follow);
        }
    }

    // ---- D: Target 三分支 ----
    static class HardCodedTarget { final String url; HardCodedTarget(String u) { url = u; } public String toString() { return "HardCoded(" + url + ")"; } }
    static class RefreshableHardCodedTarget extends HardCodedTarget { RefreshableHardCodedTarget(String u) { super(u); } public String toString() { return "Refreshable(" + url + ")"; } } // OF-9
    static class PropertyBasedTarget extends HardCodedTarget { PropertyBasedTarget(String u) { super(u); } public String toString() { return "PropertyBased(懒加载)"; } }

    static class Resolver {
        static HardCodedTarget resolve(String url, boolean hasRefreshable, boolean hasProperty) {
            if (url != null && !url.isEmpty()) return new HardCodedTarget(url);
            if (hasRefreshable) return new RefreshableHardCodedTarget("refresh://dynamic");
            return new PropertyBasedTarget(null);
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 双路径 ============
        FactoryBean lb = new FactoryBean();
        lb.name = "order-service";
        lb.client = new LBClient();
        System.out.println("A1 lb     : " + lb.getTarget());

        FactoryBean direct = new FactoryBean();
        direct.url = "http://payment";
        direct.client = new LBClient();
        System.out.println("A2 direct : " + direct.getTarget() + " (unwrap 剥 LB)");

        FactoryBean noLb = new FactoryBean();
        noLb.name = "user-service";
        try { noLb.getTarget(); } catch (IllegalStateException e) {
            System.out.println("A3 trap   : " + e.getMessage());
        }

        // ============ B: 继承开关 ============
        FeignClientFactory ctx = new FeignClientFactory();
        ctx.parent.put("Encoder", "parent-encoder");
        ctx.own.put("Contract", "own-contract");
        System.out.println("B1 inherit: " + ctx.getInstance("c1", "Encoder", true) + " (父继承)");
        System.out.println("B2 no-inh : " + ctx.getInstance("c1", "Encoder", false) + " (null, 仅子上下文)");

        // ============ C: 超时逐项 fallback ============
        Options defaults = new Options(1000, 5000, false);
        Options partial = OptionsFactoryBean.create(null, 10000, null, defaults);
        System.out.println("C1 partial: " + partial + " (connect/follow 回退默认)");

        // ============ D: Target 三分支 ============
        System.out.println("D1 target : " + Resolver.resolve(null, true, false) + " (OF-9 Refreshable)");
        System.out.println("D2 target : " + Resolver.resolve(null, false, true) + " (懒加载)");

        // 断言
        pass += lb.getTarget().contains("[LB]") ? 1 : 0;
        pass += direct.getTarget().contains("DELEGATE") ? 1 : 0; // unwrap
        pass += ctx.getInstance("c1", "Encoder", true) != null && ctx.getInstance("c1", "Encoder", false) == null ? 1 : 0;
        pass += partial.connect == 1000 && partial.read == 10000 ? 1 : 0; // 逐项 fallback
        pass += Resolver.resolve(null, true, false) instanceof RefreshableHardCodedTarget ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
