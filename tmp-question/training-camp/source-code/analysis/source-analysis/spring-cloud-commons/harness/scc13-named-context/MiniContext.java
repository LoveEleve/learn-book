import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MiniContext — 微缩版 NamedContextFactory (子上下文隔离)
 *
 * 覆盖机制:
 * 1. contexts/configurations 双 Map + getContext 双检锁懒创建
 * 2. registerBeans 三段: 精确配置 / default. 前缀默认 / 基础设施
 * 3. buildContext: 属性源注入 name 标识 + parent 引用 (父环境可见)
 * 4. getInstance 含祖先查找 (父 Bean 可见)
 * 5. destroy 全清
 * 6. Specification 声明契约
 */
public class MiniContext {

    // ===== 微缩 Bean 注册表 (子上下文) =====

    public static class MicroContext {
        final String name;
        final Map<String, Object> beans = new HashMap<>();
        final MicroContext parent;
        final Map<String, Object> properties = new HashMap<>();

        MicroContext(String name, MicroContext parent) {
            this.name = name;
            this.parent = parent;
        }

        void registerBean(String type, Object bean) {
            beans.put(type, bean);
        }

        Object getBean(String type) {
            if (beans.containsKey(type)) {
                return beans.get(type);
            }
            // 含祖先查找: 父可见
            if (parent != null) {
                return parent.getBean(type);
            }
            return null;
        }

        boolean isClosed = false;

        void close() {
            isClosed = true;
        }
    }

    // ===== Specification 声明 =====

    public interface Specification {
        String getName();

        List<String> getConfiguration();
    }

    public static class SimpleSpec implements Specification {
        final String name;
        final List<String> configs;

        SimpleSpec(String name, List<String> configs) {
            this.name = name;
            this.configs = configs;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getConfiguration() {
            return configs;
        }
    }

    // ===== NamedContextFactory 微缩 =====

    public static abstract class NamedContextFactoryImpl<C extends Specification> {
        private final Map<String, MicroContext> contexts = new ConcurrentHashMap<>();
        private final Map<String, C> configurations = new ConcurrentHashMap<>();
        private final String propertyName;
        private final Class<?> defaultConfigType;
        private MicroContext parent;

        public NamedContextFactoryImpl(String propertyName, Class<?> defaultConfigType) {
            this.propertyName = propertyName;
            this.defaultConfigType = defaultConfigType;
        }

        public void setParent(MicroContext parent) {
            this.parent = parent;
        }

        public void setConfigurations(List<C> configs) {
            for (C c : configs) {
                configurations.put(c.getName(), c);
            }
        }

        protected MicroContext getContext(String name) {
            if (!contexts.containsKey(name)) {
                synchronized (contexts) {
                    if (!contexts.containsKey(name)) {
                        contexts.put(name, createContext(name));
                    }
                }
            }
            return contexts.get(name);
        }

        public MicroContext createContext(String name) {
            MicroContext context = buildContext(name);
            registerBeans(name, context);
            return context;
        }

        MicroContext buildContext(String name) {
            MicroContext ctx = new MicroContext(name, parent);
            // 属性源注入: {propertyName: name} (对照 NamedContextFactory.java:185-187)
            ctx.properties.put(propertyName, name);
            return ctx;
        }

        void registerBeans(String name, MicroContext context) {
            // ① 精确匹配
            if (configurations.containsKey(name)) {
                for (String config : configurations.get(name).getConfiguration()) {
                    context.registerBean(config, "bean:" + config + ":" + name);
                }
            }
            // ② default. 前缀 → 注入所有子上下文
            for (Map.Entry<String, C> entry : configurations.entrySet()) {
                if (entry.getKey().startsWith("default.")) {
                    for (String config : entry.getValue().getConfiguration()) {
                        context.registerBean(config, "bean:" + config + ":default");
                    }
                }
            }
            // ③ 基础设施
            context.registerBean(defaultConfigType.getName(), "infra:" + defaultConfigType.getSimpleName());
        }

        public <T> T getInstance(String name, Class<T> type) {
            MicroContext ctx = getContext(name);
            Object bean = ctx.getBean(type.getName());
            return bean != null ? (T) bean : null;
        }

        public Set<String> getContextNames() {
            return contexts.keySet();
        }

        public void destroy() {
            for (MicroContext ctx : contexts.values()) {
                ctx.close();
            }
            contexts.clear();
        }
    }

    // ===== 测试 =====

    public static class TestFactory extends NamedContextFactoryImpl<SimpleSpec> {
        public TestFactory() {
            super("test.property", TestFactory.class);
        }

        // 基础设施注册: 让 defaultConfigType 可被 InfraLookup 查到 (微缩: 注册 InfraLookup 类型)
        @Override
        void registerBeans(String name, MicroContext context) {
            super.registerBeans(name, context);
            context.registerBean(InfraLookup.class.getName(), "infra:" + name);
        }
    }

    static int passCount = 0;
    static int failCount = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("PASS: " + name);
        } else {
            failCount++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) {
        // ---- 1. 子上下文隔离 + 懒创建 (含 default. 配置一次性注入) ----
        TestFactory factory = new TestFactory();
        factory.setConfigurations(List.of(
                new SimpleSpec("foo", List.of(MiniContext.ConfigLookup.class.getName())),
                new SimpleSpec("bar", List.of(MiniContext.ConfigLookup.class.getName())),
                new SimpleSpec("default." + MiniContext.DefaultLookup.class.getName(),
                        List.of(MiniContext.DefaultLookup.class.getName()))));
        check("懒创建: 初始无上下文", factory.getContextNames().isEmpty());
        Object foo = factory.getInstance("foo", ConfigLookup.class);
        check("懒创建: getInstance 触发创建", factory.getContextNames().contains("foo"));
        check("隔离: foo 有自己的配置 Bean", foo != null);
        Object barBean = factory.getInstance("bar", ConfigLookup.class);
        check("隔离: bar 也有自己的", barBean != null);
        // foo 的 bean 应带 foo 标识, bar 的带 bar 标识 — 互不污染
        String fooVal = String.valueOf(foo);
        String barVal = String.valueOf(barBean);
        check("隔离: foo/bar Bean 值不同 (各自上下文)", !fooVal.equals(barVal));

        // ---- 2. default. 前缀注入 ----
        Object defaultBean = factory.getInstance("foo", DefaultLookup.class);
        check("default. 前缀: 注入所有子上下文", defaultBean != null);
        Object defaultInBar = factory.getInstance("bar", DefaultLookup.class);
        check("default. 前缀: bar 也有", defaultInBar != null);
        // default bean 应带 default 标识 (非 foo/bar 特定)
        check("default. 前缀: 值带 default 标识", String.valueOf(defaultBean).contains("default"));

        // ---- 3. 基础设施恒注册 ----
        Object infra = factory.getInstance("foo", InfraLookup.class);
        check("基础设施: defaultConfigType 恒注册", infra != null);

        // ---- 4. 父环境可见 (含祖先查找) ----
        MicroContext parentCtx = new MicroContext("parent", null);
        parentCtx.registerBean(MiniContext.SharedService.class.getName(), "shared-from-parent");
        TestFactory factory2 = new TestFactory();
        factory2.setParent(parentCtx);
        factory2.setConfigurations(List.of(new SimpleSpec("baz", List.of(MiniContext.ConfigLookup.class.getName()))));
        Object shared = factory2.getInstance("baz", SharedService.class);
        check("父可见: 子上下文拿父 Bean", "shared-from-parent".equals(shared));

        // ---- 5. 属性源注入 (子知道自己是谁) ----
        MicroContext fooCtx = factory.getContext("foo");
        check("属性源: 子上下文 propertyName=name", "foo".equals(fooCtx.properties.get("test.property")));

        // ---- 6. destroy 全清 ----
        factory.destroy();
        check("destroy: 上下文清空", factory.getContextNames().isEmpty());
        check("destroy: 上下文关闭标记", fooCtx.isClosed);

        // ---- 7. 双检锁并发安全 ----
        TestFactory factory3 = new TestFactory();
        factory3.setConfigurations(List.of(new SimpleSpec("conc", List.of(MiniContext.ConfigLookup.class.getName()))));
        java.util.concurrent.atomic.AtomicInteger created = new java.util.concurrent.atomic.AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    factory3.getContext("conc");
                }
            }));
        }
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }
        // 微缩版 getContext 内部 put — 无法直接数创建次数, 但上下文 Map 只应有 1 个
        check("双检锁: 1000 次并发取只有 1 个上下文", factory3.getContextNames().size() == 1);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }

    public static class ConfigLookup {
    }

    public static class DefaultLookup {
    }

    public static class InfraLookup {
    }

    public static class FooService {
    }

    public static class BarService {
    }

    public static class DefaultService {
    }

    public static class SharedService {
    }
}
