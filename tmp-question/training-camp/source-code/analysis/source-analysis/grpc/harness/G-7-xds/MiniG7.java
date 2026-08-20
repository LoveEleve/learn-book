/**
 * MiniG7 — gRPC-Java G-7 xDS 域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 订阅模型: watch 资源 → 更新回调 (XdsClientImpl.java:251-280)
 *  2. RingHash Ketama: xxHash 简化 → 环 + 虚拟节点 + 顺时针最近 (RingHashLoadBalancer.java:60-64,323-350)
 *  3. 加权随机: 累积权重扫描 (WeightedRandomPicker.java:113-140)
 *
 * 用法: javac MiniG7.java && java MiniG7
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MiniG7 {

  // ============ 机制 1: 订阅模型 (简化) ============
  static final class ResourceSubscriber {
    final String type, name;
    final List<Runnable> watchers = new ArrayList<>();
    ResourceSubscriber(String type, String name) { this.type = type; this.name = name; }
    void addWatcher(Runnable w) { watchers.add(w); }
    void update() { for (Runnable w : watchers) w.run(); }
  }

  // XdsClientImpl 简化: resourceSubscribers (类型→名→订阅者) (L251-280)
  static final class XdsClient {
    final Map<String, Map<String, ResourceSubscriber>> subscribers = new HashMap<>();

    void watchXdsResource(String type, String name, Runnable watcher) {
      subscribers.computeIfAbsent(type, k -> new HashMap<>())
          .computeIfAbsent(name, k -> new ResourceSubscriber(type, name))
          .addWatcher(watcher);
      System.out.println("  [xds] 订阅 " + type + "/" + name + " (watchXdsResource L251)");
    }

    void pushUpdate(String type, String name, String content) {
      ResourceSubscriber s = subscribers.getOrDefault(type, Map.of()).get(name);
      if (s != null) { System.out.println("  [xds] 推送 " + type + "/" + name + "=" + content + " (ACK)"); s.update(); }
    }
  }

  // ============ 机制 2: RingHash Ketama (简化哈希) ============
  static long hash(String s) {            // xxHash64 模拟: 确定性伪随机 (均匀分散, 真实 Ketama 特性)
    return new Random(s.hashCode() * 31L + 7).nextLong();
  }

  static final class RingEntry implements Comparable<RingEntry> {
    final long hash; final String endpoint;
    RingEntry(long h, String e) { hash = h; endpoint = e; }
    public int compareTo(RingEntry o) { return Long.compare(hash, o.hash); }
  }

  // buildRing 简化: 虚拟节点数 ∝ 权重 (RingHashLoadBalancer.java:339-349)
  static List<RingEntry> buildRing(Map<String, Integer> weights, int scale) {
    List<RingEntry> ring = new ArrayList<>();
    Random r = new Random(42);                       // 单一序列: 均匀覆盖环 (xxHash 特性)
    for (Map.Entry<String, Integer> e : weights.entrySet()) {
      for (long i = 0; i < scale * e.getValue(); i++) {
        ring.add(new RingEntry(r.nextLong(), e.getKey()));
      }
    }
    Collections.sort(ring);
    return ring;
  }

  // pickSubchannel 简化: 顺时针最近 (L440)
  static String pick(List<RingEntry> ring, long requestHash) {
    int idx = Collections.binarySearch(ring, new RingEntry(requestHash, ""));
    if (idx < 0) idx = -idx - 1;
    return ring.get(idx % ring.size()).endpoint;
  }

  // ============ 机制 3: 加权随机 ============
  static final class WeightedPicker {
    final List<String> endpoints; final List<Integer> weights; final Random random = new Random(7);
    WeightedPicker(List<String> e, List<Integer> w) { endpoints = e; weights = w; }

    String pickSubchannel() {                                    // WeightedRandomPicker.java:113-140
      long total = 0;
      for (int w : weights) total += w;                          // L98-105
      long rand = random.nextLong(total);                        // L121
      long acc = 0;
      for (int i = 0; i < endpoints.size(); i++) {               // 累积权重扫描 L122-130
        acc += weights.get(i);
        if (rand < acc) return endpoints.get(i);
      }
      return endpoints.get(endpoints.size() - 1);
    }
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 订阅模型 ---
    XdsClient client = new XdsClient();
    final int[] updates = {0};
    client.watchXdsResource("EDS", "cluster-a", () -> updates[0]++);
    client.pushUpdate("EDS", "cluster-a", "endpoint1,endpoint2");
    System.out.println("[订阅] 推送后 watcher 回调次数: " + updates[0] + " (期望 1)");
    if (updates[0] == 1) pass++; else fail++;

    // --- 2. RingHash: 一致性哈希 1/N 性质 ---
    Map<String, Integer> weights = new HashMap<>();
    weights.put("A", 1); weights.put("B", 1); weights.put("C", 1);
    List<RingEntry> ring3 = buildRing(weights, 50);              // 每节点 50 虚拟点
    System.out.println("[RingHash] 3 节点环大小: " + ring3.size() + " (虚拟节点 50×权重)");

    // 请求哈希须均匀分布 (真实 xxHash64 特性) — 用确定性随机模拟
    long[] reqHashes = new long[1000];
    for (int i = 0; i < 1000; i++) reqHashes[i] = new Random(i).nextLong();
    // 移除节点 B, 重新记录
    Map<String, Integer> w2 = new HashMap<>();
    w2.put("A", 1); w2.put("C", 1);
    List<RingEntry> ring2 = buildRing(w2, 50);
    int affected = 0;
    int bPicks = 0;
    for (int i = 0; i < 1000; i++) {
      String oldE = pick(ring3, reqHashes[i]);
      String newE = pick(ring2, reqHashes[i]);
      if ("B".equals(oldE)) bPicks++;
      if (!oldE.equals(newE)) affected++;
    }
    System.out.println("  [debug] 移除前选 B 的次数: " + bPicks + "/1000");
    // 环分布诊断: 相邻点最大间隙 (最大间隙 = 最少被选区域)
    long maxGap = 0; int maxGapIdx = 0;
    for (int i = 0; i < ring3.size(); i++) {
      long gap = (i == 0) ? (Long.MIN_VALUE - ring3.get(ring3.size()-1).hash + Long.MAX_VALUE - ring3.get(0).hash + 1)
                          : (ring3.get(i).hash - ring3.get(i-1).hash);
      if (gap > maxGap) { maxGap = gap; maxGapIdx = i; }
    }
    System.out.println("  [debug] 环最大间隙 " + maxGap + " at idx " + maxGapIdx + " (" + ring3.get(maxGapIdx).endpoint + ")");
    System.out.println("[RingHash] 移除 B 后受影响请求: " + affected + "/1000 (期望 ≈1/3, Ketama 1/N 性质 L64)");
    if (affected > 200 && affected < 500) pass++; else fail++;

    // --- 3. 加权随机: 权重 9:1 ---
    WeightedPicker wp = new WeightedPicker(List.of("X", "Y"), List.of(9, 1));
    int x = 0;
    for (int i = 0; i < 1000; i++) if ("X".equals(wp.pickSubchannel())) x++;
    System.out.println("[加权随机] 权重 9:1 → X 被选 " + x + "/1000 (期望 ≈900)");
    if (x > 800 && x < 980) pass++; else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
