/**
 * MiniCommandExecutorTest — RD-4 harness 验证入口
 *
 * 跑法: javac MiniCommandExecutor.java MiniCommandExecutorTest.java && java MiniCommandExecutorTest
 * 全部 PASS = 命令执行+重试边界理解到位 (对照 RedisExecutor.java:278-375, CommandAsyncService.java:489-492)。
 */
public class MiniCommandExecutorTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 正常执行: 一次成功无重试
    static void testNormalExec() {
        MiniCommandExecutor.RedisExecutor ex =
                new MiniCommandExecutor.RedisExecutor(4, false, null, 0);
        String r = MiniCommandExecutor.async(new MiniCommandExecutor.NodeSource(123), ex);
        check("正常执行 OK", r.equals("OK") && ex.attemptsUsed.get() == 1);
    }

    // B. 连接失败 → 重试到成功
    static void testConnectRetry() {
        MiniCommandExecutor.RedisExecutor ex =
                new MiniCommandExecutor.RedisExecutor(4, false, MiniCommandExecutor.RedisExecutor.Phase.CONNECT, 2);
        String r = MiniCommandExecutor.async(new MiniCommandExecutor.NodeSource(0), ex);
        check("连接失败重试2次后成功", r.equals("OK"));
        check("用了3次尝试 (1+2重试)", ex.attemptsUsed.get() == 3);
        check("重试间隔 1000..2000", ex.lastDelayMs >= 1000 && ex.lastDelayMs < 2000);
    }

    // C. 连接一直失败 → 到 maxAttempts 停 (有界重试)
    static void testConnectExhausted() {
        MiniCommandExecutor.RedisExecutor ex =
                new MiniCommandExecutor.RedisExecutor(4, false, MiniCommandExecutor.RedisExecutor.Phase.CONNECT, 99);
        String r = MiniCommandExecutor.async(new MiniCommandExecutor.NodeSource(0), ex);
        check("连接耗尽 CONNECT_FAIL", r.equals("CONNECT_FAIL"));
        check("尝试恰 maxAttempts=4", ex.attemptsUsed.get() == 4);
    }

    // D. 响应超时 → 不重试 (关键边界: 服务端可能已执行)
    static void testResponseNoRetry() {
        MiniCommandExecutor.RedisExecutor ex =
                new MiniCommandExecutor.RedisExecutor(4, false, MiniCommandExecutor.RedisExecutor.Phase.RESPONSE, 0);
        String r = MiniCommandExecutor.async(new MiniCommandExecutor.NodeSource(0), ex);
        check("响应超时不重试 (1次)", r.equals("RESPONSE_TIMEOUT") && ex.attemptsUsed.get() == 1);
    }

    // E. noRetry 模式 (锁续期): 单次尝试快速失败
    static void testNoRetry() {
        MiniCommandExecutor.RedisExecutor ex =
                new MiniCommandExecutor.RedisExecutor(4, true, MiniCommandExecutor.RedisExecutor.Phase.CONNECT, 5);
        String r = MiniCommandExecutor.asyncNoRetry(new MiniCommandExecutor.NodeSource(0), ex);
        check("noRetry 单次尝试", r.endsWith("-single-attempt") && ex.attemptsUsed.get() == 1);
        check("noRetry 失败快速返回", r.startsWith("CONNECT_FAIL"));
    }

    public static void main(String[] args) {
        testNormalExec();
        testConnectRetry();
        testConnectExhausted();
        testResponseNoRetry();
        testNoRetry();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}