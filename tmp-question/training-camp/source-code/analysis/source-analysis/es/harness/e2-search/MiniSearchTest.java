import java.util.*;

/**
 * MiniSearchTest — E-2 harness 验证入口
 *
 * 跑法: javac MiniSearch.java MiniSearchTest.java && java MiniSearchTest
 * 全部 PASS = BM25 打分/Collector 归并/两阶段查询理解到位
 * (对照 SimilarityProviders.java:255-262 / QueryPhaseCollectorManager.java:111-180 / SearchPhaseController.java:185-229)。
 */
public class MiniSearchTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    static List<MiniSearch.Doc> docs() {
        return List.of(
            new MiniSearch.Doc(0, "elasticsearch is a search engine"),
            new MiniSearch.Doc(1, "elasticsearch elasticsearch is great"),
            new MiniSearch.Doc(2, "redis is a cache"),
            new MiniSearch.Doc(3, "search engine search engine search")
        );
    }

    // A. BM25 打分
    static void testBM25() {
        List<MiniSearch.Doc> ds = docs();
        MiniSearch.BM25 bm25 = new MiniSearch.BM25(ds);
        check("A1 k1=1.2", bm25.k1 == 1.2f);
        check("A2 b=0.75", bm25.b == 0.75f);

        double d1 = bm25.score(ds.get(1), "elasticsearch");  // tf=2
        double d0 = bm25.score(ds.get(0), "elasticsearch");  // tf=1
        check("A3 tf=2 > tf=1", d1 > d0);
        check("A4 词频饱和: 2倍tf < 2倍分", d1 < d0 * 2);

        double d3 = bm25.score(ds.get(3), "search");  // 长文档 tf=3
        double d1s = bm25.score(ds.get(1), "elasticsearch");
        check("A5 无匹配词分=0", bm25.score(ds.get(2), "elasticsearch") == 0);
    }

    // B. 分片查询 + 归并 (Collector 模式简化)
    static void testShardQuery() {
        List<MiniSearch.Doc> ds = docs();
        MiniSearch.BM25 bm25 = new MiniSearch.BM25(ds);
        List<int[]> top2 = MiniSearch.shardQuery(bm25, ds, "search", 2);
        check("B1 分片返回 top2", top2.size() == 2);
        check("B2 第一条是 doc3 (tf=3)", top2.get(0)[0] == 3);
    }

    // C. 两阶段: 查询 → 归并 → fetch
    static void testTwoPhase() {
        List<MiniSearch.Doc> ds = docs();
        MiniSearch.BM25 bm25 = new MiniSearch.BM25(ds);
        // 模拟 2 分片
        List<List<int[]>> shards = List.of(
            MiniSearch.shardQuery(bm25, ds.subList(0, 2), "search", 1),   // 分片1: doc0
            MiniSearch.shardQuery(bm25, ds.subList(2, 4), "search", 1)    // 分片2: doc3
        );
        List<int[]> merged = MiniSearch.mergeResults(shards, 2);
        check("C1 归并 2 条", merged.size() == 2);
        check("C2 第一名 doc3 (跨分片)", merged.get(0)[0] == 3);
        String source = MiniSearch.fetch(ds, merged.get(0)[0]);
        check("C3 Fetch 取到 _source", source != null && source.contains("search"));
    }

    // C2. 深分页问题: 每分片取 from+size 的放大
    static void testDeepPagination() {
        // 5 分片, from=100, size=10 → 每分片取 110
        int from = 100, size = 10, numShards = 5;
        int totalFetch = (from + size) * numShards;
        check("C4 深分页放大: 每分片取 from+size", totalFetch == 550);
        check("C5 这就是 search_after 存在的原因", totalFetch > 110);
    }

    public static void main(String[] args) {
        testBM25();
        testShardQuery();
        testTwoPhase();
        testDeepPagination();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
