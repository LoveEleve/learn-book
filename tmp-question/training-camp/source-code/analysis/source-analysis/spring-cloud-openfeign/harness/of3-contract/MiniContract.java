import java.util.*;

public class MiniContract {

    // ---- A: 注解面 (类级禁止 + 组合注解 + GET 默认) ----
    static class RequestTemplate {
        String method = "GET"; String path; String accept, contentType;
        final Map<String, String> headers = new LinkedHashMap<>();
        final List<String> queryParams = new ArrayList<>();
        Integer queryMapIndex = null;
        public String toString() { return method + " " + path + " accept=" + accept + " ct=" + contentType; }
    }

    static class Contract {
        static RequestTemplate process(String classAnn, String methodAnn, String methodValue) {
            if (classAnn != null) {
                throw new IllegalArgumentException("@RequestMapping annotation not allowed on @FeignClient interfaces"); // L231
            }
            RequestTemplate t = new RequestTemplate();
            if (methodAnn == null && !methodAnn_matches(methodAnn)) return t;
            // 组合注解: @GetMapping → method=GET (缺省)
            t.method = "GET"; // methods 空 → 默认 GET (L302-305)
            t.path = methodValue; // path 解析
            return t;
        }
        static boolean methodAnn_matches(String a) { return a != null; }
    }

    // ---- B: 请求面 (produces/consumes/headers) ----
    static class FourParse {
        static void apply(RequestTemplate t, String produces, String consumes, String[] headers) {
            if (produces != null) t.accept = produces;            // parseProduces → ACCEPT
            if (consumes != null) t.contentType = consumes;       // parseConsumes → CONTENT_TYPE
            if (headers != null) {
                for (String h : headers) {
                    int idx = h.indexOf('=');
                    if (!h.contains("!=") && idx >= 0) {
                        t.headers.put(h.substring(0, idx), h.substring(idx + 1).trim()); // parseHeaders
                    }
                }
            }
        }
    }

    // ---- C: 参数面 (注册表分发 + Pageable + GET 警告) ----
    interface Processor { boolean processArgument(String annotation); }
    static class PathVariableProcessor implements Processor {
        public boolean processArgument(String annotation) { return "PathVariable".equals(annotation); }
    }
    static class RequestParamProcessor implements Processor {
        public boolean processArgument(String annotation) { return "RequestParam".equals(annotation); }
    }

    static class ParameterRegistry {
        static final Map<String, Processor> REGISTRY = new HashMap<>();
        static {
            // 7 处理器 (穷举): MatrixVariable/PathVariable/RequestParam/RequestHeader/CookieValue/QueryMap/RequestPart
            REGISTRY.put("PathVariable", new PathVariableProcessor());
            REGISTRY.put("RequestParam", new RequestParamProcessor());
        }
        static boolean dispatch(String annotation) {
            Processor p = REGISTRY.get(annotation);
            return p != null && p.processArgument(annotation);
        }
    }

    static class GetWarning {
        static String warn(String methodSig) {
            return "[OpenFeign Warning] " + methodSig + " is declared as GET with parameters, but none of the parameters are annotated... may result in fallback to POST at runtime.";
        }
    }

    // ---- D: Pageable 特判 + SpringQueryMap ----
    static class PageableParam { int page, size; }

    static class ParamProcessor {
        static void process(RequestTemplate t, Object param, boolean hasQueryMap) {
            if (param instanceof PageableParam && !hasQueryMap) {
                t.queryMapIndex = 0; // queryMapIndex(paramIndex) — OF-4 分页关联
            }
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 类级禁止 + 组合注解 ============
        try {
            Contract.process("@RequestMapping", "@GetMapping", "/users");
        } catch (IllegalArgumentException e) {
            System.out.println("A1 ban    : " + e.getMessage());
        }
        RequestTemplate t = Contract.process(null, "@GetMapping", "/users/{id}");
        System.out.println("A2 combo  : " + t.method + " " + t.path + " (组合注解 → GET 默认)");

        // ============ B: 四解析 ============
        FourParse.apply(t, "application/json", "application/json", new String[]{"X-Trace=abc", "X-Ignore!=1"});
        System.out.println("B1 parse  : accept=" + t.accept + ", ct=" + t.contentType + ", headers=" + t.headers);

        // ============ C: 注册表 + GET 警告 ============
        System.out.println("C1 reg    : PathVariable=" + ParameterRegistry.dispatch("PathVariable") + ", Unknown=" + ParameterRegistry.dispatch("Unknown"));
        System.out.println("C2 warn   : " + GetWarning.warn("OrderClient#findUsers").substring(0, 60) + "...");

        // ============ D: Pageable → queryMapIndex ============
        RequestTemplate t2 = new RequestTemplate();
        ParamProcessor.process(t2, new PageableParam(), false);
        System.out.println("D1 page   : queryMapIndex=" + t2.queryMapIndex + " (分页展开)");
        RequestTemplate t3 = new RequestTemplate();
        ParamProcessor.process(t3, new PageableParam(), true);
        System.out.println("D2 conflict: queryMapIndex=" + t3.queryMapIndex + " (已有 QueryMap 不覆盖)");

        // 断言
        pass += t.method.equals("GET") && t.path.equals("/users/{id}") ? 1 : 0; // A
        pass += t.accept.equals("application/json") && t.headers.containsKey("X-Trace") && !t.headers.containsKey("X-Ignore") ? 1 : 0; // B
        pass += ParameterRegistry.dispatch("PathVariable") && !ParameterRegistry.dispatch("Unknown") ? 1 : 0; // C
        pass += GetWarning.warn("x").contains("fallback to POST") ? 1 : 0; // C
        pass += t2.queryMapIndex != null && t3.queryMapIndex == null ? 1 : 0; // D
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
