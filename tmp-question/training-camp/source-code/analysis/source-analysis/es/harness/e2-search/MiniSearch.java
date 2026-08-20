import java.util.*;

/**
 * MiniSearch — E-2 Search 查询路径极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 ES 8.12.2 源码):
 *   A. BM25 打分: 词频饱和 (k1) + 长度归一化 (b)
 *      (SimilarityProviders.java:255-262; LegacyBM25Similarity.java:35-69)
 *   B. CollectorManager 模式: 每段 newCollector + reduce 跨段合并
 *      (QueryPhaseCollectorManager.java:111-146 newCollector; 147-180 reduce)
 *   C. 两阶段查询: QueryPhase (doc+score) → 协调归并 → FetchPhase (_source)
 *      (SearchPhaseController.java:185-229 sortDocs)
 *
 * 用内存文档集 + BM25 公式模拟 (机制复现非完整库)。
 */
public class MiniSearch {

    /** 文档: id + 字段文本 */
    static class Doc {
        final int id;
        final String text;
        Doc(int id, String text) { this.id = id; this.text = text; }
    }

    /** BM25 打分器 (SimilarityProviders.java:255-262 参数) */
    static class BM25 {
        final float k1 = 1.2f;   // 词频饱和
        final float b = 0.75f;   // 长度归一化
        final List<Doc> docs;
        final int totalDocs;
        final double avgLen;
        final Map<String, Integer> docFreq = new HashMap<>();

        BM25(List<Doc> docs) {
            this.docs = docs;
            this.totalDocs = docs.size();
            long totalLen = 0;
            for (Doc d : docs) {
                totalLen += d.text.split(" ").length;
                Set<String> seen = new HashSet<>(Arrays.asList(d.text.split(" ")));
                for (String w : seen) docFreq.merge(w, 1, Integer::sum);
            }
            this.avgLen = (double) totalLen / totalDocs;
        }

        /** BM25 核心公式: IDF * tf_saturated * length_norm */
        double score(Doc doc, String term) {
            String[] words = doc.text.split(" ");
            int tf = 0;
            for (String w : words) if (w.equals(term)) tf++;
            if (tf == 0) return 0;
            int df = docFreq.getOrDefault(term, 0);
            // IDF (LegacyBM25Similarity 简化: 不带 boost)
            double idf = Math.log(1 + (totalDocs - df + 0.5) / (df + 0.5));
            // 词频饱和 + 长度归一化 (k1=1.2, b=0.75)
            double tfSaturated = tf * (k1 + 1) / (tf + k1 * (1 - b + b * words.length / avgLen));
            return idf * tfSaturated;
        }
    }

    /** 分片查询: 返回 topN (doc_id + score) — 对照 QueryPhase + TopDocsCollector */
    static List<int[]> shardQuery(BM25 bm25, List<Doc> docs, String term, int size) {
        List<int[]> scored = new ArrayList<>();  // {docId, score*1000 整数化}
        for (Doc d : docs) {
            double s = bm25.score(d, term);
            if (s > 0) scored.add(new int[] { d.id, (int) (s * 1000) });
        }
        scored.sort((a, b2) -> b2[1] - a[1]);  // score 降序
        return scored.subList(0, Math.min(size, scored.size()));
    }

    /** 协调节点归并 (SearchPhaseController.sortDocs L185-229 简化) */
    static List<int[]> mergeResults(List<List<int[]>> shardResults, int size) {
        List<int[]> all = new ArrayList<>();
        for (List<int[]> r : shardResults) all.addAll(r);
        all.sort((a, b) -> b[1] - a[1]);
        return all.subList(0, Math.min(size, all.size()));
    }

    /** Fetch 阶段: 按 docId 取 _source (FetchPhase 简化) */
    static String fetch(List<Doc> docs, int docId) {
        for (Doc d : docs) if (d.id == docId) return d.text;
        return null;
    }
}
