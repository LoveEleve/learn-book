import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MiniMybatisPlusInterceptor — MP-4 插件体系 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis-Plus 源码):
 *   A. 签名挂载 + plugin 过滤: 只包装 Executor/StatementHandler (MybatisPlusInterceptor.java:110-115)
 *   B. intercept 分发: Executor query/update 两路 + StatementHandler (L56-107)
 *   C. willDoQuery 短路 + beforeQuery 改写 + 重发 query (L64-81)
 *   D. InnerInterceptor 回调链: 6 回调全 default, 实现者只覆写关心时机 (InnerInterceptor.java:53-126)
 */
public class MiniMybatisPlusInterceptor {

    interface Executor {
        Object query(String sql, Object parameter);
        int update(String sql, Object parameter);
        String name();
    }
    static class SimpleExecutor implements Executor {
        int queryCount = 0;
        public Object query(String sql, Object p) { queryCount++; return "RESULT:" + sql; }
        public int update(String sql, Object p) { return 1; }
        public String name() { return "Simple"; }
    }

    // ===== D. 回调契约 =====
    static class InnerInterceptor {
        boolean willDoQuery(Executor e, String sql, Object p) { return true; }
        void beforeQuery(Executor e, String sql, Object p) {}
        boolean willDoUpdate(Executor e, String sql, Object p) { return true; }
        void beforeUpdate(Executor e, String sql, Object p) {}
        void beforePrepare(Object sh) {}
        void beforeGetBoundSql(Object sh) {}
    }

    static class PagingInterceptor extends InnerInterceptor {   // 分页样例: willDoQuery 后改写
        final List<String> log = new ArrayList<>();
        boolean willDoQuery(Executor e, String sql, Object p) { log.add("willDoQuery"); return true; }
        void beforeQuery(Executor e, String sql, Object p) {
            log.add("beforeQuery(改写 SQL)");
            ((SimpleExecutor) e).query(sql + " LIMIT 10", p);   // 简化: 改写通过重发生效
        }
    }

    static class BlockInterceptor extends InnerInterceptor {   // 拒绝查询
        boolean willDoQuery(Executor e, String sql, Object p) { return false; }
    }

    // ===== A/B/C. 宿主 =====
    static class MiniHost {
        final List<InnerInterceptor> interceptors = new ArrayList<>();
        void addInnerInterceptor(InnerInterceptor ic) { interceptors.add(ic); }

        Object intercept(String methodName, Object[] args, Object target) {
            if (target instanceof Executor) {
                Executor ex = (Executor) target;
                String sql = (String) args[0];
                Object p = args[1];
                boolean isUpdate = methodName.equals("update");
                if (!isUpdate) {
                    for (InnerInterceptor ic : interceptors) {
                        if (!ic.willDoQuery(ex, sql, p)) {
                            return java.util.Collections.emptyList();   // 短路
                        }
                        ic.beforeQuery(ex, sql, p);
                    }
                    return ex.query(sql, p);                            // 重发
                } else {
                    for (InnerInterceptor ic : interceptors) {
                        if (!ic.willDoUpdate(ex, sql, p)) return -1;
                        ic.beforeUpdate(ex, sql, p);
                    }
                    return ex.update(sql, p);
                }
            }
            return "SH_PROCESSED";                                      // StatementHandler 简化
        }

        Executor plugin(Executor target) {
            return (Executor) Proxy.newProxyInstance(target.getClass().getClassLoader(),
                new Class[]{Executor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("query") || method.getName().equals("update")) {
                        return intercept(method.getName(), args, target);
                    }
                    return method.invoke(target, args);                 // 未签名方法透传
                });
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A/B. 挂载与分发 ==");
        MiniHost host = new MiniHost();
        SimpleExecutor se = new SimpleExecutor();
        Executor proxy = host.plugin(se);
        check("未签名方法透传: " + proxy.name(), "Simple".equals(proxy.name()));

        System.out.println("== C. 分页改写+重发 ==");
        MiniHost host2 = new MiniHost();
        host2.addInnerInterceptor(new PagingInterceptor());
        SimpleExecutor se2 = new SimpleExecutor();
        Object r = host2.plugin(se2).query("SELECT * FROM user", null);
        check("分页回调链执行: " + ((PagingInterceptor) host2.interceptors.get(0)).log,
            "willDoQuery".equals(((PagingInterceptor) host2.interceptors.get(0)).log.get(0))
                && ((PagingInterceptor) host2.interceptors.get(0)).log.get(1).startsWith("beforeQuery"));
        check("重发 query 执行 (count=2: 原+重发): " + se2.queryCount, se2.queryCount == 2);

        System.out.println("== C2. willDoQuery 短路 ==");
        MiniHost host3 = new MiniHost();
        host3.addInnerInterceptor(new BlockInterceptor());
        Object r3 = host3.plugin(new SimpleExecutor()).query("SELECT 1", null);
        check("拒绝查询→空列表: " + r3, r3 instanceof List && ((List<?>) r3).isEmpty());

        System.out.println("== C3. willDoUpdate 短路 ==");
        MiniHost host4 = new MiniHost();
        host4.addInnerInterceptor(new InnerInterceptor() {
            boolean willDoUpdate(Executor e, String sql, Object p) { return false; }
        });
        Object r4 = host4.plugin(new SimpleExecutor()).update("UPDATE user SET x=1", null);
        check("拒绝更新→-1: " + r4, Integer.valueOf(-1).equals(r4));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
