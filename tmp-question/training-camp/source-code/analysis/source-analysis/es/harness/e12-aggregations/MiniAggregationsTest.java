import java.util.*;

/**
 * MiniAggregationsTest — E-12 harness 验证入口
 *
 * 跑法: javac MiniAggregations.java MiniAggregationsTest.java && java MiniAggregationsTest
 * 全部 PASS = 聚合递归组合/terms 桶计数/桶上限理解到位
 * (对照 AggregatorBase.java:219-232 / GlobalOrdinalsStringTermsAggregator.java:127-139 / MultiBucketConsumerService.java:32-60)。
 */
public class MiniAggregationsTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 递归组合: 一次遍历喂整棵树
    static void testRecursiveComposition() {
        List<Integer> docs = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
        Map<String, Object> r = MiniAggregations.runCombined(docs, 100);
        @SuppressWarnings("unchecked")
        Map<String, Long> terms = (Map<String, Long>) r.get("terms");
        @SuppressWarnings("unchecked")
        Map<String, Long> avg = (Map<String, Long>) r.get("avg");

        check("A1 terms 3 个桶 (doc%3)", terms.size() == 3);
        check("A2 value0 桶计数=3", terms.get("value0") == 3);
        check("A3 嵌套 avg 同遍历算完", avg.get("value0") == 3);  // (0+3+6)/3
        check("A4 avg value1=4", avg.get("value1") == 4);          // (1+4+7)/3
    }

    // B. terms 桶计数 (global ordinals 简化)
    static void testTermsBuckets() {
        List<Integer> docs = List.of(1, 1, 1, 2, 2, 3);
        MiniAggregations.TermsAgg t = new MiniAggregations.TermsAgg("f", 100);
        for (int d : docs) t.collect(d);
        check("B1 value1 计数=3", t.buckets.get("value1") == 3);
        check("B2 value2 计数=2", t.buckets.get("value2") == 2);
        check("B3 无超限", !t.tooMany);
    }

    // C. 桶上限 65536 保护
    static void testMaxBuckets() {
        MiniAggregations.TermsAgg t = new MiniAggregations.TermsAgg("f", 3);
        for (int d = 0; d < 100; d++) t.collect(d);
        check("C1 超限标记", t.tooMany);
        check("C2 桶被限制", t.buckets.size() <= 3);
        // 默认 65536 (MultiBucketConsumerService.java:32)
        MiniAggregations.TermsAgg t2 = new MiniAggregations.TermsAgg("f", 65536);
        for (int d = 0; d < 10000; d++) t2.collect(d);
        check("C3 默认 65536 不超 (10000 值)", !t2.tooMany);
        check("C4 默认 65536 桶数正确", t2.buckets.size() == 3);  // doc%3 只有 3 个唯一
    }

    // D. 桶上限拒绝语义 (TooManyBucketsException 简化)
    static void testTooManyException() {
        MiniAggregations.TermsAgg t = new MiniAggregations.TermsAgg("f", 2);
        for (int d = 0; d < 10; d++) t.collect(d);
        check("D1 超限后停止收集", t.tooMany);
        check("D2 桶数=上限", t.buckets.size() == 2);
    }

    public static void main(String[] args) {
        testRecursiveComposition();
        testTermsBuckets();
        testMaxBuckets();
        testTooManyException();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
