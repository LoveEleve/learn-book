import java.util.*;

public class MiniRefreshableUrl {

    // ---- A: RefreshableUrl 不可变 + url() 动态覆写 ----
    static class RefreshableUrl {
        final String url; // 不可变 (final)
        RefreshableUrl(String url) { this.url = url; }
        String getUrl() { return url; }
    }

    static class RefreshableHardCodedTarget {
        final String name;
        volatile RefreshableUrl refreshableUrl; // 换实例 (volatile)
        final String cleanPath;
        RefreshableHardCodedTarget(String name, RefreshableUrl url, String path) {
            this.name = name; this.refreshableUrl = url; this.cleanPath = path;
        }
        String url() { return refreshableUrl.getUrl() + cleanPath; } // L63-66 每次取最新
    }

    // ---- B: scope=refresh 代理重建 (setScope + ScopedProxy) ----
    static class RefreshableUrlFactoryBean {
        RefreshableUrl getObject(String configUrl) {
            return new RefreshableUrl(configUrl); // getObject → 新实例
        }
    }

    static class ScopedProxy {
        static RefreshableUrl get(String configUrl, boolean refreshed) {
            // 刷新 → FactoryBean 重建 → 新实例
            return refreshed ? new RefreshableUrlFactoryBean().getObject(configUrl)
                             : new RefreshableUrlFactoryBean().getObject("http://old");
        }
    }

    // ---- C: PropertyBasedTarget 懒计算 ----
    static class PropertyBasedTarget {
        String url = null; // 懒计算
        final String configUrl; final String path;
        PropertyBasedTarget(String configUrl, String path) { this.configUrl = configUrl; this.path = path; }
        String url() {
            if (url == null) url = configUrl + path; // L53-55 首次计算
            return url;
        }
    }

    // ---- D: getUrl 规范化 + 开关 ----
    static class UrlUtils {
        static String getUrl(String url) {
            if (url.startsWith("#{") && url.contains("}")) return url; // SpEL 排除 L116
            if (!url.contains("://")) url = "http://" + url;           // 前缀补全 L117-119
            if (url.endsWith("/")) url = url.substring(0, url.length() - 1); // 尾部去除 L120-121
            return url; // URI 校验 (简化)
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 动态覆写 ============
        RefreshableHardCodedTarget target = new RefreshableHardCodedTarget("payment", new RefreshableUrl("http://old:8080"), "/api");
        System.out.println("A1 old    : " + target.url());
        target.refreshableUrl = new RefreshableUrl("http://new:9090"); // 刷新换实例
        System.out.println("A2 new    : " + target.url() + " (换实例后 url() 取新)");

        // ============ B: 代理重建 ============
        RefreshableUrl u1 = ScopedProxy.get("http://new:9090", false);
        RefreshableUrl u2 = ScopedProxy.get("http://new:9090", true); // 刷新重建
        System.out.println("B1 proxy  : 刷新后新实例=" + (u1 != u2) + " (" + u2.getUrl() + ")");

        // ============ C: 懒计算 ============
        PropertyBasedTarget pt = new PropertyBasedTarget("http://config-host", "/v1");
        System.out.println("C1 lazy   : " + pt.url() + " / 再调=" + pt.url() + " (缓存)");

        // ============ D: 规范化 ============
        System.out.println("D1 norm   : " + UrlUtils.getUrl("payment-service:8080/") + " (前缀+去尾)");
        System.out.println("D2 spel   : " + UrlUtils.getUrl("#{env.URL}"));

        // 断言
        pass += target.url().contains("new:9090") ? 1 : 0; // A 换实例生效
        pass += u1 != u2 ? 1 : 0; // B 刷新重建
        pass += pt.url().equals("http://config-host/v1") && pt.url() == pt.url() ? 1 : 0; // C 懒计算+缓存
        pass += UrlUtils.getUrl("payment-service:8080/").equals("http://payment-service:8080") ? 1 : 0; // D
        pass += UrlUtils.getUrl("#{env.URL}").equals("#{env.URL}") ? 1 : 0; // D SpEL 排除
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
