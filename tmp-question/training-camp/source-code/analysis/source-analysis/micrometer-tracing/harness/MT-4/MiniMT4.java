package io.micrometer.tracing.annotation;

import io.micrometer.common.annotation.ValueExpressionResolver;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.*;
import io.micrometer.tracing.test.simple.SimpleSpan;
import io.micrometer.tracing.test.simple.SimpleTracer;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

public class MiniMT4 {
    static int pass = 0, fail = 0;
    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("  PASS " + name); }
        else { fail++; System.out.println("  FAIL " + name); }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("== MiniMT4: 注解切面 ==");

        SimpleTracer tracer = new SimpleTracer();
        MethodInvocationProcessor proc = new ImperativeMethodInvocationProcessor(new DefaultNewSpanParser(), tracer,
                c -> (ValueResolver) parameter -> "R:" + parameter,
                c -> (ValueExpressionResolver) (expression, parameter) -> "E:" + expression + ":" + parameter);

        // 1. @NewSpan default name lower-hyphen
        proc.process(invocation("camelCaseMethod", new Object[0], () -> null), Demo.class.getMethod("camelCaseMethod").getAnnotation(NewSpan.class), null);
        check("@NewSpan default method name lower-hyphen", "camel-case-method".equals(tracer.lastSpan().getName()));

        // 2. @NewSpan custom name precedence
        proc.process(invocation("customName", new Object[0], () -> null), Demo.class.getMethod("customName").getAnnotation(NewSpan.class), null);
        check("@NewSpan custom name honored", "custom-span".equals(tracer.lastSpan().getName()));

        // 3. SpanTag resolver/expression/toString via SpanTagAnnotationHandler
        SpanTagAnnotationHandler handler = new SpanTagAnnotationHandler(c -> (ValueResolver) parameter -> "R:" + parameter,
                c -> (ValueExpressionResolver) (expression, parameter) -> "E:" + expression + ":" + parameter);
        SpanTag a1 = (SpanTag) Demo.class.getMethod("withResolver", String.class).getParameterAnnotations()[0][0];
        SpanTag a2 = (SpanTag) Demo.class.getMethod("withExpression", String.class).getParameterAnnotations()[0][0];
        SpanTag a3 = (SpanTag) Demo.class.getMethod("withToString", Long.class).getParameterAnnotations()[0][0];
        check("SpanTag resolver applied", "R:abc".equals(SpanTagAnnotationHandler.resolveTagValue(a1, "abc", c -> (ValueResolver) parameter -> "R:" + parameter, c -> (ValueExpressionResolver) (expression, parameter) -> "E:" + expression + ":" + parameter)));
        check("SpanTag expression applied", "E:spel:abc".equals(SpanTagAnnotationHandler.resolveTagValue(a2, "abc", c -> (ValueResolver) parameter -> "R:" + parameter, c -> (ValueExpressionResolver) (expression, parameter) -> "E:" + expression + ":" + parameter)));
        check("SpanTag toString fallback", "15".equals(SpanTagAnnotationHandler.resolveTagValue(a3, 15L, c -> (ValueResolver) parameter -> "R:" + parameter, c -> (ValueExpressionResolver) (expression, parameter) -> "E:" + expression + ":" + parameter)));

        // 4. ContinueSpan with existing span logs before/after and does not create extra span
        Span existing = tracer.nextSpan().name("existing").start();
        int before = tracer.getSpans().size();
        try (Tracer.SpanInScope ws = tracer.withSpan(existing)) {
            proc.process(invocation("continueWithLog", new Object[]{"x"}, () -> null), null, Demo.class.getMethod("continueWithLog", String.class).getAnnotation(ContinueSpan.class));
        }
        existing.end();
        check("ContinueSpan reuses existing span", tracer.getSpans().size() == before && "existing".equals(tracer.lastSpan().getName()));
        check("ContinueSpan logs before/after", tracer.lastSpan().getEvents().stream().map(java.util.Map.Entry::getValue).collect(Collectors.toList()).contains("log.before") && tracer.lastSpan().getEvents().stream().map(java.util.Map.Entry::getValue).collect(Collectors.toList()).contains("log.after"));

        // 5. ContinueSpan without current span creates and closes one
        before = tracer.getSpans().size();
        proc.process(invocation("continueWithLog", new Object[]{"y"}, () -> null), null, Demo.class.getMethod("continueWithLog", String.class).getAnnotation(ContinueSpan.class));
        check("ContinueSpan without current span creates one", tracer.getSpans().size() == before + 1);
        check("ContinueSpan fallback span named from method", "continue-with-log".equals(tracer.lastSpan().getName()));

        // 6. NewSpan failure records error and ends
        try { proc.process(invocation("newSpanFailure", new Object[]{"boom"}, () -> { throw new RuntimeException("boom"); }), Demo.class.getMethod("newSpanFailure", String.class).getAnnotation(NewSpan.class), null); } catch (RuntimeException ignored) {}
        check("NewSpan failure records error", tracer.lastSpan().getError() != null && tracer.lastSpan().getError().getMessage().contains("boom"));

        // 7. ContinueSpan failure records error + afterFailure + after
        Span c = tracer.nextSpan().name("cspan").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(c)) {
            try { proc.process(invocation("continueFailure", new Object[0], () -> { throw new RuntimeException("cf"); }), null, Demo.class.getMethod("continueFailure").getAnnotation(ContinueSpan.class)); } catch (RuntimeException ignored) {}
        }
        c.end();
        check("ContinueSpan failure keeps existing span name", "cspan".equals(tracer.lastSpan().getName()));
        check("ContinueSpan without log does not add before/afterFailure events", tracer.lastSpan().getEvents().isEmpty());

        // 8. Missing SpanTagAnnotationHandler means no param tag
        SimpleTracer tracer2 = new SimpleTracer();
        MethodInvocationProcessor proc2 = new ImperativeMethodInvocationProcessor(new DefaultNewSpanParser(), tracer2);
        proc2.process(invocation("withResolver", new Object[]{"abc"}, () -> null), Demo.class.getMethod("withResolver", String.class).getAnnotation(NewSpan.class), null);
        check("missing handler means no span tag", !tracer2.lastSpan().getTags().containsKey("k1"));

        // 9. Interface + impl annotations are both effectively supported in tests; direct method invocation uses impl method metadata
        check("impl method carries annotation directly", Demo.class.getMethod("customName").getAnnotation(NewSpan.class) != null);

        System.out.println("== 结果: " + pass + " PASS, " + fail + " FAIL ==");
        if (fail > 0) System.exit(1);
    }

    static FakeInvocation invocation(String methodName, Object[] args, Thrower t) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        Method m = Demo.class.getMethod(methodName, types);
        return new FakeInvocation(new Demo(), m, args, t);
    }

    interface Thrower { Object call() throws Throwable; }

    static class FakeInvocation implements MethodInvocation {
        final Object target; final Method method; final Object[] args; final Thrower t;
        FakeInvocation(Object target, Method method, Object[] args, Thrower t){this.target=target;this.method=method;this.args=args;this.t=t;}
        @Override public Method getMethod() { return method; }
        @Override public Object[] getArguments() { return args; }
        @Override public Object proceed() throws Throwable { return t.call(); }
        @Override public Object getThis() { return target; }
        @Override public AccessibleObject getStaticPart() { return method; }
    }

    static class Demo {
        @NewSpan public void camelCaseMethod() {}
        @NewSpan(name = "custom-span") public void customName() {}
        @NewSpan public void withResolver(@SpanTag(key = "k1", resolver = ValueResolver.class) String v) {}
        @NewSpan public void withExpression(@SpanTag(key = "k2", expression = "spel") String v) {}
        @NewSpan public void withToString(@SpanTag(key = "k3") Long v) {}
        @ContinueSpan(log = "log") public void continueWithLog(@SpanTag(key = "k4") String v) {}
        @NewSpan public void newSpanFailure(@SpanTag(key = "k5") String v) { throw new RuntimeException(v); }
        @ContinueSpan public void continueFailure() { throw new RuntimeException("cf"); }
    }
}
