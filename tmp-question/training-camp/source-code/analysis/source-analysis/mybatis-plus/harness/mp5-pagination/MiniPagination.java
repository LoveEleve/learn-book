import java.util.ArrayList;
import java.util.List;

/**
 * MiniPagination — MP-5 分页插件 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis-Plus 源码):
 *   A. willDoQuery count 预检 + continuePage 短路 (PaginationInnerInterceptor.java:116-145,432-450)
 *   B. beforeQuery SQL 改写 (dialect.buildPaginationSql) (L149-195)
 *   C. 方言策略: MySql LIMIT vs Oracle ROWNUM 差异 (IDialect 实现)
 *   D. DialectModel 参数消费: offset/limit 占位标记 (DialectModel.java:100-145)
 */
public class MiniPagination {

    // ===== 页对象 =====
    static class Page {
        long current = 1, size = 10, total = 0;
        long offset() { return (current - 1) * size; }   // 简化: current 从 1 开始
        long pages() { return total == 0 ? 0 : (total + size - 1) / size; }
    }

    // ===== C. 方言策略 =====
    interface IDialect {
        DialectModel buildPaginationSql(String originalSql, long offset, long limit);
    }
    static class MySqlDialect implements IDialect {
        public DialectModel buildPaginationSql(String sql, long offset, long limit) {
            if (offset == 0) {
                return new DialectModel(sql + " LIMIT ?", limit);       // 单参数
            }
            return new DialectModel(sql + " LIMIT ?, ?", offset, limit); // 双参数
        }
    }
    static class OracleDialect implements IDialect {
        public DialectModel buildPaginationSql(String sql, long offset, long limit) {
            return new DialectModel("SELECT * FROM (SELECT TMP.*, ROWNUM ROW_ID FROM (" + sql + ") TMP WHERE ROWNUM <= ?) WHERE ROW_ID > ?", offset, limit);
        }
    }

    // ===== D. 参数消费 =====
    static class DialectModel {
        final String sql; final List<Long> params = new ArrayList<>();
        DialectModel(String sql, long... p) { this.sql = sql; for (long v : p) params.add(v); }
    }

    // ===== A. count 预检 =====
    static class MiniPaginationInterceptor {
        int countExecutions = 0;
        final Page page;

        MiniPaginationInterceptor(Page p) { this.page = p; }

        boolean willDoQuery() {
            if (page.size < 0) return true;                    // 不接管
            countExecutions++;
            page.total = 57;                                   // 模拟 count 查询结果
            return continuePage(page);
        }
        static boolean continuePage(Page p) {
            if (p.total <= 0) return false;
            if (p.current > p.pages()) return false;           // 页码越界未开 overflow
            return true;
        }

        DialectModel beforeQuery(IDialect dialect) {
            return dialect.buildPaginationSql("SELECT * FROM user", page.offset(), page.size);
        }
    }

    // ===== B. 流程 =====
    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A. count 预检 + 短路 ==");
        Page p1 = new Page();
        MiniPaginationInterceptor interceptor = new MiniPaginationInterceptor(p1);
        boolean go1 = interceptor.willDoQuery();
        check("count 执行一次 + 正常继续: " + interceptor.countExecutions + "/" + go1, interceptor.countExecutions == 1 && go1);
        check("total 已回填: " + p1.total, p1.total == 57);

        Page p2 = new Page();
        p2.current = 99;                                       // 越界
        MiniPaginationInterceptor interceptor2 = new MiniPaginationInterceptor(p2);
        boolean go2 = interceptor2.willDoQuery();
        check("页码越界短路 (count 执行但数据不查): " + go2 + "/" + interceptor2.countExecutions, !go2 && interceptor2.countExecutions == 1);

        Page p3 = new Page();
        p3.size = -1;                                          // 只排序模式
        MiniPaginationInterceptor interceptor3 = new MiniPaginationInterceptor(p3);
        check("size<0 不接管 (无 count): " + interceptor3.willDoQuery() + "/" + interceptor3.countExecutions, interceptor3.willDoQuery() && interceptor3.countExecutions == 0);

        System.out.println("== B/C. SQL 改写 + 方言 ==");
        Page p4 = new Page();
        p4.current = 2;                                         // offset=10
        MiniPaginationInterceptor pi = new MiniPaginationInterceptor(p4);
        DialectModel mysql = pi.beforeQuery(new MySqlDialect());
        check("MySQL 双参数 (offset!=0): " + mysql.sql, "SELECT * FROM user LIMIT ?, ?".equals(mysql.sql));
        check("参数值: " + mysql.params, mysql.params.get(0) == 10L && mysql.params.get(1) == 10L);

        Page p0 = new Page();                                   // current=1 → offset=0
        DialectModel mysql0 = new MiniPaginationInterceptor(p0).beforeQuery(new MySqlDialect());
        check("MySQL 单参数 (offset=0): " + mysql0.sql, "SELECT * FROM user LIMIT ?".equals(mysql0.sql));

        DialectModel oracle = new MiniPaginationInterceptor(p0).beforeQuery(new OracleDialect());
        check("Oracle ROWNUM 改写: " + oracle.sql.substring(0, 40) + "...", oracle.sql.contains("ROWNUM") && oracle.sql.contains("ROW_ID"));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
