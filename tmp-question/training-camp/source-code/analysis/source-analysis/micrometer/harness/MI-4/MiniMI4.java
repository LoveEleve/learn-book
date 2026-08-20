package io.micrometer.core.aop;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.CountedMeterTagAnnotationHandler;
import io.micrometer.core.aop.MeterTag;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.MeterTagSupport;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.SourceLocation;
import org.aspectj.runtime.internal.AroundClosure;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MiniMI4 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    static final ValueResolver STRING_RESOLVER = parameter -> "R:" + parameter;
    static final ValueExpressionResolver EXPR_RESOLVER = (expression, parameter) -> "E:" + expression + ":" + parameter;

    public static void main(String[] args) throws Throwable {
        System.out.println("== MiniMI4: AOP / annotations ==");

        // 1. MeterTagSupport precedence: resolver > expression > toString
        Method m1 = Demo.class.getMethod("meterTagResolver", String.class);
        MeterTag a1 = (MeterTag) m1.getParameterAnnotations()[0][0];
        check("resolver precedence", MeterTagSupport.resolveTagValue(a1, "x", c -> STRING_RESOLVER, c -> EXPR_RESOLVER).equals("R:x"));

        Method m2 = Demo.class.getMethod("meterTagExpression", String.class);
        MeterTag a2 = (MeterTag) m2.getParameterAnnotations()[0][0];
        check("expression precedence", MeterTagSupport.resolveTagValue(a2, "x", c -> STRING_RESOLVER, c -> EXPR_RESOLVER).equals("E:spel:x"));

        Method m3 = Demo.class.getMethod("meterTagToString", Integer.class);
        MeterTag a3 = (MeterTag) m3.getParameterAnnotations()[0][0];
        check("fallback to toString", MeterTagSupport.resolveTagValue(a3, 12, c -> STRING_RESOLVER, c -> EXPR_RESOLVER).equals("12"));
        check("null fallback to empty string", MeterTagSupport.resolveTagValue(a3, null, c -> STRING_RESOLVER, c -> EXPR_RESOLVER).equals(""));

        // 2. TimedAspect sync success
        SimpleMeterRegistry reg2 = new SimpleMeterRegistry();
        TimedAspect timedAspect = new TimedAspect(reg2);
        ProceedingJoinPoint pjp2 = new FakePjp(new Demo(), Demo.class.getMethod("timedOk"), () -> "ok", new Object[0]);
        Object out2 = timedAspect.timedMethod(pjp2);
        Timer timer2 = reg2.get("method.timed").tag("exception", "none").timer();
        check("timed sync returns result", out2.equals("ok"));
        check("timed sync records timer", timer2.count() == 1);

        // 3. TimedAspect sync exception tag
        SimpleMeterRegistry reg3 = new SimpleMeterRegistry();
        TimedAspect timedAspect3 = new TimedAspect(reg3);
        ProceedingJoinPoint pjp3 = new FakePjp(new Demo(), Demo.class.getMethod("timedFail"), () -> { throw new IllegalStateException("boom"); }, new Object[0]);
        boolean threw3 = false;
        try { timedAspect3.timedMethod(pjp3); } catch (IllegalStateException e) { threw3 = true; }
        check("timed sync propagates exception", threw3);
        check("timed sync failure tagged", reg3.get("method.timed").tag("exception", "IllegalStateException").timer().count() == 1);

        // 4. TimedAspect CompletionStage completion delayed record
        SimpleMeterRegistry reg4 = new SimpleMeterRegistry();
        TimedAspect timedAspect4 = new TimedAspect(reg4);
        CompletableFuture<String> f4 = new CompletableFuture<>();
        ProceedingJoinPoint pjp4 = new FakePjp(new Demo(), Demo.class.getMethod("timedAsync"), () -> f4, new Object[0]);
        CompletionStage<?> out4 = (CompletionStage<?>) timedAspect4.timedMethod(pjp4);
        check("timed async initially no timer count", reg4.find("method.timed").timer() == null);
        f4.complete("x");
        out4.toCompletableFuture().join();
        check("timed async records on completion", reg4.get("method.timed").tag("exception", "none").timer().count() == 1);

        // 5. TimedAspect skip predicate
        SimpleMeterRegistry reg5 = new SimpleMeterRegistry();
        TimedAspect timedAspect5 = new TimedAspect(reg5, (java.util.function.Predicate<ProceedingJoinPoint>) pjp -> true);
        ProceedingJoinPoint pjp5 = new FakePjp(new Demo(), Demo.class.getMethod("timedOk"), () -> "ok", new Object[0]);
        timedAspect5.timedMethod(pjp5);
        check("timed skip suppresses timer", reg5.find("method.timed").timer() == null);

        // 6. CountedAspect success/failure + recordFailuresOnly
        SimpleMeterRegistry reg6 = new SimpleMeterRegistry();
        CountedAspect countedAspect = new CountedAspect(reg6);
        ProceedingJoinPoint pjp6 = new FakePjp(new Demo(), Demo.class.getMethod("countedOk"), () -> "ok", new Object[0]);
        countedAspect.interceptAndRecord(pjp6, Demo.class.getMethod("countedOk").getAnnotation(Counted.class));
        check("counted success counter increments", reg6.get("method.counted").tag("result", "success").counter().count() == 1.0);

        ProceedingJoinPoint pjp6b = new FakePjp(new Demo(), Demo.class.getMethod("countedFail"), () -> { throw new IllegalArgumentException("x"); }, new Object[0]);
        boolean threw6 = false;
        try { countedAspect.interceptAndRecord(pjp6b, Demo.class.getMethod("countedFail").getAnnotation(Counted.class)); } catch (IllegalArgumentException e) { threw6 = true; }
        check("counted failure propagates", threw6);
        check("counted failure tagged", reg6.get("method.counted").tag("result", "failure").tag("exception", "IllegalArgumentException").counter().count() == 1.0);

        SimpleMeterRegistry reg6c = new SimpleMeterRegistry();
        CountedAspect countedAspect6c = new CountedAspect(reg6c);
        ProceedingJoinPoint pjp6c = new FakePjp(new Demo(), Demo.class.getMethod("countedFailuresOnlyOk"), () -> "ok", new Object[0]);
        countedAspect6c.interceptAndRecord(pjp6c, Demo.class.getMethod("countedFailuresOnlyOk").getAnnotation(Counted.class));
        check("recordFailuresOnly suppresses success", reg6c.find("method.counted").counter() == null);

        // 7. CountedAspect CompletionStage failure records on completion
        SimpleMeterRegistry reg7 = new SimpleMeterRegistry();
        CountedAspect countedAspect7 = new CountedAspect(reg7);
        CompletableFuture<String> f7 = new CompletableFuture<>();
        ProceedingJoinPoint pjp7 = new FakePjp(new Demo(), Demo.class.getMethod("countedAsync"), () -> f7, new Object[0]);
        CompletionStage<?> out7 = (CompletionStage<?>) countedAspect7.interceptAndRecord(pjp7, Demo.class.getMethod("countedAsync").getAnnotation(Counted.class));
        f7.completeExceptionally(new RuntimeException(new IllegalStateException("nested")));
        try { out7.toCompletableFuture().join(); } catch (Exception ignored) {}
        check("counted async failure uses cause simple name", reg7.get("method.counted").tag("result", "failure").tag("exception", "IllegalStateException").counter().count() == 1.0);

        // 8. MeterTag handlers add parameter tags
        SimpleMeterRegistry reg8 = new SimpleMeterRegistry();
        TimedAspect timedAspect8 = new TimedAspect(reg8);
        timedAspect8.setMeterTagAnnotationHandler(new MeterTagAnnotationHandler(c -> STRING_RESOLVER, c -> EXPR_RESOLVER));
        ProceedingJoinPoint pjp8 = new FakePjp(new Demo(), Demo.class.getMethod("timedWithTag", String.class), () -> "ok", new Object[]{"abc"});
        timedAspect8.timedMethod(pjp8);
        check("timed meterTag added", reg8.get("method.timed").tag("k", "R:abc").timer().count() == 1);

        SimpleMeterRegistry reg8b = new SimpleMeterRegistry();
        CountedAspect countedAspect8b = new CountedAspect(reg8b);
        countedAspect8b.setMeterTagAnnotationHandler(new CountedMeterTagAnnotationHandler(c -> STRING_RESOLVER, c -> EXPR_RESOLVER));
        ProceedingJoinPoint pjp8b = new FakePjp(new Demo(), Demo.class.getMethod("countedWithTag", String.class), () -> "ok", new Object[]{"abc"});
        countedAspect8b.interceptAndRecord(pjp8b, Demo.class.getMethod("countedWithTag", String.class).getAnnotation(Counted.class));
        check("counted meterTag added", reg8b.get("method.counted").tag("k", "R:abc").counter().count() == 1.0);

        // 9. longTask path creates LongTaskTimer and stops it
        SimpleMeterRegistry reg9 = new SimpleMeterRegistry();
        TimedAspect timedAspect9 = new TimedAspect(reg9);
        ProceedingJoinPoint pjp9 = new FakePjp(new Demo(), Demo.class.getMethod("longTaskOk"), () -> "ok", new Object[0]);
        timedAspect9.timedMethod(pjp9);
        LongTaskTimer ltt9 = reg9.get("ltask").longTaskTimer();
        check("longTask timer registered", ltt9 != null);
        check("longTask timer stopped", ltt9.activeTasks() == 0);

        // 10. repeatable @Timed metadata is stored in TimedSet, not Timed.class
        Method repeated = Demo.class.getMethod("repeatedTimed");
        check("repeated @Timed not visible via getAnnotation(Timed.class)", repeated.getAnnotation(Timed.class) == null);
        check("repeated @Timed visible via TimedSet container", repeated.getAnnotation(io.micrometer.core.annotation.TimedSet.class) != null && repeated.getAnnotationsByType(Timed.class).length == 2);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }

    static class Demo {
        @Timed public String timedOk() { return "ok"; }
        @Timed public String timedFail() { throw new IllegalStateException("boom"); }
        @Timed public CompletionStage<String> timedAsync() { return null; }
        @Counted public String countedOk() { return "ok"; }
        @Counted public String countedFail() { throw new IllegalArgumentException("x"); }
        @Counted(recordFailuresOnly = true) public String countedFailuresOnlyOk() { return "ok"; }
        @Counted public CompletionStage<String> countedAsync() { return null; }
        @Timed public String timedWithTag(@MeterTag(key = "k", resolver = ValueResolver.class) String v) { return v; }
        @Counted public String countedWithTag(@MeterTag(key = "k", resolver = ValueResolver.class) String v) { return v; }
        @Timed(value = "ltask", longTask = true) public String longTaskOk() { return "ok"; }
        @Timed("a") @Timed("b") public String repeatedTimed() { return "ok"; }
        public void meterTagResolver(@MeterTag(key = "k", resolver = ValueResolver.class) String v) {}
        public void meterTagExpression(@MeterTag(key = "k", expression = "spel") String v) {}
        public void meterTagToString(@MeterTag("k") Integer v) {}
    }

    static class FakePjp implements ProceedingJoinPoint {
        @Override public void set$AroundClosure(AroundClosure arc) {}
        private final Object target; private final Method method; private final ThrowingSupplier supplier; private final Object[] args;
        FakePjp(Object target, Method method, ThrowingSupplier supplier, Object[] args) { this.target = target; this.method = method; this.supplier = supplier; this.args = args; }
        @Override public Object proceed() throws Throwable { return supplier.get(); }
        @Override public Object proceed(Object[] objects) throws Throwable { return supplier.get(); }
        @Override public Object getThis() { return target; }
        @Override public Object getTarget() { return target; }
        @Override public Object[] getArgs() { return args; }
        @Override public Signature getSignature() { return new FakeMethodSignature(method); }
        @Override public SourceLocation getSourceLocation() { return null; }
        @Override public String getKind() { return "method-execution"; }
        @Override public StaticPart getStaticPart() { return new StaticPart() { @Override public String toShortString(){return method.getName();} @Override public String toLongString(){return method.toString();} @Override public Signature getSignature(){ return new FakeMethodSignature(method);} @Override public SourceLocation getSourceLocation(){return null;} @Override public String getKind(){return "method-execution";} @Override public int getId(){return 0;} }; }
        @Override public String toShortString(){ return method.getName(); }
        @Override public String toLongString(){ return method.toString(); }
    }

    static class FakeMethodSignature implements MethodSignature {
        private final Method method; FakeMethodSignature(Method method){this.method=method;}
        @Override public Method getMethod(){ return method; }
        @Override public Class<?> getReturnType(){ return method.getReturnType(); }
        @Override public Class<?>[] getParameterTypes(){ return method.getParameterTypes(); }
        @Override public String[] getParameterNames(){ return Arrays.stream(method.getParameters()).map(p -> p.getName()).toArray(String[]::new); }
        @Override public Class<?>[] getExceptionTypes(){ return method.getExceptionTypes(); }
        @Override public String toShortString(){ return method.getName(); }
        @Override public String toLongString(){ return method.toString(); }
        @Override public String getName(){ return method.getName(); }
        @Override public int getModifiers(){ return method.getModifiers(); }
        @Override public Class<?> getDeclaringType(){ return method.getDeclaringClass(); }
        @Override public String getDeclaringTypeName(){ return method.getDeclaringClass().getName(); }
    }

    interface ThrowingSupplier { Object get() throws Throwable; }
}
