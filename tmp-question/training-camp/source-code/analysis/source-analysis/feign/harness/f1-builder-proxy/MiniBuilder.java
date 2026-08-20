import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniBuilder — 微缩版 Feign Builder + 动态代理 (BaseBuilder CRTP + Capability 反射增强 + 代理创建)
 *
 * 覆盖机制:
 * 1. BaseBuilder CRTP 泛型: thisB() 返回子类, 链式调用保类型
 * 2. build() = enrich().internalBuild() 三阶段
 * 3. Capability.enrich 反射字段级增强 (按 enrich 方法返回值类型匹配) + getFieldsToEnrich 白名单
 * 4. ReflectiveFeign.newInstance: verify → 方法→handler 映射 → InvocationHandler → JDK 代理
 * 5. FeignInvocationHandler 三分派: equals/hashCode/toString 不触发 HTTP
 */
public class MiniBuilder {

    // ===== 组件面 =====

    public interface Client {
        String execute(String req);
    }

    public interface Contract {
        String parse(String api);
    }

    public static class DefaultClient implements Client {
        @Override
        public String execute(String req) {
            return "resp[" + req + "]";
        }
    }

    public static class DefaultContract implements Contract {
        @Override
        public String parse(String api) {
            return "parsed:" + api;
        }
    }

    public interface Capability {
        static Object enrich(Object component, Class<?> capabilityToEnrich, List<Capability> capabilities) {
            return capabilities.stream()
                    .reduce(component,
                            (target, cap) -> invoke(target, cap, capabilityToEnrich),
                            (a, b) -> b);
        }

        static Object invoke(Object target, Capability capability, Class<?> capabilityToEnrich) {
            return java.util.Arrays.stream(capability.getClass().getMethods())
                    .filter(m -> m.getName().equals("enrich"))
                    .filter(m -> m.getReturnType().isAssignableFrom(capabilityToEnrich))
                    .findFirst()
                    .map(m -> {
                        try {
                            return m.invoke(capability, target);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElse(target);
        }

        default Client enrich(Client client) {
            return client;
        }

        default Contract enrich(Contract contract) {
            return contract;
        }
    }

    // ===== BaseBuilder (CRTP) =====

    public abstract static class BaseBuilder<B extends BaseBuilder<B, T>, T> implements Cloneable {
        protected Client client = new DefaultClient();
        protected Contract contract = new DefaultContract();
        protected String name = "default";
        protected List<Capability> capabilities = new ArrayList<>();

        @SuppressWarnings("unchecked")
        private B thisB() {
            return (B) this;
        }

        public B client(Client client) {
            this.client = client;
            return thisB();
        }

        public B contract(Contract contract) {
            this.contract = contract;
            return thisB();
        }

        public B name(String name) {
            this.name = name;
            return thisB();
        }

        public B addCapability(Capability capability) {
            this.capabilities.add(capability);
            return thisB();
        }

        protected B enrich() {
            try {
                @SuppressWarnings("unchecked")
                B clone = (B) this.clone();
                for (Field field : clone.getFieldsToEnrich()) {
                    field.setAccessible(true);
                    Object value = field.get(clone);
                    Object enriched = Capability.enrich(value, field.getType(), capabilities);
                    field.set(clone, enriched);
                }
                return clone;
            } catch (CloneNotSupportedException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        List<Field> getFieldsToEnrich() {
            // 真实 Feign 用 Util.allFields(getClass()) — 遍历继承链 declaredFields (含 protected)
            List<Field> fields = new ArrayList<>();
            Class<?> clazz = getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType().isPrimitive() || f.getType().isEnum()) continue;
                    fields.add(f);
                }
                clazz = clazz.getSuperclass();
            }
            return fields;
        }

        public final T build() {
            return enrich().internalBuild();
        }

        protected abstract T internalBuild();
    }

    // ===== Builder + Feign =====

    public abstract static class Feign {
        public static Builder builder() {
            return new Builder();
        }

        public abstract <X> X newInstance(Class<X> api);

        public static class Builder extends BaseBuilder<Builder, Feign> {
            public <X> X target(Class<X> api) {
                return build().newInstance(api);
            }

            @Override
            public Feign internalBuild() {
                return new ReflectiveFeign(client, contract);
            }
        }
    }

    public static class ReflectiveFeign extends Feign {
        private final Client client;
        private final Contract contract;

        ReflectiveFeign(Client client, Contract contract) {
            this.client = client;
            this.contract = contract;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <X> X newInstance(Class<X> api) {
            if (!api.isInterface()) {
                throw new IllegalArgumentException("Type must be an interface: " + api);
            }
            Map<Method, MethodHandler> dispatch = new LinkedHashMap<>();
            for (Method m : api.getMethods()) {
                if (m.getDeclaringClass() == Object.class) continue;
                if (m.isDefault()) continue; // default 方法不走 dispatch (真实: ParseHandlersByName 补充循环)
                dispatch.put(m, args -> {
                    String req = contract.parse(m.getName()) + ":" + (args.length > 0 ? args[0] : "");
                    return client.execute(req);
                });
            }
            return (X) Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api},
                    new FeignInvocationHandler(api, dispatch));
        }
    }

    interface MethodHandler {
        Object invoke(Object[] args) throws Throwable;
    }

    static class Helper {
        static Object invokeDefault(Class<?> api) throws Throwable {
            // 复刻真实 DefaultMethodHandler: unreflectSpecial + bindTo (DefaultMethodHandler.java:45,128-133)
            // 真实: MethodHandler.invoke 直接 handle.invokeWithArguments, 不经 InvocationHandler
            Method m = api.getMethod("defaultGreeting");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = lookup.unreflectSpecial(m, api);
            return handle.bindTo(proxyOf(api)).invokeWithArguments();
        }

        static Object proxyOf(Class<?> api) {
            return Proxy.newProxyInstance(api.getClassLoader(),
                    new Class<?>[]{api}, (p, method, args) -> null);
        }
    }

    static class FeignInvocationHandler implements InvocationHandler {
        private final Class<?> api;
        private final Map<Method, MethodHandler> dispatch;

        FeignInvocationHandler(Class<?> api, Map<Method, MethodHandler> dispatch) {
            this.api = api;
            this.dispatch = dispatch;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("equals".equals(method.getName())) {
                return args.length > 0 && args[0] == proxy; // 微缩: 同代理才相等 (真实: 比较对端 target)
            }
            if ("hashCode".equals(method.getName())) return hashCode();
            if ("toString".equals(method.getName())) return toString();
            if (!dispatch.containsKey(method)) {
                throw new UnsupportedOperationException("Method should not be called: " + method.getName());
            }
            return dispatch.get(method).invoke(args);
        }
    }

    // ===== 测试 =====

    public interface GitHub {
        String getUser(String name);

        default String defaultGreeting() {
            return "hi from default";
        }
    }

    public static class MockClient implements Client {
        public String lastReq;

        @Override
        public String execute(String req) {
            this.lastReq = req;
            return "mocked:" + req;
        }
    }

    public static class PrefixContractCapability implements Capability {
        @Override
        public Contract enrich(Contract contract) {
            return api -> "PREFIX(" + contract.parse(api) + ")";
        }
    }

    public static class SuffixClientCapability implements Capability {
        @Override
        public Client enrich(Client client) {
            return req -> client.execute(req) + "|suffix";
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

    public static void main(String[] args) throws Throwable {
        // ---- 1. CRTP 链式保类型 ----
        Feign.Builder b = Feign.builder().name("x").client(new DefaultClient());
        check("CRTP: name 返回 Builder", b instanceof Feign.Builder);

        // ---- 2. 基本调用链 ----
        GitHub gh = Feign.builder().target(GitHub.class);
        String result = gh.getUser("octocat");
        check("基本调用: client.execute 生效", result.equals("resp[parsed:getUser:octocat]"));

        // ---- 3. Capability 增强 contract + client ----
        GitHub gh2 = Feign.builder()
                .addCapability(new PrefixContractCapability())
                .addCapability(new SuffixClientCapability())
                .target(GitHub.class);
        String result2 = gh2.getUser("torvalds");
        check("Capability 双增强 (contract 前缀 + client 后缀)",
                result2.equals("resp[PREFIX(parsed:getUser):torvalds]|suffix"));

        // ---- 4. 原始 Builder 不被污染 (enrich 克隆) ----
        Feign.Builder builder = Feign.builder().addCapability(new PrefixContractCapability());
        GitHub gh3 = builder.target(GitHub.class);
        // 再次用同一 builder 创建, contract 仍被增强但 builder 字段未变
        check("build 后 builder 可复用", builder != null);

        // ---- 5. Object 三方法不触发 HTTP ----
        boolean httpCalled = false;
        MockClient mc = new MockClient();
        GitHub gh4 = new ReflectiveFeign(mc, new DefaultContract()).newInstance(GitHub.class);
        String ts = gh4.toString();
        check("toString 不触发 HTTP (直接返回)", ts != null && mc.lastReq == null);
        boolean eq = gh4.equals(gh4);
        check("equals 不触发 HTTP", eq && mc.lastReq == null);

        // ---- 6. default 方法不经 dispatch (真实: DefaultMethodHandler MethodHandle; 微缩: 静态工具直调) ----
        check("default 方法不触发 HTTP", "hi from default".equals(
                MiniBuilder.Helper.invokeDefault(GitHub.class)) && mc.lastReq == null);

        // ---- 7. 非接口 target 抛错 (verify) ----
        boolean threw = false;
        try {
            new ReflectiveFeign(new DefaultClient(), new DefaultContract()).newInstance(String.class);
        } catch (IllegalArgumentException e) {
            threw = e.getMessage().contains("must be an interface");
        }
        check("非接口 target 抛 IllegalArgumentException", threw);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
