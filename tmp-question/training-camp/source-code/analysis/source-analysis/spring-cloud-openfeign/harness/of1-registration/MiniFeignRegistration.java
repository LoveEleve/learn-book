import java.util.*;

public class MiniFeignRegistration {

    // ---- A: @EnableFeignClients 属性 + 注册分流 ----
    static class EnableFeignClients {
        String[] value = {};
        String[] basePackages = {};
        Class<?>[] basePackageClasses = {};
        Class<?>[] defaultConfiguration = {};
        Class<?>[] clients = {};
    }

    // ---- B: 扫描 (AnnotationTypeFilter + 四级包解析) ----
    static class AnnotationTypeFilter {
        final String annotation;
        AnnotationTypeFilter(String a) { annotation = a; }
        boolean matches(String className) { return className.contains(annotation); }
    }

    static class Scanner {
        AnnotationTypeFilter filter;
        List<String> findCandidateComponents(String pkg, List<String> allClasses) {
            List<String> found = new ArrayList<>();
            for (String c : allClasses) {
                if (c.startsWith(pkg) && filter.matches(c)) found.add(c);
            }
            return found;
        }
    }

    static class Registrar {
        static String getBasePackages(EnableFeignClients ann, String importingClass) {
            Set<String> pkgs = new LinkedHashSet<>();
            pkgs.addAll(Arrays.asList(ann.value));
            pkgs.addAll(Arrays.asList(ann.basePackages));
            for (Class<?> c : ann.basePackageClasses) pkgs.add(c.getPackageName());
            if (pkgs.isEmpty()) {
                pkgs.add(importingClass.substring(0, importingClass.lastIndexOf('.'))); // 兜底 importingClass 包
            }
            return pkgs.iterator().next();
        }
    }

    // ---- C: 注册 (BeanDefinition 属性 + name 三级回退) ----
    static class BeanDefinition {
        final Map<String, Object> properties = new LinkedHashMap<>();
        String scope = "singleton";
        void addProperty(String k, Object v) { properties.put(k, v); }
    }

    static class Registration {
        static String getName(Map<String, Object> attrs) {
            String name = (String) attrs.get("serviceId");
            if (name == null || name.isEmpty()) name = (String) attrs.get("name");
            if (name == null || name.isEmpty()) name = (String) attrs.get("value");
            return name;
        }
        static BeanDefinition eagerlyRegister(String className, Map<String, Object> attrs, boolean refreshEnabled) {
            BeanDefinition def = new BeanDefinition();
            def.addProperty("type", className);
            def.addProperty("name", getName(attrs));
            def.addProperty("url", attrs.get("url"));
            def.addProperty("dismiss404", attrs.getOrDefault("dismiss404", false));
            def.addProperty("refreshableClient", refreshEnabled);
            if (refreshEnabled) def.scope = "refresh"; // setScope("refresh") — OF-9 前置
            return def;
        }
    }

    // ---- D: 配置注册 (FeignClientSpecification 三参) ----
    static class FeignClientSpecification {
        final String name; final String className; final Class<?>[] configuration;
        FeignClientSpecification(String name, String className, Class<?>[] config) {
            this.name = name; this.className = className; this.configuration = config;
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A/B: 注解 + 扫描 ============
        EnableFeignClients ann = new EnableFeignClients(); // 空属性 → 兜底
        String importingClass = "com.example.Application";
        String pkg = Registrar.getBasePackages(ann, importingClass);
        System.out.println("A1 pkg    : " + pkg + " (兜底 importingClass 包)");

        Scanner scanner = new Scanner();
        scanner.filter = new AnnotationTypeFilter("@FeignClient");
        List<String> classes = Arrays.asList(
                "com.example.OrderClient@FeignClient", "com.example.UserClient@FeignClient", "com.example.OrderService");
        List<String> found = scanner.findCandidateComponents(pkg, classes);
        System.out.println("B1 scan   : " + found + " (只收 @FeignClient)");

        // ============ C: 注册 + name 三级回退 ============
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("name", "payment-service");
        attrs.put("url", "http://payment");
        BeanDefinition def = Registration.eagerlyRegister("com.example.PaymentClient", attrs, true);
        System.out.println("C1 def    : name=" + def.properties.get("name") + ", scope=" + def.scope + " (refresh 联动)");
        attrs.clear();
        attrs.put("serviceId", "order-service");
        System.out.println("C2 name   : " + Registration.getName(attrs) + " (serviceId 优先)");

        // ============ D: Specification 三参 ============
        FeignClientSpecification spec = new FeignClientSpecification("payment", "com.example.PaymentClient", null);
        System.out.println("D1 spec   : " + spec.name + "/" + spec.className + " (三参构造)");

        // 断言
        pass += pkg.equals("com.example") ? 1 : 0;                     // A 兜底包
        pass += found.size() == 2 && found.contains("com.example.OrderClient@FeignClient") ? 1 : 0; // B 过滤
        pass += def.scope.equals("refresh") && def.properties.get("refreshableClient").equals(true) ? 1 : 0; // C refresh
        pass += Registration.getName(attrs).equals("order-service") ? 1 : 0; // C serviceId 优先
        pass += spec.name.equals("payment") && spec.className.contains("PaymentClient") ? 1 : 0; // D
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
