import java.util.ArrayList;
import java.util.List;

/**
 * MiniFilterChain — Druid Filter 链极简复现 (费曼法)
 *
 * 只保留核心控制流: pos 递归下推 + 链尾执行 + 短路三态(放行/过滤/拦截)
 * 去掉: 全量 JDBC 方法集/代理对象族/对象池复用/SPI 加载
 *
 * 对应源码:
 *   FilterChainImpl.java:37,41,469 (pos/filterSize/nextFilter)
 *   FilterChainImpl.java:3005-3010 (链尾裸执行)
 *   FilterAdapter (空实现=纯放行)
 *   FilterEventAdapter.java:178 (模板 Before/After 钩子)
 *   WallFilter.java:480-489 (覆写=裁决/拦截)
 */
public class MiniFilterChain {

    /** Filter 接口 (只留 execute 一个方法) */
    interface Filter {
        boolean execute(FilterChain chain, String sql);
    }

    /** 空实现 = 纯放行 (FilterAdapter) */
    abstract static class FilterAdapter implements Filter {
        @Override
        public boolean execute(FilterChain chain, String sql) {
            return chain.execute(sql);
        }
    }

    /** 模板层: Before/After 钩子 + 默认放行 (FilterEventAdapter) */
    abstract static class FilterEventAdapter extends FilterAdapter {
        @Override
        public boolean execute(FilterChain chain, String sql) {
            executeBefore(sql);                              // 钩子
            boolean firstResult = super.execute(chain, sql); // 默认放行
            executeAfter(sql, firstResult);                  // 钩子
            return firstResult;
        }

        protected void executeBefore(String sql) {
        }

        protected void executeAfter(String sql, boolean firstResult) {
        }
    }

    /** 链: pos 递归下推 (FilterChainImpl) */
    static class FilterChain {
        private final List<Filter> filters;
        private int pos;

        FilterChain(List<Filter> filters) {
            this.filters = filters;
        }

        boolean execute(String sql) {
            if (pos < filters.size()) {
                return filters.get(pos++).execute(this, sql);   // nextFilter 下推
            }
            return executeRaw(sql);                             // 链尾: 真实动作
        }

        /** 链尾: 裸执行 (rawObject.execute) */
        boolean executeRaw(String sql) {
            System.out.println("  [链尾] 真正执行: " + sql);
            return true;
        }
    }

    /** 风格 B: 监控器 — 只观察, 无拦截能力 (StatFilter) */
    static class MonitorFilter extends FilterEventAdapter {
        private long totalNanos;

        @Override
        protected void executeBefore(String sql) {
            System.out.println("    [Monitor] Before: " + sql);
        }

        @Override
        protected void executeAfter(String sql, boolean firstResult) {
            totalNanos += 10;
            System.out.println("    [Monitor] After: 累计耗时 " + totalNanos);
        }
    }

    /** 风格 A: 防火墙 — 覆写本体, 前置裁决, 可拦截 (WallFilter) */
    static class FirewallFilter extends FilterAdapter {
        private static final String ATTACK = "1=1";

        @Override
        public boolean execute(FilterChain chain, String sql) {
            System.out.println("    [Firewall] 检查: " + sql);
            if (sql.contains(ATTACK)) {
                // 过滤: 可清除的攻击片段先改写
                String safeSql = sql.replace("OR 1=1", "");
                if (safeSql.contains(ATTACK)) {
                    throw new SecurityException("SQL 注入拦截: " + sql); // 不可清除 → 拦截
                }
                System.out.println("    [Firewall] 过滤改写: " + sql + " → " + safeSql);
                return chain.execute(safeSql); // 改写后放行
            }
            return chain.execute(sql); // 放行
        }
    }

    /** 冒烟测试: 三态语义 (放行/过滤/拦截) + 模板钩子 */
    public static void main(String[] args) {
        List<Filter> filters = new ArrayList<>();
        filters.add(new MonitorFilter());   // 先入链: 模板钩子, 只观察
        filters.add(new FirewallFilter());  // 后入链: 覆写, 裁决

        System.out.println("== 1. 正常 SQL: 观察→检查→放行→链尾 ==");
        new FilterChain(filters).execute("SELECT * FROM user WHERE id=1");

        // 注: 每轮 new 链等价于 Druid 的 recycleFilterChain.reset(pos=0) —
        // 复用同一个链实例不 reset 会直接落到链尾(本轮调试实证了 reset 的必要性, 对应源码 FilterChainImpl.java:62-64)
        System.out.println("== 2. 攻击 SQL: 观察→检查→拦截(抛异常, 不调 chain) ==");
        try {
            new FilterChain(filters).execute("SELECT * FROM user WHERE 1=1");
            throw new AssertionError("应该被拦截");
        } catch (SecurityException e) {
            System.out.println("  拦截成功: " + e.getMessage());
        }

        System.out.println("== 3. 可改写 SQL: 检查→过滤改写→放行(链尾拿到改写后 SQL) ==");
        new FilterChain(filters).execute("SELECT * FROM user WHERE id=1 OR 1=1 AND x=2");

        System.out.println("=== MiniFilterChain 冒烟测试通过 ===");
    }
}
