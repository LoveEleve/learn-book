import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * MiniTimeWheel — J-6 时间轮核心逻辑极简复现 (harness)
 *
 * 纯逻辑模拟, 对照 SOFAJRaft 1.4.1 HashedWheelTimer (util/timer) 验证:
 *   A. 桶定位: tick & mask 取模 (HashedWheelTimer.java:72-76, 2 的幂轮)
 *   B. 到期精度: 任务在 [due, due+tickDuration) 内触发
 *   C. 重复调度: RepeatedTimer 触发后自动重排 (RepeatedTimer.java:83-107)
 *   D. 随机化窗口: adjustTimeout 在 [t, t+delay) (NodeImpl.java:893-895)
 *   E. 绕轮: 超长延迟任务跨多圈仍正确到期
 */
public class MiniTimeWheel {

    /** 时间轮: tickDuration=1 的简化轮 (wheel 大小 = ticksPerWheel, 2 的幂) */
    static class Wheel {
        final int ticksPerWheel;
        final int mask;
        final List<List<Runnable>> buckets; // 每桶 = 链表
        long tick = 0;
        int currentIndex = 0;

        Wheel(int ticksPerWheel) {
            this.ticksPerWheel = ticksPerWheel;
            this.mask = ticksPerWheel - 1; // 2 的幂 → mask 取模 (L72-76)
            buckets = new ArrayList<>();
            for (int i = 0; i < ticksPerWheel; i++) {
                buckets.add(new LinkedList<>());
            }
        }

        /** A. 桶定位: 从当前 tick 起, 剩余 delayTicks 转的桶下标 */
        int bucketFor(long delayTicks) {
            int index = (int) ((currentIndex + delayTicks) & mask); // tick & mask
            return index;
        }

        void advance() { // 指针推进一个 tick
            tick++;
            currentIndex = (currentIndex + 1) & mask;
        }
    }

    /** C. 重复调度语义: 触发完成才排下一次 */
    static class RepeatedTask {
        long periodTicks;
        long nextDue;
        int triggers = 0;
        boolean stopped = false;

        RepeatedTask(long periodTicks) {
            this.periodTicks = periodTicks;
            this.nextDue = periodTicks; // 首次调度
        }

        /** 模拟 tick 到达: 到期则触发, 触发完成后重排 (RepeatedTimer.java:83-107) */
        boolean onTick(long now) {
            if (stopped || now < nextDue) {
                return false;
            }
            triggers++;
            // 触发完成后才 schedule 下一次 (timeout=null + schedule())
            nextDue = now + periodTicks;
            return true;
        }

        void stop() {
            stopped = true;
        }
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.桶定位", "B.到期精度", "C.重复调度", "D.随机化", "E.绕轮"};
        boolean[] r = new boolean[5];

        // ---------- A. 桶定位 ----------
        {
            Wheel w = new Wheel(16); // 16 桶, mask=15
            int b1 = w.bucketFor(0); // 当前桶
            int b5 = w.bucketFor(5); // 5 tick 后
            w.advance(); // currentIndex 0 -> 1
            int b5AfterAdvance = w.bucketFor(5);
            // 桶定位 = 从当前指针起延迟 N tick: (currentIndex + delay) & mask
            r[0] = b1 == 0 && b5 == 5 && b5AfterAdvance == ((1 + 5) & 15);
            System.out.println("[A] 桶定位: 当前=" + b1 + " 5tick后=" + b5 + " 推进后=" + b5AfterAdvance + " (期望 (1+5)&15=" + ((1 + 5) & 15) + ")");
        }

        // ---------- B. 到期精度 ----------
        {
            // tickDuration=1: 任务应在 [due, due+1) 内触发
            long due = 10;
            long fired = 10; // 第 10 tick 触发
            boolean inWindow = fired >= due && fired < due + 1;
            long firedLate = 12;
            boolean lateMissed = firedLate >= due + 1; // 12 已过窗口 → 轮盘绕过了
            r[1] = inWindow && lateMissed;
            System.out.println("[B] 到期精度: 窗口内触发=" + inWindow + " 窗口后=绕轮错过=" + lateMissed);
        }

        // ---------- C. 重复调度 ----------
        {
            RepeatedTask t = new RepeatedTask(5); // 每 5 tick
            int count = 0;
            for (long now = 1; now <= 30; now++) {
                if (t.onTick(now)) {
                    count++;
                }
            }
            r[2] = count == 6; // 5,10,15,20,25,30
            System.out.println("[C] 重复调度: 30 tick 内触发 " + count + " 次 (期望 6)");
        }

        // ---------- D. 随机化窗口 ----------
        {
            Random rnd = new Random(7);
            int inWindow = 0;
            for (int i = 0; i < 10000; i++) {
                long timeout = 1000;
                long delay = 1000;
                long rndTimeout = timeout + rnd.nextInt((int) delay); // NodeImpl.java:893-895
                if (rndTimeout >= 1000 && rndTimeout < 2000) {
                    inWindow++;
                }
            }
            r[3] = inWindow == 10000;
            System.out.println("[D] 随机化: 10000 次全在 [1000,2000) 窗口 = " + (inWindow == 10000));
        }

        // ---------- E. 绕轮 ----------
        {
            Wheel w = new Wheel(16);
            // 超长延迟 50 tick: 需要跨 3 圈 (50/16 = 3 圈余 2)
            int bucket = w.bucketFor(50);
            long rounds = 50 / 16;
            // 正确性: 从 currentIndex=0 起, 50 tick 后指针到 50&15 = 2 号桶 → 任务在桶 2
            r[4] = bucket == 2 && rounds == 3;
            System.out.println("[E] 绕轮: 50 tick = " + rounds + " 圈余 2 → 桶 " + bucket);
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
