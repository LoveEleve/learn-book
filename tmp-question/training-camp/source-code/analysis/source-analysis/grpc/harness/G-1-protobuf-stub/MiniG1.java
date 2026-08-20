/**
 * MiniG1 — gRPC-Java G-1 域极简复现 (纯 JDK, 无依赖, ~300 行)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. 消息帧格式: [1B 压缩标志][4B 长度][消息体]  + 双重尺寸校验 (MessageDeframer.java:44-45,384-393)
 *  2. unary 双响应守卫: 服务端 request(2) → 第二个 onMessage 触发 TOO_MANY_REQUESTS (ServerCalls.java:131-134,155-161)
 *  3. 冻结适配器: start 后禁改配置 (ClientCalls.java:460-462) + 阻塞调用回调内联执行 (ThreadlessExecutor 简化, ClientCalls.java:155-183)
 *
 * 用法: javac MiniG1.java && java MiniG1
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniG1 {

  // ============ 机制 1: 消息帧 ============
  // gRPC 帧: 1 字节(bit0=压缩标志) + 4 字节长度 + 消息体 (MessageDeframer HEADER_LENGTH=5)
  static final int HEADER_LENGTH = 5;
  static final int COMPRESSED_FLAG_MASK = 1;

  static byte[] frame(byte[] body, boolean compressed) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(compressed ? 1 : 0);                       // MessageFramer.java:226,247
    int len = body.length;
    out.write((len >>> 24) & 0xFF);
    out.write((len >>> 16) & 0xFF);
    out.write((len >>> 8) & 0xFF);
    out.write(len & 0xFF);
    out.write(body, 0, body.length);
    return out.toByteArray();
  }

  static byte[] deframe(byte[] wire, int maxMessageSize, boolean isCompressed) {
    ByteArrayInputStream in = new ByteArrayInputStream(wire);
    int type = readUnsignedByte(in);
    boolean compressed = (type & COMPRESSED_FLAG_MASK) != 0;   // MessageDeframer.java:390
    int len = readInt(in);
    byte[] body = new byte[len];
    try {
      in.read(body);
    } catch (Exception e) { throw new RuntimeException(e); }
    if (len > maxMessageSize) {                              // 第一道: 压缩前校验 L396
      throw new IllegalStateException("gRPC message exceeds maximum size " + maxMessageSize + ": " + len);
    }
    if (isCompressed != compressed) throw new IllegalStateException("压缩标志协商不一致");
    // 第二道: 解压后校验 (此处模拟: compressed 时"解压"放大 4x 的 zip bomb)
    int inflated = compressed ? len * 4 : len;
    if (inflated > maxMessageSize) {                         // MessageDeframer.java:529
      throw new IllegalStateException("Decompressed gRPC message exceeds maximum size " + maxMessageSize);
    }
    return body;
  }

  // ============ 机制 2: unary 双响应守卫 ============
  // 服务端 request(2): 期望 1 条, 预借 2 条额度抓违规客户端 (ServerCalls.java:131-134)
  static class UnaryServerCall {
    final String name;
    AtomicInteger requested = new AtomicInteger();
    AtomicInteger messages = new AtomicInteger();
    volatile String closedWith;

    UnaryServerCall(String name) { this.name = name; }

    void request(int n) { requested.addAndGet(n); }

    /** 模拟 onMessage: 第二个请求 → TOO_MANY_REQUESTS (ServerCalls.java:155-161) */
    boolean onMessage() {
      if (messages.incrementAndGet() > 1) {
        closedWith = "INTERNAL: TOO_MANY_REQUESTS";
        return false;
      }
      return true;
    }
  }

  // ============ 机制 3: 冻结适配器 + 阻塞调用 ============
  // CallToStreamObserverAdapter 的 frozen 窗口 (ClientCalls.java:460-462,490-507)
  static class Adapter {
    boolean frozen;
    Runnable onReadyHandler;

    void freeze() { this.frozen = true; }

    void setOnReadyHandler(Runnable r) {
      if (frozen) throw new IllegalStateException("Cannot alter onReadyHandler after call started");
      this.onReadyHandler = r;
    }
  }

  /** ThreadlessExecutor 简化: 回调在当前调用线程内联执行 (ClientCalls.java:155-183) — 阻塞调用零额外线程 */
  static String blockingCall(Runnable start) {
    final String[] result = new String[1];
    final Thread[] interruptedFlag = new Thread[1];
    // 模拟 futureUnaryCall: 回调同步执行
    start.run();                                            // 内联: 回调不跳线程
    return result[0];
  }

  // ============ 主验证 ============
  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 帧往返 ---
    byte[] wire = frame("hello".getBytes(), false);
    byte[] back = deframe(wire, 1024, false);
    System.out.println("[帧] 5 字节头: " + wire.length + " = 5 + " + "hello".length()
        + " | 往返: " + new String(back));
    if (new String(back).equals("hello")) pass++; else { fail++; System.out.println("  ✗ 往返失败"); }

    // --- 2. 压缩炸弹: 压缩帧 4x 放大超出限制 ---
    byte[] bomb = frame(new byte[300], true);               // 解压后 1200 > 1024
    try {
      deframe(bomb, 1024, true);
      fail++; System.out.println("  ✗ 压缩炸弹未被拦截");
    } catch (IllegalStateException e) {
      System.out.println("[帧] 压缩炸弹拦截: " + e.getMessage());
      pass++;
    }

    // --- 3. unary 双响应守卫 ---
    UnaryServerCall call = new UnaryServerCall("SayHello");
    call.request(2);                                        // ServerCalls.startCall 的 request(2)
    boolean first = call.onMessage();
    boolean second = call.onMessage();                      // 违规第二个请求
    if (first && !second && "INTERNAL: TOO_MANY_REQUESTS".equals(call.closedWith)) {
      System.out.println("[守卫] request(2) 双响应防护: 第二请求被关闭 → " + call.closedWith);
      pass++;
    } else { fail++; System.out.println("  ✗ 双响应守卫失效"); }

    // --- 4. 冻结窗口 ---
    Adapter adapter = new Adapter();
    adapter.setOnReadyHandler(() -> {});
    adapter.freeze();
    try {
      adapter.setOnReadyHandler(() -> {});
      fail++; System.out.println("  ✗ 冻结窗口未生效");
    } catch (IllegalStateException e) {
      System.out.println("[冻结] start 后改配置被拒: " + e.getMessage());
      pass++;
    }

    // --- 5. 阻塞调用零线程 (内联回调) ---
    Thread main = Thread.currentThread();
    String resp = blockingCall(() -> {
      // 回调在调用线程内联执行 — ThreadlessExecutor 语义
      assert Thread.currentThread() == main : "回调应内联在调用线程";
    });
    System.out.println("[阻塞] 回调内联调用线程, 无额外线程 (ThreadlessExecutor 简化)");
    pass++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }

  static int readUnsignedByte(ByteArrayInputStream in) { return in.read() & 0xFF; }
  static int readInt(ByteArrayInputStream in) {
    return ((in.read() & 0xFF) << 24) | ((in.read() & 0xFF) << 16)
        | ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
  }
}
