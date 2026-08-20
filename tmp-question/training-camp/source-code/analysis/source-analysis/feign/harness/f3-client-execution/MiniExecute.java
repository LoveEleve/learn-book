import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniExecute — 微缩版 Feign 执行链 (SynchronousMethodHandler + Retryer + 三层拦截)
 *
 * 覆盖机制:
 * 1. MethodInterceptor 柯里化链 (andThen/apply) + 短路能力
 * 2. RequestInterceptor 线性 apply (改模板)
 * 3. runWithRetry: Retryer.clone() 状态隔离 + RetryableException 重试循环
 * 4. DefaultRetryer 退避数学: period=100, maxPeriod=1000, maxAttempts=5; attempt++ >= max 抛
 *    退避序列 150/225/337/506ms (1.5 指数), 累计 150/375/712/1218 — 对照 RetryerTest L32-55
 * 5. executeAndDecode: targetRequest → client.execute → response 回填 request
 */
public class MiniExecute {

    // ===== RetryableException + Retryer =====

    public static class RetryableException extends RuntimeException {
        private final Long retryAfter;

        public RetryableException(String message, Long retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }

        public Long retryAfter() {
            return retryAfter;
        }
    }

    public interface Retryer {
        void continueOrPropagate(RetryableException e);

        Retryer clone();
    }

    public static class DefaultRetryer implements Retryer {
        private final int maxAttempts;
        private final long period;
        private final long maxPeriod;
        int attempt = 1;
        long sleptForMillis = 0;
        long now = 0; // 可注入时钟 (对照真实 currentTimeMillis 可覆写)

        public DefaultRetryer() {
            this(100, 1000, 5);
        }

        public DefaultRetryer(long period, long maxPeriod, int maxAttempts) {
            this.period = period;
            this.maxPeriod = maxPeriod;
            this.maxAttempts = maxAttempts;
        }

        @Override
        public void continueOrPropagate(RetryableException e) {
            if (attempt++ >= maxAttempts) {
                throw e;
            }
            long interval;
            if (e.retryAfter() != null) {
                interval = e.retryAfter() - now;
                if (interval > maxPeriod) {
                    interval = maxPeriod;
                }
                if (interval < 0) {
                    return;
                }
            } else {
                interval = nextMaxInterval();
            }
            // 真实实现 Thread.sleep; 这里只累计不睡
            sleptForMillis += interval;
        }

        long nextMaxInterval() {
            long interval = (long) (period * Math.pow(1.5, attempt - 1));
            return Math.min(interval, maxPeriod);
        }

        @Override
        public Retryer clone() {
            return new DefaultRetryer(period, maxPeriod, maxAttempts);
        }
    }

    // ===== 三层拦截 =====

    public static class Invocation {
        public String requestLine = "GET /";
        public String log = "";
    }

    public interface MethodInterceptor {
        Object intercept(Invocation inv, Chain chain) throws Throwable;

        default Chain apply(Chain chain) {
            return inv -> intercept(inv, chain);
        }

        default MethodInterceptor andThen(MethodInterceptor next) {
            return (inv, chain) -> intercept(inv, nxt -> next.intercept(nxt, chain));
        }

        interface Chain {
            Object next(Invocation inv) throws Throwable;
        }
    }

    public interface RequestInterceptor {
        void apply(Invocation inv);
    }

    // ===== 执行核心 =====

    public static class Executor {
        private final Retryer retryer;
        private final List<RequestInterceptor> requestInterceptors;
        private final List<MethodInterceptor> methodInterceptors;
        private final Client client;
        private final boolean failFirst2; // 模拟前 2 次抛 RetryableException

        public Executor(Retryer retryer,
                        List<RequestInterceptor> requestInterceptors,
                        List<MethodInterceptor> methodInterceptors,
                        Client client,
                        boolean failFirst2) {
            this.retryer = retryer;
            this.requestInterceptors = requestInterceptors;
            this.methodInterceptors = methodInterceptors;
            this.client = client;
            this.failFirst2 = failFirst2;
        }

        public Object invoke(Invocation invocation) throws Throwable {
            MethodInterceptor.Chain endOfChain = inv -> runWithRetry(inv);
            MethodInterceptor.Chain chain = methodInterceptors.stream()
                    .reduce(MethodInterceptor::andThen)
                    .map(interceptor -> interceptor.apply(endOfChain))
                    .orElse(endOfChain);
            return chain.next(invocation);
        }

        private Object runWithRetry(Invocation invocation) throws Throwable {
            Retryer r = retryer.clone();
            while (true) {
                try {
                    return executeAndDecode(invocation);
                } catch (RetryableException e) {
                    r.continueOrPropagate(e); // 会抛当 attempt 用尽
                }
            }
        }

        Object executeAndDecode(Invocation invocation) {
            // RequestInterceptor 线性改模板 (真实: RequestTemplate 副本; 微缩: 直接改字段)
            for (RequestInterceptor ri : requestInterceptors) {
                ri.apply(invocation);
            }
            // Target.apply 定型 (真实: Target.apply(template)); 微缩: 消费已改路径
            String request = invocation.requestLine + " [auth]";
            invocation.log += "> " + request + "\n";
            String response = client.execute(request);
            invocation.log += "< " + response + "\n";
            return "decoded:" + response;
        }
    }

    public interface Client {
        String execute(String request);
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

    public static void main(String[] args) throws Throwable {
        // ---- 1. Retryer 退避数学 (对照 RetryerTest L32-55) ----
        DefaultRetryer retryer = new DefaultRetryer();
        RetryableException e = new RetryableException("conn refused", null);
        check("初始 attempt=1", retryer.attempt == 1);
        retryer.continueOrPropagate(e);
        check("第1次: attempt=2 slept=150", retryer.attempt == 2 && retryer.sleptForMillis == 150);
        retryer.continueOrPropagate(e);
        check("第2次: attempt=3 slept=375", retryer.attempt == 3 && retryer.sleptForMillis == 375);
        retryer.continueOrPropagate(e);
        check("第3次: attempt=4 slept=712", retryer.attempt == 4 && retryer.sleptForMillis == 712);
        retryer.continueOrPropagate(e);
        check("第4次: attempt=5 slept=1218", retryer.attempt == 5 && retryer.sleptForMillis == 1218);
        boolean threw = false;
        try {
            retryer.continueOrPropagate(e);
        } catch (RetryableException ex) {
            threw = true;
        }
        check("第5次调用抛 (5 次尝试/4 次重试)", threw);

        // ---- 2. retryAfter 优先 (对照 RetryerTest considersRetryAfter) ----
        DefaultRetryer r2 = new DefaultRetryer();
        r2.now = 1000;
        RetryableException ra = new RetryableException("503", 2000L);
        r2.continueOrPropagate(ra);
        check("retryAfter 优先: slept=1000 (2000-1000)", r2.sleptForMillis == 1000);

        // ---- 3. 三层拦截链: Method 短路 / Request 线性 / 重试 ----
        List<MethodInterceptor> methodInterceptors = new ArrayList<>();
        methodInterceptors.add((inv, chain) -> {
            inv.log += "MI1[before]\n";
            Object result = chain.next(inv);
            inv.log += "MI1[after]\n";
            return result;
        });
        methodInterceptors.add((inv, chain) -> {
            inv.log += "MI2[before]\n";
            Object result = chain.next(inv);
            inv.log += "MI2[after]\n";
            return result;
        });
        // 短路拦截器: 不调 next
        List<MethodInterceptor> shortCircuit = new ArrayList<>();
        shortCircuit.add((inv, chain) -> "short-circuited!");

        // 幂等拦截器: 只在路径开头加一次 /auth (真实 Feign 要求 RequestInterceptor 幂等, 因重试会重新 apply)
        List<RequestInterceptor> requestInterceptors = new ArrayList<>();
        requestInterceptors.add(inv -> {
            if (!inv.requestLine.startsWith("GET /auth")) {
                inv.requestLine = inv.requestLine.replace("GET /", "GET /auth/");
            }
        });

        AtomicInteger attempts = new AtomicInteger();
        Client failingClient = request -> {
            attempts.incrementAndGet();
            if (attempts.get() <= 2) {
                throw new RetryableException("fail " + attempts.get(), null);
            }
            return "OK(" + attempts.get() + ")";
        };

        Executor exec = new Executor(
                new DefaultRetryer(), requestInterceptors, methodInterceptors, failingClient, false);

        Invocation inv = new Invocation();
        Object result = exec.invoke(inv);
        check("重试后成功 (3 次尝试)", result.equals("decoded:OK(3)"));
        check("RequestInterceptor 幂等改了路径", inv.requestLine.equals("GET /auth/"));
        check("Method 拦截器包裹 (MI1 before 出现)", inv.log.contains("MI1[before]"));
        check("MI1 after 在 MI2 after 外", inv.log.indexOf("MI1[after]") > inv.log.indexOf("MI2[after]"));

        // ---- 4. 短路 ----
        Executor shortExec = new Executor(
                new DefaultRetryer(), new ArrayList<>(), shortCircuit,
                request -> "SHOULD NOT REACH", false);
        Object shortResult = shortExec.invoke(new Invocation());
        check("Method 拦截器短路 (client 未调用)", shortResult.equals("short-circuited!"));

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
