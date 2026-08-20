import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MiniLoadBalance {

    static class Invoker {
        final String addr; final int weight; final long startTs;
        int active = 0; long avgResponse = 100;
        Invoker(String addr, int weight, long startTs) { this.addr = addr; this.weight = weight; this.startTs = startTs; }
        public String toString() { return addr; }
    }

    // ---- A: 抽象面 — 权重 + 预热爬坡 (calculateWarmupWeight) ----
    static int calculateWarmupWeight(long uptime, long warmup, int weight) {
        long ww = (long) (uptime / ((double) warmup / weight));
        return (int) (ww < 1 ? 1 : Math.min(ww, weight));
    }

    static int getWeight(Invoker inv, long now) {
        long uptime = now - inv.startTs;
        int warmup = 600_000; // DEFAULT_WARMUP 10min
        if (uptime > 0 && uptime < warmup) {
            return calculateWarmupWeight(uptime, warmup, inv.weight);
        }
        return inv.weight;
    }

    // ---- B: 随机族 — Random 前缀和 + RoundRobin 平滑 WRR ----
    static Invoker randomSelect(List<Invoker> invokers, long now) {
        int length = invokers.size();
        if (length == 0) return null;
        int total = 0;
        for (Invoker i : invokers) total += getWeight(i, now);
        if (total <= 0) return invokers.get(ThreadLocalRandom.current().nextInt(length));
        int[] prefix = new int[length]; // 前缀和区间
        int acc = 0;
        for (int i = 0; i < length; i++) { acc += getWeight(invokers.get(i), now); prefix[i] = acc; }
        int r = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < length; i++) if (r < prefix[i]) return invokers.get(i);
        return invokers.get(length - 1);
    }

    static class WRR { int current = 0; final int weight; WRR(int w) { weight = w; } }

    static Invoker roundRobinSelect(List<Invoker> invokers, Map<String, WRR> map, long now) {
        int total = 0; long max = Long.MIN_VALUE; Invoker selected = null; WRR selWrr = null;
        for (Invoker i : invokers) {
            int w = getWeight(i, now);
            WRR wrr = map.computeIfAbsent(i.addr, k -> new WRR(w));
            wrr.current += w;         // increaseCurrent
            total += w;
            if (wrr.current > max) { max = wrr.current; selected = i; selWrr = wrr; }
        }
        if (selected != null) selWrr.current -= total; // sel(totalWeight)
        return selected;
    }

    // ---- C: 状态族 — LeastActive + ConsistentHash ----
    static Invoker leastActiveSelect(List<Invoker> invokers, long now) {
        int least = Integer.MAX_VALUE; List<Invoker> candidates = new ArrayList<>();
        for (Invoker i : invokers) {
            if (i.active < least) { least = i.active; candidates.clear(); candidates.add(i); }
            else if (i.active == least) candidates.add(i);
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    static class ConsistentHashSelector {
        final TreeMap<Long, Invoker> ring = new TreeMap<>();
        ConsistentHashSelector(List<Invoker> invokers) {
            for (Invoker i : invokers) {
                for (int n = 0; n < 160 / 4; n++) { // 160 虚拟节点
                    long hash = (long) (i.addr + n).hashCode() * 0x5DEECE66DL + n; // 简化哈希
                    ring.put(hash, i);
                }
            }
        }
        Invoker select(String key) {
            long h = key.hashCode();
            Map.Entry<Long, Invoker> e = ring.ceilingEntry(h);
            if (e == null) e = ring.firstEntry();
            return e.getValue();
        }
    }

    // ---- D: Adaptive P2C — 随机两样本取低负载 ----
    static Invoker adaptiveSelect(List<Invoker> invokers) {
        int length = invokers.size();
        if (length == 1) return invokers.get(0);
        if (length == 2) return invokers.get(0).avgResponse <= invokers.get(1).avgResponse ? invokers.get(0) : invokers.get(1);
        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length - 1);
        if (pos2 >= pos1) pos2++;
        return invokers.get(pos1).avgResponse <= invokers.get(pos2).avgResponse ? invokers.get(pos1) : invokers.get(pos2);
    }

    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;
        long now = System.currentTimeMillis();
        // ============ A: 预热爬坡 ============
        System.out.println("A1 warmup : 0s→" + calculateWarmupWeight(0, 600_000, 100) + ", 5min→" + calculateWarmupWeight(300_000, 600_000, 100) + ", 10min→" + calculateWarmupWeight(600_000, 600_000, 100));

        // ============ B: 随机族 — 平滑 WRR (权重 5:1:1, 7 轮分布) ============
        List<Invoker> invokers = Arrays.asList(
                new Invoker("a:20880", 5, now - 10L * 60_000),
                new Invoker("b:20881", 1, now - 10L * 60_000),
                new Invoker("c:20882", 1, now - 10L * 60_000));
        Map<String, WRR> map = new HashMap<>();
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < 7; i++) seq.append(roundRobinSelect(invokers, map, now).addr.charAt(0));
        System.out.println("B1 wrr    : " + seq);
        System.out.println("B2 random : " + randomSelect(invokers, now));

        // ============ C: 状态族 — LeastActive + ConsistentHash ============
        invokers.get(1).active = 5; // b 活跃高
        System.out.println("C1 least  : " + leastActiveSelect(invokers, now)); // 应避开 b
        ConsistentHashSelector chs = new ConsistentHashSelector(invokers);
        System.out.println("C2 hash   : key1→" + chs.select("user-1") + ", key1→" + chs.select("user-1") + ", key2→" + chs.select("user-2"));

        // ============ D: Adaptive P2C ============
        invokers.get(2).avgResponse = 500; // c 慢
        Invoker d = adaptiveSelect(invokers);
        System.out.println("D1 p2c    : " + d + " (应偏向 a/b)");

        // 断言
        pass += calculateWarmupWeight(300_000, 600_000, 100) == 50 ? 1 : 0;
        pass += seq.indexOf("a") >= 0 && seq.indexOf("b") >= 0 && seq.indexOf("c") >= 0 ? 1 : 0;
        pass += leastActiveSelect(invokers, now).addr.charAt(0) != 'b' ? 1 : 0;
        pass += chs.select("user-1").equals(chs.select("user-1")) ? 1 : 0;
        pass += !adaptiveSelect(invokers).addr.startsWith("c") ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
