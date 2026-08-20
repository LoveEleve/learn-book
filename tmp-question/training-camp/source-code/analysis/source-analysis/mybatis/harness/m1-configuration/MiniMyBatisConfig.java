import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * MiniMyBatisConfig — M-1 Configuration 核心+装配 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 MyBatis 源码):
 *   A. properties 三级合并优先级: 构造 props > 文件 > XML 内嵌 (XMLConfigBuilder.java:237-259)
 *   B. StrictMap 语义: put 冲突抛 / 短名自动注册 / Ambiguity 歧义占位 (Configuration.java:1104-1202)
 *   C. newExecutor 装饰链顺序: 类型分支 → CachingExecutor(内) → pluginAll(外) (Configuration.java:728-742)
 */
public class MiniMyBatisConfig {

    // ===== A. properties 三级合并 =====
    static class MiniPropertiesElement {
        static Properties merge(Properties xmlInline, Properties fileProps, Properties ctorProps) {
            Properties defaults = new Properties();
            defaults.putAll(xmlInline);        // 1. XML 内嵌 (最低)
            if (fileProps != null) {
                defaults.putAll(fileProps);    // 2. 文件 (覆盖内嵌)
            }
            if (ctorProps != null) {
                defaults.putAll(ctorProps);    // 3. 构造传入 (最高)
            }
            return defaults;
        }
    }

    // ===== B. StrictMap 语义 =====
    static class MiniStrictMap<V> extends HashMap<String, V> {
        @Override
        public V put(String key, V value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("already contains key " + key);   // 冲突直接抛
            }
            if (key.contains(".")) {           // 全限定名 → 自动注册短名
                String shortKey = key.substring(key.lastIndexOf('.') + 1);
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    super.put(shortKey, (V) new Ambiguity(shortKey));   // 短名冲突 → 占位
                }
            }
            return super.put(key, value);
        }

        @Override
        public V get(Object key) {
            V value = super.get(key);
            if (value == null) {
                throw new IllegalArgumentException("does not contain value for " + key);
            }
            if (value instanceof Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).subject + " is ambiguous");
            }
            return value;
        }

        static class Ambiguity {
            final String subject;
            Ambiguity(String subject) { this.subject = subject; }
        }
    }

    // ===== C. 装饰链: CachingExecutor 内 + pluginAll 外 =====
    interface Executor {
        String name();
    }
    static class SimpleExecutor implements Executor { public String name() { return "Simple"; } }
    static class CachingExecutor implements Executor {
        final Executor delegate;
        CachingExecutor(Executor delegate) { this.delegate = delegate; }
        public String name() { return "Caching(" + delegate.name() + ")"; }
    }
    static class PluginExecutor implements Executor {
        final Executor target;
        PluginExecutor(Executor target) { this.target = target; }
        public String name() { return "Plugin(" + target.name() + ")"; }
    }
    static Executor newExecutor(String type, boolean cacheEnabled, boolean hasPlugin) {
        Executor executor = new SimpleExecutor();           // 简化: 只演示 SIMPLE 分支
        if (cacheEnabled) {
            executor = new CachingExecutor(executor);       // 缓存在内
        }
        if (hasPlugin) {
            executor = new PluginExecutor(executor);        // 插件在外
        }
        return executor;
    }

    static int passed = 0, failed = 0;

    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A. properties 三级合并 ==");
        Properties xml = new Properties();
        xml.setProperty("driver", "xml-driver");
        xml.setProperty("url", "xml-url");
        Properties file = new Properties();
        file.setProperty("url", "file-url");        // 文件覆盖内嵌
        Properties ctor = new Properties();
        ctor.setProperty("url", "ctor-url");        // 构造覆盖文件
        Properties merged = MiniPropertiesElement.merge(xml, file, ctor);
        check("构造 props 最高优先级: url=" + merged.getProperty("url"), "ctor-url".equals(merged.getProperty("url")));
        check("文件覆盖内嵌: driver=" + merged.getProperty("driver"), "xml-driver".equals(merged.getProperty("driver")));

        System.out.println("== B. StrictMap 语义 ==");
        MiniStrictMap<String> ms = new MiniStrictMap<>();
        ms.put("com.xx.UserMapper.selectById", "ms1");
        check("全限定名注册后短名自动可用: " + ms.get("selectById"), "ms1".equals(ms.get("selectById")));
        ms.put("com.yy.UserMapper.selectById", "ms2");
        boolean ambiguous = false;
        try { ms.get("selectById"); } catch (IllegalArgumentException e) { ambiguous = e.getMessage().contains("ambiguous"); }
        check("短名冲突后 get 抛歧义异常", ambiguous);
        boolean conflict = false;
        try { ms.put("com.xx.UserMapper.selectById", "dup"); } catch (IllegalArgumentException e) { conflict = true; }
        check("重复全限定名 put 直接抛", conflict);
        boolean missing = false;
        try { ms.get("noSuchKey"); } catch (IllegalArgumentException e) { missing = true; }
        check("不存在的 key get 抛", missing);
        check("冲突后全限定名仍可读: " + ms.get("com.xx.UserMapper.selectById"), "ms1".equals(ms.get("com.xx.UserMapper.selectById")));

        System.out.println("== C. newExecutor 装饰链 ==");
        check("cacheEnabled 时缓存包内: " + newExecutor("SIMPLE", true, false).name(), "Caching(Simple)".equals(newExecutor("SIMPLE", true, false).name()));
        check("插件包最外: " + newExecutor("SIMPLE", true, true).name(), "Plugin(Caching(Simple))".equals(newExecutor("SIMPLE", true, true).name()));
        check("无缓存无插件: " + newExecutor("SIMPLE", false, false).name(), "Simple".equals(newExecutor("SIMPLE", false, false).name()));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
