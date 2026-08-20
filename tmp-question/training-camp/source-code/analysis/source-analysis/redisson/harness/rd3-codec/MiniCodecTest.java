import java.util.*;
import java.util.concurrent.*;

/**
 * MiniCodecTest — RD-3 harness 验证入口
 *
 * 跑法: javac MiniCodec.java MiniCodecTest.java && java MiniCodecTest
 * 全部 PASS = 机制理解到位 (对照 Redisson 4.6.2 源码: Codec.java:30 / CompositeCodec.java:30 / Kryo5Codec.java:202)。
 */
public class MiniCodecTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 原型零开销: StringCodec 往返 (StringCodec.java:44-53)
    static void testStringCodec() {
        MiniCodec.StringCodec sc = MiniCodec.StringCodec.INSTANCE;
        byte[] buf = sc.getValueEncoder().encode("hello-redisson");
        check("字符串编码", new String(buf, java.nio.charset.StandardCharsets.UTF_8).equals("hello-redisson"));
        Object back = sc.getValueDecoder().decode(buf);
        check("字符串解码往返", "hello-redisson".equals(back));
    }

    // 数值原型 LongCodec
    static void testLongCodec() {
        MiniCodec.LongCodec lc = MiniCodec.LongCodec.INSTANCE;
        byte[] buf = lc.getValueEncoder().encode(123456789L);
        Object back = lc.getValueDecoder().decode(buf);
        check("数值原型往返", back instanceof Long && (Long) back == 123456789L);
    }

    // B. CompositeCodec 委托: mapKey=String, mapValue=Long (对照 ReliableTopic:82 模式)
    static void testCompositeDelegation() {
        MiniCodec.CompositeCodec composite = new MiniCodec.CompositeCodec(
                MiniCodec.StringCodec.INSTANCE, MiniCodec.LongCodec.INSTANCE); // 2 参 (value=null)
        // mapKey → String
        byte[] keyBuf = composite.getMapKeyEncoder().encode("stream-id-1");
        check("composite mapKey 走 StringCodec", "stream-id-1".equals(composite.getMapKeyDecoder().decode(keyBuf)));
        // mapValue → Long
        byte[] valBuf = composite.getMapValueEncoder().encode(42L);
        Object val = composite.getMapValueDecoder().decode(valBuf);
        check("composite mapValue 走 LongCodec", val instanceof Long && (Long) val == 42L);
        // value 路径 (Stream 场景不碰): 若调用 value → NPE (契约, 展示边界)
        try {
            composite.getValueDecoder().decode(new byte[0]);
            check("valueCodec=null 触碰 NPE (契约保护)", false);
        } catch (NullPointerException e) {
            check("valueCodec=null 触碰 NPE (契约保护)", true);
        }
    }

    // C1. 丢类型: UntypedCodec 往返返回真实类型但调用方需 cast (JsonJackson:105)
    static void testUntypedRoundtrip() {
        MiniCodec.Codec uncomp = new MiniCodec.UntypedCodec();
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1); src.put("b", 2);
        byte[] buf = uncomp.getValueEncoder().encode(src);
        Object back = uncomp.getValueDecoder().decode(buf);
        check("丢类型往返 (运行时保留类型)", back instanceof HashMap);
    }

    // C2. 类型内置: typeful 往返 — 流中带类名, 反序列化精确恢复泛型 (Kryo5 ClassAndObject:202)
    static void testTypefulRoundtrip() throws Exception {
        MiniCodec.Codec typed = new MiniCodec.UntypedCodec() {
            // 用 typefulEncode/Decode 替代 (ClassAndObject 语义)
        };
        @SuppressWarnings("unused") // 直接验证 typeful 函数
        List<String> src = Arrays.asList("x", "y", "z");
        byte[] buf = MiniCodec.typefulEncode(src);
        Object back = MiniCodec.typefulDecode(buf);
        check("类型内置流 (ClassAndObject)", back instanceof List && ((List<?>) back).size() == 3);
    }

    // D. 线程安全: 模拟 Kryo 池化 — 并发大小 1 的池下 StringCodec 单例安全 (Kryo5 pool 语义)
    static void testConcurrentPool() throws Exception {
        MiniCodec.StringCodec sc = MiniCodec.StringCodec.INSTANCE; // 单例共享
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    String payload = "msg-" + idx;
                    byte[] buf = sc.getValueEncoder().encode(payload);
                    return payload.equals(sc.getValueDecoder().decode(buf));
                } catch (Exception e) { return false; }
            }));
        }
        start.countDown();
        boolean allOk = true;
        for (Future<Boolean> f : futures) allOk &= f.get();
        check("单例 StringCodec 并发安全 (无状态)", allOk);
        pool.shutdownNow();
    }

    public static void main(String[] args) throws Exception {
        testStringCodec();
        testLongCodec();
        testCompositeDelegation();
        testUntypedRoundtrip();
        testTypefulRoundtrip();
        testConcurrentPool();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}