import java.util.*;

public class MiniLoadBalancerClient {

    // ---- A: 基本版 (choose → 503/实例 → transformer) ----
    static class ServiceInstance { final String host; final int port; ServiceInstance(String h, int p) { host = h; port = p; } public String toString() { return host + ":" + port; } }

    static class LoadBalancerClient {
        final Map<String, List<ServiceInstance>> registry = new HashMap<>();
        ServiceInstance choose(String serviceId) {
            List<ServiceInstance> instances = registry.getOrDefault(serviceId, Collections.emptyList());
            return instances.isEmpty() ? null : instances.get(0); // choose 简化
        }
    }

    interface RequestTransformer { Map<String, String> transform(Map<String, String> headers, ServiceInstance instance); }

    static class XForwardedTransformer implements RequestTransformer {
        boolean enabled = true;
        public Map<String, String> transform(Map<String, String> h, ServiceInstance i) {
            if (enabled && i != null) {
                h.put("X-Forwarded-Host", i.host); // L55
                h.put("X-Forwarded-Proto", "http");
            }
            return h;
        }
    }

    static class FeignBlockingLoadBalancerClient {
        final LoadBalancerClient lb;
        final List<RequestTransformer> transformers;
        FeignBlockingLoadBalancerClient(LoadBalancerClient lb, List<RequestTransformer> t) { this.lb = lb; this.transformers = t; }

        String execute(String url) {
            String serviceId = URI_HOST(url);
            ServiceInstance instance = lb.choose(serviceId);
            if (instance == null) {
                return "503: Load balancer does not contain an instance for the service " + serviceId; // L127-131
            }
            Map<String, String> headers = new HashMap<>();
            for (RequestTransformer t : transformers) headers = t.transform(headers, instance); // 链
            return "200: " + "http://" + instance + url.substring(url.indexOf('/')); // reconstructURI
        }
        static String URI_HOST(String url) { return url.split("://")[1].split("/")[0]; }
    }

    // ---- B: 装配条件 (无 LoadBalancer → OF-2 陷阱) ----
    static class AutoConfig {
        static boolean hasLoadBalancerBean = false;
        static String getClient() {
            if (!hasLoadBalancerBean) {
                throw new IllegalStateException("Did you forget to include spring-cloud-starter-loadbalancer?"); // OF-2 L449
            }
            return "FeignBlockingLoadBalancerClient";
        }
    }

    // ---- C: Retryable 三态 ----
    enum RetryState { BACKOFF, NEVER, INTERCEPTOR }
    static class RetryTemplate {
        static String execute(RetryState state, int failCount) {
            if (state == RetryState.NEVER) return "FAIL(no retry)"; // NeverRetryPolicy
            for (int i = 0; i <= failCount; i++) {
                if (i == failCount) return "OK(retry=" + i + ", backoff=" + (state == RetryState.BACKOFF ? "yes" : "no") + ")";
            }
            return "FAIL";
        }
    }

    // ---- D: hint ----
    static class HintResolver {
        static String getHint(Map<String, String> hints, String serviceId) {
            String h = hints.get(serviceId);
            return h != null ? h : hints.getOrDefault("default", "default"); // L163-165
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 基本版 ============
        LoadBalancerClient lb = new LoadBalancerClient();
        lb.registry.put("payment-service", Arrays.asList(new ServiceInstance("10.0.0.1", 8080)));
        FeignBlockingLoadBalancerClient client = new FeignBlockingLoadBalancerClient(lb, Arrays.asList(new XForwardedTransformer()));
        System.out.println("A1 ok     : " + client.execute("http://payment-service/orders"));
        System.out.println("A2 503    : " + client.execute("http://user-service/orders"));

        // ============ B: 装配陷阱 ============
        AutoConfig.hasLoadBalancerBean = false;
        try { AutoConfig.getClient(); } catch (IllegalStateException e) {
            System.out.println("B1 trap   : " + e.getMessage());
        }
        AutoConfig.hasLoadBalancerBean = true;
        System.out.println("B2 ok     : " + AutoConfig.getClient());

        // ============ C: 重试三态 ============
        System.out.println("C1 never  : " + RetryTemplate.execute(RetryState.NEVER, 2));
        System.out.println("C2 retry  : " + RetryTemplate.execute(RetryState.BACKOFF, 2));

        // ============ D: hint ============
        Map<String, String> hints = new HashMap<>();
        hints.put("default", "default");
        hints.put("zone-a-service", "zone-a");
        System.out.println("D1 hint   : " + HintResolver.getHint(hints, "zone-a-service") + " / " + HintResolver.getHint(hints, "other"));

        // 断言
        pass += client.execute("http://payment-service/orders").startsWith("200") ? 1 : 0; // A
        pass += client.execute("http://user-service/orders").startsWith("503") ? 1 : 0; // A
        pass += RetryTemplate.execute(RetryState.NEVER, 2).contains("no retry") ? 1 : 0; // C
        pass += RetryTemplate.execute(RetryState.BACKOFF, 2).contains("retry=2") ? 1 : 0; // C
        pass += HintResolver.getHint(hints, "zone-a-service").equals("zone-a") ? 1 : 0; // D
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
