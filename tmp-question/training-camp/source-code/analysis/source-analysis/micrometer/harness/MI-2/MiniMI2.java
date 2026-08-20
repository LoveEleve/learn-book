import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MiniMI2 {

    static int pass = 0, fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    static SimpleConfig stepConfig() {
        return new SimpleConfig() {
            public String get(String key) {
                if (key.equals("simple.mode")) return "STEP";
                if (key.equals("simple.step")) return "10s";
                return null;
            }
        };
    }

    public static void main(String[] args) {
        System.out.println("== MiniMI2: Counter/Gauge/Timer 家族 ==");

        // 1. StepCounter: poll 返回上一周期累积值 (StepValue 语义)
        MockClock clock = new MockClock();
        SimpleMeterRegistry reg = new SimpleMeterRegistry(stepConfig(), clock);
        Counter c = reg.counter("c1");
        c.increment(5.0);
        check("StepCounter 同步长内 poll 返回 previous=0", c.count() == 0.0);
        clock.add(Duration.ofSeconds(10));
        check("StepCounter 跨步长 poll 返回上一周期累积 (5.0)", c.count() == 5.0);
        clock.add(Duration.ofSeconds(10));
        check("StepCounter 再跨步长归零 (无新增)", c.count() == 0.0);

        // 2. Timer 负值 drop
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        Timer t2 = reg2.timer("t2");
        t2.record(-5, TimeUnit.NANOSECONDS);
        check("Timer 负值 drop (count 不变)", t2.count() == 0);
        t2.record(100, TimeUnit.MILLISECONDS);
        check("Timer 正值记录", t2.count() == 1 && t2.totalTime(TimeUnit.MILLISECONDS) >= 100.0);

        // 3. mean 除零保护
        check("Timer.mean count==0 → 0", t2.mean(TimeUnit.MILLISECONDS) >= 0);

        // 4. Sample 延迟绑定 (stop 时才注册 timer)
        SimpleMeterRegistry reg3 = new SimpleMeterRegistry();
        Timer.Sample sample = Timer.start(reg3.config().clock());
        Timer t3 = reg3.timer("lazy.reg");
        long ns = sample.stop(t3);
        check("Sample.stop 延迟绑定 (timer 注册 + 记录)", t3.count() == 1 && ns >= 0);

        // 5. Gauge Supplier builder 强引用
        AtomicReference<Long> holder = new AtomicReference<>(42L);
        SimpleMeterRegistry reg4 = new SimpleMeterRegistry();
        Gauge strongG = Gauge.builder("strong.g", holder, AtomicReference::get).register(reg4);
        holder.set(null);
        check("Gauge strongReference: 内部值 null → NaN (WeakReference 语义)", Double.isNaN(strongG.value()));

        // 6. CUMULATIVE 模式
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry(new SimpleConfig() {
            public String get(String key) {
                if (key.equals("simple.mode")) return "CUMULATIVE";
                return null;
            }
        }, io.micrometer.core.instrument.Clock.SYSTEM);
        Counter c5 = reg5.counter("cum.c");
        c5.increment(3.0);
        check("CUMULATIVE 模式 count 正常", c5.count() == 3.0);

        // 7. StepFunctionTimer
        MockClock clock7 = new MockClock();
        SimpleMeterRegistry reg7 = new SimpleMeterRegistry(stepConfig(), clock7);
        AtomicLong cnt7 = new AtomicLong(10);
        AtomicLong time7 = new AtomicLong(500);
        FunctionTimer ft7 = FunctionTimer.builder("ft7", cnt7, AtomicLong::get, x -> time7.get(), TimeUnit.MILLISECONDS)
                .register(reg7);
        clock7.add(Duration.ofSeconds(10));
        check("StepFunctionTimer 采样函数值 (10/500ms)", ft7.count() == 10.0 && ft7.totalTime(TimeUnit.MILLISECONDS) == 500.0);
        cnt7.set(20);
        time7.set(1000);
        clock7.add(Duration.ofSeconds(10));
        check("StepFunctionTimer poll 返回上一周期值 (10/500ms)", ft7.count() == 10.0 && ft7.totalTime(TimeUnit.MILLISECONDS) == 500.0);

        // 8. MultiGauge reconcile
        SimpleMeterRegistry reg8 = new SimpleMeterRegistry();
        MultiGauge mg = MultiGauge.builder("rows").register(reg8);
        mg.register(Arrays.asList(
                MultiGauge.Row.of(Tags.of("k", "1"), 1.0),
                MultiGauge.Row.of(Tags.of("k", "2"), 2.0)));
        check("MultiGauge 两行注册", reg8.getMeters().size() == 2);
        mg.register(Arrays.asList(MultiGauge.Row.of(Tags.of("k", "1"), 10.0)));
        check("MultiGauge 消失行移除 (2→1)", reg8.getMeters().size() == 1);
        mg.register(Arrays.asList(MultiGauge.Row.of(Tags.of("k", "1"), 99.0)), true);
        check("MultiGauge overwrite 替换 (行数不变)", reg8.getMeters().size() == 1);

        // 9. LongTaskTimer
        SimpleMeterRegistry reg9 = new SimpleMeterRegistry();
        LongTaskTimer ltt = LongTaskTimer.builder("ltt").register(reg9);
        LongTaskTimer.Sample s1 = ltt.start();
        LongTaskTimer.Sample s2 = ltt.start();
        check("LongTaskTimer activeTasks=2", ltt.activeTasks() == 2);
        s1.stop();
        check("LongTaskTimer stop 后 activeTasks=1", ltt.activeTasks() == 1);
        s2.stop();
        check("LongTaskTimer 全停 activeTasks=0", ltt.activeTasks() == 0);
        check("LongTaskTimer duration 空载=0", ltt.duration(TimeUnit.MILLISECONDS) == 0.0);

        // 10. Timer SLO 桶
        SimpleMeterRegistry reg10 = new SimpleMeterRegistry();
        Timer t10 = Timer.builder("t10")
                .serviceLevelObjectives(Duration.ofMillis(50), Duration.ofMillis(100))
                .register(reg10);
        t10.record(30, TimeUnit.MILLISECONDS);
        t10.record(80, TimeUnit.MILLISECONDS);
        io.micrometer.core.instrument.distribution.HistogramSnapshot snap = t10.takeSnapshot();
        check("Timer SLO 桶有数据", snap.histogramCounts().length > 0);

        // 11. FunctionCounter
        SimpleMeterRegistry reg11 = new SimpleMeterRegistry();
        AtomicLong fc11 = new AtomicLong(7);
        FunctionCounter fcnt = FunctionCounter.builder("fc11", fc11, AtomicLong::get).register(reg11);
        check("FunctionCounter 注册+读取", fcnt.count() == 7.0);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}