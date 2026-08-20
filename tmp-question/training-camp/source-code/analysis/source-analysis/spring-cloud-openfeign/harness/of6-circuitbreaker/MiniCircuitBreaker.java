import java.util.*;

public class MiniCircuitBreaker {

    // ---- A: Targeter 三分支 ----
    static class Target { final String type; Target(String t) { type = t; } public String toString() { return type; } }

    static class CircuitBreakerTargeter {
        static String target(boolean isCbBuilder, String contextId, String name,
                             Class<?> fallback, Class<?> fallbackFactory) {
            if (!isCbBuilder) return "feign.target(普通)"; // L48-49
            String cbName = contextId != null && !contextId.isEmpty() ? contextId : name; // L52 contextId 优先
            if (fallback != void.class) return "targetWithFallback(" + cbName + ")";       // L54-56
            if (fallbackFactory != void.class) return "targetWithFallbackFactory(" + cbName + ")"; // L58-60
            return "target(" + cbName + ", 无降级)";
        }
    }

    // ---- B: invoke 核心 (create + run + 降级) ----
    interface CircuitBreaker {
        String run(Runnable supplier, Runnable fallback);
        default String run(Runnable supplier) { return "FAIL(no fallback)"; } // L117 run(supplier)
    }
    static class Resilience4J implements CircuitBreaker {
        boolean open = false;
        public String run(Runnable supplier, Runnable fallback) {
            if (open) { fallback.run(); return "FALLBACK(熔断降级)"; }
            try {
                supplier.run();
                return "SUCCESS";
            } catch (RuntimeException e) {
                fallback.run(); // circuitBreaker.run 捕获异常 → 降级函数
                return "FALLBACK(异常降级)";
            }
        }
        @Override
        public String run(Runnable supplier) {
            supplier.run(); // 无降级: 直执行, 失败抛 (L117 run(supplier))
            return "FAIL(no fallback)";
        }
    }

    static class InvocationHandler {
        static String invoke(Resilience4J cb, boolean hasFallback, boolean fails) {
            String name = "payment-service"; // resolveCircuitBreakerName
            if (hasFallback) {
                return cb.run(() -> { if (fails) throw new RuntimeException("net error"); }, // supplier
                        () -> { /* fallbackMethodMap.invoke */ });
            }
            return cb.run(() -> { if (fails) throw new RuntimeException("net error"); }); // run(supplier) 无降级 L117
        }
    }

    // ---- C: 异步线程上下文 ----
    static class RequestContextHolder {
        static final ThreadLocal<String> CTX = new ThreadLocal<>();
        static String get() { return CTX.get(); }
        static void set(String v) { CTX.set(v); }
    }

    static class AsSupplier {
        static String invoke() {
            String ctx = RequestContextHolder.get();          // L136 保存
            Thread caller = Thread.currentThread();
            // 模拟熔断线程池切换
            final String[] result = new String[1];
            Thread cbThread = new Thread(() -> {
                boolean isAsync = caller != Thread.currentThread(); // L140
                if (isAsync) RequestContextHolder.set(ctx);         // L142-143 恢复
                result[0] = "traceId=" + RequestContextHolder.get();
            });
            cbThread.start();
            try { cbThread.join(); } catch (InterruptedException ignored) {}
            return result[0];
        }
    }

    // ---- D: 命名与开关 ----
    static class NameResolver {
        static String defaultResolve(String name, String method) { return name + "#" + method; }
        static String alphanumeric(String name) { return name.replaceAll("[^a-zA-Z0-9]", ""); } // 字母数字清洗
    }

    static class DisabledConditions {
        static boolean disabled(boolean classMissing, boolean propDisabled) { return classMissing || propDisabled; }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 三分支 ============
        System.out.println("A1 3way   : " + CircuitBreakerTargeter.target(true, "ctx-1", "svc", void.class, void.class));
        System.out.println("A2 fallback: " + CircuitBreakerTargeter.target(true, "ctx-1", "svc", Object.class, void.class));
        System.out.println("A3 factory : " + CircuitBreakerTargeter.target(true, "ctx-1", "svc", void.class, Object.class));
        System.out.println("A4 normal : " + CircuitBreakerTargeter.target(false, "ctx-1", "svc", Object.class, void.class));

        // ============ B: invoke ============
        Resilience4J cb = new Resilience4J();
        System.out.println("B1 ok     : " + InvocationHandler.invoke(cb, true, false));
        cb.open = true;
        System.out.println("B2 cb     : " + InvocationHandler.invoke(cb, true, true));
        cb.open = false;
        try { InvocationHandler.invoke(cb, false, true); } catch (RuntimeException e) {
            System.out.println("B3 no-fb  : " + e.getMessage() + " (无降级 run(supplier) 直抛)");
        }

        // ============ C: 异步上下文 ============
        RequestContextHolder.set("trace-abc");
        System.out.println("C1 async  : " + AsSupplier.invoke() + " (线程切换后恢复)");

        // ============ D: 命名与开关 ============
        System.out.println("D1 name   : " + NameResolver.defaultResolve("payment", "orders") + " → alnum=" + NameResolver.alphanumeric("pay-ment_1"));
        System.out.println("D2 cond   : classMissing=" + DisabledConditions.disabled(true, false) + ", prop=" + DisabledConditions.disabled(false, true));

        // 断言
        pass += CircuitBreakerTargeter.target(true, "ctx-1", "svc", void.class, void.class).contains("无降级") ? 1 : 0;
        Resilience4J cb2 = new Resilience4J(); // 新实例避免状态污染
        pass += InvocationHandler.invoke(cb2, true, true).contains("FALLBACK") ? 1 : 0; // 异常降级
        pass += AsSupplier.invoke().contains("trace-abc") ? 1 : 0; // 异步上下文恢复
        pass += NameResolver.alphanumeric("pay-ment_1").equals("payment1") ? 1 : 0;
        pass += DisabledConditions.disabled(true, false) && DisabledConditions.disabled(false, true) ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
