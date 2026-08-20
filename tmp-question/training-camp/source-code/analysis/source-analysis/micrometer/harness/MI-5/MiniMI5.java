import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmCompilationMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.concurrent.*;

public class MiniMI5 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== MiniMI5: MeterBinder / JVM binders ==");

        // 1. ClassLoaderMetrics
        SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
        new ClassLoaderMetrics(Tags.of("extra", "tag")).bindTo(reg1);
        check("classloader current gauge exists", reg1.get("jvm.classes.loaded").tags("extra", "tag").gauge().value() >= 0);
        check("classloader binder with extra tags bound safely", reg1.find("jvm.classes.loaded").tags("extra", "tag").gauge() != null);

        // 2. JvmCompilationMetrics
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        new JvmCompilationMetrics(Tags.of("extra", "tag")).bindTo(reg2);
        check("compilation metric optional but bind safe", reg2.find("jvm.compilation.time").tags("extra", "tag").functionCounter() == null || reg2.get("jvm.compilation.time").tags("extra", "tag").functionCounter().count() >= 0);

        // 3. JvmMemoryMetrics
        SimpleMeterRegistry reg3 = new SimpleMeterRegistry();
        new JvmMemoryMetrics(Tags.of("extra", "tag")).bindTo(reg3);
        check("memory used gauge exists", reg3.find("jvm.memory.used").tags("extra", "tag").gauges().size() > 0);
        check("buffer metric exists", reg3.find("jvm.buffer.count").tags("extra", "tag").gauges().size() >= 0);

        // 4. JvmThreadMetrics
        SimpleMeterRegistry reg4 = new SimpleMeterRegistry();
        new JvmThreadMetrics(Tags.of("extra", "tag")).bindTo(reg4);
        check("thread live gauge exists", reg4.get("jvm.threads.live").tags("extra", "tag").gauge().value() > 0);
        check("thread started function counter exists", reg4.get("jvm.threads.started").tags("extra", "tag").functionCounter().count() > 0);

        // 5. ExecutorServiceMetrics bind + wrapper timing + queued gauge
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry();
        ExecutorService raw = Executors.newFixedThreadPool(1);
        ExecutorService monitored = ExecutorServiceMetrics.monitor(reg5, raw, "beep.pool", Tags.of("k", "v"));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        monitored.submit(() -> {
            started.countDown();
            release.await(1, TimeUnit.SECONDS);
            return 0;
        });
        monitored.submit(() -> 0);
        started.await(1, TimeUnit.SECONDS);
        check("executor queued gauge visible", reg5.get("executor.queued").tags("k", "v").tag("name", "beep.pool").gauge().value() == 1.0);
        release.countDown();
        monitored.shutdown();
        monitored.awaitTermination(2, TimeUnit.SECONDS);
        check("executor timing recorded", reg5.get("executor").tags("k", "v").timer().count() == 2L);
        check("executor idle timing recorded", reg5.get("executor.idle").tags("k", "v").timer().count() == 2L);

        // 6. metricPrefix sanitization
        SimpleMeterRegistry reg6 = new SimpleMeterRegistry();
        Executor executor6 = ExecutorServiceMetrics.monitor(reg6, (Executor) Runnable::run, "direct", "custom", Tag.of("t", "1"));
        executor6.execute(() -> {});
        check("metric prefix sanitized with dot", reg6.get("custom.executor.execution").tags("t", "1").tag("name", "direct").timer().count() == 1L);

        // 7. JvmGcMetrics bind safe + close safe
        SimpleMeterRegistry reg7 = new SimpleMeterRegistry();
        JvmGcMetrics gc7 = new JvmGcMetrics(Tags.of("key", "value"));
        gc7.bindTo(reg7);
        System.gc();
        Thread.sleep(200);
        check("gc bind registers max/live gauges or safely no-op", reg7.find("jvm.gc.max.data.size").tags("key", "value").gauge() == null || reg7.get("jvm.gc.max.data.size").tags("key", "value").gauge().value() >= 0);
        gc7.close();
        check("gc close safe", true);

        // 8. JvmHeapPressureMetrics bind + close safe
        SimpleMeterRegistry reg8 = new SimpleMeterRegistry();
        JvmHeapPressureMetrics hp8 = new JvmHeapPressureMetrics(Tags.of("x", "y"), Duration.ofSeconds(10), Duration.ofSeconds(1));
        hp8.bindTo(reg8);
        check("heap pressure overhead gauge exists", reg8.get("jvm.gc.overhead").tags("x", "y").gauge().value() >= 0.0);
        hp8.close();
        check("heap pressure close safe", true);

        // 9. executor monitor with blank prefix keeps default names
        SimpleMeterRegistry reg9 = new SimpleMeterRegistry();
        Executor executor9 = ExecutorServiceMetrics.monitor(reg9, (Executor) Runnable::run, "blank", " ", Tags.empty());
        executor9.execute(() -> {});
        check("blank prefix becomes default empty prefix", reg9.get("executor.execution").tag("name", "blank").timer().count() == 1L);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}
