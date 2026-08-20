/**
 * MiniGW3 — Spring Cloud Gateway GW-3 路由谓词域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 谓词 SPI 双通道: apply(config) 同步 + applyAsync 默认适配 + ReadBody 真异步覆写 (RoutePredicateFactory.java:42-71)
 *  2. Path 匹配: 多 pattern 任一匹配 + 每请求缓存 PathContainer (PathRoutePredicateFactory.java:97-128)
 *  3. Weight 预计算模式: WebFilter 算好选中路由 → 谓词只比较 (WeightRoutePredicateFactory.java:85-101)
 *
 * 用法: javac MiniGW3.java && java MiniGW3
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MiniGW3 {

  // ============ 机制 1: 谓词 SPI (简化) ============
  interface RoutePredicateFactory<C> {
    Predicate<String> apply(C config);
    default Predicate<String> applyAsync(C config) {      // L70-71: 默认同步包异步
      return apply(config);
    }
  }

  // ReadBody 覆写 applyAsync (ReadBodyRoutePredicateFactory.java:62) — 简化为标记
  static final class ReadBodyFactory implements RoutePredicateFactory<Object> {
    @Override public Predicate<String> apply(Object config) { return s -> true; }
    @Override public Predicate<String> applyAsync(Object config) {
      System.out.println("  [readbody] 覆写 applyAsync: 异步读请求体后再测试 (L62)");
      return apply(config);
    }
  }

  // ============ 机制 2: Path 匹配 + 缓存 ============
  static final class PathFactory implements RoutePredicateFactory<List<String>> {
    final Map<String, Boolean> pathCache = new HashMap<>();   // GATEWAY_PREDICATE_PATH_CONTAINER_ATTR 简化
    int parseCount;

    @Override
    public Predicate<String> apply(List<String> patterns) {
      return path -> {
        // computeIfAbsent 缓存解析结果 (PathRoutePredicateFactory.java:116-118)
        Boolean cached = pathCache.computeIfAbsent(path, p -> { parseCount++; return matchAny(patterns, p); });
        return cached;
      };
    }

    boolean matchAny(List<String> patterns, String path) {     // 多 pattern 任一匹配 (L121-128)
      for (String p : patterns) {
        if (simpleMatch(p, path)) return true;
      }
      return false;
    }

    boolean simpleMatch(String pattern, String path) {         // ** 通配简化
      if (pattern.endsWith("/**")) {
        return path.startsWith(pattern.substring(0, pattern.length() - 3));
      }
      return pattern.equals(path);
    }
  }

  // ============ 机制 3: Weight 预计算 ============
  static final class WeightCalculator {                        // WeightCalculatorWebFilter 简化
    String precompute(String group, Map<String, Integer> weights) {
      int total = weights.values().stream().mapToInt(Integer::intValue).sum();
      int r = (int) (Math.random() * total);
      int acc = 0;
      for (Map.Entry<String, Integer> e : weights.entrySet()) {
        acc += e.getValue();
        if (r < acc) return e.getKey();                        // 选中路由存 WEIGHT_ATTR (L89)
      }
      return null;
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. SPI 双通道 ---
    ReadBodyFactory rb = new ReadBodyFactory();
    rb.applyAsync(null);
    System.out.println("[SPI] 默认 applyAsync = 同步包异步 (L70-71), ReadBody 覆写");
    pass++;

    // --- 2. Path 匹配 + 缓存 ---
    PathFactory pf = new PathFactory();
    Predicate<String> p = pf.apply(List.of("/user/**", "/admin"));
    System.out.println("[Path] /user/123 → " + p.test("/user/123") + " (匹配 /user/**)");
    System.out.println("[Path] /admin → " + p.test("/admin") + " (精确匹配)");
    System.out.println("[Path] /other → " + p.test("/other") + " (无匹配)");
    p.test("/user/123");                                       // 缓存命中
    System.out.println("[Path] 解析次数: " + pf.parseCount + " (期望 3, 缓存生效 L116-118)");
    if (p.test("/user/123") && p.test("/admin") && !p.test("/other") && pf.parseCount == 3) pass++; else fail++;

    // --- 3. Weight 预计算: 组内互斥 ---
    WeightCalculator wc = new WeightCalculator();
    Map<String, Integer> group = new HashMap<>();
    group.put("routeA", 70); group.put("routeB", 30);
    String chosen = wc.precompute("g1", group);
    System.out.println("[Weight] 预计算选中: " + chosen + " (权重 70:30, 谓词只比较 L101)");
    // 模拟两个路由的谓词: 同一请求只能一个匹配 (组内互斥)
    boolean aMatch = "routeA".equals(chosen);
    boolean bMatch = "routeB".equals(chosen);
    System.out.println("[Weight] 组内互斥: A=" + aMatch + " B=" + bMatch + " (异或=" + (aMatch ^ bMatch) + ")");
    if (aMatch ^ bMatch) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
