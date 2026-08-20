/**
 * MiniG8 — gRPC-Java G-8 RLS 域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 路由模型: 每请求查缓存 → miss 调 RLS 服务器 → 目标集群 (CachingRlsLbClient.java:84-85)
 *  2. 缓存三层: Data (成功, 带过期) / Backoff (失败冷却, G-6 退避) / Pending (在途去重) (CachingRlsLbClient.java:108,675-837)
 *  3. 自适应节流: 滑动窗口比例控制, 发送率 ≈ 2x 接受率 (AdaptiveThrottler.java:45-47,86)
 *
 * 用法: javac MiniG8.java && java MiniG8
 */
import java.util.HashMap;
import java.util.Map;

public class MiniG8 {

  // ============ 机制 1+2: RLS 客户端 (简化) ============
  static final class RlsClient {
    final Map<String, Object> cache = new HashMap<>();       // key → DataEntry/BackoffEntry
    final Map<String, Object> pending = new HashMap<>();     // PendingCacheEntry (L116)
    int serverCalls;                                         // 打到 RLS 服务器的次数

    static final class DataEntry {
      String target; long expireAt;
      DataEntry(String t, long e) { target = t; expireAt = e; }
    }
    static final class BackoffEntry {
      long until;
      BackoffEntry(long u) { until = u; }
    }

    // 路由: 缓存命中 → 直接; miss → 查服务器 (L84-85 核心语义)
    String route(String key, long now) {
      Object e = cache.get(key);
      if (e instanceof DataEntry && ((DataEntry) e).expireAt > now) {
        return "缓存 → " + ((DataEntry) e).target;
      }
      if (e instanceof BackoffEntry && ((BackoffEntry) e).until > now) {
        return "退避冷却中 → 默认目标 (fallback, L136)";
      }
      if (pending.containsKey(key)) {
        return "在途查询去重 → 等待 (L705-708)";
      }
      // miss → 查 RLS 服务器
      pending.put(key, true);
      serverCalls++;
      String target = serverQuery(key);                      // 模拟服务器返回
      pending.remove(key);
      if (target != null) {
        cache.put(key, new DataEntry(target, now + 180_000)); // 缓存 180s (maxAge)
        return "RLS 服务器 → " + target + " (已缓存)";
      } else {
        cache.put(key, new BackoffEntry(now + 30_000));       // 失败 → 退避条目
        return "查询失败 → 退避 30s (L803-837)";
      }
    }

    String serverQuery(String key) {
      if (serverCalls == 1 && key.equals("K1")) return null;   // 第一次失败
      return "cluster-" + key;
    }
  }

  // ============ 机制 3: 自适应节流 (简化) ============
  static final class Throttler {
    int requests, throttled;
    static final float RATIO = 2.0f;                          // AdaptiveThrottler.java:47

    boolean shouldThrottle() {                                // L86: 比例判定 (简化无随机)
      // 若被节流率 > 接受率的倒数阈值 → 节流 (模拟: 被节流占比 > 0.5 时开始节流)
      return requests > 10 && throttled * 1.0 / requests > 0.5;
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;
    long now = System.currentTimeMillis();

    // --- 1. 路由: miss→查询→缓存; 二次命中 ---
    RlsClient client = new RlsClient();
    System.out.println("[路由] 第一次 K1 (服务器失败): " + client.route("K1", now));
    System.out.println("[路由] 第二次 K1 (退避冷却): " + client.route("K1", now));
    System.out.println("[路由] 第一次 K2: " + client.route("K2", now));
    System.out.println("[路由] 第二次 K2 (缓存命中): " + client.route("K2", now));
    System.out.println("[路由] 服务器调用次数: " + client.serverCalls + " (K1 失败 1 次 + K2 成功 1 次 = 2)");
    if (client.serverCalls == 2 && client.route("K2", now).startsWith("缓存")) pass++; else fail++;

    // --- 2. 在途去重 ---
    RlsClient client2 = new RlsClient();
    client2.pending.put("K3", true);                          // 模拟在途
    System.out.println("[去重] 在途 K3 再请求: " + client2.route("K3", now));
    if (client2.route("K3", now).contains("去重")) pass++; else fail++;

    // --- 3. 节流比例 ---
    Throttler t = new Throttler();
    for (int i = 0; i < 13; i++) { t.requests++; if (i % 2 == 0) t.throttled++; }  // 7/13 = 53.8%
    System.out.println("[节流] 被节流率 " + t.throttled + "/" + t.requests + " = "
        + (t.throttled * 100.0 / t.requests) + "% → shouldThrottle=" + t.shouldThrottle()
        + " (>50% 开始节流, 比例式 L86)");
    if (t.shouldThrottle()) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
