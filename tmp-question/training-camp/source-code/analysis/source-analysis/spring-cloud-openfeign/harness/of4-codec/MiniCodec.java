import java.util.*;

public class MiniCodec {

    // ---- A: 编码面 (canWrite 判定 + converter 选择) ----
    interface Converter {
        String name();
        boolean canWrite(Class<?> type, String contentType);
        String encode(Object body);
    }
    static class JacksonConverter implements Converter {
        public String name() { return "Jackson"; }
        public boolean canWrite(Class<?> type, String ct) { return ct == null || ct.contains("json"); }
        public String encode(Object body) { return "{\"v\":\"" + body + "\"}"; }
    }
    static class ByteArrayConverter implements Converter {
        public String name() { return "ByteArray"; }
        public boolean canWrite(Class<?> type, String ct) { return type == byte[].class; }
        public String encode(Object body) { return "BYTES"; }
    }

    static class SpringEncoder {
        static final List<Converter> CONVERTERS = Arrays.asList(new JacksonConverter(), new ByteArrayConverter());
        static String encode(Object body, String contentType) {
            for (Converter c : CONVERTERS) {
                if (c.canWrite(body.getClass(), contentType)) {
                    return c.name() + ":" + c.encode(body); // checkAndWrite → canWrite 判定
                }
            }
            throw new RuntimeException("no suitable HttpMessageConverter found for request type [" + body.getClass() + "]");
        }
    }

    // ---- B: 解码面 (类型白名单) ----
    static class SpringDecoder {
        static Object decode(Class<?> type, String body) {
            if (type != Class.class && !(type instanceof Class)) {
                throw new RuntimeException("type is not an instance of Class or ParameterizedType");
            }
            return body; // HttpMessageConverterExtractor 简化
        }
    }

    // ---- C: 兼容面 (ResponseEntity 三分支) ----
    static class ResponseEntityDecoder {
        static String decode(boolean isParameterizedEntity, boolean isEntity, String delegateResult) {
            if (isParameterizedEntity) return "ResponseEntity(" + delegateResult + ")"; // createResponse(decoded)
            if (isEntity) return "ResponseEntity(null)";                               // createResponse(null)
            return delegateResult;                                                     // delegate 透传
        }
    }

    // ---- D: 分页面 (page/size query + SimplePageImpl) ----
    static class Pageable { int page, size; Pageable(int p, int s) { page = p; size = s; } }

    static class PageableSpringEncoder {
        static Map<String, String> encode(Object obj, Map<String, String> query) {
            if (obj instanceof Pageable p) {
                query.put("page", String.valueOf(p.page));   // template.query(pageParameter, ...)
                query.put("size", String.valueOf(p.size));
            }
            return query; // 非 Pageable 走 delegate fallback
        }
        static boolean supports(Object obj) { return obj instanceof Pageable; } // L135-136: Pageable || Sort
    }

    static class SimplePageImpl {
        final List<String> content; final int page, size;
        SimplePageImpl(List<String> content, int page, int size) { this.content = content; this.page = page; this.size = size; }
        public String toString() { return "Page(content=" + content + ", page=" + page + ", size=" + size + ")"; }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 编码面 ============
        System.out.println("A1 enc    : " + SpringEncoder.encode("hello", "application/json"));
        try { SpringEncoder.encode(new Object(), "text/plain"); } catch (RuntimeException e) {
            System.out.println("A2 noConv : " + e.getMessage().substring(0, 40) + "...");
        }

        // ============ B: 解码面 ============
        System.out.println("B1 dec    : " + SpringDecoder.decode(String.class, "resp-body"));

        // ============ C: 三分支 ============
        System.out.println("C1 entity : " + ResponseEntityDecoder.decode(true, true, "decoded"));
        System.out.println("C2 plain  : " + ResponseEntityDecoder.decode(false, false, "decoded"));

        // ============ D: 分页 ============
        Map<String, String> query = new LinkedHashMap<>();
        PageableSpringEncoder.encode(new Pageable(2, 10), query);
        System.out.println("D1 page   : " + query + ", supports=" + PageableSpringEncoder.supports(new Pageable(0, 5)));
        System.out.println("D2 resp   : " + new SimplePageImpl(Arrays.asList("a", "b"), 0, 10));

        // 断言
        pass += SpringEncoder.encode("hello", "application/json").startsWith("Jackson") ? 1 : 0; // A
        pass += SpringDecoder.decode(String.class, "x").equals("x") ? 1 : 0; // B
        pass += ResponseEntityDecoder.decode(true, true, "d").equals("ResponseEntity(d)") ? 1 : 0; // C
        pass += query.get("page").equals("2") && query.get("size").equals("10") ? 1 : 0; // D 请求分页
        pass += PageableSpringEncoder.supports(new Pageable(0, 5)) ? 1 : 0; // D supports
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
