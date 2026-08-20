import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.distribution.HistogramGauges;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;

public class MiniMI3 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    static SimpleConfig stepConfig() {
        return new SimpleConfig() {
            @Override public String get(String key) {
                if (key.equals("simple.mode")) return "STEP";
                if (key.equals("simple.step")) return "10ms";
                return null;
            }
        };
    }

    public static void main(String[] args) {
        System.out.println("== MiniMI3: DistributionSummary / Histogram ==");

        // 1. negative drop is silent: count/total stay zero
        SimpleMeterRegistry reg1 = new SimpleMeterRegistry();
        DistributionSummary s1 = DistributionSummary.builder("s1").register(reg1);
        s1.record(-5);
        check("negative record dropped", s1.count() == 0 && s1.totalAmount() == 0.0);

        // 2. scale applies to total/max/histogram path while count still increments by call count
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        DistributionSummary s2 = DistributionSummary.builder("s2")
                .scale(100)
                .serviceLevelObjectives(100.0, 200.0)
                .register(reg2);
        s2.record(1.5);
        HistogramSnapshot snap2 = s2.takeSnapshot();
        check("scale leaves count as call count", s2.count() == 1);
        check("scale applies to total and max", s2.totalAmount() == 150.0 && s2.max() == 150.0);
        check("scale applies before histogram buckets", snap2.histogramCounts().length > 0 && snap2.histogramCounts()[1].count() == 1);

        // 3. merge precedence: this overrides parent, parent fills defaults
        DistributionStatisticConfig c3a = DistributionStatisticConfig.builder().percentiles(0.90).build();
        DistributionStatisticConfig c3b = DistributionStatisticConfig.builder().expiry(Duration.ofSeconds(5)).build();
        DistributionStatisticConfig c3m = c3a.merge(c3b).merge(DistributionStatisticConfig.DEFAULT);
        check("merge uses this precedence", c3m.getPercentiles()[0] == 0.90);
        check("merge pulls parent/default expiry", c3m.getExpiry().equals(Duration.ofSeconds(5)));

        // 4. config validation rejects invalid ranges
        boolean bad4 = false;
        try { DistributionStatisticConfig.builder().minimumExpectedValue(10.0).maximumExpectedValue(9.0).build(); }
        catch (Exception e) { bad4 = true; }
        check("config rejects min > max", bad4);

        // 5. StepDistributionSummary poll returns previous period values
        MockClock clock5 = new MockClock();
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry(stepConfig(), clock5);
        DistributionSummary s5 = DistributionSummary.builder("s5").register(reg5);
        s5.record(100);
        s5.record(200);
        check("step summary same period count=0 total=0", s5.count() == 0 && s5.totalAmount() == 0.0);
        clock5.add(Duration.ofMillis(10));
        check("step summary next period returns previous count/total", s5.count() == 2 && s5.totalAmount() == 300.0);
        clock5.add(Duration.ofMillis(10));
        check("step summary then zeros without new samples", s5.count() == 0 && s5.totalAmount() == 0.0);

        // 6. mean works even if total wasn't queried first
        MockClock clock6 = new MockClock();
        SimpleMeterRegistry reg6 = new SimpleMeterRegistry(stepConfig(), clock6);
        DistributionSummary s6 = DistributionSummary.builder("s6").register(reg6);
        clock6.add(Duration.ofMillis(11));
        check("mean zero when empty", s6.mean() == 0.0);
        s6.record(50);
        s6.record(100);
        clock6.add(Duration.ofMillis(10));
        check("mean derived from previous period count/total", s6.mean() == 75.0);

        // 7. closing rollover equivalent is exposed through Step meter implementation path in tests; here verify snapshot histogram decay across step
        MockClock clock7 = new MockClock();
        SimpleMeterRegistry reg7 = new SimpleMeterRegistry(stepConfig(), clock7);
        DistributionSummary s7 = DistributionSummary.builder("s7").serviceLevelObjectives(1.0).register(reg7);
        s7.record(1);
        check("histogram count initially visible", s7.takeSnapshot().histogramCounts()[0].count() == 1);
        clock7.add(Duration.ofMillis(10));
        check("histogram bucket decays after step", s7.takeSnapshot().histogramCounts()[0].count() == 0);

        // 8. HistogramGauges auto-registered by SimpleMeterRegistry; first poll of next publish cycle refreshes snapshot
        SimpleMeterRegistry reg8 = new SimpleMeterRegistry();
        DistributionSummary s8 = DistributionSummary.builder("s8").serviceLevelObjectives(1.0).register(reg8);
        s8.record(1);
        double bucket = reg8.get("s8.histogram").tag("le", "1").gauge().value();
        check("histogram gauge exposes bucket count", bucket == 1.0);
        s8.record(1);
        double bucketAgain = reg8.get("s8.histogram").tag("le", "1").gauge().value();
        check("first poll in next publish cycle refreshes snapshot", bucketAgain == 2.0);

        // 9. meter filter is not double-applied to histogram/percentile names: official test coverage, here verify mapped registration exists once
        SimpleMeterRegistry reg9 = new SimpleMeterRegistry();
        reg9.config().meterFilter(new io.micrometer.core.instrument.config.MeterFilter() {
            @Override public io.micrometer.core.instrument.Meter.Id map(io.micrometer.core.instrument.Meter.Id id) {
                return id.withName("P." + id.getName());
            }
        });
        DistributionSummary.builder("s9").serviceLevelObjectives(1.0).publishPercentiles(0.95).register(reg9);
        check("meter filter applied once to percentile gauge", reg9.get("P.s9.percentile").tag("phi", "0.95").gauge() != null);
        check("meter filter applied once to histogram gauge", reg9.get("P.s9.histogram").tag("le", "1").gauge() != null);

        // 10. histogram bucket set includes SLO even without percentileHistogram
        DistributionStatisticConfig c10 = DistributionStatisticConfig.builder().serviceLevelObjectives(5.0, 10.0).build();
        check("SLO alone enables histogram", c10.isPublishingHistogram());
        check("SLO buckets included", c10.getHistogramBuckets(false).contains(5.0) && c10.getHistogramBuckets(false).contains(10.0));

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}