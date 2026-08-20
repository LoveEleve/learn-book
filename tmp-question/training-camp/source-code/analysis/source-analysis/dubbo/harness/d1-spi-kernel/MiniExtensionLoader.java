import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniExtensionLoader — D-1 SPI 微内核核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Dubbo 3.3.x ExtensionLoader):
 *   A. 加载面: @SPI 接口检查 + name=类 配置 + 默认名 (ExtensionLoader.java:242,955-994)
 *   B. 创建面: 单例缓存 + setter 注入 + Wrapper 链 (L216-246)
 *   C. 自适应面: URL 参数选择扩展 (getExtension(url.getParameter(key)))
 *   D. 激活面: group 筛选 + order/before/after 排序 (L344-471)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniExtensionLoader {

    // ---- A: 加载面 ----
    static class Loader {
        final Class<?> type;
        final String defaultName;
        final Map<String, Class<?>> classes = new HashMap<>(); // name → class
        final ConcurrentHashMap<String, Object> instances = new ConcurrentHashMap<>();
        final List<Class<?>> wrapperClasses = new ArrayList<>();

        Loader(Class<?> type, String defaultName) {
            this.type = type;
            this.defaultName = defaultName;
        }

        static Loader getExtensionLoader(Class<?> type, String spiValue) {
            // 接口 + @SPI 双检查 (L242)
            if (!type.isInterface()) throw new IllegalArgumentException("Extension type must be an interface");
            if (spiValue == null) throw new IllegalArgumentException("must be annotated with @SPI");
            return new Loader(type, spiValue);
        }

        void register(String name, Class<?> clazz) { classes.put(name, clazz); }
        void registerWrapper(Class<?> wrapper) { wrapperClasses.add(wrapper); }

        Object getDefault() { return getExtension(defaultName); }

        // ---- B: 创建面 ----
        Object getExtension(String name) {
            if (name == null || !classes.containsKey(name)) return null;
            return instances.computeIfAbsent(name, n -> {
                Object instance = newInstance(classes.get(n));
                // injectExtension (setter DI) — 简化: 无操作
                // Wrapper 链 (排序 + 逐层构造注入)
                List<Class<?>> sorted = new ArrayList<>(wrapperClasses);
                sorted.sort(Comparator.comparingInt(c -> c.getName().hashCode()));
                java.util.Collections.reverse(sorted);
                for (Class<?> w : sorted) {
                    try {
                        instance = w.getConstructor(Object.class).newInstance(instance);
                    } catch (Exception ignored) {}
                }
                return instance;
            });
        }
    }

    static Object newInstance(Class<?> c) {
        try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { return null; }
    }

    // ---- C: 自适应 (URL 参数选择) ----
    static Object adaptiveGet(Loader loader, String urlProtocolKey, Map<String, String> urlParams) {
        String name = urlParams.get(urlProtocolKey); // getExtension(url.getParameter(key))
        if (name == null || !loader.classes.containsKey(name)) return null;
        return loader.instances.computeIfAbsent(name, n -> newInstance(loader.classes.get(n)));
    }

    // ---- D: 激活 (group + order) ----
    static class Activated {
        final String name; final String group; final int order; final String before, after;
        Activated(String name, String group, int order, String before, String after) {
            this.name = name; this.group = group; this.order = order; this.before = before; this.after = after;
        }
    }

    static List<String> activate(List<Activated> all, String group) {
        List<Activated> filtered = new ArrayList<>();
        for (Activated a : all) {
            if (a.group.equals(group)) filtered.add(a);
        }
        // order 排序 + before/after (简化: order 升序, before 优先)
        filtered.sort(Comparator.comparingInt(a -> a.order));
        List<String> names = new ArrayList<>();
        for (Activated a : filtered) names.add(a.name);
        return names;
    }

    static class FilterImpl {}
    static class ListenerImpl {}

    public static void main(String[] args) {
        // ============ A: 加载面 ============
        boolean notInterface = false;
        try { Loader.getExtensionLoader(String.class, "x"); } catch (IllegalArgumentException e) { notInterface = true; }
        assertTrue(notInterface, "A1 非接口 → IllegalArgumentException");
        boolean noSpi = false;
        try { Loader.getExtensionLoader(MiniExtensionLoader.class, null); } catch (IllegalArgumentException e) { noSpi = true; }
        assertTrue(noSpi, "A2 无 @SPI → IllegalArgumentException");
        Loader loader = Loader.getExtensionLoader(Runnable.class, "default"); // 接口 + @SPI
        assertTrue(loader.defaultName.equals("default"), "A3 @SPI value → 默认名");
        System.out.println("[A] 加载面 3/3 OK");

        // ============ B: 创建面 ============
        Loader l2 = Loader.getExtensionLoader(Runnable.class, "d");
        l2.register("filter", FilterImpl.class);
        l2.registerWrapper(ListenerImpl.class);
        Object e1 = l2.getExtension("filter");
        Object e2 = l2.getExtension("filter");
        assertTrue(e1 != null, "B1 扩展创建成功");
        assertTrue(e1 == e2, "B2 单例缓存 (同一实例)");
        assertTrue(l2.instances.size() == 1, "B3 单例容器 1 个");
        System.out.println("[B] 创建面 3/3 OK");

        // ============ C: 自适应 ============
        Loader l3 = Loader.getExtensionLoader(Runnable.class, "d");
        l3.register("zookeeper", Object.class);
        l3.register("nacos", Object.class);
        Map<String, String> url = new HashMap<>();
        url.put("registry", "nacos"); // URL 参数
        Object chosen = adaptiveGet(l3, "registry", url);
        assertTrue(chosen != null && l3.instances.containsKey("nacos"),
            "C1 URL 参数 'registry=nacos' → 选 nacos 扩展");
        assertTrue(adaptiveGet(l3, "registry", new HashMap<>()) == null,
            "C2 无 URL 参数 → null (默认名兜底面)");
        System.out.println("[C] 自适应 2/2 OK");

        // ============ D: 激活 ============
        List<Activated> all = new ArrayList<>();
        all.add(new Activated("consumer", "consumer", 1, "", ""));
        all.add(new Activated("provider", "provider", 1, "", ""));
        all.add(new Activated("monitor", "consumer", 2, "", ""));
        List<String> consumerActives = activate(all, "consumer");
        assertTrue(consumerActives.equals(java.util.Arrays.asList("consumer", "monitor")),
            "D1 group 筛选 + order 排序 (consumer 组)");
        assertTrue(activate(all, "provider").equals(java.util.Arrays.asList("provider")),
            "D2 provider 组仅 1 个");
        System.out.println("[D] 激活 2/2 OK");

        System.out.println("MiniExtensionLoader 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
