/**
 * MiniGW9 — Spring Cloud Gateway GW-9 跨域与安全域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. CORS 双装配: 全局配置注入映射器 (SimpleUrlHandlerMappingGlobalCorsAutoConfiguration.java:45)
 *  2. 路由级 CORS: 元数据声明 + 路径谓词绑定 + 路由优先合并 (CorsGatewayFilterApplicationListener.java:97-104)
 *  3. 安全头默认值: X-XSS/HSTS/X-Frame (SecureHeadersProperties.java:41-61)
 *
 * 用法: javac MiniGW9.java && java MiniGW9
 */
import java.util.HashMap;
import java.util.Map;

public class MiniGW9 {

  static final class CorsConfig {
    final String allowedOrigins;
    CorsConfig(String o) { allowedOrigins = o; }
    @Override public String toString() { return "allow=" + allowedOrigins; }
  }

  static final class CorsManager {
    final Map<String, CorsConfig> global = new HashMap<>();      // GlobalCorsProperties
    final Map<String, CorsConfig> routeCors = new HashMap<>();   // 路由级 (metadata.cors)
    final Map<String, CorsConfig> effective = new HashMap<>();   // 合并结果

    // 路由级优先 + 全局补缺 (L97-101)
    void rebuild() {
      effective.clear();
      effective.putAll(global);
      routeCors.forEach(effective::put);                          // 路由覆盖全局
    }

    // 路由 CORS 绑定到路径谓词 pattern (L109-124): 前缀模式匹配, 最具体优先
    CorsConfig lookup(String path) {
      CorsConfig global = null;                                   // /** 兜底
      CorsConfig best = null;
      int bestLen = -1;
      for (Map.Entry<String, CorsConfig> e : effective.entrySet()) {
        String pattern = e.getKey();
        if (pattern.equals("/**")) { global = e.getValue(); continue; }
        if (pattern.endsWith("/**")) {
          String base = pattern.substring(0, pattern.length() - 2);
          if (path.startsWith(base) && base.length() > bestLen) { // 最长前缀优先
            bestLen = base.length();
            best = e.getValue();
          }
        } else if (pattern.equals(path)) return e.getValue();
      }
      return best != null ? best : global;                        // 具体优先, /** 兜底
    }
  }

  static final class SecureHeaders {
    static final String X_XSS = "1 ; mode=block";                 // L41
    static final String HSTS = "max-age=631138519";               // L51
    static final String X_FRAME = "DENY";                         // L61

    static Map<String, String> apply(Map<String, String> custom) {
      Map<String, String> headers = new HashMap<>();
      headers.put("X-XSS-Protection", X_XSS);
      headers.put("Strict-Transport-Security", HSTS);
      headers.put("X-Frame-Options", X_FRAME);
      custom.forEach(headers::put);                               // 路由级覆盖 (withDefaults)
      return headers;
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    CorsManager cm = new CorsManager();
    cm.global.put("/**", new CorsConfig("*"));
    cm.routeCors.put("/user/**", new CorsConfig("http://localhost:3000"));
    cm.rebuild();
    CorsConfig userCors = cm.lookup("/user/123");
    CorsConfig otherCors = cm.lookup("/other");
    System.out.println("[CORS] /user/123 → " + userCors + " (路由级优先)");
    System.out.println("[CORS] /other → " + otherCors + " (全局兜底)");
    if (userCors.allowedOrigins.equals("http://localhost:3000")
        && otherCors.allowedOrigins.equals("*")) pass++; else fail++;

    Map<String, String> custom = new HashMap<>();
    custom.put("X-Frame-Options", "SAMEORIGIN");
    Map<String, String> headers = SecureHeaders.apply(custom);
    System.out.println("[安全头] " + headers);
    if (headers.get("X-XSS-Protection").equals("1 ; mode=block")
        && headers.get("X-Frame-Options").equals("SAMEORIGIN")) pass++; else fail++;

    cm.routeCors.put("/admin/**", new CorsConfig("http://admin.internal"));
    cm.rebuild();
    System.out.println("[重建] 新增 /admin/** → " + cm.lookup("/admin/x") + " (RefreshRoutesResultEvent L86-104)");
    if (cm.lookup("/admin/x").allowedOrigins.equals("http://admin.internal")) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
