import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * MiniBuilder — F-1 Builder 装配核心逻辑极简复现 (harness)
 *
 * 纯逻辑模拟, 对照 OpenFeign 13.14 源码验证:
 *   A. CRTP: 链式调用返回自身类型 (BaseBuilder.java:41 泛型自引用)
 *   B. 默认值: 未配置组件用默认 (BaseBuilder.java:43-62)
 *   C. clone 装饰: 副本修改不影响原 Builder (BaseBuilder.java:265-271)
 *   D. Capability 流水线: 多能力叠加顺序 (Capability.java:38-56 reduce)
 *   E. build 装配: 组件汇聚成"执行器" (Feign.java:217-242)
 */
public class MiniBuilder {

    /** 组件接口 */
    interface Encoder {
        String encode(String s);
    }

    static class DefaultEncoder implements Encoder {
        @Override
        public String encode(String s) {
            return "default:" + s;
        }
    }

    static class UpperEncoder implements Encoder {
        @Override
        public String encode(String s) {
            return "upper:" + s.toUpperCase();
        }
    }

    /** Capability: 装饰器 */
    interface Capability {
        Encoder enrich(Encoder encoder);

        static Encoder enrichAll(List<Capability> caps, Encoder original) {
            // reduce 流水线: cap1 结果喂 cap2 (Capability.java:38-56)
            Encoder result = original;
            for (Capability c : caps) {
                result = c.enrich(result);
            }
            return result;
        }
    }

    /** 执行器 (装配终点, 简化 ReflectiveFeign) */
    static class FeignClient {
        final Encoder encoder;
        final String name;

        FeignClient(Encoder encoder, String name) {
            this.encoder = encoder;
            this.name = name;
        }

        String call(String s) {
            return name + ":" + encoder.encode(s);
        }
    }

    /** A. CRTP Builder: 泛型自引用, 链式返回自身类型 */
    static class Builder<B extends Builder<B>> {
        protected Encoder encoder = new DefaultEncoder(); // B. 默认值
        protected List<Capability> capabilities = new ArrayList<>();
        protected String name = "client";

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B encoder(Encoder e) {
            this.encoder = e;
            return self(); // CRTP: 返回 B
        }

        public B name(String n) {
            this.name = n;
            return self();
        }

        public B addCapability(Capability c) {
            this.capabilities.add(c);
            return self();
        }

        /** C. clone 副本装饰: 原 Builder 不被污染 (BaseBuilder.java:265-271) */
        public Builder<B> enrich() {
            Builder<B> copy = new Builder<>();
            copy.encoder = this.encoder;
            copy.capabilities = new ArrayList<>(this.capabilities);
            copy.name = this.name;
            // 逐组件 Capability.enrich
            copy.encoder = Capability.enrichAll(copy.capabilities, copy.encoder);
            return copy;
        }

        /** E. build: 装配终点 */
        public FeignClient build() {
            Builder<B> enriched = enrich();
            return new FeignClient(enriched.encoder, enriched.name);
        }
    }

    static class MyBuilder extends Builder<MyBuilder> {
        // 子类: 链式返回 MyBuilder
        public MyBuilder extra() {
            return self();
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.CRTP", "B.默认值", "C.clone装饰", "D.Capability", "E.装配"};
        boolean[] r = new boolean[5];

        // ---------- A. CRTP ----------
        {
            MyBuilder b = new MyBuilder().encoder(new UpperEncoder()).name("x").extra(); // 链式返回 MyBuilder
            r[0] = b != null;
            System.out.println("[A] CRTP: 子类链式调用返回 MyBuilder = " + r[0]);
        }

        // ---------- B. 默认值 ----------
        {
            MyBuilder b = new MyBuilder(); // 零配置
            FeignClient client = b.build();
            r[1] = client.encoder instanceof DefaultEncoder;
            System.out.println("[B] 默认值: 未配置 encoder → DefaultEncoder = " + r[1]);
        }

        // ---------- C. clone 装饰 ----------
        {
            MyBuilder original = new MyBuilder().encoder(new UpperEncoder());
            FeignClient fromOriginal = original.build(); // build 内部 enrich 副本
            FeignClient second = original.build(); // 原 Builder 未被污染, 可再次 build
            r[2] = fromOriginal.call("hi").equals("client:upper:HI") && second.call("hi").equals("client:upper:HI");
            System.out.println("[C] clone: 两次 build 互不影响 = " + r[2]);
        }

        // ---------- D. Capability 流水线 ----------
        {
            Capability prefix = enc -> s -> "P1(" + enc.encode(s) + ")";
            Capability suffix = enc -> s -> enc.encode(s) + ")";
            MyBuilder b = new MyBuilder().encoder(new UpperEncoder())
                    .addCapability(prefix)
                    .addCapability(suffix);
            FeignClient client = b.build();
            String result = client.call("hi");
            // reduce: suffix 包 prefix 包 upper → upper 先执行
            r[3] = result.equals("client:P1(upper:HI))");
            System.out.println("[D] Capability: reduce 顺序 upper→P1→suffix = " + result);
        }

        // ---------- E. 装配 ----------
        {
            MyBuilder b = new MyBuilder().encoder(new UpperEncoder()).name("svc");
            FeignClient client = b.build();
            r[4] = client.call("a").equals("svc:upper:A");
            System.out.println("[E] 装配: " + client.call("a"));
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
