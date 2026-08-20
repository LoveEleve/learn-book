import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;

import java.util.HashMap;
import java.util.Map;

public class MiniMT3 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== MiniMT3: Observation→Tracing 处理链 ==");

        // 1. default handler creates span from observation and stops with tags
        SimpleTracer tracer = new SimpleTracer();
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer));
        Observation obs = Observation.start("tech", registry)
                .contextualName("ctx")
                .lowCardinalityKeyValue(KeyValue.of("k", "v"));
        obs.error(new IllegalStateException("boom"));
        obs.stop();
        SimpleSpan s1 = tracer.lastSpan();
        check("default handler uses contextualName as span name", "ctx".equals(s1.getName()));
        check("default handler tags key values", "v".equals(s1.getTags().get("k")));
        check("default handler forwards error", s1.getError() instanceof IllegalStateException);

        // 2. parent span from current observation scope
        Observation parent = Observation.start("parent", registry);
        try (Observation.Scope ignored = parent.openScope()) {
            Observation child = Observation.start("child", registry);
            child.stop();
            SimpleSpan childSpan = tracer.lastSpan();
            Span parentSpan = ((TracingObservationHandler.TracingContext) parent.getContext().getRequired(TracingObservationHandler.TracingContext.class)).getSpan();
            check("child observation gets parent span from current observation", childSpan.context().parentId().equals(parentSpan.context().spanId()));
        }
        parent.stop();

        // 3. event timestamp branching
        Observation obs3 = Observation.createNotStarted("e", registry).start();
        obs3.event(Observation.Event.of("plain"));
        obs3.event(new Observation.Event() {
            @Override public String getName() { return "timed"; }
            @Override public long getWallTime() { return 100L; }
        });
        obs3.stop();
        SimpleSpan s3 = tracer.lastSpan();
        check("events recorded on span", s3.getEvents().size() >= 2);

        // 4. onScopeClosed with no scope does not fail / span retained
        TracingObservationHandler<Observation.Context> h4 = new DefaultTracingObservationHandler(tracer);
        Observation.Context c4 = new Observation.Context();
        TracingObservationHandler.TracingContext tc4 = new TracingObservationHandler.TracingContext();
        Span preserved = tracer.nextSpan().name("preserved").start();
        tc4.setSpan(preserved);
        c4.put(TracingObservationHandler.TracingContext.class, tc4);
        h4.onScopeClosed(c4);
        check("null scope supported", tc4.getSpan() == preserved);
        preserved.end();

        // 5. sender handler injects propagation and tags remote metadata
        SimpleTracer tracer5 = new SimpleTracer();
        Propagator fakeProp = new Propagator() {
            @Override public java.util.List<String> fields() { return java.util.List.of("trace-id"); }
            @Override public <C> void inject(TraceContext context, C carrier, Setter<C> setter) { setter.set(carrier, "trace-id", context.traceId()); }
            @Override public <C> Span.Builder extract(C carrier, Getter<C> getter) { return tracer5.spanBuilder(); }
        };
        PropagatingSenderTracingObservationHandler<SenderContext<Map<String,String>>> sender = new PropagatingSenderTracingObservationHandler<>(tracer5, fakeProp);
        SenderContext<Map<String,String>> senderCtx = new SenderContext<>((carrier, key, value) -> carrier.put(key, value), Kind.CLIENT);
        senderCtx.setCarrier(new HashMap<>());
        senderCtx.setRemoteServiceName("remote");
        senderCtx.setRemoteServiceAddress("http://example.com:8080");
        senderCtx.setName("send");
        sender.onStart(senderCtx);
        Span sendSpan = ((TracingObservationHandler.TracingContext) senderCtx.getRequired(TracingObservationHandler.TracingContext.class)).getSpan();
        check("sender injects trace id into carrier", senderCtx.getCarrier().containsKey("trace-id"));
        sender.onStop(senderCtx);
        SimpleSpan sent = (SimpleSpan) sendSpan;
        check("sender stop names span", "send".equals(sent.getName()));
        check("sender remote service set", "remote".equals(sent.getRemoteServiceName()));

        // 6. receiver handler extracts and starts span from carrier
        SimpleTracer tracer6 = new SimpleTracer();
        Propagator fakeRecv = new Propagator() {
            @Override public java.util.List<String> fields() { return java.util.List.of("trace-id"); }
            @Override public <C> void inject(TraceContext context, C carrier, Setter<C> setter) {}
            @Override public <C> Span.Builder extract(C carrier, Getter<C> getter) {
                String traceId = getter.get(carrier, "trace-id");
                return tracer6.spanBuilder().setParent(tracer6.traceContextBuilder().traceId(traceId).spanId("p1").build());
            }
        };
        PropagatingReceiverTracingObservationHandler<ReceiverContext<Map<String,String>>> recv = new PropagatingReceiverTracingObservationHandler<>(tracer6, fakeRecv);
        ReceiverContext<Map<String,String>> recvCtx = new ReceiverContext<>((carrier, key) -> carrier.get(key), Kind.SERVER);
        recvCtx.setCarrier(Map.of("trace-id", "abc"));
        recvCtx.setName("recv");
        recv.onStart(recvCtx);
        Span recvSpan = ((TracingObservationHandler.TracingContext) recvCtx.getRequired(TracingObservationHandler.TracingContext.class)).getSpan();
        check("receiver extract uses carrier trace id", recvSpan.context().traceId().equals("abc"));
        recv.onStop(recvCtx);
        check("receiver stop names span", ((SimpleSpan) recvSpan).getName().equals("recv"));

        // 7. tracing-aware meter handler temporarily scopes current span on stop
        final boolean[] sawSpan = {false};
        io.micrometer.core.instrument.observation.MeterObservationHandler<Observation.Context> delegate = new io.micrometer.core.instrument.observation.MeterObservationHandler<>() {
            @Override public void onStart(Observation.Context context) {}
            @Override public void onError(Observation.Context context) {}
            @Override public void onEvent(Observation.Event event, Observation.Context context) {}
            @Override public void onScopeOpened(Observation.Context context) {}
            @Override public void onScopeClosed(Observation.Context context) {}
            @Override public void onStop(Observation.Context context) { sawSpan[0] = tracer.currentSpan() != null; }
            @Override public boolean supportsContext(Observation.Context context) { return true; }
        };
        TracingAwareMeterObservationHandler<Observation.Context> aware = new TracingAwareMeterObservationHandler<>(delegate, tracer);
        Observation.Context ctx7 = new Observation.Context();
        TracingObservationHandler.TracingContext tctx7 = new TracingObservationHandler.TracingContext();
        tctx7.setSpan(tracer.nextSpan().name("meter").start());
        ctx7.put(TracingObservationHandler.TracingContext.class, tctx7);
        aware.onStop(ctx7);
        check("meter handler sees span in scope during stop", sawSpan[0]);

        // 8. manual current span wins over parent observation span
        SimpleTracer tracer8 = new SimpleTracer();
        ObservationRegistry reg8 = ObservationRegistry.create();
        reg8.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer8));
        Observation parent8 = Observation.start("p8", reg8);
        try (Observation.Scope ps = parent8.openScope()) {
            Span manual = tracer8.nextSpan().name("manual-child").start();
            try (Tracer.SpanInScope ms = tracer8.withSpan(manual)) {
                Observation child8 = Observation.start("c8", reg8);
                child8.stop();
                SimpleSpan last = tracer8.lastSpan();
                check("manual current span wins over parent observation span", last.context().parentId().equals(manual.context().spanId()));
            }
            manual.end();
        }
        parent8.stop();

        // 9. RevertingScope baggage fields are copied from matching observation key values
        class BaggageAwareTracer extends SimpleTracer {
            @Override public java.util.List<String> getBaggageFields() { return java.util.List.of("tenant", "region"); }
        }
        BaggageAwareTracer tracer9 = new BaggageAwareTracer();
        ObservationRegistry reg9 = ObservationRegistry.create();
        reg9.observationConfig().observationHandler(new DefaultTracingObservationHandler(tracer9));
        Observation obs9 = Observation.start("obs9", reg9)
                .lowCardinalityKeyValue(KeyValue.of("tenant", "acme"))
                .highCardinalityKeyValue(KeyValue.of("region", "eu"))
                .lowCardinalityKeyValue(KeyValue.of("ignored", "x"));
        try (Observation.Scope scope9 = obs9.openScope()) {
            check("scope opening propagates matching baggage field tenant", "acme".equals(tracer9.getBaggage("tenant").get()));
            check("scope opening propagates matching baggage field region", "eu".equals(tracer9.getBaggage("region").get()));
            check("non-baggage key not propagated", tracer9.getBaggage("ignored").get() == null);
        }
        obs9.stop();

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }
}
