import java.util.*;
import java.util.concurrent.*;

public class MiniHttp12 {

    // ---- A: HttpChannel 抽象 (h1/h2 双实现) ----
    interface HttpChannel {
        CompletableFuture<Void> writeHeader(String contentType);
        CompletableFuture<Void> writeMessage(String body);
        String kind();
    }

    static class Http1Channel implements HttpChannel {
        public CompletableFuture<Void> writeHeader(String ct) { return CompletableFuture.completedFuture(null); }
        public CompletableFuture<Void> writeMessage(String body) { return CompletableFuture.completedFuture(null); }
        public String kind() { return "h1"; }
    }

    static class H2StreamChannel implements HttpChannel {
        public CompletableFuture<Void> writeHeader(String ct) { return CompletableFuture.completedFuture(null); }
        public CompletableFuture<Void> writeMessage(String body) { return CompletableFuture.completedFuture(null); }
        public String kind() { return "h2"; }
    }

    // ---- B: mediaType 驱动 codec 族 ----
    interface Codec { String name(); byte[] encode(String s); }
    static class BinaryCodec implements Codec { public String name() { return "binary"; } public byte[] encode(String s) { return s.getBytes(); } }
    static class JsonCodec implements Codec { public String name() { return "json"; } public byte[] encode(String s) { return ("{\"" + s + "\":1}").getBytes(); } }

    static class CodecUtils {
        static final Map<String, Codec> FACTORIES = new HashMap<>();
        static { FACTORIES.put("application/json", new JsonCodec()); FACTORIES.put("application/octet-stream", new BinaryCodec()); }
        static Codec determine(String mediaType) {
            Codec c = FACTORIES.get(mediaType);
            if (c == null) throw new RuntimeException("UnsupportedMediaType: " + mediaType);
            return c;
        }
    }

    // ---- C: 双栈协议选择 (首帧判定) ----
    static class ProtocolSelectorHandler {
        static String select(byte[] firstFrame) {
            // h2 preface: "PRI * HTTP/2.0" 开头
            String s = new String(firstFrame);
            return s.startsWith("PRI") ? "h2" : "h1";
        }
    }

    // ---- D: 写命令队列 (背压) ----
    interface QueueCommand { String describe(); }
    static class DataQueueCommand implements QueueCommand { final String data; DataQueueCommand(String d) { data = d; } public String describe() { return "DATA(" + data + ")"; } }
    static class HeaderQueueCommand implements QueueCommand { public String describe() { return "HEADER"; } }
    static class ResetQueueCommand implements QueueCommand { public String describe() { return "RESET"; } }

    static class HttpWriteQueue {
        final Queue<QueueCommand> queue = new ArrayDeque<>();
        void add(QueueCommand cmd) { queue.add(cmd); } // 背压: 队列化写操作
        String drain() {
            StringBuilder sb = new StringBuilder();
            while (!queue.isEmpty()) sb.append(queue.poll().describe()).append(" ");
            return sb.toString().trim();
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: HttpChannel 抽象 ============
        HttpChannel h1 = new Http1Channel();
        HttpChannel h2 = new H2StreamChannel();
        h1.writeHeader("text/plain").join();
        h1.writeMessage("hello").join();
        System.out.println("A1 channel: " + h1.kind() + " (完整消息) vs " + h2.kind() + " (流式帧)");

        // ============ B: mediaType 驱动 codec ============
        Codec json = CodecUtils.determine("application/json");
        Codec bin = CodecUtils.determine("application/octet-stream");
        System.out.println("B1 codec  : json→" + new String(json.encode("a")) + ", binary→" + new String(bin.encode("a")));

        // ============ C: 双栈选择 ============
        System.out.println("C1 select : " + ProtocolSelectorHandler.select("PRI * HTTP/2.0\r\n".getBytes()) + " (首帧判定)");
        System.out.println("C2 select : " + ProtocolSelectorHandler.select("GET / HTTP/1.1".getBytes()));

        // ============ D: 写命令队列 ============
        HttpWriteQueue queue = new HttpWriteQueue();
        queue.add(new HeaderQueueCommand());
        queue.add(new DataQueueCommand("body-1"));
        queue.add(new ResetQueueCommand()); // h2 流重置
        System.out.println("D1 queue  : " + queue.drain());

        // 断言
        pass += h1.kind().equals("h1") && h2.kind().equals("h2") ? 1 : 0;
        pass += new String(CodecUtils.determine("application/json").encode("a")).startsWith("{") ? 1 : 0;
        pass += ProtocolSelectorHandler.select("PRI * HTTP/2.0\r\n".getBytes()).equals("h2") ? 1 : 0;
        pass += ProtocolSelectorHandler.select("GET / HTTP/1.1".getBytes()).equals("h1") ? 1 : 0;
        pass += queue.drain().equals("") && queue.queue.isEmpty() ? 1 : 0; // D1 已排空
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
