import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniMapperProxy — M-3 Mapper 代理 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis 源码):
 *   A. MapperRegistry 先 put 后 parse + 失败回滚 (MapperRegistry.java:60-80)
 *   B. 代理 invoke 分派: Object 方法直通 / plain vs default 方法 (MapperProxy.java:81-112)
 *   C. MapperMethod.execute SELECT 五分支判定顺序 (MapperMethod.java:75-92)
 *   D. ParamNameResolver 三态: 无参/单参裸值/多参 ParamMap+paramN (ParamNameResolver.java:110-146)
 */
public class MiniMapperProxy {

    // ===== A. 注册 =====
    static class MiniRegistry {
        final Map<Class<?>, Boolean> known = new ConcurrentHashMap<>();
        int parseCount = 0;

        void addMapper(Class<?> type, boolean parseThrows) {
            if (!type.isInterface()) return;
            if (known.containsKey(type)) throw new IllegalStateException("already known");
            boolean loadCompleted = false;
            try {
                known.put(type, true);            // 先 put
                parseCount++;                     // 模拟 MapperAnnotationBuilder.parse
                if (parseThrows) throw new RuntimeException("parse failed");
                loadCompleted = true;
            } finally {
                if (!loadCompleted) {
                    known.remove(type);           // 失败回滚
                }
            }
        }
    }

    // ===== B. 代理分派 =====
    interface UserMapper {
        String selectById(Long id);               // plain 方法
        default String hello() { return "hello"; } // default 方法
        @Override String toString();
    }

    static class MiniInvoker {
        Object invoke(Method m, Object[] args) {
            if (m.getName().equals("selectById")) return "user:" + args[0];
            return null;
        }
    }

    static class MiniHandler implements InvocationHandler {
        final MiniInvoker invoker = new MiniInvoker();
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(this, args);      // Object 方法直通
            }
            if (method.isDefault()) {
                return "DEFAULT_METHOD:" + method.getName();  // 简化: default 走 MethodHandle(此处模拟)
            }
            return invoker.invoke(method, args);       // plain → MapperMethod
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T getMapper(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new MiniHandler());
    }

    // ===== C. execute 分发 =====
    static class MiniMapperMethod {
        final String type;   // SELECT
        final boolean returnsVoid, returnsMany, returnsMap, returnsCursor;
        MiniMapperMethod(boolean v, boolean many, boolean map, boolean cur) {
            returnsVoid = v; returnsMany = many; returnsMap = map; returnsCursor = cur; type = "SELECT";
        }
        String execute() {
            if (type.equals("SELECT")) {
                if (returnsVoid) return "executeWithResultHandler";
                if (returnsMany) return "executeForMany";
                if (returnsMap) return "executeForMap";
                if (returnsCursor) return "executeForCursor";
                return "selectOne";
            }
            return "rowCountResult";
        }
    }

    // ===== D. 参数命名三态 =====
    static class MiniParamResolver {
        static Object getNamedParams(boolean hasParamAnnotation, int paramCount, Object[] args, String firstKey) {
            if (args == null || paramCount == 0) return null;
            if (!hasParamAnnotation && paramCount == 1) {
                Object value = args[0];
                if (value instanceof List) {          // wrapToMapIfCollection
                    Map<String, Object> m = new HashMap<>();
                    m.put("collection", value);
                    m.put("list", value);
                    return m;
                }
                return value;                         // 裸值
            }
            Map<String, Object> param = new HashMap<>();
            param.put(firstKey, args[0]);
            if (paramCount > 1) {
                for (int i = 1; i <= paramCount; i++) {
                    param.put("param" + i, args[i - 1]);
                }
            }
            return param;
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== A. 注册时序 ==");
        MiniRegistry reg = new MiniRegistry();
        reg.addMapper(UserMapper.class, false);
        check("成功注册: " + reg.known.containsKey(UserMapper.class), reg.known.containsKey(UserMapper.class));
        boolean dupThrown = false;
        try { reg.addMapper(UserMapper.class, false); } catch (IllegalStateException e) { dupThrown = e.getMessage().contains("already known"); }
        check("重复注册抛: " + dupThrown, dupThrown);
        MiniRegistry reg2 = new MiniRegistry();
        try { reg2.addMapper(UserMapper.class, true); } catch (RuntimeException ignored) {}
        check("解析失败回滚移除: " + !reg2.known.containsKey(UserMapper.class), !reg2.known.containsKey(UserMapper.class));

        System.out.println("== B. 代理分派 ==");
        UserMapper mapper = getMapper(UserMapper.class);
        check("plain 方法转发: " + mapper.selectById(1L), "user:1".equals(mapper.selectById(1L)));
        check("default 方法特殊处理: " + mapper.hello(), "DEFAULT_METHOD:hello".equals(mapper.hello()));
        check("toString 不触发 SQL: " + mapper.toString().startsWith("MiniMapperProxy$MiniHandler"), mapper.toString().startsWith("MiniMapperProxy$MiniHandler"));

        System.out.println("== C. execute 分发 ==");
        check("void+Handler 最先: " + new MiniMapperMethod(true, true, true, true).execute(), "executeWithResultHandler".equals(new MiniMapperMethod(true, true, true, true).execute()));
        check("many 次之: " + new MiniMapperMethod(false, true, true, true).execute(), "executeForMany".equals(new MiniMapperMethod(false, true, true, true).execute()));
        check("map 第三: " + new MiniMapperMethod(false, false, true, true).execute(), "executeForMap".equals(new MiniMapperMethod(false, false, true, true).execute()));
        check("cursor 第四: " + new MiniMapperMethod(false, false, false, true).execute(), "executeForCursor".equals(new MiniMapperMethod(false, false, false, true).execute()));
        check("单值兜底: " + new MiniMapperMethod(false, false, false, false).execute(), "selectOne".equals(new MiniMapperMethod(false, false, false, false).execute()));

        System.out.println("== D. 参数命名三态 ==");
        check("无参→null", MiniParamResolver.getNamedParams(false, 0, null, null) == null);
        Object single = MiniParamResolver.getNamedParams(false, 1, new Object[]{42L}, null);
        check("单参无注解→裸值: " + single, single.equals(42L));
        Object list = MiniParamResolver.getNamedParams(false, 1, new Object[]{new ArrayList<Long>()}, null);
        check("单 List→collection/list 包装: " + ((Map) list).keySet(), ((Map) list).containsKey("collection") && ((Map) list).containsKey("list"));
        Object multi = MiniParamResolver.getNamedParams(true, 2, new Object[]{"a", 1L}, "id");
        check("多参→具名+param1..N: " + ((Map) multi).keySet(), ((Map) multi).get("id").equals("a") && ((Map) multi).get("param2").equals(1L));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
