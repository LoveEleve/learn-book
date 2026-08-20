import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MiniRefresh — 微缩版 @RefreshScope 热刷新 (GenericScope 双锁 + ContextRefresher 双阶段)
 *
 * 覆盖机制:
 * 1. BeanLifecycleWrapper 懒创建双检锁 (synchronized name)
 * 2. cache + locks(ReadWriteLock) 双结构: get 无锁路径 / destroy 写锁清缓存
 * 3. LockedScopedProxyFactoryBean 方法级读锁 (每次调用拿读锁)
 * 4. ContextRefresher.refresh 双阶段: refreshEnvironment (环境变更) → refreshAll (清缓存)
 * 5. refresh 后懒重建: 下次调用拿新值
 * 6. 刷新期间并发: 读锁阻塞写锁 (旧 Bean 用完才重建)
 */
public class MiniRefresh {

    // ===== 缓存 + 锁 =====

    public static class BeanLifecycleWrapper {
        private final String name;
        private final java.util.function.Supplier<Object> factory;
        private volatile Object bean;

        BeanLifecycleWrapper(String name, java.util.function.Supplier<Object> factory) {
            this.name = name;
            this.factory = factory;
        }

        public Object getBean() {
            if (bean == null) {
                synchronized (name) {
                    if (bean == null) {
                        bean = factory.get();
                    }
                }
            }
            return bean;
        }

        public void destroy() {
            synchronized (name) {
                bean = null; // 清实例, 下次 getBean 重建
            }
        }
    }

    public static class GenericScope {
        private final Map<String, BeanLifecycleWrapper> cache = new ConcurrentHashMap<>();
        private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

        public Object get(String name, java.util.function.Supplier<Object> factory) {
            cache.putIfAbsent(name, new BeanLifecycleWrapper(name, factory));
            locks.putIfAbsent(name, new ReentrantReadWriteLock());
            return cache.get(name).getBean();
        }

        public boolean destroy(String name) {
            BeanLifecycleWrapper wrapper = cache.remove(name);
            if (wrapper != null) {
                ReentrantReadWriteLock lock = locks.get(name);
                lock.writeLock().lock();
                try {
                    wrapper.destroy();
                } finally {
                    lock.writeLock().unlock();
                }
                return true;
            }
            return false;
        }

        public int destroyAll() {
            List<String> names = new ArrayList<>(cache.keySet());
            int count = 0;
            for (String name : names) {
                if (destroy(name)) {
                    count++;
                }
            }
            return count;
        }

        public ReentrantReadWriteLock getLock(String name) {
            return locks.get(name);
        }
    }

    // ===== 代理: 方法级读锁 =====

    public static class LockedProxy {
        private final GenericScope scope;
        private final String targetBeanName;

        public LockedProxy(GenericScope scope, String targetBeanName) {
            this.scope = scope;
            this.targetBeanName = targetBeanName;
        }

        public Object invoke(java.util.function.Supplier<Object> method) {
            ReentrantReadWriteLock rw = scope.getLock(targetBeanName);
            if (rw == null) {
                rw = new ReentrantReadWriteLock(); // 防 NPE (对照 GenericScope:468-471)
            }
            rw.readLock().lock();
            try {
                return method.get();
            } finally {
                rw.readLock().unlock();
            }
        }
    }

    // ===== ContextRefresher 双阶段 =====

    public static class ContextRefresher {
        private final GenericScope scope;
        private String currentValue;

        public ContextRefresher(GenericScope scope, String currentValue) {
            this.scope = scope;
            this.currentValue = currentValue;
        }

        public synchronized int refresh(String newValue) {
            // 阶段 1: 环境变更 (对照 refreshEnvironment: before/after + EnvironmentChangeEvent)
            String before = currentValue;
            currentValue = newValue;
            boolean changed = !before.equals(newValue);
            // 阶段 2: 清 Scope 缓存 (对照 refreshAll: super.destroy + RefreshScopeRefreshedEvent)
            int destroyed = scope.destroyAll();
            return changed ? destroyed : 0;
        }

        public String getCurrentValue() {
            return currentValue;
        }
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

    public static void main(String[] args) throws Exception {
        // ---- 1. 懒创建双检 ----
        GenericScope scope = new GenericScope();
        int[] creations = {0};
        Object b1 = scope.get("config", () -> {
            creations[0]++;
            return "config-v1";
        });
        check("懒创建: 首次创建", creations[0] == 1 && "config-v1".equals(b1));
        scope.get("config", () -> {
            creations[0]++;
            return "config-v1-dup";
        });
        check("懒创建: 二次取缓存不重建", creations[0] == 1);

        // ---- 2. destroy 清缓存 → 懒重建 ----
        scope.destroy("config");
        Object b2 = scope.get("config", () -> {
            creations[0]++;
            return "config-v2";
        });
        check("destroy 后懒重建: 新值", creations[0] == 2 && "config-v2".equals(b2));

        // ---- 3. 双阶段刷新 ----
        GenericScope scope2 = new GenericScope();
        ContextRefresher refresher = new ContextRefresher(scope2, "old");
        Object before = scope2.get("bean", () -> "value-from-old-config");
        check("刷新前: 旧配置值", "value-from-old-config".equals(before));
        int destroyed = refresher.refresh("new");
        check("刷新: 环境变更 + 清缓存", destroyed == 1);
        Object after = scope2.get("bean", () -> "value-from-new-config");
        check("刷新后: 懒重建拿新值", "value-from-new-config".equals(after));
        check("刷新器环境已更新", "new".equals(refresher.getCurrentValue()));

        // ---- 4. 并发: 读锁不阻塞读, 写锁等读完成 ----
        GenericScope scope3 = new GenericScope();
        Object proxyTarget = scope3.get("shared", () -> "shared-data");
        LockedProxy proxy = new LockedProxy(scope3, "shared");
        // 模拟并发读 (读锁) + 刷新 (写锁)
        java.util.concurrent.atomic.AtomicBoolean readHolding = new java.util.concurrent.atomic.AtomicBoolean();
        java.util.concurrent.atomic.AtomicBoolean destroyCompleted = new java.util.concurrent.atomic.AtomicBoolean();
        Thread reader = new Thread(() -> {
            proxy.invoke(() -> {
                readHolding.set(true);
                try {
                    Thread.sleep(200); // 持读锁 200ms
                } catch (InterruptedException ignored) {
                }
                return proxyTarget;
            });
        });
        reader.start();
        Thread.sleep(50); // 确保 reader 已持读锁
        Thread destroyer = new Thread(() -> {
            scope3.destroy("shared"); // 写锁: 必须等 reader 释放
            destroyCompleted.set(true);
        });
        destroyer.start();
        Thread.sleep(50);
        check("写锁被读锁阻塞 (reader 持锁时 destroy 未完成)", !destroyCompleted.get());
        reader.join();
        destroyer.join();
        check("读锁释放后写锁完成", destroyCompleted.get());

        // ---- 5. 刷新后下一次调用走新工厂 (完整链路) ----
        GenericScope scope4 = new GenericScope();
        java.util.concurrent.atomic.AtomicInteger gen = new java.util.concurrent.atomic.AtomicInteger();
        scope4.get("c", () -> "gen-" + gen.incrementAndGet());
        check("完整链路: 首建 gen-1", "gen-1".equals(scope4.get("c", () -> "x")));
        scope4.destroy("c");
        check("完整链路: destroy 后重建 gen-2", "gen-2".equals(scope4.get("c", () -> "gen-" + gen.incrementAndGet())));

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }
}
