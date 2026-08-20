/**
 * MiniGW1 — Spring Cloud Gateway GW-1 路由定位域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 首匹配胜出: 逐路由谓词过滤, 第一个通过者胜出 (RoutePredicateHandlerMapping.java:130-140)
 *  2. 谓词 AND 组合: 多谓词 reduce(and) — 全部满足才匹配 (RouteDefinitionRouteLocator.java:198-209)
 *  3. 事件刷新: 配置/心跳事件 → RefreshRoutesEvent → 清缓存 → 重装配 (RouteRefreshListener.java:46-80)
 *
 * 用法: javac MiniGW1.java && java MiniGW1
 */
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class MiniGW1 {

  // ============ 机制 1+2: 路由与谓词 ============
  static final class Route {
    final String id, uri;
    final Predicate<String> predicate;          // AsyncPredicate 简化 (String = path)
    final int order;
    Route(String id, String uri, Predicate<String> p, int order) { this.id = id; this.uri = uri; this.predicate = p; this.order = order; }
  }

  // combinePredicates 简化: 多谓词 AND (RouteDefinitionRouteLocator.java:198-209)
  static Predicate<String> combine(List<Predicate<String>> predicates) {
    if (predicates.isEmpty()) return s -> true;              // 无谓词 = 全匹配 (L201-203)
    return predicates.stream().reduce(s -> true, Predicate::and); // L207: AND
  }

  // ============ 机制 3: 缓存 + 事件刷新 ============
  static final class RouteRegistry {
    final List<Route> routes = new ArrayList<>();
    List<Route> cached;
    int assembleCount;

    void setRoutes(List<Route> rs) { routes.clear(); routes.addAll(rs); }

    // CachingRouteLocator: 缓存 + fetch 时排序 (order 序) (CachingRouteLocator.java:62-63)
    List<Route> getRoutes() {
      if (cached == null) {
        assembleCount++;                                   // 缓存 miss → 重新装配 (fetch)
        cached = new ArrayList<>(routes);
        cached.sort((a, b) -> Integer.compare(a.order, b.order));  // AnnotationAwareOrderComparator
      }
      return cached;
    }

    // RouteRefreshListener 简化: 事件 → 清缓存 (RouteRefreshListener.java:75)
    void onEvent(String event) {
      if (event.equals("refresh") || event.equals("heartbeat")) {
        cached = null;
        System.out.println("  [refresh] 事件(" + event + ") → RefreshRoutesEvent → 清缓存 (L75)");
      }
    }

    // lookupRoute 简化: 首匹配胜出 (RoutePredicateHandlerMapping.java:130-140)
    Route lookup(String path) {
      for (Route r : getRoutes()) {
        if (r.predicate.test(path)) return r;
      }
      return null;                                            // → 404
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 谓词 AND 组合 ---
    Predicate<String> and = combine(List.of(
        s -> s.startsWith("/user"),
        s -> s.endsWith("/list") || !s.contains("admin")));
    System.out.println("[谓词] /user/123 AND 组合: " + and.test("/user/123") + " (两条都满足)");
    System.out.println("[谓词] /user/admin AND 组合: " + and.test("/user/admin") + " (第二条不满足)");
    if (and.test("/user/123") && !and.test("/user/admin")) pass++; else fail++;

    // --- 2. 首匹配胜出 (order 序) ---
    RouteRegistry reg = new RouteRegistry();
    reg.setRoutes(List.of(
        new Route("admin", "lb://admin", s -> s.contains("admin"), 1),
        new Route("user", "lb://user", s -> s.startsWith("/user"), 10)));
    System.out.println("[路由] 请求 /user/123 → " + reg.lookup("/user/123").id
        + " (order 10, 首匹配)");
    System.out.println("[路由] 请求 /admin/x → " + reg.lookup("/admin/x").id
        + " (order 1 优先)");
    if ("user".equals(reg.lookup("/user/123").id) && "admin".equals(reg.lookup("/admin/x").id)) pass++; else fail++;

    // --- 3. 404 + 缓存 + 事件刷新 ---
    System.out.println("[404] 请求 /unknown → " + (reg.lookup("/unknown") == null ? "null → WebFlux 404" : "?"));
    if (reg.lookup("/unknown") == null) pass++; else fail++;

    System.out.println("[缓存] 首次装配后 lookup: " + reg.assembleCount + " 次装配 (缓存命中, 不重装)");
    reg.lookup("/user/123"); reg.lookup("/user/123");
    if (reg.assembleCount == 1) pass++; else fail++;

    reg.onEvent("heartbeat");                                  // 服务发现心跳 → 清缓存
    reg.lookup("/user/123");                                   // 重装配
    System.out.println("[刷新] 心跳后重装配: " + reg.assembleCount + " 次 (缓存已清)");
    if (reg.assembleCount == 2) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
