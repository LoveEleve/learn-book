import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniMetadata {

    // ---- A: MetadataService 自描述 ----
    static class MetadataService {
        static String getServiceDefinition(String iface, String group, String version) {
            return "DEF(" + iface + "/" + group + "/" + version + ": methods=[sayHello])";
        }
    }

    // ---- B: 发布面 (publishServiceDefinition — 双端 + 可禁用) ----
    static class MetadataReport {
        static boolean shouldReportDefinition = true;
        static final List<String> stored = new ArrayList<>();
        static void storeProviderMetadata(String serviceKey, String def) { stored.add(serviceKey + "=" + def); }
    }

    static class MetadataUtils {
        static void publishServiceDefinition(String side, String serviceKey) {
            if (!MetadataReport.shouldReportDefinition) return; // 可禁用 (无元数据中心降级)
            MetadataReport.storeProviderMetadata(serviceKey, "FullServiceDefinition");
        }
    }

    // ---- C: 应用级模型 — MetadataInfo + revision MD5 ----
    static class MetadataInfo {
        final String appName;
        final Map<String, String> services = new ConcurrentHashMap<>();
        volatile String revision = "0"; // 变更检测版本
        MetadataInfo(String app) { this.appName = app; }
        void addService(String key, String info) { services.put(key, info); refreshRevision(); }
        void refreshRevision() { revision = calRevision(services.toString()); } // MD5
        static String calRevision(String metadata) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                return new String(md.digest(metadata.getBytes()));
            } catch (Exception e) { return "0"; }
        }
    }

    // ---- D: 发现 + 迁移三态 ----
    enum MigrationStep { FORCE_INTERFACE, APPLICATION_FIRST, FORCE_APPLICATION }

    static class ServiceDiscoveryRegistry {
        static String register(String appName, String address) { return "APP_INSTANCE(" + appName + "@" + address + ")"; }
    }

    static class MigrationInvoker {
        static MigrationStep step = MigrationStep.APPLICATION_FIRST; // 默认共存
        static String decide() {
            switch (step) {
                case FORCE_INTERFACE:   return "INTERFACE_INVOKER (2.x)";
                case FORCE_APPLICATION: return "APPLICATION_INVOKER (3.x, " + ServiceDiscoveryRegistry.register("demo", "10.0.0.1:20880") + ")";
                default:                return "BOTH (双订阅共存, 平滑迁移)";
            }
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 自描述 ============
        System.out.println("A1 def    : " + MetadataService.getServiceDefinition("HelloService", "g1", "1.0"));

        // ============ B: 发布面 ============
        MetadataUtils.publishServiceDefinition("provider", "HelloService:1.0");
        System.out.println("B1 pub    : " + MetadataReport.stored);
        MetadataReport.shouldReportDefinition = false; // 无元数据中心降级
        MetadataUtils.publishServiceDefinition("provider", "SkipService");
        System.out.println("B2 skip   : stored=" + MetadataReport.stored.size() + " (禁用跳过)");
        MetadataReport.shouldReportDefinition = true;

        // ============ C: revision MD5 变更检测 ============
        MetadataInfo info = new MetadataInfo("demo-app");
        info.addService("HelloService", "v1");
        String rev1 = info.revision;
        info.addService("UserService", "v2"); // 元数据变化
        String rev2 = info.revision;
        System.out.println("C1 rev    : " + (rev1.equals(rev2) ? "不变" : "变化!") + " (MD5 指纹)");

        // ============ D: 迁移三态 ============
        System.out.println("D1 migrate: " + MigrationInvoker.decide());
        MigrationInvoker.step = MigrationStep.FORCE_APPLICATION;
        System.out.println("D2 force  : " + MigrationInvoker.decide());

        // 断言
        pass += MetadataService.getServiceDefinition("HelloService", "g1", "1.0").contains("sayHello") ? 1 : 0;
        pass += MetadataReport.stored.size() == 1 ? 1 : 0; // B2 禁用跳过
        pass += !rev1.equals(rev2) ? 1 : 0; // C revision 变化
        pass += MigrationInvoker.decide().contains("APPLICATION_INVOKER") ? 1 : 0;
        pass += MetadataInfo.calRevision("x").length() > 0 ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
