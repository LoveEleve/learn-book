import java.util.*;

/**
 * MiniAggregations — E-12 Aggregations 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 ES 8.12.2 源码):
 *   A. Aggregator 递归组合: getLeafCollector 模板委托链, 一次遍历喂整棵树
 *      (AggregatorBase.java:219-232)
 *   B. terms 聚合 global ordinals 编号计数: 直接按全局编号建桶
 *      (GlobalOrdinalsStringTermsAggregator.java:127-139)
 *   C. 桶上限 65536: 超限抛 TooManyBucketsException
 *      (MultiBucketConsumerService.java:32-38,55-60)
 *
 * 用内存文档集 + 桶计数模拟 (机制复现非完整库)。
 */
public class MiniAggregations {

    /** 聚合器接口 (Aggregator 简化) */
    interface Aggregator {
        void collect(int doc);
        Map<String, Long> result();
    }

    /** terms 聚合: 按值建桶计数 (global ordinals 简化 = 值直接作键) */
    static class TermsAgg implements Aggregator {
        final String field;
        final int maxBuckets;
        final Map<String, Long> buckets = new HashMap<>();
        boolean tooMany;

        TermsAgg(String field, int maxBuckets) { this.field = field; this.maxBuckets = maxBuckets; }

        public void collect(int doc) {
            if (tooMany) return;
            if (buckets.size() >= maxBuckets) {
                tooMany = true;   // TooManyBucketsException
                return;
            }
            // global ordinals 简化: 值直接作桶键 (真实: 读 ordValue → 编号计数)
            String v = "value" + (doc % 3);
            buckets.merge(v, 1L, Long::sum);
        }

        public Map<String, Long> result() { return buckets; }
    }

    /** 嵌套: avg 子聚合 (聚合器递归组合简化) */
    static class NestedAvg implements Aggregator {
        final TermsAgg parent;
        final Map<String, long[]> sums = new HashMap<>();
        NestedAvg(TermsAgg parent) { this.parent = parent; }

        public void collect(int doc) {
            // 子聚合也收集 (真实: 父聚合包装子 collector)
            String v = "value" + (doc % 3);
            long[] s = sums.computeIfAbsent(v, k -> new long[2]);
            s[0] += doc; s[1]++;
        }

        public Map<String, Long> result() {
            Map<String, Long> r = new HashMap<>();
            sums.forEach((k, s) -> r.put(k, s[0] / s[1]));
            return r;
        }
    }

    /** 组合查询: terms + 嵌套 avg 一次遍历 (getLeafCollector 模板链简化) */
    static Map<String, Object> runCombined(List<Integer> docs, int maxBuckets) {
        TermsAgg terms = new TermsAgg("f", maxBuckets);
        NestedAvg avg = new NestedAvg(terms);
        for (int d : docs) {
            terms.collect(d);   // 父收集
            avg.collect(d);     // 子收集 (同一次遍历)
        }
        Map<String, Object> out = new HashMap<>();
        out.put("terms", terms.result());
        out.put("avg", avg.result());
        return out;
    }
}
