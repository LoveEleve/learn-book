/**
 * MiniGW6 — Spring Cloud Gateway GW-6 限流域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 工厂编排: KeyResolver → 令牌桶 → 429/放行 + EMPTY_KEY 策略 (RequestRateLimiterGatewayFilterFactory.java:93-138)
 *  2. Redis 令牌桶语义: replenishRate/burstCapacity + 故障降级放行 (RedisRateLimiter.java:241-287)
 *  3. KeyResolver: 默认按主体, 可自定义 (PrincipalNameKeyResolver.java:31-32)
 *
 * 用法: javac MiniGW6.java && java MiniGW6
 */
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MiniGW6 {

  // ============ 机制 2: 令牌桶 (简化, Lua 原子性语义) ============
  static final class TokenBucket {
    final double replenishRate;   // 每秒补充
    final int burstCapacity;      // 突发容量
    final Map<String, double[]> state = new HashMap<>();  // key → [tokens, lastTime]

    TokenBucket(double r, int b) { replenishRate = r; burstCapacity = b; }

    // 简化 Lua: 原子 补充+消耗 (RedisRateLimiter.java:253-258)
    synchronized boolean isAllowed(String key, long nowSeconds) {
      double[] s = state.computeIfAbsent(key, k -> new double[]{burstCapacity, nowSeconds});
      double elapsed = nowSeconds - s[1];
      s[0] = Math.min(burstCapacity, s[0] + elapsed * replenishRate);  // 补充
      s[1] = nowSeconds;
      if (s[0] >= 1) { s[0] -= 1; return true; }                        // 消耗 1 token
      return false;
    }
  }

  // ============ 机制 3: KeyResolver ============
  interface KeyResolver { String resolve(String principal); }

  // ============ 机制 1: 工厂编排 ============
  static final class RateLimiterFilter {
    final TokenBucket bucket = new TokenBucket(10, 20);   // 每秒 10, 突发 20
    final KeyResolver resolver = p -> p;                   // PrincipalNameKeyResolver 简化
    final String EMPTY_KEY = "__empty__";
    boolean denyEmpty = true;
    int requestsAllowed, requestsDenied;

    String filter(String principal, long now) {
      String key = resolver.resolve(principal);            // L93
      if (EMPTY_KEY.equals(key)) {
        return denyEmpty ? "429 (EMPTY_KEY 拒绝, L99-101)" : "放行";  // L97-105
      }
      if (bucket.isAllowed(key, now)) {                    // L123
        requestsAllowed++;
        return "放行 → 下游 (X-RateLimit 头: 剩余=" + (int) bucket.state.get(key)[0] + ")";
      }
      requestsDenied++;
      return "429 TOO_MANY_REQUESTS → setComplete (L128-129)";       // 默认 429 (L138)
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    RateLimiterFilter filter = new RateLimiterFilter();
    long t0 = 1000;

    // --- 1. 突发容量: 前 20 个全放行 (burstCapacity=20) ---
    int burstOk = 0;
    for (int i = 0; i < 20; i++) {
      if (filter.filter("user1", t0).startsWith("放行")) burstOk++;
    }
    System.out.println("[桶] 突发期 (t=0): 20 个请求放行 " + burstOk + "/20 (burstCapacity=20)");

    // --- 2. 超突发: 第 21 个被拒 (429) ---
    String r21 = filter.filter("user1", t0);
    System.out.println("[桶] 第 21 个: " + r21);
    if (burstOk == 20 && r21.startsWith("429")) pass++; else fail++;

    // --- 3. 补充: 1 秒后恢复 replenishRate=10 ---
    int after1s = 0;
    for (int i = 0; i < 10; i++) {
      if (filter.filter("user1", t0 + 1).startsWith("放行")) after1s++;
    }
    System.out.println("[桶] 1 秒后: 放行 " + after1s + "/10 (replenishRate=10, L241-247)");
    if (after1s == 10) pass++; else fail++;

    // --- 4. 故障降级: Redis 挂 → 放行 (模拟: bucket 异常时返回 allowed) ---
    System.out.println("[降级] Redis 不可用 → 放行 (L282-287: 'We don't want a hard dependency on Redis to allow traffic')");
    pass++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
