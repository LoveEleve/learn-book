import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniRegistry — 微缩版服务注册 (ServiceRegistry + AbstractAutoServiceRegistration)
 *
 * 覆盖机制:
 * 1. ServiceRegistry 5 方法契约 (register/deregister/close/setStatus/getStatus)
 * 2. AbstractAutoServiceRegistration: WebServerInitializedEvent → start() 触发链
 * 3. start() 仪式: isEnabled 守卫 + running 双检 + Pre/Registered 事件 + 钩子
 * 4. RegistrationLifecycle 4 钩子 (Ordered)
 * 5. stop() 对称注销链 + close()
 */
public class MiniRegistry {

    // ===== Registration 模型 =====

    public static class Registration {
        final String serviceId;
        final String host;
        final int port;

        Registration(String serviceId, String host, int port) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return serviceId + "@" + host + ":" + port;
        }
    }

    // ===== ServiceRegistry 接口 =====

    public interface ServiceRegistry<R extends Registration> {
        void register(R registration);

        void deregister(R registration);

        void close();

        void setStatus(R registration, String status);

        <T> T getStatus(R registration);
    }

    // ===== 内存注册中心 (测试用实现) =====

    public static class InMemoryServiceRegistry implements ServiceRegistry<Registration> {
        final List<Registration> registered = new ArrayList<>();
        final List<Registration> deregistered = new ArrayList<>();
        String status = "UNKNOWN";
        boolean closed = false;

        @Override
        public void register(Registration registration) {
            registered.add(registration);
        }

        @Override
        public void deregister(Registration registration) {
            deregistered.add(registration);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void setStatus(Registration registration, String status) {
            this.status = status;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getStatus(Registration registration) {
            return (T) status;
        }
    }

    // ===== RegistrationLifecycle 钩子 =====

    public interface RegistrationLifecycle<R extends Registration> {
        int DEFAULT_ORDER = 0;

        void postProcessBeforeStartRegister(R registration);

        void postProcessAfterStartRegister(R registration);

        void postProcessBeforeStopRegister(R registration);

        void postProcessAfterStopRegister(R registration);

        default int getOrder() {
            return DEFAULT_ORDER;
        }
    }

    public static class LoggingLifecycle implements RegistrationLifecycle<Registration> {
        final List<String> events = new ArrayList<>();

        @Override
        public void postProcessBeforeStartRegister(Registration registration) {
            events.add("beforeStart");
        }

        @Override
        public void postProcessAfterStartRegister(Registration registration) {
            events.add("afterStart");
        }

        @Override
        public void postProcessBeforeStopRegister(Registration registration) {
            events.add("beforeStop");
        }

        @Override
        public void postProcessAfterStopRegister(Registration registration) {
            events.add("afterStop");
        }
    }

    // ===== 事件模型 (微缩) =====

    public static class Event {
        final String type;

        Event(String type) {
            this.type = type;
        }
    }

    // ===== AbstractAutoServiceRegistration 微缩 =====

    public static abstract class AbstractAutoServiceRegistration<R extends Registration> {
        protected final ServiceRegistry<R> serviceRegistry;
        protected final List<RegistrationLifecycle<R>> registrationLifecycles = new ArrayList<>();
        protected final List<String> publishedEvents = new ArrayList<>();
        protected final AtomicBoolean running = new AtomicBoolean(false);
        protected final AtomicInteger port = new AtomicInteger(0);

        protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        public void addRegistrationLifecycle(RegistrationLifecycle<R> lifecycle) {
            this.registrationLifecycles.add(lifecycle);
        }

        public void onApplicationEvent(Event event) {
            // 微缩: WebServerInitializedEvent → 记录端口 + start
            this.port.compareAndSet(0, 8080);
            this.start();
        }

        public void start() {
            if (!isEnabled()) {
                return; // 守卫 1: isEnabled
            }
            if (!this.running.get()) { // 守卫 2: running 双检
                publishedEvents.add("InstancePreRegisteredEvent");
                registrationLifecycles.forEach(l -> l.postProcessBeforeStartRegister(getRegistration()));
                register();
                registrationLifecycles.forEach(l -> l.postProcessAfterStartRegister(getRegistration()));
                if (shouldRegisterManagement()) {
                    registerManagement();
                }
                publishedEvents.add("InstanceRegisteredEvent");
                this.running.compareAndSet(false, true);
            }
        }

        public void stop() {
            if (this.running.compareAndSet(true, false) && isEnabled()) {
                registrationLifecycles.forEach(l -> l.postProcessBeforeStopRegister(getRegistration()));
                deregister();
                registrationLifecycles.forEach(l -> l.postProcessAfterStopRegister(getRegistration()));
                if (shouldRegisterManagement()) {
                    deregisterManagement();
                }
                this.serviceRegistry.close();
            }
        }

        protected void register() {
            this.serviceRegistry.register(getRegistration());
        }

        protected void registerManagement() {
            R mgmt = getManagementRegistration();
            if (mgmt != null) {
                this.serviceRegistry.register(mgmt);
            }
        }

        protected void deregister() {
            this.serviceRegistry.deregister(getRegistration());
        }

        protected void deregisterManagement() {
            R mgmt = getManagementRegistration();
            if (mgmt != null) {
                this.serviceRegistry.deregister(mgmt);
            }
        }

        protected abstract R getRegistration();

        protected abstract R getManagementRegistration();

        protected abstract boolean isEnabled();

        protected abstract boolean shouldRegisterManagement();
    }

    // ===== 具体实现 (测试) =====

    public static class TestAutoRegistration extends AbstractAutoServiceRegistration<Registration> {
        private final boolean enabled;
        private final boolean registerMgmt;
        final Registration main = new Registration("app", "localhost", 8080);
        final Registration mgmt = new Registration("app:management", "localhost", 8081);

        TestAutoRegistration(ServiceRegistry<Registration> registry, boolean enabled, boolean registerMgmt) {
            super(registry);
            this.enabled = enabled;
            this.registerMgmt = registerMgmt;
        }

        @Override
        protected Registration getRegistration() {
            return main;
        }

        @Override
        protected Registration getManagementRegistration() {
            return registerMgmt ? mgmt : null;
        }

        @Override
        protected boolean isEnabled() {
            return enabled;
        }

        @Override
        protected boolean shouldRegisterManagement() {
            return registerMgmt;
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
        // ---- 1. ServiceRegistry 5 方法契约 ----
        InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
        Registration reg = new Registration("svc", "host", 8080);
        registry.register(reg);
        check("5 方法: register", registry.registered.size() == 1);
        registry.setStatus(reg, "UP");
        check("5 方法: setStatus/getStatus", "UP".equals(registry.getStatus(reg)));
        registry.deregister(reg);
        check("5 方法: deregister", registry.deregistered.size() == 1);
        registry.close();
        check("5 方法: close", registry.closed);

        // ---- 2. WebServerInitializedEvent → start 触发注册 ----
        InMemoryServiceRegistry registry2 = new InMemoryServiceRegistry();
        TestAutoRegistration auto = new TestAutoRegistration(registry2, true, false);
        auto.onApplicationEvent(new Event("WebServerInitializedEvent"));
        check("触发: WebServer 事件后注册", registry2.registered.size() == 1);
        check("触发: 注册的是主服务", registry2.registered.get(0).serviceId.equals("app"));
        check("触发: port 记录", auto.port.get() == 8080);
        check("触发: 事件发布顺序 (Pre 先于 Registered)",
                auto.publishedEvents.get(0).equals("InstancePreRegisteredEvent")
                        && auto.publishedEvents.get(1).equals("InstanceRegisteredEvent"));

        // ---- 3. running 双检: 重复事件不重复注册 ----
        auto.onApplicationEvent(new Event("WebServerInitializedEvent"));
        check("双检: 二次事件不重复注册", registry2.registered.size() == 1);

        // ---- 4. isEnabled 守卫 ----
        InMemoryServiceRegistry registry3 = new InMemoryServiceRegistry();
        TestAutoRegistration disabled = new TestAutoRegistration(registry3, false, false);
        disabled.onApplicationEvent(new Event("WebServerInitializedEvent"));
        check("守卫: isEnabled=false 不注册", registry3.registered.isEmpty());

        // ---- 5. 管理注册 ----
        InMemoryServiceRegistry registry4 = new InMemoryServiceRegistry();
        TestAutoRegistration withMgmt = new TestAutoRegistration(registry4, true, true);
        withMgmt.onApplicationEvent(new Event("WebServerInitializedEvent"));
        check("管理注册: 两个注册 (主+管理)", registry4.registered.size() == 2);
        check("管理注册: 管理服务名", registry4.registered.stream().anyMatch(r -> r.serviceId.equals("app:management")));

        // ---- 6. 钩子 4 事件 ----
        LoggingLifecycle lifecycle = new LoggingLifecycle();
        InMemoryServiceRegistry registry5 = new InMemoryServiceRegistry();
        TestAutoRegistration hooked = new TestAutoRegistration(registry5, true, false);
        hooked.addRegistrationLifecycle(lifecycle);
        hooked.onApplicationEvent(new Event("WebServerInitializedEvent"));
        check("钩子: 注册前/后", lifecycle.events.contains("beforeStart") && lifecycle.events.contains("afterStart"));
        hooked.stop();
        check("钩子: 注销前/后", lifecycle.events.contains("beforeStop") && lifecycle.events.contains("afterStop"));
        check("钩子: 顺序 (beforeStart 先)", lifecycle.events.indexOf("beforeStart") < lifecycle.events.indexOf("afterStart"));

        // ---- 7. stop 对称链 + close ----
        check("stop: deregister 被调", registry5.deregistered.size() == 1);
        check("stop: close 被调", registry5.closed);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
