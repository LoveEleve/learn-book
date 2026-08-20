/**
 * MiniGW8 — Spring Cloud Gateway GW-8 路径重写域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. RewritePath: 正则替换 + 更新转发目标 (RewritePathGatewayFilterFactory.java:62-71)
 *  2. StripPrefix/PrefixPath: 段操作 + 防重复标志 (StripPrefix.java:69-75 / PrefixPath.java:65-71)
 *  3. 原始链保留: LinkedHashSet 追加 (ServerWebExchangeUtils.java:294-296)
 *
 * 用法: javac MiniGW8.java && java MiniGW8
 */
import java.util.LinkedHashSet;
import java.util.Set;

public class MiniGW8 {

  // ============ 机制 3: 原始链保留 (ServerWebExchangeUtils 简化) ============
  static final class Exchange {
    String path;
    final Set<String> originalUrls = new LinkedHashSet<>();   // GATEWAY_ORIGINAL_REQUEST_URL_ATTR

    void addOriginalRequestUrl(String url) {                  // L294-296: 追加而非覆盖
      originalUrls.add(url);
    }
  }

  // ============ 机制 1: RewritePath ============
  static String rewritePath(String path, String regexp, String replacement) {
    replacement = replacement.replace("$\\", "$");            // L62: $\\ 转义
    return path.replaceAll(regexp, replacement);              // L67-69
  }

  // ============ 机制 2: StripPrefix + PrefixPath ============
  static String stripPrefix(String path, int parts) {         // L69-75: tokenize 跳前 N 段
    String[] segments = path.split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = parts + 1; i < segments.length; i++) {
      sb.append('/').append(segments[i]);
    }
    return sb.length() == 0 ? "/" : sb.toString();
  }

  static final class PrefixPathFilter {
    boolean alreadyPrefixed;                                  // GATEWAY_ALREADY_PREFIXED_ATTR (L67)

    String apply(String path, String prefix) {                // L65-76
      if (alreadyPrefixed) return path;                       // 防重复 (L68-69)
      alreadyPrefixed = true;
      return prefix + path;
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. RewritePath: 去 /api 前缀 ---
    String r1 = rewritePath("/api/user/123", "^/api(.*)$", "$\\1");   // YAML 转义后 $1
    System.out.println("[RewritePath] /api/user/123 → " + r1);
    if (r1.equals("/user/123")) pass++; else fail++;

    // --- 2. StripPrefix: 去前 1 段 ---
    String s1 = stripPrefix("/api/user/123", 1);
    System.out.println("[StripPrefix] parts=1: /api/user/123 → " + s1);
    if (s1.equals("/user/123")) pass++; else fail++;

    // --- 3. PrefixPath: 加前缀 + 防重复 ---
    PrefixPathFilter pf = new PrefixPathFilter();
    String p1 = pf.apply("/user", "/api");
    String p2 = pf.apply(p1, "/api");                         // 二次应用 (已是 /api/user) → 不叠加 (L68-69)
    System.out.println("[PrefixPath] 首次: " + p1 + " | 重复应用(不叠加): " + p2);
    if (p1.equals("/api/user") && p2.equals("/api/user")) pass++; else fail++;

    // --- 4. 原始链保留 ---
    Exchange ex = new Exchange();
    ex.addOriginalRequestUrl("http://gw/api/user/123");
    ex.addOriginalRequestUrl("http://gw/user/123");           // 变换后
    System.out.println("[保留] 原始链: " + ex.originalUrls + " (LinkedHashSet 追加 L294-296)");
    if (ex.originalUrls.size() == 2) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
