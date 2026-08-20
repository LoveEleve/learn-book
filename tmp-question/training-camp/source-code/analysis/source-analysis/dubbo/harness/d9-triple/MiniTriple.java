import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniTriple {

    // ---- A: 协议装配 — pathResolver (gRPC 路径) + REST 双协议 ----
    static class PathResolver {
        final Map<String, String> paths = new ConcurrentHashMap<>();
        void register(String path, String service) { paths.put(path, service); }
        void unregister(String path) { paths.remove(path); }
        String resolve(String path) { return paths.get(path); }
    }

    // ---- B: 调用模式 — unary/server-stream/bi-stream + ThreadlessExecutor ----
    static class ThreadlessExecutor extends AbstractExecutorService {
        final java.util.Queue<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable r) { tasks.add(r); }
        Object waitAndDrain(long deadlineMs) {
            long end = System.currentTimeMillis() + deadlineMs;
            while (System.currentTimeMillis() < end) {
                Runnable r = tasks.poll();
                if (r != null) { r.run(); return "SYNC_RESULT"; }
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            }
            return "TIMEOUT";
        }
        public void shutdown() {}
        public List<Runnable> shutdownNow() { return Collections.emptyList(); }
        public boolean isShutdown() { return false; }
        public boolean isTerminated() { return false; }
        public boolean awaitTermination(long t, TimeUnit u) { return true; }
    }

    static class TripleInvoker {
        static Object doInvoke(String mode) {
            ThreadlessExecutor executor = new ThreadlessExecutor(); // isSync
            if ("unary".equals(mode)) {
                executor.execute(() -> {}); // 模拟回调任务 (响应回填)
                return "UNARY:" + executor.waitAndDrain(1000);
            }
            return "STREAM:" + mode; // server-stream / bi-client-stream
        }
    }

    // ---- C: 传输 — PING + GOAWAY ----
    static class PingPongHandler {
        static String onPing() { return "PONG"; } // TriplePingPongHandler
    }
    static class GoAwayHandler {
        static String gracefulShutdown() { return "GOAWAY(drain in-flight)"; } // TripleGoAwayHandler
    }

    // ---- D: gRPC 兼容 + protobuf ----
    static class SingleProtobufUtils {
        static byte[] serialize(String obj) { return obj.getBytes(); } // writeTo
        static String deserialize(byte[] data) { return new String(data); } // parseFrom
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: pathResolver ============
        PathResolver resolver = new PathResolver();
        resolver.register("/org.apache.HelloService/sayHello", "HelloService");
        System.out.println("A1 path   : " + resolver.resolve("/org.apache.HelloService/sayHello") + " (gRPC 风格路径)");

        // ============ B: 调用模式 ============
        System.out.println("B1 unary  : " + TripleInvoker.doInvoke("unary") + " (ThreadlessExecutor 零线程同步)");
        System.out.println("B2 stream : " + TripleInvoker.doInvoke("server-stream"));

        // ============ C: PING/GOAWAY ============
        System.out.println("C1 ping   : " + PingPongHandler.onPing());
        System.out.println("C2 goaway : " + GoAwayHandler.gracefulShutdown());

        // ============ D: protobuf ============
        byte[] data = SingleProtobufUtils.serialize("hello");
        System.out.println("D1 proto  : " + SingleProtobufUtils.deserialize(data) + " (writeTo/parseFrom)");

        // 断言
        pass += resolver.resolve("/org.apache.HelloService/sayHello") != null ? 1 : 0;
        pass += TripleInvoker.doInvoke("unary").equals("UNARY:SYNC_RESULT") ? 1 : 0; // Threadless drain
        pass += TripleInvoker.doInvoke("server-stream").toString().contains("STREAM") ? 1 : 0;
        pass += PingPongHandler.onPing().equals("PONG") ? 1 : 0;
        pass += SingleProtobufUtils.deserialize(SingleProtobufUtils.serialize("hello")).equals("hello") ? 1 : 0;
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
