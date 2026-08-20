/**
 * MiniGW2 — Spring Cloud Gateway GW-2 过滤器链域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 链装配: 全局+路由过滤器合并 + order 排序 (FilteringWebHandler.java:124-130)
 *  2. 链执行: 不可变索引递归 + 可中断 (FilteringWebHandler.java:153-166)
 *  3. 短路配置绑定: 短路 Map → 归一化 (ShortcutConfigurable.java:107-125)
 *
 * 用法: javac MiniGW2.java && java MiniGW2
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniGW2 {

  interface Filter {
    String name();
    int order();
    String run(String input, Chain chain);   // 返回 null = 中断链
  }

  static class Chain {                        // DefaultGatewayFilterChain 简化
    final List<Filter> filters;
    final int index;
    Chain(List<Filter> fs, int i) { filters = fs; index = i; }

    String filter(String exchange) {          // L153-166: 递归 + 可中断
      if (index < filters.size()) {
        Filter f = filters.get(index);
        System.out.println("  [chain] 执行 " + f.name() + " (index=" + index + ")");
        return f.run(exchange, new Chain(filters, index + 1));  // 过滤器决定是否调下游
      }
      return "转发: " + exchange + " (Mono.empty = 完成)";
    }
  }

  static final class GlobalFilterAdapter implements Filter {   // GatewayFilterAdapter
    final Filter delegate;
    GlobalFilterAdapter(Filter d) { delegate = d; }
    public String name() { return delegate.name(); }
    public int order() { return delegate.order(); }
    public String run(String i, Chain c) { return delegate.run(i, c); }
  }

  // ============ 短路配置绑定 (简化) ============
  static Map<String, Object> normalizeDefault(Map<String, String> args) {  // ShortcutConfigurable DEFAULT
    Map<String, Object> map = new HashMap<>();
    int idx = 0;
    for (Map.Entry<String, String> e : args.entrySet()) {
      map.put(idx == 0 ? "name" : "value", e.getValue());       // normalizeKey 简化
      idx++;
    }
    return map;
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 链装配: 合并 + 排序 (getAllFilters L124-130) ---
    Filter auth = new GlobalFilterAdapter(new Filter() {
      public String name() { return "auth(global)"; }
      public int order() { return 1; }
      public String run(String i, Chain c) { return "通过(" + name() + ") → " + c.filter(i); }
    });
    Filter log = new GlobalFilterAdapter(new Filter() {
      public String name() { return "log(global)"; }
      public int order() { return 3; }
      public String run(String i, Chain c) { return "[" + name() + "] " + c.filter(i); }
    });
    Filter rateLimit = new Filter() {                            // 路由过滤器
      public String name() { return "rateLimit(route)"; }
      public int order() { return 2; }
      public String run(String i, Chain c) { return "限流(" + name() + ") → " + c.filter(i); }
    };

    List<Filter> combined = new ArrayList<>();
    combined.addAll(List.of(auth, log));                         // globalFilters
    combined.add(rateLimit);                                     // route.getFilters()
    combined.sort((a, b) -> Integer.compare(a.order(), b.order())); // AnnotationAwareOrderComparator
    System.out.println("[装配] 合并排序后: " + combined.stream().map(Filter::name).toList());
    String result = new Chain(combined, 0).filter("/user/123");
    System.out.println("[执行] 结果: " + result);
    if (combined.get(0).order() == 1 && combined.get(1).order() == 2) pass++; else fail++;

    // --- 2. 可中断: 过滤器不调下游 (鉴权失败) ---
    Filter deny = new Filter() {
      public String name() { return "deny"; }
      public int order() { return 0; }
      public String run(String i, Chain c) { return "401 拒绝 (不调下游 = 短路)"; }
    };
    String denied = new Chain(List.of(deny, auth), 0).filter("/admin");
    System.out.println("[中断] " + denied);
    if (denied.contains("401")) pass++; else fail++;

    // --- 3. 短路绑定: AddRequestHeader=X-Request,value ---
    Map<String, String> shortcut = new HashMap<>();
    shortcut.put("0", "X-Request"); shortcut.put("1", "value");
    Map<String, Object> normalized = normalizeDefault(shortcut);
    System.out.println("[绑定] 短路 'X-Request,value' → " + normalized + " (DEFAULT 归一化 L114)");
    if ("X-Request".equals(normalized.get("name")) && "value".equals(normalized.get("value"))) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
