import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class MiniRemoting {

    // ---- B: exchange 异步 — Request id + DefaultFuture (id→future 表 + 回填) ----
    static class Request {
        static final AtomicLong INVOKE_ID = new AtomicLong(0);
        final long mId;
        final boolean heartbeat;
        Request(boolean heartbeat) { this.mId = INVOKE_ID.incrementAndGet(); this.heartbeat = heartbeat; } // newId
        public String toString() { return "Request#" + mId + (heartbeat ? "(heartbeat)" : ""); }
    }

    static class Response {
        final long id;
        final String payload;
        Response(long id, String payload) { this.id = id; this.payload = payload; }
    }

    static class DefaultFuture extends CompletableFuture<Object> { // extends CompletableFuture (L51)
        static final Map<Long, DefaultFuture> FUTURES = new ConcurrentHashMap<>(); // L63
        final long id;
        DefaultFuture(Request req) { this.id = req.mId; FUTURES.put(id, this); }

        static void received(Response resp) { // L196-209
            DefaultFuture f = FUTURES.remove(resp.id);
            if (f != null) f.complete(resp.payload);
        }
    }

    static class HeaderExchangeChannel {
        static DefaultFuture request(Request req, long timeoutMs) throws Exception {
            DefaultFuture future = new DefaultFuture(req); // DefaultFuture.newFuture
            // 模拟网络发送 (异步: 响应回来前返回)
            if (!req.heartbeat) {
                new Thread(() -> {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    DefaultFuture.received(new Response(req.mId, "RESULT(" + req.mId + ")"));
                }).start();
            }
            return future;
        }
    }

    // ---- A: 传输抽象 — Transporter SPI (URL 参数选实现) ----
    interface Transporter { String name(); }
    static class Netty4Transporter implements Transporter { public String name() { return "netty4"; } }
    static class Netty3Transporter implements Transporter { public String name() { return "netty3"; } }

    static class Transporters {
        static final Map<String, Transporter> SPI = new HashMap<>();
        static {
            SPI.put("netty4", new Netty4Transporter());
            SPI.put("netty3", new Netty3Transporter());
        }
        static Transporter getTransporter(String urlParam) { return SPI.get(urlParam); } // URL 自适应
    }

    // ---- C: 心跳 — 成对检测 + Timer ----
    static class HeartbeatHandler {
        static String received(Object msg) {
            if (msg instanceof Request) {
                Request req = (Request) msg;
                if (req.heartbeat) return "HEARTBEAT_RESPONSE(" + req.mId + ")"; // 心跳响应
            }
            return null;
        }
    }

    // ---- D: 线程模型 — Dispatcher 5 实现 ----
    interface Dispatcher { String name(); }
    static class AllDispatcher implements Dispatcher { public String name() { return "all"; } }
    static class DirectDispatcher implements Dispatcher { public String name() { return "direct"; } }
    static class ExecutionDispatcher implements Dispatcher { public String name() { return "execution"; } }

    static final Map<String, Dispatcher> DISPATCHERS = new HashMap<>();
    static {
        DISPATCHERS.put("all", new AllDispatcher());
        DISPATCHERS.put("direct", new DirectDispatcher());
        DISPATCHERS.put("execution", new ExecutionDispatcher());
    }

    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;
        // ============ A: 传输抽象 — URL 选实现 ============
        System.out.println("A1 spi    : " + Transporters.getTransporter("netty4").name() + " (默认) / " + Transporters.getTransporter("netty3").name());

        // ============ B: exchange 异步 — id + future 回填 ============
        Request req = new Request(false);
        DefaultFuture future = HeaderExchangeChannel.request(req, 1000);
        Object result = future.get(2, TimeUnit.SECONDS); // 同步等待 (recreate 语义)
        System.out.println("B1 async  : " + req + " → " + result + ", FUTURES.size=" + DefaultFuture.FUTURES.size());
        System.out.println("B2 id     : id1=" + new Request(false).mId + ", id2=" + new Request(false).mId + " (递增)");

        // ============ C: 心跳 ============
        Request heartbeat = new Request(true);
        System.out.println("C1 beat   : " + HeartbeatHandler.received(heartbeat) + " (成对)");

        // ============ D: 线程模型 ============
        System.out.println("D1 disp   : 默认=" + DISPATCHERS.get("all").name() + ", 可切=" + DISPATCHERS.get("execution").name());

        // 断言
        pass += Transporters.getTransporter("netty4").name().equals("netty4") ? 1 : 0;
        pass += result.equals("RESULT(" + req.mId + ")") && DefaultFuture.FUTURES.isEmpty() ? 1 : 0; // 回填后移除
        pass += new Request(false).mId > req.mId ? 1 : 0; // id 递增
        pass += HeartbeatHandler.received(heartbeat) != null ? 1 : 0;
        pass += DISPATCHERS.get("all") != null && DISPATCHERS.get("execution") != null ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
