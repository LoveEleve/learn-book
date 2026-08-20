import java.util.*;

public class MiniRegistry {

    static class URL {
        final String protocol, address, iface;
        final Map<String, String> params = new HashMap<>();
        URL(String protocol, String address, String iface) { this.protocol = protocol; this.address = address; this.iface = iface; }
        String category() { return params.getOrDefault("category", "providers"); }
        public String toString() { return protocol + "://" + address + "/" + iface; }
        public boolean equals(Object o) { return o instanceof URL && toString().equals(o.toString()); }
        public int hashCode() { return toString().hashCode(); }
    }

    interface NotifyListener { void notify(List<URL> urls); }

    // ---- A: 抽象面 — RegistryService 五方法契约 + AbstractRegistry 本地缓存 ----
    static abstract class AbstractRegistry {
        final List<URL> registered = new ArrayList<>();
        final Map<URL, List<NotifyListener>> subscribers = new HashMap<>();
        List<URL> localCache = new ArrayList<>(); // 本地文件缓存 (saveProperties/loadProperties)

        void register(URL url) { registered.add(url); }
        void unregister(URL url) { registered.remove(url); }
        void subscribe(URL url, NotifyListener l) {
            subscribers.computeIfAbsent(url, k -> new ArrayList<>()).add(l);
            // 首次通知阻塞: 用缓存兜底
            if (!localCache.isEmpty()) { l.notify(new ArrayList<>(localCache)); }
        }
        void unsubscribe(URL url, NotifyListener l) {
            List<NotifyListener> ls = subscribers.get(url);
            if (ls != null) ls.remove(l);
        }
        List<URL> lookup(URL url) { return new ArrayList<>(registered); }
        void saveProperties() { localCache = new ArrayList<>(registered); } // 通知后落盘
        List<URL> loadProperties() { return new ArrayList<>(localCache); }  // 抖动回退
    }

    // ---- B: 失败重试 — FailbackRegistry (失败任务 + 无限重试) ----
    static abstract class FailbackRegistry extends AbstractRegistry {
        final Queue<Runnable> failedTasks = new ArrayDeque<>();
        boolean connected = true;

        @Override public void register(URL url) {
            try { doRegister(url); registered.add(url); }
            catch (RuntimeException e) { failedTasks.add(() -> register(url)); } // FailedRegisteredTask
        }
        @Override public void subscribe(URL url, NotifyListener l) {
            try { doSubscribe(url, l); super.subscribe(url, l); }
            catch (RuntimeException e) { failedTasks.add(() -> subscribe(url, l)); } // FailedSubscribedTask
        }
        abstract void doRegister(URL url);
        abstract void doSubscribe(URL url, NotifyListener l);
        void retryLoop() { // HashedWheelTimer: unlimited retry
            while (!failedTasks.isEmpty() && connected) { failedTasks.poll().run(); }
        }
    }

    // ---- C: 订阅通知 — RegistryDirectory 动态目录 (分类/空保护/增量) ----
    static class RegistryDirectory implements NotifyListener {
        Map<URL, String> invokerMap = new LinkedHashMap<>(); // URL → invoker 标识
        boolean forbidden = false;

        public void notify(List<URL> urls) {
            // 分类: 只取 providers
            List<URL> providers = new ArrayList<>();
            for (URL u : urls) if ("providers".equals(u.category())) providers.add(u);
            refreshInvoker(providers);
        }

        void refreshInvoker(List<URL> urls) {
            if (urls.size() == 1 && "empty".equals(urls.get(0).protocol)) {
                forbidden = true; // EMPTY_PROTOCOL 空保护 → 禁止访问
                invokerMap.clear();
                return;
            }
            forbidden = false;
            if (urls.isEmpty()) {
                return; // 无数据 (真实实现会用 cachedInvokerUrls 兜底)
            }
            // 增量更新: 已存在的 URL 复用 (这里用 map put 模拟)
            Map<URL, String> newMap = new LinkedHashMap<>();
            for (URL u : urls) {
                newMap.put(u, invokerMap.containsKey(u) ? invokerMap.get(u) : "INVOKER(" + u.address + ")");
            }
            invokerMap = newMap;
        }
    }

    // ---- D: 实现族 — ZK 临时节点 vs Nacos 心跳 vs 组播 ----
    static class ZookeeperRegistry extends FailbackRegistry {
        final Map<String, String> zkNodes = new HashMap<>();
        void doRegister(URL url) { zkNodes.put(url.address + "/" + url.iface, "EPHEMERAL"); } // 临时节点
        void doSubscribe(URL url, NotifyListener l) {
            // 节点监听: 子节点变化 → notify
        }
        void simulateDisconnect() { zkNodes.clear(); } // 连接断 → 临时节点自动删
    }

    static class NacosRegistry extends FailbackRegistry {
        void doRegister(URL url) { /* 实例注册 + 心跳续约 */ }
        void doSubscribe(URL url, NotifyListener l) { /* 长轮询 */ }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: 抽象面 — 缓存兜底 ============
        FailbackRegistry reg = new FailbackRegistry() {
            void doRegister(URL url) { if (!connected) throw new RuntimeException("net down"); }
            void doSubscribe(URL url, NotifyListener l) { if (!connected) throw new RuntimeException("net down"); }
        };
        URL p1 = new URL("dubbo", "192.168.1.10:20880", "HelloService");
        reg.register(p1);
        reg.saveProperties(); // 通知后落盘
        reg.registered.clear(); // 模拟注册中心重启 (本地数据丢)
        System.out.println("A1 cache  : " + reg.loadProperties());

        // ============ B: 失败重试 — 断网后恢复 ============
        reg.connected = false;
        URL p2 = new URL("dubbo", "192.168.1.11:20880", "HelloService");
        reg.register(p2); // 失败 → 任务入队
        System.out.println("B1 queued : failed=" + reg.failedTasks.size());
        reg.connected = true;
        reg.retryLoop(); // 无限重试直到成功
        System.out.println("B2 retried: failed=" + reg.failedTasks.size() + ", registered=" + reg.registered.size());

        // ============ C: 订阅通知 — 空保护 + 增量 ============
        RegistryDirectory dir = new RegistryDirectory();
        List<URL> notify1 = new ArrayList<>(Arrays.asList(p1, p2));
        dir.notify(notify1);
        System.out.println("C1 dir    : " + dir.invokerMap.size() + " invokers, forbidden=" + dir.forbidden);
        dir.notify(new ArrayList<>(Arrays.asList(new URL("empty", "0", "HelloService")))); // 注册中心清空
        System.out.println("C2 empty  : forbidden=" + dir.forbidden + ", invokers=" + dir.invokerMap.size());
        boolean emptyForbidden = dir.forbidden; // 保存空保护状态
        dir.notify(notify1); // 恢复
        dir.notify(new ArrayList<>(Arrays.asList(p1, p2, new URL("dubbo", "192.168.1.12:20880", "HelloService")))); // 增量+1
        System.out.println("C3 grow   : " + dir.invokerMap.size() + " invokers");

        // ============ D: 实现族 — ZK 临时节点 ============
        ZookeeperRegistry zk = new ZookeeperRegistry();
        zk.doRegister(p1);
        System.out.println("D1 zk     : node=" + zk.zkNodes);
        zk.simulateDisconnect();
        System.out.println("D2 zk-disc: nodes=" + zk.zkNodes.size() + " (临时节点自动删)");

        // 断言
        pass += reg.loadProperties().size() == 1 ? 1 : 0;
        pass += reg.registered.size() == 1 ? 1 : 0;
        pass += emptyForbidden ? 1 : 0;
        pass += dir.invokerMap.size() == 3 ? 1 : 0;
        pass += zk.zkNodes.size() == 0 ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
