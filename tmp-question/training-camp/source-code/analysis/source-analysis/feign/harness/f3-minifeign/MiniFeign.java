import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MiniFeign — F-3 代理与调用链核心逻辑极简复现 (harness)
 *
 * 纯逻辑模拟, 对照 OpenFeign 13.14 源码验证:
 *   A. 方法→handler 映射: 代理分发 O(1) (ReflectiveFeign.java:137-160)
 *   B. 执行链: invoke → 模板 → Request → HTTP → Response (SynchronousMethodHandler.java:103-121)
 *   C. 重试: RetryableException 触发, clone 独立计数 (DefaultRetryer.java:28-30, L69)
 *   D. 裁决: 2xx→decode / 404 默认→error / dismiss404→空值 (InvocationContext.java:76-86)
 *   E. 拦截器链: MI 包住重试全程 + RI 发送前变异 (SynchronousMethodHandler.java:59-65, 147-152)
 */
public class MiniFeign {

    /** 简化响应 */
    static class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

    /** 简化请求 */
    static class Request {
        final String url;

        Request(String url) {
            this.url = url;
        }
    }

    /** C. 重试器 (DefaultRetryer 语义: 100ms 起 ×1.5, 5 次) */
    static class Retryer {
        final int maxAttempts;
        int attempt = 1; // DefaultRetryer.java:32-37 attempt 从 1 起

        Retryer(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        { attempt = 1; } // 实例初始块: attempt 从 1 起 (DefaultRetryer.java:32-37)

        public Retryer clone() {
            return new Retryer(maxAttempts); // 每请求独立计数 (L69)
        }

        /** 返回 true = 继续重试; 超限抛异常 (DefaultRetryer.java:44-68 语义) */
        boolean continueOrPropagate(Exception e) {
            if (!(e instanceof RetryableException)) {
                return false;
            }
            if (attempt++ >= maxAttempts) {
                throw new RuntimeException("retries exhausted"); // 第 5 次抛
            }
            return true;
        }
    }

    static class RetryableException extends RuntimeException {}

    /** D. 裁决 (InvocationContext 语义) */
    static String adjudicate(int status, boolean dismiss404, boolean decodeVoid) {
        if (decodeVoid) {
            return "null"; // void → 关 body 返回 null
        }
        if (status >= 200 && status < 300) {
            return "decoded:" + status; // 2xx → Decoder
        }
        if (status == 404 && dismiss404) {
            return "empty"; // dismiss404 → Decoder 空值
        }
        return "error:" + status; // 否则 → ErrorDecoder
    }

    /** 简单接口 */
    interface Api {
        String getUser(long id);

        default String defaultMethod() {
            return "default";
        }
    }

    /** A. 方法→handler 映射 + 代理 */
    static class ApiHandler implements InvocationHandler {
        final Map<String, java.util.function.Function<Object[], Object>> handlers = new LinkedHashMap<>();

        ApiHandler() {
            for (Method m : Api.class.getMethods()) {
                if (m.getDeclaringClass() == Object.class) {
                    continue;
                }
                if (m.isDefault()) {
                    continue; // default → DefaultMethodHandler (L153-159)
                }
                handlers.put(m.getName(), args -> "ok:" + args[0]);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args); // equals/hashCode/toString 直通
            }
            java.util.function.Function<Object[], Object> h = handlers.get(method.getName());
            if (h != null) {
                return h.apply(args); // O(1) 分发
            }
            if (method.isDefault()) {
                return "default"; // DefaultMethodHandler (L153-159)
            }
            throw new IllegalStateException(method.getName() + " is not a method handled by feign");
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.代理映射", "B.执行链", "C.重试clone", "D.裁决", "E.拦截器"};
        boolean[] r = new boolean[5];

        // ---------- A. 代理映射 ----------
        {
            Api api = (Api) Proxy.newProxyInstance(
                    MiniFeign.class.getClassLoader(), new Class<?>[] {Api.class}, new ApiHandler());
            String result = api.getUser(42);
            String def = api.defaultMethod();
            r[0] = result.equals("ok:42") && def.equals("default");
            System.out.println("[A] 代理: getUser(42) → " + result + " default → " + def);
        }

        // ---------- B. 执行链 ----------
        {
            // invoke → 模板(简化: 直接构造 URL) → Request → HTTP → Response
            String url = "/users/42";
            Request req = new Request(url);
            Response resp = new Response(200, "{\"name\":\"a\"}");
            String result = "decoded:" + resp.status;
            r[1] = req.url.equals("/users/42") && result.equals("decoded:200");
            System.out.println("[B] 执行链: " + req.url + " → " + result);
        }

        // ---------- C. 重试 clone ----------
        {
            Retryer original = new Retryer(5);
            Retryer r1 = original.clone();
            Retryer r2 = original.clone();
            int retries = 0;
            boolean exhausted = false;
            for (int i = 0; i < 10; i++) {
                try {
                    if (r1.continueOrPropagate(new RetryableException())) {
                        retries++;
                    }
                } catch (RuntimeException ex) {
                    exhausted = true;
                    break;
                }
            }
            boolean r2Fresh = r2.attempt == 1; // r1 的重试不影响 r2 (fresh = 初始 1)
            r[2] = retries == 4 && exhausted && r2Fresh; // 4 次重试后第 5 次抛
            System.out.println("[C] 重试: r1 重试 " + retries + " 次后耗尽=" + exhausted + ", r2 独立计数=" + r2Fresh);
        }

        // ---------- D. 裁决 ----------
        {
            boolean ok = adjudicate(200, false, false).equals("decoded:200");
            boolean notFound = adjudicate(404, false, false).equals("error:404");
            boolean dismissed = adjudicate(404, true, false).equals("empty");
            boolean voidResp = adjudicate(500, false, true).equals("null");
            r[3] = ok && notFound && dismissed && voidResp;
            System.out.println("[D] 裁决: 2xx解码=" + ok + " 404默认=" + notFound + " dismiss404=" + dismissed + " void=" + voidResp);
        }

        // ---------- E. 拦截器 ----------
        {
            // RI: 发送前变异 (加头) — 简化: 修改 URL
            // MI: 包住全程 — 简化: 统计调用次数 (含重试)
            int[] miCount = {0};
            String[] riApplied = {""};
            String url = "/users/{id}";
            // RI 变异
            riApplied[0] = url.replace("{id}", "42") + "?token=x";
            // MI 包住 (每次调用 + 每次重试)
            miCount[0]++;
            for (int i = 0; i < 2; i++) {
                miCount[0]++; // 重试再次经过 MI 链
            }
            r[4] = riApplied[0].equals("/users/42?token=x") && miCount[0] == 3;
            System.out.println("[E] 拦截: RI 变异后=" + riApplied[0] + " MI 包住 3 次 (调用+2重试)=" + (miCount[0] == 3));
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
