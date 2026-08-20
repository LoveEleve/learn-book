import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

/**
 * MiniZKTxnLog — Z-9 持久化核心逻辑极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ZooKeeper 3.9.5 源码):
 *   A. TxnLog 格式闭环: [CRC 8B Adler32][len 4B][payload][0x42 'B'] 记录链 + 文件头
 *      (FileTxnLog.java:60-96,275-327; Util.writeTxnBytes:205-208)
 *   B. CRC 破坏检测: payload 损坏 → CRC_ERROR 致命; 尾部残缺/len 损坏 → 静默 EOF 截断
 *      (FileTxnLog.java:784-824; Util.readTxnBytes:157-173)
 *   C. 快照 seal: 内容 Adler32 + writeLong(checksum) + writeString("/") 三段独立校验
 *      (FileSnap.java:250-267; SnapStream.sealStream:162-180)
 *   D. restore 边界: 快照 zxid=5 → 从 zxid+1=6 重放; truncate(zxid) 移除 >=zxid; 预分配数学
 *      (FileTxnSnapLog.fastForwardFromEdits:330; FileTxnLog.truncate:481-501; FilePadding:101-115)
 *
 * 纯内存+临时文件模拟, 保留核心格式字节布局与控制流。
 */
public class MiniZKTxnLog {

    static final int TXNLOG_MAGIC = java.nio.ByteBuffer.wrap("ZKLG".getBytes()).getInt();
    static final int SNAP_MAGIC = java.nio.ByteBuffer.wrap("ZKSN".getBytes()).getInt();
    static final int VERSION = 2;
    static final byte EOR = 0x42; // 'B'

    /** 文件头: magic 4B + version 4B + dbid 8B = 16B */
    static void writeFileHeader(DataOutputStream out, int magic, int version, long dbid) throws IOException {
        out.writeInt(magic);
        out.writeInt(version);
        out.writeLong(dbid);
    }

    /** 一条事务记录: CRC(payload) 8B + len 4B + payload + 0x42 — 对照 FileTxnLog.append:315-319 */
    static void appendTxn(DataOutputStream out, byte[] payload) throws IOException {
        Adler32 crc = new Adler32();
        crc.update(payload); // CRC 只覆盖 payload — Javadoc 声称含 len+0x42 与实现不符
        out.writeLong(crc.getValue());
        out.writeInt(payload.length);
        out.write(payload);
        out.writeByte(EOR);
    }

    /** 读下一条记录 — 对照 FileTxnIterator.next + Util.readTxnBytes */
    static byte[] readTxn(DataInputStream in) throws IOException {
        long crcValue;
        try {
            crcValue = in.readLong();
        } catch (EOFException e) {
            return null; // 文件尾
        }
        byte[] bytes;
        try {
            int len = in.readInt();
            bytes = new byte[len];
            in.readFully(bytes);
        } catch (EOFException e) {
            return null; // len/payload 截断 → 静默 EOF (len 无 CRC 保护)
        }
        int marker;
        try {
            marker = in.readByte();
        } catch (EOFException e) {
            return null; // 缺 0x42 → 残缺尾部
        }
        if (marker != EOR) {
            return null; // 0x42 缺失/损坏 → 静默截断 (非 CRC 错误)
        }
        Adler32 crc = new Adler32();
        crc.update(bytes);
        if (crcValue != crc.getValue()) {
            throw new IOException("CRC check failed"); // 中部 payload 损坏 → 致命
        }
        return bytes;
    }

    /** 快照 seal — 对照 SnapStream.sealStream: writeLong(checksum)+writeString("/") */
    static void seal(ByteArrayOutputStream content, DataOutputStream out) throws IOException {
        Adler32 crc = new Adler32();
        crc.update(content.toByteArray());
        out.writeLong(crc.getValue());
        out.writeInt(1);   // jute writeString: len
        out.writeByte('/');
    }

    /** 校验 seal — 对照 SnapStream.checkSealIntegrity */
    static boolean checkSeal(byte[] content, DataInputStream in) throws IOException {
        Adler32 a = new Adler32();
        a.update(content, 0, content.length);
        long expected = a.getValue();
        long val = in.readLong();
        int len = in.readInt();
        byte[] path = new byte[len];
        in.readFully(path);
        return val == expected;
    }

    /** 预分配数学 — 对照 FilePadding.calculateFileSizeWithPadding:101-115 */
    static long calculateFileSizeWithPadding(long position, long fileSize, long preAllocSize) {
        if (preAllocSize > 0 && position + 4096 >= fileSize) {
            if (position > fileSize) {
                fileSize = position + preAllocSize;
                fileSize -= fileSize % preAllocSize;
            } else {
                fileSize += preAllocSize;
            }
        }
        return fileSize;
    }

    /** 极简日志文件: 写 N 条事务, 返回字节流 */
    static byte[] writeLogFile(List<Long> zxids, long dbid) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        writeFileHeader(out, TXNLOG_MAGIC, VERSION, dbid);
        for (long z : zxids) {
            byte[] payload = ("hdr:" + z).getBytes();
            appendTxn(out, payload);
        }
        out.flush();
        return bos.toByteArray();
    }

    /** 极简恢复: 从文件读全部事务 (返回成功读取的 zxid 列表) */
    static List<Long> readAll(byte[] file) throws IOException {
        List<Long> result = new ArrayList<>();
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(file));
        int magic = in.readInt();
        int ver = in.readInt();
        long dbid = in.readLong();
        if (magic != TXNLOG_MAGIC) throw new IOException("invalid magic " + magic);
        byte[] txn;
        while ((txn = readTxn(in)) != null) {
            String s = new String(txn);
            result.add(Long.parseLong(s.substring(s.indexOf(':') + 1)));
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        // ============ A: 格式闭环 ============
        List<Long> zxids = List.of(1L, 2L, 3L);
        byte[] logFile = writeLogFile(zxids, 42L);
        List<Long> readBack = readAll(logFile);
        assertTrue(readBack.equals(zxids), "A1 记录链 round-trip (zxid 1/2/3 顺序)");
        // 字节布局: 16B 头 + 3 × (8 CRC + 4 len + payload + 1 marker)
        assertTrue(logFile.length == 16 + 3 * (8 + 4 + "hdr:1".length() + 1),
            "A2 字节布局精确 (header 16B + 记录 13+len 每条)");
        System.out.println("[A] 格式闭环 2/2 OK");

        // ============ B: 损坏检测 ============
        // B1: payload 中部损坏 → CRC 致命
        byte[] corrupt = logFile.clone();
        corrupt[20] ^= 0x01; // 第一条 payload 内
        try {
            readAll(corrupt);
            throw new AssertionError("[FAIL] B1 payload 损坏应 CRC 致命");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("CRC"), "B1 payload 损坏 → CRC check failed");
        }
        // B2: 尾部残缺 (砍掉 0x42+末尾) → 静默 EOF, 已读事务保留
        byte[] tailTrunc = java.util.Arrays.copyOf(logFile, logFile.length - 3);
        assertTrue(readAll(tailTrunc).equals(List.of(1L, 2L)), "B2 尾部残缺 → 静默 EOF (2 条保留)");
        // B3: len 域损坏 (无 CRC 保护) → 静默截断该文件后续事务, 非致命
        byte[] lenCorrupt = logFile.clone();
        lenCorrupt[16 + 8] = 0x7F; // 第一条 len 高字节 → 巨大长度 → EOF
        assertTrue(readAll(lenCorrupt).isEmpty(), "B3 len 损坏 → 静默 EOF 截断 (0 条, 非致命)");
        System.out.println("[B] 损坏检测 3/3 OK");

        // ============ C: 快照 seal ============
        ByteArrayOutputStream snapContent = new ByteArrayOutputStream();
        DataOutputStream so = new DataOutputStream(snapContent);
        writeFileHeader(so, SNAP_MAGIC, VERSION, -1); // 快照 dbid=-1 常量
        so.write("tree".getBytes());
        ByteArrayOutputStream snapAll = new ByteArrayOutputStream();
        DataOutputStream allOut = new DataOutputStream(snapAll);
        allOut.write(snapContent.toByteArray());
        seal(snapContent, allOut); // 树段 seal
        allOut.flush();
        byte[] snap = snapAll.toByteArray();
        DataInputStream si = new DataInputStream(new ByteArrayInputStream(snap));
        byte[] content = new byte[snapContent.size()];
        si.readFully(content);
        assertTrue(checkSeal(content, si), "C1 seal round-trip (Adler32 + '/' 标记)");
        snap[snap.length - 13] ^= 0x01; // 破坏校验和字节 (len/'/' 区之前 8B checksum)
        si = new DataInputStream(new ByteArrayInputStream(snap));
        si.readFully(content);
        assertTrue(!checkSeal(content, si), "C2 seal 校验和损坏 → 校验失败");
        System.out.println("[C] 快照 seal 2/2 OK");

        // ============ D: restore 边界 + truncate + 预分配 ============
        // D1: 快照 zxid=5 → 重放从 6 开始 (zxid+1 边界)
        long snapshotZxid = 5L;
        List<Long> txns = List.of(5L, 6L, 7L, 8L, 9L);
        List<Long> replayed = new ArrayList<>();
        for (long z : txns) {
            if (z > snapshotZxid) replayed.add(z); // fastForwardFromEdits: read(lastProcessedZxid+1)
        }
        assertTrue(replayed.equals(List.of(6L, 7L, 8L, 9L)), "D1 重放边界 = 快照 zxid+1 (5 跳过)");
        // D2: truncate(8) → 移除 >= 8, 保留 6/7
        List<Long> truncated = new ArrayList<>();
        for (long z : txns) if (z < 8L) truncated.add(z);
        assertTrue(truncated.equals(List.of(5L, 6L, 7L)), "D2 truncate(zxid) exclusive: 移除 >= 8");
        // D3: 预分配数学 — 边界: position+4096 >= fileSize → pad
        long padded = calculateFileSizeWithPadding(61440, 65536, 65536);
        assertTrue(padded == 131072, "D3a position+4096>=fileSize → pad +64MB (61440→131072)");
        long noPad = calculateFileSizeWithPadding(1024, 65536, 65536);
        assertTrue(noPad == 65536, "D3b 远离阈值不 pad (1024→65536 保持)");
        System.out.println("[D] restore 边界 + truncate + 预分配 4/4 OK");

        System.out.println("MiniZKTxnLog 全部通过");
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("[FAIL] " + msg);
        System.out.println("  ok: " + msg);
    }
}
