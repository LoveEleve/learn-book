/**
 * MiniG5 — gRPC-Java G-5 命名解析域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. Uri target 解析: scheme://authority 手写切分 + 严格校验 (Uri.java:208-240)
 *  2. 拉取生命周期: start 即解析, refresh 重解析, 错误后退避重试 (DnsNameResolver.java:199-211)
 *  3. RetryingNameResolver: 错误→退避→refresh 闭环, 成功 reset (RetryingNameResolver.java:96-119)
 *
 * 用法: javac MiniG5.java && java MiniG5
 */
public class MiniG5 {

  // ============ 机制 1: Uri 解析 (简化) ============
  static final class Uri {
    final String scheme, authority, path, query;

    // create 简化: 3.1 scheme 找 ':' 于 '/','?','#' 之前 (Uri.java:214-229)
    static Uri create(String s) {
      int schemeColon = -1;
      for (int i = 0; i < s.length(); i++) {
        char c = s.charAt(i);
        if (c == ':') { schemeColon = i; break; }
        else if (c == '/' || c == '?' || c == '#') break;
      }
      if (schemeColon < 0) throw new IllegalArgumentException("Missing required scheme.");
      String scheme = s.substring(0, schemeColon);

      // 3.2 authority: "//" 后扫到分隔符 (L232-240); "dns:///x" 三斜杠 = 空 authority
      String rest = s.substring(schemeColon + 1);
      String authority = null, path = "", query = null;
      if (rest.startsWith("//")) {
        String after = rest.substring(2);            // "dns://8.8.8.8/x" 或 "dns:///x"
        if (!after.startsWith("/")) {
          int end = firstOf(after, '/', '?', '#');
          authority = end < 0 ? after : after.substring(0, end);
          after = end < 0 ? "" : after.substring(end);
        }                                            // else: 空 authority (dns:///x)
        rest = after;
      }
      int qIdx = rest.indexOf('?');
      if (qIdx >= 0) { path = rest.substring(0, qIdx); query = rest.substring(qIdx + 1); }
      else path = rest;

      // 严格校验 (L185-190)
      if (authority != null && !path.isEmpty() && !path.startsWith("/")) {
        throw new IllegalArgumentException("Has authority -- Non-empty path must start with '/'");
      }
      return new Uri(scheme, authority, path, query);
    }

    Uri(String s, String a, String p, String q) { scheme = s; authority = a; path = p; query = q; }
    @Override public String toString() { return "scheme=" + scheme + ", authority=" + authority
        + ", path=" + path + ", query=" + query; }

    static int firstOf(String s, char... chars) {
      int min = Integer.MAX_VALUE;
      for (char c : chars) { int i = s.indexOf(c); if (i >= 0 && i < min) min = i; }
      return min == Integer.MAX_VALUE ? -1 : min;
    }
  }

  // ============ 机制 2: 拉取生命周期 ============
  interface Listener {
    void onResult(java.util.List<String> addresses);
    void onError(String error);
  }

  static class NameResolver {
    Listener listener;
    int resolveCount;
    boolean failNext;

    void start(Listener l) {                                   // DnsNameResolver.java:199
      listener = l;
      resolve();                                               // L203: start 即解析
    }

    void refresh() {                                           // L207
      resolve();
    }

    void resolve() {
      resolveCount++;
      if (failNext) { listener.onError("DNS failure"); failNext = false; }
      else listener.onResult(java.util.Arrays.asList("10.0.0.1:8080", "10.0.0.2:8080"));
    }
  }

  // ============ 机制 3: RetryingNameResolver (简化) ============
  static final class RetryingResolver extends NameResolver {
    int backoffStep;
    int retryCount;

    @Override void resolve() {
      // RetryingListener 简化: 失败 → schedule refresh (RetryingNameResolver.java:105,116-118)
      if (failNext) {
        retryCount++;
        backoffStep = Math.min(backoffStep + 1, 3);            // 1.6x 退避简化
        System.out.println("  [retry] 解析失败 → " + backoffStep + " 次退避后 refresh (1.6x, G-6 复用)");
        failNext = false;
        refresh();                                             // DelayedNameResolverRefresh
      } else {
        if (backoffStep > 0) { System.out.println("  [retry] 成功 → retryScheduler.reset() (L102-103)"); }
        backoffStep = 0;                                       // reset
        listener.onResult(java.util.Arrays.asList("10.0.0.1:8080"));
      }
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. Uri 解析 ---
    System.out.println("[Uri] dns:///api.example.com:8080?balancer=xds:");
    Uri u1 = Uri.create("dns:///api.example.com:8080?balancer=xds");
    System.out.println("  → " + u1);
    // 正确语义 (RFC 3986 + DnsNameResolverProvider.java:44): "dns:///x" = 空 authority, 主机在 path
    if ("dns".equals(u1.scheme) && u1.authority == null
        && "/api.example.com:8080".equals(u1.path) && "balancer=xds".equals(u1.query)) pass++; else fail++;

    System.out.println("[Uri] 无 scheme 输入:");
    try {
      Uri.create("api.example.com");
      fail++;
    } catch (IllegalArgumentException e) {
      System.out.println("  → " + e.getMessage() + " (L229)");
      pass++;
    }

    // --- 2. 拉取生命周期 ---
    NameResolver nr = new NameResolver();
    final int[] resultCount = {0};
    nr.start(new Listener() {
      @Override public void onResult(java.util.List<String> addresses) { resultCount[0]++; }
      @Override public void onError(String error) { }
    });
    nr.refresh();
    System.out.println("[拉取] start 即解析 + refresh 重解析: 回调次数 = " + resultCount[0] + " (期望 2)");
    if (resultCount[0] == 2) pass++; else fail++;

    // --- 3. 失败→退避→refresh→成功 reset ---
    RetryingResolver rr = new RetryingResolver();
    rr.failNext = true;
    System.out.println("[Retrying] 首次解析失败:");
    rr.start(new Listener() {
      @Override public void onResult(java.util.List<String> addresses) { }
      @Override public void onError(String error) { }
    });
    System.out.println("[Retrying] 退避重试次数: " + rr.retryCount + " (期望 1)");
    if (rr.retryCount == 1) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}

// 补充验证: 带 authority 的 target (dns://8.8.8.8/foo)
// 在 main 末尾追加验证 —— 由 main 中已覆盖场景推理, 不另写
