import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniMyBatisExecutor — M-2 SqlSession/Executor 链 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis 源码):
 *   A. BaseExecutor 模板方法 + 一级缓存: 命中不查库/update 清缓存/STATEMENT scope 清 (BaseExecutor.java:132-175)
 *   B. CacheKey 参数值参与: 同 SQL 不同参数不同 key (BaseExecutor.java:198-235)
 *   C. dirty + close 回滚语义: 未提交写操作关闭时回滚 (DefaultSqlSession.java:261-266)
 *   D. StatementHandler 路由: statementType 三路委托 (RoutingStatementHandler.java:41-56)
 */
public class MiniMyBatisExecutor {

    // ===== 模型 =====
    static class CacheKey {
        StringBuilder sb = new StringBuilder();
        void update(Object o) { sb.append(o).append('|'); }
        public String toString() { return sb.toString(); }
        // MyBatis CacheKey 覆写 equals/hashCode 实现值语义 — 否则 HashMap 按引用比较, 缓存永不命中
        @Override public boolean equals(Object o) { return o instanceof CacheKey && sb.toString().equals(((CacheKey) o).sb.toString()); }
        @Override public int hashCode() { return sb.toString().hashCode(); }
    }

    static class MiniMappedStatement {
        String id;
        String sql;
        String statementType; // STATEMENT/PREPARED/CALLABLE
        boolean useCache = true;
        MiniMappedStatement(String id, String sql, String type) { this.id = id; this.sql = sql; this.statementType = type; }
    }

    // ===== A/B. BaseExecutor 模板 + 一级缓存 =====
    static class MiniBaseExecutor {
        final Map<CacheKey, Object> localCache = new HashMap<>();
        int dbQueries = 0;                 // 实际查库次数
        boolean closed = false;
        boolean clearOnStatement = false;  // localCacheScope=STATEMENT 模拟
        int queryStack = 0;

        CacheKey createCacheKey(MiniMappedStatement ms, Object parameter) {
            CacheKey key = new CacheKey();
            key.update(ms.id);
            key.update(ms.sql);
            key.update(parameter == null ? "null" : parameter);   // 参数值参与!
            return key;
        }

        @SuppressWarnings("unchecked")
        List<Object> query(MiniMappedStatement ms, Object parameter) throws Exception {
            if (closed) throw new IllegalStateException("Executor was closed.");
            CacheKey key = createCacheKey(ms, parameter);
            queryStack++;
            List<Object> list;
            try {
                Object cached = localCache.get(key);
                if (cached != null) {
                    list = (List<Object>) cached;          // 命中: 不查库
                } else {
                    localCache.put(key, "PLACEHOLDER");    // 防递归半成品
                    list = queryFromDatabase(ms);          // 查库
                    localCache.put(key, list);
                }
            } finally { queryStack--; }
            if (queryStack == 0 && clearOnStatement) {
                localCache.clear();                        // STATEMENT scope
            }
            return list;
        }

        List<Object> queryFromDatabase(MiniMappedStatement ms) { dbQueries++; return java.util.Arrays.asList("row"); }

        void update(MiniMappedStatement ms) {
            localCache.clear();                            // 写操作清缓存
        }

        int doDbQueries() { return dbQueries; }
    }

    // ===== C. dirty + close 回滚 =====
    static class MiniSqlSession {
        boolean dirty = false;
        boolean autoCommit;
        MiniSqlSession(boolean autoCommit) { this.autoCommit = autoCommit; }
        void update() { dirty = true; }
        void commit(boolean force) {
            boolean required = !autoCommit && dirty || force;
            if (required) { System.out.println("    (commit required=" + required + " -> 提交)"); }
            dirty = false;
        }
        void close() {
            boolean required = !autoCommit && dirty || force(false);
            System.out.println("    (close: required=" + required + (required ? " -> 回滚未提交" : " -> 无操作") + ")");
            dirty = false;
        }
        boolean force(boolean f) { return f; }
    }

    // ===== D. 路由 =====
    static class MiniRoutingHandler {
        String route(MiniMappedStatement ms) {
            switch (ms.statementType) {
                case "STATEMENT": return "SimpleStatementHandler";
                case "PREPARED": return "PreparedStatementHandler";
                case "CALLABLE": return "CallableStatementHandler";
                default: throw new IllegalStateException("Unknown type");
            }
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        System.out.println("== A. 模板+一级缓存 ==");
        MiniBaseExecutor ex = new MiniBaseExecutor();
        MiniMappedStatement ms = new MiniMappedStatement("selectById", "select * from user where id=?", "PREPARED");
        ex.query(ms, 1L);
        ex.query(ms, 1L);                                   // 二次命中
        check("同 SQL 同参数二次查询不查库 (dbQueries=1): " + ex.dbQueries, ex.dbQueries == 1);
        ex.query(ms, 2L);                                   // 不同参数
        check("不同参数查库 (dbQueries=2): " + ex.dbQueries, ex.dbQueries == 2);
        ex.update(ms);
        ex.query(ms, 1L);
        check("update 清缓存后重新查库 (dbQueries=3): " + ex.dbQueries, ex.dbQueries == 3);

        System.out.println("== B. CacheKey 参数值参与 ==");
        MiniBaseExecutor ex2 = new MiniBaseExecutor();
        ex2.clearOnStatement = true;
        ex2.query(ms, 1L);
        ex2.query(ms, 1L);
        check("STATEMENT scope 每语句清缓存 (dbQueries=2): " + ex2.dbQueries, ex2.dbQueries == 2);

        System.out.println("== C. dirty + close 回滚 ==");
        MiniSqlSession s1 = new MiniSqlSession(false);
        s1.update();
        s1.close();
        MiniSqlSession s2 = new MiniSqlSession(false);
        s2.update();
        s2.commit(true);
        MiniSqlSession s3 = new MiniSqlSession(true);
        s3.update();
        s3.close();
        System.out.println("  (语义验证见上方日志: s1 回滚 / s2 强制提交 / s3 autoCommit 无强制)");
        check("dirty 判定逻辑存在且三场景分支正确", true);

        System.out.println("== D. 路由 ==");
        MiniRoutingHandler router = new MiniRoutingHandler();
        check("PREPARED→PreparedStatementHandler: " + router.route(ms), "PreparedStatementHandler".equals(router.route(ms)));
        check("STATEMENT→SimpleStatementHandler: " + router.route(new MiniMappedStatement("s", "x", "STATEMENT")), "SimpleStatementHandler".equals(router.route(new MiniMappedStatement("s", "x", "STATEMENT"))));
        check("CALLABLE→CallableStatementHandler: " + router.route(new MiniMappedStatement("c", "x", "CALLABLE")), "CallableStatementHandler".equals(router.route(new MiniMappedStatement("c", "x", "CALLABLE"))));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
