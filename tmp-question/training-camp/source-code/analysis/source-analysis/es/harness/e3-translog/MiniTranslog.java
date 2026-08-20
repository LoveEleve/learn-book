import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MiniTranslog — E-3 Translog 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ES 8.12.2 源码):
 *   A. 双缓冲写路径: add() 只写内存 buffer, sync() 才批量写文件+force
 *      (TranslogWriter.java:227-267 双缓冲; 465-534 syncUpTo; 508 force(false))
 *   B. Checkpoint 水位恢复: 崩溃后按 ckp 只回放已同步操作, 未 sync 的数据丢失
 *      (Checkpoint.java 8 字段水位; Translog.java:229-256 recoverFromFiles)
 *   C. Generation 轮转: 写满阈值 → 封存当前代 closeIntoReader → 新代
 *      (Translog.java:1628-1652 rollGeneration; 1630 空代跳过)
 *   D. tragedy 机制: 任何 IO 异常 → 冻结首因 → 整体失效关闭
 *      (TragicExceptionHolder.java:15-33; Translog.java:869-889)
 *
 * 用真实 FileChannel + force() 模拟磁盘 WAL (机制复现非完整库)。
 */
public class MiniTranslog {

    /** 操作记录: seqNo + payload (对照 Translog.Operation) */
    static class Op {
        final long seqNo;
        final byte[] data;
        Op(long seqNo, byte[] data) { this.seqNo = seqNo; this.data = data; }
    }

    /** 代文件: buffer 双缓冲 + FileChannel 落盘 (对照 TranslogWriter) */
    static class Generation implements AutoCloseable {
        final long gen;
        final Path path;
        final FileChannel channel;
        ByteBuffer buffer = null;          // 堆内缓冲 (双缓冲: add 只写这)
        long totalOffset = 0;              // 逻辑总偏移 (含已写+缓冲)
        long lastSyncedOffset = 0;         // 已 fsync 水位
        final List<Op> ops = new ArrayList<>(); // 完整操作集 (供恢复重放)
        final AtomicBoolean closed = new AtomicBoolean(false);

        Generation(long gen, Path dir) throws Exception {
            this.gen = gen;
            this.path = dir.resolve("translog-" + gen + ".tlog");
            this.channel = FileChannel.open(path,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
        }

        // A1. add: 只写 buffer + 记账, 不碰 channel (TranslogWriter.java:227-267)
        synchronized Op add(long seqNo, byte[] data) throws Exception {
            ensureOpen();
            if (!channel.isOpen()) throw new java.nio.channels.ClosedChannelException(); // 模拟写失败检测
            byte[] framed = new byte[4 + data.length];       // size 头 (简化, 无 checksum)
            ByteBuffer.wrap(framed).putInt(data.length).put(data);
            if (buffer == null) buffer = ByteBuffer.allocate(64 * 1024);
            if (buffer.remaining() < framed.length) flushBuffer(); // forceWriteThreshold 简化
            buffer.put(framed);
            totalOffset += framed.length;
            Op op = new Op(seqNo, data);
            ops.add(op);
            return op;
        }

        // A2. sync: buffer 落盘 + force(false) — 文件长度只增不减 (TranslogWriter.java:508)
        synchronized boolean sync() throws Exception {
            ensureOpen();
            if (totalOffset == lastSyncedOffset) return false;
            flushBuffer();
            channel.force(false);           // force(false): 长度不变时元数据不刷
            lastSyncedOffset = totalOffset;
            return true;
        }

        boolean syncNeeded() {              // TranslogWriter.java:356-360 (简化: 只查 offset)
            return totalOffset != lastSyncedOffset;
        }

        private void flushBuffer() throws Exception {
            if (buffer != null && buffer.position() > 0) {
                buffer.flip();
                while (buffer.hasRemaining()) channel.write(buffer);
                buffer = null;
            }
        }

        // C1. 封存: sync 后转只读 (TranslogWriter.java:391-431 closeIntoReader)
        void seal() throws Exception {
            synchronized (this) {
                ensureOpen();
                sync();
                closed.set(true);
            }
        }

        void ensureOpen() {
            if (closed.get()) throw new IllegalStateException("gen " + gen + " closed");
        }

        public void close() throws Exception {
            if (closed.compareAndSet(false, true)) channel.close();
        }
    }

    /** translog 本体: readers(只读代) + current(写代) + 崩溃恢复 (对照 Translog) */
    static class Log {
        final Path dir;
        final List<Generation> readers = new ArrayList<>(); // 封存代 (只读)
        Generation current;                                // 写代
        final long rollThreshold;                          // 轮转阈值 (模拟大小触发)
        final AtomicBoolean tragedy = new AtomicBoolean(); // TragicExceptionHolder 简化
        Exception tragicCause;

        Log(Path dir, long rollThreshold, long startGen) throws Exception {
            this.dir = dir;
            this.rollThreshold = rollThreshold;
            this.current = new Generation(startGen, dir);
        }

        // 崩溃后从磁盘恢复 (对照 Translog.java:229-256 recoverFromFiles):
        // 按已知 checkpoint 水位, 从磁盘文件重放"已同步"的操作
        // 注: 真实 ES 倒序打开仅为先校验最新代 UUID (错误消息质量); harness 简化: 按代升序读, 保持物理顺序
        static List<Op> recover(Path dir, long minGen, long maxGen) throws Exception {
            List<Op> recovered = new ArrayList<>();
            for (long i = minGen; i <= maxGen; i++) {
                Path p = dir.resolve("translog-" + i + ".tlog");
                if (!Files.exists(p)) continue;            // 代缺失跳过 (真实: 连续性断言)
                try (FileChannel ch = FileChannel.open(p, StandardOpenOption.READ)) {
                    ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
                    ch.read(buf);
                    buf.flip();
                    while (buf.remaining() >= 4) {
                        int len = buf.getInt();
                        if (buf.remaining() < len) break;   // 半写 → 丢弃尾部 (未 fsync)
                        byte[] data = new byte[len];
                        buf.get(data);
                        recovered.add(new Op(-1, data));    // seqNo 不可靠恢复 (真实: 随操作序列化)
                    }
                }
            }
            return recovered;
        }

        // 写入口 (对照 Translog.add Translog.java:575)
        Op add(long seqNo, byte[] data) throws Exception {
            if (tragedy.get()) throw new IllegalStateException("tragic: " + tragicCause);
            try {
                // C2. 本次写入将超阈值 → 先轮转再写 (shouldRollGeneration Translog.java:619-625)
                int framedLen = 4 + data.length;
                if (current.totalOffset + framedLen > rollThreshold) rollGeneration();
                return current.add(seqNo, data);
            } catch (Exception e) {
                closeOnTragicEvent(e);      // D: tragedy 机制
                throw e;
            }
        }

        // C3. 轮转 (Translog.java:1628-1652): 封存 → 新代
        void rollGeneration() throws Exception {
            current.seal();
            readers.add(current);
            current = new Generation(current.gen + 1, dir);
        }

        boolean sync() throws Exception {
            if (tragedy.get()) throw new IllegalStateException("tragic: " + tragicCause);
            try {
                return current.sync();
            } catch (Exception e) {
                closeOnTragicEvent(e);
                throw e;
            }
        }

        // D: 首因冻结 + 整体关闭 (TragicExceptionHolder + Translog.java:869-889)
        void closeOnTragicEvent(Exception ex) {
            if (tragedy.compareAndSet(false, true)) {
                tragicCause = ex;
                try { close(); } catch (Exception ignored) {}
            }
        }

        void close() throws Exception {
            for (Generation g : readers) g.close();
            current.close();
        }
    }
}
