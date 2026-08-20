import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.CountingMode;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.core.instrument.HighCardinalityTagsDetector;
import java.time.Duration;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniMI-1: MeterRegistry 核心机制费曼验证
 * 验证: 注册主流程 / filter 三作用 (map/accept/configure) / noop 降级 / composite 转发 / Metrics 门面 / 同名不同类型异常
 */
public class MiniMI1 {

    static int pass = 0, fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== MiniMI1: MeterRegistry 核心机制 ==");

        // 1. 注册主流程: counter/timer/summary/gauge
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        Counter c = reg.counter("requests", "uri", "/api");
        c.increment(3.0);
        check("counter 注册并计数", c.count() == 3.0);

        Timer t = reg.timer("latency");
        t.record(100, TimeUnit.MILLISECONDS);
        check("timer 注册", reg.find("latency").timer() != null);

        DistributionSummary s = reg.summary("payload");
        s.record(42);
        check("summary 注册并记录", s.count() == 1);

        AtomicInteger ai = new AtomicInteger(5);
        Gauge g = Gauge.builder("queue.size", ai, AtomicInteger::doubleValue).register(reg);
        check("gauge 注册并取值", g.value() == 5.0);
        ai.set(9);
        check("gauge 弱引用取最新值", g.value() == 9.0);

        // 2. find/get 查询
        check("find 按名查询", reg.find("requests").counter() == c);
        check("find 不匹配返回 null", reg.find("nope").counter() == null);
        check("get 命中", reg.get("requests").counter() == c);

        // 3. filter.accept DENY → noop 降级
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        reg2.config().meterFilter(MeterFilter.denyNameStartsWith("blocked"));
        Counter denied = reg2.counter("blocked", "x", "y");
        denied.increment(5.0);
        check("DENY → noop (count=0)", denied.count() == 0.0);
        Counter allowed = reg2.counter("ok");
        allowed.increment(2.0);
        check("未拒绝的 counter 正常", allowed.count() == 2.0);

        // 4. filter.map 重命名 → meterMap 用 mapped id
        SimpleMeterRegistry reg3 = new SimpleMeterRegistry();
        reg3.config().meterFilter(MeterFilter.renameTag("renamed", "old", "new"));
        Counter rc = reg3.counter("renamed", "old", "v1");
        check("renameTag 后旧 tag 查不到", reg3.find("renamed").tag("old", "v1").counter() == null);
        check("renameTag 后新 tag 命中", reg3.find("renamed").tag("new", "v1").counter() == rc);

        // 5. filter ACCEPT 短路
        SimpleMeterRegistry reg4 = new SimpleMeterRegistry();
        reg4.config().meterFilter(new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return MeterFilterReply.ACCEPT;
            }
        });
        reg4.config().meterFilter(MeterFilter.denyNameStartsWith("shortcut"));
        Counter sc = reg4.counter("shortcut");
        sc.increment(1.0);
        check("ACCEPT 短路后 DENY 不生效", sc.count() == 1.0);

        // 6. CompositeMeterRegistry 转发
        CompositeMeterRegistry comp = new CompositeMeterRegistry();
        SimpleMeterRegistry child1 = new SimpleMeterRegistry();
        comp.add(child1);
        Counter cc = comp.counter("composite.metric", "k", "v");
        cc.increment(4.0);
        check("composite 转发到子 registry", child1.find("composite.metric").counter().count() == 4.0);
        check("composite 自身可见", comp.find("composite.metric").counter() == cc);

        // 7. Metrics 门面全局
        SimpleMeterRegistry glob = new SimpleMeterRegistry();
        Metrics.addRegistry(glob);
        Counter gc = Metrics.counter("global.metric");
        gc.increment(2.0);
        check("Metrics 门面转发到已注册 registry", glob.find("global.metric").counter().count() == 2.0);
        Metrics.removeRegistry(glob);

        // 8. 同名不同类型 → IllegalArgumentException
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry();
        reg5.counter("dup.name");
        boolean threw = false;
        try {
            reg5.timer("dup.name");
        }
        catch (IllegalArgumentException e) {
            threw = true;
        }
        check("同名不同类型抛 IllegalArgumentException", threw);

        // 9. 重复注册返回同一实例 (幂等)
        Counter c2 = reg.counter("requests", "uri", "/api");
        check("重复注册返回同一实例", c2 == c);

        // 10. 迟配置 filter: stale 机制 — 相同 originalId 重新注册 → 原实例 + unmarkStale
        SimpleMeterRegistry reg6 = new SimpleMeterRegistry();
        Counter staleC = reg6.counter("late.filtered");
        reg6.config().meterFilter(MeterFilter.renameTag("late.filtered", "a", "b"));
        Counter staleC2 = reg6.counter("late.filtered");
        check("迟 filter 后相同 id 重新注册返回原实例", staleC2 == staleC);

        // 11. Step 模式: 轮换语义 (MockClock 驱动)
        SimpleMeterRegistry stepReg = new SimpleMeterRegistry(new SimpleConfig() {
            @Override
            public String get(String k) { return null; }
            @Override
            public Duration step() { return Duration.ofSeconds(1); }
            @Override
            public CountingMode mode() { return CountingMode.STEP; }
        }, new MockClock());
        Counter stepC = stepReg.counter("step.counter");
        stepC.increment(5.0);
        check("STEP 模式注册 (初始值)", stepC.count() == 0.0);
        ((MockClock) stepReg.config().clock()).add(1000, TimeUnit.MILLISECONDS);
        check("STEP 模式单步长推进后 poll 上一步值", stepC.count() == 5.0);
        ((MockClock) stepReg.config().clock()).add(2000, TimeUnit.MILLISECONDS);
        check("STEP 模式跨步长空档归零", stepC.count() == 0.0);

        // 12. LoggingMeterRegistry: 日志输出型 (E10)
        StringBuilder sink = new StringBuilder();
        LoggingMeterRegistry logReg = new LoggingMeterRegistry(new LoggingRegistryConfig() {
            @Override
            public String get(String k) { return null; }
            @Override
            public Duration step() { return Duration.ofMillis(100); }
        }, new MockClock(), sink::append);
        logReg.counter("log.metric", "a", "b").increment(1.0);
        logReg.start();
        ((MockClock) logReg.config().clock()).add(100, TimeUnit.MILLISECONDS);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        check("LoggingMeterRegistry start 后定时 publish 输出指标行", sink.length() > 0);
        logReg.close();

        // 13. HighCardinalityTagsDetector (E11): 同名列超阈值检测
        SimpleMeterRegistry hcdReg = new SimpleMeterRegistry();
        HighCardinalityTagsDetector hcd = new HighCardinalityTagsDetector(hcdReg, 2L, Duration.ofMinutes(1));
        hcdReg.counter("hcd.metric", "k", "1");
        hcdReg.counter("hcd.metric", "k", "2");
        hcdReg.counter("hcd.metric", "k", "3");
        check("HCD 检测高基数 (3>2)", hcd.findFirst().isPresent());
        check("HCD 频率计数正确", hcd.findFirstHighCardinalityMeterInfo().get().getCount() == 3L);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}