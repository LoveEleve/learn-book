import io.micrometer.tracing.Baggage;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Link;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanAndScope;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.ThreadLocalSpan;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MiniMT1 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== MiniMT1: Span / Tracer 核心抽象 ==");

        // 1. Tracer.NOOP whole surface doesn't explode
        Tracer noop = Tracer.NOOP;
        Span ns = noop.nextSpan();
        ns.start().name("x").tag("a", 1).event("ev").error(new RuntimeException()).remoteServiceName("svc").remoteIpAndPort("1.1.1.1", 80).end();
        check("noop nextSpan is Span.NOOP", ns == Span.NOOP && ns.isNoop());
        check("noop currentSpan is Span.NOOP", noop.currentSpan() == Span.NOOP);
        check("noop currentTraceContext is NOOP", noop.currentTraceContext() == CurrentTraceContext.NOOP);

        // 2. nextSpan() root vs child semantics
        SimpleTracer tracer = new SimpleTracer();
        SimpleSpan root = tracer.nextSpan().name("root").start();
        check("root span traceId == spanId in simple tracer", root.context().traceId().equals(root.context().spanId()));
        try (Tracer.SpanInScope ws = tracer.withSpan(root)) {
            SimpleSpan child = tracer.nextSpan().name("child").start();
            check("child inherits traceId", child.context().traceId().equals(root.context().traceId()));
            check("child parentId == root spanId", child.context().parentId().equals(root.context().spanId()));
            child.end();
        }
        root.end();

        // 3. nextSpan(parent) explicit parent
        SimpleSpan explicitParent = tracer.nextSpan().name("p").start();
        SimpleSpan explicitChild = tracer.nextSpan(explicitParent).name("c").start();
        check("explicit child parentId set", explicitChild.context().parentId().equals(explicitParent.context().spanId()));
        explicitChild.end();
        explicitParent.end();

        // 4. startScopedSpan puts current span in scope and clears on end
        ScopedSpan scoped = tracer.startScopedSpan("scoped");
        check("startScopedSpan returns non-noop scoped span", !scoped.isNoop());
        scoped.tag("k", "v").event("e").end();
        check("ending scoped span clears current span", tracer.currentSpan() == null);

        // 5. Span.Builder parent / noParent / links / startTimestamp
        SimpleSpan builderParent = tracer.nextSpan().name("bp").start();
        Link link = new Link(builderParent.context(), Map.of("l", "1"));
        Span built = tracer.spanBuilder().setParent(builderParent.context()).name("built").tag("a", "b").addLink(link).startTimestamp(1, TimeUnit.SECONDS).start();
        SimpleSpan builtSimple = (SimpleSpan) built;
        check("builder copies parent trace id", built.context().traceId().equals(builderParent.context().traceId()));
        check("builder sets parent span id", built.context().parentId().equals(builderParent.context().spanId()));
        check("builder preserves links", builtSimple.getLinks().size() == 1);
        check("builder uses explicit startTimestamp", builtSimple.getStartTimestamp().toEpochMilli() == 1000L);
        built.end();
        builderParent.end();

        Span noParentBuilt = tracer.spanBuilder().setNoParent().name("np").start();
        check("setNoParent clears parent relation in simple builder", noParentBuilt.context().parentId() == null || noParentBuilt.context().parentId().isEmpty());
        noParentBuilt.end();

        // 6. abandon stops lifecycle without normal recording marker
        SimpleSpan abandoned = tracer.nextSpan().name("abandoned").start();
        abandoned.abandon();
        check("abandon marks span abandoned", abandoned.toString().contains("abandoned=true"));

        // 7. CurrentTraceContext newScope / maybeScope
        SimpleSpan scopeSpan = tracer.nextSpan().name("scope").start();
        TraceContext ctx = scopeSpan.context();
        try (CurrentTraceContext.Scope s1 = tracer.currentTraceContext().newScope(ctx)) {
            check("newScope sets current context", tracer.currentTraceContext().context().spanId().equals(ctx.spanId()));
            try (CurrentTraceContext.Scope s2 = tracer.currentTraceContext().maybeScope(ctx)) {
                check("maybeScope same context keeps current", tracer.currentTraceContext().context().spanId().equals(ctx.spanId()));
            }
        }
        check("closing scope restores null current context", tracer.currentTraceContext().context() == null);
        scopeSpan.end();

        // 8. withSpan scope does not end span
        SimpleSpan wsSpan = tracer.nextSpan().name("ws").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(wsSpan)) {
            check("withSpan sets current span", tracer.currentSpan() == wsSpan);
        }
        check("closing SpanInScope does not end span", wsSpan.getEndTimestamp().toEpochMilli() == 0L);
        wsSpan.end();

        // 9. SpanAndScope closes scope then ends span
        SimpleSpan sas = tracer.nextSpan().name("sas").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(sas)) {
            new SpanAndScope(sas, ws).close();
        }
        check("SpanAndScope ends span", sas.getEndTimestamp().toEpochMilli() > 0L);

        // 10. ThreadLocalSpan stack and baggage in current scope
        SimpleSpan stackSpan = tracer.nextSpan().name("stack").start();
        ThreadLocalSpan threadLocalSpan = new ThreadLocalSpan(tracer);
        threadLocalSpan.set(stackSpan);
        check("ThreadLocalSpan get returns current entry", threadLocalSpan.get() != null && threadLocalSpan.get().getSpan() == stackSpan);
        SpanAndScope removedStack = threadLocalSpan.remove();
        check("ThreadLocalSpan remove returns entry without ending span", removedStack != null && stackSpan.getEndTimestamp().toEpochMilli() == 0L);
        stackSpan.end();

        SimpleSpan bagCurrent = tracer.nextSpan().name("bagCurrent").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(bagCurrent)) {
            BaggageInScope bis = tracer.createBaggageInScope("country", "PL");
            check("createBaggageInScope requires current context and is visible", "PL".equals(tracer.getBaggage("country").get()));
            bis.close();
        }
        bagCurrent.end();

        SimpleSpan bagParent = tracer.nextSpan().name("bagP").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(bagParent)) {
            BaggageInScope bis2 = tracer.createBaggageInScope("tenant", "acme");
            check("getAllBaggage(traceContext) overridden by simple tracer", "acme".equals(tracer.getAllBaggage(bagParent.context()).get("tenant")));
            bis2.close();
        }
        bagParent.end();

        // 11. trace context builder sampled tri-state
        TraceContext tc = tracer.traceContextBuilder().traceId("t1").spanId("s1").parentId("p1").sampled(true).build();
        check("traceContext builder sets fields", tc.traceId().equals("t1") && tc.spanId().equals("s1") && tc.parentId().equals("p1"));
        check("traceContext builder sets sampled when explicit", Boolean.TRUE.equals(tc.sampled()));

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}
