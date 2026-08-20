import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniDynamicSql — M-6 动态 SQL 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis 源码):
 *   A. IfSqlNode: test 表达式求值决定内容是否应用 (IfSqlNode.apply)
 *   B. ForEach __frch_ 参数唯一化: #{item}→#{__frch_item_i} + bind item_i (ForEachSqlNode L28,137)
 *   C. Trim 家族: Where 吃掉首个 AND/OR (TrimSqlNode applyAll/applyPrefix)
 *   D. 动态/静态分流: isDynamic→DynamicSqlSource(每次求值) vs RawSqlSource(构建期一次) (XMLScriptBuilder L65-74)
 */
public class MiniDynamicSql {

    interface SqlNode { boolean apply(DynamicContext ctx); }

    static class DynamicContext {
        final Map<String, Object> bindings = new HashMap<>();
        final StringBuilder sql = new StringBuilder();
        int uniqueNumber;
        void appendSql(String s) { sql.append(' ').append(s); }
        String getSql() { return sql.toString().trim(); }
        void bind(String k, Object v) { bindings.put(k, v); }
    }

    // A. If: 表达式求值
    static class IfSqlNode implements SqlNode {
        final String test; final SqlNode contents;
        IfSqlNode(String test, SqlNode c) { this.test = test; this.contents = c; }
        public boolean apply(DynamicContext ctx) {
            if (evalBoolean(test, ctx.bindings)) {   // 简化: 仅支持 "X != null" 判定
                contents.apply(ctx);
                return true;
            }
            return false;
        }
    }
    static boolean evalBoolean(String expr, Map<String, Object> b) {
        String name = expr.replaceAll("\\s*!=\\s*null\\s*", "").trim();
        return b.get(name) != null;
    }

    static class TextNode implements SqlNode {
        final String text;
        TextNode(String t) { this.text = t; }
        public boolean apply(DynamicContext ctx) { ctx.appendSql(text); return true; }
    }

    static class MixedNode implements SqlNode {
        final List<SqlNode> children;
        MixedNode(List<SqlNode> c) { this.children = c; }
        public boolean apply(DynamicContext ctx) { for (SqlNode n : children) n.apply(ctx); return true; }
    }

    // B. ForEach: __frch_ 唯一化
    static class ForEachNode implements SqlNode {
        static final String ITEM_PREFIX = "__frch_";
        final String collection, item, index, open, close, separator;
        final SqlNode contents;
        ForEachNode(String c, String item, String idx, String o, String cl, String sep, SqlNode body) {
            collection = c; this.item = item; index = idx; open = o; close = cl; separator = sep; contents = body;
        }
        static String itemizeItem(String item, int i) { return ITEM_PREFIX + item + "_" + i; }
        public boolean apply(DynamicContext ctx) {
            Object col = ctx.bindings.get(collection);
            if (col == null) return true;
            List<?> list = (List<?>) col;
            if (list.isEmpty()) return true;
            if (open != null) ctx.appendSql(open);
            int i = 0;
            for (Object o : list) {
                if (i > 0 && separator != null) ctx.appendSql(separator);
                ctx.bind(item, o);
                ctx.bind(index, i);
                ctx.bind(itemizeItem(item, i), o);       // bind __frch_item_i
                ctx.bind(itemizeItem(index, i), i);
                contents.apply(new FilteredContext(ctx, item, index, i)); // #{item}→#{__frch_item_i}
                i++;
            }
            if (close != null) ctx.appendSql(close);
            ctx.bindings.remove(item);
            ctx.bindings.remove(index);
            return true;
        }
    }
    static class FilteredContext extends DynamicContext {  // 对齐真实 FilteredDynamicContext: appendSql 时替换
        final DynamicContext delegate;
        final String item, index;
        final int i;
        FilteredContext(DynamicContext d, String item, String index, int i) {
            delegate = d; this.item = item; this.index = index; this.i = i;
        }
        @Override public void appendSql(String s) {   // 替换 #{item}/#{index} → #{__frch_*}
            delegate.appendSql(s.replace("#{" + item + "}", "#{" + ForEachNode.itemizeItem(item, i) + "}")
                                .replace("#{" + index + "}", "#{" + ForEachNode.itemizeItem(index, i) + "}"));
        }
    }

    // C. Trim 家族: Where
    static class TrimNode implements SqlNode {
        final String prefix; final List<String> prefixesToOverride; final SqlNode contents;
        TrimNode(String p, List<String> over, SqlNode c) { prefix = p; prefixesToOverride = over; contents = c; }
        public boolean apply(DynamicContext ctx) {
            contents.apply(ctx);                       // 先让子节点拼
            String body = ctx.sql.toString();
            ctx.sql.setLength(0);
            String trimmed = body.trim();
            String upper = trimmed.toUpperCase();
            for (String over : prefixesToOverride) {
                if (upper.startsWith(over)) { trimmed = trimmed.substring(over.length()).trim(); break; }
            }
            if (!trimmed.isEmpty()) {
                ctx.appendSql((prefix == null ? "" : prefix + " ") + trimmed);
            }
            return true;
        }
    }
    static SqlNode where(SqlNode c) { return new TrimNode("WHERE", Arrays.asList("AND ", "OR "), c); }

    // D. 分流
    static class MiniSqlSource {
        final SqlNode root; final boolean isDynamic;
        MiniSqlSource(SqlNode r, boolean dyn) { root = r; isDynamic = dyn; }
        String getSql(Map<String, Object> params) {   // 简化: 动态=每次求值; 静态=同一输出
            DynamicContext ctx = new DynamicContext();
            ctx.bindings.putAll(params);
            root.apply(ctx);
            return ctx.getSql();
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A. If 表达式 ==");
        IfSqlNode ifName = new IfSqlNode("name != null", new TextNode("AND name = #{name}"));
        DynamicContext ctx = new DynamicContext();
        ifName.apply(ctx);
        check("name 为 null 时不拼接: '" + ctx.getSql() + "'", ctx.getSql().isEmpty());
        DynamicContext ctx2 = new DynamicContext();
        ctx2.bindings.put("name", "Bob");
        ifName.apply(ctx2);
        check("name 非 null 时拼接: '" + ctx2.getSql() + "'", ctx2.getSql().contains("AND name = #{name}"));

        System.out.println("== B. ForEach __frch_ ==");
        DynamicContext ctx3 = new DynamicContext();
        ctx3.bindings.put("ids", Arrays.asList(1L, 2L));
        SqlNode body = new TextNode("#{item}");
        new ForEachNode("ids", "item", "i", "(", ")", ",", body).apply(ctx3);
        check("foreach 展开带 open/separator/close: '" + ctx3.getSql() + "'", "( #{__frch_item_0} , #{__frch_item_1} )".equals(ctx3.getSql()));
        check("__frch_ 值已 bind 且 item 已移除: " + (ctx3.bindings.get("__frch_item_0") + "/item=" + ctx3.bindings.get("item")),
            ctx3.bindings.get("__frch_item_0") != null && ctx3.bindings.get("item") == null);

        System.out.println("== C. Where 吃掉首个 AND ==");
        DynamicContext ctx4 = new DynamicContext();
        ctx4.bindings.put("age", 18);
        MixedNode body2 = new MixedNode(Arrays.asList(
            new IfSqlNode("name != null", new TextNode("AND name = #{name}")),
            new IfSqlNode("age != null", new TextNode("AND age > #{age}"))));
        where(body2).apply(ctx4);
        check("首个 AND 被吃掉+WHERE 前缀: '" + ctx4.getSql() + "'", "WHERE age > #{age}".equals(ctx4.getSql()));
        DynamicContext ctx5 = new DynamicContext();
        ctx5.bindings.put("name", "Bob");
        ctx5.bindings.put("age", 18);
        where(new MixedNode(Arrays.asList(
            new IfSqlNode("name != null", new TextNode("AND name = #{name}")),
            new IfSqlNode("age != null", new TextNode("AND age > #{age}"))))).apply(ctx5);
        check("两条件: 首 AND 吃掉第二保留: '" + ctx5.getSql() + "'", "WHERE name = #{name} AND age > #{age}".equals(ctx5.getSql()));

        System.out.println("== D. 分流 ==");
        MiniSqlSource dyn = new MiniSqlSource(ifName, true);
        check("动态: 每次按参数求值 (null→空)", dyn.getSql(new HashMap<>()).isEmpty() && dyn.getSql(java.util.Collections.singletonMap("name", "A")).contains("name"));
        MiniSqlSource stat = new MiniSqlSource(new TextNode("SELECT 1"), false);
        check("静态: 输出恒定 'SELECT 1'", "SELECT 1".equals(stat.getSql(new HashMap<>())));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
