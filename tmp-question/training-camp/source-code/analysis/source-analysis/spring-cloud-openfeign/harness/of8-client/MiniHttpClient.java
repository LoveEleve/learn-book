import java.util.*;

public class MiniHttpClient {

    // ---- A: HttpClient5 连接池 (默认值 + Builder) ----
    static class Hc5Properties {
        int maxConnTotal = 200;          // DEFAULT_MAX_CONNECTIONS
        int maxConnPerRoute = 50;        // DEFAULT_MAX_CONNECTIONS_PER_ROUTE
        boolean disableSslValidation = false; // DEFAULT_DISABLE_SSL_VALIDATION
        boolean followRedirects = true;  // DEFAULT_FOLLOW_REDIRECTS
        int connectionTimeout = 2000;    // DEFAULT_CONNECTION_TIMEOUT
    }

    static class PoolingConnectionManager {
        final Hc5Properties props;
        PoolingConnectionManager(Hc5Properties p) { props = p; }
        String describe() {
            return "pool(total=" + props.maxConnTotal + ", perRoute=" + props.maxConnPerRoute
                    + ", sslCheck=" + !props.disableSslValidation + ")";
        }
    }

    // ---- B: Http2 (Redirect 配置) ----
    static class Http2Builder {
        static String redirect(boolean follow) { return follow ? "Redirect.ALWAYS" : "Redirect.NEVER"; }
    }

    // ---- C: 请求压缩 (双条件) ----
    static class ContentGzip {
        static final List<String> MIME_TYPES = Arrays.asList("text/xml", "application/xml", "application/json"); // L36
        static final int MIN_REQUEST_SIZE = 2048; // L41
        static boolean requiresCompression(String contentType, long contentLength) {
            return MIME_TYPES.contains(contentType) && contentLength > MIN_REQUEST_SIZE; // 双条件 L60-73
        }
    }

    // ---- D: 响应解压 (Accept-Encoding + 开关 + OkHttp 特判) ----
    static class AcceptGzip {
        static boolean responseEnabled = true; // spring.cloud.openfeign.compression.response.enabled
        static boolean okHttpPresent = false;  // OkHttpFeignClientBeanMissingCondition
        static String intercept() {
            if (!responseEnabled) return "(disabled)";
            if (okHttpPresent) return "(skip: OkHttp 自带解压)"; // 不叠加
            return "Accept-Encoding: gzip"; // 协商响应压缩
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 连接池 ============
        Hc5Properties props = new Hc5Properties();
        System.out.println("A1 pool   : " + new PoolingConnectionManager(props).describe() + " (默认 200/50)");
        props.maxConnTotal = 500;
        props.disableSslValidation = true;
        System.out.println("A2 tune   : " + new PoolingConnectionManager(props).describe());

        // ============ B: Http2 ============
        System.out.println("B1 h2     : " + Http2Builder.redirect(true) + " vs " + Http2Builder.redirect(false));

        // ============ C: 请求压缩 ============
        System.out.println("C1 small  : " + ContentGzip.requiresCompression("application/json", 1000) + " (小请求不压)");
        System.out.println("C2 big    : " + ContentGzip.requiresCompression("application/json", 5000) + " (大 JSON 压)");
        System.out.println("C3 bin    : " + ContentGzip.requiresCompression("application/octet-stream", 9000) + " (二进制不压)");

        // ============ D: 响应解压 ============
        System.out.println("D1 gzip   : " + AcceptGzip.intercept());
        AcceptGzip.okHttpPresent = true;
        System.out.println("D2 okhttp : " + AcceptGzip.intercept() + " (OkHttp 特判)");

        // 断言
        pass += new PoolingConnectionManager(new Hc5Properties()).describe().contains("200") ? 1 : 0; // A 默认
        pass += Http2Builder.redirect(true).equals("Redirect.ALWAYS") ? 1 : 0; // B
        pass += !ContentGzip.requiresCompression("application/json", 1000) ? 1 : 0; // C 小不压
        pass += ContentGzip.requiresCompression("application/json", 5000) && !ContentGzip.requiresCompression("application/octet-stream", 9000) ? 1 : 0; // C 双条件
        pass += AcceptGzip.intercept().contains("skip") ? 1 : 0; // D OkHttp 特判
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
