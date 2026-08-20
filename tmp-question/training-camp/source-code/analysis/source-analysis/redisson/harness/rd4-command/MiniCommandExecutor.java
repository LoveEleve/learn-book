import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniCommandExecutor — RD-4 命令执行流水线 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 Redisson 4.6.2 源码):
 *   A. 汇聚点: async(readOnly, NodeSource, codec, command, params, noRetry) → RedisExecutor.execute()
 *      (CommandAsyncService.java:690-731)
 *   B. 重试协议边界: 连接失败可重试 (attempt++ 递归 execute) vs 响应超时不重试 (服务端可能已执行)
 *      (RedisExecutor.java:278-375)
 *   C. noRetry 模式: 续期等幂等场景禁用重试 (evalWriteNoRetryAsync CommandAsyncService.java:489-492)
 *
 * 机制复现, 非完整库 — 用模拟连接/响应服务。
 */
public class MiniCommandExecutor {

    // NodeSource 抽象: slot/addr/redirect (NodeSource.java:28-51)
    static class NodeSource {
        final int slot;
        NodeSource(int slot) { this.slot = slot; }
    }

    // ===== A. RedisExecutor: 单命令执行 (时序返回) =====
    static class RedisExecutor {
        enum Phase { CONNECT, SEND, RESPONSE }
        final boolean noRetry;
        final int maxAttempts;
        int attempts;
        Phase failPhase;       // 模拟在此阶段失败
        final int connectFailures; // 前 N 次连接失败
        AtomicInteger attemptsUsed = new AtomicInteger();
        long lastDelayMs;

        RedisExecutor(int maxAttempts, boolean noRetry, Phase failPhase, int connectFailures) {
            this.maxAttempts = maxAttempts;
            this.noRetry = noRetry;
            this.failPhase = failPhase;
            this.connectFailures = connectFailures;
        }

        // 返回 成功/成功-但重试过/失败
        String execute() {
            attemptsUsed.incrementAndGet();
            attempts = attemptsUsed.get();
            lastDelayMs = EqualJitter.calcDelay(attempts);

            if (failPhase == Phase.CONNECT && attempts <= connectFailures) {
                // 连接失败 → 可重试 (scheduleRetryTimeout 递归 execute L363-371)
                if (!noRetry && attempts < maxAttempts) {
                    return execute(); // 递归重试
                }
                return "CONNECT_FAIL";
            }
            if (failPhase == Phase.SEND) {
                // 写失败 → 可重试 (checkWriteFuture L387-407)
                if (!noRetry && attempts < maxAttempts) return execute();
                return "WRITE_FAIL";
            }
            // RESPONSE 阶段超时 → 不重试 (服务端可能已执行, 重试=重复, scheduleResponseTimeout L406)
            if (failPhase == Phase.RESPONSE) {
                return "RESPONSE_TIMEOUT";
            }
            return "OK";
        }
    }

    // DelayStrategy: EqualJitter 1-2s 语义 (BaseConfig:67 retryDelay EqualJitterDelay)
    static class EqualJitter {
        static long calcDelay(int attempt) {
            // 简化: 1000 + random(0..1000)
            return 1000 + (attempt * 37 % 1000);
        }
    }

    // ===== B. 汇总入口: async (CommandAsyncService.async L690) =====
    static String async(NodeSource source, RedisExecutor executor) {
        return executor.execute(); // 适配 (resp3/SORT_RO) 在真实流发生在 execute 前
    }

    // noRetry 快速失败版本 (evalWriteNoRetryAsync L489)
    static String asyncNoRetry(NodeSource source, RedisExecutor executor) {
        int before = executor.attemptsUsed.get();
        String r = executor.execute();
        return (executor.attemptsUsed.get() == before + 1) ? r + "-single-attempt" : r;
    }
}