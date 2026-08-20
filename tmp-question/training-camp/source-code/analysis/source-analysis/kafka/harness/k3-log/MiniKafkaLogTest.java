import java.util.List;

public class MiniKafkaLogTest {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) System.out.println("PASS " + name);
        else { System.out.println("FAIL " + name); failures++; }
    }

    static byte[] batchOf(long offset, int size) {
        byte[] b = new byte[size];
        b[0] = (byte) offset;
        return b;
    }

    public static void main(String[] args) {
        testAppendAndLEO();
        testSparseIndexAndBinarySearch();
        testRoll();
        testTruncate();
        testRecoveryRebuildIndex();
        testMinOneMessage();
        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** A. append 写活动段 + LEO 推进 (LocalLog.java:526-529) */
    static void testAppendAndLEO() {
        MiniKafkaLog log = new MiniKafkaLog(16, 1024);
        log.append(0, batchOf(0, 4));
        log.append(1, batchOf(1, 4));
        check("LEO 推进", log.logEndOffset == 2);
        check("活动段最大 offset", log.activeSegment().largestOffset == 1);
        MiniKafkaLog.FetchData fd = log.read(0, 8, false);
        check("读到数据", fd != null && fd.data.length == 8);
    }

    /** B. 稀疏索引: 小间隔下每条都有条目, 大间隔只有部分条目; 二分定位正确 */
    static void testSparseIndexAndBinarySearch() {
        MiniKafkaLog log = new MiniKafkaLog(4, 4096); // 每批 4 字节, 间隔 4 → 首条 + 隔批记
        for (long i = 0; i < 5; i++) log.append(i, batchOf(i, 4));
        check("密集索引条目 (首条+2)", log.activeSegment().index.size() == 3);

        MiniKafkaLog sparse = new MiniKafkaLog(32, 4096); // 间隔 32 > 批大小 4 → 隔 8 批一条
        for (long i = 0; i < 40; i++) sparse.append(i, batchOf(i, 4));
        check("稀疏索引条目少", sparse.activeSegment().index.size() < 10);
        // 二分定位: translateOffset(30) 返回 ≤30 的最大索引条目位置
        int pos = sparse.activeSegment().translateOffset(30);
        check("二分定位", pos >= 0 && pos <= 30 * 4);
        check("二分不越界", sparse.activeSegment().translateOffset(0) == 0);
    }

    /** C. roll: 超 segmentMaxBytes 自动滚段, 新段 baseOffset = LEO */
    static void testRoll() {
        MiniKafkaLog log = new MiniKafkaLog(16, 32); // 段上限 32 字节
        for (long i = 0; i < 10; i++) log.append(i, batchOf(i, 8));
        check("自动滚段", log.segments.size() > 1);
        check("新段 baseOffset=LEO", log.segments.get(1).baseOffset == 4);
        // 手动 roll (LocalLog.roll L581)
        MiniKafkaLog.Segment s = log.roll();
        check("手动 roll 新段", s.baseOffset == log.logEndOffset);
    }

    /** D. truncate: 删段 + 段内截断 + LEO 重置 (LocalLog.java:680-686) */
    static void testTruncate() {
        MiniKafkaLog log = new MiniKafkaLog(16, 32);
        for (long i = 0; i < 10; i++) log.append(i, batchOf(i, 8));
        int segCount = log.segments.size();
        List<MiniKafkaLog.Segment> deleted = log.truncateTo(3);
        check("删段", segCount - deleted.size() == log.segments.size());
        check("LEO 重置", log.logEndOffset == 3);
        check("读截断后", log.read(3, 8, false) == null || log.read(3, 8, false).data.length == 0);
    }

    /** E. 恢复: 索引清空后 rebuildIndex 重建, 读仍正确 (LogLoader.java:400-420) */
    static void testRecoveryRebuildIndex() {
        MiniKafkaLog log = new MiniKafkaLog(16, 32);
        for (long i = 0; i < 6; i++) log.append(i, batchOf(i, 4));
        // 模拟崩溃: 索引丢失
        for (MiniKafkaLog.Segment s : log.segments) s.index.clear();
        log.recover();
        check("恢复后索引重建", log.activeSegment().index.size() > 0);
        MiniKafkaLog.FetchData fd = log.read(0, 100, false);
        check("恢复后可读", fd != null && fd.data.length == 24);
    }

    /** 读路径: minOneMessage 保证第一条即使超 maxSize 也返回 (LogSegment.java:445-446) */
    static void testMinOneMessage() {
        MiniKafkaLog log = new MiniKafkaLog(16, 1024);
        log.append(0, batchOf(0, 100));
        MiniKafkaLog.FetchData fd = log.read(0, 10, true);
        check("minOneMessage 返回第一条", fd != null && fd.data.length == 100);
        MiniKafkaLog.FetchData fd2 = log.read(0, 10, false);
        check("非 minOneMessage 截断", fd2 == null || fd2.data.length == 0);
    }
}
