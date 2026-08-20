import java.nio.file.*;
import java.util.*;

/**
 * MiniTranslogTest — E-3 harness 验证入口
 *
 * 跑法: javac MiniTranslog.java MiniTranslogTest.java && java MiniTranslogTest
 * 全部 PASS = Translog 双缓冲/checkpoint 恢复/轮转/tragedy 四机制理解到位
 * (对照 TranslogWriter.java:227-267,465-534 / Translog.java:1628-1652,229-256 / TragicExceptionHolder.java:15-33)。
 */
public class MiniTranslogTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    static Path tmpDir(String name) throws Exception {
        Path p = Files.createTempDirectory(name);
        p.toFile().deleteOnExit();
        return p;
    }

    // A. 双缓冲: add 不落盘 (文件零字节), sync 才落盘
    static void testDualBuffer() throws Exception {
        Path dir = tmpDir("mini-dbuf");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 1024 * 1024, 1);

        log.add(0, "hello".getBytes());
        check("A1 add 后文件零字节 (纯内存)", Files.size(dir.resolve("translog-1.tlog")) == 0);
        check("A2 syncNeeded=true", log.current.syncNeeded());

        log.sync();
        check("A3 sync 后文件有数据", Files.size(dir.resolve("translog-1.tlog")) > 0);
        check("A4 sync 后 syncNeeded=false", !log.current.syncNeeded());

        log.close();
    }

    // A2. 批量: 多次 add 一次 sync 全落盘
    static void testBatchSync() throws Exception {
        Path dir = tmpDir("mini-batch");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 1024 * 1024, 1);

        for (int i = 0; i < 100; i++) log.add(i, ("op-" + i).getBytes());
        check("A5 100 add 后文件零字节", Files.size(dir.resolve("translog-1.tlog")) == 0);
        log.sync();
        check("A6 一次 sync 后 100 条全落盘", Files.size(dir.resolve("translog-1.tlog")) > 0);
        log.close();
    }

    // B. 崩溃恢复: 已 sync 操作恢复, 未 sync (断电) 丢失
    static void testCrashRecovery() throws Exception {
        Path dir = tmpDir("mini-crash");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 1024 * 1024, 1);

        log.add(0, "durable-0".getBytes());
        log.add(1, "durable-1".getBytes());
        log.sync();                          // 前 2 条已持久化
        log.add(2, "volatile-2".getBytes()); // 这条只在 buffer, 未 fsync

        // 模拟断电: 不 close, 直接从磁盘恢复 (未 sync 的 buffer 不在文件里)
        List<MiniTranslog.Op> recovered = MiniTranslog.Log.recover(dir, 1, 1);
        check("B1 恢复 2 条 (已 sync)", recovered.size() == 2);
        check("B2 首条内容正确", new String(recovered.get(0).data).equals("durable-0"));
        check("B3 未 sync 数据丢失 (断电语义)", new String(recovered.get(1).data).equals("durable-1"));
        log.close();
    }

    // B2. 半写尾部: 文件末尾不完整操作被丢弃
    static void testTruncatedTail() throws Exception {
        Path dir = tmpDir("mini-trunc");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 1024 * 1024, 1);
        log.add(0, "full".getBytes());
        log.sync();
        log.close();

        // 人为追加半条 (模拟崩溃时写了一半)
        try (java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(
                dir.resolve("translog-1.tlog"),
                StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ch.write(java.nio.ByteBuffer.wrap(new byte[] { 0, 0, 0, 20, 1, 2 })); // size=20 但只有 2 字节
        }
        List<MiniTranslog.Op> recovered = MiniTranslog.Log.recover(dir, 1, 1);
        check("B4 半写尾部被丢弃", recovered.size() == 1);
        log.close();
    }

    // C. 轮转: 超阈值 → 封存当前代 → 新代写
    static void testRollGeneration() throws Exception {
        Path dir = tmpDir("mini-roll");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 64, 1); // 阈值 64B

        log.add(0, "aaa".getBytes());   // 4+3=7B
        log.add(1, "bbb".getBytes());   // 14B — 未超 64
        check("C1 未超阈值不轮转", log.current.gen == 1);
        log.add(2, ("x".repeat(100)).getBytes()); // 超阈值 → 触发轮转
        check("C2 超阈值后轮转到新代", log.current.gen == 2);
        check("C3 旧代已封存 (readers 有 1 个)", log.readers.size() == 1);

        log.add(3, "ccc".getBytes());   // 写在新代
        log.sync();
        check("C4 新代有数据", Files.size(dir.resolve("translog-2.tlog")) > 0);
        check("C5 旧代保留 (数据可恢复)", Files.size(dir.resolve("translog-1.tlog")) > 0);
        log.close();
    }

    // C2. 跨代恢复: 多代合并回放
    static void testMultiGenRecovery() throws Exception {
        Path dir = tmpDir("mini-multigen");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 32, 1);
        log.add(0, "g1-0".getBytes());
        log.add(1, "g1-1".getBytes());
        log.sync();
        log.rollGeneration();              // 显式轮转: gen1 封存, gen2 新写
        log.add(2, "g2-0".getBytes());
        log.sync();
        log.close();

        List<MiniTranslog.Op> recovered = MiniTranslog.Log.recover(dir, 1, 2);
        check("C6 跨代恢复 3 条", recovered.size() == 3);
        check("C7 恢复顺序按 seqNo 升序", new String(recovered.get(2).data).equals("g2-0"));
    }

    // D. tragedy: 写失败 → 首因冻结 + 后续操作拒绝
    static void testTragic() throws Exception {
        Path dir = tmpDir("mini-tragic");
        MiniTranslog.Log log = new MiniTranslog.Log(dir, 1024, 1);

        // 强制制造 IO 异常: 关闭 current 通道后写入
        log.current.channel.close();
        try {
            log.add(0, "boom".getBytes());
            check("D1 写失败抛异常", false);
        } catch (Exception e) {
            check("D1 写失败抛异常", true);
        }
        check("D2 tragedy 冻结", log.tragedy.get());
        try {
            log.add(1, "after".getBytes());
            check("D3 tragedy 后拒绝写", false);
        } catch (IllegalStateException e) {
            check("D3 tragedy 后拒绝写", e.getMessage().startsWith("tragic"));
        }
        try {
            log.sync();
            check("D4 tragedy 后拒绝 sync", false);
        } catch (IllegalStateException e) {
            check("D4 tragedy 后拒绝 sync", true);
        }
    }

    public static void main(String[] args) throws Exception {
        testDualBuffer();
        testBatchSync();
        testCrashRecovery();
        testTruncatedTail();
        testRollGeneration();
        testMultiGenRecovery();
        testTragic();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
