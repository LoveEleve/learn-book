import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniResultMapping — M-5 参数/结果映射 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis 源码):
 *   A. 取参四路: additionalParameter > parameterObject null > 裸值 hasTypeHandler > metaObject.getValue
 *      (DefaultParameterHandler.java:34-62)
 *   B. 自动映射: 列名→属性驼峰推断 + 按 resultMapId:columnPrefix 缓存 (DefaultResultSetHandler.java:580-645)
 *   C. createResultObject 分支链: 原始值 > 默认构造 > 抛 "Do not know how to create an instance"
 *      (DefaultResultSetHandler.java:654-700)
 *   D. 嵌套聚合: combinedKey 同 key partialObject 复用 + 循环引用祖先保护 (DefaultResultSetHandler.java:440-462)
 */
public class MiniResultMapping {

    // ===== A. 取参四路 =====
    static class MiniParamBinder {
        Object getValue(String property, Map<String, Object> additional, Object parameterObject,
                        Map<Class<?>, Boolean> typeHandlers) {
            if (additional != null && additional.containsKey(property)) return "ADD:" + additional.get(property);
            if (parameterObject == null) return null;
            if (typeHandlers.getOrDefault(parameterObject.getClass(), false)) return parameterObject; // 裸值
            if (parameterObject instanceof Map) return ((Map<?, ?>) parameterObject).get(property);
            try {
                return parameterObject.getClass().getMethod("get" + cap(property)).invoke(parameterObject); // 简化反射
            } catch (Exception e) {
                return null;
            }
        }
        static String cap(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    }
    static class User { public String name; public String getName() { return name; } }

    // ===== B. 自动映射 (驼峰+缓存) =====
    static class MiniAutoMapper {
        final Map<String, List<String>> cache = new HashMap<>(); // resultMapId:columnPrefix -> properties
        List<String> resolve(String mapKey, List<String> columns, boolean camelCase) {
            return cache.computeIfAbsent(mapKey, k -> {
                List<String> props = new ArrayList<>();
                for (String col : columns) {
                    String prop = camelCase ? camel(col) : col;   // findProperty 简化
                    props.add(prop);
                }
                return props;
            });
        }
        static String camel(String col) {   // 对齐 MyBatis underlineToCamelCase: 先转小写再处理
            StringBuilder sb = new StringBuilder();
            boolean upper = false;
            for (char c : col.toLowerCase().toCharArray()) {
                if (c == '_') { upper = true; continue; }
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
            return sb.toString();
        }
    }

    // ===== C. 对象创建分支链 =====
    static class MiniObjectFactory {
        Object create(Class<?> type, boolean hasTypeHandler, List<String> constructorMappings) {
            if (hasTypeHandler) return "RAW_VALUE";                        // ① 原始值
            if (!constructorMappings.isEmpty()) return "PARAM_CTOR";       // ② 构造器映射
            try {
                type.getDeclaredConstructor();                             // ③ 默认构造
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Do not know how to create an instance of " + type);
            }
        }
    }
    static class NoDefaultCtor {
        public NoDefaultCtor(String x) {}
    }

    // ===== D. 嵌套聚合 =====
    static class MiniNested {
        final Map<String, Object> nestedResultObjects = new HashMap<>();
        final List<String> ancestors = new ArrayList<>();
        int createCount = 0;

        Object handleRow(String combinedKey) {
            if (nestedResultObjects.containsKey(combinedKey)) {
                return nestedResultObjects.get(combinedKey);             // partialObject 复用
            }
            createCount++;
            Object obj = new Object();
            ancestors.add(combinedKey);                                   // putAncestor
            boolean hasCycle = ancestors.contains("self");                // 循环引用检测(简化)
            ancestors.remove(combinedKey);                                // removeAncestor
            if (!hasCycle) {
                nestedResultObjects.put(combinedKey, obj);
            }
            return obj;
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A. 取参四路 ==");
        MiniParamBinder binder = new MiniParamBinder();
        Map<String, Object> additional = new HashMap<>();
        additional.put("__frch_item_0", "foreachVal");
        check("additionalParameter 优先: " + binder.getValue("__frch_item_0", additional, new User(), new HashMap<>()),
            "ADD:foreachVal".equals(binder.getValue("__frch_item_0", additional, new User(), new HashMap<>())));
        check("parameterObject null → null", binder.getValue("id", null, null, new HashMap<>()) == null);
        Map<Class<?>, Boolean> th = new HashMap<>();
        th.put(Long.class, true);
        check("裸值直接绑定: " + binder.getValue("x", null, 42L, th), binder.getValue("x", null, 42L, th).equals(42L));
        User u = new User();
        u.name = "Bob";
        check("对象属性取值: " + binder.getValue("name", null, u, new HashMap<>()), "Bob".equals(binder.getValue("name", null, u, new HashMap<>())));

        System.out.println("== B. 自动映射驼峰+缓存 ==");
        MiniAutoMapper am = new MiniAutoMapper();
        List<String> props1 = am.resolve("map1:", Arrays.asList("USER_NAME", "AGE"), true);
        check("驼峰推断: " + props1, "userName".equals(props1.get(0)) && "age".equals(props1.get(1)));
        List<String> props2 = am.resolve("map1:", Arrays.asList("USER_NAME", "AGE"), true);
        check("同 key 二次命中缓存 (同一实例): " + (props1 == props2), props1 == props2);

        System.out.println("== C. 对象创建分支链 ==");
        MiniObjectFactory of = new MiniObjectFactory();
        check("原始值: " + of.create(User.class, true, new ArrayList<>()), "RAW_VALUE".equals(of.create(User.class, true, new ArrayList<>())));
        check("默认构造: " + of.create(User.class, false, new ArrayList<>()).getClass().getSimpleName(), of.create(User.class, false, new ArrayList<>()) instanceof User);
        boolean thrown = false;
        try { of.create(NoDefaultCtor.class, false, new ArrayList<>()); } catch (IllegalStateException e) { thrown = e.getMessage().contains("Do not know how to create"); }
        check("无默认构造抛: " + thrown, thrown);

        System.out.println("== D. 嵌套聚合 ==");
        MiniNested nested = new MiniNested();
        Object first = nested.handleRow("key1");
        Object second = nested.handleRow("key1");
        check("同 combinedKey partialObject 复用: " + (first == second), first == second);
        check("只创建一次: " + nested.createCount, nested.createCount == 1);
        nested.handleRow("key2");
        check("不同 key 独立对象: " + nested.createCount, nested.createCount == 2);

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
