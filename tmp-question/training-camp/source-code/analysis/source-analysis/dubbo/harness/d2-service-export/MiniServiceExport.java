import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MiniServiceExport — D-2 服务导出核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 Dubbo 3.3.x 源码):
 *   A. export 链: export → doExportUrls → 1Protocol → doExportUrl (ServiceConfig.java:326-993)
 *   B. scope 三态决策: NONE/LOCAL/REMOTE → 本地/远程导出 (L650-690)
 *   C. openServer 缓存: serverMap 按地址 + 双检锁 + reset (DubboProtocol:377-405)
 *   D. exporterMap 注册: DubboExporter (invoker + serviceKey) (L346-375)
 *
 * 纯内存模拟, 保留核心决策数学与控制流。
 */
public class MiniServiceExport {

    enum Scope { NONE, LOCAL, REMOTE, BOTH }
    enum RegisterType { AUTO_REGISTER, MANUAL_REGISTER, NEVER_REGISTER }

    static class URL {
        final String protocol, address;
        final Map<String, String> params = new HashMap<>();
        URL(String protocol, String address) { this.protocol = protocol; this.address = address; }
    }

    // ---- B: scope 三态 ----
    static String scopeDecision(String scope) {
        if ("none".equalsIgnoreCase(scope)) return "NO_EXPORT";
        StringBuilder sb = new StringBuilder();
        if (!"remote".equalsIgnoreCase(scope)) sb.append("EXPORT_LOCAL;");   // injvm port 0
        if (!"local".equalsIgnoreCase(scope)) sb.append("EXPORT_REMOTE");    // registry
        return sb.toString();
    }

    // ---- C: openServer 缓存 ----
    static class ServerManager {
        final Map<String, Integer> serverMap = new ConcurrentHashMap<>();
        int bindCalls = 0;

        String openServer(URL url) {
            if (serverMap.containsKey(url.address)) {
                return "RESET:" + url.address; // server.reset (override)
            }
            synchronized (this) {
                if (!serverMap.containsKey(url.address)) {
                    serverMap.put(url.address, ++bindCalls); // createServer → Exchangers.bind
                    return "BIND:" + url.address;
                }
            }
            return "RESET:" + url.address;
        }
    }

    // ---- A/D: export 链 + exporter 注册 ----
    static class Protocol {
        final Map<String, Object> exporterMap = new ConcurrentHashMap<>();
        final List<Object> exporters = new CopyOnWriteArrayList<>();

        Object export(String serviceKey, Object invoker) {
            // DubboExporter: invoker + key + exporterMap
            exporterMap.put(serviceKey, invoker);
            exporters.add(invoker);
            return invoker;
        }
    }

    static class ExporterManager {
        final Map<String, RegisterType> registerTypes = new ConcurrentHashMap<>();
        void doExportUrl(Protocol protocol, String key, Object invoker, RegisterType rt) {
            protocol.export(key, invoker);
            registerTypes.put(key, rt);
        }
    }

    public static void main(String[] args) {
        // ============ B: scope 三态 ============
        assertTrue(scopeDecision("none").equals("NO_EXPORT"), "B1 scope=none → 不导出");
        assertTrue(scopeDecision("local").equals("EXPORT_LOCAL;"), "B2 scope=local → 仅本地 (injvm)");
        assertTrue(scopeDecision("remote").equals("EXPORT_REMOTE"), "B3 scope=remote → 仅远程");
        assertTrue(scopeDecision("both").equals("EXPORT_LOCAL;EXPORT_REMOTE"),
            "B4 默认双导出 (本地始终 + 远程按需)");
        System.out.println("[B] scope 三态 4/4 OK");

        // ============ C: openServer 缓存 ============
        ServerManager sm = new ServerManager();
        URL u1 = new URL("dubbo", "10.0.0.1:20880");
        URL u2 = new URL("dubbo", "10.0.0.2:20880");
        assertTrue(sm.openServer(u1).equals("BIND:10.0.0.1:20880"), "C1 首次 → createServer (Exchangers.bind)");
        assertTrue(sm.openServer(u1).equals("RESET:10.0.0.1:20880"), "C2 同地址复用 → server.reset (override)");
        assertTrue(sm.openServer(u2).equals("BIND:10.0.0.2:20880"), "C3 异地址 → 新 bind");
        assertTrue(sm.bindCalls == 2, "C4 同地址多服务共享 server (bind 仅 2 次)");
        System.out.println("[C] openServer 缓存 4/4 OK");

        // ============ A/D: export 链 + exporter 注册 ============
        Protocol protocol = new Protocol();
        ExporterManager em = new ExporterManager();
        Object invoker = new Object(); // proxyFactory.getInvoker(ref, interfaceClass, url)
        // doExportUrl: proxyFactory.getInvoker → protocolSPI.export(invoker)
        em.doExportUrl(protocol, "org.demo.UserService", invoker, RegisterType.AUTO_REGISTER);
        em.doExportUrl(protocol, "org.demo.OrderService", invoker, RegisterType.AUTO_REGISTER);
        assertTrue(protocol.exporterMap.size() == 2, "A1 exporterMap 按 serviceKey 索引 2 服务");
        assertTrue(protocol.exporters.size() == 2, "A2 exporters 收集 2 个");
        assertTrue(em.registerTypes.get("org.demo.UserService") == RegisterType.AUTO_REGISTER,
            "A3 registerType 分组 (AUTO)");
        // registerType 修正: REGISTER_KEY=false → MANUAL
        em.doExportUrl(protocol, "org.demo.InternalService", invoker, RegisterType.MANUAL_REGISTER);
        assertTrue(protocol.exporterMap.size() == 3, "A4 三服务导出完成");
        System.out.println("[A/D] export 链 + exporter 注册 4/4 OK");

        System.out.println("MiniServiceExport 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
