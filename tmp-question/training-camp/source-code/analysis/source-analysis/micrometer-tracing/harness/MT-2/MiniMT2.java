import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.BaggageToPropagate;
import io.micrometer.tracing.contextpropagation.ObservationAwareBaggageThreadLocalAccessor;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.contextpropagation.reactor.ReactorBaggage;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.test.simple.SimpleTracer;
import reactor.util.context.Context;

public class MiniMT2 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== MiniMT2: 传播与上下文适配 ==");

        // 1. BaggageToPropagate validation and copy
        boolean bad = false;
        try { new BaggageToPropagate("foo"); } catch (IllegalArgumentException e) { bad = true; }
        check("odd varargs rejected", bad);
        java.util.Map<String,String> m = new java.util.HashMap<>();
        m.put("foo", "bar");
        BaggageToPropagate b1 = new BaggageToPropagate(m);
        m.put("foo", "mutated");
        check("constructor copies incoming map", "bar".equals(b1.getBaggage().get("foo")));

        // 2. ReactorBaggage merge + overwrite
        Context c2 = Context.empty();
        Context c2a = ReactorBaggage.append("foo", "bar").apply(c2);
        BaggageToPropagate bp2a = c2a.get(ObservationAwareBaggageThreadLocalAccessor.KEY);
        check("reactor append single entry", "bar".equals(bp2a.getBaggage().get("foo")));
        Context c2b = ReactorBaggage.append(java.util.Map.of("foo", "new", "baz", "bar2")).apply(c2a);
        BaggageToPropagate bp2b = c2b.get(ObservationAwareBaggageThreadLocalAccessor.KEY);
        check("reactor merge overwrites same key", "new".equals(bp2b.getBaggage().get("foo")) && "bar2".equals(bp2b.getBaggage().get("baz")));

        // 3. Span accessor without observation falls back to tracer current span
        SimpleTracer tracer = new SimpleTracer();
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
        ObservationAwareSpanThreadLocalAccessor spanAccessor = new ObservationAwareSpanThreadLocalAccessor(registry, tracer);
        Span span = tracer.nextSpan().name("manual").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            check("span accessor returns current manual span without observation", spanAccessor.getValue() == span);
        }
        span.end();

        // 4. Baggage accessor without span warns/no-op on setValue and getValue null
        ObservationAwareBaggageThreadLocalAccessor baggageAccessor = new ObservationAwareBaggageThreadLocalAccessor(registry, tracer);
        check("baggage accessor no current span returns null", baggageAccessor.getValue() == null);
        baggageAccessor.setValue(new BaggageToPropagate("tenant", "acme"));
        check("setting baggage with no current span stays null", baggageAccessor.getValue() == null);

        // 5. Baggage accessor with current span reads and restores baggage
        Span span5 = tracer.nextSpan().name("with-baggage").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span5)) {
            baggageAccessor.setValue(new BaggageToPropagate("tenant", "acme", "region", "eu"));
            BaggageToPropagate got = baggageAccessor.getValue();
            check("baggage accessor reads propagated baggage", got != null && got.getBaggage().size() == 2 && "acme".equals(got.getBaggage().get("tenant")));
            baggageAccessor.restore();
            BaggageToPropagate afterRestore = baggageAccessor.getValue();
            check("restore clears current baggage scope", afterRestore == null || afterRestore.getBaggage().isEmpty());
        }
        span5.end();

        // 6. Observation-aware accessor backs off when observation span is current
        Observation observation = Observation.start("obs", registry);
        try (Observation.Scope scope = observation.openScope()) {
            Span fromAccessor = spanAccessor.getValue();
            check("accessor backs off when OTLA created span", fromAccessor == null);
            Span manualChild = tracer.nextSpan().name("child").start();
            try (Tracer.SpanInScope ws = tracer.withSpan(manualChild)) {
                check("manual child span wins over observation span", spanAccessor.getValue() == manualChild);
            }
            manualChild.end();
        }
        observation.stop();

        // 7. Thread-local span accessor set/restore with previous value stack
        Span a = tracer.nextSpan().name("a").start();
        Span b = tracer.nextSpan(a).name("b").start();
        spanAccessor.setValue(a);
        check("setValue sets current span", tracer.currentSpan() == a);
        spanAccessor.setValue(b);
        check("nested setValue overrides current span", tracer.currentSpan() == b);
        spanAccessor.restore(a);
        check("restore(previous) returns to previous span", tracer.currentSpan() == a);
        spanAccessor.restore();
        check("restore() clears span action stack", tracer.currentSpan() == null);
        a.end();
        b.end();

        // 8. Propagator NOOP surface
        io.micrometer.tracing.propagation.Propagator noop = io.micrometer.tracing.propagation.Propagator.NOOP;
        check("noop propagator fields empty", noop.fields().isEmpty());
        noop.inject(tracer.nextSpan().start().context(), new java.util.HashMap<String,String>(), java.util.Map::put);
        check("noop propagator extract returns builder noop", noop.extract(new java.util.HashMap<String,String>(), java.util.Map::get) == io.micrometer.tracing.Span.Builder.NOOP);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}
