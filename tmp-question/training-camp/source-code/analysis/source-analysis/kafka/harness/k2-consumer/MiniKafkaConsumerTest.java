import java.util.List;

public class MiniKafkaConsumerTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    public static void main(String[] args) {
        testDualModelDispatch();
        testAsyncFetchBuffer();
        testClassicPollSync();
        testRebalance();
        testOffsetResetThreeModes();
        testCommitCheckpoint();
        testPollConsumesFromPosition();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A. 门面分流: Async 默认 / Classic 旧协议 (ConsumerDelegateCreator L64-66) */
    static void testDualModelDispatch() {
        MiniKafkaConsumer async = new MiniKafkaConsumer(true, 2);
        MiniKafkaConsumer classic = new MiniKafkaConsumer(false, 2);
        check("Async 模型", async.delegate.name().equals("AsyncKafkaConsumer"));
        check("Classic 模型", classic.delegate.name().equals("ClassicKafkaConsumer"));
    }

    /** B. FetchBuffer 跨线程: 网络线程填/应用线程读 (AsyncKafkaConsumer L304-305) */
    static void testAsyncFetchBuffer() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(true, 2);
        MiniKafkaConsumer.AsyncDelegate d = (MiniKafkaConsumer.AsyncDelegate) c.delegate;
        d.networkThreadDeliver(List.of("m1", "m2"));
        d.networkThreadDeliver(List.of("m3"));
        check("网络线程填充", d.networkThreadWrites.get() == 2);
        List<String> batch = c.poll(100);
        check("应用线程读取全部", batch.size() == 3 && batch.contains("m1") && batch.contains("m3"));
        check("读取后清空", c.poll(100).isEmpty());
    }

    /** C2. Classic 同步拉取: pollForFetches 拉当前位置后 (ClassicKafkaConsumer L690) */
    static void testClassicPollSync() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(false, 2);
        MiniKafkaConsumer.ClassicDelegate d = (MiniKafkaConsumer.ClassicDelegate) c.delegate;
        d.log.add("a");
        d.log.add("b");
        List<String> first = c.poll(100);
        check("Classic 拉取全部", first.size() == 2);
        d.log.add("c");
        List<String> second = c.poll(100);
        check("二次只拉新增", second.size() == 1 && second.get(0).equals("c"));
    }

    /** C. rebalance 四步完成 (AbstractCoordinator L400-401) */
    static void testRebalance() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(true, 2);
        check("rebalance 完成", c.rebalance());
    }

    /** D. offset reset 三态 (ConsumerConfig L175-179) */
    static void testOffsetResetThreeModes() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(true, 2);
        check("earliest 重置到 0", c.updatePosition("t", 0, "earliest", false) == 0);
        check("latest 重置到末尾", c.updatePosition("t", 1, "latest", false) == 100);
        boolean threw = false;
        try { c.updatePosition("t", 2, "none", false); }
        catch (IllegalStateException e) { threw = true; }
        check("none 无位置抛异常", threw);
    }

    /** D2. commit 检查点: 提交后重连从 committed 恢复 (updateFetchPositions L1188) */
    static void testCommitCheckpoint() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(true, 2);
        c.updatePosition("t", 0, "earliest", false);
        c.position("t", 0).position = 42;
        c.commit("t", 0);
        check("commit 保存位置", c.position("t", 0).committed == 42);
        // 模拟重连: 有 committed → 从 committed 恢复
        long restored = c.updatePosition("t", 0, "earliest", true);
        check("重连恢复 committed", restored == 42);
    }

    /** E. poll 消费位置: Classic 从 position 开始 (设计文档 §Consumer Position) */
    static void testPollConsumesFromPosition() {
        MiniKafkaConsumer c = new MiniKafkaConsumer(false, 2);
        MiniKafkaConsumer.ClassicDelegate d = (MiniKafkaConsumer.ClassicDelegate) c.delegate;
        for (int i = 0; i < 5; i++) d.log.add("m" + i);
        List<String> batch = c.poll(100);
        check("poll 拉取全部消息", batch.size() == 5);
    }
}
