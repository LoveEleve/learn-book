package io.micrometer.observation;

import io.micrometer.common.KeyValue;

import java.util.ArrayList;
import java.util.List;

public class MiniMI6 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== MiniMI6: Observation ==");

        // 1. no handlers => registry behaves noop
        ObservationRegistry reg1 = ObservationRegistry.create();
        Observation o1 = Observation.start("x", reg1);
        check("registry without handlers is noop", o1.isNoop());

        // 2. null registry => noop but scope handling observation
        Observation o2 = Observation.start("x", (ObservationRegistry) null);
        check("null registry returns noop", o2.isNoop());
        try (Observation.Scope scope = o2.openScope()) {
            check("noop still opens scope for propagation", scope != null);
        }

        // 3. predicate false => noop but scope handling
        ObservationRegistry reg3 = ObservationRegistry.create();
        reg3.observationConfig().observationHandler(new AnyHandler());
        reg3.observationConfig().observationPredicate((name, context) -> false);
        Observation o3 = Observation.start("x", reg3);
        check("predicate false returns noop", o3.isNoop());
        try (Observation.Scope scope = o3.openScope()) {
            check("predicate-disabled noop still supports scope", reg3.getCurrentObservation() != null);
        }
        check("predicate-disabled scope closes cleanly", reg3.getCurrentObservation() == null);

        // 4. handler callbacks order: start forward, scopeClosed reverse, stop reverse
        ObservationRegistry reg4 = ObservationRegistry.create();
        java.util.List<String> shared4 = new java.util.ArrayList<>();
        RecordingHandler h1 = new RecordingHandler("h1", shared4);
        RecordingHandler h2 = new RecordingHandler("h2", shared4);
        reg4.observationConfig().observationHandler(h1).observationHandler(h2);
        Observation o4 = Observation.createNotStarted("obs4", reg4);
        o4.start();
        try (Observation.Scope ignored = o4.openScope()) {
            o4.event(new Observation.Event() { public String getName(){ return "ev";} public String getContextualName(){ return "ev";} });
            o4.error(new IllegalStateException("boom"));
        }
        o4.stop();
        List<String> log4 = h1.shared;
        check("start order forward", log4.indexOf("start:h1") < log4.indexOf("start:h2"));
        check("scope close reverse", log4.indexOf("scopeClosed:h2") < log4.indexOf("scopeClosed:h1"));
        check("stop reverse", log4.indexOf("stop:h2") < log4.indexOf("stop:h1"));

        // 5. current observation + parent from scope
        ObservationRegistry reg5 = ObservationRegistry.create();
        reg5.observationConfig().observationHandler(new AnyHandler());
        Observation parent = Observation.start("parent", reg5);
        try (Observation.Scope s = parent.openScope()) {
            Observation child = Observation.createNotStarted("child", reg5);
            check("child parent auto-linked from current scope", child.getContext().getParentObservation() == parent);
        }
        parent.stop();

        // 6. manual parentObservation also links
        ObservationRegistry reg6 = ObservationRegistry.create();
        reg6.observationConfig().observationHandler(new AnyHandler());
        Observation p6 = Observation.start("p6", reg6);
        Observation c6 = Observation.createNotStarted("c6", reg6).parentObservation(p6);
        check("manual parent observation set", c6.getContext().getParentObservation() == p6);
        p6.stop();

        // 7. filter only affects stop context
        ObservationRegistry reg7 = ObservationRegistry.create();
        RecordingHandler h7 = new RecordingHandler("h7");
        reg7.observationConfig().observationHandler(h7);
        reg7.observationConfig().observationFilter(ctx -> { ctx.addLowCardinalityKeyValue(KeyValue.of("filtered", "yes")); return ctx; });
        Observation o7 = Observation.start("obs7", reg7);
        o7.error(new RuntimeException("x"));
        o7.stop();
        check("filter not visible on error callback", !h7.seenOnErrorFiltered);
        check("filter visible on stop callback", h7.seenOnStopFiltered);

        // 8. convention precedence: custom > global > default
        ObservationRegistry reg8 = ObservationRegistry.create();
        reg8.observationConfig().observationHandler(new AnyHandler());
        reg8.observationConfig().observationConvention(new DemoGlobalConvention());
        Observation o8 = Observation.createNotStarted(new DemoCustomConvention(), new DemoDefaultConvention(), Observation.Context::new, reg8);
        o8.start();
        check("custom convention wins name", o8.getContext().getName().equals("custom"));
        o8.stop();

        Observation o8b = Observation.createNotStarted(null, new DemoDefaultConvention(), Observation.Context::new, reg8);
        o8b.start();
        check("global convention beats default", o8b.getContext().getName().equals("global"));
        o8b.stop();

        // 9. first matching composite vs all matching composite
        Observation.Context ctx9 = new Observation.Context();
        MatchingHandler a = new MatchingHandler("a");
        MatchingHandler b = new MatchingHandler("b");
        ObservationHandler.FirstMatchingCompositeObservationHandler first = new ObservationHandler.FirstMatchingCompositeObservationHandler(a, b);
        first.onStart(ctx9);
        check("first matching only first handler runs", a.starts == 1 && b.starts == 0);
        ObservationHandler.AllMatchingCompositeObservationHandler all = new ObservationHandler.AllMatchingCompositeObservationHandler(a, b);
        all.onStart(ctx9);
        check("all matching runs all handlers", a.starts == 2 && b.starts == 1);

        // 10. openScope sets current and restores previous
        ObservationRegistry reg10 = ObservationRegistry.create();
        reg10.observationConfig().observationHandler(new AnyHandler());
        Observation a10 = Observation.start("a10", reg10);
        try (Observation.Scope sa = a10.openScope()) {
            Observation b10 = Observation.start("b10", reg10);
            try (Observation.Scope sb = b10.openScope()) {
                check("current is inner observation", reg10.getCurrentObservation() == b10);
            }
            check("after inner close current restored to parent", reg10.getCurrentObservation() == a10);
        }
        check("after outer close current cleared", reg10.getCurrentObservation() == null);
        a10.stop();

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }

    static class AnyHandler implements ObservationHandler<Observation.Context> {
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }

    static class RecordingHandler implements ObservationHandler<Observation.Context> {
        final String id;
        final List<String> shared;
        boolean seenOnErrorFiltered;
        boolean seenOnStopFiltered;
        RecordingHandler(String id) { this(id, new ArrayList<>()); }
        RecordingHandler(String id, List<String> shared) { this.id = id; this.shared = shared; }
        @Override public void onStart(Observation.Context context) { shared.add("start:" + id); }
        @Override public void onError(Observation.Context context) { shared.add("error:" + id); seenOnErrorFiltered = context.getLowCardinalityKeyValue("filtered") != null; }
        @Override public void onEvent(Observation.Event event, Observation.Context context) { shared.add("event:" + id); }
        @Override public void onScopeOpened(Observation.Context context) { shared.add("scopeOpened:" + id); }
        @Override public void onScopeClosed(Observation.Context context) { shared.add("scopeClosed:" + id); }
        @Override public void onStop(Observation.Context context) { shared.add("stop:" + id); seenOnStopFiltered = context.getLowCardinalityKeyValue("filtered") != null; }
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }

    static class MatchingHandler implements ObservationHandler<Observation.Context> {
        final String id; int starts;
        MatchingHandler(String id) { this.id = id; }
        @Override public void onStart(Observation.Context context) { starts++; }
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }

    static class DemoDefaultConvention implements ObservationConvention<Observation.Context> {
        @Override public String getName() { return "default"; }
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }
    static class DemoCustomConvention implements ObservationConvention<Observation.Context> {
        @Override public String getName() { return "custom"; }
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }
    static class DemoGlobalConvention implements GlobalObservationConvention<Observation.Context> {
        @Override public String getName() { return "global"; }
        @Override public boolean supportsContext(Observation.Context context) { return true; }
    }
}
