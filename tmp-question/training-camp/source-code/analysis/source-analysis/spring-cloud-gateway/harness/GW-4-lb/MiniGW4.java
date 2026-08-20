/**
 * MiniGW4 — Spring Cloud Gateway GW-4 负载均衡域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. URL 装配: 请求路径保留 + 路由 scheme/host 覆盖 (RouteToRequestUrlFilter.java:88-97)
 *  2. LB 解析: choose → 无实例 404/503 → reconstructURI 换 host (ReactiveLoadBalancerClientFilter.java:118-163)
 *  3. 服务发现路由: 实例 → 模板路由 (Path=/serviceId/**) (DiscoveryClientRouteDefinitionLocator.java:104-130)
 *
 * 用法: javac MiniGW4.java && java MiniGW4
 */
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class MiniGW4 {

  // ============ 机制 1: URL 装配 (简化) ============
  static String assembleUrl(String requestUri, String routeUri) {
    URI req = URI.create(requestUri);
    URI route = URI.create(routeUri);
    // RouteToRequestUrlFilter: 请求路径保留 + 路由 scheme/host/port 覆盖 (L88-96)
    String scheme = route.getScheme();
    String host = route.getHost();
    int port = route.getPort();
    String result = scheme + "://" + host + (port > 0 ? ":" + port : "") + req.getPath();
    return result;
  }

  // ============ 机制 2: LB 解析 ============
  static final class LbClient {                                  // ReactiveLoadBalancerClient 简化
    final List<String> instances = new ArrayList<>();
    int seq;

    LbClient(List<String> ins) { instances.addAll(ins); }

    String choose(String serviceId) {                            // L118: 每请求 choose (RoundRobin 简化)
      if (instances.isEmpty()) return null;
      return instances.get(seq++ % instances.size());
    }
  }

  static final class NotFoundException extends RuntimeException {
    NotFoundException(String msg) { super(msg); }
    static NotFoundException create(boolean use404, String msg) {  // L123
      return new NotFoundException((use404 ? "404 " : "503 ") + msg);
    }
  }

  // ============ 机制 3: 服务发现路由 ============
  static String buildRouteDefinition(String serviceId) {         // 模板: Path=/serviceId/** (SpEL 简化)
    return serviceId + " → Path=/" + serviceId + "/** → uri=lb://" + serviceId;
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. URL 装配: 路径保留 + host 覆盖 ---
    String url = assembleUrl("http://gateway:8080/user/123", "lb://user-service");
    System.out.println("[装配] lb://user-service + /user/123 → " + url);
    if (url.equals("lb://user-service/user/123")) pass++; else fail++;

    // --- 2. LB 解析: choose + reconstruct + 无实例 ---
    LbClient lb = new LbClient(List.of("10.0.0.1:8081", "10.0.0.2:8081"));
    String instance = lb.choose("user-service");
    System.out.println("[LB] choose → " + instance + " (每请求, L118)");
    String finalUrl = url.replace("lb://user-service", "http://" + instance);  // reconstructURI 简化 (真实版换 host+port+scheme: LoadBalancerUriTools doReconstructURI L97-111; scheme 已由 overrideScheme 处理)
    System.out.println("[LB] reconstruct → " + finalUrl + " (换 host/port, scheme 已覆盖, L162-163)");
    if (instance.startsWith("10.0.0.")) pass++; else fail++;

    LbClient empty = new LbClient(List.of());
    String none = empty.choose("user-service");
    if (none == null) {
      // 真实版: !response.hasServer() → NotFoundException.create(use404, ...) (L121-123)
      System.out.println("[LB] 无实例 → " + NotFoundException.create(true, "Unable to find instance for user-service").getMessage());
      pass++;
    } else { fail++; }

    // --- 3. 服务发现路由 ---
    String route = buildRouteDefinition("user-service");
    System.out.println("[发现] 自动路由: " + route);
    if (route.contains("Path=/user-service/**")) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
