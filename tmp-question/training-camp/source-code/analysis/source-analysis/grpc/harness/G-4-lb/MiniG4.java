/**
 * MiniG4 — gRPC-Java G-4 负载均衡域极简复现 (纯 JDK, 无依赖)
 *
 * 复现三个核心机制 (与源码逐条对应):
 *  1. Picker 每 RPC 选址: 状态变更时更新 Picker, 每 RPC 调 pickSubchannel (LoadBalancer.java:453-463)
 *  2. RoundRobin 组合: 只对 READY 子流轮询, 随机起点防同相 (RoundRobinLoadBalancer.java:36,40-55)
 *  3. PickFirst 逐地址 + 退避重连: 首地址失败 → 下地址; 全失败 → 退避 (PickFirstLeafLoadBalancer.java:508-520, InternalSubchannel.java:305-322)
 *
 * 用法: javac MiniG4.java && java MiniG4
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiniG4 {

  // ============ 机制 1: Picker 协议 ============
  static final class PickResult {
    final String subchannel;
    final boolean ready;
    PickResult(String subchannel, boolean ready) { this.subchannel = subchannel; this.ready = ready; }
    static PickResult withNoResult() { return new PickResult(null, false); }
  }

  interface SubchannelPicker {                                // LoadBalancer.java:453
    PickResult pickSubchannel();                              // L461: 每 RPC 一次
  }

  // ============ 机制 2: RoundRobin (组合) ============
  static final class Subchannel {
    final String name;
    boolean ready;
    Subchannel(String name) { this.name = name; }
  }

  // RoundRobinLoadBalancer: 只对 READY 子流轮询 (L40-55) + sequence 随机起点 (L36)
  static final class RoundRobin implements SubchannelPicker {
    final List<Subchannel> subchannels = new ArrayList<>();
    int sequence = new Random().nextInt();                    // L36 随机起点防同相

    void add(String name, boolean ready) { Subchannel sc = new Subchannel(name); sc.ready = ready; subchannels.add(sc); }

    @Override
    public PickResult pickSubchannel() {
      List<Subchannel> ready = new ArrayList<>();             // getReadyChildren (L41)
      for (Subchannel sc : subchannels) if (sc.ready) ready.add(sc);
      if (ready.isEmpty()) return PickResult.withNoResult();  // 无 READY → NoResult (L48-52)
      Subchannel next = ready.get(Math.floorMod(sequence++, ready.size())); // 轮询
      return new PickResult(next.name, true);
    }
  }

  // ============ 机制 3: PickFirst 逐地址 (Happy Eyeballs 简化) ============
  static final class PickFirst {
    final List<String> addresses = new ArrayList<>();
    int index;
    String connected;

    void add(String addr) { addresses.add(addr); }

    // requestConnection 逐地址 (PickFirstLeafLoadBalancer.java:508-520)
    String connect() {
      for (int i = index; i < addresses.size(); i++) {
        if (connectTo(addresses.get(i))) {
          index = i;
          connected = addresses.get(i);
          return connected;
        }
        System.out.println("  [pickfirst] " + addresses.get(i) + " 连接失败 → 下一地址");
      }
      System.out.println("  [pickfirst] 全部失败 → TRANSIENT_FAILURE + 退避重连 (InternalSubchannel.java:309-322)");
      return null;
    }

    boolean connectTo(String addr) { return !addr.contains("down"); } // 模拟失败
  }

  public static void main(String[] args) {
    int pass = 0, fail = 0;

    // --- 1. Picker 协议: 每 RPC 一次决策 ---
    RoundRobin rr = new RoundRobin();
    rr.add("A", true); rr.add("B", true); rr.add("C", false);
    System.out.println("[picker] A/B 就绪, C 故障 → 轮询序列: "
        + rr.pickSubchannel().subchannel + ", " + rr.pickSubchannel().subchannel);
    // 轮询只在 A/B 间循环
    String p1 = rr.pickSubchannel().subchannel, p2 = rr.pickSubchannel().subchannel;
    if ((p1.equals("A") && p2.equals("B")) || (p1.equals("B") && p2.equals("A"))) pass++; else fail++;

    // --- 2. 无 READY → NoResult (CONNECTING 语义) ---
    RoundRobin empty = new RoundRobin();
    empty.add("X", false);
    if (!empty.pickSubchannel().ready) { System.out.println("[picker] 无 READY → NoResult (等待 CONNECTING)"); pass++; } else fail++;

    // --- 3. PickFirst 逐地址 ---
    PickFirst pf = new PickFirst();
    pf.add("10.0.0.1:8080"); pf.add("10.0.0.2:8080");
    System.out.println("[pickfirst] 地址列表: 10.0.0.1:8080, 10.0.0.2:8080");
    String conn = pf.connect();
    System.out.println("[pickfirst] 最终连接: " + conn);
    if ("10.0.0.1:8080".equals(conn)) pass++; else fail++;

    // --- 4. 首地址失败 → 第二地址 ---
    PickFirst pf2 = new PickFirst();
    pf2.add("10.0.0.1-down:8080"); pf2.add("10.0.0.2:8080");
    String conn2 = pf2.connect();
    System.out.println("[pickfirst] 首地址 down → 连接: " + conn2);
    if ("10.0.0.2:8080".equals(conn2)) pass++; else fail++;

    // --- 5. 全失败 → 退避 ---
    PickFirst pf3 = new PickFirst();
    pf3.add("10.0.0.1-down:8080");
    String conn3 = pf3.connect();
    if (conn3 == null) { System.out.println("[pickfirst] 全失败 → 重试待退避 (G-6 复用)"); pass++; } else fail++;

    System.out.println("\n===== PASS " + pass + " / FAIL " + fail + " =====");
    if (fail > 0) System.exit(1);
  }
}
