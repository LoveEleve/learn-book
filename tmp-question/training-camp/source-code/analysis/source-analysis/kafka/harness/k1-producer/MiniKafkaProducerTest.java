import java.util.Deque;

public class MiniKafkaProducerTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    public static void main(String[] args) {
        testAsyncPipeline();
        testBatching();
        testBufferPoolExhausted();
        testBufferPoolRecycle();
        testStickyPartition();
        testKeyHashing();
        testAcksSemantics();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A. 异步流水线: send 只入队, drain 才发送 (KafkaProducer L940 / Sender L241) */
    static void testAsyncPipeline() {
        MiniKafkaProducer p = new MiniKafkaProducer(1024 * 1024, 64, 3, 1, 2);
        p.send("t", null, "a");
        p.send("t", null, "b");
        check("send 后未发送 (入队)", p.sentCount.get() == 0 && p.pendingBatches() >= 1);
        int sent = p.drain();
        check("drain 后发送", sent == 2 && p.sentCount.get() == 2);
    }

    /** B. 批聚合: 同分区同批, 超 batchSize 新批 (RecordAccumulator L319/L345) */
    static void testBatching() {
        MiniKafkaProducer p = new MiniKafkaProducer(1024 * 1024, 100, 100, 1, 1);
        for (int i = 0; i < 3; i++) p.send("t", "k", "v");
        check("同 key 同批", p.pendingBatches() == 1);
        Deque<MiniKafkaProducer.Batch> dq = p.dequeFor("t", Math.floorMod("k".hashCode(), 1));
        MiniKafkaProducer.Batch b = dq.peekLast();
        check("批内聚合 3 条", b != null && b.records.size() == 3);
    }

    /** C. 内存池: 超 buffer.memory 抛 (BufferPool allocate 阻塞语义简化) */
    static void testBufferPoolExhausted() {
        MiniKafkaProducer p = new MiniKafkaProducer(192, 128, 10, 1, 1);
        p.send("t", "k", "v".repeat(100));   // 100 字节占批
        boolean threw = false;
        try {
            p.send("t", "k", "w".repeat(100));  // 批满 → 新批 allocate → 超 192
            p.drain();                    // 释放后才不抛 (真实: 阻塞等待)
            threw = false;
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("内存耗尽抛异常", threw);
    }

    /** C2. 回收: deallocate 后 free 队列复用 (BufferPool.java:39-41) */
    static void testBufferPoolRecycle() {
        MiniKafkaProducer p = new MiniKafkaProducer(1024, 128, 10, 1, 1);
        p.send("t", "k", "aaaa");
        p.drain();   // 发送后 deallocate → free 队列
        check("poolableSize 回收进 free", !p.bufferPool.free.isEmpty());
        p.send("t", "k", "bbbb");   // 复用 free 缓冲
        check("复用后 free 减少", p.bufferPool.free.isEmpty());
    }

    /** D. 粘性分区: 无 key 固定分区 (BuiltInPartitioner L300-302) */
    static void testStickyPartition() {
        MiniKafkaProducer p = new MiniKafkaProducer(1024 * 1024, 64, 100, 1, 4);
        int first = p.effectivePartition("t", false);
        int second = p.effectivePartition("t", false);
        check("无 key 粘性固定分区", first == second);
        check("粘性分区在范围内", first >= 0 && first < 4);
    }

    /** D2. key 哈希分区: 同 key 同分区 (BuiltInPartitioner.java:330 murmur2 简化) */
    static void testKeyHashing() {
        MiniKafkaProducer p = new MiniKafkaProducer(1024 * 1024, 64, 100, 1, 3);
        int k1 = p.effectivePartition("t", true);
        int k2 = p.effectivePartition("t", true);
        check("同 key 同分区", k1 == k2);
    }

    /** E. acks 语义: 0/1/all 都确认 (简化: 差异在等待方) */
    static void testAcksSemantics() {
        MiniKafkaProducer p0 = new MiniKafkaProducer(1024 * 1024, 64, 10, 0, 1);
        p0.send("t", null, "x");
        p0.drain();
        check("acks=0 发送即确认", p0.ackedCount.get() == 1);
        MiniKafkaProducer pall = new MiniKafkaProducer(1024 * 1024, 64, 10, -1, 1);
        pall.send("t", null, "y");
        pall.drain();
        check("acks=all 确认", pall.ackedCount.get() == 1);
    }
}
