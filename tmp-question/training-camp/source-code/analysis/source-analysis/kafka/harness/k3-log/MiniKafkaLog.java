import java.util.ArrayList;
import java.util.List;

/**
 * MiniKafkaLog — K-3 Log 存储极简复现 (harness)
 *
 * 验证五个核心控制流 (对照 Kafka 4.1.2 源码):
 *   A. 分段 + append: 只写活动段尾部, LEO 推进
 *      (LocalLog.java:526-529 append+updateLogEndOffset; LogSegment.java:250-280)
 *   B. 稀疏索引 + 二分: 每 indexIntervalBytes 记一条 (offset→position), lookup 二分
 *      (LogSegment.java:270-274 稀疏索引; OffsetIndex.java:97-107 lookup)
 *   C. roll 轮转: 大小超限 → 封存旧段建新段
 *      (LocalLog.java:581-646 roll; LogSegment.java:167-173 shouldRoll)
 *   D. truncate: 删 baseOffset>target 段 + 活动段内截断 + LEO 重置
 *      (LocalLog.java:680-686 truncateTo)
 *   E. 恢复重建索引: 索引丢失后从数据文件重建 (数据是唯一真相)
 *      (LogLoader.java:400-420 recoverSegment; LogSegment.java:478-524 recover)
 *
 * 纯内存模拟 (无 mmap/磁盘), 保留核心控制流与数据结构。
 */
public class MiniKafkaLog {

    public static class FetchData {
        final long baseOffset;
        final byte[] data;
        FetchData(long baseOffset, byte[] data) { this.baseOffset = baseOffset; this.data = data; }
    }

    /** 单段: 数据 + 稀疏索引 (对照 LogSegment 四文件中的 .log + .index) */
    public static class Segment {
        final long baseOffset;
        final int indexIntervalBytes;
        final int maxBytes;                    // 段大小上限 (对照 segment.bytes)
        final List<byte[]> batches = new ArrayList<>();  // 每批数据 (batch = 消息集)
        final List<long[]> index = new ArrayList<>();    // {offset, position} 稀疏索引
        int position = 0;                      // 当前物理位置 (对照 log.sizeInBytes)
        int bytesSinceLastIndexEntry = 0;      // 对照 LogSegment.java:102
        long largestOffset = -1;

        Segment(long baseOffset, int indexIntervalBytes, int maxBytes) {
            this.baseOffset = baseOffset;
            this.indexIntervalBytes = indexIntervalBytes;
            this.maxBytes = maxBytes;
            // 对照 OffsetIndex 初始条目: 段创建时写首条索引 (baseOffset, 0)
            index.add(new long[]{baseOffset, 0});
        }

        /** 对照 LogSegment.append (LogSegment.java:250-280) */
        void append(long lastOffset, byte[] batch) {
            int startPosition = position;
            batches.add(batch);
            largestOffset = lastOffset;
            // 稀疏索引: 超过间隔才记一条 (LogSegment.java:270-274)
            if (bytesSinceLastIndexEntry > indexIntervalBytes) {
                index.add(new long[]{lastOffset, startPosition});
                bytesSinceLastIndexEntry = 0;
            }
            position += batch.length;
            bytesSinceLastIndexEntry += batch.length;
        }

        /** 对照 OffsetIndex.lookup 二分 (OffsetIndex.java:97-107): 找 ≤ target 的最大条目 */
        int translateOffset(long targetOffset) {
            int lo = 0, hi = index.size() - 1, ans = 0;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                long[] entry = index.get(mid);
                if (entry[0] <= targetOffset) { ans = (int) entry[1]; lo = mid + 1; }
                else hi = mid - 1;
            }
            return ans;
        }

        /** 对照 LogSegment.read (LogSegment.java:431-459): 从含 startOffset 的批开始, minOneMessage 保证至少一条 */
        FetchData read(long startOffset, int maxSize, boolean minOneMessage) {
            // 先定位含 startOffset 的批 (每批 offset = baseOffset+i)
            int firstBatch = batches.size();
            for (int i = 0; i < batches.size(); i++) {
                if (baseOffset + i >= startOffset) { firstBatch = i; break; }
            }
            if (firstBatch == batches.size()) return new FetchData(baseOffset, new byte[0]);
            int start = translateOffset(baseOffset + firstBatch);
            int size = 0;
            int pos = 0;
            for (int i = 0; i < batches.size(); i++) {
                byte[] b = batches.get(i);
                if (pos < start) { pos += b.length; continue; }
                if (i < firstBatch) { pos += b.length; continue; }
                if (size == 0 && minOneMessage) { size = b.length; pos += b.length; continue; }
                if (size + b.length > maxSize) break;
                size += b.length;
                pos += b.length;
            }
            byte[] out = new byte[size];
            int off = 0, skip = start;
            for (int i = 0; i < batches.size(); i++) {
                byte[] b = batches.get(i);
                if (i < firstBatch) continue;
                if (skip > 0) { skip -= b.length; continue; }
                if (off + b.length > out.length) break;
                System.arraycopy(b, 0, out, off, b.length);
                off += b.length;
            }
            return new FetchData(baseOffset, out);
        }

        /** 段内截断: 删 offset >= target 的批次 (对照 LogSegment.truncateTo L557) */
        void truncateTo(long targetOffset) {
            int cut = batches.size();
            int newPos = 0;
            for (int i = 0; i < batches.size(); i++) {
                // 批次的最大 offset 模拟: baseOffset + i (简化, 每批一个 offset)
                if (baseOffset + i < targetOffset) { newPos += batches.get(i).length; }
                else { cut = i; break; }
            }
            while (batches.size() > cut) batches.remove(batches.size() - 1);
            position = newPos;
            largestOffset = cut == 0 ? -1 : baseOffset + cut - 1;
            rebuildIndex();
        }

        /** 恢复: 从数据重建索引 (对照 LogSegment.recover L478-524 — 数据是唯一真相) */
        void rebuildIndex() {
            index.clear();
            bytesSinceLastIndexEntry = 0;
            int pos = 0;
            for (int i = 0; i < batches.size(); i++) {
                if (bytesSinceLastIndexEntry > indexIntervalBytes) {
                    index.add(new long[]{baseOffset + i, pos});
                    bytesSinceLastIndexEntry = 0;
                }
                pos += batches.get(i).length;
                bytesSinceLastIndexEntry += batches.get(i).length;
            }
            if (index.isEmpty() && !batches.isEmpty()) index.add(new long[]{baseOffset, 0}); // 首条兜底
        }
    }

    final int indexIntervalBytes;
    final int segmentMaxBytes;
    final List<Segment> segments = new ArrayList<>();
    long logEndOffset = 0;                     // LEO (对照 updateLogEndOffset)

    public MiniKafkaLog(int indexIntervalBytes, int segmentMaxBytes) {
        this.indexIntervalBytes = indexIntervalBytes;
        this.segmentMaxBytes = segmentMaxBytes;
        segments.add(new Segment(0, indexIntervalBytes, segmentMaxBytes));
    }

    Segment activeSegment() { return segments.get(segments.size() - 1); }

    /** 对照 LocalLog.append (LocalLog.java:526-529) */
    public void append(long lastOffset, byte[] batch) {
        if (activeSegment().position + batch.length > segmentMaxBytes) roll();
        activeSegment().append(lastOffset, batch);
        logEndOffset = lastOffset + 1;
    }

    /** 对照 LocalLog.roll (LocalLog.java:581-646): 封存旧段, 新段从 LEO 开始 */
    public Segment roll() {
        Segment next = new Segment(logEndOffset, indexIntervalBytes, segmentMaxBytes);
        segments.add(next);
        return next;
    }

    /** 对照 LocalLog.truncateTo (LocalLog.java:680-686) */
    public List<Segment> truncateTo(long targetOffset) {
        List<Segment> deleted = new ArrayList<>();
        for (int i = segments.size() - 1; i >= 0; i--) {
            Segment s = segments.get(i);
            if (s.baseOffset >= targetOffset) { deleted.add(s); segments.remove(i); }
        }
        activeSegment().truncateTo(targetOffset);
        logEndOffset = targetOffset;
        return deleted;
    }

    /** 对照 LogLoader.recoverSegment (LogLoader.java:400-420): 全部段重建索引 */
    public void recover() {
        for (Segment s : segments) s.rebuildIndex();
    }

    public FetchData read(long startOffset, int maxSize, boolean minOneMessage) {
        for (Segment s : segments) {
            if (startOffset >= s.baseOffset && (startOffset < s.baseOffset + s.batches.size() || s == activeSegment())) {
                FetchData fd = s.read(startOffset, maxSize, minOneMessage);
                if (fd != null && fd.data.length > 0) return fd;
            }
        }
        return null;
    }
}
