import java.util.*;

/**
 * MiniSlotChain — Sentinel S-1 微缩引擎(极简复现,无并发/无配置)
 *
 * 覆盖核心机制:
 *  A. DefaultProcessorSlotChain 匿名头节点 + addLast(边界折叠)
 *  B. fireEntry → transformEntry 递归遍历(泛型桥,entry 转换/exit 直传)
 *  C. SpiLoader 按 @Spi order 稳定排序 + 单例/原型分治
 *  D. LogSlot 一个 catch 覆盖全部下游检查槽的 BlockException
 *  E. chainMap copy-on-write + 6000 上限静默放行
 */
public class MiniSlotChain {

    // ---- A: 槽抽象(与 AbstractLinkedProcessorSlot 同构) ----
    static abstract class Slot<T> {
        Slot<?> next = null;
        void fireEntry(String res, Object obj) {
            if (next != null) next.transformEntry(res, obj);
        }
        @SuppressWarnings("unchecked")
        void transformEntry(String res, Object o) {
            entry(res, (T) o);   // 泛型桥:entry 方向必须 cast
        }
        void fireExit(String res) {
            if (next != null) next.exit(res);
        }
        abstract void entry(String res, T obj);
        void exit(String res) { fireExit(res); }   // exit 无泛型参数,直传
    }

    // ---- B: 链容器(匿名头节点 + end 尾指针) ----
    static class ProcessorSlotChain {
        final Slot<Object> first = new Slot<Object>() {   // 头节点:自己不检查
            @Override void entry(String res, Object o) { fireEntry(res, o); }
        };
        Slot<?> end = first;
        void addLast(Slot<?> s) { end.next = s; end = s; }   // 空链/非空链统一,无边界判断
        void entry(String res) { first.transformEntry(res, null); }
    }

    // ---- C: @Spi 注解 + 迷你 SpiLoader ----
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)   // 反射可读(真实 Spi.java:26)
    @interface Spi {
        int order() default 0;
        boolean isSingleton() default true;
    }

    static class SpiLoader {
        static final Map<String, Object> singletonMap = new HashMap<>();
        static List<Class<?>> loadInstanceListSorted(Class<?> service, Map<String, Class<?>> registry) {
            List<Class<?>> list = new ArrayList<>(registry.values());
            list.sort((a, b) -> Integer.compare(a.getAnnotation(Spi.class).order(),
                                                b.getAnnotation(Spi.class).order()));   // 稳定排序
            return list;
        }
        @SuppressWarnings("unchecked")
        static <T> T create(Class<?> clazz) {
            Spi spi = clazz.getAnnotation(Spi.class);
            if (spi.isSingleton()) {
                Object o = singletonMap.get(clazz.getName());          // 单例:双检锁(此处简化无锁)
                if (o == null) { o = newInstance(clazz); singletonMap.put(clazz.getName(), o); }
                return (T) o;
            }
            return (T) newInstance(clazz);                             // 原型:每链一新
        }
        static Object newInstance(Class<?> c) {
            try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { throw new RuntimeException(e); }
        }
    }

    // ---- 内置槽:9 个(order 与 Constants 一致)+ 1 个扩展槽 ----
    static class BlockException extends RuntimeException {}

    @Spi(order = -10000, isSingleton = false)   // 原型:持有资源节点状态
    static class NodeSelectorSlot extends Slot<Object> {
        String node;                            // 资源绑定状态(演示原型动机)
        @Override void entry(String res, Object o) { node = res; fireEntry(res, o); }
    }

    @Spi(order = -9000, isSingleton = false)    // 原型
    static class ClusterBuilderSlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    @Spi(order = -8000)                         // 单例:try-fireEntry-catch 包住全部下游
    static class LogSlot extends Slot<Object> {
        static int blockLogCount = 0;
        @Override void entry(String res, Object o) {
            try {
                fireEntry(res, o);
            } catch (BlockException e) {
                blockLogCount++;                 // 一个 catch 记全部 block
                throw e;                         // 记完继续上抛
            }
        }
    }

    @Spi(order = -7000)
    static class StatisticSlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    @Spi(order = -6000)
    static class AuthoritySlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    @Spi(order = -5000)
    static class SystemSlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    @Spi(order = -3000)                         // 扩展槽:靠 order 插进 System 与 Flow 之间
    static class ParamFlowSlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    @Spi(order = -2000)
    static class FlowSlot extends Slot<Object> {
        static boolean flowBlock = false;
        @Override void entry(String res, Object o) {
            if (flowBlock) throw new BlockException();   // 流控拒绝
            fireEntry(res, o);
        }
    }

    @Spi(order = -1000)
    static class DegradeSlot extends Slot<Object> {
        @Override void entry(String res, Object o) { fireEntry(res, o); }
    }

    // ---- D: 默认链构建器(loadInstanceListSorted + 过滤非 Slot) ----
    static class DefaultSlotChainBuilder {
        static ProcessorSlotChain build(Map<String, Class<?>> registry) {
            ProcessorSlotChain chain = new ProcessorSlotChain();
            for (Class<?> c : SpiLoader.loadInstanceListSorted(Slot.class, registry)) {
                chain.addLast(SpiLoader.create(c));
            }
            return chain;
        }
    }

    // ---- E: chainMap(COW + 6000 上限) ----
    static class CtSph {
        static final int MAX_SLOT_CHAIN_SIZE = 6000;
        static volatile Map<String, ProcessorSlotChain> chainMap = new HashMap<>();
        static final Map<String, Class<?>> slotRegistry = new LinkedHashMap<>();

        static ProcessorSlotChain lookProcessChain(String res) {
            ProcessorSlotChain chain = chainMap.get(res);
            if (chain == null) {
                synchronized (CtSph.class) {
                    chain = chainMap.get(res);
                    if (chain == null) {
                        if (chainMap.size() >= MAX_SLOT_CHAIN_SIZE) return null;   // 超限:静默放行
                        chain = DefaultSlotChainBuilder.build(slotRegistry);
                        Map<String, ProcessorSlotChain> newMap = new HashMap<>(chainMap);  // COW
                        newMap.put(res, chain);
                        chainMap = newMap;
                    }
                }
            }
            return chain;
        }

        static boolean entry(String res) {
            ProcessorSlotChain chain = lookProcessChain(res);
            if (chain == null) return true;              // 无链:放行(降级路径)
            try {
                chain.entry(res);
                return true;                             // 通过
            } catch (BlockException e) {
                return false;                            // 被拒
            }
        }
    }

    // ---- 主流程:场景 + 断言 ----
    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // 注册 9 内置 + 1 扩展槽
        CtSph.slotRegistry.put("NodeSelectorSlot", NodeSelectorSlot.class);
        CtSph.slotRegistry.put("ClusterBuilderSlot", ClusterBuilderSlot.class);
        CtSph.slotRegistry.put("LogSlot", LogSlot.class);
        CtSph.slotRegistry.put("StatisticSlot", StatisticSlot.class);
        CtSph.slotRegistry.put("AuthoritySlot", AuthoritySlot.class);
        CtSph.slotRegistry.put("SystemSlot", SystemSlot.class);
        CtSph.slotRegistry.put("ParamFlowSlot", ParamFlowSlot.class);
        CtSph.slotRegistry.put("FlowSlot", FlowSlot.class);
        CtSph.slotRegistry.put("DegradeSlot", DegradeSlot.class);

        // C1: 排序 — 首个必须 NodeSelector(order -10000),扩展槽插在 System 与 Flow 之间
        List<Class<?>> sorted = SpiLoader.loadInstanceListSorted(Slot.class, CtSph.slotRegistry);
        pass += sorted.get(0) == NodeSelectorSlot.class ? 1 : 0;
        pass += sorted.indexOf(SystemSlot.class) < sorted.indexOf(ParamFlowSlot.class)
             && sorted.indexOf(ParamFlowSlot.class) < sorted.indexOf(FlowSlot.class) ? 1 : 0;

        // C2: 单例/原型分治 — LogSlot 单例复用,NodeSelector 每链一新
        ProcessorSlotChain c1 = DefaultSlotChainBuilder.build(CtSph.slotRegistry);
        ProcessorSlotChain c2 = DefaultSlotChainBuilder.build(CtSph.slotRegistry);
        pass += c1 != c2 ? 1 : 0;   // 每 build 一条新链
        // 链内容物:NodeSelector(第1槽)必须不同实例(原型),LogSlot(第3槽)必须同一实例(单例)
        Slot<?> ns1 = c1.first.next, ns2 = c2.first.next;
        Slot<?> log1 = ns1.next.next, log2 = ns2.next.next;   // 第3槽 = LogSlot
        pass += (ns1 != ns2) && (log1 == log2) ? 1 : 0;

        // D1: 正常流量放行 + block 计数为 0
        FlowSlot.flowBlock = false;
        boolean ok = CtSph.entry("getUserById");
        pass += ok && LogSlot.blockLogCount == 0 ? 1 : 0;

        // D2: 流控拒绝 → LogSlot 恰好记 1 条(一个 catch 覆盖全部检查槽)
        FlowSlot.flowBlock = true;
        boolean blocked = !CtSph.entry("getUserById");
        pass += blocked && LogSlot.blockLogCount == 1 ? 1 : 0;

        // E1: 链按资源缓存 — 同一资源第二次走同一链
        pass += CtSph.chainMap.size() == 1 ? 1 : 0;

        // E2: 6000 上限静默放行 — 满链后新资源返回 null 链直接放行
        CtSph.chainMap = new HashMap<>();
        for (int i = 0; i < CtSph.MAX_SLOT_CHAIN_SIZE; i++) CtSph.chainMap.put("res" + i, new ProcessorSlotChain());
        boolean degrade = CtSph.entry("overLimitRes");   // 超限资源:无链
        pass += degrade ? 1 : 0;                          // 静默放行
        pass += CtSph.chainMap.size() == CtSph.MAX_SLOT_CHAIN_SIZE ? 1 : 0;   // 未再建链

        fail = 9 - pass;
        System.out.println("PASS=" + pass + "/9 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}