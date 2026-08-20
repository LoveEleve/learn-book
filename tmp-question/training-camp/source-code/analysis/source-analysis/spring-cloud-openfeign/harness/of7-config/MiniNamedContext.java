import java.util.*;

public class MiniNamedContext {

    // ---- A: 子上下文 (惰性创建 + 规范注册 + 组件获取) ----
    static class ChildContext {
        final String name;
        final Map<String, Object> beans = new HashMap<>();
        ChildContext(String name, Map<String, Object> defaults) { this.name = name; beans.putAll(defaults); }
        Object getBean(Class<?> type) {
            return beans.values().stream().filter(b -> type.isInstance(b)).findFirst().orElse(null);
        }
    }

    static class NamedContextFactory {
        final Map<String, ChildContext> contexts = new HashMap<>();
        final Map<String, Object> defaults;
        NamedContextFactory(Map<String, Object> defaults) { this.defaults = defaults; }
        ChildContext getContext(String name) {
            return contexts.computeIfAbsent(name, n -> new ChildContext(n, defaults)); // 惰性创建 L119
        }
        Object getInstance(String name, Class<?> type) { return getContext(name).getBean(type); } // L253
        Object getInstanceWithoutAncestors(String name, Class<?> type) { // OF-2 继承开关 false 时
            ChildContext c = contexts.get(name);
            return c != null ? c.getBean(type) : null;
        }
    }

    // ---- B: 默认组件 (Builder 三态 + 熔断条件) ----
    static class Builder { String kind; Builder(String k) { kind = k; } public String toString() { return kind; } }

    static class FeignClientsConfiguration {
        static final Map<String, Object> DEFAULTS = new HashMap<>();
        static {
            DEFAULTS.put("feignBuilder", new Builder("feignBuilder"));
            DEFAULTS.put("defaultFeignBuilder", new Builder("defaultFeignBuilder"));
        }
        static boolean circuitBreakerFactoryPresent = false;
        static Builder getBuilder(String name) {
            if ("circuitBreakerFeignBuilder".equals(name)) {
                return circuitBreakerFactoryPresent ? new Builder("circuitBreakerFeignBuilder") : null; // @ConditionalOnBean L230-232
            }
            return (Builder) DEFAULTS.get(name);
        }
    }

    // ---- C: 配置面 (defaultConfig + config + 覆盖顺序) ----
    static class FeignClientConfiguration { Integer connectTimeout; String retryer; }

    static class FeignClientProperties {
        String defaultConfig = "default";
        Map<String, FeignClientConfiguration> config = new HashMap<>();
        boolean defaultToProperties = true; // L52 默认 true

        FeignClientConfiguration merged(String contextId) {
            FeignClientConfiguration merged = new FeignClientConfiguration();
            FeignClientConfiguration def = config.get(defaultConfig);
            FeignClientConfiguration ctx = config.get(contextId);
            if (def != null) merged.connectTimeout = def.connectTimeout; // 默认级
            if (ctx != null) {
                if (ctx.connectTimeout != null) merged.connectTimeout = ctx.connectTimeout; // 客户端覆盖
                merged.retryer = ctx.retryer;
            }
            return merged;
        }
    }

    // ---- D: 继承开关 ----
    static class FeignClientConfigurer {
        static boolean inheritParentConfiguration() { return true; } // 默认 true
    }

    static class FactoryBean {
        boolean inheritParentContext;
        void configureFeign(FeignClientProperties props, boolean isDefaultToProperties) {
            inheritParentContext = FeignClientConfigurer.inheritParentConfiguration(); // L172-173
            String order = isDefaultToProperties ? "Properties后覆盖" : "Configuration后覆盖"; // L174-188
            System.out.println("D1 order  : " + order + ", inherit=" + inheritParentContext);
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 子上下文 ============
        NamedContextFactory factory = new NamedContextFactory(FeignClientsConfiguration.DEFAULTS);
        ChildContext c1 = factory.getContext("payment-service"); // 惰性创建
        ChildContext c2 = factory.getContext("payment-service"); // 复用
        System.out.println("A1 lazy   : " + (c1 == c2) + " (同 contextId 复用)");
        System.out.println("A2 bean   : " + factory.getInstance("payment-service", Builder.class));

        // ============ B: 熔断 Builder 条件 ============
        System.out.println("B1 no-cf  : " + FeignClientsConfiguration.getBuilder("circuitBreakerFeignBuilder") + " (无工厂→null)");
        FeignClientsConfiguration.circuitBreakerFactoryPresent = true;
        System.out.println("B2 with-cf: " + FeignClientsConfiguration.getBuilder("circuitBreakerFeignBuilder") + " (有工厂→提供)");

        // ============ C: 双级配置 ============
        FeignClientProperties props = new FeignClientProperties();
        FeignClientConfiguration def = new FeignClientConfiguration();
        def.connectTimeout = 1000;
        FeignClientConfiguration ctx = new FeignClientConfiguration();
        ctx.connectTimeout = 5000;
        ctx.retryer = "custom-retryer";
        props.config.put("default", def);
        props.config.put("payment-service", ctx);
        FeignClientConfiguration merged = props.merged("payment-service");
        System.out.println("C1 merged : connect=" + merged.connectTimeout + " (客户端覆盖默认), retryer=" + merged.retryer);

        // ============ D: 继承开关 ============
        new FactoryBean().configureFeign(props, props.defaultToProperties);

        // 断言
        pass += factory.getInstance("payment-service", Builder.class) != null ? 1 : 0;
        pass += FeignClientsConfiguration.getBuilder("circuitBreakerFeignBuilder") != null ? 1 : 0; // B2
        pass += merged.connectTimeout == 5000 ? 1 : 0; // C 客户端覆盖
        pass += merged.retryer.equals("custom-retryer") ? 1 : 0;
        pass += FeignClientConfigurer.inheritParentConfiguration() ? 1 : 0; // D 默认 true
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
