import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniLambdaWrapper — MP-3 Lambda 条件构造器 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis-Plus 源码):
 *   A. 方法引用→方法名: writeReplace 反射提取 (SerializedLambda.extract 简化, LambdaUtils L50-62)
 *   B. 列名解析链: methodToProperty → formatKey 大写 → ColumnCache (AbstractLambdaWrapper L127-133)
 *   C. 条件构建: addCondition 统一入口 + MPGENVAL 参数命名 (AbstractWrapper L467-470)
 *   D. NormalSegmentList 净化: 首段 and/or 丢弃 + 相邻同类合并 (NormalSegmentList L30-60)
 */
public class MiniLambdaWrapper {

    static class User {
        private String userName;
        public String getUserName() { return userName; }
    }

    // SFunction 简化: Serializable 方法引用
    interface SFunction<T, R> extends Serializable { R apply(T t); }

    // ===== A/B. 提取与列名 =====
    static class MiniLambdaMeta {
        final String implMethodName;
        final Class<?> instantiatedClass;
        MiniLambdaMeta(String n, Class<?> c) { implMethodName = n; instantiatedClass = c; }
    }

    static MiniLambdaMeta extract(SFunction<?, ?> func) {
        try {
            Method m = func.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            Object sl = m.invoke(func);                          // 反射路径 (主流)
            String methodName = (String) sl.getClass().getMethod("getImplMethodName").invoke(sl);
            String instType = (String) sl.getClass().getMethod("getInstantiatedMethodType").invoke(sl);
            String cls = instType.substring(2, instType.indexOf(';')).replace('/', '.');
            return new MiniLambdaMeta(methodName, Class.forName(cls));
        } catch (Exception e) {
            throw new IllegalStateException("extract failed", e);
        }
    }

    static String methodToProperty(String name) {
        if (name.startsWith("get")) return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        return name;
    }

    static class MiniColumnCache {
        final String column;
        MiniColumnCache(String c) { column = c; }
    }

    static Map<String, MiniColumnCache> buildColumnMap(Class<?> clazz) {
        Map<String, MiniColumnCache> map = new HashMap<>();     // 模拟 MP-2 installCache
        map.put("USERNAME".toUpperCase(), new MiniColumnCache("user_name"));
        return map;
    }

    static String columnToString(SFunction<?, ?> column, Map<String, MiniColumnCache> cache) {
        MiniLambdaMeta meta = extract(column);
        String property = methodToProperty(meta.implMethodName);
        return cache.get(property.toUpperCase()).column;         // formatKey 全大写查
    }

    // ===== C. 条件构建 =====
    static class MiniWrapper {
        final List<String> segments = new ArrayList<>();
        int seq = 0;
        final Map<String, Object> paramNameValuePairs = new HashMap<>();

        MiniWrapper addCondition(boolean condition, String column, String op, Object val) {
            if (!condition) return this;                        // maybeDo 守卫
            String param = "MPGENVAL" + (++seq);               // 参数命名
            paramNameValuePairs.put(param, val);
            segments.add(column + " " + op + " #{ew.paramNameValuePairs." + param + "}");
            return this;
        }
        String getSql() { return String.join(" AND ", segments); }
    }

    // ===== D. 段净化 =====
    static class MiniNormalList {
        final List<String> items = new ArrayList<>();
        void add(String... segs) {
            for (String s : segs) {
                boolean isConnector = s.equals("AND") || s.equals("OR");
                if (isConnector && items.isEmpty()) continue;              // 首段连接词丢弃
                if (isConnector && (items.get(items.size()-1).equals("AND") || items.get(items.size()-1).equals("OR"))) {
                    if (items.get(items.size()-1).equals(s)) continue;      // 相邻同类合并
                }
                items.add(s);
            }
        }
        String getSql() {
            StringBuilder sb = new StringBuilder();
            for (String s : items) sb.append(sb.length() == 0 ? "" : " ").append(s);
            return sb.toString();
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A/B. 提取与列名 ==");
        SFunction<User, String> fn = User::getUserName;
        MiniLambdaMeta meta = extract(fn);
        check("writeReplace 提取方法名: " + meta.implMethodName, "getUserName".equals(meta.implMethodName));
        check("实体类解析: " + meta.instantiatedClass.getSimpleName(), "User".equals(meta.instantiatedClass.getSimpleName()));
        check("methodToProperty: " + methodToProperty(meta.implMethodName), "userName".equals(methodToProperty(meta.implMethodName)));
        String col = columnToString(fn, buildColumnMap(User.class));
        check("列名解析链→user_name: " + col, "user_name".equals(col));

        System.out.println("== C. 条件构建 ==");
        MiniWrapper w = new MiniWrapper();
        w.addCondition(true, "name", "=", "张三");
        w.addCondition(false, "age", ">", 18);      // 条件守卫关闭
        w.addCondition(true, "age", "<", 60);
        check("守卫+MPGENVAL: " + w.getSql(), "name = #{ew.paramNameValuePairs.MPGENVAL1} AND age < #{ew.paramNameValuePairs.MPGENVAL2}".equals(w.getSql()));
        check("参数表: " + w.paramNameValuePairs, "张三".equals(w.paramNameValuePairs.get("MPGENVAL1")));

        System.out.println("== D. 段净化 ==");
        MiniNormalList list = new MiniNormalList();
        list.add("AND", "name = ?", "OR", "age > ?", "AND", "AND", "id = ?");
        check("首段 AND 丢弃+相邻同类合并: " + list.getSql(), "name = ? OR age > ? AND id = ?".equals(list.getSql()));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
