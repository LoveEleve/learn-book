import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * MiniLoadBalanced — 微缩版 @LoadBalanced 拦截 (LoadBalancerInterceptor + BlockingLoadBalancerClient)
 *
 * 覆盖机制:
 * 1. 拦截: URL host 即服务名 (http://orders/api → serviceName=orders)
 * 2. LoadBalancerClient: execute 双形态 (选+发 / 指定+发) + choose + reconstructURI
 * 3. Blocking 包装: choose 阻塞式取实例
 * 4. 请求包装: BlockingLoadBalancerRequest 携带原始请求 + transformers
 * 5. URI 重建: 逻辑服务名 host → 实例 host:port
 */
public class MiniLoadBalanced {

    // ===== ServiceInstance 模型 =====

    public static class ServiceInstance {
        final String serviceId;
        final String host;
        final int port;

        ServiceInstance(String serviceId, String host, int port) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return serviceId + "@" + host + ":" + port;
        }
    }

    // ===== LoadBalancerClient 接口 =====

    public interface LoadBalancerRequest<T> {
        T apply(ServiceInstance instance);
    }

    public interface ServiceInstanceChooser {
        ServiceInstance choose(String serviceId);
    }

    public interface LoadBalancerClient extends ServiceInstanceChooser {
        <T> T execute(String serviceId, LoadBalancerRequest<T> request);

        <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request);

        URI reconstructURI(ServiceInstance instance, URI original);
    }

    // ===== 简单轮询实现 =====

    public static class SimpleLoadBalancer implements LoadBalancerClient {
        private final List<ServiceInstance> instances;
        private int position = 0;

        public SimpleLoadBalancer(List<ServiceInstance> instances) {
            this.instances = instances;
        }

        @Override
        public ServiceInstance choose(String serviceId) {
            // 轮询
            if (instances.isEmpty()) {
                return null;
            }
            ServiceInstance selected = instances.get(position % instances.size());
            position++;
            return selected;
        }

        @Override
        public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
            ServiceInstance instance = choose(serviceId);
            return request.apply(instance);
        }

        @Override
        public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) {
            return request.apply(serviceInstance);
        }

        @Override
        public URI reconstructURI(ServiceInstance instance, URI original) {
            // http://orders/api → http://host:port/api
            String newUri = original.toString().replaceFirst(instance.serviceId, instance.host + ":" + instance.port);
            return URI.create(newUri);
        }
    }

    // ===== 拦截器 =====

    public interface ClientHttpRequest {
        URI getURI();

        String getBody();
    }

    public interface ClientHttpRequestExecution {
        String execute(ClientHttpRequest request);
    }

    public static class HttpRequestImpl implements ClientHttpRequest {
        final URI uri;
        final String body;

        HttpRequestImpl(String url, String body) {
            this.uri = URI.create(url);
            this.body = body;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public String getBody() {
            return body;
        }
    }

    public static class LoadBalancerRequestFactory {
        private final LoadBalancerClient loadBalancer;
        private final List<Function<ClientHttpRequest, ClientHttpRequest>> transformers;

        public LoadBalancerRequestFactory(LoadBalancerClient loadBalancer) {
            this(loadBalancer, new ArrayList<>());
        }

        public LoadBalancerRequestFactory(LoadBalancerClient loadBalancer,
                List<Function<ClientHttpRequest, ClientHttpRequest>> transformers) {
            this.loadBalancer = loadBalancer;
            this.transformers = transformers;
        }

        public LoadBalancerRequest<String> createRequest(ClientHttpRequest request, ClientHttpRequestExecution execution) {
            // 返回一个 request: 选定实例后重建 URI 再执行
            return instance -> {
                URI newUri = loadBalancer.reconstructURI(instance, request.getURI());
                ClientHttpRequest transformed = request;
                for (Function<ClientHttpRequest, ClientHttpRequest> t : transformers) {
                    transformed = t.apply(transformed);
                }
                HttpRequestImpl newRequest = new HttpRequestImpl(newUri.toString(), transformed.getBody());
                return execution.execute(newRequest);
            };
        }
    }

    public static class LoadBalancerInterceptor {
        private final LoadBalancerClient loadBalancer;
        private final LoadBalancerRequestFactory requestFactory;

        public LoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRequestFactory requestFactory) {
            this.loadBalancer = loadBalancer;
            this.requestFactory = requestFactory;
        }

        public String intercept(ClientHttpRequest request, ClientHttpRequestExecution execution) {
            URI originalUri = request.getURI();
            String serviceName = originalUri.getHost(); // URL host 即服务名
            if (serviceName == null) {
                throw new IllegalStateException("Request URI does not contain a valid hostname: " + originalUri);
            }
            return loadBalancer.execute(serviceName,
                    requestFactory.createRequest(request, execution));
        }
    }

    // ===== 测试 =====

    static int passCount = 0;
    static int failCount = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("PASS: " + name);
        } else {
            failCount++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) {
        // ---- 1. URL host 即服务名 ----
        List<ServiceInstance> instances = List.of(
                new ServiceInstance("orders", "10.0.0.1", 8080),
                new ServiceInstance("orders", "10.0.0.2", 8080));
        SimpleLoadBalancer lb = new SimpleLoadBalancer(instances);
        LoadBalancerRequestFactory factory = new LoadBalancerRequestFactory(lb);
        LoadBalancerInterceptor interceptor = new LoadBalancerInterceptor(lb, factory);

        // ---- 2. 拦截 + URI 重建 (轮询选择) ----
        List<String> executedUrls = new ArrayList<>();
        ClientHttpRequestExecution execution = req -> {
            executedUrls.add(req.getURI().toString());
            return "resp:" + req.getURI().toString();
        };

        String r1 = interceptor.intercept(new HttpRequestImpl("http://orders/api/orders/1", ""), execution);
        check("拦截: 服务名从 host 提取 (orders)", executedUrls.get(0).contains("10.0.0.1:8080/api/orders/1"));
        check("拦截: 请求执行返回", r1.startsWith("resp:"));

        String r2 = interceptor.intercept(new HttpRequestImpl("http://orders/api/orders/2", ""), execution);
        check("轮询: 第二次选 10.0.0.2", executedUrls.get(1).contains("10.0.0.2:8080"));

        // ---- 3. 轮询第三次回 10.0.0.1 ----
        interceptor.intercept(new HttpRequestImpl("http://orders/api/orders/3", ""), execution);
        check("轮询: 第三次回 10.0.0.1", executedUrls.get(2).contains("10.0.0.1:8080"));

        // ---- 4. 无 host 抛错 ----
        boolean threw = false;
        try {
            interceptor.intercept(new HttpRequestImpl("not-a-url", ""), execution);
        } catch (IllegalStateException e) {
            threw = e.getMessage().contains("valid hostname");
        }
        check("校验: 无 host 抛 IllegalStateException", threw);

        // ---- 5. transformers 变换请求 ----
        List<Function<ClientHttpRequest, ClientHttpRequest>> transformers = new ArrayList<>();
        transformers.add(req -> new HttpRequestImpl(req.getURI().toString(), req.getBody() + "+TRANSFORMED"));
        LoadBalancerRequestFactory factory2 = new LoadBalancerRequestFactory(lb, transformers);
        LoadBalancerInterceptor interceptor2 = new LoadBalancerInterceptor(lb, factory2);
        List<String> bodies = new ArrayList<>();
        ClientHttpRequestExecution exec2 = req -> {
            bodies.add(req.getBody());
            return "ok";
        };
        interceptor2.intercept(new HttpRequestImpl("http://orders/api/x", "body"), exec2);
        check("transformers: 请求体被变换", bodies.get(0).equals("body+TRANSFORMED"));

        // ---- 6. execute 双形态 ----
        ServiceInstance specific = new ServiceInstance("orders", "10.0.0.9", 9999);
        String specified = lb.execute("orders", specific, instance -> "sent-to-" + instance.host);
        check("execute 指定实例", "sent-to-10.0.0.9".equals(specified));

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
