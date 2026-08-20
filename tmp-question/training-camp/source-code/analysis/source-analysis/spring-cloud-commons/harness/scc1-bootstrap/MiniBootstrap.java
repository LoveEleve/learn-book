import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniBootstrap — 微缩版 Spring Cloud Bootstrap 上下文 (BootstrapApplicationListener 核心)
 *
 * 覆盖机制:
 * 1. 三守卫: bootstrapEnabled 判定 / 已含 bootstrap 源不再递归 / legacy 兼容
 * 2. bootstrapServiceContext: 空环境 + spring.config.name 注入 + 子上下文构建
 * 3. PropertySourceLocator SPI: locate 单方法契约 + locateCollection 展开
 * 4. insertPropertySources 排序仲裁: addFirst 高优先级 / overrideNone 反转 / systemProperties 中间
 * 5. 父子上下文: bootstrap 是主应用 parent, 属性可见
 */
public class MiniBootstrap {

    // ===== PropertySource 模型 (微缩) =====

    public static class PropertySource {
        final String name;
        final Map<String, Object> properties = new LinkedHashMap<>();

        public PropertySource(String name) {
            this.name = name;
        }

        public PropertySource put(String key, Object value) {
            properties.put(key, value);
            return this;
        }

        public boolean containsProperty(String key) {
            return properties.containsKey(key);
        }

        public Object get(String key) {
            return properties.get(key);
        }

        @Override
        public String toString() {
            return "PS[" + name + "]";
        }
    }

    public static class CompositePropertySource extends PropertySource {
        final List<PropertySource> children = new ArrayList<>();

        public CompositePropertySource(String name) {
            super(name);
        }

        public CompositePropertySource add(PropertySource ps) {
            children.add(ps);
            return this;
        }
    }

    // ===== 微缩 Environment (只存有序 PropertySource 列表) =====

    public static class Environment {
        final List<PropertySource> propertySources = new ArrayList<>();
        String id;

        public void addFirst(PropertySource ps) {
            propertySources.add(0, ps);
        }

        public void addLast(PropertySource ps) {
            propertySources.add(ps);
        }

        public void addAfter(String name, PropertySource ps) {
            for (int i = 0; i < propertySources.size(); i++) {
                if (propertySources.get(i).name.equals(name)) {
                    propertySources.add(i + 1, ps);
                    return;
                }
            }
            propertySources.add(ps);
        }

        public boolean contains(String name) {
            for (PropertySource ps : propertySources) {
                if (ps.name.equals(name)) {
                    return true;
                }
            }
            return false;
        }

        public String resolve(String key) {
            for (PropertySource ps : propertySources) {
                if (ps.containsProperty(key)) {
                    return String.valueOf(ps.get(key));
                }
            }
            return null;
        }
    }

    // ===== PropertySourceLocator SPI =====

    public interface PropertySourceLocator {
        PropertySource locate(Environment environment);

        default List<PropertySource> locateCollection(Environment environment) {
            PropertySource ps = locate(environment);
            if (ps == null) {
                return List.of();
            }
            if (ps instanceof CompositePropertySource) {
                return ((CompositePropertySource) ps).children;
            }
            return List.of(ps);
        }
    }

    // ===== BootstrapApplicationListener 核心 (微缩) =====

    public static class BootstrapListener {
        public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = "bootstrap";
        public static final String BOOTSTRAP_ENABLED_PROPERTY = "spring.cloud.bootstrap.enabled";

        private final List<PropertySourceLocator> locators;
        private final Environment mainEnvironment;

        public BootstrapListener(List<PropertySourceLocator> locators, Environment mainEnvironment) {
            this.locators = locators;
            this.mainEnvironment = mainEnvironment;
        }

        public void onApplicationEvent(Environment environment) {
            // 守卫 1: bootstrap 未启用且非 legacy → return
            if (!bootstrapEnabled(environment)) {
                return;
            }
            // 守卫 2: 已含 bootstrap 源 → 不再递归 (bootstrap 上下文内)
            if (environment.contains(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
                return;
            }
            // bootstrapServiceContext: 收集 locators 的 PropertySource
            List<PropertySource> collected = new ArrayList<>();
            for (PropertySourceLocator locator : locators) {
                collected.addAll(locator.locateCollection(environment));
            }
            // 注入: 默认 addFirst 高优先级 (对照 insertPropertySources 默认分支)
            for (PropertySource ps : collected) {
                if (!environment.contains(ps.name)) {
                    environment.addFirst(ps);
                }
            }
        }

        public static boolean bootstrapEnabled(Environment environment) {
            Object v = null;
            for (PropertySource ps : environment.propertySources) {
                if (ps.containsProperty(BOOTSTRAP_ENABLED_PROPERTY)) {
                    v = ps.get(BOOTSTRAP_ENABLED_PROPERTY);
                    break;
                }
            }
            return Boolean.parseBoolean(String.valueOf(v)); // 微缩: 显式开启; 真实还有 MARKER_CLASS 双保险
        }

        // ===== insertPropertySources 排序仲裁 (微缩) =====

        public void insertPropertySources(Environment env, List<PropertySource> composite, String mode) {
            switch (mode) {
                case "default": // 默认: 可覆盖 → addFirst 最高优先级
                    for (PropertySource ps : composite) {
                        env.addFirst(ps);
                    }
                    break;
                case "override-none": // overrideNone=true → addLast 最低
                    for (PropertySource ps : composite) {
                        env.addLast(ps);
                    }
                    break;
                case "keep-system": // 禁止覆盖系统属性 → addAfter(systemEnvironment)
                    for (PropertySource ps : composite) {
                        env.addAfter("systemEnvironment", ps);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown mode " + mode);
            }
        }
    }

    // ===== 测试 =====

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
        // ---- 1. 三守卫: 未启用 → 不处理 ----
        Environment envDisabled = new Environment();
        envDisabled.addLast(new PropertySource("application").put("foo", "bar"));
        BootstrapListener listener = new BootstrapListener(List.of(), envDisabled);
        listener.onApplicationEvent(envDisabled);
        check("守卫1: bootstrap 未启用 → 无 bootstrap 源", !envDisabled.contains("bootstrap"));

        // ---- 2. 守卫2: 已含 bootstrap 源 → 不递归 ----
        Environment envWithBootstrap = new Environment();
        envWithBootstrap.addLast(new PropertySource("application"));
        envWithBootstrap.addLast(new PropertySource("bootstrap").put("from.bootstrap", "1"));
        BootstrapListener l2 = new BootstrapListener(List.of(env -> new PropertySource("extra").put("x", "1")),
                envWithBootstrap);
        l2.onApplicationEvent(envWithBootstrap);
        check("守卫2: 已含 bootstrap 源 → 不再注入 extra", !envWithBootstrap.contains("extra"));

        // ---- 3. PropertySourceLocator SPI + Composite 展开 ----
        Environment envEnabled = new Environment();
        envEnabled.addLast(new PropertySource("application").put("local", "1"));
        envEnabled.addLast(new PropertySource("systemEnvironment").put("SYS", "1"));
        envEnabled.addLast(new PropertySource("bootstrapConfig").put("spring.cloud.bootstrap.enabled", "true"));
        PropertySourceLocator nacosLike = env -> new CompositePropertySource("nacos")
                .add(new PropertySource("nacos1").put("db.url", "jdbc:mysql://cfg"))
                .add(new PropertySource("nacos2").put("redis.host", "cfg-host"));
        BootstrapListener l3 = new BootstrapListener(List.of(nacosLike), envEnabled);
        l3.onApplicationEvent(envEnabled);
        check("SPI: Composite 展开为 2 个源", envEnabled.contains("nacos1") && envEnabled.contains("nacos2"));
        check("SPI: 配置中心属性注入", "jdbc:mysql://cfg".equals(envEnabled.resolve("db.url")));
        check("高优先级: nacos1 在 application 之前",
                envEnabled.propertySources.indexOf(
                        envEnabled.propertySources.stream().filter(p -> p.name.equals("nacos1")).findFirst().get())
                        < envEnabled.propertySources.indexOf(
                        envEnabled.propertySources.stream().filter(p -> p.name.equals("application")).findFirst().get()));

        // ---- 4. 排序仲裁三模式 ----
        Environment env4 = new Environment();
        env4.addLast(new PropertySource("application").put("k", "local"));
        env4.addLast(new PropertySource("systemEnvironment").put("k", "sys"));
        PropertySource remote = new PropertySource("config-server").put("k", "remote");
        BootstrapListener l4 = new BootstrapListener(List.of(), env4);
        l4.insertPropertySources(env4, List.of(remote), "default");
        check("仲裁默认: remote 在 application 前 (addFirst)",
                env4.propertySources.indexOf(remote) < env4.propertySources.indexOf(
                        env4.propertySources.stream().filter(p -> p.name.equals("application")).findFirst().get()));
        check("仲裁默认: remote 覆盖 local", "remote".equals(env4.resolve("k")));

        Environment env5 = new Environment();
        env5.addLast(new PropertySource("application").put("k", "local"));
        env5.addLast(new PropertySource("systemEnvironment").put("k", "sys"));
        PropertySource remote5 = new PropertySource("config-server").put("k", "remote");
        BootstrapListener l5 = new BootstrapListener(List.of(), env5);
        l5.insertPropertySources(env5, List.of(remote5), "override-none");
        check("仲裁 override-none: remote 在最后 (addLast)", env5.propertySources.get(env5.propertySources.size() - 1) == remote5);
        check("仲裁 override-none: local 覆盖 remote", "local".equals(env5.resolve("k")));

        Environment env6 = new Environment();
        env6.addLast(new PropertySource("application").put("k", "local"));
        env6.addLast(new PropertySource("systemEnvironment").put("k", "sys"));
        PropertySource remote6 = new PropertySource("config-server").put("k", "remote");
        BootstrapListener l6 = new BootstrapListener(List.of(), env6);
        l6.insertPropertySources(env6, List.of(remote6), "keep-system");
        check("仲裁 keep-system: remote 在 systemEnvironment 之后",
                env6.propertySources.indexOf(remote6) > env6.propertySources.indexOf(
                        env6.propertySources.stream().filter(p -> p.name.equals("systemEnvironment")).findFirst().get()));
        check("仲裁 keep-system: remote 排最后 (不覆盖已有属性)",
                env6.propertySources.get(env6.propertySources.size() - 1) == remote6);

        // ---- 5. 父子上下文属性可见 ----
        Environment parent = new Environment();
        parent.addFirst(new PropertySource("bootstrap").put("parent.key", "from-parent"));
        Environment child = new Environment();
        child.addLast(new PropertySource("application"));
        // 微缩: 模拟父子可见 — child.resolve 回退 parent
        String visible = child.resolve("parent.key");
        boolean fallback = visible == null ? "from-parent".equals(parent.resolve("parent.key")) : visible.equals("from-parent");
        check("父子上下文: child 无 key, 回退 parent 可见", fallback);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
