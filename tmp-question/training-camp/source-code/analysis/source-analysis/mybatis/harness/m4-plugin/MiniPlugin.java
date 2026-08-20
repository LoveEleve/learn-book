import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MiniPlugin — M-4 插件机制 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis 源码):
 *   A. pluginAll 洋葱嵌套: 后注册包外层, 执行顺序与注册相反 (InterceptorChain.java:34-39)
 *   B. Plugin.wrap 无匹配接口不包装 (Plugin.java:44-52)
 *   C. invoke 双条件分派: declaringClass 命中+contains → intercept; 否则透传 (Plugin.java:55-65)
 *   D. Invocation 4 类白名单: 其他目标 invoke 时抛 (Invocation.java:30-36, 2024-03 引入)
 */
public class MiniPlugin {

    interface Executor { Object query(String sql); String name(); }
    static class SimpleExecutor implements Executor {
        public Object query(String sql) { return "RESULT:" + sql; }
        public String name() { return "Simple"; }
    }

    static class Invocation {
        static final List<Class<?>> targetClasses = Arrays.asList(Executor.class); // 简化白名单: 只允许 Executor
        final Object target; final Method method; final Object[] args;
        Invocation(Object t, Method m, Object[] a) {
            if (!targetClasses.contains(m.getDeclaringClass())) {
                throw new IllegalArgumentException("Method '" + m + "' is not supported as a plugin target.");
            }
            target = t; method = m; args = a;
        }
        Object proceed() throws Exception { return method.invoke(target, args); }
    }

    interface Interceptor {
        Object intercept(Invocation inv) throws Exception;
        default Object plugin(Object target) { return MiniPlugin.wrap(target, this); }
    }

    static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor ic, Class<?> targetType, String methodName) {
        Map<Class<?>, Set<Method>> map = new HashMap<>();
        try {
            Set<Method> ms = new HashSet<>();
            for (Method m : targetType.getMethods()) {   // 简化: 按名匹配所有签名
                if (m.getName().equals(methodName)) ms.add(m);
            }
            map.put(targetType, ms);
        } catch (Exception ignored) {}
        return map;
    }

    static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> sigMap) {
        Set<Class<?>> interfaces = new HashSet<>();
        while (type != null) {
            for (Class<?> c : type.getInterfaces()) {
                if (sigMap.containsKey(c)) interfaces.add(c);
            }
            type = type.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[0]);
    }

    static Object wrap(Object target, Interceptor interceptor) {
        Map<Class<?>, Set<Method>> sigMap = getSignatureMap(interceptor,
            target instanceof Executor ? Executor.class : target.getClass(), "query");
        Class<?>[] interfaces = getAllInterfaces(target.getClass(), sigMap);
        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(target.getClass().getClassLoader(), interfaces,
                (proxy, method, args) -> {
                    Set<Method> ms = sigMap.get(method.getDeclaringClass());
                    if (ms != null && ms.contains(method)) {
                        return interceptor.intercept(new Invocation(target, method, args));
                    }
                    return method.invoke(target, args);
                });
        }
        return target;   // 无匹配不包装
    }

    static class Chain {
        final List<Interceptor> interceptors = new ArrayList<>();
        Object pluginAll(Object target) {
            for (Interceptor ic : interceptors) { target = ic.plugin(target); }
            return target;
        }
    }

    static class LoggingInterceptor implements Interceptor {
        final String name; final List<String> log;
        LoggingInterceptor(String n, List<String> l) { name = n; log = l; }
        public Object intercept(Invocation inv) throws Exception {
            log.add(name + ":before");
            Object r = inv.proceed();
            log.add(name + ":after");
            return r;
        }
    }
    static class BlockInterceptor implements Interceptor {   // 短路拦截
        public Object intercept(Invocation inv) { return "BLOCKED"; }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== A. pluginAll 洋葱嵌套 ==");
        List<String> log = new ArrayList<>();
        Chain chain = new Chain();
        chain.interceptors.add(new LoggingInterceptor("A", log));
        chain.interceptors.add(new LoggingInterceptor("B", log));
        Executor ex = (Executor) chain.pluginAll(new SimpleExecutor());
        ex.query("select 1");
        check("后注册 B 先执行: " + log, "B:before".equals(log.get(0)) && "A:before".equals(log.get(1)));
        check("最外层 B 的 after 最后(洋葱): " + log.get(log.size() - 1), "B:after".equals(log.get(log.size() - 1)));

        System.out.println("== B. 无匹配不包装 ==");
        Chain noMatch = new Chain();
        noMatch.interceptors.add(new BlockInterceptor());  // 只拦 query, String 无接口匹配
        Object s = noMatch.pluginAll("plain-string");
        check("无关对象原样返回: " + s.getClass().getSimpleName(), "String".equals(s.getClass().getSimpleName()));

        System.out.println("== C. invoke 双条件 + 短路 ==");
        Chain blocked = new Chain();
        blocked.interceptors.add(new BlockInterceptor());
        Executor bEx = (Executor) blocked.pluginAll(new SimpleExecutor());
        check("拦截方法短路: " + bEx.query("x"), "BLOCKED".equals(bEx.query("x")));
        check("未拦截方法透传: " + bEx.name(), "Simple".equals(bEx.name()));

        System.out.println("== D. 白名单 ==");
        boolean thrown = false;
        try {
            Executor proxy = (Executor) Proxy.newProxyInstance(Executor.class.getClassLoader(),
                new Class[]{Executor.class}, (p, m, a) -> new Invocation(new HashMap<>(), HashMap.class.getMethod("get", Object.class), new Object[]{"k"}));
            proxy.query("x");
        } catch (IllegalArgumentException e) {
            thrown = e.getMessage().contains("not supported as a plugin target");
        }
        check("非 Executor 目标 invoke 抛: " + thrown, thrown);

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
