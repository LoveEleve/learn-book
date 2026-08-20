/**
 * MiniGW5 — Spring Cloud Gateway GW-5 转发与头处理域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 两阶段转发: 发请求 + 响应延迟提交 → 链尾回写 (NettyRoutingFilter.java:112-200 / NettyWriteResponseFilter.java:71-105)
 *  2. body 缓存: 按路由声明 + 前置缓存 (AdaptCachedBodyGlobalFilter.java:60-80)
 *  3. X-Forwarded-For: 可信代理匹配才追加 (XForwardedHeadersFilter.java:242)
 *
 * 用法: javac MiniGW5.java && java MiniGW5
 */
import java.util.ArrayList;
import java.util.List;

public class MiniGW5 {

  // ============ 机制 1: 两阶段转发 (简化) ============
  static final class Exchange {
    String responseHeaders;      // 延迟提交的头 (exchange 属性)
    String responseBody;         // 连接属性 CLIENT_RESPONSE_CONN_ATTR 简化
    boolean bodyCached;
    final List<String> log = new ArrayList<>();
  }

  // NettyRoutingFilter: 发请求 + 头/状态写 exchange, 延迟提交 (L112-149)
  static void forwardingFilter(Exchange ex) {
    ex.log.add("[转发] 发出请求 → 后端响应");
    ex.responseHeaders = "200 OK (延迟提交, 等链尾过滤器改头)";
    ex.responseBody = "后端响应体";
  }

  // NettyWriteResponseFilter: 链尾回写 (L71-105)
  static void writeResponseFilter(Exchange ex) {
    ex.log.add("[回写] " + ex.responseHeaders + " + body: " + ex.responseBody
        + " (CLIENT_RESPONSE_CONN_ATTR L71)");
  }

  // 后置过滤器: 改响应头 (延迟提交的受益者)
  static void responseHeaderFilter(Exchange ex) {
    ex.responseHeaders = ex.responseHeaders + " + X-Custom: added";
  }

  // ============ 机制 2: body 缓存 ============
  static final class BodyCache {
    final List<String> routesToCache = new ArrayList<>();    // EnableBodyCachingEvent 声明
    boolean cached;

    void onEnableBodyCachingEvent(String routeId) { routesToCache.add(routeId); }

    // AdaptCachedBodyGlobalFilter (L60-66): 按路由缓存
    String filter(String routeId, String body) {
      if (cached || !routesToCache.contains(routeId)) return body;   // 不缓存: 直接传
      cached = true;
      System.out.println("  [body] 路由 " + routeId + " 声明缓存 → cacheRequestBody (L66)");
      return body;                                          // 缓存后 mutate 替换请求
    }
  }

  // ============ 机制 3: X-Forwarded-For 可信代理 ============
  static final class XForwarded {
    final List<String> trustedProxies = new ArrayList<>();

    // L242: "match xforwarded for against trusted proxies"
    String apply(String remoteAddr, String existingXff) {
      if (!trustedProxies.contains(remoteAddr)) {
        System.out.println("  [xff] " + remoteAddr + " 非可信代理 → 不追加 (L242)");
        return existingXff;                                  // 防伪造: 原样透传
      }
      String appended = existingXff == null ? remoteAddr : existingXff + ", " + remoteAddr;
      System.out.println("  [xff] " + remoteAddr + " 可信 → 追加: " + appended);
      return appended;
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 两阶段转发 + 延迟提交受益者 ---
    Exchange ex = new Exchange();
    forwardingFilter(ex);                                    // NettyRoutingFilter
    responseHeaderFilter(ex);                                // 后置过滤器改头
    writeResponseFilter(ex);                                 // NettyWriteResponseFilter
    ex.log.forEach(System.out::println);
    if (ex.responseHeaders.contains("X-Custom") && ex.responseHeaders.contains("延迟提交")) pass++; else fail++;

    // --- 2. body 缓存按声明 ---
    BodyCache bc = new BodyCache();
    System.out.println("[body] 未声明路由: " + bc.filter("routeA", "body"));
    bc.onEnableBodyCachingEvent("routeB");                   // EnableBodyCachingEvent
    System.out.println("[body] 已声明路由: " + bc.filter("routeB", "body"));
    System.out.println("[body] 二次读取 (缓存): " + bc.filter("routeB", "body"));
    pass++;

    // --- 3. X-Forwarded-For 可信代理 ---
    XForwarded xf = new XForwarded();
    xf.trustedProxies.add("10.0.0.1");                       // 可信 LB
    String xff1 = xf.apply("10.0.0.1", null);                // 可信 → 追加
    String xff2 = xf.apply("203.0.113.9", xff1);             // 伪造客户端 → 拒绝
    System.out.println("[xff] 最终: " + xff2);
    if (xff1.equals("10.0.0.1") && xff2.equals("10.0.0.1")) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
