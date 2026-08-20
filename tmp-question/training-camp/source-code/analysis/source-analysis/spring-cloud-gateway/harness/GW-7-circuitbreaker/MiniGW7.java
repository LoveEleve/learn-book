/**
 * MiniGW7 — Spring Cloud Gateway GW-7 熔断与重试域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 状态码熔断: 链成功但响应码在配置集 → 视为失败 (SpringCloudCircuitBreakerFilterFactory.java:100-109)
 *  2. 熔断+重试组合: 链顺序即语义, CircuitBreaker 外层兜底含重试调用 (pass2-q4)
 *  3. Retry backoff: Reactor Backoff.exponential (RetryGatewayFilterFactory.java:221-222)
 *
 * 用法: javac MiniGW7.java && java MiniGW7
 */
public class MiniGW7 {

  // ============ 机制 1: 状态码熔断 (简化) ============
  static final class CircuitBreaker {
    int failureCount;
    boolean open;
    static final int THRESHOLD = 3;

    // cb.run(chain + 状态码熔断) (L100-109): 返回是否成功
    String run(String result, String statusCode, boolean statusInSet) {
      if (statusInSet) {                            // 成功但状态码在白名单 → 视为失败 (L104-109)
        failureCount++;
        if (failureCount >= THRESHOLD) open = true;  // 熔断打开
        return "状态码熔断 (" + statusCode + " → CircuitBreakerStatusCodeException)";
      }
      if (open) return "熔断打开 → fallback (不调下游)";
      failureCount = 0;                              // 成功复位
      return "正常: " + result;
    }
  }

  // ============ 机制 2: 熔断+重试组合 ============
  // 组合语义: CircuitBreaker 外层 → 熔断判定基于"含重试的调用" (pass2-q4)
  static String combinedFilter(int failuresBeforeSuccess) {
    int attempts = 0;
    while (attempts <= 3) {                          // Retry retries=3
      attempts++;
      if (attempts > failuresBeforeSuccess) {
        return "第 " + attempts + " 次尝试成功 (Retry 内层) → 熔断不触发";
      }
    }
    return "重试耗尽仍失败 → 熔断触发 → fallback (CircuitBreaker 外层)";
  }

  // ============ 机制 3: Backoff 简化 ============
  static long exponentialBackoff(long first, long max, double factor) {  // L221-222
    return Math.min(first * (long) factor, max);
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 状态码熔断 ---
    CircuitBreaker cb = new CircuitBreaker();
    System.out.println("[熔断] " + cb.run("200 OK", "200", false));
    System.out.println("[熔断] " + cb.run("500", "500", true) + " (白名单 500)");
    System.out.println("[熔断] " + cb.run("502", "502", true));
    System.out.println("[熔断] " + cb.run("503", "503", true) + " (3 次 → 打开)");
    System.out.println("[熔断] " + cb.run("任何", null, false) + " (打开后直接 fallback)");
    if (cb.open && cb.failureCount >= 3) pass++; else fail++;

    // --- 2. 熔断+重试组合 ---
    String ok = combinedFilter(2);                   // 前 2 次失败, 第 3 次成功
    String failAll = combinedFilter(99);             // 全失败
    System.out.println("[组合] 重试后成功: " + ok);
    System.out.println("[组合] 重试耗尽: " + failAll);
    if (ok.contains("成功") && failAll.contains("熔断触发")) pass++; else fail++;

    // --- 3. Backoff ---
    long b1 = exponentialBackoff(100, 500, 2);
    long b2 = exponentialBackoff(b1, 500, 2);
    System.out.println("[退避] 100 → " + b1 + " → " + b2 + " (exponential, 对照 gRPC G-6 1.6x)");
    if (b1 == 200 && b2 == 400) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
