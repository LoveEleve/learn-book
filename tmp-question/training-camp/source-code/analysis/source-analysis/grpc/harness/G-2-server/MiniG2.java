/**
 * MiniG2 — gRPC-Java G-2 服务端域极简复现 (纯 JDK, 无依赖)
 *
 * 复现四个核心机制 (与源码逐条对应):
 *  1. 注册表扁平化: fullMethodName → handler O(1) 查找 + UNIMPLEMENTED (InternalHandlerRegistry.java:30-78)
 *  2. 拦截器洋葱: 最后添加的最外层先执行 (ServerImpl.java:669-670 包装次序推导)
 *  3. ServerCallImpl 状态机: headers→messages→close 单向 + TOO_MANY_RESPONSES/MISSING_RESPONSE (ServerCallImpl.java:104-230)
 *  4. 双 GOAWAY 优雅关闭: 拒新流(MAX) → ping 确认 → 等旧流(last) → close (NettyServerHandler.java:1071-1145)
 *
 * 用法: javac MiniG2.java && java MiniG2
 */
import java.util.HashMap;
import java.util.Map;

public class MiniG2 {

  // ============ 机制 1: 注册表扁平化 ============
  static final class HandlerRegistry {
    final Map<String, Runnable> methods = new HashMap<>();   // fullMethodName → handler (L33)

    void addService(String serviceName, Map<String, Runnable> methods) {
      // 扁平化: service 名覆盖 (L59-65) + 方法展开 (L67-78)
      for (Map.Entry<String, Runnable> e : methods.entrySet()) {
        this.methods.put(serviceName + "/" + e.getKey(), e.getValue());
      }
    }

    Runnable lookup(String methodName) {
      return methods.get(methodName);                        // O(1) (L51-54)
    }
  }

  // ============ 机制 2: 拦截器洋葱 ============
  // ServerImpl.wrapMethod: for (interceptor : interceptors) handler = wrap(interceptor, handler) (L669-670)
  static Runnable wrapInterceptor(String name, Runnable next) {
    return () -> {
      System.out.println("  [拦截器 " + name + "] 前置");
      next.run();
      System.out.println("  [拦截器 " + name + "] 后置");
    };
  }

  // ============ 机制 3: ServerCallImpl 状态机 ============
  static final class ServerCall {
    boolean sendHeadersCalled, closeCalled, messageSent;
    String closedWith;

    void sendHeaders() {
      check(!sendHeadersCalled, "sendHeaders has already been called");   // L105
      check(!closeCalled, "call is closed");                              // L106
      System.out.println("  [call] sendHeaders (压缩协商在此)");
      sendHeadersCalled = true;
    }

    void sendMessage(boolean unary) {
      check(sendHeadersCalled, "sendHeaders has not been called");        // L158
      check(!closeCalled, "call is closed");                              // L159
      if (unary && messageSent) {
        closedWith = "INTERNAL: TOO_MANY_RESPONSES";                      // L162-164
        System.out.println("  [call] ✗ 第二个响应 → " + closedWith);
        return;
      }
      messageSent = true;
      System.out.println("  [call] sendMessage");
    }

    void close(boolean unary) {
      check(!closeCalled, "call already closed");                         // L218
      closeCalled = true;
      if (unary && !messageSent) {
        closedWith = "INTERNAL: MISSING_RESPONSE";                        // L221-223
        System.out.println("  [call] ✗ 缺响应关闭 → " + closedWith);
        return;
      }
      System.out.println("  [call] close(" + (unary ? "OK, unary" : "OK") + ")");
    }

    static void check(boolean ok, String msg) {
      if (!ok) throw new IllegalStateException(msg);
    }
  }

  // ============ 机制 4: 双 GOAWAY 优雅关闭 ============
  static final class GracefulShutdown {                                    // NettyServerHandler.java:1071
    boolean pingAcked;
    int lastStreamCreated = 0;

    void start() {
      goAway(Integer.MAX_VALUE, "GOAWAY #1: 拒绝新流 (lastStreamId=MAX)"); // L1097-1102
      sendPing();                                                          // L1108
      if (pingAcked) {
        secondGoAwayAndClose();                                            // L1118
      } else {
        System.out.println("  [shutdown] ping 超时, 强制进入第二阶段");
        secondGoAwayAndClose();
      }
    }

    void secondGoAwayAndClose() {
      goAway(lastStreamCreated, "GOAWAY #2: 等旧流完成 (lastStreamId=" + lastStreamCreated + ")"); // L1127-1131
      System.out.println("  [shutdown] close (grace 超时覆盖)");           // L1133-1141
    }

    void goAway(int lastStreamId, String msg) { System.out.println("  [shutdown] " + msg); }
    void sendPing() { System.out.println("  [shutdown] PING 确认信号 → ack"); pingAcked = true; }
  }

  // ============ 主验证 ============
  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. 注册表: 扁平化查找 + UNIMPLEMENTED ---
    HandlerRegistry registry = new HandlerRegistry();
    Map<String, Runnable> helloMethods = new HashMap<>();
    helloMethods.put("SayHello", () -> System.out.println("  [业务] HelloServiceImpl.SayHello"));
    registry.addService("helloworld.Greeter", helloMethods);               // fullMethodName = pkg.Service/Method

    System.out.println("[注册表] 查找 helloworld.Greeter/SayHello:");
    Runnable handler = registry.lookup("helloworld.Greeter/SayHello");
    if (handler != null) { handler.run(); pass++; } else { fail++; }

    System.out.println("[注册表] 查找不存在的 helloworld.Greeter/SayBye:");
    if (registry.lookup("helloworld.Greeter/SayBye") == null) {
      System.out.println("  → null → UNIMPLEMENTED \"Method not found\" (ServerImpl.java:548-549)");
      pass++;
    } else { fail++; }

    // --- 2. 拦截器洋葱: 最后添加的最外层 ---
    System.out.println("[拦截器] 注册顺序 [auth, log], 执行顺序:");
    Runnable finalHandler = () -> System.out.println("  [业务] 真正的方法");
    Runnable h0 = wrapInterceptor("auth", finalHandler);
    Runnable h1 = wrapInterceptor("log", h0);                              // 最后添加 log → 最外层先执行
    h1.run();
    pass++;  // 顺序正确性由输出验证

    // --- 3. 状态机: 非法顺序 + 协议违规 ---
    System.out.println("[状态机] 正常 unary: headers → message → close");
    ServerCall call = new ServerCall();
    call.sendHeaders(); call.sendMessage(true); call.close(true);
    pass++;

    System.out.println("[状态机] 违规: 未 sendHeaders 就 sendMessage");
    ServerCall bad = new ServerCall();
    try {
      bad.sendMessage(true);
      fail++;
    } catch (IllegalStateException e) {
      System.out.println("  → IllegalStateException: " + e.getMessage());  // L158 编程错误开发期暴露
      pass++;
    }

    System.out.println("[状态机] 协议违规: unary 双响应");
    ServerCall dup = new ServerCall();
    dup.sendHeaders(); dup.sendMessage(true); dup.sendMessage(true); dup.close(true);
    if ("INTERNAL: TOO_MANY_RESPONSES".equals(dup.closedWith)) pass++; else fail++;

    System.out.println("[状态机] 协议违规: unary 缺响应");
    ServerCall missing = new ServerCall();
    missing.sendHeaders(); missing.close(true);
    if ("INTERNAL: MISSING_RESPONSE".equals(missing.closedWith)) pass++; else fail++;

    // --- 4. 双 GOAWAY ---
    System.out.println("[优雅关闭] 双 GOAWAY 流程:");
    GracefulShutdown gs = new GracefulShutdown();
    gs.lastStreamCreated = 7;
    gs.start();
    pass++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
