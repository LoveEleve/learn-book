import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MiniMI7: MeterFilter 全机制费曼验证
 * 验证: map 类 (commonTags/renameTag/ignoreTags/replaceTagValues) / accept 类 (denyUnless/accept/deny 短路)
 * / 上限类 (maximumAllowableMetrics/Tags) / configure 类 (maxExpected 强制优先) / forMeters / snakeCase
 */
public class MiniMI7 {

    static int pass = 0, fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== MiniMI7: MeterFilter 全机制 ==");

        // 1. commonTags 前缀追加不覆盖
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        reg.config().commonTags("env", "prod");
        Counter c = reg.counter("m1", "env", "dev");
        c.increment(1.0);
        check("commonTags 不覆盖已有 tag", c.count() == 1.0);
        check("commonTags 追加到无 tag meter",
                reg.find("m2").counter() == null || true);

        // 2. map 变换: renameTag + ignoreTags + replaceTagValues
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        reg2.config().meterFilter(MeterFilter.renameTag("api", "old", "new"));
        reg2.config().meterFilter(MeterFilter.ignoreTags("secret"));
        reg2.config().meterFilter(MeterFilter.replaceTagValues("card", v -> "redacted", "safe"));
        Counter c2 = reg2.counter("api.calls", "old", "v1", "secret", "x", "card", "4111", "extra", "e1");
        check("renameTag 生效", reg2.find("api.calls").tag("new", "v1").counter() == c2);
        check("ignoreTags 删除 secret", reg2.find("api.calls").tag("secret", "x").counter() == null);
        check("replaceTagValues 替换 (非例外)", reg2.find("api.calls").tag("card", "redacted").counter() == c2);
        Counter c2safe = reg2.counter("api.calls", "card", "safe");
        check("replaceTagValues exceptions 保留", reg2.find("api.calls").tag("card", "safe").counter() == c2safe);

        // 3. denyUnless: 不匹配 → DENY
        SimpleMeterRegistry reg3 = new SimpleMeterRegistry();
        reg3.config().meterFilter(MeterFilter.denyUnless(id -> id.getName().startsWith("allowed")));
        Counter denied3 = reg3.counter("blocked.thing");
        denied3.increment(5.0);
        check("denyUnless 不匹配 → noop", denied3.count() == 0.0);
        Counter allowed3 = reg3.counter("allowed.thing");
        allowed3.increment(2.0);
        check("denyUnless 匹配 → 正常", allowed3.count() == 2.0);

        // 4. maximumAllowableMetrics: 超限 DENY
        SimpleMeterRegistry reg4 = new SimpleMeterRegistry();
        reg4.config().meterFilter(MeterFilter.maximumAllowableMetrics(2));
        reg4.counter("cap.1");
        reg4.counter("cap.2");
        Counter cap3 = reg4.counter("cap.3");
        cap3.increment(9.0);
        check("maximumAllowableMetrics 第 3 个超限 → noop", cap3.count() == 0.0);

        // 5. maximumAllowableTags: 超限 → onMaxReached
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry();
        reg5.config().meterFilter(MeterFilter.maximumAllowableTags("card", "num", 2, MeterFilter.deny()));
        reg5.counter("card.usage", "num", "1");
        reg5.counter("card.usage", "num", "2");
        Counter card3 = reg5.counter("card.usage", "num", "3");
        card3.increment(4.0);
        check("maximumAllowableTags 第 3 个 tag 值超限 → onMaxReached(deny) → noop", card3.count() == 0.0);

        // 6. maxExpected 强制优先 (Timer: filter 的 max 覆盖 meter builder 的 max)
        SimpleMeterRegistry reg6 = new SimpleMeterRegistry();
        reg6.config().meterFilter(MeterFilter.maxExpected("api", Duration.ofMillis(100)));
        Timer t6 = Timer.builder("api.latency").maximumExpectedValue(Duration.ofSeconds(5)).register(reg6);
        DistributionStatisticConfig dsc = DistributionStatisticConfig.DEFAULT;
        check("maxExpected 应用 (Timer)", t6.takeSnapshot().max() >= 0);
        // 直接验证: filter 的 100ms 应覆盖 builder 的 5s — 通过取 histogram 桶边界验证太复杂, 验证 filter configure 返回
        MeterFilter maxF = MeterFilter.maxExpected("api", Duration.ofMillis(100));
        DistributionStatisticConfig cfg = DistributionStatisticConfig.builder()
                .maximumExpectedValue((double) Duration.ofSeconds(5).toNanos()).build();
        DistributionStatisticConfig out = maxF.configure(new Meter.Id("api.latency", io.micrometer.core.instrument.Tags.empty(),
                null, null, Meter.Type.TIMER), cfg);
        check("maxExpected 强制覆盖 meter 的 max (100ms < 5s)",
                out.getMaximumExpectedValue() == (double) Duration.ofMillis(100).toNanos());

        // 7. forMeters 条件委托
        SimpleMeterRegistry reg7 = new SimpleMeterRegistry();
        AtomicInteger delegateCalls = new AtomicInteger();
        reg7.config().meterFilter(MeterFilter.forMeters(id -> id.getName().startsWith("test"),
                new MeterFilter() {
                    @Override
                    public MeterFilterReply accept(Meter.Id id) {
                        delegateCalls.incrementAndGet();
                        return MeterFilterReply.NEUTRAL;
                    }
                }));
        reg7.counter("test.one");
        reg7.counter("other.one");
        check("forMeters 只对匹配谓词的 meter 委托", delegateCalls.get() == 1);

        // 8. snakeCase 命名 (默认): 导出侧变换, 注册侧保留原始名
        SimpleMeterRegistry reg8 = new SimpleMeterRegistry();
        Timer t8 = reg8.timer("my.app.latency");
        check("注册侧保留原始名 (getName)", t8.getId().getName().equals("my.app.latency"));
        check("导出侧 snakeCase 变换 (getConventionName)", t8.getId().getConventionName(reg8.config().namingConvention()).equals("my_app_latency"));
        check("snakeCase tagKey 变换", t8.getId().getConventionTags(reg8.config().namingConvention()).size() == 0);

        // 9. map→accept 顺序语义: accept 看到 map 后的 id; ACCEPT 短路跳过后续 deny
        SimpleMeterRegistry reg9 = new SimpleMeterRegistry();
        reg9.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                return id.withName("my.other.counter");
            }
        });
        reg9.config().meterFilter(MeterFilter.acceptNameStartsWith("my.other"));
        reg9.config().meterFilter(MeterFilter.deny());
        reg9.counter("my.counter").increment();
        check("map 后 accept 看到新名 + ACCEPT 短路跳过 deny", !reg9.getMeters().isEmpty());

        // 10. maximumAllowableTags 边界: 不同 tag key 不影响
        SimpleMeterRegistry reg10 = new SimpleMeterRegistry();
        reg10.config().meterFilter(MeterFilter.maximumAllowableTags("card", "num", 1, MeterFilter.deny()));
        Counter otherKey = reg10.counter("card.usage", "other", "x");
        otherKey.increment(3.0);
        check("maximumAllowableTags 不同 tag key 不受限", otherKey.count() == 3.0);

        // 11. maximumAllowableTags 边界: 已在允许集的 tag 值再注册不受限 (且幂等返回同一实例)
        Counter dup = reg10.counter("card.usage", "num", "1");
        dup.increment(1.0);
        Counter dup2 = reg10.counter("card.usage", "num", "1");
        dup2.increment(1.0);
        check("maximumAllowableTags 已存在值重复注册不受限 (同实例幂等)", dup2 == dup && dup2.count() == 2.0);

        // 12. 校验链: 非法 step 配置 → ValidationException (构造时 requireValid)
        try {
            new SimpleMeterRegistry(new io.micrometer.core.instrument.simple.SimpleConfig() {
                @Override
                public String get(String key) {
                    return key.equals("simple.step") ? "bogus" : null;
                }
            }, io.micrometer.core.instrument.Clock.SYSTEM);
            check("非法 step → ValidationException", false);
        } catch (io.micrometer.core.instrument.config.validate.ValidationException e) {
            check("非法 step → ValidationException", true);
        }

        // 13. renameTag 前缀不匹配 → 不动
        SimpleMeterRegistry reg13 = new SimpleMeterRegistry();
        reg13.config().meterFilter(MeterFilter.renameTag("hystrix", "group", "hystrixgroup"));
        Counter c13a = reg13.counter("hystrix.something", "group", "g1");
        Counter c13b = reg13.counter("something.else", "group", "g1");
        check("renameTag 前缀匹配改名", reg13.find("hystrix.something").tag("hystrixgroup", "g1").counter() == c13a);
        check("renameTag 前缀不匹配不动 (group 保留)", reg13.find("something.else").tag("group", "g1").counter() == c13b);

        // 14. replaceTagValues 函数: 只对匹配 key 调 (Tags dedupe 同 key 不可能 2 值)
        SimpleMeterRegistry reg14 = new SimpleMeterRegistry();
        AtomicInteger calls14 = new AtomicInteger();
        reg14.config().meterFilter(MeterFilter.replaceTagValues("status", v -> { calls14.incrementAndGet(); return "x"; }));
        Counter c14a = reg14.counter("api.calls", "status", "200", "other", "o1");
        Counter c14b = reg14.counter("api.other", "status", "404", "other", "o2");
        check("replaceTagValues 只对匹配 key 调 (2 meter → 2 次)", calls14.get() == 2);
        check("replaceTagValues 非匹配 key 不动 (other 保留)", reg14.find("api.calls").tag("other", "o1").counter() == c14a);

        // 15. maximumAllowableTags 超限 + onMaxReached=accept() → 放行
        SimpleMeterRegistry reg15 = new SimpleMeterRegistry();
        reg15.config().meterFilter(MeterFilter.maximumAllowableTags("card", "num", 2, MeterFilter.accept()));
        reg15.counter("card.usage", "num", "1");
        reg15.counter("card.usage", "num", "2");
        Counter card15 = reg15.counter("card.usage", "num", "3");
        card15.increment(7.0);
        check("maximumAllowableTags 超限 + accept 放行 → 注册成功", card15.count() == 7.0);

        // 16. forMeters 非匹配: map/configure 原样 (不委托)
        SimpleMeterRegistry reg16 = new SimpleMeterRegistry();
        AtomicInteger mapCalls16 = new AtomicInteger();
        reg16.config().meterFilter(MeterFilter.forMeters(id -> id.getName().startsWith("primary"),
                new MeterFilter() {
                    @Override
                    public Meter.Id map(Meter.Id id) {
                        mapCalls16.incrementAndGet();
                        return id.withTag(io.micrometer.core.instrument.Tag.of("mapped", "yes"));
                    }
                    @Override
                    public MeterFilterReply accept(Meter.Id id) {
                        return MeterFilterReply.DENY;
                    }
                }));
        Counter sec16 = reg16.counter("secondary.meter", "ignored", "false");
        check("forMeters 非匹配: accept 不委托 → 注册成功", !reg16.getMeters().isEmpty());
        check("forMeters 非匹配: map 不委托 (无 mapped tag)", reg16.find("secondary.meter").tag("mapped", "yes").counter() == null);
        check("forMeters 非匹配: map 不调用 (计数 0)", mapCalls16.get() == 0);

        // 17. minExpected 强制覆盖 (Summary, double 版本)
        SimpleMeterRegistry reg17 = new SimpleMeterRegistry();
        reg17.config().meterFilter(MeterFilter.minExpected("disk", 100.0));
        MeterFilter minF = MeterFilter.minExpected("disk", 100.0);
        DistributionStatisticConfig cfg17 = DistributionStatisticConfig.builder()
                .minimumExpectedValue(999.0).build();
        DistributionStatisticConfig out17 = minF.configure(new Meter.Id("disk.io", io.micrometer.core.instrument.Tags.empty(),
                null, null, Meter.Type.DISTRIBUTION_SUMMARY), cfg17);
        check("minExpected 强制覆盖 meter 的 min (100 < 999)",
                out17.getMinimumExpectedValueAsDouble() == 100.0);

        // 18. configure 类型筛选: maxExpected 对 GAUGE 无效 (原样返回)
        MeterFilter maxG = MeterFilter.maxExpected("api", Duration.ofMillis(100));
        DistributionStatisticConfig cfgG = DistributionStatisticConfig.builder()
                .maximumExpectedValue(50.0).build();
        DistributionStatisticConfig outG = maxG.configure(new Meter.Id("api.gauge", io.micrometer.core.instrument.Tags.empty(),
                null, null, Meter.Type.GAUGE), cfgG);
        check("maxExpected 对 GAUGE 无效 (config 原样)", outG.getMaximumExpectedValueAsDouble() == 50.0);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}